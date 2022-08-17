package titanicsend.lasercontrol;

import heronarts.lx.LX;
import heronarts.lx.color.LXDynamicColor;
import titanicsend.app.autopilot.TEOscMessage;
import titanicsend.util.TE;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * Information to be able to send OSC messages to
 * Pangolin host.
 */
public class PangolinLaser {
    public static final int DEFAULT_PORT = 42070;
    public String hostname;
    public int port = DEFAULT_PORT;

    public static int PRIMARY_COLOR_INDEX = 2;

    private LX lx;

    public PangolinLaser(LX lx, String pathname) {
        this.lx = lx;

        // read config file
        try {
            Scanner scanner = new Scanner(new File(pathname));
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.startsWith("lasers-3")) {
                    String[] parts = line.split(",");
                    hostname = parts[1].strip().replace(";", "");
                }
            }
            scanner.close();
            TE.log("Loaded laser at %s:%d !", hostname, port);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // prepare to send OSC
        lx.engine.osc.transmitActive.setValue(true);
        lx.engine.osc.transmitHost.setValue(hostname);
        lx.engine.osc.transmitPort.setValue(port);
    }

    /**
     * Call this function to send the current LX palette to Pangolin
     * lasers via OSC.
     *
     * Sends the primary color.
     */
    public void sendPaletteToLasers() {
        LXDynamicColor color = lx.engine.palette.swatch.getColor(PRIMARY_COLOR_INDEX);
        double hue = color.color.hue.getValue();
        String address = TEOscMessage.makePaletteHueAddress();
        lx.engine.osc.sendMessage(address, (float)hue);
        TE.log("Sent hue=%f to lasers ...", hue);
    }
}
