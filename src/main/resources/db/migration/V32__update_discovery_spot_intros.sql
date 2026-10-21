-- 탐색 스팟 운영자 한줄평 갱신 (2026-10-21 팀 전달 문구).
-- 한줄평 노출 순서는 최신 공개 사용자 후기 → 이 운영자 한줄평 → "후기가 없습니다."다.
-- 식당명은 운영 DB 값(Google 갱신으로 바뀐 이름 포함)을 그대로 두고, 지역 코드·노출 순서로 행을 찾는다.
-- 신사 4위 영동설렁탕, 5위 멘쇼쿠는 전달 문구가 "pick 기록 입력 완료"라 한줄평을 바꾸지 않는다.

UPDATE discovery_spot_restaurants dsr
SET one_line_intro = seed.one_line_intro
FROM discovery_spots ds,
     (VALUES
         ('SINSA', 1, '프라이빗한 공간에서 맛있는 양고기 세트'),
         ('SINSA', 2, '육즙 가득한 정통 중식 만두집'),
         ('SINSA', 3, '압구정의 퀄리티 좋은 음식을 내는 화상 노포 중식당'),
         ('HYEHWA', 1, '주말만 여는 메뉴가 매일 바뀌는 파스타집'),
         ('HYEHWA', 2, '진한 국물과 쫄깃한 면발의 국시'),
         ('HYEHWA', 3, '시간이 머무는 앤틱 감성 카페'),
         ('HYEHWA', 4, '바삭촉촉 정통 일식 돈까스'),
         ('HYEHWA', 5, '화덕에서 구워낸 쫄깃한 피자들'),
         ('SEOCHON', 1, '흑백요리사 임태훈 셰프의 중식당'),
         ('SEOCHON', 2, '여유롭게 커피마시기 좋은 공간'),
         ('SEOCHON', 3, '서촌의 맛있는 안주들이 있는 한식주점'),
         ('SEOCHON', 4, '아인슈페너가 맛있는 청와대 근처 카페'),
         ('SEOCHON', 5, '서촌의 삼겹살 맛집'),
         ('HANNAM', 1, '프라이빗한 공간에서 즐기는 중식 코스'),
         ('HANNAM', 2, '감각적인 타코 전문 식당'),
         ('HANNAM', 3, '부대찌개와는 다른 존슨탕의 매력'),
         ('HANNAM', 4, '성시경 먹을텐데에 나온 짜장떡볶이가 맛있는 분식집'),
         ('HANNAM', 5, '감각적인 공간에서 마시는 커피 한잔'),
         ('JONGNO', 1, '닭한마리가 맛있는 종로 맛집'),
         ('JONGNO', 2, '종로5가의 닭도리탕 전문 노포'),
         ('JONGNO', 3, '세운상가 지하 상가에 위치해 있는 정갈한 백반 한상'),
         ('JONGNO', 4, '북촌의 화덕으로 구운 맛있는 피자'),
         ('JONGNO', 5, '종로의 퀄리티 좋은 돈카츠집')
     ) AS seed (region_code, display_order, one_line_intro)
WHERE ds.id = dsr.discovery_spot_id
  AND ds.region_code = seed.region_code
  AND dsr.display_order = seed.display_order;
