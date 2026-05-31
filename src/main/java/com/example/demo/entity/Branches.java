package com.example.demo.entity; // Thay bằng package chuẩn của em

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "branches")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Branches {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "branch_name", nullable = false)
    private String branchName;

    @Column(name = "address")
    private String address;

    @Column(name = "phone")
    private String phone;



}