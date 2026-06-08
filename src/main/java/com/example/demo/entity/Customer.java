package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "customers")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "full_name", nullable = false)
    private String fullName; // Tên [cite: 7]

    @Column(name = "address")
    private String address; // Địa chỉ

    @Column(name = "sdt")
    private String sdt; // SĐT

    @Column(name = "identity_number", nullable = false, unique = true)
    private String identityNumber; // Căn cước công dân

    @Column(name = "credit_score")
    private int creditScore; // Mức độ tín dụng [cite: 10]

    @Column(name = "job")
    private String job; // Nghề nghiệp

    // Bên trong file Customers.java
    @OneToOne(mappedBy = "customer", cascade = CascadeType.ALL)
    @JsonIgnoreProperties("customer")// CascadeType.ALL cực kỳ quan trọng ở đây!
    private User user;

    // 🌟 SỬA LỖI: Thêm mối quan hệ Một-Nhiều sang Khoản vay
    @OneToMany(mappedBy = "customer")
    @JsonIgnore // 👈 Ngắt vòng lặp JSON tại đây
    private List<Loan> loans;
}
