package org.skysongdev.skysongwardrobe.mannequin;

import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mannequin;
import org.bukkit.inventory.MainHand;
import org.bukkit.profile.PlayerTextures;
import org.skysongdev.skysongwardrobe.render.ArmModel;

/**
 * Spawns Paper's vanilla mannequin as furniture: it can't be pushed, shows no name or
 * description, never despawns, and is saved with its chunk. Damage is handled by the plugin's
 * break listener, not here. Until its outfit is rendered it wears the game's default skin, with
 * its arm model already fixed so a classic mannequin never shows up slim; the rendered skin then
 * carries the same model.
 * The {@link MannequinSpawnGuard} keeps mob-spawning rules from cancelling it.
 */
public final class PaperMannequinSpawner implements MannequinSpawner {
    private final MannequinSpawnGuard guard;

    public PaperMannequinSpawner(MannequinSpawnGuard guard) {
        this.guard = guard;
    }

    @Override
    public LivingEntity spawn(Location location, ArmModel model) {
        return guard.allowWhile(() -> location.getWorld().spawn(location, Mannequin.class, mannequin -> {
            guard.expect(mannequin.getUniqueId());
            mannequin.setImmovable(true);
            mannequin.setDescription(null);
            mannequin.setCustomNameVisible(false);
            mannequin.setMainHand(MainHand.RIGHT);
            mannequin.setRemoveWhenFarAway(false);
            mannequin.setPersistent(true);
            mannequin.setSilent(true);
            mannequin.setCollidable(false);
            mannequin.setProfile(ResolvableProfile.resolvableProfile()
                    .skinPatch(patch -> patch.model(skinModel(model)))
                    .build());
        }));
    }

    /** The game's skin model for an arm model. */
    static PlayerTextures.SkinModel skinModel(ArmModel model) {
        return model == ArmModel.SLIM ? PlayerTextures.SkinModel.SLIM : PlayerTextures.SkinModel.CLASSIC;
    }
}
