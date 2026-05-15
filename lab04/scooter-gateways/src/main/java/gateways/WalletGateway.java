package gateways;

import core.domain.PaymentResult;
import core.interfaces.PaymentGateway;

import java.util.UUID;

public class WalletGateway implements PaymentGateway {

    @Override
    public PaymentResult charge(String userId, double amount) {
        System.out.println("[WalletGateway] Deducting funds from internal electronic wallet...");
        System.out.printf("[WalletGateway] Deducting balance for User %s: $%.2f\n", userId, amount);

        String transactionId = "wal_" + UUID.randomUUID().toString().substring(0, 10);
        return new PaymentResult(true, transactionId, "Deducted from digital wallet");
    }
}
