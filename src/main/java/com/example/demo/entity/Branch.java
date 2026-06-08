package com.example.demo.entity; // Thay bằng package chuẩn của em

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "branches")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "branch_id")
    private Long branchId;
    @NotBlank(message = "Tên chi nhánh không được để trống!")
    @Column(name = "branch_name", nullable = false)
    private String branchName;
    @NotBlank(message = "Địa chỉ chi nhánh không được để trống!")
    @Column(name = "address")
    private String address;
    @NotBlank(message = "Số điện thoại chi nhánh không được để trống!")
    @Column(name = "phone")
    private String phone;



}