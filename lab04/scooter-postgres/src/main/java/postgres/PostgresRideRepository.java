package postgres;

import core.domain.Ride;
import core.domain.RideStatus;
import core.interfaces.RideRepository;

import java.sql.*;
import java.time.LocalDateTime;

public class PostgresRideRepository implements RideRepository {

    private final Connection connection;

    // The core domain knows nothing about java.sql.Connection
    public PostgresRideRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void save(Ride ride) {
        String sql = "INSERT INTO rides (id, user_id, start_time, status) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, ride.getId());
            stmt.setString(2, ride.getUserId());
            stmt.setTimestamp(3, Timestamp.valueOf(ride.getStartTime()));
            stmt.setString(4, ride.getStatus().name());
            
            stmt.executeUpdate();
            System.out.println("[Postgres] Successfully saved ride " + ride.getId());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(Ride ride) {
        String sql = "UPDATE rides SET end_time = ?, distance_km = ?, cost = ?, status = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, ride.getEndTime() != null ? Timestamp.valueOf(ride.getEndTime()) : null);
            stmt.setDouble(2, ride.getDistanceKm());
            stmt.setDouble(3, ride.getCost());
            stmt.setString(4, ride.getStatus().name());
            stmt.setString(5, ride.getId());
            
            stmt.executeUpdate();
            System.out.println("[Postgres] Successfully updated ride " + ride.getId());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Ride findById(String rideId) {
        String sql = "SELECT * FROM rides WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, rideId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String userId = rs.getString("user_id");
                LocalDateTime startTime = rs.getTimestamp("start_time").toLocalDateTime();
                Ride ride = new Ride(rideId, userId, startTime);
                
                String status = rs.getString("status");
                if (RideStatus.COMPLETED.name().equals(status)) {
                    LocalDateTime endTime = rs.getTimestamp("end_time").toLocalDateTime();
                    double distanceKm = rs.getDouble("distance_km");
                    double cost = rs.getDouble("cost");
                    ride.complete(endTime, distanceKm, cost);
                }
                return ride;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
