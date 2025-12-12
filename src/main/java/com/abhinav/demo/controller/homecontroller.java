package com.abhinav.demo.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import com.abhinav.demo.model.Revision;
import com.abhinav.demo.model.User;
import com.abhinav.demo.service.UserService;
import com.abhinav.demo.service.revisiontrackerservice;

@Controller
@CrossOrigin
public class homecontroller {
    @Autowired
    revisiontrackerservice service;
    
    @Autowired
    UserService userService;
    
    @RequestMapping("/")
    public String greet(){
        return "revision-tracker.html";
    }
    
    @RequestMapping("/about")
    public ResponseEntity<String> about(){
        return ResponseEntity.ok("Hello, this is the Revision Tracker API");
    }
    
    @PostMapping("/topics")
    public ResponseEntity<Map<String, Object>> addtopics(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId){
        
        Map<String, Object> response = new HashMap<>();
        
        // Get userId from request if not in header
        if (userId == null && request.containsKey("userId")) {
            userId = ((Number) request.get("userId")).longValue();
        }
        
        if (userId == null) {
            response.put("success", false);
            response.put("message", "User not authenticated. Please login first.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        User user = userService.getUserById(userId);
        if (user == null) {
            response.put("success", false);
            response.put("message", "User not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        
        String topicName = (String) request.get("topic");
        if (topicName == null || topicName.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Topic name required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        
        if (service.topicExists(topicName, userId)) {
            response.put("success", false);
            response.put("message", "Topic already exists");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        
        Revision revision = new Revision();
        revision.setTopic(topicName);
        revision.setUser(user);
        service.addtopics(revision);
        
        response.put("success", true);
        response.put("message", "Topic added successfully!");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/topics")
    public ResponseEntity<Object> gettopics(@RequestHeader(value = "X-User-Id", required = false) Long userId){
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated. Please login first.");
        }
        
        return ResponseEntity.ok(service.gettopics(userId));
    }

    @DeleteMapping("/topics/{topic}")
    public ResponseEntity<Map<String, Object>> deletetopics(
            @PathVariable String topic,
            @RequestHeader(value = "X-User-Id", required = false) Long userId){
        
        Map<String, Object> response = new HashMap<>();
        
        if (userId == null) {
            response.put("success", false);
            response.put("message", "User not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        System.out.println("[DELETE] topic=" + topic + " userId=" + userId);
        service.removetopicsTransactional(topic, userId);
        response.put("success", true);
        response.put("message", "Topic deleted successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/topics/delete")
    public ResponseEntity<Map<String, Object>> deletetopicsPost(
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "X-User-Id", required = false) Long userId){

        Map<String, Object> response = new HashMap<>();

        if (userId == null) {
            response.put("success", false);
            response.put("message", "User not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        String topic = body.get("topic");
        System.out.println("[POST /topics/delete] topic=" + topic + " userId=" + userId);
        if (topic == null || topic.trim().isEmpty()){
            response.put("success", false);
            response.put("message", "Topic is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        service.removetopicsTransactional(topic, userId);
        response.put("success", true);
        response.put("message", "Topic deleted successfully");
        return ResponseEntity.ok(response);
    }
}
