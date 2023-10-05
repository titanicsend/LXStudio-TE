package heronarts.lx.studio;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.CoreFoundation;
import com.sun.jna.platform.mac.CoreFoundation.*;
import com.sun.jna.platform.win32.User32;

public class WindowTitleCheck {
    public interface CoreGraphics extends Library {
        CoreGraphics INSTANCE = Native.load("CoreGraphics", CoreGraphics.class);

        CoreFoundation.CFArrayRef CGWindowListCopyWindowInfo(int option, int relativeToWindow);
    }

    public static boolean instanceRunningWin32(String appTitle) {
        char[] buffer = new char[1024];
        boolean result = true;
        result = User32.INSTANCE.EnumWindows((hwnd, pointer) -> {
            User32.INSTANCE.GetWindowText(hwnd, buffer, buffer.length);
            String title = Native.toString(buffer);
            if (!title.isEmpty()) {
                if  (title.startsWith(appTitle)) {
                    // found it. stop enumeration
                    return false;
                }
            }
            // try next window
            return true;
        }, null);
        return !result;
    }

    public static boolean instanceRunningMac(String appTitle) {
        boolean result = false;
        CoreFoundation.CFArrayRef windowInfo = CoreGraphics.INSTANCE.CGWindowListCopyWindowInfo(0, 0);
        CFStringRef kCGWindowName = CFStringRef.createCFString("kCGWindowName");

// Iterate through the array of window info dictionaries
        int numWindows = windowInfo.getCount();
        for (int i = 0; i < numWindows; i++) {

            // get the Window title (which can be null)
            Pointer ptr = windowInfo.getValueAtIndex(i);
            CFDictionaryRef windowRef = new CFDictionaryRef(ptr);
            ptr = windowRef.getValue(kCGWindowName);
            String windowName = ptr == null ? "" : new CFStringRef(ptr).stringValue();

            if (windowName.startsWith(appTitle)) {
                result = true;
                break;
            }
        }

        // release native resources
        kCGWindowName.release();
        windowInfo.release();
        return result;
    }

    public static String getOS() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return "win";
        } else if (os.contains("mac")) {
            return "mac";
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            return "linux";
        } else {
            return "unknown";
        }
    }

    public static boolean isRunning(String appTitle) {
        if (getOS().equals("win")) {
            return instanceRunningWin32(appTitle);
        }
        else if (getOS().equals("mac")) {
            return instanceRunningMac(appTitle);
        }
        else {
            // TODO - add Linux support
            return false;
        }
    }
}



