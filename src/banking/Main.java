package banking;

import banking.exception.AccountNotFoundException;
import banking.exception.BankingException;
import banking.exception.CustomerNotFoundException;
import banking.exception.InsufficientFundsException;
import banking.exception.ValidationException;
import banking.model.BankAccount;
import banking.model.BankSummary;
import banking.model.Customer;
import banking.model.Transaction;
import banking.service.BankService;
import banking.util.InputValidator;

import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;

/**
 * Console-based user interface and entry point for the Java Banking Management System.
 */
public class Main {

    private final BankService bankService;
    private final Scanner scanner;

    public Main() {
        this.bankService = new BankService();
        this.scanner = new Scanner(System.in);
    }

    public static void main(String[] args) {
        Main app = new Main();
        app.run();
    }

    /**
     * Main application loop.
     */
    public void run() {
        boolean running = true;
        while (running) {
            try {
                printMainMenu();
                int choice = InputValidator.readInt(scanner, "Enter your choice: ", 1, 12);
                System.out.println();

                switch (choice) {
                    case 1 -> handleAddNewCustomer();
                    case 2 -> handleViewAllCustomers();
                    case 3 -> handleSearchCustomer();
                    case 4 -> handleUpdateCustomerDetails();
                    case 5 -> handleDeleteCustomer();
                    case 6 -> handleCheckBankBalance();
                    case 7 -> handleDepositMoney();
                    case 8 -> handleWithdrawMoney();
                    case 9 -> handleTransferMoney();
                    case 10 -> handleViewTransactionHistory();
                    case 11 -> handleDisplayBankSummary();
                    case 12 -> {
                        System.out.println("========================================");
                        System.out.println(" Thank you for using Java Bank. Goodbye!");
                        System.out.println("========================================");
                        running = false;
                    }
                    default -> System.out.println(">> Invalid option. Please choose between 1 and 12.");
                }

                if (running) {
                    pauseForUser();
                }
            } catch (Exception e) {
                System.out.printf("%n>> An unexpected error occurred: %s%n", e.getMessage());
                pauseForUser();
            }
        }
    }

    private void printMainMenu() {
        System.out.println();
        System.out.println("========================================");
        System.out.println("       WELCOME TO JAVA BANK");
        System.out.println("========================================");
        System.out.println();
        System.out.println("1. Add New Customer");
        System.out.println("2. View All Customers");
        System.out.println("3. Search Customer");
        System.out.println("4. Update Customer Details");
        System.out.println("5. Delete Customer");
        System.out.println("6. Check Bank Balance");
        System.out.println("7. Deposit Money");
        System.out.println("8. Withdraw Money");
        System.out.println("9. Transfer Money");
        System.out.println("10. View Transaction History");
        System.out.println("11. Display Bank Summary");
        System.out.println("12. Exit");
        System.out.println();
    }

    private void pauseForUser() {
        System.out.println();
        System.out.print("Press [Enter] to continue...");
        scanner.nextLine();
    }

    // ==========================================
    // 1. Add New Customer
    // ==========================================
    private void handleAddNewCustomer() {
        System.out.println("----------------------------------------");
        System.out.println("          ADD NEW CUSTOMER");
        System.out.println("----------------------------------------");

        String name = promptValidName("Enter customer full name: ");
        int age = promptValidAge("Enter customer age (18-120): ");
        String phone = promptValidPhone("Enter phone number: ");
        String email = promptValidEmail("Enter email address: ");
        String address = promptValidAddress("Enter residential address: ");
        BigDecimal openingDeposit = InputValidator.readBigDecimal(
                scanner, "Enter initial deposit amount (or 0 for none): $", true);

        try {
            Customer customer = bankService.addCustomer(name, age, phone, email, address, openingDeposit);
            BankAccount account = bankService.getAccount(customer.getAccountNumber());

            System.out.println();
            System.out.println(">> SUCCESS: Customer account successfully created!");
            System.out.println("----------------------------------------");
            System.out.printf(" Customer ID    : %s%n", customer.getCustomerId());
            System.out.printf(" Account Number : %s%n", customer.getAccountNumber());
            System.out.printf(" Account Holder : %s%n", customer.getName());
            System.out.printf(" Opening Balance: $%,.2f%n", account.getBalance());
            System.out.println("----------------------------------------");
        } catch (BankingException e) {
            System.out.printf(">> ERROR: Failed to create customer account: %s%n", e.getMessage());
        }
    }

