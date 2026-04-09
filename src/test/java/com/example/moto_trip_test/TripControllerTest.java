package com.example.moto_trip_test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TripController.class)
@Import(TestJacksonConfig.class)
public class TripControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TripService tripService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testCreateUser() throws Exception {
        User user = new User("Alice", true);
        ReflectionTestUtils.setField(user, "id", 1L);
        when(tripService.createUser("Alice", true)).thenReturn(user);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", "Alice", "premium", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice"));
    }

    @Test
    public void testCreateTrip() throws Exception {
        Trip trip = new Trip("Trip A", 10, false);
        ReflectionTestUtils.setField(trip, "id", 1L);
        when(tripService.createTrip("Trip A", 10, false)).thenReturn(trip);

        mockMvc.perform(post("/api/trips")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", "Trip A", "maxParticipants", 10, "premiumOnly", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Trip A"));
    }

    @Test
    public void testJoinTrip() throws Exception {
        Trip trip = new Trip("Trip A", 10, false);
        User user = new User("Alice", false);
        trip.join(user);
        ReflectionTestUtils.setField(trip, "id", 1L);
        when(tripService.joinTrip(1L, 2L)).thenReturn(trip);

        mockMvc.perform(post("/api/trips/1/join")
                .param("userId", "2"))
                .andExpect(status().isOk());
    }

    @Test
    public void testStartTrip() throws Exception {
        Trip trip = new Trip("Trip A", 10, false);
        ReflectionTestUtils.setField(trip, "started", true);
        when(tripService.startTrip(1L)).thenReturn(trip);

        mockMvc.perform(post("/api/trips/1/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.started").value(true));
    }

    @Test
    public void testAllTrips() throws Exception {
        Trip trip = new Trip("Trip A", 10, false);
        when(tripService.allTrips()).thenReturn(Collections.singletonList(trip));

        mockMvc.perform(get("/api/trips"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Trip A"));
    }
}
