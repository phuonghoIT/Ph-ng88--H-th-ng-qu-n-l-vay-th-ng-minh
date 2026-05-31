package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "loans")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "loan_id")
    private Long loanId;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount; // Số tiền vay [cite: 14]

    @Column(name = "loan_date", nullable = false)
    private LocalDate loanDate; // Ngày vay [cite: 22]

    @Enumerated(EnumType.STRING)
    @Column(name = "loan_type", nullable = false)
    private LoanType loanType; // Tín chấp hoặc Thế chấp

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LoanStatus status = LoanStatus.PENDING; // Trạng thái gói vay

    // Khóa ngoại trỏ sang bảng Customers (Người vay) [cite: 21, 79]
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customers customer;

    // Khóa ngoại trỏ sang bảng Staff (Nhân viên quản lý) [cite: 20, 80]
    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employees employee;

    // Khóa ngoại trỏ sang bảng LoanProducts (Gói vay áp dụng) [cite: 23, 155]
    @ManyToOne
    @JoinColumn(name = "loan_product_id", nullable = false)
    private LoanProduct loanProduct;

    // Kết nối Một-Nhiều sang bảng Lịch trả nợ [cite: 30]
    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL)
    private List<RepaymentSchedule> repaymentSchedules;

}