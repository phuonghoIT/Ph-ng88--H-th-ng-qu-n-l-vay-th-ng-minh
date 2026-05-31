package com.example.demo.entity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "employees")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor

public class Employees {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_id")
    private Long employeeId;

    @Column(name = "full_name")
    private String fullName;

    @Column(name ="role")
    private String role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    @ManyToOne
    @JoinColumn(name = "branch_id", nullable = false)
    private Branches branch;
}
