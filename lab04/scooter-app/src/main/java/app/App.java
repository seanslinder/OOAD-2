package app;

import core.interfaces.PaymentGateway;
import core.interfaces.RideRepository;
import core.service.RideService;
import gateways.StripeGateway;
import gateways.WalletGateway;
import postgres.PostgresRideRepository;

import java.sql.Connection;
import java.sql.DriverManager;

public class App {

    public static void main(String[] args) throws Exception {
        System.out.println("Starting City E-Scooter API...");

        // 1. Setup Infrastructure: Database Connection
        // (Make sure docker-compose is running before launching this)
        String jdbcUrl = "jdbc:postgresql://localhost:5432/scooter_db";
        Connection connection = DriverManager.getConnection(jdbcUrl, "scooter_user", "scooter_password");

        // 2. Instantiate implementations of the Separated Interfaces
        RideRepository rideRepository = new PostgresRideRepository(connection);
        
        // We can swap PaymentGateways seamlessly
        PaymentGateway stripeGateway = new StripeGateway();
        PaymentGateway walletGateway = new WalletGateway();

        // 3. Inject dependencies into Core Service
        RideService rideServiceWithStripe = new RideService(rideRepository, stripeGateway);
        RideService rideServiceWithWallet = new RideService(rideRepository, walletGateway);

        // 4. Simulate a user journey (Using Stripe)
        System.out.println("\n--- User 1: Ride simulation (Stripe Gateway) ---");
        String rideId1 = rideServiceWithStripe.startRide("USR-ALICE-99");
        
        // Simulate time passing (Sleep 1.5 seconds)
        Thread.sleep(1500);
        
        rideServiceWithStripe.endRide(rideId1, 2.5); // Distance 2.5km

        // 5. Simulate another user journey (Using Wallet)
        System.out.println("\n--- User 2: Ride simulation (Internal Wallet Gateway) ---");
        String rideId2 = rideServiceWithWallet.startRide("USR-BOB-42");
        Thread.sleep(1000);
        rideServiceWithWallet.endRide(rideId2, 1.2);

        // Cleanup
        connection.close();
        System.out.println("\nApp Finished.");
    }
}