    // ==========================================
    // 2. View All Customers
    // ==========================================
    private void handleViewAllCustomers() {
        System.out.println("----------------------------------------");
        System.out.println("           ALL REGISTERED CUSTOMERS");
        System.out.println("----------------------------------------");

        List<Customer> customerList = bankService.getAllCustomers();
        if (customerList.isEmpty()) {
            System.out.println(">> No customers currently registered in the system.");
            return;
        }

        String border = "+---------------+----------------------+------------------+------------------+";
        System.out.println(border);
        System.out.printf("| %-13s | %-20s | %-16s | %-16s |%n", "Customer ID", "Customer Name", "Account Number", "Current Balance");
        System.out.println(border);

        for (Customer c : customerList) {
            BigDecimal balance = BigDecimal.ZERO;
            try {
                BankAccount acc = bankService.getAccount(c.getAccountNumber());
                balance = acc.getBalance();
            } catch (AccountNotFoundException ignored) {}

            String formattedBal = String.format("$%,.2f", balance);
            System.out.printf("| %-13s | %-20s | %-16s | %16s |%n",
                    c.getCustomerId(),
                    truncate(c.getName(), 20),
                    c.getAccountNumber(),
                    formattedBal);
        }
        System.out.println(border);
        System.out.printf("Total Registered Customers: %d%n", customerList.size());
    }

    // ==========================================
    // 3. Search Customer
    // ==========================================
    private void handleSearchCustomer() {
        System.out.println("----------------------------------------");
        System.out.println("           SEARCH CUSTOMER");
        System.out.println("----------------------------------------");

        String query = InputValidator.readString(scanner, "Enter Customer ID, Account Number, or Name to search: ", false);
        List<Customer> results = bankService.searchCustomers(query);

        if (results.isEmpty()) {
            System.out.printf(">> No customer found matching \"%s\".%n", query);
            return;
        }

        System.out.printf("%nFound %d matching record(s):%n", results.size());
        for (Customer c : results) {
            BigDecimal balance = BigDecimal.ZERO;
            try {
                BankAccount acc = bankService.getAccount(c.getAccountNumber());
                balance = acc.getBalance();
            } catch (AccountNotFoundException ignored) {}

            System.out.println("----------------------------------------");
            System.out.printf(" Customer ID     : %s%n", c.getCustomerId());
            System.out.printf(" Full Name       : %s%n", c.getName());
            System.out.printf(" Age             : %d%n", c.getAge());
            System.out.printf(" Phone Number    : %s%n", c.getPhoneNumber());
            System.out.printf(" Email           : %s%n", c.getEmail());
            System.out.printf(" Address         : %s%n", c.getAddress());
            System.out.printf(" Account Number  : %s%n", c.getAccountNumber());
            System.out.printf(" Current Balance : $%,.2f%n", balance);
            System.out.printf(" Registered On   : %s%n", c.getFormattedCreatedAt());
        }
        System.out.println("----------------------------------------");
    }

    // ==========================================
    // 4. Update Customer Details
    // ==========================================
    private void handleUpdateCustomerDetails() {
        System.out.println("----------------------------------------");
        System.out.println("        UPDATE CUSTOMER DETAILS");
        System.out.println("----------------------------------------");

        String customerId = InputValidator.readString(scanner, "Enter Customer ID to update: ", false);
        Customer customer;
        try {
            customer = bankService.getCustomerById(customerId);
        } catch (CustomerNotFoundException e) {
            System.out.printf(">> ERROR: %s%n", e.getMessage());
            return;
        }

        System.out.println();
        System.out.println("Current Profile Information:");
        System.out.printf("  Customer ID : %s%n", customer.getCustomerId());
        System.out.printf("  Account No  : %s (Immutable)%n", customer.getAccountNumber());
        System.out.printf("  1. Name     : %s%n", customer.getName());
        System.out.printf("  2. Age      : %d%n", customer.getAge());
        System.out.printf("  3. Phone    : %s%n", customer.getPhoneNumber());
        System.out.printf("  4. Email    : %s%n", customer.getEmail());
        System.out.printf("  5. Address  : %s%n", customer.getAddress());
        System.out.println();
        System.out.println("Leave input blank (press [Enter]) to keep current value.");

        String newName = promptOptionalName("Enter new name [" + customer.getName() + "]: ", customer.getName());
        Integer newAge = promptOptionalAge("Enter new age [" + customer.getAge() + "]: ", customer.getAge());
        String newPhone = promptOptionalPhone("Enter new phone [" + customer.getPhoneNumber() + "]: ", customer.getPhoneNumber());
        String newEmail = promptOptionalEmail("Enter new email [" + customer.getEmail() + "]: ", customer.getEmail());
        String newAddress = promptOptionalAddress("Enter new address [" + customer.getAddress() + "]: ", customer.getAddress());

        boolean confirm = InputValidator.readConfirmation(scanner, "Save these changes?");
        if (!confirm) {
            System.out.println(">> Update operation cancelled.");
            return;
        }

        try {
            bankService.updateCustomer(customerId, newName, newAge, newPhone, newEmail, newAddress);
            System.out.println(">> SUCCESS: Customer profile updated successfully.");
        } catch (BankingException e) {
            System.out.printf(">> ERROR: Failed to update customer: %s%n", e.getMessage());
        }
    }

