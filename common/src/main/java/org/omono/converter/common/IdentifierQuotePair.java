package org.omono.converter.common;

/**
 * Represents a pair of quote characters for identifiers.
 * E.g., ('"', '"') for "name", ('[', ']') for [name]
 * 
 * Shared between sql-comparator and mybatistool modules.
 */
public class IdentifierQuotePair {
    
    private final char start;
    private final char end;
    
    /**
     * Create a quote pair with different start and end characters.
     * @param start the start quote character
     * @param end the end quote character
     */
    public IdentifierQuotePair(char start, char end) {
        this.start = start;
        this.end = end;
    }
    
    /**
     * Create a quote pair with the same character for both start and end.
     * E.g., ('"', '"') for "name"
     * @param quote the quote character
     * @return a new IdentifierQuotePair
     */
    public static IdentifierQuotePair of(char quote) {
        return new IdentifierQuotePair(quote, quote);
    }
    
    /**
     * Create a quote pair with different start and end characters.
     * E.g., ('[', ']') for [name]
     * @param start the start quote character
     * @param end the end quote character
     * @return a new IdentifierQuotePair
     */
    public static IdentifierQuotePair of(char start, char end) {
        return new IdentifierQuotePair(start, end);
    }
    
    /**
     * Get the start quote character.
     * @return the start character
     */
    public char getStart() {
        return start;
    }
    
    /**
     * Get the end quote character.
     * @return the end character
     */
    public char getEnd() {
        return end;
    }
    
    /**
     * Check if this pair matches the given string boundaries.
     * @param identifier the identifier string to check
     * @return true if the string starts with start char and ends with end char
     */
    public boolean matches(String identifier) {
        if (identifier == null || identifier.length() < 2) {
            return false;
        }
        return identifier.charAt(0) == start 
            && identifier.charAt(identifier.length() - 1) == end;
    }
    
    /**
     * Strip quotes from identifier if matched.
     * @param identifier the identifier string
     * @return the identifier without quotes if matched, otherwise the original string
     */
    public String strip(String identifier) {
        if (matches(identifier)) {
            return identifier.substring(1, identifier.length() - 1);
        }
        return identifier;
    }
    
    @Override
    public String toString() {
        return start == end ? "'" + start + "'" : "'" + start + "'..'" + end + "'";
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdentifierQuotePair that = (IdentifierQuotePair) o;
        return start == that.start && end == that.end;
    }
    
    @Override
    public int hashCode() {
        return 31 * start + end;
    }
}
