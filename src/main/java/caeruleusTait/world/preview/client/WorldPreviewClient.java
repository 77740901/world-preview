package caeruleusTait.world.preview.client;

import caeruleusTait.world.preview.mixin.client.GameRendererAccessor;
import caeruleusTait.world.preview.mixin.client.GuiRendererInvoker;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

public class WorldPreviewClient implements ClientModInitializer {

    public static final Identifier DISPLAY_TEXTURE_ID = Identifier.fromNamespaceAndPath("world-preview-unofficial", "display");
    private static boolean textureRegistered = false;

    @Override
    public void onInitializeClient() {
    }

    public static void registerDisplayTexture(DynamicTexture texture) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getTextureManager() == null) {
            return;
        }
        if (textureRegistered) {
            mc.getTextureManager().release(DISPLAY_TEXTURE_ID);
        }
        mc.getTextureManager().register(DISPLAY_TEXTURE_ID, texture);
        textureRegistered = true;
    }

    /**
     * Force the client's GUI item atlas to be rebuilt on the next render.
     * <p>
     * GUI item icons are rendered once into a client-wide cached atlas ({@code GuiItemAtlas}) and then
     * blitted from it on every frame; a static item's slot stays marked "ready" and is never redrawn.
     * In a heavily modded client that shared atlas can be re-laid-out or cleared by other screens
     * (e.g. the inventory / REI with many items), after which the structure's item slot still reports
     * "ready" but its pixels are gone - the icon silently disappears with no error.
     * <p>
     * Invalidating the atlas while the structure list is being extracted makes the renderer recreate
     * it and draw the item fresh, reproducing the conditions of the (working) first frame on every frame.
     */
    public static void invalidateItemAtlas() {
        try {
            final Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.gameRenderer == null) {
                return;
            }
            final GuiRenderer guiRenderer = ((GameRendererAccessor) mc.gameRenderer).worldPreview$getGuiRenderer();
            if (guiRenderer != null) {
                ((GuiRendererInvoker) guiRenderer).worldPreview$invalidateItemAtlas();
            }
        } catch (Throwable ignored) {
            // Never let atlas housekeeping break the GUI; the texture underlay still guarantees an icon.
        }
    }

    private static RenderPipeline borderPipeline = null;

    /**
     * Pipeline for the preview border, configured identically to vanilla {@link RenderPipelines#GUI}
     * (core/gui shaders, POSITION_COLOR, translucent quads) but built lazily after the vanilla pipelines.
     * <p>
     * The GUI draw order is determined by a monotonic per-pipeline sort key; a pipeline built later sorts
     * above GUI_TEXTURED icon draws, which is exactly what keeps the border on top of structure icons.
     */
    public static RenderPipeline borderPipeline() {
        if (borderPipeline == null) {
            borderPipeline = RenderPipeline.builder()
                    .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
                    .withLocation("pipeline/world_preview_gui_border")
                    .withVertexShader("core/gui")
                    .withFragmentShader("core/gui")
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
                    .withPrimitiveTopology(PrimitiveTopology.QUADS)
                    .build();
        }
        return borderPipeline;
    }

    public static void renderTexture(GuiGraphicsExtractor extractor, int xMin, int yMin, int xMax, int yMax, int texWidth, int texHeight) {
        extractor.blit(
                DISPLAY_TEXTURE_ID,
                xMin, yMin, xMax, yMax,
                0.0F, 1.0F, 0.0F, 1.0F
        );
    }

    public static void renderTexture(GuiGraphicsExtractor extractor, AbstractTexture texture, int xMin, int yMin, int xMax, int yMax) {
        extractor.blit(texture.getTextureView(), texture.getSampler(), xMin, yMin, xMax - xMin, yMax - yMin, 0.0F, 0.0F, 1.0F, 1.0F);
    }

    public static String toTitleCase(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        return Arrays
                .stream(input.split(" "))
                .map(StringUtils::capitalize)
                .collect(Collectors.joining(" "));
    }
}
