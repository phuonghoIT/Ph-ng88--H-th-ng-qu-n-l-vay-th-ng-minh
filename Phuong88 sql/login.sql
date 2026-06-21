CREATE OR REPLACE FUNCTION authenticate_user_simple(
    p_username VARCHAR,
    p_plain_password VARCHAR
)
RETURNS TABLE(role VARCHAR, message TEXT) AS $$
DECLARE
    v_user RECORD;
BEGIN
    -- Tìm người dùng trong bảng users
    SELECT * INTO v_user FROM users WHERE username = p_username;

    -- Nếu không tìm thấy user, trả về lỗi
    IF NOT FOUND THEN
        RETURN QUERY SELECT NULL::VARCHAR, 'Lỗi: Tên đăng nhập không tồn tại.';
        RETURN;
    END IF;

    -- So sánh mật khẩu thô (KHÔNG AN TOÀN!)
    IF v_user.password = p_plain_password THEN
        -- Nếu thành công, trả về vai trò của người dùng
        RETURN QUERY SELECT v_user.role, 'Đăng nhập thành công!';
    ELSE
        -- Nếu sai mật khẩu, trả về lỗi
        RETURN QUERY SELECT NULL::VARCHAR, 'Lỗi: Mật khẩu không chính xác.';
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Kịch bản để phân quyền sẽ như thế này



    -- Áp dụng vai trò 'CUSTOMER_ROLE' cho session này
	    -- Bước 1: Xác thực người dùng 'nguyenvana'
    SELECT role FROM authenticate_user_simple('nguyenvana', 'matkhaucuakhach');
    
    SET ROLE CUSTOMER_ROLE;

    -- Cho PostgreSQL biết rằng người dùng của session này là 'nguyenvana'
    -- Lệnh này cực kỳ quan trọng để RLS hoạt động
    SET SESSION AUTHORIZATION 'nguyenvana';

	    RESET ROLE;
    RESET SESSION AUTHORIZATION;
    
    
