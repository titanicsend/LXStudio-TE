package titanicsend.lasercontrol;

import heronarts.lx.LX;
import heronarts.lx.color.LXDynamicColor;
import heronarts.lx.osc.LXOscEngine;
import heronarts.lx.osc.OscFloat;
import heronarts.lx.osc.OscMessage;
import titanicsend.app.autopilot.TEOscMessage;
import titanicsend.util.TE;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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

    public static final String HOSTNAME_LABEL = "lasers-3";

    /**
     * Will read from a text file in format provided by Ali:
     *
     *      hostname1, ip_address1;
     *      hostname2, ip_address2;
     *      hostname3, ip_address3;
     *      ...
     *
     * Then, given this info, will look for one hostname starting with
     * HOSTNAME_LABEL, and then use port=DEFAULT_PORT
     *
     * Then creates a listener on palette changes to send this to Pangolin host.
     *
     * @param lx
     * @param pathname
     */
    public PangolinLaser(LX lx, String pathname) {
        this.lx = lx;

        // read config file
        try {
            Scanner scanner = new Scanner(new File(pathname));
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.startsWith(HOSTNAME_LABEL)) {
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
        try {
            final LXOscEngine.Transmitter transmitter = lx.engine.osc.transmitter(hostname, port);

            // Set up a message and parameter
            final OscMessage message = new OscMessage(TEOscMessage.makePaletteHueAddress());
            final OscFloat hueValue = new OscFloat(0f);

            // Monitor your parameter for changes, and issue a callback to send out if changed
            lx.engine.palette.swatch.getColor(PRIMARY_COLOR_INDEX).color.hue.addListener(p -> {
                hueValue.setValue(p.getValuef());
                message.clearArguments();
                message.add(hueValue);

                try {
                    transmitter.send(message);
                    TE.log("Sent hue=%f to lasers ...", hueValue);
                } catch (IOException ioe) {
                    TE.err("Could not send hue=%f to lasers: %s", hueValue, ioe);
                    ioe.printStackTrace();
                }
            });

        } catch (Exception e) {
            TE.err("Could not set up OSC transmission for Pangolin laser: %s", e);
            e.printStackTrace();
        }
    }
}
