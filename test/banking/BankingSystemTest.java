package banking;

import banking.exception.AccountNotFoundException;
import banking.exception.BankingException;
import banking.exception.InsufficientFundsException;
import banking.exception.ValidationException;
import banking.model.BankAccount;
import banking.model.BankSummary;
import banking.model.Customer;
import banking.model.Transaction;
import banking.model.TransactionType;
import banking.service.BankService;
import banking.service.FileStorageService;
import banking.util.InputValidator;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;

/**
 * Comprehensive automated test suite verifying all 10 required test scenarios.
 */
public class BankingSystemTest {

    private static final String TEST_DATA_DIR = "test_data";
    private static int testsRun = 0;
    private static int testsPassed = 0;

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  STARTING JAVA BANKING SYSTEM AUTOMATED TESTS   ");
        System.out.println("=================================================");

        // Clean up test data directory if it exists
        cleanupDirectory(Paths.get(TEST_DATA_DIR));

        try {
            FileStorageService storageService = new FileStorageService(TEST_DATA_DIR);
            BankService bankService = new BankService(storageService);

            testScenario1_AddMultipleCustomers(bankService);
            testScenario2_SearchAndUpdateCustomer(bankService);
            testScenario3_DepositAndWithdrawMoney(bankService);
            testScenario4_PreventWithdrawalInsufficientFunds(bankService);
            testScenario5_TransferAndMoneyConservation(bankService);
            testScenario6_PreventInvalidTransfers(bankService);
            testScenario7_DeleteAccountNonZeroBalanceConstraint(bankService);
            testScenario8_DataPersistenceAndReload();
            testScenario9_InputValidation();
            testScenario10_BankSummaryVerification(bankService);

        } catch (Exception e) {
            System.err.println("Fatal unexpected test error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanupDirectory(Paths.get(TEST_DATA_DIR));
        }

        System.out.println("=================================================");
        System.out.printf("  TEST RESULTS: %d / %d PASSED%n", testsPassed, testsRun);
        System.out.println("=================================================");

        if (testsRun > 0 && testsPassed == testsRun) {
            System.out.println("  ALL TESTS PASSED SUCCESSFULLY!                 ");
        } else {
            System.err.println("  SOME TESTS FAILED!                             ");
            System.exit(1);
        }
    }

    private static void assertTrue(String testName, boolean condition, String failureMessage) {
        testsRun++;
        if (condition) {
            testsPassed++;
            System.out.printf("  [PASS] %s%n", testName);
        } else {
            System.err.printf("  [FAIL] %s: %s%n", testName, failureMessage);
        }
    }

    private static void assertEquals(String testName, Object expected, Object actual) {
        testsRun++;
        boolean equal = (expected == null && actual == null) || (expected != null && expected.equals(actual));
        if (equal) {
            testsPassed++;
            System.out.printf("  [PASS] %s%n", testName);
        } else {
            System.err.printf("  [FAIL] %s: Expected [%s] but found [%s]%n", testName, expected, actual);
        }
    }

    // -------------------------------------------------------------
    // Scenario 1: Adding multiple customers & auto-generating IDs
    // -------------------------------------------------------------
    private static void testScenario1_AddMultipleCustomers(BankService bank) throws BankingException {
        System.out.println("\n--- Scenario 1: Adding Multiple Customers & Auto-Generation ---");

        Customer c1 = bank.addCustomer("Alice Johnson", 29, "+1-555-123-0101", "alice@example.com", "123 Maple St", new BigDecimal("1000.00"));
        Customer c2 = bank.addCustomer("Bob Smith", 42, "+1-555-123-0202", "bob@example.com", "456 Oak Ave", new BigDecimal("500.00"));
        Customer c3 = bank.addCustomer("Charlie Brown", 19, "+1-555-123-0303", "charlie@example.com", "789 Pine Rd", BigDecimal.ZERO);

        assertEquals("Customer 1 ID format", "CUST1001", c1.getCustomerId());
        assertEquals("Customer 1 Account format", "ACC100001", c1.getAccountNumber());
        assertEquals("Customer 2 ID format", "CUST1002", c2.getCustomerId());
        assertEquals("Customer 2 Account format", "ACC100002", c2.getAccountNumber());
        assertEquals("Customer 3 ID format", "CUST1003", c3.getCustomerId());
        assertEquals("Customer 3 Account format", "ACC100003", c3.getAccountNumber());

        assertEquals("Total registered customers is 3", 3, bank.getAllCustomers().size());
        assertEquals("Alice initial balance is $1000.00", new BigDecimal("1000.00"), bank.checkBalance(c1.getAccountNumber()));
        assertEquals("Charlie initial balance is $0.00", new BigDecimal("0.00"), bank.checkBalance(c3.getAccountNumber()));
    }

