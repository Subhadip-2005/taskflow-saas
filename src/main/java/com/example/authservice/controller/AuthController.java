package com.example.authservice.controller;

import com.example.authservice.dto.*;
import com.example.authservice.entity.Tenant;
import com.example.authservice.entity.User;
import com.example.authservice.repository.TenantRepository;
import com.example.authservice.repository.UserRepository;
import com.example.authservice.security.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import com.example.authservice.security.InvalidCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepo;
    private final TenantRepository tenantRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepo, TenantRepository tenantRepo,
                          PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepo = userRepo;
        this.tenantRepo = tenantRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        Tenant tenant = new Tenant();
        tenant.setName(req.getTenantName());
        tenant = tenantRepo.save(tenant);

        User user = new User();
        user.setTenantId(tenant.getId());
        user.setEmail(req.getEmail());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setRole("ADMIN");
        userRepo.save(user);

        String token = jwtUtil.generateToken(user.getId(), tenant.getId(), user.getRole());
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        User user = userRepo.findByEmailAndTenantId(req.getEmail(), req.getTenantId())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getTenantId(), user.getRole());
        return ResponseEntity.ok(new AuthResponse(token));
    }
}