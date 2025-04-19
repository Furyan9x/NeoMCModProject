package net.furyan.riyaposmod.registries;

import net.furyan.riyaposmod.RiyaposMod;
import net.furyan.riyaposmod.faction.capability.PlayerFactionImpl;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Registry for all attachment types in the mod.
 * This class centralizes the registration of attachment types to keep other classes clean.
 */
public class FactionAttachmentRegistry {
    // Create a DeferredRegister for attachment types
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, RiyaposMod.MOD_ID);

    // Register the attachment type for player faction data with serialization
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerFactionImpl>> PLAYER_FACTION_ATTACHMENT =
            ATTACHMENT_TYPES.register("player_faction", () ->
                    AttachmentType.serializable(PlayerFactionImpl::new)
                            .copyOnDeath()
                            .build());

    /**
     * Registers all attachment types with the event bus.
     *
     * @param eventBus The mod event bus
     */
    public static void register(IEventBus eventBus) {
        ATTACHMENT_TYPES.register(eventBus);
    }
}
