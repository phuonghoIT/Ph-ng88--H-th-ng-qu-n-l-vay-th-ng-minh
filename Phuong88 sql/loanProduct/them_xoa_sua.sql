SELECT *
From loan_products;

-- Thêm một gói vay mới với lãi suất 12%/năm, phạt 25%, thời hạn 24 tháng
INSERT INTO loan_products (name, interest_rate, penalty_rate, duration_months)
VALUES ('Vay mua nhà 2 năm', 0.12, 0.25, 24);


-- Cố gắng thay đổi lãi suất của gói vay có ID là 1
UPDATE loan_products
SET interest_rate = 0.18
WHERE loan_product_id = 1;


-- Cố gắng xóa gói vay có ID là 1
DELETE FROM loan_products WHERE loan_product_id = 1;