package core.interfaces;

import core.domain.Ride;
import java.util.List;

public interface RideRepository {
    void save(Ride ride);
    void update(Ride ride);
    Ride findById(String rideId);
    List<Ride> findAll();
}
