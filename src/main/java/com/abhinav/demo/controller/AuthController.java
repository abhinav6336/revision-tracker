package com.abhinav.demo.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.abhinav.demo.model.User;
import com.abhinav.demo.service.UserService;

@RestController
@CrossOrigin
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private UserService userService;
    
    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signup(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        String username = request.get("username");
        String email = request.get("email");
        String password = request.get("password");
        
        // Validate input
        if (username == null || username.trim().isEmpty() ||
            email == null || email.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "All fields are required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        
        if (password.length() < 6) {
            response.put("success", false);
            response.put("message", "Password must be at least 6 characters");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        
        // Attempt signup
        boolean success = userService.signup(username, email, password);
        
        if (success) {
            response.put("success", true);
            response.put("message", "Signup successful! You can now login.");
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "Username or email already exists");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        String username = request.get("username");
        String password = request.get("password");
        
        // Validate input
        if (username == null || username.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Username and password are required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        
        // Attempt login
        User user = userService.login(username, password);
        
        if (user != null) {
            response.put("success", true);
            response.put("message", "Login successful!");
            response.put("user", new HashMap<String, Object>() {{
                put("id", user.getId());
                put("username", user.getUsername());
                put("email", user.getEmail());
            }});
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "Invalid username or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
    
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        String email = request.get("email");
        
        if (email == null || email.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Email is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        
        String token = userService.createPasswordResetToken(email);
        
        if (token == null) {
            response.put("success", false);
            response.put("message", "Email not found in system");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        
        response.put("success", true);
        response.put("message", "Password reset link sent to email. Check your inbox.");
        response.put("token", token);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        String token = request.get("token");
        String newPassword = request.get("password");
        
        if (token == null || token.trim().isEmpty() ||
            newPassword == null || newPassword.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Token and password are required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        
        if (newPassword.length() < 6) {
            response.put("success", false);
            response.put("message", "Password must be at least 6 characters");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        
        boolean success = userService.resetPassword(token, newPassword);
        
        if (success) {
            response.put("success", true);
            response.put("message", "Password reset successful! You can now login.");
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "Invalid or expired reset token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
}
