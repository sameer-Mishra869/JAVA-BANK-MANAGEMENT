import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Central service class. Stores customers and accounts in ArrayLists and
 * exposes every operation the GUI needs (create, update, search, transact).
 */
public class Bank implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    // not saved: where to save, and the last save problem (if any)
    private transient File storageFile;
    private transient String lastSaveError;
    private final ArrayList<Customer> customers = new ArrayList<>();
    private final ArrayList<Account> accounts = new ArrayList<>();
    private int nextCustomerNo = 1;
    private int nextSavingsNo = 1001;
    private int nextCurrentNo = 5001;

    // ================= Persistence =================
    /** From now on every successful change is written to this file automatically. */
    public void setStorageFile(File file) { this.storageFile = file; }
    public File getStorageFile() { return storageFile; }
    public String getLastSaveError() { return lastSaveError; }

    /** Writes the bank to disk (does nothing if no storage file is set, e.g. in tests). */
    public void save() {
        if (storageFile == null) return;
        try {
            BankStorage.save(this, storageFile);
            lastSaveError = null;
        } catch (IOException e) {
            lastSaveError = e.getMessage();
            System.err.println("Could not save bank data: " + e);
        }
    }

    /** Called after loading: keep transaction IDs unique. */
    void afterLoad() {
        int max = 0;
        for (Account a : accounts) {
            for (Transaction t : a.getHistory()) max = Math.max(max, t.getId());
        }
        Transaction.syncCounter(max);
    }

    /** Deletes every customer, account and transaction and restarts numbering. */
    public void clearAll() {
        customers.clear();
        accounts.clear();
        nextCustomerNo = 1;
        nextSavingsNo = 1001;
        nextCurrentNo = 5001;
        save();
    }

    // ================= Customers =================
    public Customer registerCustomer(String name, String phone, String email, String address) {
        String id = String.format("C%03d", nextCustomerNo);
        Customer c = new Customer(id, name, phone, email, address); // validates input
        ensurePhoneUnique(c.getPhone(), null);
        customers.add(c);
        nextCustomerNo++;
        save();
        return c;
    }

    public void updateCustomer(String customerId, String name, String phone, String email, String address)
            throws InvalidTransactionException {
        Customer c = getCustomer(customerId);
        ensurePhoneUnique(phone == null ? "" : phone.trim(), c);
        // validate everything on a throw-away copy first, so a bad field never half-updates the record
        new Customer(customerId, name, phone, email, address);
        c.setName(name);
        c.setPhone(phone);
        c.setEmail(email);
        c.setAddress(address);
        save();
    }

    public void removeCustomer(String customerId) throws InvalidTransactionException {
        Customer c = getCustomer(customerId);
        if (!c.getAccounts().isEmpty()) {
            throw new InvalidTransactionException(String.format(
                    "%s still has %d account(s). Delete those accounts first (Accounts tab -> Delete Selected Account), then delete the customer.",
                    c.getName(), c.getAccounts().size()));
        }
        customers.remove(c);
        save();
    }

    public Customer getCustomer(String customerId) throws InvalidTransactionException {
        for (Customer c : customers) {
            if (c.getCustomerId().equalsIgnoreCase(customerId)) return c;
        }
        throw new InvalidTransactionException("Customer not found: " + customerId);
    }

    /** Case-insensitive search on ID, name or phone. Blank query returns everyone. */
    public ArrayList<Customer> searchCustomers(String query) {
        ArrayList<Customer> result = new ArrayList<>();
        String q = query == null ? "" : query.trim().toLowerCase();
        for (Customer c : customers) {
            if (q.isEmpty() || c.getCustomerId().toLowerCase().contains(q)
                    || c.getName().toLowerCase().contains(q) || c.getPhone().contains(q)) {
                result.add(c);
            }
        }
        return result;
    }

    public List<Customer> getCustomers() { return Collections.unmodifiableList(customers); }

    private void ensurePhoneUnique(String phone, Customer ignore) {
        for (Customer c : customers) {
            if (c != ignore && c.getPhone().equals(phone)) {
                throw new IllegalArgumentException("A customer with this phone number already exists.");
            }
        }
    }

    // ================= Accounts =================
    public Account openAccount(String customerId, String type, double openingBalance)
            throws InvalidTransactionException {
        Customer owner = getCustomer(customerId);
        Account acc;
        if ("Savings".equalsIgnoreCase(type)) {
            acc = new SavingsAccount("SB" + nextSavingsNo, owner, openingBalance);
            nextSavingsNo++;
        } else if ("Current".equalsIgnoreCase(type)) {
            acc = new CurrentAccount("CA" + nextCurrentNo, owner, openingBalance);
            nextCurrentNo++;
        } else {
            throw new InvalidTransactionException("Unknown account type: " + type);
        }
        accounts.add(acc);
        owner.addAccount(acc);
        save();
        return acc;
    }

    /**
     * Permanently deletes an account (and its history). Only allowed when the balance is zero
     * (close the account first - closing pays out the balance). If it was the customer's last
     * account, the customer record is deleted too.
     * @return true if the customer was removed as well
     */
    public boolean deleteAccount(String accountNumber) throws InvalidTransactionException {
        Account a = getAccount(accountNumber);
        if (Math.abs(a.getBalance()) > 0.004) {
            throw new InvalidTransactionException(String.format(
                    "Account %s still has a balance of Rs. %,.2f. Close the account first (this pays out the balance), then delete it.",
                    a.getAccountNumber(), a.getBalance()));
        }
        Customer owner = a.getOwner();
        accounts.remove(a);
        owner.removeAccount(a);
        boolean customerRemoved = false;
        if (owner.getAccounts().isEmpty()) {
            customers.remove(owner);
            customerRemoved = true;
        }
        save();
        return customerRemoved;
    }

    public Account getAccount(String accountNumber) throws InvalidTransactionException {
        for (Account a : accounts) {
            if (a.getAccountNumber().equalsIgnoreCase(accountNumber)) return a;
        }
        throw new InvalidTransactionException("Account not found: " + accountNumber);
    }

    /** Search on account number, holder name, type or status. Blank query returns all. */
    public ArrayList<Account> searchAccounts(String query) {
        ArrayList<Account> result = new ArrayList<>();
        String q = query == null ? "" : query.trim().toLowerCase();
        for (Account a : accounts) {
            if (q.isEmpty() || a.getAccountNumber().toLowerCase().contains(q)
                    || a.getOwner().getName().toLowerCase().contains(q)
                    || a.getAccountType().toLowerCase().contains(q)
                    || a.getStatus().name().toLowerCase().contains(q)) {
                result.add(a);
            }
        }
        return result;
    }

    public List<Account> getAccounts() { return Collections.unmodifiableList(accounts); }

    public void changeStatus(String accountNumber, Account.Status status) throws InvalidTransactionException {
        getAccount(accountNumber).setStatus(status);
        save();
    }

    public double applyInterest(String accountNumber) throws InvalidTransactionException {
        Account a = getAccount(accountNumber);
        if (!(a instanceof InterestBearing)) {
            throw new InvalidTransactionException("Only savings accounts earn interest.");
        }
        double before = a.getBalance();
        ((InterestBearing) a).applyMonthlyInterest();
        save();
        return Math.round((a.getBalance() - before) * 100.0) / 100.0;
    }

    // ================= Transactions =================
    public void deposit(String accountNumber, double amount) throws InvalidTransactionException {
        getAccount(accountNumber).deposit(amount);
        save();
    }

    public void withdraw(String accountNumber, double amount) throws InvalidTransactionException {
        getAccount(accountNumber).withdraw(amount);
        save();
    }

    public void transfer(String fromAcc, String toAcc, double amount) throws InvalidTransactionException {
        getAccount(fromAcc).transferTo(getAccount(toAcc), amount);
        save();
    }

    // ================= Summary =================
    public double getTotalBalance() {
        double total = 0;
        for (Account a : accounts) {
            if (a.getStatus() != Account.Status.CLOSED) total += a.getBalance();
        }
        return Math.round(total * 100.0) / 100.0;
    }

    public int countByStatus(Account.Status s) {
        int n = 0;
        for (Account a : accounts) if (a.getStatus() == s) n++;
        return n;
    }

    public int countByType(String type) {
        int n = 0;
        for (Account a : accounts) if (a.getAccountType().equalsIgnoreCase(type)) n++;
        return n;
    }

    public int getTotalTransactions() {
        int n = 0;
        for (Account a : accounts) n += a.getHistory().size();
        return n;
    }

    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("========== BANK SUMMARY ==========\n");
        sb.append(String.format("Total customers      : %d%n", customers.size()));
        sb.append(String.format("Total accounts       : %d (Savings: %d, Current: %d)%n",
                accounts.size(), countByType("Savings"), countByType("Current")));
        sb.append(String.format("Active / Frozen / Closed : %d / %d / %d%n",
                countByStatus(Account.Status.ACTIVE), countByStatus(Account.Status.FROZEN),
                countByStatus(Account.Status.CLOSED)));
        sb.append(String.format("Total balance held   : Rs. %,.2f (excluding closed accounts)%n", getTotalBalance()));
        sb.append(String.format("Transactions recorded: %d%n%n", getTotalTransactions()));
        sb.append("---------- Per-customer holdings ----------\n");
        for (Customer c : customers) {
            double total = 0;
            for (Account a : c.getAccounts()) {
                if (a.getStatus() != Account.Status.CLOSED) total += a.getBalance();
            }
            sb.append(String.format("%-6s %-22s accounts: %d   balance: Rs. %,.2f%n",
                    c.getCustomerId(), c.getName(), c.getAccounts().size(), total));
        }
        return sb.toString();
    }

    /** Sample data so the GUI is not empty on first run. */
    public void loadDemoData() {
        try {
            Customer a = registerCustomer("Sameer Mishra", "9876543210", "sameer@example.com", "Vadodara, Gujarat");
            Customer b = registerCustomer("Riya Sharma", "9123456780", "riya@example.com", "Mumbai, Maharashtra");
            Customer c = registerCustomer("Aman Verma", "9988776655", "aman@example.com", "Pune, Maharashtra");
            Account s1 = openAccount(a.getCustomerId(), "Savings", 25000);
            Account c1 = openAccount(a.getCustomerId(), "Current", 15000);
            Account s2 = openAccount(b.getCustomerId(), "Savings", 12000);
            Account s3 = openAccount(c.getCustomerId(), "Savings", 8000);
            deposit(s1.getAccountNumber(), 5000);
            withdraw(s2.getAccountNumber(), 2000);
            transfer(s1.getAccountNumber(), s2.getAccountNumber(), 3000);
            transfer(c1.getAccountNumber(), s3.getAccountNumber(), 1500);
        } catch (InvalidTransactionException e) {
            throw new IllegalStateException("Demo data failed: " + e.getMessage(), e);
        }
    }
}
