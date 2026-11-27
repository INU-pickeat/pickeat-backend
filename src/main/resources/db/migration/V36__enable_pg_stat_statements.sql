-- 관리자 운영 상태의 느린 쿼리 TOP 10용. 통계를 실제로 모으려면 서버가 shared_preload_libraries에
-- pg_stat_statements를 올려야 한다(deploy/ec2/bootstrap.sh). 안 올라가 있으면 화면에 "꺼짐"으로 보인다.
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
