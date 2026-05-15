package core.domain;

import java.time.LocalDateTime;

public class Ride {
    private String id;
    private String userId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double distanceKm;
    private double cost;
    private RideStatus status;

    public Ride(String id, String userId, LocalDateTime startTime) {
        this.id = id;
        this.userId = userId;
        this.startTime = startTime;
        this.status = RideStatus.ACTIVE;
    }

    public void complete(LocalDateTime endTime, double distanceKm, double cost) {
        this.endTime = endTime;
        this.distanceKm = distanceKm;
        this.cost = cost;
        this.status = RideStatus.COMPLETED;
    }

    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public double getDistanceKm() { return distanceKm; }
    public double getCost() { return cost; }
    public RideStatus getStatus() { return status; }
}
