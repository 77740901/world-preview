package caeruleusTait.world.preview.backend.color;

import caeruleusTait.world.preview.WorldPreview;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import static caeruleusTait.world.preview.WorldPreview.LOGGER;

public class HeightmapPresetReloadListener extends SimplePreparableReloadListener<Map<Identifier, JsonElement>> {
    private static final Gson GSON = (new GsonBuilder()).create();
    private static final String DIRECTORY = "heightmap_preview_presets";

    @Override
    protected Map<Identifier, JsonElement> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Identifier, JsonElement> result = new HashMap<>();
        var resources = resourceManager.listResources(DIRECTORY, id -> id.getPath().endsWith(".json"));
        LOGGER.info("HeightmapPresetReloadListener.prepare: found {} resources in '{}'", resources.size(), DIRECTORY);
        for (var entry : resources.entrySet()) {
            Identifier id = entry.getKey();
            String path = id.getPath();
            Identifier trimmedId = Identifier.fromNamespaceAndPath(id.getNamespace(), path.substring(DIRECTORY.length() + 1, path.length() - 5));
            try (Reader reader = entry.getValue().openAsReader()) {
                result.put(trimmedId, GSON.fromJson(reader, JsonElement.class));
            } catch (IOException e) {
                LOGGER.error("Failed to load heightmap preset {}", trimmedId, e);
            }
        }
        return result;
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        final WorldPreview worldPreview = WorldPreview.get();
        final PreviewMappingData previewMappingData = worldPreview.biomeColorMap();

        LOGGER.info("HeightmapPresetReloadListener.apply: {} presets from resource manager", object.size());
        if (object.isEmpty()) {
            LOGGER.info("No heightmap presets from resource manager, classpath fallback will handle loading");
            return;
        }

        previewMappingData.clearHeightmapPresets();
        object.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    final PreviewData.HeightmapPresetData value = GSON.fromJson(entry.getValue(), PreviewData.HeightmapPresetData.class);
                    LOGGER.debug(" - {}: {} | {} to {}", entry.getKey(), value.name(), value.minY(), value.maxY());
                    previewMappingData.addHeightmapPreset(value);
                });
    }
}
