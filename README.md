# Java Console Banking Management System

> **Educational Simulation Notice**: This project is an educational banking simulation designed to demonstrate Core Java, Object-Oriented Programming (OOP) principles, data structures, and file persistence. It is **not** intended or suitable for real-world financial transactions or securing sensitive financial data.

---

## 1. Project Overview

The **Java Console Banking Management System** is a modular, menu-driven Core Java console application. It provides complete banking lifecycle management including customer registration, account balances, deposits, withdrawals, fund transfers, transaction logging, consolidated bank reporting, and resilient local CSV file persistence.

### Key Highlights
- **100% Core Java (JDK 17 LTS compatible)**: Zero external dependencies, no Spring Boot, and no third-party libraries.
- **Accurate Financial Arithmetic**: Uses `java.math.BigDecimal` with `RoundingMode.HALF_UP` for all monetary calculations, preventing IEEE 754 floating-point inaccuracies.
- **Resilient File Persistence**: Stores customer, account, transaction, and auto-increment sequence metadata in human-readable, RFC 4180-compliant CSV files in a dedicated `data/` folder.
- **Strict Financial Integrity & Conservation of Money**: Account transfers are atomic; money cannot be created or destroyed during transfers.
- **Defensive Input Validation**: Scanner inputs will not crash on non-numeric or malformed data; comprehensive validation guards email addresses, phone numbers, age boundaries, account numbers, and transaction limits.

---

## 2. Project Directory Structure

```
java_banking_application/
├── src/
│   └── banking/
│       ├── Main.java                        # Console UI and menu controller
│       ├── exception/                       # Domain-specific banking exceptions
│       │   ├── BankingException.java        # Base checked exception
│       │   ├── InsufficientFundsException.java
│       │   ├── AccountNotFoundException.java
│       │   ├── CustomerNotFoundException.java
│       │   └── ValidationException.java
│       ├── model/                           # Domain entities and value objects
│       │   ├── Customer.java                # Customer profile entity
│       │   ├── BankAccount.java             # Account balance and operations
│       │   ├── Transaction.java             # Immutable audit log entry
│       │   ├── TransactionType.java         # Enum of transaction categories
│       │   └── BankSummary.java             # Consolidated bank statistics
│       ├── service/                         # Business logic and storage layers
│       │   ├── BankService.java             # Core banking business logic
│       │   └── FileStorageService.java      # CSV data persistence layer
│       └── util/                            # Reusable helpers and validators
│           ├── InputValidator.java          # Validation rules and console reader
│           └── CsvUtils.java                # RFC 4180 CSV escaping and parsing
├── test/
│   └── banking/
│       └── BankingSystemTest.java           # 10-scenario automated test runner
├── data/                                    # Persistent CSV storage directory
│   ├── customers.csv                        # Customer master data
│   ├── accounts.csv                         # Account master balances
│   ├── transactions.csv                     # Historical audit log
│   └── sequences.properties                 # Monotonic ID counters
├── out/                                     # Compiled bytecode classes
├── build.bat                                # Windows Batch compile script
├── run.bat                                  # Windows Batch launch script
├── test.bat                                 # Windows Batch test runner
├── build.ps1                                # PowerShell compile script
├── run.ps1                                  # PowerShell launch script
├── test.ps1                                 # PowerShell test runner
├── .gitignore                               # Git ignore configuration
└── README.md                                # Comprehensive documentation
```

---

## 3. Main Menu Options

When the application starts, it displays the main menu:

```
========================================
       WELCOME TO JAVA BANK
========================================

1. Add New Customer
2. View All Customers
3. Search Customer
4. Update Customer Details
5. Delete Customer
6. Check Bank Balance
7. Deposit Money
8. Withdraw Money
9. Transfer Money
10. View Transaction History
11. Display Bank Summary
12. Exit

Enter your choice:
```

### Detailed Functional Walkthrough

