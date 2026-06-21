
SELECT *
from collaterals;

SELECT *
from loans;

SELECT public.create_loan_and_generate_schedules(
    2,          -- p_customer_id
    8,          -- p_employee_id
    1,          -- p_loan_product_id
    50000000,   -- p_amount
    (CURRENT_DATE - INTERVAL '3 months')::date, -- p_loan_date (SỬA LỖI)
    'SECURED' -- p_loan_type (Sửa lỗi chính tả từ UNSERCURED)
);

-- Thêm một tài sản là "Sổ đỏ nhà đất" cho khoản vay có ID là 2
INSERT INTO collaterals (asset_type, estimated_value, conversion_rate, loan_id)
VALUES ('Sổ đỏ nhà đất', 1500000000, 0.7, 7);

-- Cập nhật lại giá trị ước tính của tài sản (Giả sử khoản vay liên quan đang PENDING)
UPDATE collaterals
SET estimated_value = 1600000000
WHERE collateral_id = 3;

UPDATE loans
SET status = 'ACTIVE'
WHERE loan_id = 7;


-- Cố gắng xóa tài sản thế chấp có ID là 1 (Chỉ thành công nếu khoản vay đang PENDING)
DELETE FROM collaterals WHERE collateral_id = 3;


