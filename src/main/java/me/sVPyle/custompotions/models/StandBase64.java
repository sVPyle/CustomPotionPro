package me.sVPyle.custompotions.models;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

public class StandBase64 {

    /**
     * Применяет Base64 текстуру к SkullMeta
     */
    public static void applyTexture(SkullMeta meta, String base64) {
        if (base64 == null || base64.isEmpty()) return;

        // Создаем уникальный профиль на основе случайного UUID
        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), "CustomCoreItem");

        // Устанавливаем свойство текстуры
        profile.setProperty(new ProfileProperty("textures", base64));

        // Применяем профиль к мете головы
        meta.setPlayerProfile(profile);
    }
}
