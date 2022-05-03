package titanicsend.model;

public enum TEPanelSection {
    // For cart orientation, see
    // https://github.com/titanicsend/LXStudio-TE/blob/main/assets/vehicle-axes-orientation.png

    // All panels on the front face of the cart (stage left)
    FORE,
    // All panels on the back face of the cart (stage right)
    AFT,
    // All panels on the port face of the cart
    // These are the solid panels opposite the stage
    PORT,
    // The section of consecutive panels on the front half of the starboard (stage) side of the cart
    // Does not include the single panel
    STARBOARD_FORE,
    // The one standalone panel on the front (stage left) half of the starboard (stage) side of the cart
    STARBOARD_FORE_SINGLE,
    // The section of consecutive panels on the back (stage right) half of the starboard (stage) side of the cart
    // Does not include the single panel
    STARBOARD_AFT,
    // The one standalone panel on the front (stage right) half of the starboard (stage) side of the cart
    STARBOARD_AFT_SINGLE
}
