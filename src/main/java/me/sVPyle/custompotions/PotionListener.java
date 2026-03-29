package me.sVPyle.custompotions;

import me.sVPyle.custompotions.permissions.PermissionChecker;
import me.sVPyle.custompotions.sounds.SoundManager;
import me.sVPyle.custompotions.effects.ParticleManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class PotionListener implements Listener {
    private final CustomPotionsPro plugin;
    private final HashMap<UUID, Long> cooldowns = new HashMap<>();
    private final Random random = new Random();

    public PotionListener(CustomPotionsPro plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        // При потреблении (питье) всегда считаем, что это основное использование
        this.handlePotionUsage(event.getPlayer(), event.getItem(), event, true);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        // Обработка только правой кнопки мыши
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;

        // Если предмет нельзя съесть штатно (не еда и не зелье) — активируем его через клик
        if (!item.getType().isEdible() && !item.getType().name().contains("POTION")) {
            boolean isMainHand = (event.getHand() == EquipmentSlot.HAND);
            this.handlePotionUsage(event.getPlayer(), item, event, isMainHand);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        // Запрещаем ставить кастомные головы или блоки
        if (this.getCustomId(event.getItemInHand()) != null) {
            event.setCancelled(true);
        }
    }

    private String getCustomId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(this.plugin.getPotionKey(), PersistentDataType.STRING);
    }

    private void handlePotionUsage(Player player, ItemStack item, org.bukkit.event.Cancellable event, boolean isMainHand) {
        String id = this.getCustomId(item);
        if (id == null) return;

        // Проверка: разрешена ли активация для конкретной руки
        boolean allowMain = this.plugin.getConfig().getBoolean("potions." + id + ".Hand-Settings.MainHand", true);
        boolean allowOff = this.plugin.getConfig().getBoolean("potions." + id + ".Hand-Settings.OffHand", false);

        if (isMainHand && !allowMain) return;
        if (!isMainHand && !allowOff) return;

        // 1. Проверка прав доступа
        if (!PermissionChecker.canUse(player, id, this.plugin)) {
            player.sendActionBar(this.plugin.parseText("<red>У вас нет прав на активацию этого предмета!"));
            event.setCancelled(true);
            return;
        }

        // 2. Проверка кулдауна (перезарядки)
        if (this.cooldowns.containsKey(player.getUniqueId())) {
            long timeLeft = (this.cooldowns.get(player.getUniqueId()) - System.currentTimeMillis()) / 1000;
            if (timeLeft > 0) {
                player.sendActionBar(this.plugin.parseText("<red>Подождите еще " + timeLeft + " сек."));
                event.setCancelled(true);
                return;
            }
        }

        // 3. Выполнение логики эффектов и команд
        this.applyPotionLogic(player, id);

        // 4. Звуковые эффекты
        SoundManager.play(player, "potions." + id + ".Sound", this.plugin);

        // 5. Визуальные эффекты (Всплеск частиц)
        String burstParticle = this.plugin.getConfig().getString("potions." + id + ".Particles.Burst");
        if (burstParticle != null && !burstParticle.isEmpty()) {
            int count = this.plugin.getConfig().getInt("potions." + id + ".Particles.Count", 30);
            ParticleManager.playBurst(player, burstParticle, count);
        }

        // 6. Установка кулдауна
        int cdSec = this.plugin.getConfig().getInt("potions." + id + ".temporary-ban", 0);
        if (cdSec > 0) {
            this.cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (cdSec * 1000L));
        }

        // 7. Удаление предмета (если он не бесконечный)
        boolean isInfinite = this.plugin.getConfig().getBoolean("potions." + id + ".Infinite", false);
        if (!isInfinite) {
            // Зелья Minecraft съедает сам, для остального уменьшаем стак
            if (!item.getType().name().contains("POTION")) {
                item.setAmount(item.getAmount() - 1);
            }
        }
    }

    private void applyPotionLogic(Player player, String id) {
        List<String> entries = this.plugin.getConfig().getStringList("potions." + id + ".Effect");
        boolean isRandom = this.plugin.getConfig().getBoolean("potions." + id + ".Random-Effect", false);

        if (isRandom && !entries.isEmpty()) {
            this.parseAndExecute(player, entries.get(this.random.nextInt(entries.size())));
        } else {
            entries.forEach(entry -> this.parseAndExecute(player, entry));
        }
    }

    private void parseAndExecute(Player player, String entry) {
        // Проверка: это команда или эффект зелья?
        if (entry.startsWith("/") || entry.contains("{") || entry.startsWith("tellraw")) {
            String cmd = entry.startsWith("/") ? entry.substring(1) : entry;

            // Замена плейсхолдеров вручную и через PAPI
            cmd = cmd.replace("%player%", player.getName());
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                cmd = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, cmd);
            }

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        } else {
            // Парсинг эффекта формата ID:ДЛИТЕЛЬНОСТЬ:СИЛА
            try {
                String[] parts = entry.split(":");
                PotionEffectType type = PotionEffectType.getByName(parts[0].toUpperCase());
                if (type != null) {
                    int duration = Integer.parseInt(parts[1]) * 20; // секунды в тики
                    int amplifier = Integer.parseInt(parts[2]) - 1; // I уровень в конфиге = 0 в коде
                    player.addPotionEffect(new PotionEffect(type, duration, Math.max(0, amplifier)));
                }
            } catch (Exception e) {
                this.plugin.getLogger().warning("Ошибка в формате эффекта: " + entry);
            }
        }
    }
}
