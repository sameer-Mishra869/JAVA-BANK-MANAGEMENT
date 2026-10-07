# Case Study 111 - Personal Bank Account Management System (Java)

## How to run
```
javac *.java          # compile
java Main             # start the GUI (loads 3 demo customers / 4 accounts)
java Tests            # business-logic tests (73 checks)
java -Djava.awt.headless=true GuiTest   # clicks every GUI button automatically (28 checks)
```
Data is saved automatically to `~/BankSystemData/bank_data.ser` (your user folder) after every change, and loaded again on the next start. Demo data is created only on the very first run. Use the "Reset All Data" button (Summary tab) to start clean.
Needs JDK 11+ (tested on JDK 21). No external libraries.

## File map
| File | Role |
|---|---|
| Main.java | Entry point, loads demo data, opens GUI |
| BankGUI.java | Application window (JFrame), saves data on close |
| BankPanel.java | All Swing screens: Customers, Accounts, Transactions, Summary tabs |
| BankStorage.java | Saves/loads the whole Bank to a file (Java serialization) |
| Bank.java | Service class: ArrayLists of customers/accounts, search, transactions, summary |
| Customer.java | Customer entity with validated private fields |
| Account.java | Abstract base class: balance, status, history, deposit/withdraw/transfer |
| SavingsAccount.java | Min balance, withdrawal limit, interest |
| CurrentAccount.java | Overdraft allowed up to a limit |
| InterestBearing.java | Interface implemented by SavingsAccount |
| Transaction.java | One history record (id, time, type, amount, balance after) |
| InvalidTransactionException.java | Business-rule violation |
| InsufficientFundsException.java | Subclass: balance cannot cover the amount |
| Tests.java | Console tests for every rule |
| GuiTest.java | Automated GUI test (simulated button clicks) |

## Objectives -> where and why

**Apply OOP to account types**
- `Account` is abstract (shared data and behaviour once); `SavingsAccount` and `CurrentAccount` extend it and override `checkWithdrawalRules()`, so `account.withdraw()` behaves differently per type = inheritance + polymorphism + abstraction.
- Justification: new account types can be added without touching existing code.

**Encapsulation**
- Every field is `private`; balance only changes through `deposit/withdraw/transferTo`. `getHistory()` returns an unmodifiable list so outside code cannot edit history.
- Justification: a bank balance must never be changed without validation and a history entry.

**Constructors**
- `Account` and `Customer` constructors validate their input and refuse to create invalid objects (e.g. below minimum opening balance, bad phone/email).

**ArrayList**
- `Bank` keeps `ArrayList<Customer>` and `ArrayList<Account>`; each `Account` keeps an `ArrayList<Transaction>`.
- Justification: size is unknown in advance, insertion order = chronological order, and linear search is fine at this scale.

**Process deposits and withdrawals**
- `deposit()` / `withdraw()` validate amount > 0, <= 10,000,000, max 2 decimals, account ACTIVE, then apply account-type rules.

**Transfer funds safely**
- `transferTo()` runs every check (amount, both accounts active, source rules) BEFORE touching any balance, so a failed transfer never leaves money debited but not credited.

**Maintain transaction history**
- Every balance change (opening, deposit, withdrawal, transfer in/out, interest, closing payout) creates a `Transaction` with balance-after. The Transactions tab shows it per account.

**Handle invalid transactions**
- Custom checked exceptions: `InvalidTransactionException` and `InsufficientFundsException` (a subclass). The GUI catches them and shows a clear message instead of crashing. Customer input errors use `IllegalArgumentException`; non-numeric amounts are handled too.

**Record creation, update, search, status management**
- Customers: add / update / delete (only if no accounts) / search by ID, name or phone.
- Accounts: open / search by number, holder, type, status / status ACTIVE, FROZEN, CLOSED (closing pays out the balance, overdrawn accounts cannot close, closed is final).

**GUI and summaries**
- Swing `JTabbedPane`; tables use read-only models; Summary tab shows totals, counts by type/status and per-customer holdings.

**Persistence (data survives restart)**
- `Bank`, `Customer`, `Account` (and subclasses) and `Transaction` implement `Serializable`. After every successful change `Bank.save()` writes the object graph to disk; `Main` loads it on start. The file is written to a temp file first and then moved into place, so a crash while saving cannot corrupt the data. A damaged file is kept as `.corrupt` and the app starts fresh instead of crashing. Transaction ID and account-number counters continue after reload.
- Justification: the data volume is small and Serialization needs no database or external library, keeping the project pure core Java.

**Deleting customers and accounts**
- A customer with accounts cannot be deleted - the message says to delete the accounts first.
- An account can be deleted only when its balance is zero (close it first; closing pays out the balance). When a customer's last account is deleted, the customer record is removed automatically (the confirmation dialog warns about this).
- Justification: stops money from silently disappearing and keeps the customer/account lists consistent.

**What is "INTEREST" in the transaction history?**
- It is the monthly interest the bank credits to a SAVINGS account: balance x 4% / 12 (e.g. Rs. 98,765 gives Rs. 329.22). It is credited with the "Credit Monthly Interest" button (Accounts tab), once per calendar month per account, and appears as an INTEREST row in the history. Current accounts earn no interest.

## Business rules
| Rule | Savings | Current |
|---|---|---|
| Minimum opening balance | Rs. 500 | Rs. 1,000 |
| Minimum balance | Rs. 500 | none |
| Overdraft | no | up to Rs. 10,000 |
| Max withdrawal per transaction | Rs. 50,000 | none |
| Interest | 4% p.a. (credited monthly, once per month) | none |
| FROZEN / CLOSED accounts | no deposits, withdrawals or transfers | same |

## Design note
Money uses `double` rounded to 2 decimals, which is fine for a prototype. A production banking system would use `BigDecimal` or integer paise to avoid floating-point error.
