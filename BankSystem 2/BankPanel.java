import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** Swing GUI with four tabs: Customers, Accounts, Transactions, Summary. */
/** All the screens and button logic. Lives in a JPanel so it can be tested without a display. */
public class BankPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final Bank bank;
    private boolean updating = false; // stops combo listeners firing while we refill them

    // Customers tab
    private JTextField cName, cPhone, cEmail, cAddress, cSearch;
    private JLabel cIdLabel;
    private DefaultTableModel customerModel;
    private JTable customerTable;

    // Accounts tab
    private JComboBox<String> customerCombo, typeCombo, statusCombo;
    private JTextField openingField, aSearch;
    private JLabel rulesLabel;
    private DefaultTableModel accountModel;
    private JTable accountTable;

    // Transactions tab
    private JComboBox<String> fromCombo, toCombo;
    private JTextField amountField;
    private JLabel balanceLabel;
    private DefaultTableModel historyModel;

    // Summary tab
    private JTextArea summaryArea;

    public BankPanel(Bank bank) {
        super(new BorderLayout());
        this.bank = bank;

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Customers", buildCustomerTab());
        tabs.addTab("Accounts", buildAccountTab());
        tabs.addTab("Transactions", buildTransactionTab());
        tabs.addTab("Summary", buildSummaryTab());
        tabs.addChangeListener(e -> refreshAll());
        add(tabs);
        refreshAll();
    }

    // ====================== Customers tab ======================
    private JPanel buildCustomerTab() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.setBorder(BorderFactory.createTitledBorder("Customer details"));
        cIdLabel = new JLabel("(auto-generated)");
        cName = new JTextField();
        cPhone = new JTextField();
        cEmail = new JTextField();
        cAddress = new JTextField();
        addRow(form, "Customer ID", cIdLabel);
        addRow(form, "Full name", cName);
        addRow(form, "Phone (10 digits)", cPhone);
        addRow(form, "Email", cEmail);
        addRow(form, "Address", cAddress);

        JButton add = new JButton("Add Customer");
        JButton update = new JButton("Update Selected");
        JButton delete = new JButton("Delete Selected");
        JButton clear = new JButton("Clear Form");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(add); buttons.add(update); buttons.add(delete); buttons.add(clear);

        JPanel top = new JPanel(new BorderLayout());
        top.add(form, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.SOUTH);

        cSearch = new JTextField(20);
        JButton search = new JButton("Search");
        JButton showAll = new JButton("Show All");
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchBar.add(new JLabel("Search (ID / name / phone):"));
        searchBar.add(cSearch); searchBar.add(search); searchBar.add(showAll);

        customerModel = readOnlyModel("ID", "Name", "Phone", "Email", "Address", "Accounts");
        customerTable = new JTable(customerModel);
        customerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        customerTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) fillCustomerForm();
        });

        JPanel center = new JPanel(new BorderLayout());
        center.add(searchBar, BorderLayout.NORTH);
        center.add(new JScrollPane(customerTable), BorderLayout.CENTER);

        root.add(top, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);

        add.addActionListener(e -> {
            try {
                Customer c = bank.registerCustomer(cName.getText(), cPhone.getText(),
                        cEmail.getText(), cAddress.getText());
                info("Customer added with ID " + c.getCustomerId());
                clearCustomerForm();
                refreshAll();
            } catch (IllegalArgumentException ex) {
                error(ex.getMessage());
            }
        });
        update.addActionListener(e -> {
            String id = selectedCustomerId();
            if (id == null) { error("Select a customer from the table first."); return; }
            try {
                bank.updateCustomer(id, cName.getText(), cPhone.getText(), cEmail.getText(), cAddress.getText());
                info("Customer " + id + " updated.");
                refreshAll();
            } catch (InvalidTransactionException | IllegalArgumentException ex) {
                error(ex.getMessage());
            }
        });
        delete.addActionListener(e -> {
            String id = selectedCustomerId();
            if (id == null) { error("Select a customer from the table first."); return; }
            if (!confirm("Delete customer " + id + "?")) return;
            try {
                bank.removeCustomer(id);
                clearCustomerForm();
                refreshAll();
            } catch (InvalidTransactionException ex) {
                error(ex.getMessage());
            }
        });
        clear.addActionListener(e -> clearCustomerForm());
        search.addActionListener(e -> refreshCustomers());
        cSearch.addActionListener(e -> refreshCustomers());
        showAll.addActionListener(e -> { cSearch.setText(""); refreshCustomers(); });
        return root;
    }

    private String selectedCustomerId() {
        int row = customerTable.getSelectedRow();
        return row < 0 ? null : (String) customerModel.getValueAt(row, 0);
    }

    private void fillCustomerForm() {
        int row = customerTable.getSelectedRow();
        if (row < 0) return;
        cIdLabel.setText((String) customerModel.getValueAt(row, 0));
        cName.setText((String) customerModel.getValueAt(row, 1));
        cPhone.setText((String) customerModel.getValueAt(row, 2));
        cEmail.setText((String) customerModel.getValueAt(row, 3));
        cAddress.setText((String) customerModel.getValueAt(row, 4));
    }

    private void clearCustomerForm() {
        customerTable.clearSelection();
        cIdLabel.setText("(auto-generated)");
        cName.setText(""); cPhone.setText(""); cEmail.setText(""); cAddress.setText("");
    }

    private void refreshCustomers() {
        String keep = selectedCustomerId();
        customerModel.setRowCount(0);
        for (Customer c : bank.searchCustomers(cSearch.getText())) {
            customerModel.addRow(new Object[]{c.getCustomerId(), c.getName(), c.getPhone(),
                    c.getEmail(), c.getAddress(), c.getAccounts().size()});
        }
        if (keep != null) {
            for (int i = 0; i < customerModel.getRowCount(); i++) {
                if (keep.equals(customerModel.getValueAt(i, 0))) { customerTable.setRowSelectionInterval(i, i); break; }
            }
        }
    }

    // ====================== Accounts tab ======================
    private JPanel buildAccountTab() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- top: open a new account ---
        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.setBorder(BorderFactory.createTitledBorder("Open a new account"));
        customerCombo = new JComboBox<>();
        typeCombo = new JComboBox<>(new String[]{"Savings", "Current"});
        openingField = new JTextField();
        rulesLabel = new JLabel();
        addRow(form, "Customer", customerCombo);
        addRow(form, "Account type", typeCombo);
        addRow(form, "Opening balance (Rs.)", openingField);
        addRow(form, "Rules", rulesLabel);
        JButton open = new JButton("Open Account");
        JPanel openRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        openRow.add(open);
        JPanel top = new JPanel(new BorderLayout());
        top.add(form, BorderLayout.CENTER);
        top.add(openRow, BorderLayout.SOUTH);

        // --- middle: search + table ---
        aSearch = new JTextField(20);
        JButton search = new JButton("Search");
        JButton showAll = new JButton("Show All");
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchBar.add(new JLabel("Search (number / holder / type / status):"));
        searchBar.add(aSearch); searchBar.add(search); searchBar.add(showAll);

        accountModel = readOnlyModel("Account No", "Type", "Holder", "Balance (Rs.)", "Status");
        accountTable = new JTable(accountModel);
        accountTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // --- bottom: actions on the SELECTED account (two rows so nothing is ever clipped) ---
        statusCombo = new JComboBox<>(new String[]{"ACTIVE", "FROZEN", "CLOSED"});
        JButton setStatus = new JButton("Set Status");
        JButton interest = new JButton("Credit Monthly Interest");
        JButton delete = new JButton("Delete Selected Account");
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row1.add(new JLabel("Status:"));
        row1.add(statusCombo);
        row1.add(setStatus);
        row1.add(Box.createHorizontalStrut(20));
        row1.add(interest);
        row1.add(Box.createHorizontalStrut(20));
        row1.add(delete);
        JLabel hint = new JLabel("<html>Select an account in the table first. Interest (Savings only) = balance x 4% / 12, "
                + "credited once per month. Deleting needs a zero balance - close the account first. "
                + "Deleting a customer's last account also removes the customer.</html>");
        JPanel manage = new JPanel(new GridLayout(0, 1));
        manage.setBorder(BorderFactory.createTitledBorder("Manage selected account"));
        manage.add(row1);
        manage.add(hint);

        JPanel center = new JPanel(new BorderLayout());
        center.add(searchBar, BorderLayout.NORTH);
        center.add(new JScrollPane(accountTable), BorderLayout.CENTER);
        center.add(manage, BorderLayout.SOUTH);
        root.add(top, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);

        typeCombo.addActionListener(e -> updateRulesLabel());
        updateRulesLabel();

        open.addActionListener(e -> {
            if (customerCombo.getSelectedItem() == null) { error("Add a customer first."); return; }
            try {
                double opening = parseAmount(openingField.getText());
                Account a = bank.openAccount(firstWord((String) customerCombo.getSelectedItem()),
                        (String) typeCombo.getSelectedItem(), opening);
                info("Account opened: " + a.getAccountNumber());
                openingField.setText("");
                refreshAll();
                selectAccountRow(a.getAccountNumber());
            } catch (InvalidTransactionException ex) {
                error(ex.getMessage());
            }
        });
        setStatus.addActionListener(e -> {
            String accNo = selectedAccountNumber();
            if (accNo == null) { error("Select an account from the table first."); return; }
            Account.Status s = Account.Status.valueOf((String) statusCombo.getSelectedItem());
            if (s == Account.Status.CLOSED && !confirm(
                    "Closing is permanent and pays out the remaining balance. Continue?")) return;
            try {
                bank.changeStatus(accNo, s);
                info("Account " + accNo + " is now " + s);
                refreshAll();
                selectAccountRow(accNo);
            } catch (InvalidTransactionException ex) {
                error(ex.getMessage());
            }
        });
        interest.addActionListener(e -> {
            String accNo = selectedAccountNumber();
            if (accNo == null) { error("Select a Savings account in the table first."); return; }
            try {
                double credited = bank.applyInterest(accNo);
                Account acc = bank.getAccount(accNo);
                info(String.format("Interest of Rs. %,.2f credited to %s.%nNew balance: Rs. %,.2f",
                        credited, accNo, acc.getBalance()));
                refreshAll();
                selectAccountRow(accNo);
            } catch (InvalidTransactionException ex) {
                error(ex.getMessage());
            }
        });
        delete.addActionListener(e -> {
            String accNo = selectedAccountNumber();
            if (accNo == null) { error("Select an account from the table first."); return; }
            try {
                Account acc = bank.getAccount(accNo);
                String msg = "Permanently delete account " + accNo + " and its transaction history?";
                if (acc.getOwner().getAccounts().size() == 1) {
                    msg += "\n\nThis is the only account of " + acc.getOwner().getName()
                            + ", so the customer record will be deleted too.";
                }
                if (!confirm(msg)) return;
                boolean customerGone = bank.deleteAccount(accNo);
                clearCustomerForm();
                refreshAll();
                info(customerGone ? "Account " + accNo + " and its customer were deleted."
                                  : "Account " + accNo + " was deleted.");
            } catch (InvalidTransactionException ex) {
                error(ex.getMessage());
            }
        });
        search.addActionListener(e -> refreshAccounts());
        aSearch.addActionListener(e -> refreshAccounts());
        showAll.addActionListener(e -> { aSearch.setText(""); refreshAccounts(); });
        return root;
    }

    private String selectedAccountNumber() {
        int row = accountTable.getSelectedRow();
        return row < 0 ? null : (String) accountModel.getValueAt(row, 0);
    }

    private void selectAccountRow(String accNo) {
        for (int i = 0; i < accountModel.getRowCount(); i++) {
            if (accNo.equals(accountModel.getValueAt(i, 0))) {
                accountTable.setRowSelectionInterval(i, i);
                return;
            }
        }
    }

    private void updateRulesLabel() {
        if ("Savings".equals(typeCombo.getSelectedItem())) {
            rulesLabel.setText("Min opening 500, min balance 500, withdrawal limit 50,000/txn, 4% interest");
        } else {
            rulesLabel.setText("Min opening 1,000, overdraft up to 10,000, no interest");
        }
    }

    private void refreshAccounts() {
        String keep = selectedAccountNumber();
        accountModel.setRowCount(0);
        for (Account a : bank.searchAccounts(aSearch.getText())) {
            accountModel.addRow(new Object[]{a.getAccountNumber(), a.getAccountType(),
                    a.getOwner().getName(), String.format("%,.2f", a.getBalance()), a.getStatus()});
        }
        if (keep != null) selectAccountRow(keep);   // keep the user's selection after a refresh
    }

    // ====================== Transactions tab ======================
    private JPanel buildTransactionTab() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.setBorder(BorderFactory.createTitledBorder("Perform a transaction"));
        fromCombo = new JComboBox<>();
        toCombo = new JComboBox<>();
        amountField = new JTextField();
        balanceLabel = new JLabel(" ");
        addRow(form, "Account (deposit / withdraw / transfer FROM)", fromCombo);
        addRow(form, "Current balance", balanceLabel);
        addRow(form, "Destination account (transfer TO)", toCombo);
        addRow(form, "Amount (Rs.)", amountField);

        JButton deposit = new JButton("Deposit");
        JButton withdraw = new JButton("Withdraw");
        JButton transfer = new JButton("Transfer");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(deposit); buttons.add(withdraw); buttons.add(transfer);

        JPanel top = new JPanel(new BorderLayout());
        top.add(form, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.SOUTH);

        historyModel = readOnlyModel("Txn ID", "Date & Time", "Type", "Amount (Rs.)", "Balance After (Rs.)", "Description");
        JTable historyTable = new JTable(historyModel);
        JScrollPane scroll = new JScrollPane(historyTable);
        scroll.setBorder(BorderFactory.createTitledBorder("Transaction history of selected account"));

        root.add(top, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);

        fromCombo.addActionListener(e -> { if (!updating) refreshHistory(); });
        deposit.addActionListener(e -> runTransaction("deposit"));
        withdraw.addActionListener(e -> runTransaction("withdraw"));
        transfer.addActionListener(e -> runTransaction("transfer"));
        return root;
    }

    private void runTransaction(String kind) {
        if (fromCombo.getSelectedItem() == null) { error("No account available."); return; }
        String from = firstWord((String) fromCombo.getSelectedItem());
        try {
            double amount = parseAmount(amountField.getText());
            switch (kind) {
                case "deposit":
                    bank.deposit(from, amount);
                    info(String.format("Rs. %,.2f deposited into %s.", amount, from));
                    break;
                case "withdraw":
                    bank.withdraw(from, amount);
                    info(String.format("Rs. %,.2f withdrawn from %s.", amount, from));
                    break;
                default:
                    if (toCombo.getSelectedItem() == null) { error("Select a destination account."); return; }
                    String to = firstWord((String) toCombo.getSelectedItem());
                    bank.transfer(from, to, amount);
                    info(String.format("Rs. %,.2f transferred from %s to %s.", amount, from, to));
            }
            amountField.setText("");
            refreshAll();
        } catch (InsufficientFundsException ex) {
            warn(ex.getMessage());
        } catch (InvalidTransactionException ex) {
            error(ex.getMessage());
        }
    }

    private void refreshHistory() {
        historyModel.setRowCount(0);
        if (fromCombo.getSelectedItem() == null) { balanceLabel.setText(" "); return; }
        try {
            Account a = bank.getAccount(firstWord((String) fromCombo.getSelectedItem()));
            balanceLabel.setText(String.format("Rs. %,.2f   [%s]", a.getBalance(), a.getStatus()));
            for (Transaction t : a.getHistory()) {
                historyModel.addRow(new Object[]{t.getId(), t.getFormattedTime(), t.getType(),
                        String.format("%,.2f", t.getAmount()), String.format("%,.2f", t.getBalanceAfter()),
                        t.getDescription()});
            }
        } catch (InvalidTransactionException ex) {
            error(ex.getMessage());
        }
    }

    // ====================== Summary tab ======================
    private JPanel buildSummaryTab() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        summaryArea = new JTextArea();
        summaryArea.setEditable(false);
        summaryArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        JButton refresh = new JButton("Refresh Summary");
        refresh.addActionListener(e -> refreshAll());
        JButton reset = new JButton("Reset All Data");
        reset.addActionListener(e -> {
            if (!confirm("This deletes ALL customers, accounts and transactions permanently. Continue?")) return;
            bank.clearAll();
            clearCustomerForm();
            refreshAll();
        });
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(refresh);
        buttons.add(reset);
        root.add(new JScrollPane(summaryArea), BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        return root;
    }

    // ====================== Shared helpers ======================
    private void refreshAll() {
        updating = true;
        try {
            refreshCustomers();
            refreshAccounts();
            refillCombo(customerCombo, true);
            refillCombo(fromCombo, false);
            refillCombo(toCombo, false);
        } finally {
            updating = false;
        }
        refreshHistory();
        String where = bank.getStorageFile() == null ? "" : "\nData is saved automatically to:\n" + bank.getStorageFile() + "\n";
        String warn = bank.getLastSaveError() == null ? "" : "\nWARNING: could not save data: " + bank.getLastSaveError() + "\n";
        summaryArea.setText(bank.getSummary() + where + warn);
    }

    /** Refills a combo box with customers or accounts while keeping the current selection. */
    private void refillCombo(JComboBox<String> combo, boolean customers) {
        String keep = combo.getSelectedItem() == null ? null : firstWord((String) combo.getSelectedItem());
        combo.removeAllItems();
        if (customers) {
            for (Customer c : bank.getCustomers()) combo.addItem(c.toString());
        } else {
            for (Account a : bank.getAccounts()) combo.addItem(a.toString());
        }
        if (keep != null) {
            for (int i = 0; i < combo.getItemCount(); i++) {
                if (firstWord(combo.getItemAt(i)).equals(keep)) { combo.setSelectedIndex(i); break; }
            }
        }
    }

    private static String firstWord(String s) { return s.split(" ")[0]; }

    private static double parseAmount(String text) throws InvalidTransactionException {
        try {
            return Double.parseDouble(text.trim().replace(",", ""));
        } catch (NumberFormatException | NullPointerException ex) {
            throw new InvalidTransactionException("Please enter a valid numeric amount.");
        }
    }

    private static void addRow(JPanel p, String label, JComponent field) {
        p.add(new JLabel(label));
        p.add(field);
    }

    private static DefaultTableModel readOnlyModel(String... cols) {
        return new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    // Dialog hooks (protected so tests can replace them without a real display)
    protected void info(String msg) { JOptionPane.showMessageDialog(this, msg, "Success", JOptionPane.INFORMATION_MESSAGE); }
    protected void error(String msg) { JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE); }
    protected void warn(String msg) { JOptionPane.showMessageDialog(this, msg, "Insufficient funds", JOptionPane.WARNING_MESSAGE); }
    protected boolean confirm(String msg) {
        return JOptionPane.showConfirmDialog(this, msg, "Please confirm", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }
}
