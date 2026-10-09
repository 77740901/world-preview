package caeruleusTait.world.preview.backend.color;

import caeruleusTait.world.preview.WorldPreview;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import static caeruleusTait.world.preview.WorldPreview.LOGGER;

public class ColormapReloadListener extends SimplePreparableReloadListener<Map<Identifier, JsonElement>> {
    private static final Gson GSON = (new GsonBuilder()).create();
    private static final String DIRECTORY = "colormap_preview";

    @Override
    protected Map<Identifier, JsonElement> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Identifier, JsonElement> result = new HashMap<>();
        for (var entry : resourceManager.listResources(DIRECTORY, id -> id.getPath().endsWith(".json")).entrySet()) {
            Identifier id = entry.getKey();
            String path = id.getPath();
            Identifier trimmedId = Identifier.fromNamespaceAndPath(id.getNamespace(), path.substring(DIRECTORY.length() + 1, path.length() - 5));
            try (Reader reader = entry.getValue().openAsReader()) {
                result.put(trimmedId, GSON.fromJson(reader, JsonElement.class));
            } catch (IOException e) {
                LOGGER.error("Failed to load colormap {}", trimmedId, e);
            }
        }
        return result;
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        final WorldPreview worldPreview = WorldPreview.get();
        final PreviewMappingData previewMappingData = worldPreview.biomeColorMap();
        previewMappingData.clearColorMappings();

        LOGGER.debug("Loading colormaps:");
        for (Map.Entry<Identifier, JsonElement> entry : object.entrySet()) {
            final ColorMap.RawColorMap value = GSON.fromJson(entry.getValue(), ColorMap.RawColorMap.class);
            LOGGER.debug(" - {}: {} | {} entries", entry.getKey(), value.name(), value.data().size());
            previewMappingData.addColormap(new ColorMap(entry.getKey(), value));
        }
    }
}
