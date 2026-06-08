package com.example.demo.entity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "employees")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor

public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_id")
    private Long employeeId;
    @NotBlank(message = "Tên nhân viên không được để trống")
    @Column(name = "full_name")
    private String fullName;
    @NotBlank(message = "Vai trò nhân viên không được để trống")
    @Column(name ="role")
    private String role;
    @NotNull(message = "Trạng thái nhân viên không được để trống")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    @ManyToOne
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @OneToOne(mappedBy = "employee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("employee") // Chặn vòng lặp vô tận khi Jackson sinh JSON
    private User user;
}
