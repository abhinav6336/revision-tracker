package com.abhinav.demo.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.abhinav.demo.model.User;
import com.abhinav.demo.service.UserService;
import com.abhinav.demo.service.revisiontrackerservice;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(homecontroller.class)
public class DuplicateTopicTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private revisiontrackerservice revisionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testAddDuplicateTopic() throws Exception {
        // Arrange
        Long userId = 1L;
        String topicName = "Java";

        User mockUser = new User();
        mockUser.setId(userId);

        when(userService.getUserById(userId)).thenReturn(mockUser);

        // precise behavior: Even if we were to assume topic exists (which we can't
        // easily force the controller to check since we removed the check),
        // we just want to ensure we get a 200 OK and success.
        // If the check was still there and we mocked topicExists to true, we would get
        // 409.
        // Since we removed the check, even if revisionService.topicExists(...) was
        // called (it won't be),
        // we essentially just want to prove the happy path works for ANY topic.

        // Let's explicitly mock topicExists to true, just in case the code WAS NOT
        // modified correctly.
        // If the code still had the check, this would fail with 409.
        when(revisionService.topicExists(anyString(), anyLong())).thenReturn(true);

        Map<String, Object> request = new HashMap<>();
        request.put("topic", topicName);
        request.put("userId", userId); // passing in body as fallback, or header

        // Act & Assert
        mockMvc.perform(post("/topics")
                .header("X-User-Id", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Topic added successfully!"));
    }
}
