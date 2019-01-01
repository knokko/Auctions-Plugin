package nl.knokko.auctions.plugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class AuctionsEventHandler implements Listener {
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		AuctionsPlugin.getInstance().getManager().onDisconnect(event.getPlayer());
		AuctionsPlugin.getInstance().setLogoutLocation(event.getPlayer().getUniqueId(), event.getPlayer().getLocation());
	}
}