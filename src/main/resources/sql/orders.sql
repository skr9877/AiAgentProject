-- 고객 ID로 주문 목록 조회 (최신순, 최대 20건)
SELECT order_id, product_id, product_name, quantity, total_price, status, TO_CHAR(order_date) AS order_date
FROM orders
WHERE customer_id = :customer_id
ORDER BY order_date DESC
FETCH FIRST 20 ROWS ONLY;

-- 주문 ID로 상세 조회 (상품명 JOIN)
SELECT o.order_id, o.customer_id, o.product_id, p.product_name,
       o.quantity, o.total_price, o.status, TO_CHAR(o.order_date) AS order_date, o.delivery_address
FROM orders o
JOIN products p ON o.product_id = p.product_id
WHERE o.order_id = :order_id;
