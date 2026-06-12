-- 상품명으로 검색 (최대 10건)
SELECT product_id, product_name, category, price, description
FROM products
WHERE product_name LIKE :name
FETCH FIRST 10 ROWS ONLY;

-- 상품 ID로 상세 조회
SELECT product_id, product_name, category, price, description, TO_CHAR(created_at) AS created_at
FROM products
WHERE product_id = :product_id;
