package caeruleusTait.world.preview.client;

import net.minecraft.network.chat.Component;

public class WorldPreviewComponents {
    // Main view components
    public static final Component TITLE = Component.translatable("world-preview-unofficial.preview.title");
    public static final Component TITLE_FULL = Component.translatable("world-preview-unofficial.preview.title-full");
    public static final Component SAVING_PREVIEW = Component.translatable("world-preview-unofficial.preview.saving");
    public static final Component LOADING_PREVIEW = Component.translatable("world-preview-unofficial.preview.loading");
    public static final Component SEED_FIELD = Component.translatable("world-preview-unofficial.preview.seed-field");
    public static final Component SEED_LABEL = Component.translatable("world-preview-unofficial.preview.seed-label");
    public static final Component BTN_RANDOM = Component.translatable("world-preview-unofficial.preview.btn-random");
    public static final Component BTN_SAVE_SEED = Component.translatable("world-preview-unofficial.preview.btn-save-seed");
    public static final Component BTN_SETTINGS = Component.translatable("world-preview-unofficial.preview.btn-settings");
    public static final Component BTN_CAVES = Component.translatable("world-preview-unofficial.preview.btn-caves");
    public static final Component BTN_HOME = Component.translatable("world-preview-unofficial.preview.btn-home");
    public static final Component BTN_SWITCH_STRUCT_DISABLED = Component.translatable("world-preview-unofficial.preview.btn-cycle.structures.disabled.tooltip");
    public static final Component BTN_TOGGLE_STRUCTURES = Component.translatable("world-preview-unofficial.preview.btn-toggle-structures");
    public static final Component BTN_TOGGLE_STRUCTURES_DISABLED = Component.translatable("world-preview-unofficial.preview.btn-toggle-structures.disabled");
    public static final Component BTN_RESET_STRUCTURES = Component.translatable("world-preview-unofficial.preview.btn-reset-structures");
    public static final Component BTN_RESET_STRUCTURES_TOOLTIP = Component.translatable("world-preview-unofficial.preview.btn-reset-structures.tooltip");
    public static final Component BTN_TOGGLE_BIOMES = Component.translatable("world-preview-unofficial.preview.btn-toggle-biomes");
    public static final Component BTN_TOGGLE_NOISE = Component.translatable("world-preview-unofficial.preview.btn-toggle-noise");
    public static final Component BTN_TOGGLE_NOISE_DISABLED = Component.translatable("world-preview-unofficial.preview.btn-toggle-noise.disabled");
    public static final Component BTN_TOGGLE_HEIGHTMAP = Component.translatable("world-preview-unofficial.preview.btn-toggle-heightmap");
    public static final Component BTN_TOGGLE_HEIGHTMAP_DISABLED = Component.translatable("world-preview-unofficial.preview.btn-toggle-heightmap.disabled");
    public static final Component BTN_TOGGLE_INTERSECT = Component.translatable("world-preview-unofficial.preview.btn-toggle-intersect");
    public static final Component BTN_TOGGLE_INTERSECT_DISABLED = Component.translatable("world-preview-unofficial.preview.btn-toggle-intersect.disabled");
    public static final Component BTN_TOGGLE_EXPAND =  Component.translatable("world-preview-unofficial.preview.btn-toggle-expand");
    public static final Component BTN_CYCLE_NOISE =  Component.translatable("world-preview-unofficial.preview.btn-cycle-noise");

    // Error message on setup
    public static final Component MSG_ERROR_SETUP_FAILED = Component.translatable("world-preview-unofficial.preview.error.setup-failed");
    public static final Component MSG_PREVIEW_SETUP_LOADING = Component.translatable("world-preview-unofficial.preview.msg.loading");

    // Settings
    public static final Component SETTINGS_TITLE = Component.translatable("world-preview-unofficial.settings.title");