| # | Menu Option | Description | Key Validations & Constraints |
|---|-------------|-------------|-------------------------------|
| **1** | **Add New Customer** | Registers a new customer and primary account with an optional opening deposit. | Generates unique `CUST#####` and `ACC######`. Validates age (18–120), email format, 10–15 digit phone, non-empty address, and opening deposit >= $0. |
| **2** | **View All Customers** | Displays a formatted tabular list of all registered customers with balances. | Handles empty datasets gracefully; formats balances as `$#,##0.00`. |
| **3** | **Search Customer** | Looks up customers by Customer ID, Account Number, or Name. | Case-insensitive; displays full profile and linked account balance. |
| **4** | **Update Customer Details** | Modifies an existing customer's contact information. | Allows pressing `[Enter]` to preserve existing values. Customer ID and Account Number are strictly immutable. |
| **5** | **Delete Customer** | Removes customer record and closes account upon confirmation. | **Blocked if balance is non-zero.** Funds must be withdrawn/settled to `$0.00` first. |
| **6** | **Check Bank Balance** | Retrieves live balance for a given account number. | Displays account number, holder's full name, customer ID, and current balance. |
| **7** | **Deposit Money** | Credits an account with a specified positive monetary sum. | Validates amount > $0.00. Records a timestamped `DEPOSIT` transaction and updates balance. |
| **8** | **Withdraw Money** | Debits an account with a specified positive monetary sum. | Enforces available balance constraints via `InsufficientFundsException`. Rejects overdrafts. Records `WITHDRAWAL` transaction. |
| **9** | **Transfer Money** | Atomically moves funds between two distinct accounts. | Validates existence of both accounts, checks source != destination, checks sufficient balance, and debits/credits atomically. Writes `TRANSFER_OUT` and `TRANSFER_IN` audit entries. |
| **10**| **View Transaction History**| Displays chronological transaction log for an account. | Shows Transaction ID, timestamp, type, debit/credit amount, resulting balance, and remarks. |
| **11**| **Display Bank Summary** | Generates consolidated bank metrics from active state. | Computes total customers, active accounts, total deposits held, total logged transactions, total money deposited, and total money withdrawn. |
| **12**| **Exit** | Persists pending data and safely terminates application. | Goodbye message displayed. |

---

## 4. Architecture & Object-Oriented Design

```
+-----------------------------------------------------------+
|                        Main.java                          |
|             (Console Menu & User Interaction)             |
+-----------------------------+-----------------------------+
                              |
                              v
+-----------------------------------------------------------+
|                     BankService.java                      |
|             (Business Logic & State Registry)             |
+--------------+-----------------------------+--------------+
               |                             |
               v                             v
+-------------------------------+ +-------------------------+
|          Domain Model         | |    FileStorageService   |
| - Customer                    | | - loadCustomers()       |
| - BankAccount                 | | - loadAccounts()        |
| - Transaction                 | | - loadTransactions()    |
| - TransactionType             | | - saveAll()             |
| - BankSummary                 | | - saveSequences()       |
+-------------------------------+ +------------+------------+
               |                               |
               v                               v
+-------------------------------+ +-------------------------+
|     InputValidator / CsvUtils | |     Local File System   |
|   (Regex, Parsing, Escaping)  | |        data/*.csv       |
+-------------------------------+ +-------------------------+
```

### Class Responsibilities

1. **`Customer.java` (`banking.model`)**:
   - Represents customer profile data: `customerId`, `name`, `age`, `phoneNumber`, `email`, `address`, `accountNumber`, and `createdAt`.
   - Protects immutable identifiers (`customerId`, `accountNumber`, `createdAt`) while exposing validated mutators for editable fields.

2. **`BankAccount.java` (`banking.model`)**:
   - Encapsulates `accountNumber`, `customerId`, `balance` (`BigDecimal`), and `createdAt`.
   - Core mutation methods `deposit(BigDecimal)` and `withdraw(BigDecimal)` are thread-safe (`synchronized`) and enforce positive values and sufficient funds.

