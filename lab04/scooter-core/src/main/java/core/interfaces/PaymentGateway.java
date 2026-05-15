package core.interfaces;

import core.domain.PaymentResult;

public interface PaymentGateway {
    PaymentResult charge(String userId, double amount);
}
