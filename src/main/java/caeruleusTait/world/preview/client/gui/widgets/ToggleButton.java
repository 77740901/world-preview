package caeruleusTait.world.preview.client.gui.widgets;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.resources.Identifier;

public class ToggleButton extends OldStyleImageButton {
    public boolean selected;
    protected final int xDiff;

    public ToggleButton(int x, int y, int width, int height, int xTexStart, int yTexStart, Identifier Identifier, OnPress onPress) {
        this(x, y, width, height, xTexStart, yTexStart, width, height, Identifier, 256, 256, onPress);
    }

    public ToggleButton(int x, int y, int width, int height, int xTexStart, int yTexStart, int xDiff, int yDiff, Identifier Identifier, OnPress onPress) {
        this(x, y, width, height, xTexStart, yTexStart, xDiff, yDiff, Identifier, 256, 256, onPress);
    }

    public ToggleButton(
            int x,
            int y,
            int width,
            int height,
            int xTexStart,
            int yTexStart,
            int xDiff,
            int yDiff,
            Identifier Identifier,
            int texWidth,
            int texHeight,
            OnPress onPress
    ) {
        super(x, y, width, height, xTexStart, yTexStart, yDiff, Identifier, texWidth, texHeight, onPress);
        this.xDiff = xDiff;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        int x = this.xTexStart;
        int y = this.yTexStart;
        if (!selected) {
            x += xDiff;
        }
        if (!this.isActive()) {
            y += yDiffTex * 2;
        } else if (this.isHoveredOrFocused()) {
            y += yDiffTex;
        }

        extractor.blit(
                RenderPipelines.GUI_TEXTURED,
                texture,
                getX(), getY(),
                (float) x, (float) y,
                getWidth(), getHeight(),
                getWidth(), getHeight(),
                texWidth, texHeight,
                -1
        );
    }

    @Override
    public void onPress(InputWithModifiers input) {
        selected = !selected;
        super.onPress(input);
    }
}
