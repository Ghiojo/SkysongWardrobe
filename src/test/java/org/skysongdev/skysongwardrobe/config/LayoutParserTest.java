package org.skysongdev.skysongwardrobe.config;

import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayoutParserTest {
    private final LayoutParser parser = new LayoutParser();

    @BeforeEach
    void startServer() {
        // Material.matchMaterial needs a registry, which MockBukkit provides.
        MockBukkit.mock();
    }

    @AfterEach
    void stopServer() {
        MockBukkit.unmock();
    }

    @Test
    void readsTitleFillerAndSlots() {
        WardrobeLayout layout = parse("""
                title: "<gold>Closet"
                filler: {material: BLACK_STAINED_GLASS_PANE, name: "-"}
                slots:
                  hat: {position: 4, accepts: [Hat, crown], priority: 50}
                """).orElseThrow();

        assertEquals("<gold>Closet", layout.title());
        assertEquals(Material.BLACK_STAINED_GLASS_PANE, layout.fillerMaterial());
        assertEquals("-", layout.fillerName());
        SlotDefinition hat = layout.slotAt(4).orElseThrow();
        assertEquals("hat", hat.id());
        assertEquals(50, hat.priority());
        assertTrue(hat.accepts("HAT"));
        assertTrue(hat.accepts("crown"));
        assertFalse(hat.accepts("boots"));
        assertTrue(parser.errors().isEmpty());
    }

    @Test
    void missingTitleAndFillerUseDefaults() {
        WardrobeLayout layout = parse("slots:\n  coat: {position: 10, accepts: [coat], priority: 1}\n").orElseThrow();

        assertEquals(LayoutParser.DEFAULT_TITLE, layout.title());
        assertEquals(LayoutParser.DEFAULT_FILLER, layout.fillerMaterial());
        assertEquals(" ", layout.fillerName());
    }

    @Test
    void slotsComeOutLowestPriorityFirst() {
        WardrobeLayout layout = parse("""
                slots:
                  coat: {position: 10, accepts: [coat], priority: 30}
                  boots: {position: 28, accepts: [boots], priority: 10}
                  gloves: {position: 19, accepts: [gloves], priority: 20}
                """).orElseThrow();

        assertEquals(List.of("boots", "gloves", "coat"), layout.slots().stream().map(SlotDefinition::id).toList());
    }

    @Test
    void refusesSharedPosition() {
        assertRefused("""
                slots:
                  coat: {position: 10, accepts: [coat], priority: 30}
                  cape: {position: 10, accepts: [cape], priority: 40}
                """, "slots.cape and slots.coat share position 10.");
    }

    @Test
    void refusesSharedPriority() {
        assertRefused("""
                slots:
                  coat: {position: 10, accepts: [coat], priority: 30}
                  cape: {position: 11, accepts: [cape], priority: 30}
                """, "slots.cape and slots.coat share priority 30, so their draw order is undefined.");
    }

    @Test
    void refusesPositionOutsideTheMenu() {
        assertRefused("slots:\n  coat: {position: 54, accepts: [coat], priority: 1}\n",
                "slots.coat.position must be a whole number from 0 to 53.");
        assertRefused("slots:\n  coat: {position: -1, accepts: [coat], priority: 1}\n",
                "slots.coat.position must be a whole number from 0 to 53.");
    }

    @Test
    void refusesTheColumnsKeptForMannequinEquipment() {
        assertRefused("slots:\n  coat: {position: 7, accepts: [coat], priority: 1}\n",
                "slots.coat.position 7 is in the last two columns, which are kept for mannequin equipment.");
        assertRefused("slots:\n  coat: {position: 53, accepts: [coat], priority: 1}\n",
                "slots.coat.position 53 is in the last two columns, which are kept for mannequin equipment.");
        assertTrue(parse("slots:\n  coat: {position: 6, accepts: [coat], priority: 1}\n").isPresent());
    }

    @Test
    void refusesSlotIdsThatLookLikeEquipment() {
        assertRefused("slots:\n  \"equipment:head\": {position: 10, accepts: [hat], priority: 1}\n",
                "slots.equipment:head: slot ids can't start with equipment:, which is kept for mannequin equipment.");
    }

    @Test
    void refusesMissingPriorityOrAccepts() {
        assertRefused("slots:\n  coat: {position: 10, accepts: [coat]}\n",
                "slots.coat.priority must be a whole number.");
        assertRefused("slots:\n  coat: {position: 10, accepts: [], priority: 1}\n",
                "slots.coat.accepts lists no wardrobe_type values.");
    }

    @Test
    void refusesEmptySlotsAndBadFiller() {
        assertRefused("title: x\n", "slots lists no slots.");
        assertRefused("filler: {material: NOT_A_BLOCK}\nslots:\n  coat: {position: 10, accepts: [coat], priority: 1}\n",
                "filler.material NOT_A_BLOCK is not an item.");
    }

    @Test
    void errorsClearOnTheNextParse() {
        parse("title: x\n");
        parse("slots:\n  coat: {position: 10, accepts: [coat], priority: 1}\n");

        assertTrue(parser.errors().isEmpty());
    }

    private void assertRefused(String yaml, String error) {
        assertTrue(parse(yaml).isEmpty());
        assertTrue(parser.errors().contains(error), () -> "errors were " + parser.errors());
    }

    private Optional<WardrobeLayout> parse(String yaml) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(yaml);
        } catch (InvalidConfigurationException e) {
            throw new IllegalArgumentException(e);
        }
        return parser.parse(config);
    }
}
