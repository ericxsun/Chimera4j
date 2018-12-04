package io.carpe.hyperscan.wrapper;

import java.util.Optional;

/**
 * Represents a match found during the scan
 */
public class Match {
    final private long startPosition;
    final private long endPosition;
    final private String matchedString;
    final private Expression matchedExpression;

    Match(long start, long end, String match, Expression expression) {
        startPosition = start;
        endPosition = end;
        matchedString = match;
        matchedExpression = expression;
    }

    Match(long start, long end, Expression expression) {
        startPosition = start;
        endPosition = end;
        matchedString = null;
        matchedExpression = expression;
    }

    /**
     * Get the exact matched string.
     * <p>
     * WARNING! matched text could be null! It's safer to use {@link #getMatched()}
     *
     * @return matched string if flag was set, otherwise null
     */
    public String getMatchedString() {
        return matchedString;
    }

    /**
     * Get the exact matched string wrapped in an optional for your safety.
     *
     * @return matched string in optional if flag was set, otherwise empty optional
     */
    public Optional<String> getMatched() {
        return Optional.ofNullable(matchedString);
    }

    /**
     * Get the start position of the match
     *
     * @return if the SOM flag is set the position of the match, otherwise zero.
     */
    public long getStartPosition() {
        return startPosition;
    }

    /**
     * Get the end position of the match
     *
     * @return end position of match regardless of flags
     */
    public long getEndPosition() {
        return endPosition;
    }

    /**
     * Get the HyperscanExpression object used to find the match
     *
     * @return HyperscanExpression instance
     */
    public Expression getMatchedExpression() {
        return matchedExpression;
    }
}
