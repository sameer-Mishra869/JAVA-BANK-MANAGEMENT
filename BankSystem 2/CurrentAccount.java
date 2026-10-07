/** Current account: no minimum balance, but allows an overdraft up to a fixed limit. */
public class CurrentAccount extends Account {
    private static final long serialVersionUID = 1L;
    public static final double MIN_OPENING_BALANCE = 1_000.00;
    public static final double OVERDRAFT_LIMIT = 10_000.00;

    public CurrentAccount(String accountNumber, Customer owner, double openingBalance)
            throws InvalidTransactionException {
        super(accountNumber, owner, openingBalance);
    }

    @Override public String getAccountType() { return "Current"; }
    @Override public double getMinimumOpeningBalance() { return MIN_OPENING_BALANCE; }

    @Override
    public String describeRules() {
        return String.format("Min opening Rs. %,.0f | Overdraft limit Rs. %,.0f | No interest",
                MIN_OPENING_BALANCE, OVERDRAFT_LIMIT);
    }

    @Override
    protected void checkWithdrawalRules(double amount) throws InvalidTransactionException {
        if (getBalance() - amount < -OVERDRAFT_LIMIT) {
            throw new InsufficientFundsException(String.format(
                    "Insufficient funds. Overdraft limit of Rs. %,.2f would be exceeded (available: Rs. %,.2f).",
                    OVERDRAFT_LIMIT, getBalance() + OVERDRAFT_LIMIT));
        }
    }
}
