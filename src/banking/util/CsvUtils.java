package banking.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class providing RFC 4180 compliant CSV string escaping and line parsing.
 */
public final class CsvUtils {

    private CsvUtils() {
        // Prevent instantiation
    }

    /**
     * Escapes a single field value for CSV output.
     * If the field contains commas, double quotes, or newlines, it will be surrounded by
     * double quotes, with internal double quotes doubled.
     */
    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        if (needsQuotes) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * Joins an array or list of fields into a CSV line.
     */
    public static String toCsvLine(String... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(escape(fields[i]));
        }
        return sb.toString();
    }

    /**
     * Parses a line of CSV text into a list of tokens, properly handling quoted fields.
     */
    public static List<String> parseLine(String line) {
        List<String> tokens = new ArrayList<>();
        if (line == null) {
            return tokens;
        }

        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        int length = line.length();

        for (int i = 0; i < length; i++) {
            char c = line.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < length && line.charAt(i + 1) == '"') {
                        // Escaped quote
                        current.append('"');
                        i++; // skip next quote
                    } else {
                        // End of quoted section
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    tokens.add(current.toString().trim());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
        }
        tokens.add(current.toString().trim());

        return tokens;
    }
}
