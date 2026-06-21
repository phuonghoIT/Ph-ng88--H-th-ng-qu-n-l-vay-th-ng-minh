SELECT * from loans;

SELECT public.create_loan_and_generate_schedules(
    2,          -- p_customer_id
    8,          -- p_employee_id
    1,          -- p_loan_product_id
    50000000,   -- p_amount
    (CURRENT_DATE - INTERVAL '3 months')::date, -- p_loan_date (SỬA LỖI)
    'UNSECURED' -- p_loan_type (Sửa lỗi chính tả từ UNSERCURED)
);


-- Duyệt một khoản vay đang ở trạng thái 'PENDING' sang 'ACTIVE' (hành động hợp lệ)
UPDATE loans
SET status = 'ACTIVE'
WHERE loan_id = 1;


-- Cố gắng xóa khoản vay có ID là 1 (hành động sẽ bị chặn)
DELETE FROM loans WHERE loan_id = 1;