package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    private BigDecimal amount; // Số tiền thực tế đóng

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate; // Ngày trả

    @Column(name = "payment_method")
    private String paymentMethod; // Phương thức trả (Chuyển khoản, Tiền mặt...)

    // Trả cho kỳ hạn nào
    @ManyToOne
    @JoinColumn(name = "repayment_schedules", nullable = false)
    // THAY THẾ @JsonIgnore BẰNG CÁCH NÀY ĐỂ VỪA NGẮT VÒNG LẶP, VỪA CHO PHÉP NHẬN DỮ LIỆU TỪ CLIENT
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private RepaymentSchedule repaymentSchedule;

    // Nhân viên tiếp nhận/duyệt khoản tiền này
    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

}
