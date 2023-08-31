package titanicsend.model;

public enum TEPanelSection {
    // For cart orientation, see
    // https://github.com/titanicsend/LXStudio-TE/blob/main/assets/vehicle-axes-orientation.png

    // All panels on the front face of the cart (stage left)
    FORE("Fore", "fore"),
    // All panels on the back face of the cart (stage right)
    AFT("Aft", "aft"),
     // The section of consecutive panels on the front half of the starboard (stage) side of the cart
    // Does not include the single panel
    STARBOARD_FORE("Starboard Fore", "starboard-fore"),
    // The one standalone panel on the front (stage left) half of the starboard (stage) side of the cart
    STARBOARD_FORE_SINGLE("Starboard Fore Single", "starboard-fore-single"),
    // The section of consecutive panels on the back (stage right) half of the starboard (stage) side of the cart
    // Does not include the single panel
    STARBOARD_AFT("Starboard Aft", "starboard-aft"),
    // The one standalone panel on the front (stage right) half of the starboard (stage) side of the cart
    STARBOARD_AFT_SINGLE("Starboard Aft Single", "starboard-aft-single"),

    // add matching definitions for port sections, now that the port side is completely lit
    PORT_FORE("Port Fore", "port-fore"),
    PORT_FORE_SINGLE("Port Fore Single", "port-fore-single"),
    PORT_AFT("Port Aft", "port-aft"),
    PORT_AFT_SINGLE("Port Aft Single", "port-aft-single");


    public final String label;    // For enum.toString() calls such as by a parameter
    public final String tag;      // For views.  Lower case is most convenient.

    private TEPanelSection(String label, String tag) {
        this.label = label;
        this.tag = tag;
    }

    @Override
    public String toString() {
        return this.tag;
    }

    /**
     * Get the model tag associated with this section, for use by views.
     */
    public String getTag() {
        return this.tag;
    }
}
