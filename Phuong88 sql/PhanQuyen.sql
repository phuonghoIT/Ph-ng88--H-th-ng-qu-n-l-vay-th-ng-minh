-- Xóa các role cũ nếu cần chạy lại
DROP ROLE IF EXISTS CUSTOMER_ROLE;
DROP ROLE IF EXISTS STAFF_ROLE;
DROP ROLE IF EXISTS MANAGER_ROLE;

-- Tạo các vai trò cơ bản
CREATE ROLE CUSTOMER_ROLE;
CREATE ROLE STAFF_ROLE;
CREATE ROLE MANAGER_ROLE;

-- Thiết lập kế thừa: MANAGER có tất cả các quyền của STAFF
GRANT STAFF_ROLE TO MANAGER_ROLE;


-- =================================================================
-- QUYỀN CỦA KHÁCH HÀNG (CUSTOMER_ROLE)
-- =================================================================
-- Chỉ được xem thông tin của chính mình (sẽ được RLS siết lại sau)
GRANT SELECT ON customers, loans, repayment_schedules, payments TO CUSTOMER_ROLE;
-- Được phép tạo khoản vay mới và thực hiện thanh toán
GRANT INSERT ON loans, payments TO CUSTOMER_ROLE;
-- Cần quyền sử dụng các sequence để tạo ID mới
GRANT USAGE, SELECT ON SEQUENCE loans_loan_id_seq, payments_payment_id_seq TO CUSTOMER_ROLE;


-- =================================================================
-- QUYỀN CỦA NHÂN VIÊN (STAFF_ROLE)
-- =================================================================
-- Được xem, thêm, sửa trên hầu hết các bảng nghiệp vụ
GRANT SELECT, INSERT, UPDATE ON customers, loans, repayment_schedules, payments, collaterals TO STAFF_ROLE;
-- Được xem thông tin nhân viên và chi nhánh
GRANT SELECT ON employees, branches TO STAFF_ROLE;
-- Được sử dụng tất cả các sequence
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO STAFF_ROLE;


-- =================================================================
-- QUYỀN CỦA QUẢN LÝ (MANAGER_ROLE)
-- =================================================================
-- Manager kế thừa tất cả quyền của STAFF.
-- Cấp thêm quyền đặc biệt: quyền gọi hàm sa thải nhân viên
GRANT EXECUTE ON FUNCTION fire_employee_and_reassign_loans(BIGINT) TO MANAGER_ROLE;
-- Cấp thêm quyền trên các bảng tham số hệ thống
GRANT SELECT, INSERT, UPDATE ON loan_products, branches TO MANAGER_ROLE;



-- 1. Kích hoạt RLS trên bảng 'loans'
ALTER TABLE loans ENABLE ROW LEVEL SECURITY;

-- 2. Tạo một "Chính sách" (Policy) cho vai trò CUSTOMER_ROLE
CREATE POLICY customer_can_see_own_loans
ON loans                            -- Áp dụng trên bảng 'loans'
FOR SELECT                         -- Chỉ cho hành động SELECT
TO CUSTOMER_ROLE                   -- Chỉ áp dụng cho vai trò CUSTOMER_ROLE
USING (                            -- Với điều kiện là:
    -- Cột customer_id trong bảng loans phải bằng với customer_id
    -- của người dùng đang thực hiện truy vấn.
    customer_id = (
        SELECT customer_id
        FROM users
        WHERE username = current_user -- current_user là biến đặc biệt của Postgres, trả về tên user của session hiện tại
    )
);