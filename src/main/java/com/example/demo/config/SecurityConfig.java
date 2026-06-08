package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // Cục băm mật khẩu chuẩn mã hóa BCrypt
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Vẫn tắt CSRF bảo mật trình duyệt để làm việc với Postman không bị chặn vô cớ
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        // 1. CỔNG CÔNG KHAI (PUBLIC)
                        // Cho phép truy cập không cần token: Đăng nhập, đăng ký, tra cứu thông tin chi nhánh ngân hàng
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/branches/**").permitAll()

                        // 2. CỔNG SẢN PHẨM & CHÍNH SÁCH (MANAGER ONLY)
                        // Chỉ Quản lý mới được tạo/sửa/xóa các gói sản phẩm vay (loan_products)
                        .requestMatchers("/api/loan-products/**").hasRole("MANAGER")

                        // 3. CỔNG TIẾP NHẬN & QUẢN LÝ TÀI SẢN ĐẢM BẢO (STAFF ONLY)
                        // Chỉ nhân viên nghiệp vụ mới được quyền thẩm định, thêm/sửa thông tin tài sản thế chấp (collaterals)
                        .requestMatchers("/api/collaterals/**").hasRole("STAFF")

                        // 4. CỔNG XỬ LÝ HỒ SƠ TÍN DỤNG (STAFF & MANAGER)
                        // Nhân viên tạo hồ sơ, Quản lý duyệt hồ sơ vay (loans).
                        // Khách hàng không được gọi trực tiếp cổng quản lý chung này mà có cổng riêng.
                        .requestMatchers("/api/loans/**").hasAnyRole("STAFF", "MANAGER")
                        .requestMatchers("/api/repayment-schedules/**").hasAnyRole("STAFF", "MANAGER")
                        .requestMatchers("/api/payments/**").hasAnyRole("STAFF", "MANAGER")

                        // 5. CỔNG RIÊNG DÀNH CHO KHÁCH HÀNG (CUSTOMER PORTAL)
                        // Khách hàng muốn xem dữ liệu phải thông qua các URL riêng biệt đã được gom cụm bảo mật
                        .requestMatchers("/api/my-loans/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/my-schedules/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/my-payments/**").hasRole("CUSTOMER")

                        // 6. CHỐT CHẶN CUỐI CÙNG
                        // Bất kỳ Request nào khác không nằm trong danh sách trên đều phải đăng nhập thành công
                        .anyRequest().authenticated()
                )

                // 3. Sử dụng cấu hình Xác thực cơ bản (Username/Password gửi kèm HTTP Header) để test nhanh trên Postman
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}