package me.sVPyle.custompotions.permissions;

import org.bukkit.entity.Player;
import me.sVPyle.custompotions.CustomPotionsPro;

public class PermissionChecker {
    public static boolean canUse(Player player, String potionId, CustomPotionsPro plugin) {
        String perm = plugin.getConfig().getString("potions." + potionId + ".Permission");
        if (perm == null || perm.isEmpty()) return true;
        return player.hasPermission(perm);
    }
}
