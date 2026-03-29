package me.sVPyle.custompotions.effects;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Random;

public class ParticleManager {

    private static final Random random = new Random();

    /**
     * Создает эффект всплеска частиц вокруг игрока
     */
    public static void playBurst(Player player, String particleName, int count) {
        try {
            Particle particle = Particle.valueOf(particleName.toUpperCase());
            player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), count, 0.5, 0.5, 0.5, 0.1);
        } catch (Exception ignored) {}
    }

    /**
     * Создает ауру (эффект, пока предмет в руке)
     */
    public static void playAura(Player player, String particleName) {
        try {
            Particle particle = Particle.valueOf(particleName.toUpperCase());
            Location loc = player.getLocation().add(0, 0.1, 0);

            // Рисуем небольшое кольцо у ног или пыль вокруг
            for (int i = 0; i < 5; i++) {
                double x = (random.nextDouble() - 0.5) * 1.2;
                double z = (random.nextDouble() - 0.5) * 1.2;
                player.getWorld().spawnParticle(particle, loc.clone().add(x, random.nextDouble() * 2, z), 0, 0, 0.1, 0, 1);
            }
        } catch (Exception ignored) {}
    }
}
