-- Hàm này sẽ là "API" ở tầng DB để đăng ký một khách hàng mới kèm tài khoản đăng nhập.
-- Nó đóng gói toàn bộ nghiệp vụ, tương tự như create_employee_and_account.

CREATE OR REPLACE FUNCTION create_customer_and_account(
    p_full_name VARCHAR,
    p_address VARCHAR,
    p_sdt VARCHAR,
    p_identity_number VARCHAR,
    p_job VARCHAR,
    p_username VARCHAR,
    p_plain_password VARCHAR -- Mật khẩu dạng văn bản thô
)
RETURNS BIGINT -- Trả về ID của khách hàng vừa được tạo
AS $$
DECLARE
    v_customer_id BIGINT;
BEGIN
    -- =================================================================
    -- QUY TẮC 1: KIỂM TRA SỰ TRÙNG LẶP TRƯỚC KHI INSERT
    -- =================================================================

    -- Kiểm tra username đã tồn tại trong bảng users chưa
    IF EXISTS (SELECT 1 FROM users WHERE username = p_username) THEN
        RAISE EXCEPTION 'LỖI DB: Tên đăng nhập (username) "%" đã tồn tại.', p_username;
    END IF;

    -- Kiểm tra số CCCD đã tồn tại trong bảng customers chưa
    IF EXISTS (SELECT 1 FROM customers WHERE identity_number = p_identity_number) THEN
        RAISE EXCEPTION 'LỖI DB: Số CCCD (identity_number) "%" đã tồn tại.', p_identity_number;
    END IF;

    -- =================================================================
    -- BẮT ĐẦU THAO TÁC INSERT (được bao bọc trong một giao dịch)
    -- =================================================================

    -- Bước 1: Insert vào bảng customers trước để lấy ID.
    -- Điểm tín dụng mặc định là 600 cho khách hàng mới.
    INSERT INTO customers (full_name, address, sdt, identity_number, job, credit_score)
    VALUES (
        p_full_name,
        p_address,
        p_sdt,
        p_identity_number,
        p_job,
        600 -- Giá trị mặc định
    )
    RETURNING customer_id INTO v_customer_id; -- Lấy ID vừa được sinh ra

    -- Bước 2: Insert vào bảng users với customer_id vừa có.
    -- Vai trò mặc định là 'CUSTOMER'.
    INSERT INTO users (username, password, role, customer_id)
    VALUES (
        p_username,
        p_plain_password, -- LƯU Ý: Đây là mật khẩu thô cho mục đích demo
        'CUSTOMER',       -- Vai trò mặc định
        v_customer_id
    );

    -- Trả về ID của khách hàng mới để ứng dụng có thể sử dụng nếu cần
    RETURN v_customer_id;
END;
$$ LANGUAGE plpgsql;
