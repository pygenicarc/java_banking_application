package banking.util;

import banking.exception.ValidationException;

import java.math.BigDecimal;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Reusable utility class for validating user input and reading sanitized data from the console.
 */
public final class InputValidator {

    // Validation patterns
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z]+([ '-][A-Za-z]+)*$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9\\s().\\-]{10,25}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}$");
    private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile("^ACC\\d{6}$");
    private static final Pattern CUSTOMER_ID_PATTERN = Pattern.compile("^CUST\\d{4,6}$");

    public static final int MIN_AGE = 18;
    public static final int MAX_AGE = 120;
    public static final BigDecimal MAX_TRANSACTION_AMOUNT = new BigDecimal("100000000.00"); // 100 Million limit

    private InputValidator() {
        // Prevent instantiation
    }

    // ==========================================
    // Field Validation Methods
    // ==========================================

    /**
     * Validates customer name.
     */
    public static void validateName(String name) throws ValidationException {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("name", "Name cannot be empty.");
        }
        String trimmed = name.trim();
        if (trimmed.length() < 2 || trimmed.length() > 60) {
            throw new ValidationException("name", "Name must be between 2 and 60 characters long.");
        }
        if (!NAME_PATTERN.matcher(trimmed).matches()) {
            throw new ValidationException("name", "Name can only contain alphabetic letters, spaces, hyphens, and apostrophes.");
        }
    }

    /**
     * Validates customer age.
     */
    public static void validateAge(int age) throws ValidationException {
        if (age < MIN_AGE || age > MAX_AGE) {
            throw new ValidationException("age", String.format("Age must be between %d and %d years.", MIN_AGE, MAX_AGE));
        }
    }

    /**
     * Validates phone number.
     */
    public static void validatePhoneNumber(String phone) throws ValidationException {
        if (phone == null || phone.trim().isEmpty()) {
            throw new ValidationException("phone", "Phone number cannot be empty.");
        }
        String trimmed = phone.trim();
        String digitsOnly = trimmed.replaceAll("[^0-9]", "");
        if (digitsOnly.length() < 10 || digitsOnly.length() > 15 || !PHONE_PATTERN.matcher(trimmed).matches()) {
            throw new ValidationException("phone", "Invalid phone number. Please enter a valid 10-15 digit phone number (e.g., 9876543210 or +1-555-0199).");
        }
    }

    /**
     * Validates email address.
     */
    public static void validateEmail(String email) throws ValidationException {
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException("email", "Email address cannot be empty.");
        }
        String trimmed = email.trim();
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new ValidationException("email", "Invalid email address format (e.g., customer@example.com).");
        }
    }

    /**
     * Validates address.
     */
    public static void validateAddress(String address) throws ValidationException {
        if (address == null || address.trim().isEmpty()) {
            throw new ValidationException("address", "Address cannot be empty.");
        }
        String trimmed = address.trim();
        if (trimmed.length() < 3 || trimmed.length() > 150) {
            throw new ValidationException("address", "Address must be between 3 and 150 characters.");
        }
    }

    /**
     * Validates account number format.
     */
    public static void validateAccountNumberFormat(String accountNumber) throws ValidationException {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new ValidationException("accountNumber", "Account number cannot be empty.");
        }
        String trimmed = accountNumber.trim().toUpperCase();
        if (!ACCOUNT_NUMBER_PATTERN.matcher(trimmed).matches()) {
            throw new ValidationException("accountNumber", "Account number must be in format 'ACC######' (e.g., ACC100001).");
        }
    }

    /**
     * Validates transaction amount (deposit/withdrawal/transfer).
     */
    public static void validateTransactionAmount(BigDecimal amount) throws ValidationException {
        if (amount == null) {
            throw new ValidationException("amount", "Amount cannot be null.");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("amount", "Transaction amount must be strictly greater than zero ($0.00).");
        }
        if (amount.compareTo(MAX_TRANSACTION_AMOUNT) > 0) {
            throw new ValidationException("amount", String.format("Transaction amount exceeds maximum allowable limit of $%,.2f.", MAX_TRANSACTION_AMOUNT));
        }
    }

    /**
     * Validates opening deposit amount (allows zero for zero opening balance).
     */
    public static void validateOpeningDeposit(BigDecimal amount) throws ValidationException {
        if (amount == null) {
            throw new ValidationException("openingDeposit", "Opening deposit cannot be null.");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("openingDeposit", "Opening deposit cannot be negative.");
        }
        if (amount.compareTo(MAX_TRANSACTION_AMOUNT) > 0) {
            throw new ValidationException("openingDeposit", String.format("Opening deposit exceeds maximum allowable limit of $%,.2f.", MAX_TRANSACTION_AMOUNT));
        }
    }

    // ==========================================
    // Console Input Helper Methods
    // ==========================================

    /**
     * Reads a non-empty string line from console.
     */
    public static String readString(Scanner scanner, String prompt, boolean allowEmpty) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine();
            if (line == null) {
                return "";
            }
            line = line.trim();
            if (!allowEmpty && line.isEmpty()) {
                System.out.println(">> Error: Input cannot be blank. Please try again.");
                continue;
            }
            return line;
        }
    }

    /**
     * Reads an integer within a specified range from console.
     */
    public static int readInt(Scanner scanner, String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine();
            if (line == null) {
                return min;
            }
            line = line.trim();
            try {
                int value = Integer.parseInt(line);
                if (value < min || value > max) {
                    System.out.printf(">> Error: Please enter a number between %d and %d.%n", min, max);
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println(">> Error: Invalid numeric input. Please enter a whole number.");
            }
        }
    }

    /**
     * Reads a monetary BigDecimal amount from console.
     */
    public static BigDecimal readBigDecimal(Scanner scanner, String prompt, boolean allowZero) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine();
            if (line == null) {
                return BigDecimal.ZERO;
            }
            line = line.trim().replace("$", "").replace(",", "");
            if (line.isEmpty()) {
                if (allowZero) {
                    return BigDecimal.ZERO;
                } else {
                    System.out.println(">> Error: Amount cannot be blank. Please enter a valid monetary amount.");
                    continue;
                }
            }
            try {
                BigDecimal value = new BigDecimal(line);
                if (value.scale() > 2) {
                    System.out.println(">> Error: Amount cannot have more than 2 decimal places (cents).");
                    continue;
                }
                if (allowZero && value.compareTo(BigDecimal.ZERO) == 0) {
                    return BigDecimal.ZERO;
                }
                if (value.compareTo(BigDecimal.ZERO) <= 0) {
                    System.out.println(">> Error: Amount must be strictly greater than zero.");
                    continue;
                }
                if (value.compareTo(MAX_TRANSACTION_AMOUNT) > 0) {
                    System.out.printf(">> Error: Amount exceeds maximum limit of $%,.2f.%n", MAX_TRANSACTION_AMOUNT);
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println(">> Error: Invalid monetary amount. Example format: 150.00 or 50");
            }
        }
    }

    /**
     * Prompts user for a Yes/No confirmation.
     */
    public static boolean readConfirmation(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt + " (y/n): ");
            String line = scanner.nextLine();
            if (line == null) {
                return false;
            }
            line = line.trim().toLowerCase();
            if (line.equals("y") || line.equals("yes")) {
                return true;
            }
            if (line.equals("n") || line.equals("no")) {
                return false;
            }
            System.out.println(">> Error: Please enter 'y' for Yes or 'n' for No.");
        }
    }
}