3. **`Transaction.java` & `TransactionType.java` (`banking.model`)**:
   - Immutable audit record holding `transactionId`, `accountNumber`, `type`, `amount`, `balanceAfter`, `timestamp` (`LocalDateTime`), and `description`.
   - Enum `TransactionType` defines `OPENING_DEPOSIT`, `DEPOSIT`, `WITHDRAWAL`, `TRANSFER_OUT`, and `TRANSFER_IN`.

4. **`BankSummary.java` (`banking.model`)**:
   - Immutable metric container for bank-wide health metrics.

5. **`BankService.java` (`banking.service`)**:
   - Central business service managing collections `Map<String, Customer>`, `Map<String, BankAccount>`, and `List<Transaction>`.
   - Enforces business rules: balance non-zero deletion locks, transfer atomicity, unique identifier sequences (`AtomicLong`), and persistence triggers.

6. **`FileStorageService.java` (`banking.service`)**:
   - Handles read/write operations to CSV files.
   - Implements atomic writes via temporary files (`.tmp`) and atomic rename (`StandardCopyOption.ATOMIC_MOVE`), preventing data corruption upon sudden termination.
   - Persists monotonic sequence counters in `sequences.properties`.

7. **`InputValidator.java` & `CsvUtils.java` (`banking.util`)**:
   - `InputValidator`: Centralized validation logic for names, age, phone numbers, email addresses, and monetary amounts; safe scanner wrapper methods that prevent console input crashes.
   - `CsvUtils`: RFC 4180-compliant quote escaping and tokenizer that supports addresses or names containing commas.

8. **Custom Exceptions (`banking.exception`)**:
   - `BankingException`: Base checked application exception.
   - `InsufficientFundsException`: Raised on overdrawing.
   - `AccountNotFoundException` & `CustomerNotFoundException`: Raised when entities cannot be located.
   - `ValidationException`: Raised when input violates domain constraints.

---

## 5. Compilation and Execution

### Prerequisites
- **JDK 17** or higher installed.
- `java` and `javac` accessible in your system `PATH`.

Verify Java version:
```powershell
java -version
javac -version
```

---

### Option A: Using the Build & Run Scripts (Recommended for Windows)

#### Command Prompt (`cmd`):
```cmd
build.bat
run.bat
```
To run tests:
```cmd
test.bat
```

#### PowerShell:
```powershell
.\build.ps1
.\run.ps1
```
To run tests:
```powershell
.\test.ps1
```

---

### Option B: Manual Command-Line Compilation & Execution

#### 1. Compile all source and test files:
```powershell
javac -d out src/banking/Main.java src/banking/model/*.java src/banking/service/*.java src/banking/util/*.java src/banking/exception/*.java test/banking/*.java
```

#### 2. Run the Banking Management Application:
```powershell
java -cp out banking.Main
```

#### 3. Run the Automated Test Suite:
```powershell
java -cp out banking.BankingSystemTest
```

---

### Option C: Running in VS Code

1. Open the project folder in VS Code:
   ```powershell
   code .
   ```
2. Ensure the **Extension Pack for Java** is installed in VS Code.
3. Open `src/banking/Main.java`.
4. Click the **Run** button above the `main` method, or press `F5` / `Ctrl+F5`.
5. The terminal panel at the bottom of VS Code will run the interactive console application.

---

## 6. Automated Testing & Verification

An automated test suite is provided in `test/banking/BankingSystemTest.java`. It runs in an isolated `test_data/` directory and verifies all 10 required test scenarios with **62 assertion checkpoints**:

