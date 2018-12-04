package io.carpe.hyperscan.wrapper;

import io.carpe.hyperscan.HyperscanUtils;
import io.carpe.hyperscan.jna.CompileErrorStruct;
import io.carpe.hyperscan.jna.HyperscanLibrary;
import io.carpe.hyperscan.wrapper.flags.HyperscanExpressionFlag;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

import java.util.EnumSet;
import java.util.Optional;


/**
 * HyperscanExpression to be compiled as a Database and then be used for scanning using the Scanner
 */
public class HyperscanExpression implements Expression {
    private EnumSet<HyperscanExpressionFlag> flags = EnumSet.noneOf(HyperscanExpressionFlag.class);
    private String expression;
    private Object context = null;

    /**
     * Constructor for a new expression without flags
     *
     * @param expression HyperscanExpression to use for matching
     */
    public HyperscanExpression(String expression) {
        checkArguments(expression);

        this.expression = expression;
    }


    /**
     * Constructor for a new expression without flags
     *
     * @param expression HyperscanExpression to use for matching
     * @param context    Context object associated with expression
     */
    public HyperscanExpression(String expression, Object context) {
        checkArguments(expression);

        this.expression = expression;
        this.context = context;
    }

    /**
     * Constructor for a new expression
     *
     * @param expression HyperscanExpression to use for matching
     * @param flags      Flags influencing the behaviour of the scanner
     */
    public HyperscanExpression(String expression, EnumSet<HyperscanExpressionFlag> flags) {
        checkArguments(expression);

        this.expression = expression;
        this.flags = flags;
    }

    /**
     * Constructor for a new expression
     *
     * @param expression HyperscanExpression to use for matching
     * @param flag       Single HyperscanExpressionFlag influencing the behaviour of the scanner
     */
    public HyperscanExpression(String expression, HyperscanExpressionFlag flag) {
        checkArguments(expression);

        this.expression = expression;
        this.flags = EnumSet.of(flag);
    }


    /**
     * Constructor for a new expression
     *
     * @param expression HyperscanExpression to use for matching
     * @param flags      Flags influencing the behaviour of the scanner
     * @param context    Context object associated with the expression
     */
    public HyperscanExpression(String expression, EnumSet<HyperscanExpressionFlag> flags, Object context) {
        checkArguments(expression);

        this.expression = expression;
        this.flags = flags;
        this.context = context;
    }

    /**
     * Constructor for a new expression
     *
     * @param expression HyperscanExpression to use for matching
     * @param flag       Single HyperscanExpressionFlag influencing the behaviour of the scanner
     * @param context    Context object associated with the expression
     */
    public HyperscanExpression(String expression, HyperscanExpressionFlag flag, Object context) {
        checkArguments(expression);

        this.expression = expression;
        this.flags = EnumSet.of(flag);
        this.context = context;
    }

    private static void checkArguments(String expression) {
        if (expression == null) {
            throw new NullPointerException("Null value for expression is not allowed");
        }
    }

    /**
     * Get the context object associated with the HyperscanExpression
     *
     * @return context
     */
    public Object getContext() {
        return context;
    }

    /**
     * Validates if the expression instance is valid for either chimera or hyperscan
     * <p>
     * Avoid calling multiple times if you can. It's a bit expensive.
     *
     * @return ValidationResult results of validation
     */
    public ValidationResult validate() {
        return new ValidationResult(
                hyperscanValidate().orElse(null),
                null
        );
    }

    private Optional<String> hyperscanValidate() {
        PointerByReference info = new PointerByReference();
        PointerByReference error = new PointerByReference();

        int hsResult = HyperscanLibrary.INSTANCE.hs_expression_info(this.expression, HyperscanUtils.bitEnumSetToInt(this.flags), info, error);

        String errorMessage = null;

        if (hsResult != 0) {
            CompileErrorStruct errorStruct = new CompileErrorStruct(error.getValue());
            errorMessage = errorStruct.message;
            errorStruct.setAutoRead(false);
            HyperscanLibrary.INSTANCE.hs_free_compile_error(errorStruct);
        } else {
            Native.free(Pointer.nativeValue(info.getValue()));
        }

        return Optional.ofNullable(errorMessage);
    }


    /**
     * Get the flags influencing the behaviour of the scanner
     *
     * @return All defined flags for this expression
     */
    public EnumSet<HyperscanExpressionFlag> getFlags() {
        return flags;
    }

    /**
     * Get the expression String used for matching
     *
     * @return expression as String
     */
    @Override
    public String getExpression() {
        return expression;
    }

    /**
     * Represents the validation results for a expression
     */
    public class ValidationResult {
        private String hyperscanError;
        private String chimeraError;

        ValidationResult(String hyperscanError, String chimeraError) {
            this.hyperscanError = hyperscanError;
            this.chimeraError = chimeraError;
        }


        /**
         * Get a boolean indicating if the expression is valid
         *
         * @return true if valid, otherwise false
         */
        public boolean isValidHyperscan() {
            return hyperscanError == null;
        }

        /**
         * Get a boolean indicating if the expression is valid
         *
         * @return true if valid, otherwise false
         */
        public boolean isValidChimera() {
            return chimeraError == null;
        }

        /**
         * Get a boolean indicating if the expression is valid
         *
         * @return true if valid, otherwise false
         */
        public boolean isValid() {
            return (isValidChimera() || isValidHyperscan());
        }

        /**
         * Get an string containing an error message in case of an invalid expression
         *
         * @return error message string if invalid, otherwise empty string.
         */
        public Optional<String> getHyperscanErrorMessage() {
            return Optional.ofNullable(this.hyperscanError);
        }

        /**
         * Get an string containing an error message in case of an invalid expression
         *
         * @return error message string if invalid, otherwise empty string.
         */
        public Optional<String> getChimeraErrorMessage() {
            return Optional.ofNullable(this.chimeraError);
        }
    }
}