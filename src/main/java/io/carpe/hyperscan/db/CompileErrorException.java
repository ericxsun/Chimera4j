package io.carpe.hyperscan.db;

import io.carpe.hyperscan.wrapper.Expression;
import io.carpe.hyperscan.wrapper.HyperscanException;

/**
 * Represents a compiler error due to an invalid expression
 */
public class CompileErrorException extends HyperscanException {

    private Expression failedExpression;

    public CompileErrorException(String s, Expression failedExpression) {
        super(s);

        this.failedExpression = failedExpression;
    }

    /**
     * Get the failed expression object
     *
     * @return HyperscanExpression object
     */
    public Expression getFailedExpression() {
        return failedExpression;
    }
}
