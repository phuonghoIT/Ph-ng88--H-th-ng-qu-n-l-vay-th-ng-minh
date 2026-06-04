package com.example.demo.service;

import com.example.demo.entity.Branches;
import com.example.demo.entity.EmployeeStatus;
import com.example.demo.entity.Employees; // Khớp với Entity Employees có chữ s của em
import com.example.demo.repository.BranchRepository;
import com.example.demo.repository.EmployeeRepository;
import com.example.demo.repository.LoanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private LoanRepository loanRepository;

    // =========================================================================
    // PHẦN 1: CÁC HÀM NGHIỆP VỤ CRUD CƠ BẢN (ĐỂ PHỤC VỤ CONTROLLER)
    // =========================================================================

    /**
     * 🟢 Lấy tất cả danh sách nhân viên
     */
    public List<Employees> getAllEmployees() {
        return employeeRepository.findAll();
    }

    /**
     * 🟢 Tìm nhân viên theo ID (Cơ chế gác cổng phòng ngự)
     */
    public Employees getEmployeeById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("🔴 LỖI: Không tìm thấy nhân viên có ID là " + id));
    }

    /**
     * 🟢 Lọc nhân viên theo chi nhánh
     */
    public List<Employees> getEmployeesByBranch(Long branchId) {
        branchRepository.findById(branchId)
                .orElseThrow(() -> new RuntimeException("🔴 LỖI: Chi nhánh ID " + branchId + " không tồn tại."));
        return employeeRepository.findByBranchBranchId(branchId);
    }

    /**
     * 🔵 Tạo mới nhân viên (Tuyển dụng nhân sự)
     * Tự động ép trạng thái ban đầu là 'Working' để tính tải thuật toán
     */
    @Transactional
    public Employees createEmployee(Employees newEmployee) {
        if (newEmployee.getBranch() == null || newEmployee.getBranch().getBranchId() == null) {
            throw new RuntimeException("🔴 LỖI: Không thể tạo! Thiếu thông tin chi nhánh (branchId).");
        }

        Long targetBranchId = newEmployee.getBranch().getBranchId();
        Branches branch = branchRepository.findById(targetBranchId)
                .orElseThrow(() -> new RuntimeException("🔴 LỖI: Không thể tạo! Chi nhánh ID " + targetBranchId + " không tồn tại."));

        newEmployee.setBranch(branch);
        newEmployee.setStatus(EmployeeStatus.ACTIVE);
        return employeeRepository.save(newEmployee);
    }

    /**
     * 🟡 Cập nhật thông tin nhân viên cơ bản (Tên, vai trò, trạng thái)
     */
    @Transactional
    public Employees updateEmployee(Long id, Employees updatedData) {
        Employees existingEmployee = this.getEmployeeById(id);

        if ((EmployeeStatus.INACTIVE)==(existingEmployee.getStatus())) {
            throw new RuntimeException("🔴 LỖI: Nhân viên này đã nghỉ việc, thông tin lịch sử bị khóa!");
        }

        existingEmployee.setFullName(updatedData.getFullName());
        existingEmployee.setRole(updatedData.getRole());
        existingEmployee.setStatus(updatedData.getStatus());

        return employeeRepository.save(existingEmployee);
    }


    // =========================================================================
    // PHẦN 2: CÁC NGHIỆP VỤ NÂNG CAO ĐẲNG CẤP CỦA PHƯƠNG
    // =========================================================================

    /**
     * ✈️ NGHIỆP VỤ NÂNG CAO 1: Điều chuyển nhân viên sang chi nhánh mới & Tự động bàn giao công việc
     */
    @Transactional
    public void ship_work_same_branch(Long employeeId, Long newBranchId) {
        // 1. Tìm nhân viên cần điều chuyển
        Employees e = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));

        Long oldBranchId = e.getBranch().getBranchId();

        // Nếu thực sự có sự thay đổi chi nhánh thì mới xử lý
        if (!oldBranchId.equals(newBranchId)) {

            // 2. Thuật toán đỉnh cao của Phương: Tìm người rảnh nhất ở chi nhánh CŨ để nhận bàn giao

            Long newHandlerId = employeeRepository.findLeastContractSameBranch(oldBranchId, employeeId);

            if (newHandlerId == null) {
                throw new RuntimeException("🔴 LỖI: Chi nhánh cũ không còn ai khác đang 'Working' để nhận bàn giao việc!");
            }

            // 3. Chạy lệnh Native SQL để chuyển toàn bộ hợp đồng sang cho người mới
            loanRepository.shipWorkNative(employeeId, newHandlerId);

            // 4. Tìm thực thể chi nhánh MỚI và gán cho nhân viên
            Branches newBranch = branchRepository.findById(newBranchId)
                    .orElseThrow(() -> new RuntimeException("🔴 LỖI: Chi nhánh mới không tồn tại!"));
            e.setBranch(newBranch);

            // 5. Lưu lại thông tin điều chuyển
            employeeRepository.save(e);
        }
    }

    /**
     * 💥 NGHIỆP VỤ NÂNG CAO 2: Sa thải nhân viên (Soft Delete) có bàn giao hồ sơ nợ
     */
    @Transactional
    public void fired_employee(Long employeeId) {
        // 1. Tìm nhân viên để lấy chi nhánh cũ của họ trước khi đuổi
        Employees e = this.getEmployeeById(employeeId);
        Long oldBranchId = e.getBranch().getBranchId();

        // 2. Tìm người nhận bàn giao cùng chi nhánh
        // FIX: Truyền thêm employeeId để truy vấn SQL loại trừ chính người đang bị sa thải
        Long newHandlerId = employeeRepository.findLeastContractSameBranch(oldBranchId, employeeId);

        if (newHandlerId == null) {
            throw new RuntimeException("🔴 LỖI: Không thể sa thải! Chi nhánh không còn ai khác để nhận bàn giao lại hồ sơ vay!");
        }

        // 3. Tiến hành chuyển hết hợp đồng vay sang người rảnh nhất
        loanRepository.shipWorkNative(employeeId, newHandlerId);

        // 4. Đuổi việc (Xóa mềm bằng hàm native update status = 'INACTIVE' của em)
        employeeRepository.softDeleteEmployee(employeeId);

        // 5. Ép đồng bộ xuống ổ cứng ngay lập tức
        employeeRepository.flush();
    }
}