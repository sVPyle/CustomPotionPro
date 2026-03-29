package me.sVPyle.custompotions;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class PotionCommand implements CommandExecutor {

    private final CustomPotionsPro plugin;

    public PotionCommand(CustomPotionsPro plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String prefix = plugin.getConfig().getString("plugin.prefix", "<gradient:#4facfe:#00f2fe>[CustomPotionsPro]</gradient> ");

        // Проверка прав на базовую команду
        if (!sender.hasPermission("custompotions.admin")) {
            sender.sendMessage(plugin.parseText(prefix + "<red>У вас недостаточно прав для использования этой команды."));
            return true;
        }

        // Если аргументов нет — показываем помощь
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        // Подкоманда: RELOAD
        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            plugin.registerRecipes(); // Обновляем крафты в реальном времени
            sender.sendMessage(plugin.parseText(prefix + "<green>Конфигурация и рецепты успешно перезагружены!"));
            return true;
        }

        // Подкоманда: GIVE <player> <id>
        if (args[0].equalsIgnoreCase("give")) {
            if (args.length < 3) {
                sender.sendMessage(plugin.parseText(prefix + "<red>Ошибка! Используйте: <yellow>/cp give <игрок> <id>"));
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(plugin.parseText(prefix + "<red>Игрок <white>" + args[1] + " <red>не найден в сети."));
                return true;
            }

            String potionId = args[2];
            if (!plugin.getConfig().contains("potions." + potionId)) {
                sender.sendMessage(plugin.parseText(prefix + "<red>Зелье с ID <white>" + potionId + " <red>не найдено в конфиге."));
                return true;
            }

            // Создаем предмет и выдаем игроку
            ItemStack item = plugin.createCustomPotion(potionId);
            target.getInventory().addItem(item);

            sender.sendMessage(plugin.parseText(prefix + "<gray>Предмет <white>" + potionId + " <gray>успешно выдан игроку <white>" + target.getName() + " <gray>в инвентарь."));

            // Логируем выдачу, если игрок выдал другому
            if (sender instanceof Player p && !p.equals(target)) {
                target.sendMessage(plugin.parseText(prefix + "<gray>Вы получили артефакт: <white>" + potionId));
            }
            return true;
        }

        // Если введена неизвестная подкоманда
        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.parseText("<strikethrough><gray>      </strikethrough> <gradient:#4facfe:#00f2fe><bold>CustomPotionsPro Help</bold></gradient> <strikethrough><gray>      </strikethrough>"));
        sender.sendMessage(plugin.parseText("<yellow>/cp give <player> <id> <gray>- Выдать кастомное зелье"));
        sender.sendMessage(plugin.parseText("<yellow>/cp reload <gray>- Перезагрузить конфиг и рецепты"));
        sender.sendMessage(plugin.parseText("<strikethrough><gray>                                          </strikethrough>"));
    }
}
