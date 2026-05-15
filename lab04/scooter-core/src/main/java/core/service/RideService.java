package core.service;

import core.domain.PaymentResult;
import core.domain.Ride;
import core.domain.RideStatus;
import core.interfaces.PaymentGateway;
import core.interfaces.RideRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

public class RideService {

    private final RideRepository rideRepository;
    private final PaymentGateway paymentGateway;

    // Base fare: $1.00 to unlock, $0.20 per minute
    private static final double UNLOCK_FEE = 1.00;
    private static final double PRICE_PER_MINUTE = 0.20;

    // Dependency Injection via Constructor
    public RideService(RideRepository rideRepository, PaymentGateway paymentGateway) {
        this.rideRepository = rideRepository;
        this.paymentGateway = paymentGateway;
    }

    public String startRide(String userId) {
        String rideId = UUID.randomUUID().toString();
        Ride ride = new Ride(rideId, userId, LocalDateTime.now());
        
        rideRepository.save(ride);
        System.out.println("[Core] Ride " + rideId + " started for user " + userId);
        return rideId;
    }

    public void endRide(String rideId, double distanceKm) {
        Ride ride = rideRepository.findById(rideId);
        
        if (ride == null || ride.getStatus() == RideStatus.COMPLETED) {
            throw new IllegalArgumentException("Ride invalid or already completed.");
        }

        LocalDateTime endTime = LocalDateTime.now();
        // Calculate minutes elapsed. (We use at least 1 minute for demo purposes)
        long minutes = Duration.between(ride.getStartTime(), endTime).toMinutes();
        if (minutes == 0) minutes = 1;

        double totalCost = UNLOCK_FEE + (minutes * PRICE_PER_MINUTE);

        ride.complete(endTime, distanceKm, totalCost);
        rideRepository.update(ride);

        System.out.println("[Core] Ride " + rideId + " completed. Cost calculated: $" + totalCost);

        // Charge user via Separated Interface
        PaymentResult result = paymentGateway.charge(ride.getUserId(), totalCost);

        if (result.isSuccessful()) {
            System.out.println("[Core] Payment successful: " + result.getTransactionId());
        } else {
            System.out.println("[Core] Payment failed: " + result.getMessage() + ". User balance flagged.");
            // Further domain logic for failed payments...
        }
    }
}
