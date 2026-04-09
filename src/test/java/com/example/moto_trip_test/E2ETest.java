package com.example.moto_trip_test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestJacksonConfig.class)
public class E2ETest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testE2EScenario() throws Exception {
        // 1. Create User
        MvcResult userResult = mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", "Alice", "premium", true))))
                .andExpect(status().isOk())
                .andReturn();

        Integer userId = JsonPath.read(userResult.getResponse().getContentAsString(), "$.id");

        // 2. Create Trip
        MvcResult tripResult = mockMvc.perform(post("/api/trips")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", "Trip A", "maxParticipants", 10, "premiumOnly", false))))
                .andExpect(status().isOk())
                .andReturn();

        Integer tripId = JsonPath.read(tripResult.getResponse().getContentAsString(), "$.id");

        // 3. Join Trip
        mockMvc.perform(post("/api/trips/" + tripId + "/join")
                .param("userId", userId.toString()))
                .andExpect(status().isOk());

        // 4. Start Trip
        mockMvc.perform(post("/api/trips/" + tripId + "/start"))
                .andExpect(status().isOk());

        // 5. Verify State
        mockMvc.perform(get("/api/trips"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].started").value(true));
    }
}

