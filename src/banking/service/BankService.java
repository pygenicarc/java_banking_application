package banking.service;

import banking.exception.AccountNotFoundException;
import banking.exception.BankingException;
import banking.exception.CustomerNotFoundException;
import banking.exception.InsufficientFundsException;
import banking.exception.ValidationException;
import banking.model.BankAccount;
import banking.model.BankSummary;
import banking.model.Customer;
import banking.model.Transaction;
import banking.model.TransactionType;
import banking.util.InputValidator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Core business service handling customer lifecycle, banking operations,
 * transaction auditing, and financial consistency.
 */
public class BankService {

    private final FileStorageService storageService;

    // In-memory registry (keyed by customerId and accountNumber)
    private final Map<String, Customer> customers = new LinkedHashMap<>();
    private final Map<String, BankAccount> accounts = new LinkedHashMap<>();
    private final List<Transaction> transactions = new ArrayList<>();

    // ID Sequence generators
    private final AtomicLong customerIdSeq = new AtomicLong(1000);
    private final AtomicLong accountNumberSeq = new AtomicLong(100000);
    private final AtomicLong transactionIdSeq = new AtomicLong(100000);

    /**
     * Initializes BankService using the default FileStorageService.
     */
    public BankService() {
        this(new FileStorageService());
    }

    /**
     * Initializes BankService with a custom FileStorageService.
     */
    public BankService(FileStorageService storageService) {
        this.storageService = storageService;
        loadDataFromStorage();
    }

    /**
     * Loads persisted records and initializes the ID sequences to prevent collision.
     */
    private synchronized void loadDataFromStorage() {
        customers.clear();
        accounts.clear();
        transactions.clear();

        long[] seqs = storageService.loadSequences();
        customerIdSeq.set(seqs[0]);
        accountNumberSeq.set(seqs[1]);
        transactionIdSeq.set(seqs[2]);

        List<Customer> loadedCustomers = storageService.loadCustomers();
        List<BankAccount> loadedAccounts = storageService.loadAccounts();
        List<Transaction> loadedTransactions = storageService.loadTransactions();

        for (Customer c : loadedCustomers) {
            customers.put(c.getCustomerId(), c);
            extractAndAdvanceId(c.getCustomerId(), "CUST", customerIdSeq);
        }

        for (BankAccount a : loadedAccounts) {
            accounts.put(a.getAccountNumber(), a);
            extractAndAdvanceId(a.getAccountNumber(), "ACC", accountNumberSeq);
        }

        for (Transaction t : loadedTransactions) {
            transactions.add(t);
            extractAndAdvanceId(t.getTransactionId(), "TXN", transactionIdSeq);
        }
    }

    private void extractAndAdvanceId(String idStr, String prefix, AtomicLong sequence) {
        if (idStr != null && idStr.startsWith(prefix)) {
            try {
                long val = Long.parseLong(idStr.substring(prefix.length()));
                sequence.accumulateAndGet(val, Math::max);
            } catch (NumberFormatException ignored) {}
        }
    }

    private String generateNextCustomerId() {
        return "CUST" + customerIdSeq.incrementAndGet();
    }

    private String generateNextAccountNumber() {
        return "ACC" + accountNumberSeq.incrementAndGet();
    }

    private String generateNextTransactionId() {
        return "TXN" + transactionIdSeq.incrementAndGet();
    }

    // ==========================================
    // Customer Management Operations
    // ==========================================

