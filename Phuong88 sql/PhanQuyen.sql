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


GRANT EXECUTE ON FUNCTION create_customer_and_account(
    VARCHAR, -- p_full_name
    VARCHAR, -- p_address
    VARCHAR, -- p_sdt
    VARCHAR, -- p_identity_number
    VARCHAR, -- p_job
    VARCHAR, -- p_username
    VARCHAR  -- p_plain_password
) TO PUBLIC;


-- 2. Chỉ có vai trò STAFF và MANAGER mới được phép tạo tài khoản cho NHÂN VIÊN mới.
-- (Giả sử hàm tạo nhân viên của bạn nằm trong file Employee_trigger.sql và có tên là create_employee_and_account)
-- Lưu ý: Tôi sẽ cần đọc lại file Employee_trigger.sql để có chữ ký chính xác.
-- Giả sử chữ ký là:
GRANT EXECUTE ON FUNCTION create_employee_and_account(
    VARCHAR, -- p_full_name
    VARCHAR, -- p_role
    
    VARCHAR, -- p_username
    VARCHAR,  -- p_plain_password
    BIGINT  -- p_branch_id
) TO STAFF_ROLE;

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

GRANT SELECT, INSERT, UPDATE ON customers, loans, repayment_schedules, payments, collaterals, employees, users TO STAFF_ROLE;
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
-- Xóa policy cũ
DROP POLICY IF EXISTS customer_can_see_own_loans ON loans;

-- Tạo policy mới, gọi hàm an toàn
CREATE POLICY customer_can_see_own_loans
ON loans
FOR SELECT
TO CUSTOMER_ROLE
USING (
    -- Chỉ cần so sánh customer_id của khoản vay với kết quả của hàm
    customer_id = get_current_customer_id()
);



CREATE OR REPLACE FUNCTION get_current_customer_id()
RETURNS BIGINT
LANGUAGE plpgsql
SECURITY DEFINER -- 👈 Chạy với quyền của người tạo hàm (superuser)
AS $$
BEGIN
    -- Hàm này có thể đọc bảng 'users' một cách an toàn
    RETURN (
        SELECT customer_id
        FROM public.users -- Chỉ định rõ schema để tăng bảo mật
        WHERE username = current_setting('app.current_username', true) -- 'true' để không báo lỗi nếu biến chưa tồn tại
    );
END;
$$;

-- Cấp quyền cho các vai trò được phép gọi hàm này
GRANT EXECUTE ON FUNCTION get_current_customer_id() TO CUSTOMER_ROLE, STAFF_ROLE, MANAGER_ROLE;