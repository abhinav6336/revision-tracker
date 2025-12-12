package com.abhinav.demo.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.abhinav.demo.model.User;
import com.abhinav.demo.repo.UserRepository;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    
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
}
