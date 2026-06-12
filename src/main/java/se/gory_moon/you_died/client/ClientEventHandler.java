package se.gory_moon.you_died.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import se.gory_moon.you_died.YouDied;

/**
 * Swaps the vanilla death screen for the animated {@link GuiDeathSplash} and plays the
 * death sting. Uses Forge's {@link GuiOpenEvent} hook — no vanilla class is replaced or
 * re-registered, so this stays compatible with other mods that touch {@code GuiGameOver}.
 */
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = YouDied.MOD_ID)
public final class ClientEventHandler {

    /** SRG + MCP names for {@code GuiGameOver.causeOfDeath} (private, no getter in 1.12.2). */
    private static final String[] CAUSE_OF_DEATH = {"field_184871_f", "causeOfDeath"};

    private ClientEventHandler() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onGuiOpen(GuiOpenEvent event) {
        GuiScreen gui = event.getGui();
        if (!(gui instanceof GuiGameOver) || gui instanceof GuiDeathSplash) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        // Don't re-wrap if we're already showing a death screen (e.g. a resize re-open).
        if (player == null || mc.currentScreen instanceof GuiGameOver) {
            return;
        }

        ITextComponent cause = ReflectionHelper.getPrivateValue(GuiGameOver.class, (GuiGameOver) gui, CAUSE_OF_DEATH);
        event.setGui(new GuiDeathSplash(cause));
        // Guard against a missing ObjectHolder injection so a registry hiccup can't crash every death.
        if (YouDied.DEATH_SOUND != null) {
            // getRecord(sound, volume, pitch) at full volume. getMasterRecord plays at the quiet
            // UI volume (0.25); upstream used volume 1.0 so the 8s sting clearly bleeds into the
            // respawn menu (the splash hands off at 5.3s).
            mc.getSoundHandler().playSound(PositionedSoundRecord.getRecord(YouDied.DEATH_SOUND, 1.0F, 1.0F));
        }
    }
}
