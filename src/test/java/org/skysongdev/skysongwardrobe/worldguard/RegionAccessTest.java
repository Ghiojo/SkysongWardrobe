package org.skysongdev.skysongwardrobe.worldguard;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongwardrobe.SkysongWardrobe;
import org.skysongdev.skysongwardrobe.mannequin.Mannequin;
import org.skysongdev.skysongwardrobe.mannequin.OwnerAccess;
import org.skysongdev.skysongwardrobe.render.ArmModel;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionAccessTest {
    private SkysongWardrobe plugin;
    private PlayerMock owner;
    private PlayerMock visitor;
    private Location here;
    private Mannequin mannequin;
    /** Empty means no region covers the location. */
    private final Map<MannequinAction, Boolean> rules = new EnumMap<>(MannequinAction.class);
    private boolean inRegion;
    private RegionAccess access;

    @BeforeEach
    void setUp() {
        ServerMock server = MockBukkit.mock();
        plugin = MockBukkit.load(SkysongWardrobe.class);
        owner = server.addPlayer();
        visitor = server.addPlayer();
        here = new Location(server.addSimpleWorld("world"), 0, 64, 0);
        mannequin = new Mannequin(UUID.randomUUID(), owner.getUniqueId(), new ItemStack(Material.ARMOR_STAND), "oak",
                ArmModel.CLASSIC);
        RegionRules regions = (player, location, action) ->
                inRegion ? Optional.of(rules.getOrDefault(action, false)) : Optional.empty();
        access = new RegionAccess(regions, new OwnerAccess(() -> plugin.configuration().settings().mannequins()),
                () -> plugin.configuration().settings().mannequins());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void outsideRegionsOnlyTheOwnerMayUseOrBreak() {
        assertTrue(access.canPlace(visitor, here));
        assertTrue(access.canUse(owner, mannequin, here));
        assertFalse(access.canUse(visitor, mannequin, here));
        assertFalse(access.canBreak(visitor, mannequin, here));
    }

    @Test
    void insideARegionItsFlagsDecideEvenForTheOwner() {
        inRegion = true;
        rules.put(MannequinAction.USE, true);

        assertTrue(access.canUse(visitor, mannequin, here));
        assertFalse(access.canBreak(owner, mannequin, here));
        assertFalse(access.canPlace(owner, here));
    }

    @Test
    void staffMayAlways() {
        inRegion = true;
        visitor.addAttachment(plugin, "skysongwardrobe.mannequin.other", true);

        assertTrue(access.canPlace(visitor, here));
        assertTrue(access.canBreak(visitor, mannequin, here));
    }
}