    // ==========================================
    // 5. Delete Customer
    // ==========================================
    private void handleDeleteCustomer() {
        System.out.println("----------------------------------------");
        System.out.println("            DELETE CUSTOMER");
        System.out.println("----------------------------------------");

        String customerId = InputValidator.readString(scanner, "Enter Customer ID to delete: ", false);
        Customer customer;
        BankAccount account;
        try {
            customer = bankService.getCustomerById(customerId);
            account = bankService.getAccount(customer.getAccountNumber());
        } catch (BankingException e) {
            System.out.printf(">> ERROR: %s%n", e.getMessage());
            return;
        }

        System.out.println();
        System.out.printf("Customer Found: %s (%s)%n", customer.getName(), customer.getCustomerId());
        System.out.printf("Linked Account: %s | Balance: $%,.2f%n", account.getAccountNumber(), account.getBalance());

        if (!account.hasZeroBalance()) {
            System.out.println();
            System.out.printf(">> REJECTED: Account %s currently has an active balance of $%,.2f.%n",
                    account.getAccountNumber(), account.getBalance());
            System.out.println(">> Accounts with non-zero balances cannot be deleted.");
            System.out.println(">> Please withdraw or transfer all funds to settle balance to $0.00 first.");
            return;
        }

        System.out.println();
        boolean confirm = InputValidator.readConfirmation(
                scanner, "WARNING: Are you sure you want to permanently delete this customer and account?");
        if (!confirm) {
            System.out.println(">> Deletion cancelled.");
            return;
        }

        try {
            bankService.deleteCustomer(customerId);
            System.out.printf(">> SUCCESS: Customer %s and account %s were deleted.%n",
                    customer.getCustomerId(), account.getAccountNumber());
        } catch (BankingException e) {
            System.out.printf(">> ERROR: Deletion failed: %s%n", e.getMessage());
        }
    }

    // ==========================================
    // 6. Check Bank Balance
    // ==========================================
    private void handleCheckBankBalance() {
        System.out.println("----------------------------------------");
        System.out.println("          CHECK BANK BALANCE");
        System.out.println("----------------------------------------");

        String accountNumber = InputValidator.readString(scanner, "Enter Account Number (e.g. ACC100001): ", false);
        try {
            BankAccount account = bankService.getAccount(accountNumber);
            Customer customer = bankService.getCustomerByAccountNumber(account.getAccountNumber());

            System.out.println();
            System.out.println("----------------------------------------");
            System.out.printf(" Account Number : %s%n", account.getAccountNumber());
            System.out.printf(" Account Holder : %s%n", customer.getName());
            System.out.printf(" Customer ID    : %s%n", customer.getCustomerId());
            System.out.printf(" Current Balance: $%,.2f%n", account.getBalance());
            System.out.println("----------------------------------------");
        } catch (AccountNotFoundException e) {
            System.out.printf(">> ERROR: %s%n", e.getMessage());
        }
    }

