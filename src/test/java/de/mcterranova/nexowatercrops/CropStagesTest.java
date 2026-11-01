package de.mcterranova.nexowatercrops;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void looksComeFirstThenVanillaKelp() {
        Map<String, String> models = KelpHost.modelsByAge(List.of("a", "b"));
        assertEquals(26, models.size());
        assertEquals("a", models.get("age=0"));
        assertEquals("b", models.get("age=1"));
        assertEquals("minecraft:block/kelp", models.get("age=2"));
        assertEquals("minecraft:block/kelp", models.get("age=25"));
    }
}
