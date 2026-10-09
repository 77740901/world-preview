package caeruleusTait.world.preview.mixin.client;

import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiRenderer.class)
public interface GuiRendererInvoker {

    @Invoker("invalidateItemAtlas")
    void worldPreview$invalidateItemAtlas();

}
