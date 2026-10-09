package caeruleusTait.world.preview.backend.color;

import caeruleusTait.world.preview.WorldPreview;
import it.unimi.dsi.fastutil.objects.Object2ShortOpenHashMap;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

import static caeruleusTait.world.preview.WorldPreview.LOGGER;

public class PreviewMappingData {
    private final Map<String, ColorEntry> resourceOnlyColorMappingData = new HashMap<>();
    private final Map<String, ColorEntry> colorMappingData = new HashMap<>();

    private final Map<String, StructureEntry> structMappingData = new HashMap<>();

    private final List<PreviewData.HeightmapPresetData> heightmapPresets = new ArrayList<>();
    private final List<ColorMap> colorMaps = new ArrayList<>();

    private static final MessageDigest sha1;
    private static final com.google.gson.Gson GSON = new com.google.gson.Gson();

    static {
        try {
            sha1 = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    void clearBiomes() {
        colorMappingData.clear();
        resourceOnlyColorMappingData.clear();
    }

    void clearStructures() {
        structMappingData.clear();
    }

    void clearColorMappings() {
        colorMaps.clear();
    }

    void clearHeightmapPresets() {
        heightmapPresets.clear();
    }

    public void makeBiomeResourceOnlyBackup() {
        resourceOnlyColorMappingData.putAll(colorMappingData);
    }

    public void update(Map<Identifier, ColorEntry> newData) {
        colorMappingData.putAll(
                newData.entrySet()
                        .stream()
                        .collect(Collectors.toMap(x -> x.getKey().toString(), Map.Entry::getValue))
        );
    }

    public void updateStruct(Map<Identifier, StructureEntry> newData) {
        structMappingData.putAll(
                newData.entrySet()
                        .stream()
                        .collect(Collectors.toMap(x -> x.getKey().toString(), Map.Entry::getValue))
        );
    }

    public void addHeightmapPreset(PreviewData.HeightmapPresetData presetData) {
        heightmapPresets.add(presetData);
    }

    public List<PreviewData.HeightmapPresetData> heightmapPresets() {
        return heightmapPresets;
    }

    public void loadHeightmapPresetsFromClasspath() {
        if (!heightmapPresets.isEmpty()) {
            return;
        }
        String[] presetFiles = {"end", "overworld"};
        LOGGER.info("Loading heightmap presets from classpath (fallback)");
        for (String name : presetFiles) {
            String path = "/data/world-preview-unofficial/heightmap_preview_presets/" + name + ".json";
            try (InputStream is = PreviewMappingData.class.getResourceAsStream(path)) {
                if (is == null) {
                    LOGGER.warn("Heightmap preset resource not found: {}", path);
                    continue;
                }
                try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    Identifier id = Identifier.fromNamespaceAndPath("world-preview-unofficial", name);
                    PreviewData.HeightmapPresetData value = GSON.fromJson(reader, PreviewData.HeightmapPresetData.class);
                    LOGGER.info("Loaded heightmap preset from classpath: {} ({} to {})", id, value.name(), value.minY(), value.maxY());
                    heightmapPresets.add(value);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load heightmap preset from classpath: {}", path, e);
            }
        }
    }

    public void addColormap(ColorMap colorMap) {
        colorMaps.add(colorMap);
    }

    public PreviewData generateMapData(
            Set<Identifier> biomesSet,
            Set<Identifier> caveBiomesSet,
            Set<Identifier> structuresSet,
            Set<Identifier> displayByDefaultStructuresSet
    ) {
        List<String> biomes = biomesSet.stream().map(Identifier::toString).sorted().toList();
        List<String> structures = structuresSet.stream().map(Identifier::toString).sorted().toList();

        final PreviewData res = new PreviewData(
                new PreviewData.BiomeData[biomes.size()],
                new PreviewData.StructureData[structures.size()],
                new Object2ShortOpenHashMap<>(),
                new Object2ShortOpenHashMap<>(),
                heightmapPresets,
                colorMaps.stream().collect(Collectors.toMap(x -> x.key().toString(), x -> x))
        );

        for (short id = 0; id < biomes.size(); ++id) {
            final String biome = biomes.get(id);
            res.biome2Id().put(biome, id);

            ColorEntry color = colorMappingData.get(biome);
            if (color == null) {
                color = new ColorEntry();
                color.dataSource = PreviewData.DataSource.MISSING;
                byte[] hash = sha1.digest(biome.getBytes(StandardCharsets.UTF_8));
                ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.BYTES);
                for (int i = 0; i < Integer.BYTES && i < hash.length; ++i) {
                    byteBuffer.put(hash[i]);
                }
                color.color = byteBuffer.getInt(0) & 0xFFFFFF;
                color.name = null;
            }

            ColorEntry resourceOnlyColor = resourceOnlyColorMappingData.get(biome);
            if (resourceOnlyColor == null) {
                resourceOnlyColor = color;
            }

            Identifier biomeRes = Identifier.parse(biome);
            res.biomeId2BiomeData()[id] = new PreviewData.BiomeData(
                    id,
                    biomeRes,
                    color.color,
                    resourceOnlyColor.color,
                    color.cave.orElse(caveBiomesSet.contains(biomeRes)),
                    resourceOnlyColor.cave.orElse(caveBiomesSet.contains(biomeRes)),
                    color.name,
                    resourceOnlyColor.name,
                    color.dataSource
            );
        }

        for (short id = 0; id < structures.size(); ++id) {
            final String structTag = structures.get(id);
            res.struct2Id().put(structTag, id);

            StructureEntry structure = structMappingData.get(structTag);
            if (structure == null) {
                structure = new StructureEntry();
                structure.dataSource = PreviewData.DataSource.MISSING;
                structure.texture = "world-preview-unofficial:textures/structure/unknown.png";
                structure.name = structTag;
                structure.showByDefault = Optional.empty();
            }

            Identifier structureRes = Identifier.parse(structTag);
            res.structId2StructData()[id] = new PreviewData.StructureData(
                    id,
                    structureRes,
                    structure.name,
                    structure.texture == null ? null : Identifier.parse(structure.texture),
                    structure.item == null ? null : Identifier.parse(structure.item),
                    structure.showByDefault.orElse(displayByDefaultStructuresSet.contains(structureRes)),
                    structure.dataSource
            );
        }

        return res;
    }

    public static class ColorEntry {
        public PreviewData.DataSource dataSource;
        public int color;
        public Optional<Boolean> cave = Optional.empty();
        public String name = null;

        public ColorEntry() {
        }

        public ColorEntry(PreviewData.DataSource dataSource, int color, boolean cave, String name) {
            this.dataSource = dataSource;
            this.color = color;
            this.cave = Optional.of(cave);
            this.name = name;
        }
    }

    public static class StructureEntry {
        public PreviewData.DataSource dataSource;
        public String name = null;
        public String texture = null;
        public String item = null;
        public Optional<Boolean> showByDefault = Optional.empty();
    }
}
