package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "collaterals")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Collateral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "collateral_id")
    private Long collateralId;

    @Column(name = "asset_type", nullable = false)
    private String assetType; // Loại tài sản thế chấp [cite: 17]

    @Column(name = "estimated_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal estimatedValue; // Giá trị ước tính [cite: 18]

    @Column(name = "conversion_rate", precision = 5, scale = 4)
    private BigDecimal conversionRate; // Tỉ lệ chuyển đổi [cite: 19]

    // Quan hệ 1-1: Một khoản vay chỉ liên kết tối đa với 1 tài sản thế chấp [cite: 120, 121]
    @OneToOne
    @JoinColumn(name = "loan_id", nullable = false, unique = true)
    private Loan loan;
}