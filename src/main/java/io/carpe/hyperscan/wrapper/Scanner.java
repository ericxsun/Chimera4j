package io.carpe.hyperscan.wrapper;

import io.carpe.hyperscan.HyperscanUtils;
import io.carpe.hyperscan.db.ChimeraDatabase;
import io.carpe.hyperscan.jna.ChimeraLibrary;
import io.carpe.hyperscan.jna.ChimeraLibraryDirect;
import io.carpe.hyperscan.jna.HyperscanLibrary;
import io.carpe.hyperscan.jna.SizeTByReference;
import io.carpe.hyperscan.wrapper.flags.ChimeraExpressionFlag;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

import java.io.Closeable;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Scanner, can be used with databases to scan for expressions in input string
 * In case of multithreaded scanning, you need one scanner instance per thread.
 */
public class Scanner implements Closeable {
    private final LinkedList<long[]> matchedIds = new LinkedList<>();
    private final List<Match> noMatches = Collections.emptyList();
    private final ChimeraLibrary.match_event_handler chimeraMatchHandler = new ChimeraLibrary.match_event_handler() {
        public int invoke(int id, long from, long to, int flags, Pointer context) {
            long[] tuple = {id, from, to};
            matchedIds.add(tuple);
            return 0;
        }
    };
    private PointerByReference scratchReference = new PointerByReference();
    private Pointer scratch;

    /**
     * Check if the hardware platform is supported
     *
     * @return true if supported, otherwise false
     */
    public static boolean getIsValidPlatform() {
        return HyperscanLibrary.INSTANCE.hs_valid_platform() == 0;
    }

    /**
     * Get the version information for the underlying hyperscan library
     *
     * @return version string
     */
    public static String getVersion() {
        return HyperscanLibrary.INSTANCE.hs_version();
    }

    /**
     * Get the scratch space size used by hyperscan in bytes
     *
     * @return count of bytes
     */
    public long getHyperscanSize() {
        if (scratch == null) {
            throw new IllegalStateException("Scratch space has alredy been deallocated");
        }

        final SizeTByReference size = new SizeTByReference();
        HyperscanLibrary.INSTANCE.hs_scratch_size(scratch, size);
        return size.getValue().longValue();
    }

    /**
     * Get the scratch space size used by chimera in bytes
     *
     * @return count of bytes
     */
    public long getChimeraSize() {
        if (scratch == null) {
            throw new IllegalStateException("Scratch space has alredy been deallocated");
        }

        final SizeTByReference size = new SizeTByReference();
        ChimeraLibrary.INSTANCE.ch_scratch_size(scratch, size);
        return size.getValue().longValue();
    }

    /**
     * Allocate a scratch space.  Must be called at least once with each
     * database that will be used before scan is called.
     *
     * @param db Database containing expressions to use for matching
     * @throws HyperscanException Throws if out of memory or platform not supported
     *                            or if the allocation fails
     */
    public void allocScratch(final ChimeraDatabase db) throws HyperscanException {
        final Pointer dbPointer = db.getPointer();

        if (scratchReference == null) {
            scratchReference = new PointerByReference();
        }

        final int hsError = ChimeraLibrary.INSTANCE.ch_alloc_scratch(dbPointer, scratchReference);

        if (hsError != 0)
            throw new HyperscanException("Failed to allocScratch.", HyperscanUtils.hsErrorIntToException(hsError));

        scratch = scratchReference.getValue();
    }

    /**
     * scan for a match in a string using a compiled expression database
     * Can only be executed one at a time on a per instance basis
     *
     * @param db    Database containing expressions to use for matching
     * @param input String to match against
     * @return List of Matches
     * @throws HyperscanException Throws if out of memory, platform not supported or database is null
     */
    public List<Match> scan(final ChimeraDatabase db, final String input) throws HyperscanException {
        final Pointer dbPointer = db.getPointer();

        final byte[] utf8bytes = input.getBytes(StandardCharsets.UTF_8);
        final int bytesLength = utf8bytes.length;

        matchedIds.clear();
        int hsError = ChimeraLibraryDirect.ch_scan(dbPointer, input, bytesLength,
                0, scratch, chimeraMatchHandler, Pointer.NULL);

        if (hsError != 0)
            throw new HyperscanException("Failed to scan.", HyperscanUtils.hsErrorIntToException(hsError));

        if (matchedIds.isEmpty())
            return noMatches;

        final int[] byteToIndex = HyperscanUtils.utf8ByteIndexesMapping(input, bytesLength);
        final LinkedList<Match> matches = new LinkedList<>();

        matchedIds.forEach(tuple -> {
            final int id = (int) tuple[0];
            final long from = tuple[1];
            final long to = tuple[2] < 1 ? 1 : tuple[2]; //prevent index out of bound exception later

            final int startIndex = byteToIndex[(int) from];
            final int endIndex = byteToIndex[(int) to - 1] + 1;   //Adding the +1 here to make more sense in the string.substring().
            //This is just for convenience and it can be adjusted later or configurable
            //2018-12-04 ~Carpe
            final ChimeraExpression matchingExpression = db.getExpression(id);

            if (matchingExpression.getFlags().contains(ChimeraExpressionFlag.EXTRACT_MATCHED)) {
                // extract matched text from input and store in match
                matches.add(new Match(startIndex, endIndex, input.substring(startIndex, endIndex), matchingExpression));
            } else {
                matches.add(new Match(startIndex, endIndex, matchingExpression));
            }
        });

        return matches;
    }

    @Override
    protected void finalize() {
        //check and setting scratch pointer to null to avoid double free
        if (scratch != null) {
            HyperscanLibrary.INSTANCE.hs_free_scratch(scratch);
            scratch = null;
            scratchReference = null;
        }
    }

    @Override
    public void close() throws HyperscanException {
        this.finalize();
    }
}
