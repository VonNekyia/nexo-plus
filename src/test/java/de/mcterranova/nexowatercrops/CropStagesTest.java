package de.mcterranova.nexowatercrops;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CropStagesTest {

    // rice: stages 0 to 6, then ripe 7, 7a, 7b at looks 7, 8, 9
    private final CropStages rice = new CropStages(7, 3);

    @Test
    void growsOneStageAtATime() {
        assertEquals(1, rice.grown(0, 1, 2));
        assertEquals(6, rice.grown(5, 1, 0));
    }

    @Test
    void theChainPicksTheRipeLook() {
        assertEquals(7, rice.grown(6, 1, 0));
        assertEquals(8, rice.grown(6, 1, 1));
        assertEquals(9, rice.grown(6, 1, 2));
        assertEquals(9, rice.grown(4, 5, 2));
    }

    @Test
    void ripeStaysRipe() {
        assertTrue(rice.isRipe(8));
        assertFalse(rice.isRipe(6));
        assertEquals(8, rice.grown(8, 3, 2));
    }

    @Test
    void movedFurnitureKeepsItsStageAndChain() {
        assertArrayEquals(new int[]{0, 0}, RiceMigration.stageAndChain("reis_saat"));
        assertArrayEquals(new int[]{4, 0}, RiceMigration.stageAndChain("reis_stufe4"));
        assertArrayEquals(new int[]{7, 1}, RiceMigration.stageAndChain("reis_b_stufe7"));
        assertArrayEquals(new int[]{3, 2}, RiceMigration.stageAndChain("reis_c_stufe3"));
        assertNull(RiceMigration.stageAndChain("kohl_stufe1"));
        // ripe furniture lands on the ripe look of its chain
        assertEquals(8, rice.look(7, 1));
        assertEquals(4, rice.look(4, 1));
    }

    @Test
    void looksComeFirstThenVanillaKelp() {
        Map<String, String> models = KelpHost.modelsByAge(List.of("a", "b"));
        assertEquals(26, models.size());
        assertEquals("a", models.get("age=0"));
        assertEquals("b", models.get("age=1"));
        assertEquals("minecraft:block/kelp", models.get("age=2"));
        assertEquals("minecraft:block/kelp", models.get("age=25"));
    }
}
