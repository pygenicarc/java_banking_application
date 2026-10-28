package banking.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Represents a bank customer profile.
 */
public class Customer {
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String customerId;
    private String name;
    private int age;
    private String phoneNumber;
    private String email;
    private String address;
    private final String accountNumber;
    private final LocalDateTime createdAt;

    /**
     * Constructor for a newly registered customer.
     */
    public Customer(String customerId,
                    String name,
                    int age,
                    String phoneNumber,
                    String email,
                    String address,
                    String accountNumber) {
        this(customerId, name, age, phoneNumber, email, address, accountNumber, LocalDateTime.now());
    }

    /**
     * Full constructor for reconstructing a customer from persistent storage.
     */
    public Customer(String customerId,
                    String name,
                    int age,
                    String phoneNumber,
                    String email,
                    String address,
                    String accountNumber,
                    LocalDateTime createdAt) {
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null").trim();
        this.name = Objects.requireNonNull(name, "Name cannot be null").trim();
        this.age = age;
        this.phoneNumber = Objects.requireNonNull(phoneNumber, "Phone number cannot be null").trim();
        this.email = Objects.requireNonNull(email, "Email cannot be null").trim();
        this.address = Objects.requireNonNull(address, "Address cannot be null").trim();
        this.accountNumber = Objects.requireNonNull(accountNumber, "Account number cannot be null").trim();
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "Name cannot be null").trim();
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = Objects.requireNonNull(phoneNumber, "Phone number cannot be null").trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = Objects.requireNonNull(email, "Email cannot be null").trim();
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = Objects.requireNonNull(address, "Address cannot be null").trim();
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getFormattedCreatedAt() {
        return createdAt.format(DATE_TIME_FORMATTER);
    }

    @Override
    public String toString() {
        return String.format("Customer [ID: %s, Name: %s, Age: %d, Phone: %s, Email: %s, Account: %s, Address: %s]",
                customerId, name, age, phoneNumber, email, accountNumber, address);
    }
}
