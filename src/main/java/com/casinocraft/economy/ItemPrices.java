package com.casinocraft.economy;

import com.casinocraft.CasinoCraftMod;
import com.casinocraft.ModItems;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.SmithingTransformRecipe;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.world.World;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Цены кассы и магазина: config/casinocraft-prices.json.
 * <p>
 * sell — по какой цене касса покупает предметы у игрока (ключ с # — тег);
 * shop — что и за сколько можно купить. В магазине состоит практически весь
 * реестр предметов: цена берётся из конфига, из цены скупки или считается
 * по рецептам крафта (как в моде Upgrader).
 */
public final class ItemPrices {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Map<Identifier, Long> SELL_ITEMS = new LinkedHashMap<>();
    private static final Map<Identifier, Long> SELL_TAGS = new LinkedHashMap<>();
    private static final Map<Item, Long> SELL_CACHE = new HashMap<>();

    /** Явные цены магазина (конфиг + значения по умолчанию для важных предметов). */
    private static final Map<Item, Long> SHOP_PRICES = new HashMap<>();
    /** Все предметы, которые можно купить. */
    private static final List<Item> CATALOG = new ArrayList<>();
    /** Цены скупки после защиты от крафт-арбитража. */
    private static final Map<Item, Long> SAFE_SELL = new HashMap<>();
    private static boolean sellReady;
    /** Готовый снапшот цен для открытых окон казино; сбрасывается при перезагрузке. */
    private static volatile PriceSnapshot snapshotCache;

    /** Кэш цен, посчитанных по рецептам. */
    private static final Map<Item, Double> DERIVED = new HashMap<>();
    private static final Map<Item, List<PricedRecipe>> RECIPES = new HashMap<>();
    private static RecipeManager recipeSource;

    /** Минимальная наценка магазина над скупкой. */
    private static final double MIN_MARKUP = 1.25;

    private static final Map<String, Long> DEFAULT_SHOP = new LinkedHashMap<>();
    /** Резервные цены скупки для предметов без рецепта (смитинг, лут). */
    private static final Map<String, Long> DEFAULT_SELL = new LinkedHashMap<>();
    private static final Set<Item> BLOCKED = new HashSet<>();
    private static final List<CookingEntry> COOKING = new ArrayList<>();
    private static Path configFile;

    static {
        DEFAULT_SHOP.put("minecraft:emerald", 210L);
        DEFAULT_SHOP.put("minecraft:diamond", 600L);
        DEFAULT_SHOP.put("minecraft:gold_ingot", 90L);
        DEFAULT_SHOP.put("minecraft:iron_ingot", 60L);
        DEFAULT_SHOP.put("minecraft:golden_apple", 450L);
        DEFAULT_SHOP.put("minecraft:ender_pearl", 75L);
        DEFAULT_SHOP.put("minecraft:blaze_rod", 75L);
        DEFAULT_SHOP.put("minecraft:obsidian", 3L);
        DEFAULT_SHOP.put("minecraft:shulker_shell", 180L);
        DEFAULT_SHOP.put("minecraft:netherite_scrap", 1800L);
        DEFAULT_SHOP.put("minecraft:name_tag", 900L);
        DEFAULT_SHOP.put("minecraft:saddle", 900L);
        // Смитинг-предметы: рецепт в 1.20.1 не отдаёт ингредиенты, поэтому цену задаём явно.
        DEFAULT_SHOP.put("minecraft:netherite_sword", 12000L);
        DEFAULT_SHOP.put("minecraft:netherite_pickaxe", 13500L);
        DEFAULT_SHOP.put("minecraft:netherite_axe", 13000L);
        DEFAULT_SHOP.put("minecraft:netherite_shovel", 11500L);
        DEFAULT_SHOP.put("minecraft:netherite_hoe", 11500L);
        DEFAULT_SHOP.put("minecraft:netherite_helmet", 15000L);
        DEFAULT_SHOP.put("minecraft:netherite_chestplate", 18000L);
        DEFAULT_SHOP.put("minecraft:netherite_leggings", 17000L);
        DEFAULT_SHOP.put("minecraft:netherite_boots", 14000L);
        DEFAULT_SHOP.put("minecraft:experience_bottle", 40L);
        DEFAULT_SHOP.put("minecraft:crying_obsidian", 30L);
        DEFAULT_SHOP.put("minecraft:dragon_breath", 800L);
        DEFAULT_SHOP.put("minecraft:iron_horse_armor", 500L);
        DEFAULT_SHOP.put("minecraft:golden_horse_armor", 800L);
        DEFAULT_SHOP.put("minecraft:diamond_horse_armor", 2500L);

        // Цены скупки для того, что не выводится из рецептов 1.20.1.
        DEFAULT_SELL.put("minecraft:netherite_sword", 9000L);
        DEFAULT_SELL.put("minecraft:netherite_pickaxe", 10000L);
        DEFAULT_SELL.put("minecraft:netherite_axe", 9500L);
        DEFAULT_SELL.put("minecraft:netherite_shovel", 8500L);
        DEFAULT_SELL.put("minecraft:netherite_hoe", 8500L);
        DEFAULT_SELL.put("minecraft:netherite_helmet", 11000L);
        DEFAULT_SELL.put("minecraft:netherite_chestplate", 13000L);
        DEFAULT_SELL.put("minecraft:netherite_leggings", 12000L);
        DEFAULT_SELL.put("minecraft:netherite_boots", 10000L);
        DEFAULT_SELL.put("minecraft:iron_horse_armor", 400L);
        DEFAULT_SELL.put("minecraft:golden_horse_armor", 600L);
        DEFAULT_SELL.put("minecraft:diamond_horse_armor", 2000L);
        DEFAULT_SELL.put("minecraft:chainmail_helmet", 200L);
        DEFAULT_SELL.put("minecraft:chainmail_chestplate", 240L);
        DEFAULT_SELL.put("minecraft:chainmail_leggings", 220L);
        DEFAULT_SELL.put("minecraft:chainmail_boots", 180L);

        Collections.addAll(BLOCKED,
                Items.AIR, Items.BEDROCK, Items.BARRIER, Items.LIGHT, Items.STRUCTURE_BLOCK,
                Items.STRUCTURE_VOID, Items.JIGSAW, Items.COMMAND_BLOCK, Items.CHAIN_COMMAND_BLOCK,
                Items.REPEATING_COMMAND_BLOCK, Items.COMMAND_BLOCK_MINECART, Items.DEBUG_STICK,
                Items.KNOWLEDGE_BOOK, Items.SPAWNER, Items.BUDDING_AMETHYST, Items.END_PORTAL_FRAME,
                Items.REINFORCED_DEEPSLATE, Items.PETRIFIED_OAK_SLAB, Items.FARMLAND, Items.DIRT_PATH,
                Items.INFESTED_STONE, Items.INFESTED_COBBLESTONE, Items.INFESTED_STONE_BRICKS,
                Items.INFESTED_MOSSY_STONE_BRICKS, Items.INFESTED_CRACKED_STONE_BRICKS,
                Items.INFESTED_CHISELED_STONE_BRICKS, Items.INFESTED_DEEPSLATE,
                Items.SUSPICIOUS_SAND, Items.SUSPICIOUS_GRAVEL,
                Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION, Items.TIPPED_ARROW,
                Items.ENCHANTED_BOOK, Items.WRITTEN_BOOK, Items.SUSPICIOUS_STEW, Items.FILLED_MAP,
                Items.GOAT_HORN,
                // блоки, из которых добыча/переработка даёт больше, чем стоит блок
                Items.COBWEB, Items.AMETHYST_CLUSTER, Items.GILDED_BLACKSTONE, Items.WET_SPONGE);
    }

    private ItemPrices() {
    }

    public static void load() {
        invalidateCache();
        configFile = FabricLoader.getInstance().getConfigDir().resolve("casinocraft-prices.json");
        try {
            if (!Files.exists(configFile)) {
                writeDefaults();
            }
            SHOP_PRICES.clear();
            for (Map.Entry<String, Long> entry : DEFAULT_SHOP.entrySet()) {
                resolveShopEntry(entry.getKey(), entry.getValue());
            }
            readConfig();
            DERIVED.clear();
            buildCatalog();
            CasinoCraftMod.LOGGER.info("Цены: {} предметов, {} тегов, {} товаров в магазине",
                    SELL_ITEMS.size(), SELL_TAGS.size(), CATALOG.size());
        } catch (Exception exception) {
            CasinoCraftMod.LOGGER.error("Не удалось прочитать цены, касса и магазин будут пустыми", exception);
        }
    }

    public static void invalidateCache() {
        SELL_CACHE.clear();
        DERIVED.clear();
        RECIPES.clear();
        SAFE_SELL.clear();
        sellReady = false;
        snapshotCache = null;
        recipeSource = null;
    }

    private static void writeDefaults() throws IOException {
        Files.createDirectories(configFile.getParent());
        JsonObject root = new JsonObject();

        JsonObject sell = new JsonObject();
        for (Map.Entry<String, Long> entry : defaultSellPrices().entrySet()) {
            sell.addProperty(entry.getKey(), entry.getValue());
        }
        root.add("sell", sell);

        JsonObject shop = new JsonObject();
        for (Map.Entry<String, Long> entry : DEFAULT_SHOP.entrySet()) {
            shop.addProperty(entry.getKey(), entry.getValue());
        }
        root.add("shop", shop);

        try (Writer writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        }
    }

    private static void readConfig() throws IOException {
        SELL_ITEMS.clear();
        SELL_TAGS.clear();
        SELL_CACHE.clear();

        try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            boolean sectioned = root.has("sell") || root.has("shop");

            JsonObject sellObject = sectioned && root.has("sell") ? root.getAsJsonObject("sell") : root;
            readSellPrices(sellObject);

            if (sectioned && root.has("shop")) {
                for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("shop").entrySet()) {
                    long price = entry.getValue().getAsLong();
                    if (price > 0L) {
                        resolveShopEntry(entry.getKey(), price);
                    }
                }
            }
        }
    }

    private static void readSellPrices(JsonObject object) {
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String key = entry.getKey();
            long value = Math.max(0L, entry.getValue().getAsLong());
            if (key.startsWith("#")) {
                Identifier tagId = Identifier.tryParse(key.substring(1));
                if (tagId != null) {
                    SELL_TAGS.put(tagId, value);
                }
            } else {
                Identifier itemId = Identifier.tryParse(key);
                if (itemId != null) {
                    SELL_ITEMS.put(itemId, value);
                }
            }
        }
    }

    private static void resolveShopEntry(String rawId, long price) {
        Identifier id = Identifier.tryParse(rawId);
        if (id == null) {
            return;
        }
        Item item = Registries.ITEM.get(id);
        if (item == Items.AIR) {
            return;
        }
        SHOP_PRICES.put(item, price);
    }

    private static void buildCatalog() {
        CATALOG.clear();
        for (Item item : Registries.ITEM) {
            if (isShopAllowed(item)) {
                CATALOG.add(item);
            }
        }
    }

    private static boolean isShopAllowed(Item item) {
        if (item == Items.AIR || BLOCKED.contains(item) || item instanceof SpawnEggItem) {
            return false;
        }
        Identifier id = Registries.ITEM.getId(item);
        if (id == null || CasinoCraftMod.MOD_ID.equals(id.getNamespace())) {
            return false;
        }
        // Руды и блоки-«конвертеры» нельзя покупать: из них добычей/плавкой
        // получаются ресурсы дороже их расчётной цены (алмазная руда -> алмаз и т.п.).
        return !id.getPath().endsWith("_ore");
    }

    /** Можно ли купить предмет в магазине (проверяется и на сервере). */
    public static boolean isBuyable(Item item) {
        return isShopAllowed(item);
    }

    // ------------------------------------------------------------- скупка

    public static long price(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0L;
        }
        return price(stack.getItem());
    }

    public static long price(Item item) {
        if (sellReady) {
            return SAFE_SELL.getOrDefault(item, 0L);
        }
        return SELL_CACHE.computeIfAbsent(item, ItemPrices::resolveSell);
    }

    public static long totalPrice(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0L;
        }
        return PriceSnapshot.totalPrice(price(stack.getItem()), stack.getCount(),
                stack.getDamage(), stack.isDamageable() ? stack.getMaxDamage() : 0);
    }

    public static PriceSnapshot snapshot(World world) {
        PriceSnapshot cached = snapshotCache;
        if (cached != null) {
            return cached;
        }
        ensureRecipes(world);
        Map<String, PriceSnapshot.Entry> entries = new LinkedHashMap<>();
        for (Item item : Registries.ITEM) {
            long sell = price(item);
            long shop = isBuyable(item) ? shopPrice(item) : 0L;
            if (sell > 0L || shop > 0L) {
                entries.put(Registries.ITEM.getId(item).toString(), new PriceSnapshot.Entry(sell, shop));
            }
        }
        PriceSnapshot result = new PriceSnapshot(entries);
        snapshotCache = result;
        return result;
    }

    private static long resolveSell(Item item) {
        // Фишки — фиксированный номинал, он же цена приёма в кассу.
        if (item == ModItems.CHIP_100) {
            return 100L;
        }
        if (item == ModItems.CHIP_10) {
            return 10L;
        }
        Long direct = SELL_ITEMS.get(Registries.ITEM.getId(item));
        if (direct != null) {
            return direct;
        }
        Long fallback = DEFAULT_SELL.get(Registries.ITEM.getId(item).toString());
        if (fallback != null) {
            return fallback;
        }
        ItemStack probe = new ItemStack(item);
        for (Map.Entry<Identifier, Long> entry : SELL_TAGS.entrySet()) {
            TagKey<Item> tag = TagKey.of(RegistryKeys.ITEM, entry.getKey());
            if (probe.isIn(tag)) {
                return entry.getValue();
            }
        }
        return 0L;
    }

    // ------------------------------------------------------------- магазин

    public static List<Item> shopItems() {
        return CATALOG;
    }

    /**
     * Цена покупки: явная из конфига, иначе наценка над скупкой,
     * иначе стоимость крафта по рецептам, иначе запасная цена по редкости.
     */
    public static long shopPrice(Item item) {
        Long configured = SHOP_PRICES.get(item);
        long sell = price(item);
        long minimum = sell > 0L ? (long) Math.ceil(sell * MIN_MARKUP) : 0L;
        if (configured != null) {
            return Math.max(configured, minimum);
        }
        if (sell > 0L) {
            return minimum;
        }
        return Math.max(1L, (long) Math.ceil(derivedValue(item) * MIN_MARKUP));
    }

    private static void addOptions(List<List<Item>> groups, Ingredient ingredient) {
        List<Item> options = new ArrayList<>();
        for (ItemStack stack : ingredient.getMatchingStacks()) {
            if (!stack.isEmpty() && !options.contains(stack.getItem())) {
                options.add(stack.getItem());
            }
        }
        if (!options.isEmpty()) {
            groups.add(options);
        }
    }

    /**
     * Строит индекс рецептов и запасные цены. Вызывается на сервере при старте
     * и при изменении серверных рецептов. Клиент получает готовый снимок цен.
     */
    public static synchronized void ensureRecipes(World world) {
        if (world == null) {
            return;
        }
        RecipeManager manager = world.getRecipeManager();
        if (manager == recipeSource && !RECIPES.isEmpty()) {
            return;
        }
        recipeSource = manager;
        sellReady = false;
        SAFE_SELL.clear();
        SELL_CACHE.clear();
        snapshotCache = null;
        RECIPES.clear();
        COOKING.clear();
        DERIVED.clear();

        DynamicRegistryManager registries = world.getRegistryManager();
        for (Recipe<?> recipe : manager.values()) {
            ItemStack result;
            try {
                result = recipe.getOutput(registries);
            } catch (Exception ignored) {
                continue;
            }
            if (result == null || result.isEmpty() || result.getCount() <= 0) {
                continue;
            }
            List<Ingredient> ingredients = recipe.getIngredients();
            List<List<Item>> groups = new ArrayList<>();
            if (recipe instanceof SmithingTransformRecipe smithing) {
                // В 1.20.1 смитинг не отдаёт ингредиенты через getIngredients().
                addOptions(groups, smithing.template);
                addOptions(groups, smithing.base);
                addOptions(groups, smithing.addition);
            } else {
                if (ingredients.isEmpty()) {
                    continue;
                }
                for (Ingredient ingredient : ingredients) {
                    addOptions(groups, ingredient);
                }
            }
            if (!groups.isEmpty()) {
                RECIPES.computeIfAbsent(result.getItem(), key -> new ArrayList<>())
                        .add(new PricedRecipe(groups, result.getCount()));
                if (groups.size() == 1 && result.getCount() == 1 && recipe instanceof AbstractCookingRecipe) {
                    COOKING.add(new CookingEntry(groups.get(0), result.getItem()));
                }
            }
        }
        int entries = 0;
        for (List<PricedRecipe> list : RECIPES.values()) {
            entries += list.size();
        }
        buildSafeSell();
        DERIVED.clear();
        CasinoCraftMod.LOGGER.info("Индекс рецептов: {} предметов, {} рецептов", RECIPES.size(), entries);
    }

    /**
     * Защита скупки от крафт-арбитража: цена результата не может быть больше
     * суммарной цены ингредиентов (делимой на количество). Иначе игрок покупал бы
     * брёвна за бесценок и сдавал доски дороже — печатный станок.
     * Рецепты, где у ингредиентов нет цены вообще, не учитываются.
     */
    private static void buildSafeSell() {
        SAFE_SELL.clear();
        for (Item item : Registries.ITEM) {
            if (!isSellable(item)) {
                continue;
            }
            long value = resolveSell(item);
            if (value <= 0L) {
                value = Math.max(1L, Math.round(derivedValue(item)));
            }
            if (value > 0L) {
                SAFE_SELL.put(item, value);
            }
        }

        // Плавка: сырьё не может стоить дешевле результата (алмазная руда -> алмаз).
        for (CookingEntry entry : COOKING) {
            long resultValue = resolveSell(entry.result);
            if (resultValue <= 0L) {
                continue;
            }
            for (Item input : entry.inputs) {
                if (!isSellable(input)) {
                    continue;
                }
                if (SAFE_SELL.getOrDefault(input, 0L) < resultValue) {
                    SAFE_SELL.put(input, resultValue);
                }
            }
        }

        int adjusted = 0;
        for (int pass = 0; pass < 16; pass++) {
            boolean changed = false;
            for (Map.Entry<Item, List<PricedRecipe>> entry : RECIPES.entrySet()) {
                Long current = SAFE_SELL.get(entry.getKey());
                if (current == null) {
                    continue;
                }
                long best = Long.MAX_VALUE;
                for (PricedRecipe recipe : entry.getValue()) {
                    long cost = 0L;
                    for (List<Item> group : recipe.groups) {
                        long cheapest = Long.MAX_VALUE;
                        for (Item option : group) {
                            // Непродаваемый ингредиент считаем бесплатным: иначе
                            // из палок (0 монет) получались бы платные мечи.
                            cheapest = Math.min(cheapest, SAFE_SELL.getOrDefault(option, 0L));
                        }
                        cost += cheapest == Long.MAX_VALUE ? 0L : cheapest;
                    }
                    if (cost > 0L && recipe.count > 0) {
                        best = Math.min(best, cost / recipe.count);
                    }
                }
                if (best != Long.MAX_VALUE && current > best) {
                    if (best <= 0L) {
                        SAFE_SELL.remove(entry.getKey());
                    } else {
                        SAFE_SELL.put(entry.getKey(), best);
                    }
                    adjusted++;
                    changed = true;
                }
            }
            if (!changed) {
                break;
            }
        }

        sellReady = true;
        if (adjusted > 0) {
            CasinoCraftMod.LOGGER.info("Скорректировано {} цен от крафт-арбитража", adjusted);
        }
    }

    /** Что вообще имеет смысл принимать в кассу. */
    private static boolean isSellable(Item item) {
        if (item == Items.AIR || BLOCKED.contains(item) || item instanceof SpawnEggItem) {
            return false;
        }
        if (item == ModItems.CASINO_CHIP) {
            return false;
        }
        Identifier id = Registries.ITEM.getId(item);
        if (id == null) {
            return false;
        }
        if (CasinoCraftMod.MOD_ID.equals(id.getNamespace())) {
            return item == ModItems.CHIP_100 || item == ModItems.CHIP_10;
        }
        return true;
    }

    private static double derivedValue(Item item) {
        return derivedValue(item, new HashSet<>());
    }

    private static double derivedValue(Item item, Set<Item> visiting) {
        long sell = price(item);
        if (sell > 0L) {
            return sell;
        }
        Double cached = DERIVED.get(item);
        if (cached != null) {
            return cached;
        }
        if (!visiting.add(item)) {
            return rarityValue(item);
        }

        double best = Double.MAX_VALUE;
        List<PricedRecipe> recipes = RECIPES.get(item);
        if (recipes != null) {
            for (PricedRecipe recipe : recipes) {
                double cost = 0.0;
                for (List<Item> group : recipe.groups) {
                    double cheapest = Double.MAX_VALUE;
                    for (Item option : group) {
                        cheapest = Math.min(cheapest, derivedValue(option, visiting));
                    }
                    cost += cheapest;
                }
                if (recipe.count > 0) {
                    best = Math.min(best, cost / recipe.count);
                }
            }
        }

        visiting.remove(item);
        double value = best == Double.MAX_VALUE ? rarityValue(item) : Math.max(1.0, best);
        DERIVED.put(item, value);
        return value;
    }

    private static double rarityValue(Item item) {
        Rarity rarity = new ItemStack(item).getRarity();
        return switch (rarity) {
            case COMMON -> 3.0;
            case UNCOMMON -> 150.0;
            case RARE -> 600.0;
            case EPIC -> 2500.0;
        };
    }

    private static final class PricedRecipe {
        private final List<List<Item>> groups;
        private final int count;

        private PricedRecipe(List<List<Item>> groups, int count) {
            this.groups = groups;
            this.count = count;
        }
    }

    private static final class CookingEntry {
        private final List<Item> inputs;
        private final Item result;

        private CookingEntry(List<Item> inputs, Item result) {
            this.inputs = inputs;
            this.result = result;
        }
    }

    // ------------------------------------------------------------- значения по умолчанию

    /**
     * Цены взяты из мода Upgrader 1.3.1 (таблица BaseValues: сырьё, слитки, лут, редкости).
     * Ниже добавлены производные значения для блоков и предметов, которые Upgrader
     * считает по рецептам. Все значения — целые монеты.
     */
    private static Map<String, Long> defaultSellPrices() {
        Map<String, Long> prices = new LinkedHashMap<>();

        // ---- базовая таблица Upgrader ----
        putAll(prices, 1L,
                "dirt", "cobblestone", "cobbled_deepslate", "sand", "red_sand", "gravel",
                "netherrack", "end_stone", "granite", "diorite", "andesite", "tuff",
                "calcite", "basalt", "blackstone", "snowball", "wheat_seeds", "kelp");
        putAll(prices, 2L,
                "deepslate", "flint", "bamboo", "cactus", "sugar_cane", "obsidian",
                "soul_sand", "soul_soil", "clay_ball", "moss_block");
        putAll(prices, 4L,
                "oak_log", "spruce_log", "birch_log", "jungle_log", "acacia_log", "dark_oak_log",
                "mangrove_log", "cherry_log", "crimson_stem", "warped_stem",
                "chorus_fruit", "string", "feather", "bone", "rotten_flesh", "spider_eye", "ink_sac");
        putAll(prices, 3L, "wheat", "potato", "carrot", "beetroot", "apple", "egg");
        putAll(prices, 6L, "porkchop", "beef", "chicken", "mutton", "cod", "salmon", "coal");
        putAll(prices, 12L,
                "leather", "slime_ball", "rabbit_hide", "prismarine_shard", "prismarine_crystals",
                "copper_ingot", "raw_copper", "quartz", "amethyst_shard");
        putAll(prices, 20L, "gunpowder", "magma_cream", "glow_ink_sac");
        putAll(prices, 30L, "honeycomb", "pufferfish");
        putAll(prices, 50L, "ender_pearl", "blaze_rod", "phantom_membrane", "nautilus_shell");
        putAll(prices, 120L, "ghast_tear", "rabbit_foot", "shulker_shell");
        putAll(prices, 8L, "redstone", "glowstone_dust");
        putAll(prices, 10L, "lapis_lazuli");
        putAll(prices, 40L, "iron_ingot", "raw_iron");
        putAll(prices, 60L, "gold_ingot", "raw_gold");
        putAll(prices, 140L, "emerald");
        putAll(prices, 400L, "diamond");
        putAll(prices, 300L, "heart_of_the_sea", "echo_shard", "sponge");
        putAll(prices, 600L, "saddle", "name_tag");
        putAll(prices, 1200L, "ancient_debris", "netherite_scrap");
        putAll(prices, 1400L, "wither_skeleton_skull");
        putAll(prices, 1500L, "totem_of_undying", "trident");
        putAll(prices, 2500L, "enchanted_golden_apple", "elytra");
        putAll(prices, 4000L, "nether_star");
        putAll(prices, 20000L, "dragon_egg");

        // В Upgrader шаблон стоит 800, но его можно размножать (1 шаблон + алмаз + незерак = 2).
        // 401 — точка безубыточности: продать два таких шаблона нельзя дороже, чем стоят материалы.
        putAll(prices, 401L, "netherite_upgrade_smithing_template");

        // В Upgrader камень = 2, но из булыжника в печи выходит 2 камня за 1 —
        // в казино это был бы бесконечный доход. Ставим 1.
        putAll(prices, 1L, "stone");

        // ---- производные значения ----
        // 9 предметов в блоке
        putAll(prices, 54L, "coal_block");
        putAll(prices, 72L, "redstone_block");
        putAll(prices, 90L, "lapis_block");
        putAll(prices, 108L, "copper_block", "raw_copper_block", "slime_block");
        putAll(prices, 360L, "iron_block", "raw_iron_block");
        putAll(prices, 540L, "gold_block", "raw_gold_block");
        putAll(prices, 1260L, "emerald_block");
        putAll(prices, 3600L, "diamond_block");
        putAll(prices, 45360L, "netherite_block");
        // 4 предмета в блоке
        putAll(prices, 48L, "quartz_block", "amethyst_block");
        // 1/9 от слитка (округление вниз, чтобы нельзя было переплавить в прибыль)
        putAll(prices, 4L, "iron_nugget");
        putAll(prices, 6L, "gold_nugget");
        // доски: 1 бревно (4) -> 4 доски
        putAll(prices, 1L,
                "oak_planks", "spruce_planks", "birch_planks", "jungle_planks", "acacia_planks",
                "dark_oak_planks", "mangrove_planks", "cherry_planks", "crimson_planks",
                "warped_planks", "bamboo_planks");
        putAll(prices, 18L, "bamboo_block");
        // простые рецепты
        putAll(prices, 4L, "snow_block", "charcoal");
        putAll(prices, 8L, "clay", "bricks");
        putAll(prices, 2L, "brick");
        putAll(prices, 1L, "bone_meal", "dried_kelp");
        putAll(prices, 9L, "bone_block", "dried_kelp_block", "bread");
        putAll(prices, 27L, "hay_block");
        putAll(prices, 32L, "glowstone");
        putAll(prices, 5040L, "netherite_ingot");
        putAll(prices, 6L,
                "cooked_porkchop", "cooked_beef", "cooked_chicken", "cooked_mutton",
                "cooked_cod", "cooked_salmon");

        // ---- теги ----
        prices.put("#minecraft:logs", 4L);
        prices.put("#minecraft:planks", 1L);
        prices.put("#minecraft:saplings", 1L);
        prices.put("#minecraft:wool", 3L);
        prices.put("#minecraft:coals", 6L);
        prices.put("#minecraft:small_flowers", 2L);
        return prices;
    }

    private static void putAll(Map<String, Long> prices, long value, String... ids) {
        for (String id : ids) {
            prices.put("minecraft:" + id, value);
        }
    }
}

