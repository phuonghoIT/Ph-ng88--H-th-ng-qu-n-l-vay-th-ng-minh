package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "customers")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Customers {

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
}