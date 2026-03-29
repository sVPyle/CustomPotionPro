package me.sVPyle.custompotions;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import java.util.ArrayList;
import java.util.List;

public class PotionTabCompleter implements TabCompleter {
    private final CustomPotionsPro plugin;
    public PotionTabCompleter(CustomPotionsPro plugin) { this.plugin = plugin; }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String a, String[] args) {
        if (args.length == 1) return List.of("give", "reload");
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            var sect = plugin.getConfig().getConfigurationSection("potions");
            return sect != null ? new ArrayList<>(sect.getKeys(false)) : null;
        }
        return null;
    }
}
