package io.carpe.hyperscan.wrapper;

import io.carpe.hyperscan.db.ChimeraDatabase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;


class DeallocationTest {
    @Test
    void databaseShouldThrowExceptionOnCallingSizeAfterClose() throws HyperscanException {
        try (final ChimeraDatabase db = ChimeraDatabase.compile(new ChimeraExpression("test"))) {
            // close the db
            db.close();

            // now try and get the size.
            // this should throw the exception because the db is already closed.
            assertThatExceptionOfType(IllegalStateException.class).isThrownBy(db::getSize);
        }
    }

    @Test
    void scrannerShouldThrowExceptionOnCallingSizeAfterClose() throws HyperscanException {
        try (final ChimeraDatabase db = ChimeraDatabase.compile(new ChimeraExpression("test"))) {
            final Scanner scanner = new Scanner();
            scanner.allocScratch(db);
            scanner.close();

            assertThatExceptionOfType(IllegalStateException.class).isThrownBy(scanner::getHyperscanSize);
        }
    }
}
