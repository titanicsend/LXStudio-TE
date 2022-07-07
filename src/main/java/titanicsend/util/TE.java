package titanicsend.util;

import heronarts.lx.LX;

/**
 * For utilty funcs that we want really fast
 * syntatic sugar access to!
 */
public class TE {
    public static void log(String format, Object... arguments) {
        String msg = String.format(format, arguments);
        LX.log(msg);
    }

    public static void err(String format, Object... arguments) {
        String msg = String.format(format, arguments);
        LX.error(msg);
    }
}
