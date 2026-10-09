package caeruleusTait.world.preview.client.gui.widgets;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.Identifier;

import java.awt.Color;

public class ColorChooser extends AbstractWidget {

    public static final int INITIAL_SV_SQUARE_SIZE = 128;
    public static final int INITIAL_H_BAR_WIDTH = 16;
    public static final int SEPARATOR = 10;
    public static final int INITIAL_FINAL_COLOR_HEIGHT = 20;

    private int svSquareSize;
    private int hBarWidth;
    private int finalColorHeight;

    private float hue = 0f;
    private float saturation = 0f;
    private float value = 0f;

    private int argbColor = 0xFF000000;
    private int argbHueOnly = 0xFF000000;

    private ColorUpdater updater;

    private NativeImage svImage;
    private DynamicTexture svTexture;
    private NativeImage hueImage;
    private DynamicTexture hueTexture;
    private int cachedHueInt = -1;

    private static final Identifier SV_IDENTIFIER = Identifier.fromNamespaceAndPath("world-preview-unofficial", "color_chooser_sv");
    private static final Identifier HUE_IDENTIFIER = Identifier.fromNamespaceAndPath("world-preview-unofficial", "color_chooser_hue");

    public ColorChooser(int x, int y) {
        super(x, y, 10, 10, CommonComponents.EMPTY);
        svSquareSize = INITIAL_SV_SQUARE_SIZE;
        hBarWidth = INITIAL_H_BAR_WIDTH;
        finalColorHeight = INITIAL_FINAL_COLOR_HEIGHT;
        recalculateSize();
    }

    private void recalculateSize() {
        width = svSquareSize + SEPARATOR + hBarWidth;
        height = svSquareSize + SEPARATOR + finalColorHeight;
    }

    public void setSquareSize(int squareSize) {
        float scalor = (float) squareSize / (float) INITIAL_SV_SQUARE_SIZE;
        svSquareSize = squareSize;
        hBarWidth = (int)(INITIAL_H_BAR_WIDTH * scalor);
        finalColorHeight = (int)(INITIAL_FINAL_COLOR_HEIGHT * scalor);
        recalculateSize();
        cachedHueInt = -1;
    }

    private void ensureTextures() {
        Minecraft mc = Minecraft.getInstance();
        if (svImage == null || svImage.getWidth() != svSquareSize || svImage.getHeight() != svSquareSize) {
            if (svTexture != null) svTexture.close();
            svImage = new NativeImage(svSquareSize, svSquareSize, true);
            svTexture = new DynamicTexture(() -> "world-preview-unofficial_color_chooser_sv", svImage);
            mc.getTextureManager().register(SV_IDENTIFIER, svTexture);
            cachedHueInt = -1;
        }
        if (hueImage == null || hueImage.getWidth() != hBarWidth || hueImage.getHeight() != svSquareSize) {
            if (hueTexture != null) hueTexture.close();
            hueImage = new NativeImage(hBarWidth, svSquareSize, true);
            hueTexture = new DynamicTexture(() -> "world-preview-unofficial_color_chooser_hue", hueImage);
            mc.getTextureManager().register(HUE_IDENTIFIER, hueTexture);
            cachedHueInt = -1;
        }
    }

