-- 서촌 지역 코드를 로마자 표기법(촌=chon)에 맞춰 SEOCHEON에서 SEOCHON으로 바꾼다. 프론트가 SEOCHON으로 매핑한다.
UPDATE discovery_spots
SET region_code = 'SEOCHON',
    updated_at = CURRENT_TIMESTAMP
WHERE region_code = 'SEOCHEON';

UPDATE restaurants
SET representative_image_url = REPLACE(representative_image_url, '/images/discovery/seocheon_', '/images/discovery/seochon_')
WHERE representative_image_url LIKE '/images/discovery/seocheon\_%';
