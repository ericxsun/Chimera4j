package io.carpe.hyperscan.wrapper;

import java.io.IOException;

public class HyperscanException extends IOException {
    public HyperscanException(String message) {
        super(message);
    }

    public HyperscanException(String message, Throwable cause) {
        super(message, cause);
    }
}