```
=================================================
  STARTING JAVA BANKING SYSTEM AUTOMATED TESTS   
=================================================

--- Scenario 1: Adding Multiple Customers & Auto-Generation ---
  [PASS] Customer 1 ID format
  [PASS] Customer 1 Account format
  [PASS] Customer 2 ID format
  [PASS] Customer 2 Account format
  [PASS] Customer 3 ID format
  [PASS] Customer 3 Account format
  [PASS] Total registered customers is 3
  [PASS] Alice initial balance is $1000.00
  [PASS] Charlie initial balance is $0.00

--- Scenario 2: Searching and Updating Customers ---
  [PASS] Search by Customer ID found Alice
  [PASS] Search by Account Number found Bob
  [PASS] Search by Name found Charlie
  [PASS] Bob's name remains unchanged
  [PASS] Bob's age updated to 43
  [PASS] Bob's phone updated
  [PASS] Bob's email updated
  [PASS] Bob's address updated
  [PASS] Bob's account number remains immutable

--- Scenario 3: Depositing and Withdrawing Money ---
  [PASS] Deposit amount is $250.00
  [PASS] Alice balance after deposit is $1250.00
  [PASS] Deposit transaction type
  [PASS] Withdrawal amount is $100.00
  [PASS] Alice balance after withdrawal is $1150.00
  [PASS] Withdrawal transaction type

--- Scenario 4: Preventing Withdrawals with Insufficient Funds ---
  [PASS] Withdrawal exceeding balance throws InsufficientFundsException
  [PASS] Balance remains unchanged after declined withdrawal
  [PASS] Negative withdrawal throws ValidationException

--- Scenario 5: Transferring Money & Verifying Conservation of Money ---
  [PASS] Alice balance reduced by $300.00
  [PASS] Bob balance increased by $300.00
  [PASS] Conservation of money verified: total funds before equals total funds after
  [PASS] Alice has TRANSFER_OUT transaction
  [PASS] Alice TRANSFER_OUT amount
  [PASS] Bob has TRANSFER_IN transaction
  [PASS] Bob TRANSFER_IN amount

--- Scenario 6: Preventing Invalid Transfers ---
  [PASS] Transfer to same account rejected
  [PASS] Transfer from non-existent account rejected
  [PASS] Transfer to non-existent account rejected
  [PASS] Transfer exceeding balance rejected

--- Scenario 7: Deleting Account & Balance Constraints ---
  [PASS] Deleting customer with non-zero balance is blocked
  [PASS] Charlie with $0 balance was deleted successfully
  [PASS] Registered customer count is now 2

--- Scenario 8: Saving and Reloading Data (Persistence) ---
  [PASS] Reloaded customer count matches
  [PASS] Reloaded Alice name
  [PASS] Reloaded Alice balance is $850.00
  [PASS] New customer after reload has sequential ID CUST1004
  [PASS] New customer after reload has sequential Account ACC100004

--- Scenario 9: Input Validation Rules ---
  [PASS] Age 17 rejected
  [PASS] Age 121 rejected
  [PASS] Empty name rejected
  [PASS] Single character name rejected
  [PASS] Numeric characters in name rejected
  [PASS] Missing @ rejected
  [PASS] Missing domain rejected
  [PASS] Too short phone rejected
  [PASS] Alphabetic phone rejected
  [PASS] Zero amount rejected
  [PASS] Negative amount rejected

--- Scenario 10: Verifying Consolidated Bank Summary ---
  [PASS] Bank summary customer count >= 2
  [PASS] Bank summary deposits held > 0
  [PASS] Bank summary transactions count > 0
  [PASS] Bank summary total deposited > 0
  [PASS] Bank summary total withdrawn > 0
=================================================
  TEST RESULTS: 62 / 62 PASSED
=================================================
  ALL TESTS PASSED SUCCESSFULLY!
```

---

## 7. Sample Console Walkthroughs

