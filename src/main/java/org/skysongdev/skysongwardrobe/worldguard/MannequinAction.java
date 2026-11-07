package org.skysongdev.skysongwardrobe.worldguard;

/** The three things a region can allow or deny for mannequins, one WorldGuard flag each. */
public enum MannequinAction {
    PLACE("mannequin-place"),
    USE("mannequin-use"),
    BREAK("mannequin-break");

    private final String flagName;

    MannequinAction(String flagName) {
        this.flagName = flagName;
    }

    public String flagName() {
        return flagName;
    }
}
