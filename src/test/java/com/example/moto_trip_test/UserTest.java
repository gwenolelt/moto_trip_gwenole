package com.example.moto_trip_test;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

public class UserTest {

    @Test
    public void testNewUserStartsWithZeroPoints() {
        User user = new User("Alice", false);
        int points = (int) ReflectionTestUtils.getField(user, "points");
        assertEquals(0, points);
    }

    @Test
    public void testAddPointsIncrementsTotal() {
        User user = new User("Alice", false);
        user.addPoints(10);
        assertEquals(10, (int) ReflectionTestUtils.getField(user, "points"));

        user.addPoints(5);
        assertEquals(15, (int) ReflectionTestUtils.getField(user, "points"));
    }

    @Test
    public void testAddPointsZero() {
        User user = new User("Alice", false);
        user.addPoints(0);
        assertEquals(0, (int) ReflectionTestUtils.getField(user, "points"));
    }

    @Test
    public void testPremiumUserCanJoinPremium() {
        User premium = new User("Alice", true);
        assertTrue(premium.canJoinPremium());
    }

    @Test
    public void testNonPremiumUserCannotJoinPremium() {
        User basic = new User("Bob", false);
        assertFalse(basic.canJoinPremium());
    }

    @Test
    public void testJoiningTripAwardsTenPoints() {
        Trip trip = new Trip("Trip A", 5, false);
        User user = new User("Alice", false);

        trip.join(user);

        assertEquals(10, (int) ReflectionTestUtils.getField(user, "points"));
    }
}