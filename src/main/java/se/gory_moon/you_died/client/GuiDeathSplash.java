package se.gory_moon.you_died.client;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.opengl.GL11;
import se.gory_moon.you_died.YouDied;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * Dark Souls style death splash. Extends the vanilla {@link GuiGameOver} so the normal
 * death menu (respawn / title buttons) renders for free once the splash finishes.
 *
 * <p>Faithful port of the upstream 1.20 animation. Timeline (ms from first frame):</p>
 * <ul>
 *   <li>0&ndash;1000: black bands + "YOU DIED" fade in (text starts at 500ms)</li>
 *   <li>1000&ndash;4000: hold</li>
 *   <li>4000&ndash;5000: bands + text fade out</li>
 *   <li>5000&ndash;5300: brief beat showing just the frozen world (no overlay)</li>
 *   <li>5300&ndash;6300: the death screen (red gradient + title + score) fades up from
 *       transparent; once fully opaque it hands off to the real vanilla menu with buttons</li>
 * </ul>
 *
 * <p>1.12.2 cannot alpha-multiply the whole vanilla {@code GuiGameOver.drawScreen} the way the
 * 1.20 shader pipeline does, so the menu render is reimplemented here with per-element alpha
 * (mirroring upstream's {@code DeathScreenWrapper}) until the fade completes.</p>
 */
public class GuiDeathSplash extends GuiGameOver {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(YouDied.MOD_ID, "textures/gui/you_died.png");

    /** Native dimensions of the baked "YOU DIED" texture. */
    private static final int TEX_W = 512;
    private static final int TEX_H = 128;

    /** Solid alpha of the letterbox bands at full fade-in (0xEA, matching upstream). */
    private static final int BAND_ALPHA = 0xEA;

    /** Vanilla death-screen background gradient (top 0x60500000 -> bottom 0xA0803030). */
    private static final int BG_TOP_ALPHA = 0x60;
    private static final int BG_TOP_RGB = 0x500000;
    private static final int BG_BOTTOM_ALPHA = 0xA0;
    private static final int BG_BOTTOM_RGB = 0x803030;

    private final ITextComponent causeOfDeath;

    private long fadeInStart;
    private long fadeOutStart;
    private long fadeInMenuStart;
    private boolean showingMenu;

    public GuiDeathSplash(@Nullable ITextComponent causeOfDeath) {
        super(causeOfDeath);
        this.causeOfDeath = causeOfDeath;
    }

    @Override
    public void initGui() {
        super.initGui();
        // Hide the vanilla menu buttons until the death screen has finished fading in.
        for (GuiButton button : this.buttonList) {
            button.enabled = false;
            button.visible = false;
        }
    }

    @Override
    public void updateScreen() {
        // Intentionally NOT calling super: GuiGameOver.updateScreen runs a private timer
        // that would re-enable the buttons after 1s. We enable them ourselves once faded in.
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
        }

        float zoomIn = MathHelper.clamp((now - fadeInStart) / 5500.0F, 0.0F, 1.0F);
        float fadeIn;
        float fadeInText;

        if (fadeOutStart == 0L) {
            // Fade in, then hold.
            float f = (now - fadeInStart) / 1000.0F;
            fadeIn = MathHelper.clamp(f, 0.0F, 1.0F);
            fadeInText = MathHelper.clamp(f - 0.5F, 0.0F, 1.0F);
        } else if (fadeInMenuStart == 0L) {
            // Bands + text fade out.
            float f = (now - fadeOutStart) / 1000.0F;
            fadeIn = MathHelper.clamp(1.0F - f, 0.0F, 1.0F);
            fadeInText = MathHelper.clamp(1.3F - f, 0.0F, 1.0F);
        } else {
            // Death screen fades up from transparent.
            fadeIn = MathHelper.clamp((now - fadeInMenuStart) / 1000.0F, 0.0F, 1.0F);
            fadeInText = 0.0F;
        }

        if (showingMenu) {
            if (fadeIn >= 1.0F) {
                // Fully faded in: hand off to the real vanilla menu (buttons included).
                for (GuiButton button : this.buttonList) {
                    button.enabled = true;
                    button.visible = true;
                }
                super.drawScreen(mouseX, mouseY, partialTicks);
            } else {
                drawFadingMenu(fadeIn);
            }
        } else {
            drawBands(fadeIn);
            drawTitle(zoomIn, fadeInText);
        }
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

        int w = Math.round(Math.min(this.width * 0.6F, 480.0F));
        int h = Math.round(w * ((float) TEX_H / (float) TEX_W));
        float zoom = 1.0F + 0.08F * zoomIn;

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        this.mc.getTextureManager().bindTexture(TEXTURE);
        GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);

        // Fixed integer quad scaled on the GPU. Rounding the quad coordinates every frame
        // (as the zoom grew) made the text jitter; a constant quad + matrix scale is smooth.
        GlStateManager.pushMatrix();
        GlStateManager.translate(this.width / 2.0F, this.height / 2.0F, 0.0F);
        GlStateManager.scale(zoom, zoom, 1.0F);
        drawModalRectWithCustomSizedTexture(-w / 2, -h / 2, 0.0F, 0.0F, w, h, w, h);
        GlStateManager.popMatrix();

        // Reset color so later draws (the menu, tooltips) aren't tinted. See port gotcha #3.
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
    }

    /**
     * Reimplements the vanilla death-screen render with a global alpha so it can fade up from
     * transparent. Matches {@code GuiGameOver}'s layout exactly so the hand-off to {@code super}
     * (when {@code fadeIn} reaches 1) is seamless.
     */
    private void drawFadingMenu(float fadeIn) {
        int topColor = (MathHelper.clamp((int) (BG_TOP_ALPHA * fadeIn), 0, BG_TOP_ALPHA) << 24) | BG_TOP_RGB;
        int bottomColor = (MathHelper.clamp((int) (BG_BOTTOM_ALPHA * fadeIn), 0, BG_BOTTOM_ALPHA) << 24) | BG_BOTTOM_RGB;
        drawGradientRect(0, 0, this.width, this.height, topColor, bottomColor);

        int alphaColor = MathHelper.clamp((int) (fadeIn * 255.0F), 0, 255) << 24;
        // The font renderer forces full opacity when the top 6 alpha bits are clear, so only
        // draw text once it would actually be translucent (matches upstream's guard).
        if ((alphaColor & 0xFC000000) == 0) {
            return;
        }

        boolean hardcore = this.mc.world != null && this.mc.world.getWorldInfo().isHardcoreModeEnabled();
        String title = I18n.format(hardcore ? "deathScreen.title.hardcore" : "deathScreen.title");

        GlStateManager.pushMatrix();
        GlStateManager.scale(2.0F, 2.0F, 2.0F);
        drawCenteredString(this.fontRenderer, title, this.width / 2 / 2, 30, 0xFFFFFF | alphaColor);
        GlStateManager.popMatrix();

        if (this.causeOfDeath != null) {
            drawCenteredString(this.fontRenderer, this.causeOfDeath.getFormattedText(), this.width / 2, 85, 0xFFFFFF | alphaColor);
        }

        int score = this.mc.player != null ? this.mc.player.getScore() : 0;
        String scoreText = I18n.format("deathScreen.score") + ": " + TextFormatting.YELLOW + score;
        drawCenteredString(this.fontRenderer, scoreText, this.width / 2, 100, 0xFFFFFF | alphaColor);
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
