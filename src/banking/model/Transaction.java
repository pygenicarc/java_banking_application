package banking.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Represents an immutable financial transaction entry for a bank account.
 */
public class Transaction {
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String transactionId;
    private final String accountNumber;
    private final TransactionType type;
    private final BigDecimal amount;
    private final BigDecimal balanceAfter;
    private final LocalDateTime timestamp;
    private final String description;

    /**
     * Constructs a new Transaction.
     *
     * @param transactionId unique transaction identifier
     * @param accountNumber associated bank account number
     * @param type          type of transaction
     * @param amount        transaction amount
     * @param balanceAfter  account balance immediately following the transaction
     * @param timestamp     date and time when transaction occurred
     * @param description   narrative description or reference
     */
    public Transaction(String transactionId,
                       String accountNumber,
                       TransactionType type,
                       BigDecimal amount,
                       BigDecimal balanceAfter,
                       LocalDateTime timestamp,
                       String description) {
        this.transactionId = Objects.requireNonNull(transactionId, "Transaction ID cannot be null");
        this.accountNumber = Objects.requireNonNull(accountNumber, "Account number cannot be null");
        this.type = Objects.requireNonNull(type, "Transaction type cannot be null");
        this.amount = Objects.requireNonNull(amount, "Amount cannot be null").setScale(2, RoundingMode.HALF_UP);
        this.balanceAfter = Objects.requireNonNull(balanceAfter, "Balance after cannot be null").setScale(2, RoundingMode.HALF_UP);
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        this.description = description == null ? "" : description.trim();
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getFormattedTimestamp() {
        return timestamp.format(DATE_TIME_FORMATTER);
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s | %-15s | Amount: $%,10.2f | Balance: $%,10.2f | %s",
                getFormattedTimestamp(),
                transactionId,
                type.getDisplayName(),
                amount,
                balanceAfter,
                description);
    }
}
