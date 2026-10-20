
package id.server.deathpenalty;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class DeathPenalty extends JavaPlugin implements Listener {

    private static final long DURATION_MS = 60L * 60L * 1000L;
    private static final int EFFECT_TICKS = 100;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        getServer().getScheduler().runTaskTimer(this, () -> {
            ConfigurationSection section =
                    getConfig().getConfigurationSection("penalties");

            if (section == null) {
                return;
            }

            long now = System.currentTimeMillis();
            boolean changed = false;

            for (String uuid : section.getKeys(false)) {
                long end = section.getLong(uuid);

                if (now >= end) {
                    getConfig().set("penalties." + uuid, null);
                    changed = true;
                    continue;
                }

                Player player = Bukkit.getPlayer(
                        java.util.UUID.fromString(uuid));

                if (player != null && player.isOnline()) {
                    applyPenalty(player);
                }
            }

            if (changed) {
                saveConfig();
            }
        }, 40L, 40L);

        getLogger().info("DeathPenalty berhasil diaktifkan!");
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        String uuid = event.getEntity().getUniqueId().toString();
        getConfig().set("pending." + uuid, true);
        saveConfig();
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTask(this, () -> {
            if (player.isOnline()) {
                startPenalty(player);
            }
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTask(this, () -> {
            String uuid = player.getUniqueId().toString();

            if (getConfig().getBoolean("pending." + uuid, false)) {
                startPenalty(player);
            } else if (isPunished(player)) {
                applyPenalty(player);
            }
        });
    }

    private void startPenalty(Player player) {
        String uuid = player.getUniqueId().toString();

        getConfig().set("pending." + uuid, null);
        getConfig().set(
                "penalties." + uuid,
                System.currentTimeMillis() + DURATION_MS
        );
        saveConfig();

        if (player.isOnline()) {
            applyPenalty(player);
        }
    }

    private boolean isPunished(Player player) {
        String path = "penalties." + player.getUniqueId();
        long end = getConfig().getLong(path, 0L);

        return end > System.currentTimeMillis();
    }

    private void applyPenalty(Player player) {
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS,
                EFFECT_TICKS,
                1,
                false,
                false,
                true
        ));
    }
          }
