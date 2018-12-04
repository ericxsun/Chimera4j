package io.carpe.hyperscan.jna;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

import java.util.HashMap;
import java.util.Map;

public interface ChimeraLibrary extends Library {
    Map opts = new HashMap() {
        {
            put(OPTION_STRING_ENCODING, "UTF-8");
        }
    };

    ChimeraLibrary INSTANCE = (ChimeraLibrary) Native.loadLibrary("chimera", ChimeraLibrary.class, opts);

    String ch_version();

    int ch_database_size(Pointer database, SizeTByReference database_size);

    int hs_database_size(Pointer database, SizeTByReference database_size);

    int ch_compile(String expression, int flags, int mode, Pointer platform, PointerByReference database,
                   PointerByReference error);

    int ch_compile_multi(String[] expressions, int[] flags, int[] ids, int elements, int mode, Pointer platform,
                         PointerByReference database, PointerByReference error);

    int ch_compile_ext_multi(String[] expressions, int[] flags, int[] ids, PatternBehaviourStruct[] ext, int elements,
                             int mode, Pointer platform, PointerByReference database, PointerByReference error);

    int hs_compile(String expression, int flags, int mode, Pointer platform, PointerByReference database,
                   PointerByReference error);

    int hs_compile_multi(String[] expressions, int[] flags, int[] ids, int elements, int mode, Pointer platform,
                         PointerByReference database, PointerByReference error);

    int ch_alloc_scratch(Pointer database, PointerByReference scratch);

    int ch_scratch_size(Pointer scratch, SizeTByReference scratch_size);

    int ch_scan(Pointer database, String data, int length, int flags, Pointer scratch, HyperscanLibrary.match_event_handler callback, Pointer context);

    interface match_event_handler extends Callback {
        int invoke(int id, long from, long to, int flags, Pointer context);
    }
}