    // -------------------------------------------------------------
    // Scenario 2: Searching and updating customers
    // -------------------------------------------------------------
    private static void testScenario2_SearchAndUpdateCustomer(BankService bank) throws BankingException {
        System.out.println("\n--- Scenario 2: Searching and Updating Customers ---");

        // Search by ID
        List<Customer> searchById = bank.searchCustomers("CUST1001");
        assertTrue("Search by Customer ID found Alice", searchById.size() == 1 && searchById.get(0).getName().equals("Alice Johnson"),
                "Expected Alice Johnson");

        // Search by Account Number
        List<Customer> searchByAcc = bank.searchCustomers("ACC100002");
        assertTrue("Search by Account Number found Bob", searchByAcc.size() == 1 && searchByAcc.get(0).getName().equals("Bob Smith"),
                "Expected Bob Smith");

        // Search by Name
        List<Customer> searchByName = bank.searchCustomers("charlie");
        assertTrue("Search by Name found Charlie", searchByName.size() == 1 && searchByName.get(0).getName().equals("Charlie Brown"),
                "Expected Charlie Brown");

        // Update details for Bob
        bank.updateCustomer("CUST1002", null, 43, "+1-555-123-9999", "bob.updated@example.com", "999 Elm Blvd");
        Customer updatedBob = bank.getCustomerById("CUST1002");

        assertEquals("Bob's name remains unchanged", "Bob Smith", updatedBob.getName());
        assertEquals("Bob's age updated to 43", 43, updatedBob.getAge());
        assertEquals("Bob's phone updated", "+1-555-123-9999", updatedBob.getPhoneNumber());
        assertEquals("Bob's email updated", "bob.updated@example.com", updatedBob.getEmail());
        assertEquals("Bob's address updated", "999 Elm Blvd", updatedBob.getAddress());
        assertEquals("Bob's account number remains immutable", "ACC100002", updatedBob.getAccountNumber());
    }

    // -------------------------------------------------------------
    // Scenario 3: Depositing and withdrawing money
    // -------------------------------------------------------------
    private static void testScenario3_DepositAndWithdrawMoney(BankService bank) throws BankingException {
        System.out.println("\n--- Scenario 3: Depositing and Withdrawing Money ---");

        // Deposit $250.00 to Alice
        Transaction depTxn = bank.deposit("ACC100001", new BigDecimal("250.00"), "Bonus Deposit");
        assertEquals("Deposit amount is $250.00", new BigDecimal("250.00"), depTxn.getAmount());
        assertEquals("Alice balance after deposit is $1250.00", new BigDecimal("1250.00"), bank.checkBalance("ACC100001"));
        assertEquals("Deposit transaction type", TransactionType.DEPOSIT, depTxn.getType());

        // Withdraw $100.00 from Alice
        Transaction wthTxn = bank.withdraw("ACC100001", new BigDecimal("100.00"), "ATM Cash Withdrawal");
        assertEquals("Withdrawal amount is $100.00", new BigDecimal("100.00"), wthTxn.getAmount());
        assertEquals("Alice balance after withdrawal is $1150.00", new BigDecimal("1150.00"), bank.checkBalance("ACC100001"));
        assertEquals("Withdrawal transaction type", TransactionType.WITHDRAWAL, wthTxn.getType());
    }

