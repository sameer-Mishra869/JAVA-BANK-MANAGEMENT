import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Drives the real BankPanel by "clicking" its buttons (works without a display).
 * Dialogs are replaced by recording hooks so we can check the messages the user would see.
 * Run: java -Djava.awt.headless=true GuiTest
 */
public class GuiTest {
    static final List<String> msgs = new ArrayList<>();
    static int ok = 0, bad = 0;

    static void check(String n, boolean c) {
        if (c) ok++; else bad++;
        System.out.println((c ? "PASS  " : "FAIL  ") + n);
    }
    static <T extends Component> List<T> all(Container root, Class<T> type) {
        List<T> out = new ArrayList<>();
        collect(root, type, out);
        return out;
    }
    static <T extends Component> void collect(Container c, Class<T> type, List<T> out) {
        for (Component k : c.getComponents()) {
            if (type.isInstance(k)) out.add(type.cast(k));
            if (k instanceof Container) collect((Container) k, type, out);
        }
    }
    static void click(Container root, String text) {
        for (JButton b : all(root, JButton.class)) {
            if (b.getText().equals(text)) { b.doClick(0); return; }
        }
        throw new RuntimeException("No button: " + text);
    }
    static boolean said(String part) {
        for (String m : msgs) if (m.contains(part)) return true;
        return false;
    }