    // - General settings
    public static final Component SETTINGS_GENERAL_TITLE = Component.translatable("world-preview-unofficial.settings.general.title");
    public static final Component SETTINGS_GENERAL_HEAD = Component.translatable("world-preview-unofficial.settings.general.head");
    public static final Component SETTINGS_GENERAL_THREADS = Component.translatable("world-preview-unofficial.settings.general.threads");
    public static final Component SETTINGS_GENERAL_THREADS_TOOLTIP = Component.translatable("world-preview-unofficial.settings.general.threads.tooltip");
    public static final Component SETTINGS_GENERAL_FC = Component.translatable("world-preview-unofficial.settings.general.full.chunk");
    public static final Component SETTINGS_GENERAL_STRUCT = Component.translatable("world-preview-unofficial.settings.general.struct");
    public static final Component SETTINGS_GENERAL_STRUCT_TOOLTIP = Component.translatable("world-preview-unofficial.settings.general.struct.tooltip");
    public static final Component SETTINGS_GENERAL_EXTRA_TRANSLATIONS = Component.translatable("world-preview-unofficial.settings.general.extra-translations");
    public static final Component SETTINGS_GENERAL_EXTRA_TRANSLATIONS_TOOLTIP = Component.translatable("world-preview-unofficial.settings.general.extra-translations.tooltip");
    public static final Component SETTINGS_GENERAL_EXTRA_TRANSLATIONS_LOCALE = Component.translatable("world-preview-unofficial.settings.general.extra-translations.locale");
    public static final Component SETTINGS_GENERAL_HEIGHTMAP = Component.translatable("world-preview-unofficial.settings.general.heightmap");
    public static final Component SETTINGS_GENERAL_HEIGHTMAP_TOOLTIP = Component.translatable("world-preview-unofficial.settings.general.heightmap.tooltip");
    public static final Component SETTINGS_GENERAL_INTERSECT = Component.translatable("world-preview-unofficial.settings.general.intersect");
    public static final Component SETTINGS_GENERAL_INTERSECT_TOOLTIP = Component.translatable("world-preview-unofficial.settings.general.intersect.tooltip");
    public static final Component SETTINGS_GENERAL_NOISE = Component.translatable("world-preview-unofficial.settings.general.noise");
    public static final Component SETTINGS_GENERAL_NOISE_TOOLTIP = Component.translatable("world-preview-unofficial.settings.general.noise.tooltip");
    public static final Component SETTINGS_GENERAL_FC_TOOLTIP = Component.translatable("world-preview-unofficial.settings.general.full.chunk.tooltip");
    public static final Component SETTINGS_GENERAL_BG = Component.translatable("world-preview-unofficial.settings.general.background");
    public static final Component SETTINGS_GENERAL_BG_TOOLTIP = Component.translatable("world-preview-unofficial.settings.general.background.tooltip");
    public static final Component SETTINGS_GENERAL_CONTROLS = Component.translatable("world-preview-unofficial.settings.general.controls");
    public static final Component SETTINGS_GENERAL_CONTROLS_TOOLTIP = Component.translatable("world-preview-unofficial.settings.general.controls.tooltip");
    public static final Component SETTINGS_GENERAL_FRAMETIME = Component.translatable("world-preview-unofficial.settings.general.frametime");
    public static final Component SETTINGS_GENERAL_FRAMETIME_TOOLTIP = Component.translatable("world-preview-unofficial.settings.general.frametime.tooltip");
    public static final Component SETTINGS_GENERAL_SHOW_IN_MENU = Component.translatable("world-preview-unofficial.settings.general.showinmenu");
    public static final Component SETTINGS_GENERAL_SHOW_IN_MENU_TOOLTIP = Component.translatable("world-preview-unofficial.settings.general.showinmenu.tooltip");
    public static final Component SETTINGS_GENERAL_SHOW_PLAYER = Component.translatable("world-preview-unofficial.settings.general.showplayer");
    public static final Component SETTINGS_GENERAL_SHOW_PLAYER_TOOLTIP = Component.translatable("world-preview-unofficial.settings.general.showplayer.tooltip");


    // - Sampling settings
    public static final Component SETTINGS_SAMPLE_TITLE = Component.translatable("world-preview-unofficial.settings.sample.title");
    public static final Component SETTINGS_SAMPLE_HEAD = Component.translatable("world-preview-unofficial.settings.sample.head");
    public static final Component SETTINGS_SAMPLE_PIXELS_TITLE_1 = Component.translatable("world-preview-unofficial.settings.sample.numChunk.title1");
    public static final Component SETTINGS_SAMPLE_PIXELS_TITLE_2 = Component.translatable("world-preview-unofficial.settings.sample.numChunk.title2");
    public static final Component SETTINGS_SAMPLE_SAMPLE_TITLE_1 = Component.translatable("world-preview-unofficial.settings.sample.sampler.title1");
    public static final Component SETTINGS_SAMPLE_SAMPLE_TITLE_2 = Component.translatable("world-preview-unofficial.settings.sample.sampler.title2");

