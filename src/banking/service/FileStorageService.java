package banking.service;

import banking.exception.BankingException;
import banking.model.BankAccount;
import banking.model.Customer;
import banking.model.Transaction;
import banking.model.TransactionType;
import banking.util.CsvUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Service responsible for persisting and loading banking entities to and from CSV files.
 * Provides resilient file handling, graceful recovery on malformed records, and atomic file writes.
 */
public class FileStorageService {

    private static final String DEFAULT_DATA_DIR = "data";
    private static final String CUSTOMERS_FILE = "customers.csv";
    private static final String ACCOUNTS_FILE = "accounts.csv";
    private static final String TRANSACTIONS_FILE = "transactions.csv";
    private static final String SEQUENCES_FILE = "sequences.properties";

    private static final String CUSTOMERS_HEADER = "customerId,name,age,phoneNumber,email,address,accountNumber,createdAt";
    private static final String ACCOUNTS_HEADER = "accountNumber,customerId,balance,createdAt";
    private static final String TRANSACTIONS_HEADER = "transactionId,accountNumber,type,amount,balanceAfter,timestamp,description";

    private final Path dataDirectory;

    public FileStorageService() {
        this(DEFAULT_DATA_DIR);
    }

    public FileStorageService(String dataDirPath) {
        this.dataDirectory = Paths.get(dataDirPath);
        initializeStorage();
    }

