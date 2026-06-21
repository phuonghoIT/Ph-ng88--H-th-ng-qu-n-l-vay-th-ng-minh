-- Tên hàm: apply_overdue_penalties
-- Mục đích: Quét và xử lý các kỳ hạn đã quá hạn nhưng chưa được xử lý.
CREATE OR REPLACE FUNCTION apply_overdue_penalties()
RETURNS void AS $$
BEGIN
    -- ========================================================================
    -- BƯỚC 1: TRỪ ĐIỂM TÍN DỤNG (CHỈ MỘT LẦN DUY NHẤT)
    -- Tìm những khách hàng có kỳ hạn quá hạn đúng ngày hôm qua và vẫn UNPAID.
    -- ========================================================================
    UPDATE customers c
    SET credit_score = GREATEST(c.credit_score - 20, 0) -- GREATEST để điểm không bị âm
    WHERE c.customer_id IN (
        SELECT DISTINCT l.customer_id
        FROM repayment_schedules rs
        JOIN loans l ON rs.loan_id = l.loan_id
        WHERE rs.due_date = CURRENT_DATE - INTERVAL '1 day' -- Quá hạn đúng ngày hôm qua
          AND rs.status = 'UNPAID'
    );

    -- ========================================================================
    -- BƯỚC 2: CẬP NHẬT TRẠNG THÁI VÀ TÍNH PHẠT (MỘT LẦN DUY NHẤT)
    -- Tìm tất cả các kỳ hạn có due_date < hôm nay và status vẫn là 'UNPAID'.
    -- ========================================================================
    UPDATE repayment_schedules rs
    SET
        status = 'OVERDUE',
        -- Công thức tính phạt một lần: (Gốc + Lãi) * (Tỷ lệ phạt / 100)
        penalty_amount = (rs.principal_amount + rs.interest_amount) * (lp.penalty_rate / 100.0)
    FROM
        loans l,
        loan_products lp
    WHERE
        rs.loan_id = l.loan_id
        AND l.loan_product_id = lp.loan_product_id
        AND rs.due_date < CURRENT_DATE -- Ngày đáo hạn đã trôi qua
        AND rs.status = 'UNPAID';      -- Nhưng vẫn chưa trả (và chưa bị đổi thành OVERDUE)

END;
$$ LANGUAGE plpgsql;



-- 1. Cài đặt extension (chỉ cần chạy một lần, có thể cần quyền superuser)
CREATE EXTENSION IF NOT EXISTS pg_cron;

-- 2. Lập lịch để chạy hàm 'apply_overdue_penalties' vào lúc 00:05 mỗi ngày
-- '5 0 * * *' là cú pháp cron cho "5 phút sau nửa đêm, mỗi ngày"
SELECT cron.schedule(
    'daily-overdue-penalty-job',        -- Tên của công việc (để dễ quản lý)
    '10 seconds',                        -- Lịch trình (Cron syntax)
    'SELECT apply_overdue_penalties();' -- Lệnh SQL cần thực thi
);

-- Để xem lại các công việc đã lập lịch:
-- SELECT * FROM cron.job;

-- Để hủy lịch, bạn có thể dùng:
-- SELECT cron.unschedule('daily-overdue-penalty-job');