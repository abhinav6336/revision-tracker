package com.abhinav.demo.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.abhinav.demo.service.QuizService;
import com.abhinav.demo.service.UserService;
import com.abhinav.demo.service.revisiontrackerservice;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@WebMvcTest(QuizController.class)
@AutoConfigureMockMvc(addFilters = false)
public class QuizControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QuizService quizService;

    @MockBean
    private revisiontrackerservice revisionService;

    @MockBean
    private UserService userService; // required because it might be loaded in context

    @Test
    public void testGetQuizForToday() throws Exception {
        Long userId = 1L;
        List<String> mockTopics = List.of("Java");

        when(revisionService.getTopicsForRevision(eq(userId), any(LocalDate.class))).thenReturn(mockTopics);

        Map<String, Object> mockQuestion = new HashMap<>();
        mockQuestion.put("question", "Test Q");
        mockQuestion.put("options", List.of("A", "B"));
        mockQuestion.put("answer", "A");

        when(quizService.generateQuiz(anyList(), any(Integer.class))).thenReturn(List.of(mockQuestion));

        mockMvc.perform(get("/api/quiz/today")
                .header("X-User-Id", String.valueOf(userId)))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topics[0]").value("Java"))
                .andExpect(jsonPath("$.questions[0].question").value("Test Q"));
    }
}