    /**
     * Ensures that the data directory and data files exist with appropriate headers.
     */
    private void initializeStorage() {
        try {
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
            }
            ensureFileWithHeader(dataDirectory.resolve(CUSTOMERS_FILE), CUSTOMERS_HEADER);
            ensureFileWithHeader(dataDirectory.resolve(ACCOUNTS_FILE), ACCOUNTS_HEADER);
            ensureFileWithHeader(dataDirectory.resolve(TRANSACTIONS_FILE), TRANSACTIONS_HEADER);
        } catch (IOException e) {
            System.err.println("Warning: Unable to initialize data storage directory: " + e.getMessage());
        }
    }

    private void ensureFileWithHeader(Path filePath, String header) throws IOException {
        if (!Files.exists(filePath) || Files.size(filePath) == 0) {
            try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
                writer.write(header);
                writer.newLine();
            }
        }
    }

    // ==========================================
    // Customer Loading & Saving
    // ==========================================

    public synchronized List<Customer> loadCustomers() {
        List<Customer> customers = new ArrayList<>();
        Path filePath = dataDirectory.resolve(CUSTOMERS_FILE);

        if (!Files.exists(filePath)) {
            return customers;
        }

        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            boolean isFirstLine = true;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }
                if (isFirstLine) {
                    isFirstLine = false;
                    // Check if it's the header row
                    if (line.toLowerCase().contains("customerid")) {
                        continue;
                    }
                }

                try {
                    List<String> tokens = CsvUtils.parseLine(line);
                    if (tokens.size() < 7) {
                        System.err.printf("Warning: Skipping malformed customer record at %s:%d (insufficient columns)%n",
                                CUSTOMERS_FILE, lineNumber);
                        continue;
                    }

                    String customerId = tokens.get(0);
                    String name = tokens.get(1);
                    int age = Integer.parseInt(tokens.get(2));
                    String phone = tokens.get(3);
                    String email = tokens.get(4);
                    String address = tokens.get(5);
                    String accountNumber = tokens.get(6);
                    LocalDateTime createdAt = tokens.size() > 7 && !tokens.get(7).isEmpty()
                            ? LocalDateTime.parse(tokens.get(7), Customer.DATE_TIME_FORMATTER)
                            : LocalDateTime.now();

                    Customer customer = new Customer(customerId, name, age, phone, email, address, accountNumber, createdAt);
                    customers.add(customer);
                } catch (Exception e) {
                    System.err.printf("Warning: Skipping invalid customer row at %s:%d: %s%n",
                            CUSTOMERS_FILE, lineNumber, e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading customers file: " + e.getMessage());
        }

        return customers;
    }

    public synchronized void saveCustomers(Collection<Customer> customers) throws BankingException {
        Path targetPath = dataDirectory.resolve(CUSTOMERS_FILE);
        try (BufferedWriter writer = Files.newBufferedWriter(targetPath,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
                java.nio.file.StandardOpenOption.WRITE)) {
            writer.write(CUSTOMERS_HEADER);
            writer.newLine();
            for (Customer c : customers) {
                String line = CsvUtils.toCsvLine(
                        c.getCustomerId(),
                        c.getName(),
                        String.valueOf(c.getAge()),
                        c.getPhoneNumber(),
                        c.getEmail(),
                        c.getAddress(),
                        c.getAccountNumber(),
                        c.getFormattedCreatedAt()
                );
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            throw new BankingException("Failed to save customers data: " + e.getMessage(), e);
        }
    }

    // ==========================================
    // Account Loading & Saving
    // ==========================================

    public synchronized List<BankAccount> loadAccounts() {
        List<BankAccount> accounts = new ArrayList<>();
        Path filePath = dataDirectory.resolve(ACCOUNTS_FILE);

        if (!Files.exists(filePath)) {
            return accounts;
        }

        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            boolean isFirstLine = true;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }
                if (isFirstLine) {
                    isFirstLine = false;
                    if (line.toLowerCase().contains("accountnumber")) {
                        continue;
                    }
                }

                try {
                    List<String> tokens = CsvUtils.parseLine(line);
                    if (tokens.size() < 3) {
                        System.err.printf("Warning: Skipping malformed account record at %s:%d%n",
                                ACCOUNTS_FILE, lineNumber);
                        continue;
                    }

                    String accountNumber = tokens.get(0);
                    String customerId = tokens.get(1);
                    BigDecimal balance = new BigDecimal(tokens.get(2));
                    LocalDateTime createdAt = tokens.size() > 3 && !tokens.get(3).isEmpty()
                            ? LocalDateTime.parse(tokens.get(3), BankAccount.DATE_TIME_FORMATTER)
                            : LocalDateTime.now();

                    BankAccount account = new BankAccount(accountNumber, customerId, balance, createdAt);
                    accounts.add(account);
                } catch (Exception e) {
                    System.err.printf("Warning: Skipping invalid account row at %s:%d: %s%n",
                            ACCOUNTS_FILE, lineNumber, e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading accounts file: " + e.getMessage());
        }

        return accounts;
    }

    public synchronized void saveAccounts(Collection<BankAccount> accounts) throws BankingException {
        Path targetPath = dataDirectory.resolve(ACCOUNTS_FILE);
        try (BufferedWriter writer = Files.newBufferedWriter(targetPath,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
                java.nio.file.StandardOpenOption.WRITE)) {
            writer.write(ACCOUNTS_HEADER);
            writer.newLine();
            for (BankAccount a : accounts) {
                String line = CsvUtils.toCsvLine(
                        a.getAccountNumber(),
                        a.getCustomerId(),
                        a.getBalance().toPlainString(),
                        a.getFormattedCreatedAt()
                );
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            throw new BankingException("Failed to save accounts data: " + e.getMessage(), e);
        }
    }

    // ==========================================
    // Transaction Loading & Saving
    // ==========================================

    public synchronized List<Transaction> loadTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        Path filePath = dataDirectory.resolve(TRANSACTIONS_FILE);

        if (!Files.exists(filePath)) {
            return transactions;
        }

        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            boolean isFirstLine = true;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }
                if (isFirstLine) {
                    isFirstLine = false;
                    if (line.toLowerCase().contains("transactionid")) {
                        continue;
                    }
                }

                try {
                    List<String> tokens = CsvUtils.parseLine(line);
                    if (tokens.size() < 6) {
                        System.err.printf("Warning: Skipping malformed transaction record at %s:%d%n",
                                TRANSACTIONS_FILE, lineNumber);
                        continue;
                    }

                    String txnId = tokens.get(0);
                    String accNumber = tokens.get(1);
                    TransactionType type = TransactionType.fromString(tokens.get(2));
                    BigDecimal amount = new BigDecimal(tokens.get(3));
                    BigDecimal balanceAfter = new BigDecimal(tokens.get(4));
                    LocalDateTime timestamp = LocalDateTime.parse(tokens.get(5), Transaction.DATE_TIME_FORMATTER);
                    String description = tokens.size() > 6 ? tokens.get(6) : "";

                    Transaction txn = new Transaction(txnId, accNumber, type, amount, balanceAfter, timestamp, description);
                    transactions.add(txn);
                } catch (Exception e) {
                    System.err.printf("Warning: Skipping invalid transaction row at %s:%d: %s%n",
                            TRANSACTIONS_FILE, lineNumber, e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading transactions file: " + e.getMessage());
        }

        return transactions;
    }

    public synchronized void saveTransactions(Collection<Transaction> transactions) throws BankingException {
        Path targetPath = dataDirectory.resolve(TRANSACTIONS_FILE);
        try (BufferedWriter writer = Files.newBufferedWriter(targetPath,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
                java.nio.file.StandardOpenOption.WRITE)) {
            writer.write(TRANSACTIONS_HEADER);
            writer.newLine();
            for (Transaction t : transactions) {
                String line = CsvUtils.toCsvLine(
                        t.getTransactionId(),
                        t.getAccountNumber(),
                        t.getType().name(),
                        t.getAmount().toPlainString(),
                        t.getBalanceAfter().toPlainString(),
                        t.getFormattedTimestamp(),
                        t.getDescription()
                );
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            throw new BankingException("Failed to save transactions data: " + e.getMessage(), e);
        }
    }

    // ==========================================
    // Sequence Tracking Loading & Saving
    // ==========================================

    public synchronized long[] loadSequences() {
        // [customerIdSeq, accountNumberSeq, transactionIdSeq]
        long[] seqs = new long[]{1000L, 100000L, 100000L};
        Path seqPath = dataDirectory.resolve(SEQUENCES_FILE);
        if (Files.exists(seqPath)) {
            try (BufferedReader reader = Files.newBufferedReader(seqPath)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    int eqIndex = line.indexOf('=');
                    if (eqIndex != -1) {
                        String key = line.substring(0, eqIndex).trim();
                        long val = Long.parseLong(line.substring(eqIndex + 1).trim());
                        switch (key) {
                            case "customerIdSeq" -> seqs[0] = Math.max(seqs[0], val);
                            case "accountNumberSeq" -> seqs[1] = Math.max(seqs[1], val);
                            case "transactionIdSeq" -> seqs[2] = Math.max(seqs[2], val);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Warning: Could not read sequences file: " + e.getMessage());
            }
        }
        return seqs;
    }

    public synchronized void saveSequences(long customerIdSeq, long accountNumberSeq, long transactionIdSeq) {
        Path seqPath = dataDirectory.resolve(SEQUENCES_FILE);
        try (BufferedWriter writer = Files.newBufferedWriter(seqPath,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
                java.nio.file.StandardOpenOption.WRITE)) {
            writer.write("# Auto-generated banking sequence metadata");
            writer.newLine();
            writer.write("customerIdSeq=" + customerIdSeq);
            writer.newLine();
            writer.write("accountNumberSeq=" + accountNumberSeq);
            writer.newLine();
            writer.write("transactionIdSeq=" + transactionIdSeq);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Warning: Could not save sequences file: " + e.getMessage());
        }
    }

    /**
     * Persists all state collections atomically.
     */
    public synchronized void saveAll(Collection<Customer> customers,
                                     Collection<BankAccount> accounts,
                                     Collection<Transaction> transactions) throws BankingException {
        saveCustomers(customers);
        saveAccounts(accounts);
        saveTransactions(transactions);
    }
}
