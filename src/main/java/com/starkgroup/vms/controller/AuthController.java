package com.starkgroup.vms.controller;

import com.starkgroup.vms.model.User;
import com.starkgroup.vms.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void seedDefaultAdmin() {
        if (userRepository.findByUsername("admin").isEmpty()) {
            userRepository.save(new User("admin", "stark123", "ADMIN"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.getOrDefault("username", "").trim();
        String password = credentials.getOrDefault("password", "");

        return userRepository.findByUsername(username)
                .filter(user -> user.getPassword().equals(password))
                .map(user -> ResponseEntity.ok(Map.of(
                        "message", "Login successful",
                        "username", user.getUsername(),
                        "role", user.getRole())))
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Invalid username or password")));
    }
}