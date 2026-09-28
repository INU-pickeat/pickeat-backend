# Pick Eat DB 구조 (ERD)

마지막 갱신일: 2026-09-28 · Flyway V3~V18 기준

아래 DBML을 [dbdiagram.io](https://dbdiagram.io/d)에 붙여넣으면 다이어그램이 그려진다. 마이그레이션을 추가하면 이 문서도 함께 갱신한다.

## 읽을 때 참고할 점

- DB의 enum 컬럼은 실제로 `VARCHAR`이고, 허용 값은 CHECK 제약으로 제한한다. 다이어그램에서 값이 보이도록 DBML `Enum`으로 표현했다. `data_provider`만 DB CHECK 없이 코드 enum으로 관리한다.
- `restaurants.google_place_id`는 NULL이 아닐 때만 unique인 부분 인덱스다. DBML에서는 일반 unique로 보인다.
- `restaurants.location`은 `latitude`·`longitude`로 자동 계산되는 PostGIS `geography(Point, 4326)` 컬럼이고 GiST 인덱스를 쓴다.
- 삭제 연쇄(`ON DELETE CASCADE`): 세션 → 카테고리·후보·제외, 탐색 지역 → 지역별 식당, 회원 → refresh token. 탐색 지역에 연결된 식당은 삭제할 수 없다(`RESTRICT`).
- V3의 `users` 테이블은 V4에서 삭제했으므로 포함하지 않는다.

## DBML

```dbml
Project pick_eat {
  database_type: 'PostgreSQL'
  Note: 'Pick Eat 백엔드 스키마 (Flyway V3~V18 기준, PostgreSQL 16 + PostGIS)'
}

// ───────── Enums ─────────
Enum food_category {
  KOREAN [note: '한식']
  JAPANESE [note: '일식']
  CHINESE [note: '중식']
  WESTERN [note: '양식']
  CAFE_DESSERT [note: '커피·디저트']
  PUB_BAR [note: '펍·와인·술집']
  OTHER [note: '기타']
}

Enum companion_type {
  DATE [note: '데이트']
  FAMILY [note: '가족과 함께']
  CHILDREN [note: '아이와 함께']
  SOLO [note: '혼밥']
  GROUP [note: '단체']
  DOG [note: '반려견과 함께']
}

Enum pick_status {
  SELECTED
  REVIEWED
  CANCELED
}

Enum exclusion_reason {
  DISTANCE_TOO_FAR
  PRICE_TOO_HIGH
  MENU_UNSATISFACTORY
  ATMOSPHERE_MISMATCH
  WANT_DIFFERENT
}

Enum data_provider {
  GOOGLE
  CURATED
}

// ───────── Member ─────────
Table members {
  id bigint [pk, increment]
  email varchar(255) [not null, unique]
  password varchar(255) [not null, note: 'BCrypt 해시']
  nickname varchar(10) [not null]
  login_provider varchar(20) [not null, default: 'LOCAL']
  bio varchar(150)
  profile_image_url varchar(500)
  default_region varchar(100)
  created_at timestamp [not null]
  updated_at timestamp [not null]
}

Table refresh_tokens {
  id bigint [pk, increment]
  member_id bigint [not null, ref: > members.id, note: '회원 삭제 시 함께 삭제']
  token_hash varchar(64) [not null, unique, note: '원문 토큰의 SHA-256 hex']
  expires_at timestamptz [not null]
  created_at timestamptz [not null, default: `CURRENT_TIMESTAMP`]

  indexes {
    member_id [name: 'refresh_tokens_member_id_idx']
  }
}

// ───────── Restaurant ─────────
Table restaurants {
  id bigint [pk, increment]
  data_provider data_provider [not null, default: 'GOOGLE']
  google_place_id varchar(255) [note: 'NULL이 아닐 때 unique (부분 인덱스)']
  name varchar(200) [not null]
  food_category food_category
  address varchar(500)
  latitude double [not null]
  longitude double [not null]
  location geography(Point_4326) [not null, note: 'GENERATED ALWAYS (lng, lat) STORED · GiST 인덱스']
  phone_number varchar(50)
  opening_hours_text text
  external_rating numeric(2,1) [note: '0~5']
  external_rating_count integer
  price_level varchar(30)
  price_range_start numeric(19,2)
  price_range_end numeric(19,2)
  price_currency_code varchar(3)
  representative_image_url varchar(1000)
  suitable_for_date boolean [note: 'true/false/NULL(미평가), 큐레이션 전용']
  suitable_for_family boolean
  suitable_for_children boolean
  suitable_for_solo boolean
  suitable_for_group boolean
  suitable_for_dogs boolean
  external_data_refreshed_at timestamptz
  created_at timestamptz [not null, default: `CURRENT_TIMESTAMP`]
  updated_at timestamptz [not null, default: `CURRENT_TIMESTAMP`]

  indexes {
    google_place_id [unique, name: 'restaurants_google_place_id_unique_idx']
    location [type: gist, name: 'restaurants_location_gist_idx']
  }
}

// ───────── Recommendation ─────────
Table recommendation_sessions {
  id bigint [pk, increment]
  member_id bigint [not null, ref: > members.id]
  companion_type companion_type [not null]
  latitude double [not null]
  longitude double [not null]
  price_range_min numeric(19,2) [note: 'NULL이면 가격 무관']
  price_range_max numeric(19,2)
  created_at timestamptz [not null, default: `CURRENT_TIMESTAMP`]

  indexes {
    member_id [name: 'recommendation_sessions_member_id_idx']
  }
}

Table recommendation_session_food_categories {
  session_id bigint [not null, ref: > recommendation_sessions.id]
  food_category food_category [not null]

  indexes {
    (session_id, food_category) [pk]
  }
}

Table recommendation_candidates {
  id bigint [pk, increment]
  session_id bigint [not null, ref: > recommendation_sessions.id]
  restaurant_id bigint [not null, ref: > restaurants.id]
  result_rank int [not null, note: '1~10 (1~5위 노출, 6~10위 대체 후보)']
  distance_meters double [not null, note: '0~5000']
  rating_contribution double [not null]
  distance_contribution double [not null]
  companion_bonus double [not null]
  total_score double [not null]

  indexes {
    (session_id, result_rank) [unique]
    (session_id, restaurant_id) [unique]
    restaurant_id [name: 'recommendation_candidates_restaurant_id_idx']
  }
}

Table recommendation_exclusions {
  id bigint [pk, increment]
  session_id bigint [not null, ref: > recommendation_sessions.id]
  restaurant_id bigint [not null, ref: > restaurants.id]
  reason exclusion_reason [not null]
  excluded_at timestamptz [not null, default: `CURRENT_TIMESTAMP`]

  indexes {
    (session_id, restaurant_id) [unique]
    session_id [name: 'recommendation_exclusions_session_id_idx']
  }
}

// ───────── Pick ─────────
Table picks {
  id bigint [pk, increment]
  member_id bigint [not null, ref: > members.id]
  restaurant_id bigint [not null, ref: > restaurants.id]
  recommendation_session_id bigint [not null, unique, ref: - recommendation_sessions.id, note: '세션당 Pick 1개']
  companion_type companion_type [not null, note: '선택 당시 동행 유형 스냅샷']
  status pick_status [not null, default: 'SELECTED']
  selected_at timestamptz [not null, default: `CURRENT_TIMESTAMP`]
  visited_at timestamptz
  updated_at timestamptz [not null, default: `CURRENT_TIMESTAMP`]

  indexes {
    (member_id, selected_at, id) [name: 'picks_member_selected_idx']
    restaurant_id [name: 'picks_restaurant_id_idx']
    (member_id, status) [name: 'picks_member_status_idx']
  }
}

// ───────── Discovery ─────────
Table discovery_spots {
  id bigint [pk, increment]
  region_code varchar(30) [not null, unique, note: 'SINSA, HYEHWA, SEOCHEON, HANNAM, JONGNO']
  region_name varchar(50) [not null]
  display_order integer [not null, unique]
  created_at timestamptz [not null, default: `CURRENT_TIMESTAMP`]
  updated_at timestamptz [not null, default: `CURRENT_TIMESTAMP`]
}

Table discovery_spot_restaurants {
  id bigint [pk, increment]
  discovery_spot_id bigint [not null, ref: > discovery_spots.id]
  restaurant_id bigint [not null, ref: > restaurants.id]
  display_order integer [not null, note: '1~5']
  one_line_intro varchar(200) [not null]
  created_at timestamptz [not null, default: `CURRENT_TIMESTAMP`]

  indexes {
    (discovery_spot_id, restaurant_id) [unique]
    (discovery_spot_id, display_order) [unique]
    restaurant_id [name: 'discovery_spot_restaurants_restaurant_idx']
  }
}
```
