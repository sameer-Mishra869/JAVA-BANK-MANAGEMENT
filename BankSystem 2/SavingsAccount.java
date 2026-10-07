import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Savings account: must keep a minimum balance, has a per-transaction
 * withdrawal limit and earns interest.
 */
public class SavingsAccount extends Account implements InterestBearing {
    private static final long serialVersionUID = 1L;
    public static final double MIN_OPENING_BALANCE = 500.00;
    public static final double MIN_BALANCE = 500.00;
    public static final double MAX_WITHDRAWAL_PER_TXN = 50_000.00;
    public static final double INTEREST_RATE = 4.0; // % per year

    public SavingsAccount(String accountNumber, Customer owner, double openingBalance)
            throws InvalidTransactionException {
        super(accountNumber, owner, openingBalance);
    }

    @Override public String getAccountType() { return "Savings"; }
    @Override public double getMinimumOpeningBalance() { return MIN_OPENING_BALANCE; }

    @Override
    public String describeRules() {
        return String.format("Min opening Rs. %,.0f | Min balance Rs. %,.0f | Max withdrawal/txn Rs. %,.0f | Interest %.1f%% p.a.",
                MIN_OPENING_BALANCE, MIN_BALANCE, MAX_WITHDRAWAL_PER_TXN, INTEREST_RATE);
    }

    @Override
    protected void checkWithdrawalRules(double amount) throws InvalidTransactionException {
        if (amount > MAX_WITHDRAWAL_PER_TXN) {
            throw new InvalidTransactionException(String.format(
                    "Savings withdrawal limit per transaction is Rs. %,.2f.", MAX_WITHDRAWAL_PER_TXN));
        }
        if (getBalance() - amount < MIN_BALANCE) {
            throw new InsufficientFundsException(String.format(
                    "Insufficient funds. Savings account must keep a minimum balance of Rs. %,.2f (available to withdraw: Rs. %,.2f).",
                    MIN_BALANCE, Math.max(0, getBalance() - MIN_BALANCE)));
        }
    }

    @Override public double getInterestRate() { return INTEREST_RATE; }

    @Override
    public void applyMonthlyInterest() throws InvalidTransactionException {
        requireActive();
        YearMonth now = YearMonth.now();
        for (Transaction t : getHistory()) {
            if (t.getType() == Transaction.Type.INTEREST && YearMonth.from(t.getTimestamp()).equals(now)) {
                throw new InvalidTransactionException(String.format(
                        "Interest for %s %d has already been credited to %s. Interest is credited once per month.",
                        now.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH), now.getYear(),
                        getAccountNumber()));
            }
        }
        double interest = round2(getBalance() * INTEREST_RATE / 100.0 / 12.0);
        if (interest <= 0) {
            throw new InvalidTransactionException("No interest to credit.");
        }
        credit(interest, Transaction.Type.INTEREST, "Monthly interest @ " + INTEREST_RATE + "% p.a.");
    }
}
