package com.example.demo.controller;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.JwtAuthResponse;
import com.example.demo.entity.User;
import com.example.demo.security.JwtTokenProvider;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    // 🌟 1. TIÊM THÊM 2 THẰNG NÀY ĐỂ XỬ LÝ LỌC JWT
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    // API Đăng ký tài khoản (Giữ nguyên bản cũ sạch sẽ của em)
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            User registeredUser = userService.registerUser(user);
            return ResponseEntity.ok(registeredUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 🌟 2. CẬP NHẬT LẠI HÀM LOGIN ĐỂ SINH VÀ TRẢ VỀ TOKEN JWT
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            // Thọc xuống DB đối chiếu tài khoản và mật khẩu (Đã băm BCrypt) xem khớp không
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            // Nạp thông tin chứng thực vào bộ nhớ gác cổng của hệ thống Spring
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Gọi máy dập Token bốc Username + Role đóng dấu vào chuỗi JWT
            String jwt = tokenProvider.generateToken(authentication);

            // Trả Token về dưới dạng Object DTO sạch sẽ { "accessToken": "...", "tokenType": "Bearer" }
            return ResponseEntity.ok(new JwtAuthResponse(jwt));

        } catch (Exception e) {
            return ResponseEntity.status(401).body("Tài khoản hoặc mật khẩu không chính xác!");
        }
    }

    // API Đổi mật khẩu (Giữ nguyên bản cũ sạch sẽ của em)
    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String oldPassword = request.get("oldPassword");
            String newPassword = request.get("newPassword");

            userService.changePassword(username, oldPassword, newPassword);

            return ResponseEntity.ok("Đổi mật khẩu thành công!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}