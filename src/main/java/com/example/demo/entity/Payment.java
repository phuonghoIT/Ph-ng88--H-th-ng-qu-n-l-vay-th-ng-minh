package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "payments")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount; // Số tiền thực tế đóng [cite: 38]

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate; // Ngày trả [cite: 39]

    @Column(name = "payment_method")
    private String paymentMethod; // Phương thức trả (Chuyển khoản, Tiền mặt...) [cite: 41]

    // Trả cho khoản vay nào [cite: 40, 81]
    @ManyToOne
    @JoinColumn(name = "repayment_schedules", nullable = false)
    private RepaymentSchedule repaymentSchedule;

    // Nhân viên tiếp nhận/duyệt khoản tiền này [cite: 42]
    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;


}