    private void regenerateTextures() {
        int hueInt = (int) (hue * 360f);
        if (hueInt == cachedHueInt) return;
        cachedHueInt = hueInt;

        ensureTextures();

        for (int y = 0; y < svSquareSize; y++) {
            float v = 1f - (float) y / (float) (svSquareSize - 1);
            for (int x = 0; x < svSquareSize; x++) {
                float s = (float) x / (float) (svSquareSize - 1);
                int rgb = Color.HSBtoRGB(hue, s, v);
                svImage.setPixel(x, y, 0xFF000000 | rgb);
            }
        }
        svTexture.upload();

        for (int y = 0; y < svSquareSize; y++) {
            float h = 1f - (float) y / (float) (svSquareSize - 1);
            int rgb = Color.HSBtoRGB(h, 1f, 1f);
            for (int x = 0; x < hBarWidth; x++) {
                hueImage.setPixel(x, y, 0xFF000000 | rgb);
            }
        }
        hueTexture.upload();
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        extractor.fill(getX() - 2, getY() - 2, getX() + width + 2, getY() + height + 2, 0x77000000);

        regenerateTextures();

        int leftX = getX();
        int topY = getY();
        int rightX = leftX + svSquareSize;
        int botY = topY + svSquareSize;

        extractor.blit(RenderPipelines.GUI_TEXTURED, SV_IDENTIFIER, leftX, topY, 0, 0, svSquareSize, svSquareSize, svSquareSize, svSquareSize);

        int satX = leftX + Math.round(saturation * svSquareSize);
        int valY = topY + Math.round((1f - value) * svSquareSize);
        extractor.fill(satX - 4, valY - 4, satX + 4, valY + 4, value > .3 ? 0xFF000000 : 0xFFFFFFFF);
        extractor.fill(satX - 3, valY - 3, satX + 3, valY + 3, argbColor);

        leftX = rightX + SEPARATOR;
        rightX = leftX + hBarWidth;

        extractor.blit(RenderPipelines.GUI_TEXTURED, HUE_IDENTIFIER, leftX, topY, 0, 0, hBarWidth, svSquareSize, hBarWidth, svSquareSize);

        int hueY = topY + Math.round((1f - hue) * svSquareSize);
        extractor.fill(leftX - 2, hueY - 4, rightX + 2, hueY + 4, 0xFF000000);
        extractor.fill(leftX - 1, hueY - 3, rightX + 1, hueY + 3, argbHueOnly);

        extractor.fill(getX(), botY + SEPARATOR, getX() + width, getY() + height, argbColor);
    }

    public boolean mouseEvent(double mouseX, double mouseY, int button, boolean playSound) {
        if (!this.active || !this.visible || !isValidClickButton(new MouseButtonInfo(button, 0)) || !isMouseOver(mouseX, mouseY)) {
            return false;
        }
        double leftX = getX();
        double topY = getY();
        double rightX = leftX + svSquareSize;
        double botY = topY + svSquareSize;

        boolean updated = false;

        if (mouseX >= leftX && mouseX <= rightX && mouseY >= topY && mouseY <= botY) {
            if (playSound) {
                this.playDownSound(Minecraft.getInstance().getSoundManager());
            }
            value = 1f - (float) ((mouseY - topY) / (botY - topY));
            saturation = (float) ((mouseX - leftX) / (rightX - leftX));
            updated = true;
        }

        leftX = rightX + SEPARATOR;
        rightX = leftX + hBarWidth;

        if (mouseX >= leftX && mouseX <= rightX && mouseY >= topY && mouseY <= botY) {
            if (playSound) {
                this.playDownSound(Minecraft.getInstance().getSoundManager());
            }
            hue = 1f - (float) ((mouseY - topY) / (botY - topY));
            updated = true;
        }

        argbColor = Color.HSBtoRGB(hue, saturation, value);
        argbHueOnly = Color.HSBtoRGB(hue, 1f, 1f);
        if (updated) {
            runUpdater();
        }
        return updated;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean playSound) {
        return mouseEvent(event.x(), event.y(), event.buttonInfo().button(), true);
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
        mouseEvent(event.x(), event.y(), 0, false);
    }

    public void runUpdater() {
        if (updater == null) {
            return;
        }

        updater.doUpdate(
                (int) (hue * 360f),
                (int) (saturation * 100f),
                (int) (value * 100f)
        );
    }

    public void setUpdater(ColorUpdater updater) {
        this.updater = updater;
    }

    public void updateHSV(int h, int s, int v) {
        hue = (float) h / 360f;
        saturation = (float) s / 100f;
        value = (float) v / 100f;
        argbColor = Color.HSBtoRGB(hue, saturation, value);
        argbHueOnly = Color.HSBtoRGB(hue, 1f, 1f);
        runUpdater();
    }

    public void updateRGB(int rgb) {
        final int r = (rgb >> 16) & 0xFF;
        final int g = (rgb >> 8) & 0xFF;
        final int b = rgb & 0xFF;
        float[] hsv = Color.RGBtoHSB(r, g, b, null);
        hue = hsv[0];
        saturation = hsv[1];
        value = hsv[2];
        argbColor = 0xFF000000 | rgb;
        argbHueOnly = Color.HSBtoRGB(hue, 1f, 1f);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }

    public int colorRGB() {
        return argbColor & 0x00FFFFFF;
    }

    public interface ColorUpdater {
        void doUpdate(int hue, int saturation, int value);
    }
}
