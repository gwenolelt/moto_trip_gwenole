package com.example.moto_trip_test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TripServiceTest {

    @Mock
    private TripRepository tripRepo;

    @Mock
    private UserRepository userRepo;

    @InjectMocks
    private TripService tripService;

    @Test
    public void testCreateTrip() {
        Trip mockTrip = new Trip("Trip A", 10, false);
        when(tripRepo.save(any(Trip.class))).thenReturn(mockTrip);

        Trip trip = tripService.createTrip("Trip A", 10, false);
        assertNotNull(trip);
        verify(tripRepo).save(any(Trip.class));
    }

    @Test
    public void testCreateTripInvalidCapacity() {
        Exception e = assertThrows(IllegalArgumentException.class, () -> tripService.createTrip("Trip A", 0, false));
        assertEquals("Invalid capacity", e.getMessage());
    }

    @Test
    public void testJoinTrip() {
        Trip trip = new Trip("Trip A", 10, false);
        User user = new User("Alice", false);

        when(tripRepo.findById(1L)).thenReturn(Optional.of(trip));
        when(userRepo.findById(2L)).thenReturn(Optional.of(user));
        when(tripRepo.save(any(Trip.class))).thenReturn(trip);

        tripService.joinTrip(1L, 2L);
        assertEquals(9, trip.remainingPlaces());
    }

    @Test
    public void testJoinTripNonExistentTrip() {
        when(tripRepo.findById(1L)).thenReturn(Optional.empty());

        Exception e = assertThrows(RuntimeException.class, () -> tripService.joinTrip(1L, 2L));
        assertEquals("Trip not found", e.getMessage());
    }

    @Test
    public void testJoinTripNonExistentUser() {
        Trip trip = new Trip("Trip A", 10, false);
        when(tripRepo.findById(1L)).thenReturn(Optional.of(trip));
        when(userRepo.findById(2L)).thenReturn(Optional.empty());

        Exception e = assertThrows(RuntimeException.class, () -> tripService.joinTrip(1L, 2L));
        assertEquals("User not found", e.getMessage());
    }
}

