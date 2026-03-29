package me.sVPyle.custompotions.sounds;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import me.sVPyle.custompotions.CustomPotionsPro;

public class SoundManager {
    public static void play(Player player, String soundPath, CustomPotionsPro plugin) {
        String soundName = plugin.getConfig().getString(soundPath);
        if (soundName == null || soundName.isEmpty()) return;

        try {
            player.playSound(player.getLocation(), Sound.valueOf(soundName.toUpperCase()), 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Звук не найден: " + soundName);
        }
    }
}