    // ==========================================
    // 7. Deposit Money
    // ==========================================
    private void handleDepositMoney() {
        System.out.println("----------------------------------------");
        System.out.println("             DEPOSIT MONEY");
        System.out.println("----------------------------------------");

        String accountNumber = InputValidator.readString(scanner, "Enter Account Number: ", false);
        try {
            BankAccount account = bankService.getAccount(accountNumber);
            Customer customer = bankService.getCustomerByAccountNumber(account.getAccountNumber());

            System.out.printf("Account Holder: %s | Current Balance: $%,.2f%n", customer.getName(), account.getBalance());

            BigDecimal amount = InputValidator.readBigDecimal(scanner, "Enter deposit amount: $", false);
            String description = InputValidator.readString(scanner, "Enter remarks (optional, press [Enter] to skip): ", true);
            if (description.isEmpty()) {
                description = "Cash Deposit";
            }

            Transaction txn = bankService.deposit(account.getAccountNumber(), amount, description);

            System.out.println();
            System.out.println(">> SUCCESS: Deposit processed successfully.");
            System.out.println("----------------------------------------");
            System.out.printf(" Transaction ID : %s%n", txn.getTransactionId());
            System.out.printf(" Account Number : %s%n", txn.getAccountNumber());
            System.out.printf(" Amount Credited: $%,.2f%n", txn.getAmount());
            System.out.printf(" Updated Balance: $%,.2f%n", txn.getBalanceAfter());
            System.out.printf(" Timestamp      : %s%n", txn.getFormattedTimestamp());
            System.out.println("----------------------------------------");
        } catch (BankingException e) {
            System.out.printf(">> ERROR: Deposit failed: %s%n", e.getMessage());
        }
    }

    // ==========================================
    // 8. Withdraw Money
    // ==========================================
    private void handleWithdrawMoney() {
        System.out.println("----------------------------------------");
        System.out.println("            WITHDRAW MONEY");
        System.out.println("----------------------------------------");

        String accountNumber = InputValidator.readString(scanner, "Enter Account Number: ", false);
        try {
            BankAccount account = bankService.getAccount(accountNumber);
            Customer customer = bankService.getCustomerByAccountNumber(account.getAccountNumber());

            System.out.printf("Account Holder: %s | Current Available Balance: $%,.2f%n",
                    customer.getName(), account.getBalance());

            BigDecimal amount = InputValidator.readBigDecimal(scanner, "Enter withdrawal amount: $", false);
            String description = InputValidator.readString(scanner, "Enter remarks (optional, press [Enter] to skip): ", true);
            if (description.isEmpty()) {
                description = "Cash Withdrawal";
            }

            Transaction txn = bankService.withdraw(account.getAccountNumber(), amount, description);

            System.out.println();
            System.out.println(">> SUCCESS: Withdrawal processed successfully.");
            System.out.println("----------------------------------------");
            System.out.printf(" Transaction ID : %s%n", txn.getTransactionId());
            System.out.printf(" Account Number : %s%n", txn.getAccountNumber());
            System.out.printf(" Amount Debited : $%,.2f%n", txn.getAmount());
            System.out.printf(" Updated Balance: $%,.2f%n", txn.getBalanceAfter());
            System.out.printf(" Timestamp      : %s%n", txn.getFormattedTimestamp());
            System.out.println("----------------------------------------");
        } catch (InsufficientFundsException e) {
            System.out.printf(">> TRANSACTION DECLINED: %s%n", e.getMessage());
        } catch (BankingException e) {
            System.out.printf(">> ERROR: Withdrawal failed: %s%n", e.getMessage());
        }
    }

