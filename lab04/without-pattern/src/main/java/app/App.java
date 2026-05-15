package app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.json.JavalinJackson;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class App {

    public static void main(String[] args) throws Exception {
        String jdbcUrl = "jdbc:postgresql://localhost:5432/scooter_db";
        Connection connection = DriverManager.getConnection(jdbcUrl, "scooter_user", "scooter_password");
        RideRepository repository = new RideRepository(connection);

        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/public");
            config.jsonMapper(new JavalinJackson(objectMapper, false));
        }).start(7071); // Use different port

        app.get("/api/rides", ctx -> ctx.json(repository.findAll()));

        app.post("/api/rides/start", ctx -> {
            String userId = (String) ctx.bodyAsClass(Map.class).get("userId");
            String rideId = UUID.randomUUID().toString();
            Ride ride = new Ride(rideId, userId, LocalDateTime.now());
            repository.save(ride);
            ctx.status(HttpStatus.CREATED).json(Map.of("rideId", rideId));
        });

        app.post("/api/rides/end", ctx -> {
            Map body = ctx.bodyAsClass(Map.class);
            String rideId = (String) body.get("rideId");
            double distance = ((Number) body.get("distance")).doubleValue();

            Ride ride = repository.findById(rideId);
            if (ride == null || ride.getStatus() == RideStatus.COMPLETED) {
                ctx.status(HttpStatus.BAD_REQUEST).result("Invalid ride");
                return;
            }

            LocalDateTime endTime = LocalDateTime.now();
            long minutes = Math.max(1, Duration.between(ride.getStartTime(), endTime).toMinutes());
            double cost = 1.0 + (minutes * 0.2);

            ride.complete(endTime, distance, cost);
            repository.update(ride);
            
            // Mock payment directly here
            System.out.println("Processing payment for user " + ride.getUserId() + ": $" + cost);
            
            ctx.status(HttpStatus.OK).result("Ride ended");
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { connection.close(); } catch (Exception e) {}
        }));
    }
}