### 1. View All Customers
```
----------------------------------------
           ALL REGISTERED CUSTOMERS
----------------------------------------
+---------------+----------------------+------------------+------------------+
| Customer ID   | Customer Name        | Account Number   | Current Balance  |
+---------------+----------------------+------------------+------------------+
| CUST1001      | Alice Johnson        | ACC100001        |        $1,250.00 |
| CUST1002      | Bob Smith            | ACC100002        |          $500.00 |
+---------------+----------------------+------------------+------------------+
Total Registered Customers: 2
```

### 2. Transfer Money
```
----------------------------------------
            TRANSFER MONEY
----------------------------------------
Enter Source Account Number: ACC100001
Enter Destination Account Number: ACC100002

 Source Account      : ACC100001 (Holder: Alice Johnson)
 Available Balance   : $1,250.00
 Destination Account : ACC100002 (Holder: Bob Smith)

Enter transfer amount: $300.00
Enter transfer note (optional, press [Enter] to skip): Project collaboration
Confirm transfer of $300.00 to Bob Smith (ACC100002)? (y/n): y

>> SUCCESS: Transfer completed successfully!
----------------------------------------
 Transferred Amount : $300.00
 Debited Account    : ACC100001
 Credited Account   : ACC100002
 Source New Balance : $950.00
----------------------------------------
```

### 3. View Transaction History
```
----------------------------------------
         TRANSACTION HISTORY
----------------------------------------
Enter Account Number: ACC100001

Account: ACC100001 | Holder: Alice Johnson | Current Balance: $950.00

+---------------------+------------+-----------------+----------------+----------------+----------------------------------------+
| Date & Time         | Txn ID     | Type            | Amount         | Balance After  | Description                            |
+---------------------+------------+-----------------+----------------+----------------+----------------------------------------+
| 2026-10-28 10:00:00 | TXN100001  | Opening Deposit |     +$1,000.00 |      $1,000.00 | Opening Account Deposit                |
| 2026-10-28 11:15:00 | TXN100003  | Deposit         |       +$250.00 |      $1,250.00 | Initial Welcome Bonus                  |
| 2026-10-29 00:15:30 | TXN100004  | Transfer Out    |       -$300.00 |        $950.00 | Transfer to ACC100002 - Project collab |
+---------------------+------------+-----------------+----------------+----------------+----------------------------------------+
Total Transactions: 3
```

### 4. Consolidated Bank Summary
```
========================================
          BANK SUMMARY REPORT
========================================
 Total Customers Registered : 2
 Total Accounts Managed     : 2
 Total Deposits Held        : $1,750.00
 Total Transactions Logged  : 4
 Total Amount Deposited     : $1,750.00
 Total Amount Withdrawn     : $0.00
========================================
```

---

## 8. Data File Formats

All data files are stored in `data/`:

### `customers.csv`
```csv
customerId,name,age,phoneNumber,email,address,accountNumber,createdAt
CUST1001,Alice Johnson,29,+1-555-123-0101,alice@example.com,123 Maple St,ACC100001,2026-10-28 10:00:00
CUST1002,Bob Smith,42,+1-555-123-0202,bob@example.com,456 Oak Ave,ACC100002,2026-10-28 10:30:00
```

### `accounts.csv`
```csv
accountNumber,customerId,balance,createdAt
ACC100001,CUST1001,1250.00,2026-10-28 10:00:00
ACC100002,CUST1002,500.00,2026-10-28 10:30:00
```

### `transactions.csv`
```csv
transactionId,accountNumber,type,amount,balanceAfter,timestamp,description
TXN100001,ACC100001,OPENING_DEPOSIT,1000.00,1000.00,2026-10-28 10:00:00,Opening Account Deposit
TXN100002,ACC100002,OPENING_DEPOSIT,500.00,500.00,2026-10-28 10:30:00,Opening Account Deposit
TXN100003,ACC100001,DEPOSIT,250.00,1250.00,2026-10-28 11:15:00,Initial Welcome Bonus
```

### `sequences.properties`
```properties
# Auto-generated banking sequence metadata
customerIdSeq=1002
accountNumberSeq=100002
transactionIdSeq=100003
```
