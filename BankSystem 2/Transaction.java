import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** One immutable entry in an account's transaction history. */
public class Transaction implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    public enum Type { OPENING, DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT, INTEREST }

    private static int counter = 1000;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private final int id;
    private final Type type;
    private final double amount;
    private final double balanceAfter;
    private final String description;
    private final LocalDateTime timestamp;

    public Transaction(Type type, double amount, double balanceAfter, String description) {
        this.id = ++counter;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.timestamp = LocalDateTime.now();
    }

    /** After loading saved data, make sure new IDs continue after the highest saved one. */
    public static void syncCounter(int highestSavedId) {
        if (highestSavedId > counter) counter = highestSavedId;
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public int getId() { return id; }
    public Type getType() { return type; }
    public double getAmount() { return amount; }
    public double getBalanceAfter() { return balanceAfter; }
    public String getDescription() { return description; }
    public String getFormattedTime() { return timestamp.format(FMT); }

    @Override
    public String toString() {
        return String.format("#%d | %s | %-12s | Rs. %,10.2f | Bal: Rs. %,10.2f | %s",
                id, getFormattedTime(), type, amount, balanceAfter, description);
    }
}
