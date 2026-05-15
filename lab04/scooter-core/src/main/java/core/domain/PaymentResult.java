package core.domain;

public class PaymentResult {
    private boolean successful;
    private String transactionId;
    private String message;

    public PaymentResult(boolean successful, String transactionId, String message) {
        this.successful = successful;
        this.transactionId = transactionId;
        this.message = message;
    }

    public boolean isSuccessful() { return successful; }
    public String getTransactionId() { return transactionId; }
    public String getMessage() { return message; }
}
