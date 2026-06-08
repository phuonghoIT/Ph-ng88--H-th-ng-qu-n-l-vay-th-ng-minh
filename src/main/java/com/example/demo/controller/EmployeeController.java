package com.example.demo.controller;

import com.example.demo.entity.Employee;
import com.example.demo.repository.EmployeeRepository;
import com.example.demo.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
@CrossOrigin(origins = "*") // Cấp phép CORS cho Sơn và Việt kết nối Front-end
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private EmployeeRepository employeeRepository;


    /**
     * 🟢 1 & 2. LẤY DANH SÁCH NHÂN VIÊN VÀ BỘ LỌC THEO CHI NHÁNH
     * Bản chất: Nếu khách gọi trơn '/api/employees' -> Trả về tất cả.
     * Nếu khách gọi '/api/employees?branchId=5' -> Chỉ trả về nhân viên chi nhánh 5.
     * Dùng required = false để báo cho Java biết tham số sau dấu ? có thể có hoặc không.
     */
    @GetMapping
    public ResponseEntity<List<Employee>> getEmployees(@RequestParam(required = false) Long branchId) {
        if (branchId != null) {
            // Nếu có truyền branchId, gọi Service lọc theo chi nhánh
            return ResponseEntity.ok(employeeService.getEmployeesByBranch(branchId));
        }
        // Nếu không truyền, mặc định trả về tất cả nhân viên
        return ResponseEntity.ok(employeeService.getAllEmployees());
    }

    /**
     * 🟢 3. XEM CHI TIẾT MỘT NHÂN VIÊN THEO ID
     * GET http://localhost:8080/api/employees/12
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getEmployeeById(@PathVariable Long id) {
        try {
            Employee employee = employeeService.getEmployeeById(id);
            return ResponseEntity.ok(employee);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * 🔵 4. TẠO MỚI NHÂN VIÊN (Tuyển dụng nhân sự)
     * POST http://localhost:8080/api/employees
     * Cần @Valid để kích hoạt chip gác cổng dữ liệu rác trong Entity Employee
     */
    @PostMapping
    public ResponseEntity<?> createEmployee(@Valid @RequestBody Employee newEmployee) {
        try {
            Employee savedEmployee = employeeService.createEmployeeWithAccount(newEmployee);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedEmployee);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    /**
     * 🟡 5. SỬA THÔNG TIN / ĐIỀU CHUYỂN CHI NHÁNH / ĐỔI TRẠNG THÁI NGHỈ VIỆC
     * PUT http://localhost:8080/api/employees/12
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody Employee updatedData
    ) {
        try {
            Employee result = employeeService.updateEmployee(id, updatedData);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
    @PostMapping("/bulk")
    public ResponseEntity<?> createMultipleEmployees(@Valid @RequestBody List<Employee> employeeList) {
        // (Lưu ý: Nếu file Entity của em tên là Employees thì đổi chữ Branches thành Employees nhé)

        // Gọi Repository hoặc Service để lưu cả cụm
        List<Employee> savedList = new ArrayList<>();

        // 2. Dùng vòng lặp chạy qua từng ông nhân viên gửi từ Postman lên
        for (Employee emp : employeeList) {
            // Gọi hàm save() truyền thống của Repository để ném từng ông xuống DB
            Employee savedEmp = employeeRepository.save(emp);

            // Lưu xong thì nhét ông đó vào danh sách kết quả
            savedList.add(savedEmp);
        }

        // 3. Trả về mã 201 Created kèm cả đội quân nhân viên đã có ID tự tăng dưới DB
        return new ResponseEntity<>(savedList, HttpStatus.CREATED);
    }
}