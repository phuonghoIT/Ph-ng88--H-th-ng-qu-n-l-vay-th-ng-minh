-- Hàm này sẽ là "API" để tạo khoản vay và sinh lịch trả nợ trong DB mới
CREATE OR REPLACE FUNCTION public.create_loan_and_generate_schedules(
    p_customer_id BIGINT, -- 👈 Đầu vào quan trọng, thay thế cho SecurityContextHolder
    p_employee_id BIGINT,
    p_loan_product_id BIGINT,
    p_amount NUMERIC,
    p_loan_date DATE,
    p_loan_type VARCHAR
)
RETURNS BIGINT -- Trả về ID của khoản vay vừa được tạo
AS $$
DECLARE
    v_loan_id BIGINT;
    v_duration INT;
    v_interest_rate NUMERIC;
    v_principal_per_month NUMERIC;
    v_monthly_rate NUMERIC;
    v_remaining NUMERIC;
    v_interest_amount NUMERIC;
    v_current_principal NUMERIC;
BEGIN
    -- =================================================================
    -- TRIGGER SỐ 1 & 2: Xác thực và Ghi đè Customer ID
    -- Trong SQL, bước này được thực hiện bằng cách bắt buộc người gọi
    -- phải truyền vào p_customer_id. Chúng ta tin tưởng vào tham số này.
    -- =================================================================

    -- Bước 1: Insert vào bảng loans trước để lấy ID
    INSERT INTO loans (customer_id, employee_id, loan_product_id, amount, loan_date, loan_type, status)
    VALUES (
        p_customer_id,
        p_employee_id,
        p_loan_product_id,
        p_amount,
        p_loan_date,
        p_loan_type,
        'PENDING' -- Trạng thái mặc định
    )
    RETURNING loan_id INTO v_loan_id; -- Lấy ID vừa được sinh ra

    -- =================================================================
    -- TRIGGER SỐ 3: TỰ ĐỘNG SINH LỊCH TRẢ NỢ (AFTER INSERT)
    -- =================================================================

    -- Lấy thông tin từ gói vay (loan_products) để tính toán
    SELECT duration_months, interest_rate INTO v_duration, v_interest_rate
    FROM loan_products WHERE loan_product_id = p_loan_product_id;

    -- Chuẩn bị các biến tính toán
    v_remaining := p_amount;
    v_principal_per_month := ROUND(p_amount / v_duration, 2);
    v_monthly_rate := ROUND((v_interest_rate / 100) / 12, 6);

    -- Vòng lặp FOR trong SQL để sinh ra các kỳ trả nợ
    FOR i IN 1..v_duration LOOP
        -- Tính lãi dựa trên dư nợ còn lại
        v_interest_amount := ROUND(v_remaining * v_monthly_rate, 2);
        
        -- Xử lý làm tròn cho kỳ cuối cùng
        IF i = v_duration THEN
            v_current_principal := v_remaining;
        ELSE
            v_current_principal := v_principal_per_month;
        END IF;

        -- Insert vào bảng lịch trả nợ
        INSERT INTO repayment_schedules (
            loan_id, period_number, due_date, principal_amount, 
            interest_amount, penalty_amount, status
        ) VALUES (
            v_loan_id,                                  -- Khóa ngoại trỏ về khoản vay vừa tạo
            i,                                          -- Số thứ tự kỳ
            p_loan_date + (i || ' month')::interval,    -- Ngày đáo hạn = ngày vay + số tháng
            v_current_principal,                        -- Tiền gốc phải trả
            v_interest_amount,                          -- Tiền lãi phải trả
            0,                                          -- Tiền phạt ban đầu = 0
            'UNPAID'                                    -- Trạng thái ban đầu
        );

        -- Cập nhật lại dư nợ gốc
        v_remaining := v_remaining - v_current_principal;
    END LOOP;

    -- Trả về ID của khoản vay mới
    RETURN v_loan_id;
END;
$$ LANGUAGE plpgsql;



-- Tạo một khoản vay 50,000,000 cho khách hàng có ID = 1
SELECT public.create_loan_and_generate_schedules(
    1,          -- p_customer_id: Khách hàng đang đăng nhập
    1,          -- p_employee_id: Nhân viên quản lý
    1,          -- p_loan_product_id: Gói vay áp dụng
    50000000,   -- p_amount: Số tiền vay
    '2024-01-01', -- p_loan_date: Ngày vay
    'TIN_CHAP'  -- p_loan_type: Loại vay
);