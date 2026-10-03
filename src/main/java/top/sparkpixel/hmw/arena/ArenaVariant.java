package top.sparkpixel.hmw.arena;

/**
 * The 12 programmatically generated arena variants of the original map
 * (chop:lobby/options/arena, ids 0..11).
 */
public enum ArenaVariant {
    CLASSIC("Classic"),
    CLIFF("Cliff"),
    SLOPE("Slope"),
    HILL("Hill"),
    VALLEY("Valley"),
    BRIDGE("Bridge"),
    ISLAND("Island"),
    WALL("Wall"),
    HARDCORE("Hardcore"),
    HONEY("Honey"),
    THICK("Thick"),
    TOWERS("Towers");

    private final String displayName;

    ArenaVariant(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return this.displayName;
    }

    /** THICK uses narrower wall-placement limits (missile:place_control). */
    public boolean isThick() {
        return this == THICK;
    }
}
