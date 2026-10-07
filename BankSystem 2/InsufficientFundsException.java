/**
 * Specialised exception for withdrawals/transfers that the balance cannot cover.
 * Extends InvalidTransactionException so one catch block can handle both.
 */
public class InsufficientFundsException extends InvalidTransactionException {
    private static final long serialVersionUID = 1L;
    public InsufficientFundsException(String message) {
        super(message);
    }
}
