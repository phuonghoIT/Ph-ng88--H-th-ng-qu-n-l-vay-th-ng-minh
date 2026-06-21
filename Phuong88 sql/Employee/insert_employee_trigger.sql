-- Hàm này sẽ là "API" duy nhất để thêm nhân viên vào DB mới của bạn
CREATE OR REPLACE FUNCTION public.create_employee_and_account_plaintext(
    p_full_name VARCHAR,
    p_role VARCHAR,
    p_username VARCHAR,
    p_plain_password VARCHAR, -- Mật khẩu thô
    p_branch_id BIGINT
)
RETURNS BIGINT -- Trả về ID của nhân viên vừa được tạo
AS $$
DECLARE
    v_employee_id BIGINT;
BEGIN
    -- =================================================================
    -- TRIGGER SỐ 1: CHẶN TRÙNG USERNAME (BEFORE INSERT)
    -- =================================================================
    IF EXISTS (SELECT 1 FROM users WHERE username = p_username) THEN
        RAISE EXCEPTION 'LỖI DB: Username "%" đã tồn tại.', p_username;
    END IF;

    -- =================================================================
    -- BẮT ĐẦU THAO TÁC INSERT
    -- =================================================================
    
    -- Bước 1: Insert vào bảng employees trước để lấy ID
    -- "Trigger" gán trạng thái ACTIVE được thực hiện ngay tại đây
    INSERT INTO employees (full_name, role, status, branch_id)
    VALUES (
        p_full_name,
        p_role, 
        'ACTIVE', -- 👈 "Trigger" gán trạng thái mặc định
        p_branch_id
    )
    RETURNING employee_id INTO v_employee_id; -- Lấy ID vừa được sinh ra

    -- Bước 2: Insert vào bảng users với employee_id vừa có
    -- "Trigger" đồng bộ Role và lưu mật khẩu thô được thực hiện tại đây
    INSERT INTO users (username, password, role, employee_id, enabled)
    VALUES (
        p_username, 
        p_plain_password, -- 👈 Lưu thẳng mật khẩu thô
        p_role,           -- 👈 "Trigger" đồng bộ Role
        v_employee_id,
        true              -- Kích hoạt tài khoản
    );

    -- Trả về ID của nhân viên mới
    RETURN v_employee_id;
END;
$$ LANGUAGE plpgsql;



SELECT public.create_employee_and_account_plaintext(
    'Lê Thị A', 
    'lta@hfinance.com', 
    '0912345678', 
    'STAFF', 
    'lethia', 
    'password123',
    1 -- Branch ID
);