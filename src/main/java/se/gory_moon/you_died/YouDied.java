package se.gory_moon.you_died;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

/**
 * You Died — a Dark Souls inspired death screen.
 *
 * <p>1.12.2 backport of Gory_Moon's "You Died". Pure client-side: on death the vanilla
 * {@code GuiGameOver} is swapped for an animated "YOU DIED" splash that fades into the
 * normal death menu. See {@code se.gory_moon.you_died.client} for the rendering + screen swap.</p>
 */
@Mod(modid = YouDied.MOD_ID, name = YouDied.NAME, version = YouDied.VERSION, clientSideOnly = true,
        acceptedMinecraftVersions = "[1.12.2]")
public class YouDied {

    public static final String MOD_ID = "you_died";
    public static final String NAME = "You Died";
    public static final String VERSION = "1.12.2-1.0";

    @GameRegistry.ObjectHolder(MOD_ID + ":death")
    public static SoundEvent DEATH_SOUND;

    @Mod.EventBusSubscriber(modid = MOD_ID)
    public static class Registration {
        @SubscribeEvent
        public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
            ResourceLocation id = new ResourceLocation(MOD_ID, "death");
            event.getRegistry().register(new SoundEvent(id).setRegistryName(id));
        }
    }
}
