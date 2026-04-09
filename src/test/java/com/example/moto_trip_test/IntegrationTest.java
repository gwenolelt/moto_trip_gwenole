package com.example.moto_trip_test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class IntegrationTest {

    @Autowired
    private TripRepository tripRepo;

    @Autowired
    private UserRepository userRepo;

    @Test
    public void testPersistenceAndRelations() {
        User user = new User("Alice", true);
        user = userRepo.save(user);

        Trip trip = new Trip("Trip A", 10, false);
        trip = tripRepo.save(trip);

        trip.join(user);
        trip = tripRepo.save(trip);

        assertNotNull(user);
        assertNotNull(trip);

        Long tripId = (Long) ReflectionTestUtils.getField(trip, "id");
        Long userId = (Long) ReflectionTestUtils.getField(user, "id");
        assertNotNull(tripId);
        assertNotNull(userId);

        Trip savedTrip = tripRepo.findById(tripId).orElse(null);
        assertNotNull(savedTrip);
        assertEquals(9, savedTrip.remainingPlaces());

        User savedUser = userRepo.findById(userId).orElse(null);
        assertNotNull(savedUser);
        assertEquals(10, (int) ReflectionTestUtils.getField(savedUser, "points"));
    }
}