    // - Caching settings
    public static final Component SETTINGS_CACHE_TITLE = Component.translatable("world-preview-unofficial.settings.cache.title");
    public static final Component SETTINGS_CACHE_DESC = Component.translatable("world-preview-unofficial.settings.cache.desc");
    public static final Component SETTINGS_CACHE_G_ENABLE = Component.translatable("world-preview-unofficial.settings.cache.game.enable");
    public static final Component SETTINGS_CACHE_N_ENABLE = Component.translatable("world-preview-unofficial.settings.cache.new.enable");
    public static final Component SETTINGS_CACHE_CLEAR = Component.translatable("world-preview-unofficial.settings.cache.clear");
    public static final Component SETTINGS_CACHE_CLEAR_TOOLTIP = Component.translatable("world-preview-unofficial.settings.cache.clear.tooltip");
    public static final Component SETTINGS_CACHE_COMPRESSION = Component.translatable("world-preview-unofficial.settings.cache.compression");
    public static final Component SETTINGS_CACHE_COMPRESSION_TOOLTIP = Component.translatable("world-preview-unofficial.settings.cache.compression.tooltip");

    // - Heightmap settings
    public static final Component SETTINGS_HEIGHTMAP_TITLE = Component.translatable("world-preview-unofficial.settings.heightmap.title");
    public static final Component SETTINGS_HEIGHTMAP_DISABLED = Component.translatable("world-preview-unofficial.settings.heightmap.disabled");
    public static final Component SETTINGS_HEIGHTMAP_PRESETS = Component.translatable("world-preview-unofficial.settings.heightmap.presets");
    public static final Component SETTINGS_HEIGHTMAP_COLORMAP = Component.translatable("world-preview-unofficial.settings.heightmap.colormap");
    public static final Component SETTINGS_HEIGHTMAP_MIN_Y = Component.translatable("world-preview-unofficial.settings.heightmap.minY");
    public static final Component SETTINGS_HEIGHTMAP_MAX_Y = Component.translatable("world-preview-unofficial.settings.heightmap.maxY");
    public static final Component SETTINGS_HEIGHTMAP_MIN_Y_TOOLTIP = Component.translatable("world-preview-unofficial.settings.heightmap.minY.tooltip");
    public static final Component SETTINGS_HEIGHTMAP_MAX_Y_TOOLTIP = Component.translatable("world-preview-unofficial.settings.heightmap.maxY.tooltip");
    public static final Component SETTINGS_HEIGHTMAP_VISUAL = Component.translatable("world-preview-unofficial.settings.heightmap.visual");
    public static final Component SETTINGS_HEIGHTMAP_VISUAL_TOOLTIP = Component.translatable("world-preview-unofficial.settings.heightmap.visual.tooltip");

    // - Dimensions settings
    public static final Component SETTINGS_DIM_TITLE = Component.translatable("world-preview-unofficial.settings.dimensions.title");
    public static final Component SETTINGS_DIM_HEAD = Component.translatable("world-preview-unofficial.settings.dimensions.head");

    // - Biome color chooser
    public static final Component SETTINGS_BIOMES_TITLE = Component.translatable("world-preview-unofficial.settings.biomes.title");

    public static final Component COLOR_HUE = Component.translatable("world-preview-unofficial.color.picker.hue");
    public static final Component COLOR_SAT = Component.translatable("world-preview-unofficial.color.picker.saturation");
    public static final Component COLOR_VAL = Component.translatable("world-preview-unofficial.color.picker.value");
    public static final Component COLOR_R = Component.translatable("world-preview-unofficial.color.picker.r");
    public static final Component COLOR_G = Component.translatable("world-preview-unofficial.color.picker.g");
    public static final Component COLOR_B = Component.translatable("world-preview-unofficial.color.picker.b");

    public static final Component COLOR_CAVE = Component.translatable("world-preview-unofficial.settings.biomes.cave");
    public static final Component COLOR_RESET = Component.translatable("world-preview-unofficial.settings.biomes.reset");
    public static final Component COLOR_APPLY = Component.translatable("world-preview-unofficial.settings.biomes.apply");
    public static final Component COLOR_LIST_FILTER = Component.translatable("world-preview-unofficial.settings.biomes.filter");



}
