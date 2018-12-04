package io.carpe.hyperscan.jna;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;

import java.util.HashMap;

public class ChimeraLibraryDirect {

    static {
        HashMap<String, Object> opts = new HashMap<>();
        opts.put(Library.OPTION_STRING_ENCODING, "UTF-8");

        Native.register(NativeLibrary.getInstance("chimera", opts));
    }

    public static native int ch_scan(Pointer ch_database, String data, int length, int flags, Pointer scratch, ChimeraLibrary.match_event_handler callback, Pointer context);

    public static native int hs_scan(Pointer database, String data, int length, int flags, Pointer scratch, ChimeraLibrary.match_event_handler callback, Pointer context);
}