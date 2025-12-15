package com.abhinav.demo.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.abhinav.demo.model.User;
import com.abhinav.demo.model.PasswordResetToken;
import com.abhinav.demo.repo.UserRepository;
import com.abhinav.demo.repo.PasswordResetTokenRepository;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordResetTokenRepository tokenRepository;
    
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    
    @Autowired(required = false)
    private JavaMailSender mailSender;
    
    public boolean signup(String username, String email, String password) {
        // Check if username already exists
        if (userRepository.existsByUsername(username)) {
            return false;
        }
        
        // Check if email already exists
        if (userRepository.existsByEmail(email)) {
            return false;
        }
        
        // Create new user with hashed password
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        
        userRepository.save(user);
        return true;
    }
    
    public User login(String username, String password) {
        Optional<User> user = userRepository.findByUsername(username);
        
        if (user.isPresent()) {
            // Check if password matches
            if (passwordEncoder.matches(password, user.get().getPassword())) {
                return user.get();
            }
        }
        
        return null;
    }
    
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }
    
    public User getUserById(Long id) {
        if (id == null) return null;
        return userRepository.findById(id).orElse(null);
    }
    
    public String createPasswordResetToken(String email) {
        User user = userRepository.findByUsername(email).orElse(null);
        if (user == null) {
            user = userRepository.findAll().stream()
                .filter(u -> u.getEmail().equals(email))
                .findFirst()
                .orElse(null);
        }
        
        if (user == null) {
            return null;
        }
        
        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(24);
        
        // Delete old tokens for this user
        tokenRepository.deleteByUserId(user.getId());
        
        PasswordResetToken resetToken = new PasswordResetToken(token, user, expiryDate);
        tokenRepository.save(resetToken);
        
        sendPasswordResetEmail(user.getEmail(), token);
        return token;
    }
    
    public boolean resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> resetToken = tokenRepository.findByToken(token);
        
        if (resetToken.isEmpty()) {
            return false;
        }
        
        PasswordResetToken prt = resetToken.get();
        if (prt.isExpired()) {
            tokenRepository.delete(prt);
            return false;
        }
        
        User user = prt.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        tokenRepository.delete(prt);
        
        return true;
    }
    
    private void sendPasswordResetEmail(String email, String token) {
        if (mailSender == null) {
            System.out.println("Email not configured. Reset token: " + token);
            return;
        }
        
        String resetLink = "http://localhost:8080/reset-password?token=" + token;
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Password Reset Request");
        message.setText("Click the link below to reset your password:\n\n" + resetLink + 
                       "\n\nThis link expires in 24 hours.\n\nIf you didn't request this, ignore this email.");
        
        try {
            mailSender.send(message);
        } catch (Exception e) {
            System.out.println("Failed to send email: " + e.getMessage());
        }
    }
}
