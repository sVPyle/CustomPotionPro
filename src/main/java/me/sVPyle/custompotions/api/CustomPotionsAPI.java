package me.sVPyle.custompotions.api;

import me.sVPyle.custompotions.CustomPotionsPro;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

public class CustomPotionsAPI {
    private final CustomPotionsPro plugin;

    public CustomPotionsAPI(CustomPotionsPro plugin) { this.plugin = plugin; }

    // Получить предмет зелья по ID из конфига
    public ItemStack getPotionById(String id) {
        return plugin.createCustomPotion(id);
    }

    // Проверить, является ли предмет нашим кастомным зельем
    public @Nullable String getCustomPotionId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer()
                .get(plugin.getPotionKey(), PersistentDataType.STRING);
    }
}
