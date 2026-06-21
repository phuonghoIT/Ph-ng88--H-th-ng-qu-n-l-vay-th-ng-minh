-- Xóa các bảng cũ theo thứ tự ngược lại để tránh lỗi khóa ngoại (nếu cần chạy lại)
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS collaterals; -- Thêm collateral vào danh sách drop
DROP TABLE IF EXISTS repayment_schedules;
DROP TABLE IF EXISTS loans;
DROP TABLE IF EXISTS loan_products;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS employees;
DROP TABLE IF EXISTS customers;
DROP TABLE IF EXISTS branches;

-- Xóa các kiểu ENUM cũ (nếu cần chạy lại)
DROP TYPE IF EXISTS loan_type;
DROP TYPE IF EXISTS loan_status;
DROP TYPE IF EXISTS schedule_status;
DROP TYPE IF EXISTS employee_status;

-- ====================================================================
-- 1. ĐỊNH NGHĨA CÁC KIỂU DỮ LIỆU ENUM
-- ====================================================================

CREATE TYPE loan_type AS ENUM ('SECURED', 'UNSECURED'); -- Tín chấp, Thế chấp
CREATE TYPE loan_status AS ENUM ('PENDING', 'ACTIVE', 'PAID', 'DEFAULTED', 'OVERDUE');
CREATE TYPE schedule_status AS ENUM ('UNPAID', 'PAID', 'OVERDUE');
CREATE TYPE employee_status AS ENUM ('ACTIVE', 'INACTIVE');

-- ====================================================================
-- 2. TẠO CÁC BẢNG KHÔNG PHỤ THUỘC HOẶC ÍT PHỤ THUỘC TRƯỚC
-- ====================================================================

-- Bảng Chi nhánh (branches)
CREATE TABLE branches (
    branch_id BIGSERIAL PRIMARY KEY,
    branch_name VARCHAR(255) NOT NULL,
    address VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL
);

-- Bảng Khách hàng (customers)
CREATE TABLE customers (
    customer_id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    address VARCHAR(255),
    sdt VARCHAR(20),
    identity_number VARCHAR(50) NOT NULL UNIQUE,
    credit_score INT DEFAULT 600,
    job VARCHAR(255)
);

-- Bảng Sản phẩm vay (loan_products)
CREATE TABLE loan_products (
    loan_product_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    interest_rate NUMERIC(5, 4) NOT NULL,
    penalty_rate NUMERIC(5, 4) NOT NULL,
    duration_months INT NOT NULL
);

-- ====================================================================
-- 3. TẠO CÁC BẢNG CÓ KHÓA NGOẠI
-- ====================================================================

-- Bảng Nhân viên (employees)
CREATE TABLE employees (
    employee_id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL, -- 'STAFF', 'MANAGER'
    status employee_status NOT NULL DEFAULT 'ACTIVE',
    branch_id BIGINT NOT NULL,
    CONSTRAINT fk_employee_branch FOREIGN KEY (branch_id) REFERENCES branches(branch_id) ON DELETE RESTRICT
);

-- Bảng Người dùng hệ thống (users)
CREATE TABLE users (
    user_id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    customer_id BIGINT UNIQUE,
    employee_id BIGINT UNIQUE,
    CONSTRAINT fk_user_customer FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE SET NULL,
    CONSTRAINT fk_user_employee FOREIGN KEY (employee_id) REFERENCES employees(employee_id) ON DELETE SET NULL
);

-- Bảng Khoản vay (loans)
CREATE TABLE loans (
    loan_id BIGSERIAL PRIMARY KEY,
    amount NUMERIC(15, 2) NOT NULL,
    loan_date DATE NOT NULL,
    loan_type loan_type NOT NULL,
    status loan_status NOT NULL DEFAULT 'PENDING',
    customer_id BIGINT NOT NULL,
    employee_id BIGINT,
    loan_product_id BIGINT NOT NULL,
    CONSTRAINT fk_loan_customer FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE RESTRICT,
    CONSTRAINT fk_loan_employee FOREIGN KEY (employee_id) REFERENCES employees(employee_id) ON DELETE SET NULL,
    CONSTRAINT fk_loan_product FOREIGN KEY (loan_product_id) REFERENCES loan_products(loan_product_id) ON DELETE RESTRICT
);

-- Bảng Lịch trả nợ (repayment_schedules)
CREATE TABLE repayment_schedules (
    schedule_id BIGSERIAL PRIMARY KEY,
    period_number INT NOT NULL,
    due_date DATE NOT NULL,
    principal_amount NUMERIC(15, 2) NOT NULL,
    interest_amount NUMERIC(15, 2) NOT NULL,
    penalty_amount NUMERIC(15, 2) DEFAULT 0,
    status schedule_status NOT NULL DEFAULT 'UNPAID',
    loan_id BIGINT NOT NULL,
    CONSTRAINT fk_schedule_loan FOREIGN KEY (loan_id) REFERENCES loans(loan_id) ON DELETE CASCADE
);

-- Bảng Thanh toán (payments)
CREATE TABLE payments (
    payment_id BIGSERIAL PRIMARY KEY,
    amount NUMERIC(15, 2) NOT NULL,
    payment_date DATE NOT NULL,
    payment_method VARCHAR(50),
    repayment_schedule_id BIGINT NOT NULL, -- SỬA LỖI TÊN CỘT
    employee_id BIGINT NOT NULL,
    CONSTRAINT fk_payment_schedule FOREIGN KEY (repayment_schedule_id) REFERENCES repayment_schedules(schedule_id) ON DELETE RESTRICT,
    CONSTRAINT fk_payment_employee FOREIGN KEY (employee_id) REFERENCES employees(employee_id) ON DELETE RESTRICT
);

-- Bảng Tài sản thế chấp (collaterals) - BỔ SUNG BẢNG
CREATE TABLE collaterals (
    collateral_id BIGSERIAL PRIMARY KEY,
    asset_type VARCHAR(255) NOT NULL,
    estimated_value NUMERIC(15, 2) NOT NULL,
    conversion_rate NUMERIC(5, 4),
    loan_id BIGINT NOT NULL UNIQUE,
    CONSTRAINT fk_collateral_loan FOREIGN KEY (loan_id) REFERENCES loans(loan_id) ON DELETE CASCADE
);

-- ====================================================================
-- 4. THÊM CÁC RÀNG BUỘC UNIQUE VÀ CHECK ĐỂ TĂNG CƯỜNG TÍNH TOÀN VẸN
-- ====================================================================

-- Thêm một ràng buộc UNIQUE để đảm bảo không có 2 kỳ hạn trùng số trong cùng 1 khoản vay
ALTER TABLE repayment_schedules
ADD CONSTRAINT unique_loan_period UNIQUE (loan_id, period_number);

-- BỔ SUNG CÁC RÀNG BUỘC CHECK
ALTER TABLE loans
ADD CONSTRAINT chk_loan_amount_positive CHECK (amount > 0);

ALTER TABLE payments
ADD CONSTRAINT chk_payment_amount_positive CHECK (amount > 0);

ALTER TABLE loan_products
ADD CONSTRAINT chk_product_values_positive CHECK (interest_rate > 0 AND penalty_rate >= 0 AND duration_months > 0);

ALTER TABLE collaterals
ADD CONSTRAINT chk_collateral_value_positive CHECK (estimated_value > 0);
