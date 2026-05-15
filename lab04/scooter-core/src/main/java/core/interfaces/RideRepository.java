package core.interfaces;

import core.domain.Ride;

public interface RideRepository {
    void save(Ride ride);
    void update(Ride ride);
    Ride findById(String rideId);
}
