import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.io.File;

/** Entry point: loads saved data (or demo data on the very first run) and opens the GUI. */
public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // fall back to the default look and feel
        }
        File file = BankStorage.defaultFile();
        Bank loaded = BankStorage.load(file);
        final Bank bank;
        if (loaded != null) {
            bank = loaded;
        } else {
            bank = new Bank();
            bank.loadDemoData();      // only on the first run, when no save file exists
        }
        bank.setStorageFile(file);
        bank.save();
        SwingUtilities.invokeLater(() -> new BankGUI(bank).setVisible(true));
    }
}
