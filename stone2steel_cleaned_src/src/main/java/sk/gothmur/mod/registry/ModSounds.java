package sk.gothmur.mod.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import sk.gothmur.mod.stone2steel;

public class ModSounds {
    // NeoForge 1.21.x: používaj Registries.SOUND_EVENT (nie ForgeRegistries)
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, stone2steel.MODID);

    // stone2steel:bow_drill.grind  → mapuje sa na assets/stone2steel/sounds.json
    public static final DeferredHolder<SoundEvent, SoundEvent> BOW_DRILL_GRIND =
            SOUND_EVENTS.register("bow_drill.grind",
                    () -> SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(stone2steel.MODID, "bow_drill.grind")));

    public static void register(IEventBus bus) {
        SOUND_EVENTS.register(bus);
    }
}
