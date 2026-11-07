-- 演示数据优化：把销量放大到更有观感的量级（百份以上），并补齐所有在售菜品的销售记录
UPDATE dish_sales
SET amount = amount * 80,
    quantity = quantity * 80
WHERE quantity < 50;

INSERT INTO dish_sales (stall_id, dish_id, dish_name, sale_date, quantity, amount)
SELECT d.stall_id,
       d.id,
       d.name,
       DATE_SUB(CURDATE(), INTERVAL MOD(d.id * 7, 60) DAY),
       200 + MOD(d.id * 97 + 37, 800),
       (200 + MOD(d.id * 97 + 37, 800)) * d.price
FROM dish d
WHERE d.deleted = 0
  AND d.status = 1
  AND NOT EXISTS (
      SELECT 1 FROM dish_sales ds WHERE ds.dish_id = d.id
  );
