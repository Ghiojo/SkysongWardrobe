package org.skysongdev.skysongwardrobe.mannequin;

import org.bukkit.profile.PlayerTextures;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongwardrobe.render.ArmModel;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaperMannequinSpawnerTest {
    @Test
    void placeholderSkinKeepsTheSpawnersArmModel() {
        assertEquals(PlayerTextures.SkinModel.CLASSIC, PaperMannequinSpawner.skinModel(ArmModel.CLASSIC));
        assertEquals(PlayerTextures.SkinModel.SLIM, PaperMannequinSpawner.skinModel(ArmModel.SLIM));
    }
}
