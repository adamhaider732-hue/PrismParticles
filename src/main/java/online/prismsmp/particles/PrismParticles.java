package online.prismsmp.particles;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public class PrismParticles extends JavaPlugin {

    private BukkitTask particleTask;
    private double angle = 0;

    private static class CrateEntry {
        String world;
        double x, y, z;
        Particle particle;
        Color color;
        String name;

        CrateEntry(String name, String world, double x, double y, double z, Particle particle, Color color) {
            this.name = name;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.particle = particle;
            this.color = color;
        }
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        startParticles();
        getLogger().info("PrismParticles enabled!");
    }

    @Override
    public void onDisable() {
        if (particleTask != null) particleTask.cancel();
        getLogger().info("PrismParticles disabled.");
    }

    private List<CrateEntry> loadCrates() {
        FileConfiguration config = getConfig();
        List<CrateEntry> crates = new ArrayList<>();
        ConfigurationSection cratesSection = config.getConfigurationSection("crates");
        if (cratesSection == null) return crates;

        for (String key : cratesSection.getKeys(false)) {
            ConfigurationSection c = cratesSection.getConfigurationSection(key);
            if (c == null) continue;
            try {
                String world = c.getString("world", "spawnworld");
                double x = c.getDouble("x");
                double y = c.getDouble("y");
                double z = c.getDouble("z");
                String particleName = c.getString("particle", "FLAME");
                String colorHex = c.getString("color", "#FFFFFF");

                Particle particle;
                try {
                    particle = Particle.valueOf(particleName);
                } catch (IllegalArgumentException e) {
                    getLogger().warning("Unknown particle '" + particleName + "' for crate " + key + ", defaulting to FLAME");
                    particle = Particle.FLAME;
                }

                Color color = hexToColor(colorHex);
                crates.add(new CrateEntry(key, world, x, y, z, particle, color));
            } catch (Exception e) {
                getLogger().warning("Failed to load crate '" + key + "': " + e.getMessage());
            }
        }
        return crates;
    }

    private void startParticles() {
        List<CrateEntry> crates = loadCrates();
        if (crates.isEmpty()) {
            getLogger().warning("No crates configured in config.yml!");
            return;
        }

        particleTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            angle += 0.1;
            if (angle > Math.PI * 2) angle -= Math.PI * 2;

            for (CrateEntry crate : crates) {
                World world = Bukkit.getWorld(crate.world);
                if (world == null) continue;

                Location center = new Location(world, crate.x + 0.5, crate.y + 0.5, crate.z + 0.5);

                // 3 orbiting particles equally spaced
                for (int i = 0; i < 3; i++) {
                    double orbitAngle = angle + (i * (Math.PI * 2 / 3));
                    double radius = 0.8;
                    double px = center.getX() + Math.cos(orbitAngle) * radius;
                    double py = center.getY() + 0.5;
                    double pz = center.getZ() + Math.sin(orbitAngle) * radius;
                    Location pLoc = new Location(world, px, py, pz);

                    if (crate.particle == Particle.DUST) {
                        Particle.DustOptions dust = new Particle.DustOptions(crate.color, 1.2f);
                        world.spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, dust);
                    } else {
                        world.spawnParticle(crate.particle, pLoc, 1, 0, 0, 0, 0);
                    }
                }

                // Rising colored dust from center
                Particle.DustOptions risingDust = new Particle.DustOptions(crate.color, 0.8f);
                double riseY = center.getY() + (Math.sin(angle * 2) * 0.3);
                Location riseLoc = new Location(world, center.getX(), riseY, center.getZ());
                world.spawnParticle(Particle.DUST, riseLoc, 2, 0.15, 0.1, 0.15, 0, risingDust);
            }
        }, 0L, 3L);
    }

    private Color hexToColor(String hex) {
        try {
            hex = hex.replace("#", "");
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            return Color.fromRGB(r, g, b);
        } catch (Exception e) {
            return Color.WHITE;
        }
    }
}
