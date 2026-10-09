package caeruleusTait.world.preview.client.gui.widgets;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class OldStyleImageButton extends Button {

    protected final int xTexStart;
    protected final int yTexStart;
    protected final int yDiffTex;
    protected final Identifier texture;
    protected final int texWidth;
    protected final int texHeight;

    public OldStyleImageButton(
            int x, int y, int width, int height,
            int xTexStart, int yTexStart, int yDiffTex,
            Identifier texture, int texWidth, int texHeight,
            OnPress onPress
    ) {
        super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        this.xTexStart = xTexStart;
        this.yTexStart = yTexStart;
        this.yDiffTex = yDiffTex;
        this.texture = texture;
        this.texWidth = texWidth;
        this.texHeight = texHeight;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        int x = this.xTexStart;
        int y = this.yTexStart;
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
}