    // -------------------------------------------------------------
    // Scenario 4: Preventing withdrawals with insufficient funds
    // -------------------------------------------------------------
    private static void testScenario4_PreventWithdrawalInsufficientFunds(BankService bank) throws BankingException {
        System.out.println("\n--- Scenario 4: Preventing Withdrawals with Insufficient Funds ---");

        BigDecimal balanceBefore = bank.checkBalance("ACC100001"); // $1150.00
        boolean caughtInsufficient = false;

        try {
            bank.withdraw("ACC100001", new BigDecimal("2000.00"), "Excessive withdrawal");
        } catch (InsufficientFundsException e) {
            caughtInsufficient = true;
        }

        assertTrue("Withdrawal exceeding balance throws InsufficientFundsException", caughtInsufficient,
                "Did not throw InsufficientFundsException");
        assertEquals("Balance remains unchanged after declined withdrawal", balanceBefore, bank.checkBalance("ACC100001"));

        // Test invalid amounts (negative/zero)
        boolean caughtInvalidAmount = false;
        try {
            bank.withdraw("ACC100001", new BigDecimal("-50.00"), "Negative withdrawal");
        } catch (ValidationException e) {
            caughtInvalidAmount = true;
        }
        assertTrue("Negative withdrawal throws ValidationException", caughtInvalidAmount,
                "Did not throw ValidationException for negative amount");
    }

    // -------------------------------------------------------------
    // Scenario 5: Transferring money & conservation of money
    // -------------------------------------------------------------
    private static void testScenario5_TransferAndMoneyConservation(BankService bank) throws BankingException {
        System.out.println("\n--- Scenario 5: Transferring Money & Verifying Conservation of Money ---");

        BigDecimal aliceInitial = bank.checkBalance("ACC100001"); // $1150.00
        BigDecimal bobInitial = bank.checkBalance("ACC100002");   // $500.00
        BigDecimal totalBefore = aliceInitial.add(bobInitial);

        BigDecimal transferAmount = new BigDecimal("300.00");
        bank.transfer("ACC100001", "ACC100002", transferAmount, "Payment for services");

        BigDecimal aliceAfter = bank.checkBalance("ACC100001");
        BigDecimal bobAfter = bank.checkBalance("ACC100002");
        BigDecimal totalAfter = aliceAfter.add(bobAfter);

        assertEquals("Alice balance reduced by $300.00", new BigDecimal("850.00"), aliceAfter);
        assertEquals("Bob balance increased by $300.00", new BigDecimal("800.00"), bobAfter);
        assertEquals("Conservation of money verified: total funds before equals total funds after", totalBefore, totalAfter);

        // Verify transaction histories
        List<Transaction> aliceHistory = bank.getTransactionHistory("ACC100001");
        Transaction lastAliceTxn = aliceHistory.get(aliceHistory.size() - 1);
        assertEquals("Alice has TRANSFER_OUT transaction", TransactionType.TRANSFER_OUT, lastAliceTxn.getType());
        assertEquals("Alice TRANSFER_OUT amount", transferAmount, lastAliceTxn.getAmount());

        List<Transaction> bobHistory = bank.getTransactionHistory("ACC100002");
        Transaction lastBobTxn = bobHistory.get(bobHistory.size() - 1);
        assertEquals("Bob has TRANSFER_IN transaction", TransactionType.TRANSFER_IN, lastBobTxn.getType());
        assertEquals("Bob TRANSFER_IN amount", transferAmount, lastBobTxn.getAmount());
    }

