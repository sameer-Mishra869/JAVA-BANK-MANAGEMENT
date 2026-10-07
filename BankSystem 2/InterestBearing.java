/** Interface for accounts that earn interest (only SavingsAccount implements it). */
public interface InterestBearing {
    double getInterestRate();                       // annual % rate
    void applyMonthlyInterest() throws InvalidTransactionException;
}
