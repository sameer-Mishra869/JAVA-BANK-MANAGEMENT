import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A bank customer. All fields are private and validated (encapsulation). */
public class Customer implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private final String customerId;
    private String name;
    private String phone;
    private String email;
    private String address;
    private final ArrayList<Account> accounts = new ArrayList<>();

    public Customer(String customerId, String name, String phone, String email, String address) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID is required.");
        }
        this.customerId = customerId;
        setName(name);
        setPhone(phone);
        setEmail(email);
        setAddress(address);
    }

    public String getCustomerId() { return customerId; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getAddress() { return address; }
    public List<Account> getAccounts() { return Collections.unmodifiableList(accounts); }

    public void setName(String name) {
        if (name == null || !name.trim().matches("[A-Za-z][A-Za-z .'-]{1,59}")) {
            throw new IllegalArgumentException("Name must be 2-60 characters (letters, spaces, . ' - only).");
        }
        this.name = name.trim();
    }

    public void setPhone(String phone) {
        if (phone == null || !phone.trim().matches("[6-9]\\d{9}")) {
            throw new IllegalArgumentException("Phone must be a valid 10-digit mobile number.");
        }
        this.phone = phone.trim();
    }

    public void setEmail(String email) {
        if (email == null || !email.trim().matches("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+")) {
            throw new IllegalArgumentException("Please enter a valid email address.");
        }
        this.email = email.trim();
    }

    public void setAddress(String address) {
        if (address == null || address.trim().length() < 5) {
            throw new IllegalArgumentException("Address must be at least 5 characters.");
        }
        this.address = address.trim();
    }

    void addAccount(Account account) { accounts.add(account); }
    void removeAccount(Account account) { accounts.remove(account); }

    @Override
    public String toString() { return customerId + " - " + name; }
}