    // -------------------------------------------------------------
    // Scenario 6: Preventing invalid transfers
    // -------------------------------------------------------------
    private static void testScenario6_PreventInvalidTransfers(BankService bank) {
        System.out.println("\n--- Scenario 6: Preventing Invalid Transfers ---");

        // Transfer to self
        boolean caughtSameAccount = false;
        try {
            bank.transfer("ACC100001", "ACC100001", new BigDecimal("50.00"), "Self transfer");
        } catch (BankingException e) {
            caughtSameAccount = true;
        }
        assertTrue("Transfer to same account rejected", caughtSameAccount, "Failed to reject transfer to same account");

        // Transfer from non-existent account
        boolean caughtMissingSource = false;
        try {
            bank.transfer("ACC999999", "ACC100001", new BigDecimal("50.00"), "From ghost");
        } catch (AccountNotFoundException e) {
            caughtMissingSource = true;
        } catch (BankingException ignored) {}
        assertTrue("Transfer from non-existent account rejected", caughtMissingSource, "Failed to reject non-existent source");

        // Transfer to non-existent account
        boolean caughtMissingDest = false;
        try {
            bank.transfer("ACC100001", "ACC999999", new BigDecimal("50.00"), "To ghost");
        } catch (AccountNotFoundException e) {
            caughtMissingDest = true;
        } catch (BankingException ignored) {}
        assertTrue("Transfer to non-existent account rejected", caughtMissingDest, "Failed to reject non-existent destination");

        // Transfer with insufficient funds
        boolean caughtInsufficient = false;
        try {
            bank.transfer("ACC100001", "ACC100002", new BigDecimal("50000.00"), "Overdraft transfer");
        } catch (InsufficientFundsException e) {
            caughtInsufficient = true;
        } catch (BankingException ignored) {}
        assertTrue("Transfer exceeding balance rejected", caughtInsufficient, "Failed to reject overdraft transfer");
    }

    // -------------------------------------------------------------
    // Scenario 7: Deleting an account with non-zero balance
    // -------------------------------------------------------------
    private static void testScenario7_DeleteAccountNonZeroBalanceConstraint(BankService bank) throws BankingException {
        System.out.println("\n--- Scenario 7: Deleting Account & Balance Constraints ---");

        // Alice has balance $850.00 -> Deletion must be rejected
        boolean caughtNonZeroDelete = false;
        try {
            bank.deleteCustomer("CUST1001");
        } catch (BankingException e) {
            caughtNonZeroDelete = true;
        }
        assertTrue("Deleting customer with non-zero balance is blocked", caughtNonZeroDelete,
                "Did not prevent deleting customer with balance > 0");

        // Charlie has balance $0.00 -> Deletion must succeed
        bank.deleteCustomer("CUST1003");
        boolean charlieFound = false;
        try {
            bank.getCustomerById("CUST1003");
            charlieFound = true;
        } catch (BankingException ignored) {}

        assertTrue("Charlie with $0 balance was deleted successfully", !charlieFound, "Charlie was not deleted");
        assertEquals("Registered customer count is now 2", 2, bank.getAllCustomers().size());
    }

    // -------------------------------------------------------------
    // Scenario 8: Saving and reloading data after restarting
    // -------------------------------------------------------------
    private static void testScenario8_DataPersistenceAndReload() throws BankingException {
        System.out.println("\n--- Scenario 8: Saving and Reloading Data (Persistence) ---");

        // Create a completely new BankService instance simulating an application restart
        FileStorageService newStorage = new FileStorageService(TEST_DATA_DIR);
        BankService restartedBank = new BankService(newStorage);

        List<Customer> reloadedCustomers = restartedBank.getAllCustomers();
        assertEquals("Reloaded customer count matches", 2, reloadedCustomers.size());

        Customer reloadedAlice = restartedBank.getCustomerById("CUST1001");
        assertEquals("Reloaded Alice name", "Alice Johnson", reloadedAlice.getName());

        BigDecimal aliceBalance = restartedBank.checkBalance(reloadedAlice.getAccountNumber());
        assertEquals("Reloaded Alice balance is $850.00", new BigDecimal("850.00"), aliceBalance);

        // Verify ID counter continues seamlessly and doesn't reuse CUST1001/CUST1002/CUST1003
        Customer c4 = restartedBank.addCustomer("David Miller", 35, "+1-555-123-0404", "david@example.com", "321 Cedar Ln", new BigDecimal("150.00"));
        assertEquals("New customer after reload has sequential ID CUST1004", "CUST1004", c4.getCustomerId());
        assertEquals("New customer after reload has sequential Account ACC100004", "ACC100004", c4.getAccountNumber());
    }

