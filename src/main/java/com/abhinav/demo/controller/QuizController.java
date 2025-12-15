package com.abhinav.demo.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.abhinav.demo.service.QuizService;
import com.abhinav.demo.service.revisiontrackerservice;

@RestController
@RequestMapping("/api/quiz")
public class QuizController {

    @Autowired
    private QuizService quizService;

    @Autowired
    private revisiontrackerservice revisionService;

    @GetMapping("/today")
    public ResponseEntity<Object> getQuizForToday(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("message", "User not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
        }

        List<String> topics = revisionService.getTopicsForRevision(userId, LocalDate.now());

        if (topics.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "No topics scheduled for revision today!");
            response.put("questions", List.of());
            return ResponseEntity.ok(response);
        }

        List<Map<String, Object>> questions = quizService.generateQuiz(topics);

        Map<String, Object> response = new HashMap<>();
        response.put("topics", topics);
        response.put("questions", questions);

        return ResponseEntity.ok(response);
    }
}
