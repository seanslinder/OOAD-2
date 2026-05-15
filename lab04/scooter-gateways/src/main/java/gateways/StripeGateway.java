package gateways;

import core.domain.PaymentResult;
import core.interfaces.PaymentGateway;

import java.util.UUID;

public class StripeGateway implements PaymentGateway {

    @Override
    public PaymentResult charge(String userId, double amount) {
        System.out.println("[StripeGateway] Negotiating secure connection to Stripe API...");
        System.out.printf("[StripeGateway] Attempting to charge credit card for User %s: $%.2f\n", userId, amount);

        // Simulate API call delay and response
        String transactionId = "ch_" + UUID.randomUUID().toString().substring(0, 10);
        return new PaymentResult(true, transactionId, "Charge successful via Stripe");
    }
}
