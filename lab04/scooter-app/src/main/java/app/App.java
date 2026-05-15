package app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import core.interfaces.PaymentGateway;
import core.interfaces.RideRepository;
import core.service.RideService;
import gateways.StripeGateway;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.json.JavalinJackson;
import postgres.PostgresRideRepository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;

public class App {

    public static void main(String[] args) throws Exception {
        System.out.println("Starting City E-Scooter Web API...");

        // 1. Setup Infrastructure: Database Connection
        String jdbcUrl = "jdbc:postgresql://localhost:5432/scooter_db";
        Connection connection = DriverManager.getConnection(jdbcUrl, "scooter_user", "scooter_password");

        // 2. Instantiate implementations
        RideRepository rideRepository = new PostgresRideRepository(connection);
        PaymentGateway paymentGateway = new StripeGateway();

        // 3. Inject dependencies into Core Service
        RideService rideService = new RideService(rideRepository, paymentGateway);

        // 4. Configure Jackson for Javalin
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // 5. Start Javalin
        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/public");
            config.jsonMapper(new JavalinJackson(objectMapper, false));
        }).start(7070);

        // --- API Routes ---

        // List all rides
        app.get("/api/rides", ctx -> {
            ctx.json(rideRepository.findAll());
        });

        // Start a ride
        app.post("/api/rides/start", ctx -> {
            Map<String, String> body = ctx.bodyAsClass(Map.class);
            String userId = body.get("userId");
            if (userId == null || userId.isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST).result("userId is required");
                return;
            }
            String rideId = rideService.startRide(userId);
            ctx.status(HttpStatus.CREATED).json(Map.of("rideId", rideId));
        });

        // End a ride
        app.post("/api/rides/end", ctx -> {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            String rideId = (String) body.get("rideId");
            Number distance = (Number) body.get("distance");

            if (rideId == null || distance == null) {
                ctx.status(HttpStatus.BAD_REQUEST).result("rideId and distance are required");
                return;
            }

            try {
                rideService.endRide(rideId, distance.doubleValue());
                ctx.status(HttpStatus.OK).result("Ride ended successfully");
            } catch (Exception e) {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).result(e.getMessage());
            }
        });

        System.out.println("Web Interface available at: http://localhost:7070");

        // Runtime shutdown hook to close connection
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                connection.close();
                System.out.println("Database connection closed.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
    }
}
