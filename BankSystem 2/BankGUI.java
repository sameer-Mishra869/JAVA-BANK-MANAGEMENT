import javax.swing.JFrame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/** The application window: a thin frame around BankPanel; saves the data when closed. */
public class BankGUI extends JFrame {
    private static final long serialVersionUID = 1L;

    public BankGUI(Bank bank) {
        super("Personal Bank Account Management System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        add(new BankPanel(bank));
        setSize(1050, 720);
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { bank.save(); }
        });
    }
}
