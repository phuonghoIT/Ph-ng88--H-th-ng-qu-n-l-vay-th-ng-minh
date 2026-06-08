package com.example.demo.security;

import com.example.demo.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String token = getJwtFromRequest(request);
        String authHeader = request.getHeader("Authorization");
        logger.debug("JwtAuthenticationFilter: path=" + request.getRequestURI() + ", authHeader=" + authHeader + ", token=" + (token != null ? "present" : "null"));

        // 🌟 SỬA LỖI: Chỉ thực hiện logic xác thực NẾU có Token trong Header
        if (StringUtils.hasText(token)) {
            try {
                if (tokenProvider.validateToken(token)) {
                    String username = tokenProvider.getUsernameFromJWT(token);
                    UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.debug("JwtAuthenticationFilter: authenticated user=" + username + ", authorities=" + userDetails.getAuthorities());
                }
            } catch (Exception ex) {
                // Ghi log lỗi nhưng không chặn request, để các bộ lọc sau xử lý
                logger.error("Could not set user authentication in security context", ex);
            }
        } else {
            logger.debug("JwtAuthenticationFilter: no token found in Authorization header");
        }

        // Luôn gọi filterChain.doFilter() ở cuối để request tiếp tục được xử lý
        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
