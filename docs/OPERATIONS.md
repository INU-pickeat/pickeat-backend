# 운영 작업

EC2에 접속한 뒤(`ssh ubuntu@api.pickeat.kr` 또는 AWS 콘솔 → EC2 → 연결 → EC2 Instance Connect) 실행한다.
DB 접속은 아래 한 줄로 연다.

```bash
sudo docker exec -it pickeat-postgres psql -U pick_eat -d pick_eat
```

## 식당 신고 검토

사용자가 `POST /api/v1/restaurants/{id}/reports`로 남긴 신고는 `PENDING`으로 쌓이고, 운영자가 확인하기 전에는 추천에 영향이 없다.
일주일에 한 번 정도 확인한다.

### 1. 검토할 신고 보기

식당별로 묶어 신고가 많은 순서로 본다.

```sql
SELECT r.id AS restaurant_id, r.name, r.address, r.google_place_id,
       COUNT(*) AS reports,
       STRING_AGG(DISTINCT rr.reason, ',') AS reasons,
       STRING_AGG(rr.detail, ' / ') FILTER (WHERE rr.detail IS NOT NULL) AS details,
       MIN(rr.created_at) AS first_reported_at
FROM restaurant_reports rr
JOIN restaurants r ON r.id = rr.restaurant_id
WHERE rr.status = 'PENDING'
GROUP BY r.id
ORDER BY reports DESC, first_reported_at;
```

네이버·카카오 지도나 Google 지도(`https://www.google.com/maps/place/?q=place_id:<google_place_id>`)에서 실제 상태를 확인한다.

### 2-a. 폐업 등이 맞으면: 추천에서 빼고 신고를 받아들인다

```sql
BEGIN;
UPDATE restaurants
SET excluded_at = now(), excluded_reason = '폐업 확인 (2026-10-08 신고 검토)'
WHERE id = :restaurant_id;

UPDATE restaurant_reports
SET status = 'ACCEPTED', reviewed_at = now()
WHERE restaurant_id = :restaurant_id AND status = 'PENDING';
COMMIT;
```

`excluded_at`이 찍힌 식당은 추천 후보 조회(`Restaurant.findWithinRadius`)에서 빠진다. Google 보충으로 같은 Place ID가 다시 들어와도 기존 행이 갱신될 뿐 `excluded_at`은 그대로라 다시 추천되지 않는다.
식당 상세·Pick 기록·후기는 그대로 보인다.

정보 오류(`WRONG_INFO`·`WRONG_CATEGORY`)는 빼지 않고 값을 고친 뒤 `ACCEPTED`로 닫아도 된다.

```sql
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE id = :restaurant_id;
```

### 2-b. 사실이 아니면: 신고만 닫는다

```sql
UPDATE restaurant_reports
SET status = 'REJECTED', reviewed_at = now()
WHERE restaurant_id = :restaurant_id AND status = 'PENDING';
```

### 되돌리기 (다시 영업하는 경우)

```sql
UPDATE restaurants SET excluded_at = NULL, excluded_reason = NULL WHERE id = :restaurant_id;
```

### 지금 빠져 있는 식당 보기

```sql
SELECT id, name, address, excluded_at, excluded_reason
FROM restaurants
WHERE excluded_at IS NOT NULL
ORDER BY excluded_at DESC;
```
