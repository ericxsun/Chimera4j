package io.carpe.hyperscan;

import io.carpe.hyperscan.wrapper.HyperscanException;
import io.carpe.hyperscan.wrapper.flags.HyperscanBitFlag;

import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.EnumSet;

public class HyperscanUtils {
    public static int bitEnumSetToInt(EnumSet enumSet) {
        int bitValue = 0;
        for (final Object e : enumSet) {
            if (e instanceof HyperscanBitFlag) {
                Integer bits = ((HyperscanBitFlag) e).getBits();

                if (bits != null) {
                    bitValue = bits | bitValue;
                }
            } else {
                throw new InvalidParameterException();
            }
        }

        return bitValue;
    }

    public static int[] utf8ByteIndexesMapping(String s, int bytesLength) {
        final int[] byteIndexes = new int[bytesLength];
        int currentByte = 0;

        for (int stringPosition = 0; stringPosition < s.length(); stringPosition++) {
            final int c = s.codePointAt(stringPosition);

            final int unicodeCharLength;

            if (c <= 0x7F) unicodeCharLength = 1;
            else if (c <= 0x7FF) unicodeCharLength = 2;
            else if (c <= 0xFFFF) unicodeCharLength = 3;
            else if (c <= 0x1FFFFF) unicodeCharLength = 4;
            else
                throw new Error();

            Arrays.fill(byteIndexes, currentByte, currentByte + unicodeCharLength, stringPosition);

            currentByte += unicodeCharLength;

            if (Character.charCount(c) == 2) {
                stringPosition++;
            }
        }

        return byteIndexes;
    }

    public static HyperscanException hsErrorIntToException(int hsError) {
        switch (hsError) {
            case -1:
                return new HyperscanException("An invalid parameter has been passed. Is scratch allocated?");
            case -2:
                return new HyperscanException("Hyperscan was unable to allocate memory");
            case -3:
                return new HyperscanException("The engine was terminated by callback.");
            case -4:
                return new HyperscanException("The pattern compiler failed.");
            case -5:
                return new HyperscanException("The given database was built for a different version of Hyperscan.");
            case -6:
                return new HyperscanException("The given database was built for a different platform.");
            case -7:
                return new HyperscanException("The given database was built for a different mode of operation.");
            case -8:
                return new HyperscanException("A parameter passed to this function was not correctly aligned.");
            case -9:
                return new HyperscanException("The allocator did not return memory suitably aligned for the largest representable data type on this platform.");
            case -10:
                return new HyperscanException("The scratch region was already in use.");
            case -11:
                return new HyperscanException("Unsupported CPU architecture. At least SSE3 is needed");
            case -12:
                return new HyperscanException("Provided buffer was too small.");
            default:
                return new HyperscanException("Unexpected error: " + hsError);
        }
    }
}
