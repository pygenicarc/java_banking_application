package banking.model;

import banking.exception.InsufficientFundsException;
import banking.exception.ValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Represents a bank account associated with a customer.
 * Encapsulates the account balance and core balance modification operations.
 */
public class BankAccount {
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String accountNumber;
    private final String customerId;
    private BigDecimal balance;
    private final LocalDateTime createdAt;

    /**
     * Constructs a new BankAccount with an initial balance.
     */
    public BankAccount(String accountNumber, String customerId, BigDecimal initialBalance) {
        this(accountNumber,
             customerId,
             initialBalance != null ? initialBalance : BigDecimal.ZERO,
             LocalDateTime.now());
    }

    /**
     * Full constructor, used for loading accounts from storage.
     */
    public BankAccount(String accountNumber, String customerId, BigDecimal balance, LocalDateTime createdAt) {
        this.accountNumber = Objects.requireNonNull(accountNumber, "Account number cannot be null").trim();
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null").trim();
        this.balance = (balance == null ? BigDecimal.ZERO : balance).setScale(2, RoundingMode.HALF_UP);
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getCustomerId() {
        return customerId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getFormattedCreatedAt() {
        return createdAt.format(DATE_TIME_FORMATTER);
    }

    /**
     * Credits the account balance with the specified positive amount.
     *
     * @param amount the deposit amount (must be positive)
     * @return the new updated balance
     * @throws ValidationException if amount is null or not greater than zero
     */
    public synchronized BigDecimal deposit(BigDecimal amount) throws ValidationException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("amount", "Deposit amount must be strictly greater than zero.");
        }
        this.balance = this.balance.add(amount.setScale(2, RoundingMode.HALF_UP));
        return this.balance;
    }

    /**
     * Debits the account balance with the specified positive amount.
     *
     * @param amount the withdrawal amount (must be positive)
     * @return the new updated balance
     * @throws ValidationException if amount is null or not greater than zero
     * @throws InsufficientFundsException if balance is insufficient
     */
    public synchronized BigDecimal withdraw(BigDecimal amount) throws ValidationException, InsufficientFundsException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("amount", "Withdrawal amount must be strictly greater than zero.");
        }
        BigDecimal scaledAmount = amount.setScale(2, RoundingMode.HALF_UP);
        if (this.balance.compareTo(scaledAmount) < 0) {
            throw new InsufficientFundsException(this.balance, scaledAmount);
        }
        this.balance = this.balance.subtract(scaledAmount);
        return this.balance;
    }

    /**
     * Checks if the account balance is currently zero.
     */
    public boolean hasZeroBalance() {
        return this.balance.compareTo(BigDecimal.ZERO) == 0;
    }

    @Override
    public String toString() {
        return String.format("Account #%s [CustomerID: %s, Balance: $%,.2f, Created: %s]",
                accountNumber, customerId, balance, getFormattedCreatedAt());
    }
}
