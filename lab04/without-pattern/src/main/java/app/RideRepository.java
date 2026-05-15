package app;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RideRepository {
    private final Connection connection;

    public RideRepository(Connection connection) {
        this.connection = connection;
    }

    public void save(Ride ride) {
        String sql = "INSERT INTO rides (id, user_id, start_time, status) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, ride.getId());
            stmt.setString(2, ride.getUserId());
            stmt.setTimestamp(3, Timestamp.valueOf(ride.getStartTime()));
            stmt.setString(4, ride.getStatus().name());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void update(Ride ride) {
        String sql = "UPDATE rides SET end_time = ?, distance_km = ?, cost = ?, status = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, ride.getEndTime() != null ? Timestamp.valueOf(ride.getEndTime()) : null);
            stmt.setDouble(2, ride.getDistanceKm());
            stmt.setDouble(3, ride.getCost());
            stmt.setString(4, ride.getStatus().name());
            stmt.setString(5, ride.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Ride findById(String rideId) {
        String sql = "SELECT * FROM rides WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, rideId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Ride> findAll() {
        List<Ride> rides = new ArrayList<>();
        String sql = "SELECT * FROM rides ORDER BY start_time DESC";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) rides.add(mapRow(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rides;
    }

    private Ride mapRow(ResultSet rs) throws SQLException {
        Ride ride = new Ride(rs.getString("id"), rs.getString("user_id"), rs.getTimestamp("start_time").toLocalDateTime());
        if (RideStatus.COMPLETED.name().equals(rs.getString("status"))) {
            ride.complete(rs.getTimestamp("end_time").toLocalDateTime(), rs.getDouble("distance_km"), rs.getDouble("cost"));
        }
        return ride;
    }
}
