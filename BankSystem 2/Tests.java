/** Console test-runner (no GUI) that proves every business rule works. Run: java Tests */
public class Tests {
    static int passed = 0, failed = 0;

    static void check(String name, boolean ok) {
        if (ok) { passed++; System.out.println("PASS  " + name); }
        else    { failed++; System.out.println("FAIL  " + name); }
    }

    interface Action { void run() throws Exception; }

    static boolean throwsType(Class<? extends Exception> type, Action a) {
        try { a.run(); return false; }
        catch (Exception e) { return type.isInstance(e); }
    }

    public static void main(String[] args) throws Exception {
        Bank bank = new Bank();
        Customer c1 = bank.registerCustomer("Sameer Mishra", "9876543210", "s@example.com", "Vadodara, Gujarat");
        Customer c2 = bank.registerCustomer("Riya Sharma", "9123456780", "r@example.com", "Mumbai, Maharashtra");

        // ---- customer validation ----
        check("bad phone rejected", throwsType(IllegalArgumentException.class,
                () -> bank.registerCustomer("Test User", "12345", "t@example.com", "Some address")));
        check("bad email rejected", throwsType(IllegalArgumentException.class,
                () -> bank.registerCustomer("Test User", "9000000001", "not-an-email", "Some address")));
        check("bad name rejected", throwsType(IllegalArgumentException.class,
                () -> bank.registerCustomer("12", "9000000002", "t@example.com", "Some address")));
        check("duplicate phone rejected", throwsType(IllegalArgumentException.class,
                () -> bank.registerCustomer("Other Person", "9876543210", "o@example.com", "Some address")));
        check("failed registration did not consume an ID", bank.getCustomers().size() == 2);
        check("search by name", bank.searchCustomers("riya").size() == 1);
        check("search by phone", bank.searchCustomers("98765").size() == 1);
        check("blank search returns all", bank.searchCustomers("").size() == 2);
        bank.updateCustomer("C001", "Sameer M Mishra", "9876543210", "s2@example.com", "Vadodara, Gujarat");
        check("update customer works", bank.getCustomer("C001").getName().equals("Sameer M Mishra"));
        check("bad update leaves record unchanged", throwsType(IllegalArgumentException.class,
                () -> bank.updateCustomer("C001", "Valid Name", "bad", "s2@example.com", "Vadodara, Gujarat"))
                && bank.getCustomer("C001").getName().equals("Sameer M Mishra"));

        // ---- opening accounts ----
        check("savings below min opening rejected", throwsType(InvalidTransactionException.class,
                () -> bank.openAccount("C001", "Savings", 100)));
        check("current below min opening rejected", throwsType(InvalidTransactionException.class,
                () -> bank.openAccount("C001", "Current", 500)));
        check("unknown customer rejected", throwsType(InvalidTransactionException.class,
                () -> bank.openAccount("C999", "Savings", 1000)));
        check("unknown type rejected", throwsType(InvalidTransactionException.class,
                () -> bank.openAccount("C001", "Fixed", 1000)));
        Account sb = bank.openAccount("C001", "Savings", 10000);
        Account ca = bank.openAccount("C001", "Current", 5000);
        Account sb2 = bank.openAccount("C002", "Savings", 2000);
        check("account numbers generated", sb.getAccountNumber().equals("SB1001") && ca.getAccountNumber().equals("CA5001"));
        check("polymorphism: types", sb instanceof SavingsAccount && ca instanceof CurrentAccount);

        // ---- deposit ----
        bank.deposit("SB1001", 500.50);
        check("deposit increases balance", sb.getBalance() == 10500.50);
        check("zero deposit rejected", throwsType(InvalidTransactionException.class, () -> bank.deposit("SB1001", 0)));
        check("negative deposit rejected", throwsType(InvalidTransactionException.class, () -> bank.deposit("SB1001", -5)));
        check("NaN deposit rejected", throwsType(InvalidTransactionException.class, () -> bank.deposit("SB1001", Double.NaN)));
        check("3-decimal amount rejected", throwsType(InvalidTransactionException.class, () -> bank.deposit("SB1001", 10.123)));
        check("huge amount rejected", throwsType(InvalidTransactionException.class, () -> bank.deposit("SB1001", 99_999_999)));
        check("unknown account rejected", throwsType(InvalidTransactionException.class, () -> bank.deposit("XX0000", 100)));

        // ---- withdraw: savings ----
        bank.withdraw("SB1001", 500.50);
        check("withdraw decreases balance", sb.getBalance() == 10000.00);
        check("savings min balance enforced", throwsType(InsufficientFundsException.class, () -> bank.withdraw("SB1001", 9600)));
        bank.withdraw("SB1001", 9500);
        check("can withdraw down to exactly min balance", sb.getBalance() == 500.00);
        check("failed withdrawals leave balance unchanged", sb.getBalance() == 500.00);
        bank.deposit("SB1001", 100000);
        check("savings per-txn limit enforced", throwsType(InvalidTransactionException.class, () -> bank.withdraw("SB1001", 60000)));

        // ---- withdraw: current/overdraft ----
        bank.withdraw("CA5001", 14000);   // 5000 - 14000 = -9000, inside the 10,000 overdraft
        check("overdraft allowed in current account", ca.getBalance() == -9000.00);
        check("overdraft limit enforced", throwsType(InsufficientFundsException.class, () -> bank.withdraw("CA5001", 1500)));
        check("InsufficientFunds is an InvalidTransaction", throwsType(InvalidTransactionException.class, () -> bank.withdraw("CA5001", 1500)));
        bank.deposit("CA5001", 9000);

        // ---- transfers ----
        double beforeFrom = sb.getBalance(), beforeTo = sb2.getBalance();
        bank.transfer("SB1001", "SB1002", 1000);
        check("transfer debits source", sb.getBalance() == beforeFrom - 1000);
        check("transfer credits target", sb2.getBalance() == beforeTo + 1000);
        check("transfer to same account rejected", throwsType(InvalidTransactionException.class, () -> bank.transfer("SB1001", "SB1001", 10)));
        check("transfer to unknown account rejected", throwsType(InvalidTransactionException.class, () -> bank.transfer("SB1001", "ZZ1", 10)));
        double f2 = sb2.getBalance(), t2 = sb.getBalance();
        check("transfer with insufficient funds rejected", throwsType(InsufficientFundsException.class, () -> bank.transfer("SB1002", "SB1001", 5000)));
        check("failed transfer changes nothing", sb2.getBalance() == f2 && sb.getBalance() == t2);

        // ---- status rules ----
        bank.changeStatus("SB1002", Account.Status.FROZEN);
        check("frozen account cannot deposit", throwsType(InvalidTransactionException.class, () -> bank.deposit("SB1002", 100)));
        check("frozen account cannot withdraw", throwsType(InvalidTransactionException.class, () -> bank.withdraw("SB1002", 100)));
        double src = sb.getBalance();
        check("transfer into frozen account rejected", throwsType(InvalidTransactionException.class, () -> bank.transfer("SB1001", "SB1002", 100)));
        check("rejected transfer left source unchanged", sb.getBalance() == src);
        bank.changeStatus("SB1002", Account.Status.ACTIVE);
        bank.deposit("SB1002", 100);
        check("reactivated account works again", true);
        check("cannot close overdrawn account", throwsType(InvalidTransactionException.class, () -> {
            bank.withdraw("CA5001", 10000); bank.changeStatus("CA5001", Account.Status.CLOSED); }));
        bank.deposit("CA5001", 10000);
        bank.changeStatus("CA5001", Account.Status.CLOSED);
        check("closing pays out balance to zero", ca.getBalance() == 0 && ca.getStatus() == Account.Status.CLOSED);
        check("closed account cannot be reopened", throwsType(InvalidTransactionException.class, () -> bank.changeStatus("CA5001", Account.Status.ACTIVE)));
        check("closed account cannot transact", throwsType(InvalidTransactionException.class, () -> bank.deposit("CA5001", 100)));

        // ---- interest ----
        double bal = sb2.getBalance();
        bank.applyInterest("SB1002");
        check("interest credited to savings", sb2.getBalance() > bal);
        check("current account earns no interest", throwsType(InvalidTransactionException.class, () -> bank.applyInterest("CA5001")));

        check("second interest in same month rejected", throwsType(InvalidTransactionException.class, () -> bank.applyInterest("SB1002")));

        // ---- history ----
        boolean ids = true;
        for (int i = 1; i < sb.getHistory().size(); i++) {
            if (sb.getHistory().get(i).getId() <= sb.getHistory().get(i - 1).getId()) ids = false;
        }
        check("history recorded with increasing IDs", sb.getHistory().size() >= 6 && ids);
        check("history is read-only", throwsType(UnsupportedOperationException.class, () -> sb.getHistory().clear()));
        check("last history balance equals account balance",
                sb.getHistory().get(sb.getHistory().size() - 1).getBalanceAfter() == sb.getBalance());

        // ---- delete customer ----
        check("customer with accounts cannot be deleted", throwsType(InvalidTransactionException.class, () -> bank.removeCustomer("C001")));
        Customer c3 = bank.registerCustomer("Aman Verma", "9988776655", "a@example.com", "Pune, Maharashtra");
        bank.removeCustomer(c3.getCustomerId());
        check("customer without accounts can be deleted", bank.getCustomers().size() == 2);

        // ---- delete account (+ customer auto-removed with last account) ----
        Customer c4 = bank.registerCustomer("Neha Kapoor", "9811122233", "n@example.com", "Delhi, India");
        Account n1 = bank.openAccount(c4.getCustomerId(), "Savings", 3000);
        Account n2 = bank.openAccount(c4.getCustomerId(), "Current", 2000);
        check("customer with accounts: delete refused", throwsType(InvalidTransactionException.class, () -> bank.removeCustomer(c4.getCustomerId())));
        check("account with balance cannot be deleted", throwsType(InvalidTransactionException.class, () -> bank.deleteAccount(n1.getAccountNumber())));
        bank.changeStatus(n1.getAccountNumber(), Account.Status.CLOSED);
        check("closed account deleted, customer stays (other account left)",
                !bank.deleteAccount(n1.getAccountNumber()) && bank.searchCustomers("Neha").size() == 1
                && c4.getAccounts().size() == 1);
        check("deleted account no longer found", throwsType(InvalidTransactionException.class, () -> bank.getAccount(n1.getAccountNumber())));
        bank.changeStatus(n2.getAccountNumber(), Account.Status.CLOSED);
        check("deleting last account removes the customer too",
                bank.deleteAccount(n2.getAccountNumber()) && bank.searchCustomers("Neha").isEmpty());

        // ---- persistence round-trip ----
        java.io.File tmp = java.io.File.createTempFile("bank_test", ".ser");
        tmp.delete();
        bank.setStorageFile(tmp);
        bank.save();
        check("save file written", tmp.exists() && bank.getLastSaveError() == null);
        Bank back = BankStorage.load(tmp);
        check("loaded bank is not null", back != null);
        check("customers restored", back.getCustomers().size() == bank.getCustomers().size());
        check("accounts restored", back.getAccounts().size() == bank.getAccounts().size());
        check("balance restored", back.getAccount("SB1001").getBalance() == bank.getAccount("SB1001").getBalance());
        check("history restored", back.getAccount("SB1001").getHistory().size() == bank.getAccount("SB1001").getHistory().size());
        check("account type restored", back.getAccount("CA5001") instanceof CurrentAccount
                && back.getAccount("CA5001").getStatus() == Account.Status.CLOSED);
        check("owner link restored", back.getAccount("SB1001").getOwner() == back.getCustomer("C001"));
        int newId = back.getAccount("SB1001").getHistory().get(back.getAccount("SB1001").getHistory().size() - 1).getId();
        back.deposit("SB1001", 10);
        check("new transaction IDs continue after loaded ones",
                back.getAccount("SB1001").getHistory().get(back.getAccount("SB1001").getHistory().size() - 1).getId() > newId);
        back.setStorageFile(tmp);
        Account fresh = back.openAccount("C002", "Savings", 700);
        check("numbering continues after reload", fresh.getAccountNumber().equals("SB1004"));
        back.save();
        check("reloaded interest-month rule survives", throwsType(InvalidTransactionException.class, () -> BankStorage.load(tmp).applyInterest("SB1002")));
        try (java.io.FileWriter w = new java.io.FileWriter(tmp)) { w.write("garbage"); }
        check("corrupt file -> null, not a crash", BankStorage.load(tmp) == null);
        new java.io.File(tmp.getPath() + ".corrupt").delete();
        tmp.delete();

        // ---- summary + demo data ----
        check("summary text produced", bank.getSummary().contains("BANK SUMMARY"));
        Bank demo = new Bank();
        demo.loadDemoData();
        check("demo data loads", demo.getCustomers().size() == 3 && demo.getAccounts().size() == 4);

        System.out.println("\nResult: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}