    // ==========================================
    // 9. Transfer Money
    // ==========================================
    private void handleTransferMoney() {
        System.out.println("----------------------------------------");
        System.out.println("            TRANSFER MONEY");
        System.out.println("----------------------------------------");

        String srcAcc = InputValidator.readString(scanner, "Enter Source Account Number: ", false);
        String dstAcc = InputValidator.readString(scanner, "Enter Destination Account Number: ", false);

        if (srcAcc.equalsIgnoreCase(dstAcc)) {
            System.out.println(">> ERROR: Source and Destination accounts cannot be the same.");
            return;
        }

        try {
            BankAccount sourceAccount = bankService.getAccount(srcAcc);
            Customer sourceCustomer = bankService.getCustomerByAccountNumber(sourceAccount.getAccountNumber());

            BankAccount destAccount = bankService.getAccount(dstAcc);
            Customer destCustomer = bankService.getCustomerByAccountNumber(destAccount.getAccountNumber());

            System.out.println();
            System.out.printf(" Source Account      : %s (Holder: %s)%n",
                    sourceAccount.getAccountNumber(), sourceCustomer.getName());
            System.out.printf(" Available Balance   : $%,.2f%n", sourceAccount.getBalance());
            System.out.printf(" Destination Account : %s (Holder: %s)%n",
                    destAccount.getAccountNumber(), destCustomer.getName());
            System.out.println();

            BigDecimal amount = InputValidator.readBigDecimal(scanner, "Enter transfer amount: $", false);
            String note = InputValidator.readString(scanner, "Enter transfer note (optional, press [Enter] to skip): ", true);

            boolean confirm = InputValidator.readConfirmation(scanner,
                    String.format("Confirm transfer of $%,.2f to %s (%s)?", amount, destCustomer.getName(), destAccount.getAccountNumber()));
            if (!confirm) {
                System.out.println(">> Transfer cancelled.");
                return;
            }

            bankService.transfer(sourceAccount.getAccountNumber(), destAccount.getAccountNumber(), amount, note);

            // Fetch refreshed balance
            BankAccount refreshedSource = bankService.getAccount(sourceAccount.getAccountNumber());

            System.out.println();
            System.out.println(">> SUCCESS: Transfer completed successfully!");
            System.out.println("----------------------------------------");
            System.out.printf(" Transferred Amount : $%,.2f%n", amount);
            System.out.printf(" Debited Account    : %s%n", sourceAccount.getAccountNumber());
            System.out.printf(" Credited Account   : %s%n", destAccount.getAccountNumber());
            System.out.printf(" Source New Balance : $%,.2f%n", refreshedSource.getBalance());
            System.out.println("----------------------------------------");
        } catch (InsufficientFundsException e) {
            System.out.printf(">> TRANSFER DECLINED: %s%n", e.getMessage());
        } catch (BankingException e) {
            System.out.printf(">> ERROR: Transfer failed: %s%n", e.getMessage());
        }
    }

    // ==========================================
    // 10. View Transaction History
    // ==========================================
    private void handleViewTransactionHistory() {
        System.out.println("----------------------------------------");
        System.out.println("         TRANSACTION HISTORY");
        System.out.println("----------------------------------------");

        String accountNumber = InputValidator.readString(scanner, "Enter Account Number: ", false);
        try {
            BankAccount account = bankService.getAccount(accountNumber);
            Customer customer = bankService.getCustomerByAccountNumber(account.getAccountNumber());
            List<Transaction> history = bankService.getTransactionHistory(account.getAccountNumber());

            System.out.println();
            System.out.printf("Account: %s | Holder: %s | Current Balance: $%,.2f%n",
                    account.getAccountNumber(), customer.getName(), account.getBalance());
            System.out.println();

            if (history.isEmpty()) {
                System.out.println(">> No transaction history recorded for this account.");
                return;
            }

            String border = "+---------------------+------------+-----------------+----------------+----------------+----------------------------------------+";
            System.out.println(border);
            System.out.printf("| %-19s | %-10s | %-15s | %-14s | %-14s | %-38s |%n",
                    "Date & Time", "Txn ID", "Type", "Amount", "Balance After", "Description");
            System.out.println(border);

            for (Transaction t : history) {
                String amountStr = (t.getType().isCredit() ? "+$" : "-$") + String.format("%,.2f", t.getAmount());
                String balStr = String.format("$%,.2f", t.getBalanceAfter());

                System.out.printf("| %-19s | %-10s | %-15s | %14s | %14s | %-38s |%n",
                        t.getFormattedTimestamp(),
                        t.getTransactionId(),
                        t.getType().getDisplayName(),
                        amountStr,
                        balStr,
                        truncate(t.getDescription(), 38));
            }
            System.out.println(border);
            System.out.printf("Total Transactions: %d%n", history.size());
        } catch (BankingException e) {
            System.out.printf(">> ERROR: %s%n", e.getMessage());
        }
    }

