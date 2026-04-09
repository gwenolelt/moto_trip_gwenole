package com.example.moto_trip_test;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TripController {

    private final TripService service;

    public TripController(TripService service) {
        this.service = service;
    }

    @PostMapping("/users")
    public User createUser(@RequestBody Map<String, Object> p) {
        return service.createUser((String)p.get("name"), (boolean)p.get("premium"));
    }

    @PostMapping("/trips")
    public Trip createTrip(@RequestBody Map<String, Object> p) {
        return service.createTrip(
                (String)p.get("name"),
                (int)p.get("maxParticipants"),
                (boolean)p.get("premiumOnly")
        );
    }

    @PostMapping("/trips/{id}/join")
    public Trip join(@PathVariable Long id, @RequestParam Long userId) {
        return service.joinTrip(id, userId);
    }

    @PostMapping("/trips/{id}/start")
    public Trip start(@PathVariable Long id) {
        return service.startTrip(id);
    }

    @GetMapping("/trips")
    public List<Trip> all() {
        return service.allTrips();
    }
}