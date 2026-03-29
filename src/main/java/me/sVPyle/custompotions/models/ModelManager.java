package me.sVPyle.custompotions.models;

import me.sVPyle.custompotions.CustomPotionsPro;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.components.FoodComponent;

public class ModelManager {

    public static ItemStack applyAdvancedVisuals(ItemStack item, String id, CustomPotionsPro plugin) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // 1. Установка текстуры головы (Base64 Value)
        if (meta instanceof SkullMeta skullMeta) {
            String b64Value = plugin.getConfig().getString("potions." + id + ".Value");
            if (b64Value != null && !b64Value.isEmpty()) {
                StandBase64.applyTexture(skullMeta, b64Value);
            }
        }

        // 2. Custom Model Data (3D модели)
        int modelData = plugin.getConfig().getInt("potions." + id + ".model-id", 0);
        if (modelData != 0) meta.setCustomModelData(modelData);

        // 3. Редкость (Rarity)
        String rarityStr = plugin.getConfig().getString("potions." + id + ".Rarity", "COMMON");
        try {
            meta.setRarity(ItemRarity.valueOf(rarityStr.toUpperCase()));
        } catch (Exception ignored) {}

        // 4. Блеск (Glint)
        if (plugin.getConfig().getBoolean("potions." + id + ".Enchanted-Glint", false)) {
            meta.setEnchantmentGlintOverride(true);
        }

        // 5. Food Component 1.21.1 (Механика поедания через рефлексию)
        if (plugin.getConfig().getBoolean("potions." + id + ".Make-Edible", true)) {
            try {
                Object food = meta.getClass().getMethod("getFood").invoke(meta);
                Object builder = food.getClass().getMethod("toBuilder").invoke(food);

                double eatTime = plugin.getConfig().getDouble("potions." + id + ".Eat-Time", 1.6);

                builder.getClass().getMethod("nutrition", int.class).invoke(builder, 0);
                builder.getClass().getMethod("saturation", float.class).invoke(builder, 0f);
                builder.getClass().getMethod("canAlwaysEat", boolean.class).invoke(builder, true);

                try {
                    builder.getClass().getMethod("eatSeconds", float.class).invoke(builder, (float) eatTime);
                } catch (NoSuchMethodException e) {
                    int ticks = (int) (eatTime * 20);
                    builder.getClass().getMethod("eatSeconds", int.class).invoke(builder, ticks);
                }

                Object newFood = builder.getClass().getMethod("build").invoke(builder);
                meta.getClass().getMethod("setFood", food.getClass()).invoke(meta, newFood);
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка настройки FoodComponent для " + id);
            }
        }

        // 6. Скрытие флагов (Anti-No-Effects)
        if (plugin.getConfig().getBoolean("potions." + id + ".anti-no-effects", true)) {
            for (ItemFlag flag : ItemFlag.values()) {
                meta.addItemFlags(flag);
            }
        }

        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }
}
