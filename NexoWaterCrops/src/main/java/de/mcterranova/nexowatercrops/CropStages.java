package de.mcterranova.nexowatercrops;

/**
 * The looks of a crop in order: first the growing stages, then the ripe looks, one for each chain. Free of Bukkit, so
 * it can be tested.
 */
record CropStages(int growing, int ripe) {

    boolean isRipe(int look) {
        return look >= growing;
    }

    /** The look {@code steps} stages on in this chain, at most ripe. */
    int grown(int look, int steps, int chain) {
        if (isRipe(look)) return look;
        int stage = look + steps;
        return stage < growing ? stage : growing + chain % ripe;
    }
}
