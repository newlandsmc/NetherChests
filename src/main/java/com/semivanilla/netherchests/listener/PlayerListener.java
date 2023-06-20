package com.semivanilla.netherchests.listener;

import com.semivanilla.netherchests.NetherChests;
import com.semivanilla.netherchests.utils.Cooldown;
import com.semivanilla.netherchests.utils.MigrationManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Cooldown.removeAllCooldowns(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (NetherChests.getInstance().getConfig().getBoolean("migrate.on-join", false)) {
            MigrationManager.INSTANCE.tryMigration(event.getPlayer().getUniqueId());
        }
    }
}
