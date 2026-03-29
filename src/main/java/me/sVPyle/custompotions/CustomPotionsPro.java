package me.sVPyle.custompotions;

import me.clip.placeholderapi.PlaceholderAPI;
import me.sVPyle.custompotions.api.CustomPotionsAPI;
import me.sVPyle.custompotions.effects.ParticleManager;
import me.sVPyle.custompotions.models.ModelManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CustomPotionsPro extends JavaPlugin {

    private static CustomPotionsAPI api;
    private final NamespacedKey potionKey = new NamespacedKey(this, "custom_potion_id");
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private boolean papiEnabled = false;

    @Override
    public void onEnable() {
        // Загрузка конфигурации
        this.saveDefaultConfig();
        // Загрузка документации в папку плагина
        this.saveResource("DOCUMENTATION.md", false);

        // Проверка зависимостей
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            this.papiEnabled = true;
            this.getLogger().info("PlaceholderAPI найден и успешно подключен.");
        }

        // Регистрация внутреннего API
        api = new CustomPotionsAPI(this);

        // Регистрация команд
        if (this.getCommand("cp") != null) {
            this.getCommand("cp").setExecutor(new PotionCommand(this));
            this.getCommand("cp").setTabCompleter(new PotionTabCompleter(this));
        }

        // Регистрация слушателя событий
        this.getServer().getPluginManager().registerEvents(new PotionListener(this), this);

        // Регистрация рецептов крафта
        this.registerRecipes();

        // Запуск задачи для пассивных эффектов и частиц
        this.startPassiveTask();

        this.getLogger().info("§6[CorePhial] §aПлагин активирован, документация загружена. Версия: " + this.getDescription().getVersion());
    }

    private void startPassiveTask() {
        // Проверка каждые 10 тиков (0.5 сек) для плавности ауры
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                // Проверка основной руки
                ItemStack mainHand = player.getInventory().getItemInMainHand();
                handleHandPassive(player, mainHand, true);

                // Проверка второй руки
                ItemStack offHand = player.getInventory().getItemInOffHand();
                handleHandPassive(player, offHand, false);
            }
        }, 20L, 10L);
    }

    private void handleHandPassive(Player player, ItemStack item, boolean isMainHand) {
        String id = this.getPotionId(item);
        if (id == null) return;

        // Детальная проверка настроек рук
        boolean useInMain = this.getConfig().getBoolean("potions." + id + ".Hand-Settings.MainHand", true);
        boolean useInOff = this.getConfig().getBoolean("potions." + id + ".Hand-Settings.OffHand", false);

        if (isMainHand && !useInMain) return;
        if (!isMainHand && !useInOff) return;

        // Применение пассивных эффектов
        if (this.getConfig().contains("potions." + id + ".Passive-Effects")) {
            List<String> passiveEffects = this.getConfig().getStringList("potions." + id + ".Passive-Effects");
            for (String entry : passiveEffects) {
                try {
                    String[] parts = entry.split(":");
                    PotionEffectType type = PotionEffectType.getByName(parts[0].toUpperCase());
                    if (type != null) {
                        int amplifier = (parts.length > 1) ? Integer.parseInt(parts[1]) - 1 : 0;
                        // Длительность чуть больше периода задачи, чтобы эффект не мерцал
                        player.addPotionEffect(new PotionEffect(type, 35, Math.max(0, amplifier), true, false, true));
                    }
                } catch (Exception ignored) {}
            }
        }

        // Отрисовка ауры частиц
        String auraParticle = this.getConfig().getString("potions." + id + ".Particles.Aura");
        if (auraParticle != null && !auraParticle.isEmpty()) {
            ParticleManager.playAura(player, auraParticle);
        }
    }

    public void registerRecipes() {
        var section = this.getConfig().getConfigurationSection("potions");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            if (!this.getConfig().getBoolean("potions." + key + ".Craft.enabled", false)) continue;

            ItemStack result = this.createCustomPotion(key);
            NamespacedKey recipeKey = new NamespacedKey(this, "recipe_" + key);

            if (Bukkit.getRecipe(recipeKey) != null) {
                Bukkit.removeRecipe(recipeKey);
            }

            ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);
            recipe.shape("012", "345", "678");

            boolean hasIngredients = false;
            for (int i = 0; i < 9; i++) {
                String input = this.getConfig().getString("potions." + key + ".Craft.items." + i);
                if (input != null && !input.equalsIgnoreCase("air")) {
                    recipe.setIngredient(String.valueOf(i).charAt(0), parseIngredient(input));
                    hasIngredients = true;
                }
            }
            if (hasIngredients) {
                Bukkit.addRecipe(recipe);
            }
        }
    }

    private RecipeChoice parseIngredient(String input) {
        input = input.toLowerCase();
        if (input.contains("potion")) {
            Material type = input.contains("splash") ? Material.SPLASH_POTION : Material.POTION;
            ItemStack item = new ItemStack(type);
            PotionMeta pm = (PotionMeta) item.getItemMeta();
            if (pm != null) {
                if (input.contains("thick")) pm.setBasePotionType(PotionType.THICK);
                else if (input.contains("awkward")) pm.setBasePotionType(PotionType.AWKWARD);
                item.setItemMeta(pm);
            }
            return new RecipeChoice.ExactChoice(item);
        }
        Material mat = Material.matchMaterial(input.toUpperCase());
        return new RecipeChoice.MaterialChoice(mat != null ? mat : Material.AIR);
    }

    public ItemStack createCustomPotion(String id) {
        String matName = this.getConfig().getString("potions." + id + ".Material", "POTION");
        Material mat = Material.matchMaterial(matName.toUpperCase());
        ItemStack item = new ItemStack(mat != null ? mat : Material.POTION);

        // Применяем все визуальные настройки и компоненты 1.21.1
        item = ModelManager.applyAdvancedVisuals(item, id, this);

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // Имя
        String displayName = this.getConfig().getString("potions." + id + ".Name", id);
        meta.displayName(this.parseText(displayName));

        // Описание (Lore)
        List<String> loreLines = this.getConfig().getStringList("potions." + id + ".Lore");
        List<Component> loreComponents = loreLines.stream()
                .map(this::parseText)
                .collect(Collectors.toList());
        meta.lore(loreComponents);

        // Цвет зелья
        if (meta instanceof PotionMeta pm) {
            String colorStr = this.getConfig().getString("potions." + id + ".Color");
            if (colorStr != null && colorStr.contains(",")) {
                String[] rgb = colorStr.split(",");
                if (rgb.length == 3) {
                    try {
                        pm.setColor(org.bukkit.Color.fromRGB(
                                Integer.parseInt(rgb[0].trim()),
                                Integer.parseInt(rgb[1].trim()),
                                Integer.parseInt(rgb[2].trim())
                        ));
                    } catch (Exception ignored) {}
                }
            }
        }

        // Скрытая метка плагина
        meta.getPersistentDataContainer().set(this.potionKey, PersistentDataType.STRING, id);
        item.setItemMeta(meta);

        return item;
    }

    public Component parseText(String text) {
        if (text == null) return Component.empty();
        if (this.papiEnabled) {
            text = PlaceholderAPI.setPlaceholders(null, text);
        }
        Component legacy = LegacyComponentSerializer.legacyAmpersand().deserialize(text);
        String serialized = this.miniMessage.serialize(legacy).replace("\\<", "<");
        return this.miniMessage.deserialize(serialized);
    }

    public String getPotionId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(this.potionKey, PersistentDataType.STRING);
    }

    public static CustomPotionsAPI getApi() { return api; }
    public NamespacedKey getPotionKey() { return potionKey; }
}
