package se.gory_moon.you_died.client;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import org.lwjgl.opengl.GL11;
import se.gory_moon.you_died.YouDied;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * Dark Souls style death splash. Extends the vanilla {@link GuiGameOver} so the normal
 * death menu (respawn / title buttons, cause of death, score) renders for free once the
 * splash animation completes.
 *
 * <p>Timeline (ms from first frame), ported from the upstream 1.20 animation:</p>
 * <ul>
 *   <li>0&ndash;1000: bands + "YOU DIED" fade in</li>
 *   <li>4000: text begins fading out</li>
 *   <li>5300: hand off to the vanilla death menu, which fades in from black</li>
 * </ul>
 */
public class GuiDeathSplash extends GuiGameOver {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(YouDied.MOD_ID, "textures/gui/you_died.png");

    /** Native dimensions of the baked "YOU DIED" texture. */
    private static final int TEX_W = 512;
    private static final int TEX_H = 128;

    /** Solid alpha of the letterbox bands at full fade-in (0xEA, matching upstream). */
    private static final int BAND_ALPHA = 0xEA;

    private long fadeInStart;
    private long fadeOutStart;
    private long fadeInMenuStart;
    private boolean showingMenu;

    public GuiDeathSplash(@Nullable ITextComponent causeOfDeath) {
        super(causeOfDeath);
    }

    @Override
    public void initGui() {
        super.initGui();
        // Hide the vanilla menu buttons until the splash finishes.
        for (GuiButton button : this.buttonList) {
            button.enabled = false;
            button.visible = false;
        }
    }

    @Override
    public void updateScreen() {
        // Intentionally NOT calling super: GuiGameOver.updateScreen runs a private timer
        // that would re-enable the buttons after 1s. We enable them ourselves at menu hand-off.
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        long now = System.currentTimeMillis();

        if (fadeInStart == 0L) {
            fadeInStart = now;
        }
        if (fadeOutStart == 0L && fadeInStart + 4000L < now) {
            fadeOutStart = now;
        }
        if (fadeInMenuStart == 0L && fadeInStart + 5300L < now) {
            fadeInMenuStart = now;
            showingMenu = true;
            for (GuiButton button : this.buttonList) {
                button.enabled = true;
                button.visible = true;
            }
        }

        if (showingMenu) {
            drawMenu(mouseX, mouseY, partialTicks, now);
        } else {
            drawSplash(now);
        }
    }

    private void drawSplash(long now) {
        float zoomIn = MathHelper.clamp((now - fadeInStart) / 5500.0F, 0.0F, 1.0F);

        float fadeIn = 0.0F;
        float fadeInText = 0.0F;
        if (fadeOutStart == 0L) {
            float f = (now - fadeInStart) / 1000.0F;
            fadeIn = MathHelper.clamp(f, 0.0F, 1.0F);
            fadeInText = MathHelper.clamp(f - 0.5F, 0.0F, 1.0F);
        } else {
            float f = (now - fadeOutStart) / 1000.0F;
            fadeIn = MathHelper.clamp(1.0F - f, 0.0F, 1.0F);
            fadeInText = MathHelper.clamp(1.3F - f, 0.0F, 1.0F);
        }

        drawBands(fadeIn);
        drawTitle(zoomIn, fadeInText);
    }

    /** Black letterbox bands behind the title, alpha-scaled by the overall fade. */
    private void drawBands(float fade) {
        int alpha = MathHelper.clamp((int) (fade * BAND_ALPHA), 0, BAND_ALPHA);
        int band = alpha << 24;
        int transparent = 0x00000000;
        int centerY = this.height / 2;

        drawGradientRect(0, centerY - 45, this.width, centerY - 25, transparent, band);
        drawGradientRect(0, centerY - 25, this.width, centerY + 25, band, band);
        drawGradientRect(0, centerY + 25, this.width, centerY + 45, band, transparent);
    }

    /** Draws the baked "YOU DIED" texture, centered, with a slow zoom-in and alpha fade. */
    private void drawTitle(float zoomIn, float alpha) {
        if (alpha <= 0.0F) {
            return;
        }

        float baseW = Math.min(this.width * 0.6F, 480.0F);
        float zoom = 1.0F + 0.08F * zoomIn;
        float w = baseW * zoom;
        float h = w * ((float) TEX_H / (float) TEX_W);
        float x = (this.width / 2.0F) - (w / 2.0F);
        float y = (this.height / 2.0F) - (h / 2.0F);

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        this.mc.getTextureManager().bindTexture(TEXTURE);
        GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
        drawModalRectWithCustomSizedTexture(
                Math.round(x), Math.round(y), 0.0F, 0.0F,
                Math.round(w), Math.round(h), w, h);
        // Reset color so later draws (the menu, tooltips) aren't tinted. See port gotcha #3.
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
    }

    /** Renders the vanilla death menu, fading in from black over the first second. */
    private void drawMenu(int mouseX, int mouseY, float partialTicks, long now) {
        super.drawScreen(mouseX, mouseY, partialTicks);

        float menuFade = MathHelper.clamp((now - fadeInMenuStart) / 1000.0F, 0.0F, 1.0F);
        if (menuFade < 1.0F) {
            int overlayAlpha = MathHelper.clamp((int) ((1.0F - menuFade) * 255.0F), 0, 255);
            drawRect(0, 0, this.width, this.height, overlayAlpha << 24);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (showingMenu) {
            super.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (showingMenu) {
            super.keyTyped(typedChar, keyCode);
        }
        // Swallow all input during the splash, including ESC.
    }
}
