package com.example.demo.service;

import com.example.demo.entity.Branches;
import com.example.demo.repository.BranchRepository;
import com.example.demo.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BranchService {
    @Autowired
    private BranchRepository branchRepository;

    public List<Branches> getAllBranches() {
        return branchRepository.findAll(); // Gọi hàm có sẵn của Repo
    }

    // Hàm tạo mới chi nhánh
    public Branches createBranch(Branches branch) {
        return branchRepository.save(branch); // Gọi hàm có sẵn của Repo
    }

    public Branches getBranchById(Long id) {
        // .findById(id) của JPA sẽ trả về một cái hộp Optional (có thể có dữ liệu hoặc rỗng)
        // .orElseThrow() nghĩa là: Nếu trong hộp rỗng tuếch, lập tức quăng lỗi RuntimeException ra!
        return branchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("🔴 LỖI: Không tìm thấy chi nhánh nào có ID là " + id + " trên hệ thống!"));
    }

    public Branches updateBranch(Long id, Branches updatedData) {
        // Bước A: Phải dùng chính hàm getBranchById ở trên để check xem chi nhánh đó có tồn tại không
        Branches existingBranch = this.getBranchById(id);

        // Bước B: Nếu tồn tại, tiến hành đè dữ liệu mới lên dữ liệu cũ
        existingBranch.setBranchName(updatedData.getBranchName());
        existingBranch.setAddress(updatedData.getAddress());
        existingBranch.setPhone(updatedData.getPhone());

        // Bước C: Lưu lại xuống Database
        return branchRepository.save(existingBranch);
    }

}
