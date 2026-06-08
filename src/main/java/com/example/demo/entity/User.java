package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password", nullable = false, length = 255)
    private String password; // Nơi lưu chuỗi mật khẩu đã băm (BCrypt)

    @Column(name = "role", nullable = false, length = 20)
    private String role; // 'CUSTOMER', 'STAFF', 'MANAGER'

    // Liên kết sang bảng Customers (Khách hàng) - có thể null nếu user là nhân viên
    @OneToOne
    @JoinColumn(name = "customer_id", referencedColumnName = "customer_id", nullable = true)
    private Customer customer;

    // Liên kết sang bảng Employees (Nhân viên) - có thể null nếu user là khách hàng
    @OneToOne
    @JoinColumn(name = "employee_id", referencedColumnName = "employee_id", nullable = true)
    @JsonIgnoreProperties("user")
    private Employee employee;
}