    // ==========================================
    // 11. Display Bank Summary
    // ==========================================
    private void handleDisplayBankSummary() {
        BankSummary summary = bankService.getBankSummary();

        System.out.println("========================================");
        System.out.println("          BANK SUMMARY REPORT");
        System.out.println("========================================");
        System.out.printf(" Total Customers Registered : %d%n", summary.getTotalCustomers());
        System.out.printf(" Total Accounts Managed     : %d%n", summary.getTotalAccounts());
        System.out.printf(" Total Deposits Held        : $%,.2f%n", summary.getTotalDepositsHeld());
        System.out.printf(" Total Transactions Logged  : %d%n", summary.getTotalTransactions());
        System.out.printf(" Total Amount Deposited     : $%,.2f%n", summary.getTotalAmountDeposited());
        System.out.printf(" Total Amount Withdrawn     : $%,.2f%n", summary.getTotalAmountWithdrawn());
        System.out.println("========================================");
    }

    // ==========================================
    // Input Helpers with validation loops
    // ==========================================

    private String promptValidName(String prompt) {
        while (true) {
            String val = InputValidator.readString(scanner, prompt, false);
            try {
                InputValidator.validateName(val);
                return val;
            } catch (ValidationException e) {
                System.out.printf(">> %s%n", e.getMessage());
            }
        }
    }

    private int promptValidAge(String prompt) {
        while (true) {
            int age = InputValidator.readInt(scanner, prompt, 1, 150);
            try {
                InputValidator.validateAge(age);
                return age;
            } catch (ValidationException e) {
                System.out.printf(">> %s%n", e.getMessage());
            }
        }
    }

    private String promptValidPhone(String prompt) {
        while (true) {
            String val = InputValidator.readString(scanner, prompt, false);
            try {
                InputValidator.validatePhoneNumber(val);
                return val;
            } catch (ValidationException e) {
                System.out.printf(">> %s%n", e.getMessage());
            }
        }
    }

    private String promptValidEmail(String prompt) {
        while (true) {
            String val = InputValidator.readString(scanner, prompt, false);
            try {
                InputValidator.validateEmail(val);
                return val;
            } catch (ValidationException e) {
                System.out.printf(">> %s%n", e.getMessage());
            }
        }
    }

    private String promptValidAddress(String prompt) {
        while (true) {
            String val = InputValidator.readString(scanner, prompt, false);
            try {
                InputValidator.validateAddress(val);
                return val;
            } catch (ValidationException e) {
                System.out.printf(">> %s%n", e.getMessage());
            }
        }
    }

    private String promptOptionalName(String prompt, String currentVal) {
        while (true) {
            String val = InputValidator.readString(scanner, prompt, true);
            if (val.isEmpty()) {
                return currentVal;
            }
            try {
                InputValidator.validateName(val);
                return val;
            } catch (ValidationException e) {
                System.out.printf(">> %s%n", e.getMessage());
            }
        }
    }

    private Integer promptOptionalAge(String prompt, int currentVal) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine();
            if (line == null || line.trim().isEmpty()) {
                return currentVal;
            }
            try {
                int age = Integer.parseInt(line.trim());
                InputValidator.validateAge(age);
                return age;
            } catch (NumberFormatException e) {
                System.out.println(">> Error: Please enter a valid whole number for age.");
            } catch (ValidationException e) {
                System.out.printf(">> %s%n", e.getMessage());
            }
        }
    }

    private String promptOptionalPhone(String prompt, String currentVal) {
        while (true) {
            String val = InputValidator.readString(scanner, prompt, true);
            if (val.isEmpty()) {
                return currentVal;
            }
            try {
                InputValidator.validatePhoneNumber(val);
                return val;
            } catch (ValidationException e) {
                System.out.printf(">> %s%n", e.getMessage());
            }
        }
    }

    private String promptOptionalEmail(String prompt, String currentVal) {
        while (true) {
            String val = InputValidator.readString(scanner, prompt, true);
            if (val.isEmpty()) {
                return currentVal;
            }
            try {
                InputValidator.validateEmail(val);
                return val;
            } catch (ValidationException e) {
                System.out.printf(">> %s%n", e.getMessage());
            }
        }
    }

    private String promptOptionalAddress(String prompt, String currentVal) {
        while (true) {
            String val = InputValidator.readString(scanner, prompt, true);
            if (val.isEmpty()) {
                return currentVal;
            }
            try {
                InputValidator.validateAddress(val);
                return val;
            } catch (ValidationException e) {
                System.out.printf(">> %s%n", e.getMessage());
            }
        }
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}