    /**
     * Adds a new customer, creates a corresponding bank account, and optionally registers an opening deposit.
     */
    public synchronized Customer addCustomer(String name,
                                             int age,
                                             String phone,
                                             String email,
                                             String address,
                                             BigDecimal openingDeposit) throws BankingException {
        // Validation
        InputValidator.validateName(name);
        InputValidator.validateAge(age);
        InputValidator.validatePhoneNumber(phone);
        InputValidator.validateEmail(email);
        InputValidator.validateAddress(address);
        InputValidator.validateOpeningDeposit(openingDeposit);

        String customerId = generateNextCustomerId();
        String accountNumber = generateNextAccountNumber();

        BigDecimal initialBalance = (openingDeposit != null) ? openingDeposit.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        BankAccount account = new BankAccount(accountNumber, customerId, initialBalance);
        Customer customer = new Customer(customerId, name, age, phone, email, address, accountNumber);

        customers.put(customerId, customer);
        accounts.put(accountNumber, account);

        // Record opening deposit transaction if amount is greater than zero
        if (initialBalance.compareTo(BigDecimal.ZERO) > 0) {
            Transaction openingTxn = new Transaction(
                    generateNextTransactionId(),
                    accountNumber,
                    TransactionType.OPENING_DEPOSIT,
                    initialBalance,
                    initialBalance,
                    LocalDateTime.now(),
                    "Opening Account Deposit"
            );
            transactions.add(openingTxn);
        }

        // Persist state
        storageService.saveSequences(customerIdSeq.get(), accountNumberSeq.get(), transactionIdSeq.get());
        storageService.saveAll(customers.values(), accounts.values(), transactions);

        return customer;
    }

    /**
     * Retrieves an unmodifiable view of all customers.
     */
    public synchronized List<Customer> getAllCustomers() {
        return Collections.unmodifiableList(new ArrayList<>(customers.values()));
    }

    /**
     * Finds a customer by their unique customer ID.
     */
    public synchronized Customer getCustomerById(String customerId) throws CustomerNotFoundException {
        if (customerId == null) {
            throw new CustomerNotFoundException("null");
        }
        Customer customer = customers.get(customerId.trim().toUpperCase());
        if (customer == null) {
            // Also check raw string if case varies
            customer = customers.get(customerId.trim());
        }
        if (customer == null) {
            throw new CustomerNotFoundException(customerId);
        }
        return customer;
    }

    /**
     * Finds a customer associated with an account number.
     */
    public synchronized Customer getCustomerByAccountNumber(String accountNumber) throws AccountNotFoundException {
        if (accountNumber == null) {
            throw new AccountNotFoundException("null");
        }
        String accNorm = accountNumber.trim().toUpperCase();
        for (Customer c : customers.values()) {
            if (c.getAccountNumber().equalsIgnoreCase(accNorm)) {
                return c;
            }
        }
        throw new AccountNotFoundException(accountNumber);
    }

    /**
     * Method overloading: Searches customers matching query across Customer ID, Account Number, or Name.
     */
    public synchronized List<Customer> searchCustomers(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String q = query.trim().toLowerCase();
        return customers.values().stream()
                .filter(c -> c.getCustomerId().toLowerCase().contains(q)
                        || c.getAccountNumber().toLowerCase().contains(q)
                        || c.getName().toLowerCase().contains(q))
                .collect(Collectors.toList());
    }

