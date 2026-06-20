package com.example.demo.repository;

import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Hàm cực kỳ quan trọng để phục vụ việc Login
    Optional<User> findByUsername(String username);

    // 🌟 SỬA LỖI: Thêm hàm kiểm tra sự tồn tại của username
    boolean existsByUsername(String username);
}
