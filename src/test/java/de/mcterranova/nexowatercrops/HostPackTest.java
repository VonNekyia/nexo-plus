package de.mcterranova.nexowatercrops;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HostPackTest {

    @Test
    void onlyStandingBelowAgeFourIsTheHost() {
        String blockstate = HostPack.blockstate();
        for (int age = 0; age < 4; age++) {
            assertTrue(blockstate.contains("\"age=" + age + ",hanging=false\": {\n      \"model\": \"nexowatercrops:block/host\""));
        }
        assertTrue(blockstate.contains("\"age=4,hanging=false\": {\n      \"model\": \"minecraft:block/mangrove_propagule\""));
        for (int age = 0; age <= 4; age++) {
            assertTrue(blockstate.contains("\"age=" + age + ",hanging=true\": {\n      \"model\": \"minecraft:block/mangrove_propagule_hanging_" + age + "\""));
        }
        assertEquals(10, blockstate.split("\"model\"").length - 1);
    }

    @Test
    void modelHasNoElements() {
        assertEquals("{\n  \"textures\": {\n    \"particle\": \"minecraft:terranova/reis/reis_stufe3\"\n  },\n  \"elements\": []\n}\n",
                HostPack.model("minecraft:terranova/reis/reis_stufe3"));
    }
}