    /**
     * Method overloading: Searches customer by specific field criteria.
     */
    public synchronized List<Customer> searchCustomersByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String q = name.trim().toLowerCase();
        return customers.values().stream()
                .filter(c -> c.getName().toLowerCase().contains(q))
                .collect(Collectors.toList());
    }

    /**
     * Updates an existing customer's contact details.
     */
    public synchronized Customer updateCustomer(String customerId,
                                                String newName,
                                                Integer newAge,
                                                String newPhone,
                                                String newEmail,
                                                String newAddress) throws BankingException {
        Customer customer = getCustomerById(customerId);

        if (newName != null && !newName.trim().isEmpty()) {
            InputValidator.validateName(newName);
            customer.setName(newName.trim());
        }
        if (newAge != null) {
            InputValidator.validateAge(newAge);
            customer.setAge(newAge);
        }
        if (newPhone != null && !newPhone.trim().isEmpty()) {
            InputValidator.validatePhoneNumber(newPhone);
            customer.setPhoneNumber(newPhone.trim());
        }
        if (newEmail != null && !newEmail.trim().isEmpty()) {
            InputValidator.validateEmail(newEmail);
            customer.setEmail(newEmail.trim());
        }
        if (newAddress != null && !newAddress.trim().isEmpty()) {
            InputValidator.validateAddress(newAddress);
            customer.setAddress(newAddress.trim());
        }

        storageService.saveCustomers(customers.values());
        return customer;
    }

    /**
     * Deletes a customer. Refuses deletion if the account has a non-zero balance.
     */
    public synchronized void deleteCustomer(String customerId) throws BankingException {
        Customer customer = getCustomerById(customerId);
        String accountNumber = customer.getAccountNumber();
        BankAccount account = accounts.get(accountNumber);

        if (account != null && !account.hasZeroBalance()) {
            throw new BankingException(String.format(
                    "Cannot delete customer %s (%s). Associated account %s has an active balance of $%,.2f. " +
                    "Account balance must be settled to $0.00 prior to account closure and deletion.",
                    customer.getName(), customer.getCustomerId(), accountNumber, account.getBalance()));
        }

        // Remove from registries
        customers.remove(customer.getCustomerId());
        if (accountNumber != null) {
            accounts.remove(accountNumber);
        }

        storageService.saveSequences(customerIdSeq.get(), accountNumberSeq.get(), transactionIdSeq.get());
        storageService.saveAll(customers.values(), accounts.values(), transactions);
    }

    // ==========================================
    // Banking Operations
    // ==========================================

    /**
     * Retrieves account by account number.
     */
    public synchronized BankAccount getAccount(String accountNumber) throws AccountNotFoundException {
        if (accountNumber == null) {
            throw new AccountNotFoundException("null");
        }
        String accNorm = accountNumber.trim().toUpperCase();
        BankAccount account = accounts.get(accNorm);
        if (account == null) {
            account = accounts.get(accountNumber.trim());
        }
        if (account == null) {
            throw new AccountNotFoundException(accountNumber);
        }
        return account;
    }

    /**
     * Checks balance for an account number.
     */
    public synchronized BigDecimal checkBalance(String accountNumber) throws AccountNotFoundException {
        BankAccount account = getAccount(accountNumber);
        return account.getBalance();
    }

    /**
     * Deposits money into an account.
     */
    public synchronized Transaction deposit(String accountNumber, BigDecimal amount, String description) throws BankingException {
        InputValidator.validateTransactionAmount(amount);
        BankAccount account = getAccount(accountNumber);

        BigDecimal newBalance = account.deposit(amount);
        String note = (description != null && !description.trim().isEmpty()) ? description.trim() : "Cash Deposit";

        Transaction txn = new Transaction(
                generateNextTransactionId(),
                account.getAccountNumber(),
                TransactionType.DEPOSIT,
                amount,
                newBalance,
                LocalDateTime.now(),
                note
        );

        transactions.add(txn);
        storageService.saveSequences(customerIdSeq.get(), accountNumberSeq.get(), transactionIdSeq.get());
        storageService.saveAccounts(accounts.values());
        storageService.saveTransactions(transactions);

        return txn;
    }

    /**
     * Withdraws money from an account.
     */
    public synchronized Transaction withdraw(String accountNumber, BigDecimal amount, String description) throws BankingException {
        InputValidator.validateTransactionAmount(amount);
        BankAccount account = getAccount(accountNumber);

        BigDecimal newBalance = account.withdraw(amount);
        String note = (description != null && !description.trim().isEmpty()) ? description.trim() : "Cash Withdrawal";

        Transaction txn = new Transaction(
                generateNextTransactionId(),
                account.getAccountNumber(),
                TransactionType.WITHDRAWAL,
                amount,
                newBalance,
                LocalDateTime.now(),
                note
        );

        transactions.add(txn);
        storageService.saveSequences(customerIdSeq.get(), accountNumberSeq.get(), transactionIdSeq.get());
        storageService.saveAccounts(accounts.values());
        storageService.saveTransactions(transactions);

        return txn;
    }

    /**
     * Atomically transfers money between two distinct accounts.
     * Ensures conservation of money (neither created nor destroyed).
     */
    public synchronized void transfer(String sourceAccNumber,
                                      String destAccNumber,
                                      BigDecimal amount,
                                      String note) throws BankingException {
        if (sourceAccNumber == null || destAccNumber == null) {
            throw new ValidationException("Source and destination account numbers must be provided.");
        }
        String srcNorm = sourceAccNumber.trim().toUpperCase();
        String dstNorm = destAccNumber.trim().toUpperCase();

        if (srcNorm.equalsIgnoreCase(dstNorm)) {
            throw new ValidationException("Source and destination accounts must be different.");
        }

        InputValidator.validateTransactionAmount(amount);

        BankAccount srcAccount = getAccount(srcNorm);
        BankAccount dstAccount = getAccount(dstNorm);

        // Check sufficient funds before debiting
        BigDecimal scaledAmount = amount.setScale(2, RoundingMode.HALF_UP);
        if (srcAccount.getBalance().compareTo(scaledAmount) < 0) {
            throw new InsufficientFundsException(srcAccount.getBalance(), scaledAmount);
        }

        // Perform atomic balance updates
        BigDecimal srcNewBalance = srcAccount.withdraw(scaledAmount);
        BigDecimal dstNewBalance = dstAccount.deposit(scaledAmount);

        String transferNote = (note != null && !note.trim().isEmpty()) ? " - " + note.trim() : "";

        Transaction debitTxn = new Transaction(
                generateNextTransactionId(),
                srcAccount.getAccountNumber(),
                TransactionType.TRANSFER_OUT,
                scaledAmount,
                srcNewBalance,
                LocalDateTime.now(),
                "Transfer to " + dstAccount.getAccountNumber() + transferNote
        );

        Transaction creditTxn = new Transaction(
                generateNextTransactionId(),
                dstAccount.getAccountNumber(),
                TransactionType.TRANSFER_IN,
                scaledAmount,
                dstNewBalance,
                LocalDateTime.now(),
                "Transfer from " + srcAccount.getAccountNumber() + transferNote
        );

        transactions.add(debitTxn);
        transactions.add(creditTxn);

        storageService.saveSequences(customerIdSeq.get(), accountNumberSeq.get(), transactionIdSeq.get());
        storageService.saveAccounts(accounts.values());
        storageService.saveTransactions(transactions);
    }

    /**
     * Retrieves transaction history for an account.
     */
    public synchronized List<Transaction> getTransactionHistory(String accountNumber) throws AccountNotFoundException {
        // Validate account exists
        BankAccount account = getAccount(accountNumber);
        String targetAcc = account.getAccountNumber();

        return transactions.stream()
                .filter(t -> t.getAccountNumber().equalsIgnoreCase(targetAcc))
                .collect(Collectors.toList());
    }

    /**
     * Computes consolidated bank metrics from current operational state.
     */
    public synchronized BankSummary getBankSummary() {
        int totalCustomers = customers.size();
        int totalAccounts = accounts.size();

        BigDecimal totalDepositsHeld = accounts.values().stream()
                .map(BankAccount::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        int totalTxnCount = transactions.size();

        BigDecimal totalDeposited = transactions.stream()
                .filter(t -> t.getType() == TransactionType.DEPOSIT || t.getType() == TransactionType.OPENING_DEPOSIT)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalWithdrawn = transactions.stream()
                .filter(t -> t.getType() == TransactionType.WITHDRAWAL)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return new BankSummary(
                totalCustomers,
                totalAccounts,
                totalDepositsHeld,
                totalTxnCount,
                totalDeposited,
                totalWithdrawn
        );
    }
}