    // -------------------------------------------------------------
    // Scenario 9: Handling invalid inputs and validation rules
    // -------------------------------------------------------------
    private static void testScenario9_InputValidation() {
        System.out.println("\n--- Scenario 9: Input Validation Rules ---");

        // Invalid Age (< 18)
        assertThrowsValidation("Age 17 rejected", () -> InputValidator.validateAge(17));
        assertThrowsValidation("Age 121 rejected", () -> InputValidator.validateAge(121));

        // Invalid Names
        assertThrowsValidation("Empty name rejected", () -> InputValidator.validateName(""));
        assertThrowsValidation("Single character name rejected", () -> InputValidator.validateName("A"));
        assertThrowsValidation("Numeric characters in name rejected", () -> InputValidator.validateName("Alice123"));

        // Invalid Emails
        assertThrowsValidation("Missing @ rejected", () -> InputValidator.validateEmail("invalid-email.com"));
        assertThrowsValidation("Missing domain rejected", () -> InputValidator.validateEmail("user@"));

        // Invalid Phones
        assertThrowsValidation("Too short phone rejected", () -> InputValidator.validatePhoneNumber("12345"));
        assertThrowsValidation("Alphabetic phone rejected", () -> InputValidator.validatePhoneNumber("abc-def-ghij"));

        // Invalid Amounts
        assertThrowsValidation("Zero amount rejected", () -> InputValidator.validateTransactionAmount(BigDecimal.ZERO));
        assertThrowsValidation("Negative amount rejected", () -> InputValidator.validateTransactionAmount(new BigDecimal("-10.00")));
    }

    private static void assertThrowsValidation(String testName, ThrowingRunnable runnable) {
        testsRun++;
        try {
            runnable.run();
            System.err.printf("  [FAIL] %s: Expected ValidationException was not thrown%n", testName);
        } catch (ValidationException e) {
            testsPassed++;
            System.out.printf("  [PASS] %s%n", testName);
        } catch (Exception e) {
            System.err.printf("  [FAIL] %s: Unexpected exception type: %s%n", testName, e.getClass().getName());
        }
    }

    // -------------------------------------------------------------
    // Scenario 10: Bank summary accuracy
    // -------------------------------------------------------------
    private static void testScenario10_BankSummaryVerification(BankService bank) {
        System.out.println("\n--- Scenario 10: Verifying Consolidated Bank Summary ---");

        BankSummary summary = bank.getBankSummary();
        assertTrue("Bank summary customer count >= 2", summary.getTotalCustomers() >= 2, "Customer count mismatch");
        assertTrue("Bank summary deposits held > 0", summary.getTotalDepositsHeld().compareTo(BigDecimal.ZERO) > 0, "Deposits held should be > 0");
        assertTrue("Bank summary transactions count > 0", summary.getTotalTransactions() > 0, "Transactions count should be > 0");
        assertTrue("Bank summary total deposited > 0", summary.getTotalAmountDeposited().compareTo(BigDecimal.ZERO) > 0, "Total deposited should be > 0");
        assertTrue("Bank summary total withdrawn > 0", summary.getTotalAmountWithdrawn().compareTo(BigDecimal.ZERO) > 0, "Total withdrawn should be > 0");
    }

    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static void cleanupDirectory(Path dir) {
        if (!Files.exists(dir)) {
            return;
        }
        try {
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {}
                    });
        } catch (IOException ignored) {}
    }
}
