-- 고객 이름으로 검색 (최대 10건)
SELECT id, name, email, phone
FROM customers
WHERE name LIKE :name
FETCH FIRST 10 ROWS ONLY;

-- 고객 ID로 상세 조회
SELECT id, name, email, phone, address, TO_CHAR(created_at) AS created_at
FROM customers
WHERE id = :customer_id;
