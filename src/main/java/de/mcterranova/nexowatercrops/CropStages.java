package de.mcterranova.nexowatercrops;

/**
 * The looks of a crop in order: first the growing stages, then the ripe looks, one for each chain. Free of Bukkit, so
 * it can be tested.
 */
record CropStages(int growing, int ripe) {

    boolean isRipe(int look) {
        return look >= growing;
    }

    /** The look of a stage in this chain; every stage from {@code growing} on is the chain's ripe look. */
    int look(int stage, int chain) {
        return stage < growing ? stage : growing + chain % ripe;
    }

    /** The look {@code steps} stages on in this chain, at most ripe. */
    int grown(int look, int steps, int chain) {
        return isRipe(look) ? look : look(look + steps, chain);
    }
}
