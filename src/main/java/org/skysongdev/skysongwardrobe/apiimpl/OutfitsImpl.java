package org.skysongdev.skysongwardrobe.apiimpl;

import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongwardrobe.api.ChangeResult;
import org.skysongdev.skysongwardrobe.api.Outfits;
import org.skysongdev.skysongwardrobe.api.Wearer;
import org.skysongdev.skysongwardrobe.config.PluginConfiguration;
import org.skysongdev.skysongwardrobe.config.SlotDefinition;
import org.skysongdev.skysongwardrobe.mannequin.MannequinRepository;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeEditor;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeKind;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/** {@link Outfits} on the {@link WardrobeEditor}, after checking the wearer exists. */
final class OutfitsImpl implements Outfits {
    private final MainThread mainThread;
    private final WardrobeEditor editor;
    private final PluginConfiguration configuration;
    private final MannequinRepository mannequins;
    private final Supplier<Optional<NpcHooks>> npcs;

    OutfitsImpl(MainThread mainThread, WardrobeEditor editor, PluginConfiguration configuration,
                MannequinRepository mannequins, Supplier<Optional<NpcHooks>> npcs) {
        this.mainThread = mainThread;
        this.editor = editor;
        this.configuration = configuration;
        this.mannequins = mannequins;
        this.npcs = npcs;
    }

    @Override
    public List<String> slots() {
        mainThread.check();
        return configuration.layout().map(layout -> layout.slots().stream().map(SlotDefinition::id).toList())
                .orElse(List.of());
    }

    @Override
    public Map<String, ItemStack> outfit(Wearer wearer) {
        mainThread.check();
        Objects.requireNonNull(wearer, "wearer");
        return missing(wearer).isPresent() ? Map.of() : Map.copyOf(editor.items(kind(wearer), wearer.id()));
    }

    @Override
    public Optional<ItemStack> item(Wearer wearer, String slotId) {
        Objects.requireNonNull(slotId, "slotId");
        return Optional.ofNullable(outfit(wearer).get(slotId)).map(ItemStack::clone);
    }

    @Override
    public ChangeResult setItem(Wearer wearer, String slotId, ItemStack item) {
        mainThread.check();
        Objects.requireNonNull(wearer, "wearer");
        Objects.requireNonNull(slotId, "slotId");
        Optional<ChangeResult> missing = missing(wearer);
        if (missing.isPresent()) {
            return missing.get();
        }
        return ChangeResult.valueOf(editor.set(kind(wearer), wearer.id(), slotId, item).name());
    }

    @Override
    public ChangeResult clear(Wearer wearer, String slotId) {
        return setItem(wearer, slotId, null);
    }

    /** Why the wearer has no wardrobe here: Citizens is missing, or no such NPC or mannequin. */
    private Optional<ChangeResult> missing(Wearer wearer) {
        return switch (wearer.type()) {
            case PLAYER -> Optional.empty();
            case MANNEQUIN -> mannequins.find(wearer.id()).isPresent() ? Optional.empty()
                    : Optional.of(ChangeResult.NOT_FOUND);
            case NPC -> npcs.get()
                    .map(hooks -> hooks.directory().byId(wearer.id()).isPresent() ? Optional.<ChangeResult>empty()
                            : Optional.of(ChangeResult.NOT_FOUND))
                    .orElse(Optional.of(ChangeResult.UNAVAILABLE));
        };
    }

    private static WardrobeKind kind(Wearer wearer) {
        return switch (wearer.type()) {
            case PLAYER -> WardrobeKind.PLAYER;
            case NPC -> WardrobeKind.NPC;
            case MANNEQUIN -> WardrobeKind.MANNEQUIN;
        };
    }
}
