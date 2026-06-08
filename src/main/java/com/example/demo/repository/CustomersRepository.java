package com.example.demo.repository;

import com.example.demo.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CustomersRepository extends JpaRepository<Customer, Long> {

    // Trả về Optional để tránh lỗi NullPointerException nếu không tìm thấy khách hàng
    Optional<Customer> findByIdentityNumber(String identityNumber);
    boolean existsByIdentityNumber(String identityNumber);
}