    public static void main(String[] args) throws Exception {
        File file = File.createTempFile("gui_test", ".ser");
        file.delete();
        Bank bank = new Bank();
        bank.loadDemoData();
        bank.setStorageFile(file);
        bank.save();

        BankPanel p = new BankPanel(bank) {
            @Override protected void info(String m) { msgs.add("INFO: " + m); }
            @Override protected void error(String m) { msgs.add("ERROR: " + m); }
            @Override protected void warn(String m) { msgs.add("WARN: " + m); }
            @Override protected boolean confirm(String m) { msgs.add("CONFIRM: " + m); return true; }
        };

        JTabbedPane tabs = all(p, JTabbedPane.class).get(0);
        List<JTable> tables = all(p, JTable.class);
        JTable customers = tables.get(0), accounts = tables.get(1), history = tables.get(2);
        List<JTextField> tf = all(p, JTextField.class);   // 0-3 customer form, 4 search, 5 opening bal, 6 search, 7 amount
        @SuppressWarnings("unchecked")
        List<JComboBox<String>> combos = (List<JComboBox<String>>) (List<?>) all(p, JComboBox.class); // 0 cust,1 type,2 status,3 from,4 to

        // ---- Customers tab ----
        tf.get(0).setText("Test Person"); tf.get(1).setText("9000011122");
        tf.get(2).setText("tp@example.com"); tf.get(3).setText("Surat, Gujarat");
        click(p, "Add Customer");
        check("customer added via GUI", bank.searchCustomers("Test Person").size() == 1);
        String cid = bank.searchCustomers("Test Person").get(0).getCustomerId();

        msgs.clear();
        tf.get(1).setText("123");
        click(p, "Add Customer");
        check("invalid phone shows error", said("ERROR:") && bank.getCustomers().size() == 4);

        // update: select row, change address
        int row = -1;
        for (int i = 0; i < customers.getRowCount(); i++) if (cid.equals(customers.getValueAt(i, 0))) row = i;
        customers.setRowSelectionInterval(row, row);
        check("selecting a row fills the form", tf.get(0).getText().equals("Test Person"));
        tf.get(3).setText("Rajkot, Gujarat");
        click(p, "Update Selected");
        check("customer updated via GUI", bank.getCustomer(cid).getAddress().equals("Rajkot, Gujarat"));

        // ---- open savings account ----
        tabs.setSelectedIndex(1);
        for (int i = 0; i < combos.get(0).getItemCount(); i++)
            if (combos.get(0).getItemAt(i).startsWith(cid)) combos.get(0).setSelectedIndex(i);
        tf.get(5).setText("20000");
        click(p, "Open Account");
        Account acc = bank.getCustomer(cid).getAccounts().get(0);
        check("account opened via GUI", acc.getBalance() == 20000.0);
        check("new account row is selected", accounts.getSelectedRow() >= 0
                && acc.getAccountNumber().equals(accounts.getValueAt(accounts.getSelectedRow(), 0)));

        // ---- interest button ----
        msgs.clear();
        click(p, "Credit Monthly Interest");
        check("interest credited (20000 x 4% / 12 = 66.67)", Math.abs(acc.getBalance() - 20066.67) < 0.001);
        check("interest message shows the amount", said("66.67"));
        check("row still selected after interest", accounts.getSelectedRow() >= 0);
        msgs.clear();
        click(p, "Credit Monthly Interest");
        check("second click: clear message, no double credit",
                Math.abs(acc.getBalance() - 20066.67) < 0.001 && said("already been credited"));
        accounts.clearSelection();
        msgs.clear();
        click(p, "Credit Monthly Interest");
        check("no row selected: tells user to select", said("Select a Savings account"));

        // ---- transactions tab ----
        tabs.setSelectedIndex(2);
        for (int i = 0; i < combos.get(3).getItemCount(); i++)
            if (combos.get(3).getItemAt(i).startsWith(acc.getAccountNumber())) combos.get(3).setSelectedIndex(i);
        check("history table lists the account's transactions", history.getRowCount() == acc.getHistory().size());
        boolean hasInterestRow = false;
        for (int i = 0; i < history.getRowCount(); i++) if ("INTEREST".equals(String.valueOf(history.getValueAt(i, 2)))) hasInterestRow = true;
        check("history shows the INTEREST row", hasInterestRow);
        tf.get(7).setText("1000");
        click(p, "Deposit");
        check("deposit via GUI", Math.abs(acc.getBalance() - 21066.67) < 0.001);
        tf.get(7).setText("500");
        click(p, "Withdraw");
        check("withdraw via GUI", Math.abs(acc.getBalance() - 20566.67) < 0.001);
        msgs.clear();
        tf.get(7).setText("60000");
        click(p, "Withdraw");
        check("over-limit withdrawal blocked with message", said("ERROR:") || said("WARN:"));
        msgs.clear();
        tf.get(7).setText("abc");
        click(p, "Deposit");
        check("non-numeric amount handled", said("valid numeric amount"));
        // transfer to demo account SB1001
        for (int i = 0; i < combos.get(4).getItemCount(); i++)
            if (combos.get(4).getItemAt(i).startsWith("SB1001")) combos.get(4).setSelectedIndex(i);
        double before = bank.getAccount("SB1001").getBalance();
        tf.get(7).setText("100");
        click(p, "Transfer");
        check("transfer via GUI", bank.getAccount("SB1001").getBalance() == before + 100
                && Math.abs(acc.getBalance() - 20466.67) < 0.001);

        // ---- delete rules ----
        tabs.setSelectedIndex(1);
        for (int i = 0; i < accounts.getRowCount(); i++)
            if (acc.getAccountNumber().equals(accounts.getValueAt(i, 0))) accounts.setRowSelectionInterval(i, i);
        msgs.clear();
        click(p, "Delete Selected Account");
        check("delete refused while balance > 0", bank.getAccounts().contains(acc) && said("Close the account first"));
        check("last-account warning shown in confirmation", said("customer record will be deleted"));

        combos.get(2).setSelectedItem("CLOSED");
        click(p, "Set Status");
        check("close pays out balance to zero", acc.getStatus() == Account.Status.CLOSED && acc.getBalance() == 0);
        msgs.clear();
        click(p, "Delete Selected Account");
        check("closed account deleted", !bank.getAccounts().contains(acc));
        check("customer deleted automatically with last account", bank.searchCustomers("Test Person").isEmpty());
        check("user told both were deleted", said("and its customer were deleted"));

        // ---- customer with accounts cannot be deleted ----
        tabs.setSelectedIndex(0);
        customers.setRowSelectionInterval(0, 0);
        msgs.clear();
        int n = bank.getCustomers().size();
        click(p, "Delete Selected");
        check("customer with accounts: refused with instructions", bank.getCustomers().size() == n && said("Delete those accounts first"));

        // ---- everything saved automatically ----
        Bank reloaded = BankStorage.load(file);
        check("auto-save file equals live data",
                reloaded.getCustomers().size() == bank.getCustomers().size()
                && reloaded.getAccounts().size() == bank.getAccounts().size()
                && reloaded.getTotalBalance() == bank.getTotalBalance());

        // ---- reset ----
        click(p, "Reset All Data");
        check("reset clears everything", bank.getCustomers().isEmpty() && bank.getAccounts().isEmpty());
        check("reset is saved too", BankStorage.load(file).getCustomers().isEmpty());

        System.out.println("\nGUI result: " + ok + " passed, " + bad + " failed");
        file.delete();
        System.exit(bad == 0 ? 0 : 1);
    }
}
