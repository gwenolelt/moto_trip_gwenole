package com.example.moto_trip_test;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TripTest {

    @Test
    public void testJoinTripSuccess() {
        Trip trip = new Trip("Trip 1", 2, false);
        User user = new User("Alice", false);
        trip.join(user);
        assertEquals(1, trip.remainingPlaces());
    }

    @Test
    public void testJoinTripFull() {
        Trip trip = new Trip("Trip 1", 1, false);
        User u1 = new User("Alice", false);
        User u2 = new User("Bob", false);

        trip.join(u1);
        Exception e = assertThrows(RuntimeException.class, () -> trip.join(u2));
        assertEquals("Trip full", e.getMessage());
    }

    @Test
    public void testJoinPremiumRefused() {
        Trip trip = new Trip("Prestige", 2, true);
        User user = new User("Bob", false);

        Exception e = assertThrows(RuntimeException.class, () -> trip.join(user));
        assertEquals("Premium required", e.getMessage());
    }

    @Test
    public void testStartTrip() {
        Trip trip = new Trip("Trip 1", 2, false);
        User user = new User("Alice", false);
        trip.join(user);
        trip.start();

        Exception e = assertThrows(RuntimeException.class, () -> trip.join(new User("Bob", false)));
        assertEquals("Trip already started", e.getMessage());
    }

    @Test
    public void testStartTripEmpty() {
        Trip trip = new Trip("Trip 1", 2, false);

        Exception e = assertThrows(RuntimeException.class, trip::start);
        assertEquals("No participants", e.getMessage());
    }
}

