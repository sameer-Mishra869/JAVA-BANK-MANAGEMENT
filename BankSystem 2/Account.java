import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Abstract base class for every account type.
 * Holds the common data + operations; subclasses supply their own rules
 * (minimum balance, overdraft, limits) via abstract methods (polymorphism).
 */
public abstract class Account implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    public enum Status { ACTIVE, FROZEN, CLOSED }

    public static final double MAX_TRANSACTION_AMOUNT = 10_000_000.00;

    private final String accountNumber;
    private final Customer owner;
    private double balance;
    private Status status;
    private final ArrayList<Transaction> history = new ArrayList<>();

    protected Account(String accountNumber, Customer owner, double openingBalance)
            throws InvalidTransactionException {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new InvalidTransactionException("Account number is required.");
        }
        if (owner == null) {
            throw new InvalidTransactionException("An account must belong to a customer.");
        }
        validateAmount(openingBalance);
        if (openingBalance < getMinimumOpeningBalance()) {
            throw new InvalidTransactionException(String.format(
                    "Minimum opening balance for a %s account is Rs. %,.2f.",
                    getAccountType(), getMinimumOpeningBalance()));
        }
        this.accountNumber = accountNumber;
        this.owner = owner;
        this.balance = 0;
        this.status = Status.ACTIVE;
        credit(openingBalance, Transaction.Type.OPENING, "Account opened");
    }

    // ---------- rules each account type must define ----------
    public abstract String getAccountType();
    public abstract double getMinimumOpeningBalance();
    public abstract String describeRules();
    /** Throws if this withdrawal/transfer-out breaks the account type's rules. */
    protected abstract void checkWithdrawalRules(double amount) throws InvalidTransactionException;

    // ---------- getters ----------
    public String getAccountNumber() { return accountNumber; }
    public Customer getOwner() { return owner; }
    public double getBalance() { return balance; }
    public Status getStatus() { return status; }
    public List<Transaction> getHistory() { return Collections.unmodifiableList(history); }

    // ---------- public operations ----------
    public void deposit(double amount) throws InvalidTransactionException {
        validateAmount(amount);
        requireActive();
        credit(amount, Transaction.Type.DEPOSIT, "Cash deposit");
    }

    public void withdraw(double amount) throws InvalidTransactionException {
        validateAmount(amount);
        requireActive();
        checkWithdrawalRules(amount);
        debit(amount, Transaction.Type.WITHDRAWAL, "Cash withdrawal");
    }

    /**
     * Transfers money to another account. Every check happens BEFORE any balance
     * changes, so a failed transfer never leaves the two accounts out of sync.
     */
    public void transferTo(Account target, double amount) throws InvalidTransactionException {
        if (target == null) {
            throw new InvalidTransactionException("Destination account not found.");
        }
        if (target == this) {
            throw new InvalidTransactionException("Cannot transfer to the same account.");
        }
        validateAmount(amount);
        requireActive();
        target.requireActive();
        checkWithdrawalRules(amount);
        debit(amount, Transaction.Type.TRANSFER_OUT, "Transfer to " + target.getAccountNumber());
        target.credit(amount, Transaction.Type.TRANSFER_IN, "Transfer from " + accountNumber);
    }

    /** Changes status. Closing pays out any remaining positive balance; overdrawn accounts cannot close. */
    public void setStatus(Status newStatus) throws InvalidTransactionException {
        if (newStatus == null) {
            throw new InvalidTransactionException("Status is required.");
        }
        if (status == Status.CLOSED) {
            throw new InvalidTransactionException("A closed account cannot be changed.");
        }
        if (newStatus == Status.CLOSED) {
            if (balance < 0) {
                throw new InvalidTransactionException("Clear the overdraft before closing this account.");
            }
            if (balance > 0) {
                debit(balance, Transaction.Type.WITHDRAWAL, "Closing payout");
            }
        }
        status = newStatus;
    }

    // ---------- helpers ----------
    protected void requireActive() throws InvalidTransactionException {
        if (status != Status.ACTIVE) {
            throw new InvalidTransactionException(
                    "Account " + accountNumber + " is " + status + " - transactions are not allowed.");
        }
    }

    protected final void credit(double amount, Transaction.Type type, String note) {
        balance = round2(balance + amount);
        history.add(new Transaction(type, amount, balance, note));
    }

    protected final void debit(double amount, Transaction.Type type, String note) {
        balance = round2(balance - amount);
        history.add(new Transaction(type, amount, balance, note));
    }

    public static void validateAmount(double amount) throws InvalidTransactionException {
        if (Double.isNaN(amount) || Double.isInfinite(amount)) {
            throw new InvalidTransactionException("Invalid amount.");
        }
        if (amount <= 0) {
            throw new InvalidTransactionException("Amount must be greater than zero.");
        }
        if (amount > MAX_TRANSACTION_AMOUNT) {
            throw new InvalidTransactionException(String.format(
                    "Amount cannot exceed Rs. %,.2f.", MAX_TRANSACTION_AMOUNT));
        }
        if (Math.abs(amount * 100 - Math.round(amount * 100)) > 1e-6) {
            throw new InvalidTransactionException("Amount can have at most 2 decimal places.");
        }
    }

    protected static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    @Override
    public String toString() {
        return accountNumber + " (" + getAccountType() + ") - " + owner.getName();
    }
}
