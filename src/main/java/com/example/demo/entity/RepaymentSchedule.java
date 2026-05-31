package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "repayment_schedules")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class RepaymentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long scheduleId;

    @Column(name = "period_number", nullable = false)
    private int periodNumber; // Số thứ tự kỳ trả nợ (Kỳ 1, Kỳ 2...) [cite: 31]

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate; // Hạn chót thanh toán kỳ này [cite: 32]

    @Column(name = "principal_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal principalAmount; // Tiền gốc phải trả [cite: 33]

    @Column(name = "interest_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal interestAmount; // Tiền lãi phải trả [cite: 34]

    @Column(name = "penalty_amount", precision = 15, scale = 2)
    private BigDecimal penaltyAmount = BigDecimal.ZERO; // Tiền phạt tích lũy nếu trễ hạn [cite: 35]

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ScheduleStatus status = ScheduleStatus.UNPAID; // Trạng thái kì thanh toán

    // Nhiều kỳ trả nợ thuộc về một khoản vay
    @ManyToOne
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @OneToMany(mappedBy = "repaymentSchedule", cascade = CascadeType.ALL)
    private List<Payment> payments;
}