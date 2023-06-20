package com.semivanilla.netherchests.listener;

import com.semivanilla.netherchests.utils.Cooldown;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Cooldown.removeAllCooldowns(event.getPlayer().getUniqueId());
    }
}
