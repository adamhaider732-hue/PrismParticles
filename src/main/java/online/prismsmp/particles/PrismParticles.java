package online.prismsmp.particles;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public class PrismParticles extends JavaPlugin {

    private BukkitTask particleTask;
    private double angle = 0;
    private List<CrateEntry> crates;

    private static class CrateEntry {
        final String world;
        final double x, y, z;
        final String particleName;
        final int r, g, b;

        CrateEntry(String world, double x, double y, double z, String particleName, int r, int g, int b) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.particleName = particleName;
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        crates = loadCrates();
        if (crates.isEmpty()) {
            getLogger().warning("No crates configured in config.yml!");
        } else {
            getLogger().info("Loaded " + crates.size() + " crates.");
            startParticles();
        }
        getLogger().info("PrismParticles enabled!");
    }

    @Override
    public void onDisable() {
        if (particleTask != null) {
            particleTask.cancel();
        }
        getLogger().info("PrismParticles disabled.");
    }

    private List<CrateEntry> loadCrates() {
        List<CrateEntry> list = new ArrayList<>();
        ConfigurationSection section = getConfig().getConfigurationSection("crates");
        if (section == null) return list;

        for (String key : section.getKeys(false)) {
            ConfigurationSection c = section.getConfigurationSection(key);
            if (c == null) continue;
            try {
                String world = c.getString("world", "spawnworld");
                double x = c.getDouble("x");
                double y = c.getDouble("y");
                double z = c.getDouble("z");
                String particleName = c.getString("particle", "FLAME");
                String hex = c.getString("color", "#FFFFFF").replace("#", "");
                int r = Integer.parseInt(hex.substring(0, 2), 16);
                int g = Integer.parseInt(hex.substring(2, 4), 16);
                int b = Integer.parseInt(hex.substring(4, 6), 16);
                list.add(new CrateEntry(world, x, y, z, particleName, r, g, b));
                getLogger().info("Loaded crate: " + key + " at " + world + " " + x + "," + y + "," + z);
            } catch (Exception e) {
                getLogger().warning("Failed to load crate '" + key + "': " + e.getMessage());
            }
        }
        return list;
    }

    private void startParticles() {
        particleTask = Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            @Override
            public void run() {
                angle += 0.1;
                if (angle > Math.PI * 2) angle -= Math.PI * 2;

                for (CrateEntry crate : crates) {
                    World world = Bukkit.getWorld(crate.world);
                    if (world == null) continue;

                    double cx = crate.x + 0.5;
                    double cy = crate.y + 0.5;
                    double cz = crate.z + 0.5;

                    // 3 orbiting dust particles spaced equally
                    for (int i = 0; i < 3; i++) {
                        double a = angle + (i * (Math.PI * 2.0 / 3.0));
                        double px = cx + Math.cos(a) * 0.8;
                        double py = cy + 0.5;
                        double pz = cz + Math.sin(a) * 0.8;
                        Location loc = new Location(world, px, py, pz);
                        try {
                            Particle.DustOptions dust = new Particle.DustOptions(
                                Color.fromRGB(crate.r, crate.g, crate.b), 1.2f);
                            world.spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, dust);
                        } catch (Exception e) {
                            // silently ignore particle errors
                        }
                    }

                    // Rising dust from center
                    try {
                        double ry = cy + (Math.sin(angle * 2) * 0.3);
                        Location rise = new Location(world, cx, ry, cz);
                        Particle.DustOptions risingDust = new Particle.DustOptions(
                            Color.fromRGB(crate.r, crate.g, crate.b), 0.8f);
                        world.spawnParticle(Particle.DUST, rise, 2, 0.15, 0.1, 0.15, 0, risingDust);
                    } catch (Exception e) {
                        // silently ignore particle errors
                    }
                }
            }
        }, 0L, 3L);
    }
}
