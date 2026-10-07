/**
 * Checked exception thrown when a banking operation breaks a business rule
 * (bad amount, inactive account, unknown account, etc.).
 */
public class InvalidTransactionException extends Exception {
    private static final long serialVersionUID = 1L;
    public InvalidTransactionException(String message) {
        super(message);
    }
}
