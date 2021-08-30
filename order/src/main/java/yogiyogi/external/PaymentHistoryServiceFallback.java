package yogiyogi.external;


public class PaymentHistoryServiceFallback implements PaymentHistoryService {
    @Override
    public void pay(PaymentHistory paymentHistory)
    {
        System.out.println("Circuit breaker has been opened. Fallback returned instead.");
    }

}
