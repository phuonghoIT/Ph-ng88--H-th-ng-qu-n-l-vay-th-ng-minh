package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "loan_products")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class LoanProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "loan_product_id")
    private Long loanProductId;

    @Column(name = "name", nullable = false)
    private String name;

    // Lãi suất (Ví dụ: 0.05 tương đương 5%)
    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal interestRate;

    // Tỉ lệ phạt khi trễ hạn
    @Column(name = "penalty_rate", precision = 5, scale = 4)
    private BigDecimal penaltyRate;

    // Thời hạn gói vay (đơn vị: số tháng, ví dụ: 6 tháng, 12 tháng, 24 tháng)
    @Column(name = "duration_months", nullable = false)
    private Integer durationMonths;
}
