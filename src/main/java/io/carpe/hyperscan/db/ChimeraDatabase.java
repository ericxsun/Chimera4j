package io.carpe.hyperscan.db;

import io.carpe.hyperscan.HyperscanUtils;
import io.carpe.hyperscan.jna.ChimeraLibrary;
import io.carpe.hyperscan.jna.CompileErrorStruct;
import io.carpe.hyperscan.jna.HyperscanLibrary;
import io.carpe.hyperscan.jna.SizeTByReference;
import io.carpe.hyperscan.wrapper.ChimeraExpression;
import io.carpe.hyperscan.wrapper.HyperscanException;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Database containing compiled expressions ready for scanning using the Scanner.
 * <p>
 * Make sure to remember to close after you're done using.
 * But beware, rebuilding is expensive.
 */
public class ChimeraDatabase implements Closeable {
    private static final int HS_MODE_BLOCK = 1048576;
    private static final int HS_COMPILE_ERROR = -4;
    private Pointer database;
    private List<ChimeraExpression> expressions;

    private ChimeraDatabase(Pointer ch_database, List<ChimeraExpression> expressions) {
        this.database = ch_database;
        this.expressions = expressions;
    }

    private static void handleErrors(int chError, Pointer compileError, List<ChimeraExpression> expressions) throws HyperscanException {
        if (chError == 0)
            return;

        if (chError == HS_COMPILE_ERROR) {
            final CompileErrorStruct errorStruct = new CompileErrorStruct(compileError);
            try {
                if (errorStruct.expression > -1) {
                    throw new CompileErrorException(errorStruct.message, expressions.get(errorStruct.expression));
                } else {
                    throw new CompileErrorException(errorStruct.message, expressions.get(0));
                }
            } finally {
                errorStruct.setAutoRead(false);
                HyperscanLibrary.INSTANCE.hs_free_compile_error(errorStruct);
            }
        } else {
            throw new HyperscanException("Failed to handle error", HyperscanUtils.hsErrorIntToException(chError));
        }
    }

    /**
     * compile an expression into a database to use for scanning
     *
     * @param expression HyperscanExpression to compile
     * @return Compiled database
     * @throws HyperscanException CompileErrorException on errors concerning the pattern, otherwise different Throwable's
     */
    public static ChimeraDatabase compile(ChimeraExpression expression) throws HyperscanException {
        final PointerByReference database = new PointerByReference();
        final PointerByReference error = new PointerByReference();

        int hsError = ChimeraLibrary.INSTANCE.ch_compile(expression.getExpression(),
                HyperscanUtils.bitEnumSetToInt(expression.getFlags()), HS_MODE_BLOCK, Pointer.NULL, database, error);

        ArrayList<ChimeraExpression> expressions = new ArrayList<ChimeraExpression>(1);
        expressions.add(expression);

        handleErrors(hsError, error.getValue(), expressions);

        return new ChimeraDatabase(database.getValue(), expressions);
    }

    /**
     * compile an expression into a database to use for scanning
     *
     * @param expressions HyperscanExpression to compile
     * @return Compiled database
     * @throws HyperscanException CompileErrorException on errors concerning the pattern, otherwise different Throwable's
     */
    public static ChimeraDatabase compile(Collection<ChimeraExpression> expressions) throws HyperscanException {

        final ExpressionGroup expressionGroup = cifyExpressions(expressions);

        final PointerByReference database = new PointerByReference();
        final PointerByReference error = new PointerByReference();

        final int hsError = ChimeraLibrary.INSTANCE.ch_compile_multi(expressionGroup.expressions,
                expressionGroup.flags, expressionGroup.ids, expressionGroup.size, HS_MODE_BLOCK, Pointer.NULL, database, error);

        ArrayList<ChimeraExpression> compiledExpressions = new ArrayList<>(expressions);
        handleErrors(hsError, error.getValue(), compiledExpressions);

        return new ChimeraDatabase(database.getValue(), compiledExpressions);
    }

    private static ExpressionGroup cifyExpressions(Collection<ChimeraExpression> expressions) {
        final int expressionsSize = expressions.size();

        final String[] expressionsStr = new String[expressionsSize];
        final int[] flags = new int[expressionsSize];
        final int[] ids = new int[expressionsSize];
        final AtomicInteger indexCounter = new AtomicInteger();

        expressions.forEach((expression) -> {
            expressionsStr[indexCounter.get()] = expression.getExpression();
            flags[indexCounter.get()] = HyperscanUtils.bitEnumSetToInt(expression.getFlags());
            ids[indexCounter.get()] = indexCounter.get();
            indexCounter.getAndIncrement();
        });

        return new ExpressionGroup(expressionsStr, flags, ids);
    }

    public Pointer getPointer() {
        return database;
    }

    /**
     * Get the database size in bytes
     *
     * @return count of bytes
     */
    public long getSize() {
        if (database == null) {
            throw new IllegalStateException("Database has alredy been deallocated");
        }

        final SizeTByReference size = new SizeTByReference();
        ChimeraLibrary.INSTANCE.ch_database_size(database, size);
        return size.getValue().longValue();
    }

    @Override
    protected void finalize() {
        if (database != null) {
            HyperscanLibrary.INSTANCE.hs_free_database(database);
            database = null;
        }
    }

    public ChimeraExpression getExpression(int id) {
        return expressions.get(id);
    }

    @Override
    public void close() throws HyperscanException {
        this.finalize();
    }

    private static class ExpressionGroup {
        private final String[] expressions;
        private final int[] flags;
        private final int[] ids;
        private final int size;

        private ExpressionGroup(String[] expressions, int[] flags, int[] ids) {
            this.expressions = expressions;
            this.flags = flags;
            this.ids = ids;
            this.size = expressions.length;
        }
    }
}
