package nl.knokko.auctions;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import nl.knokko.auctions.plugin.AuctionsPlugin;

public class Auction {
	
	private final Player owner;
	private final ItemStack item;
	
	private int amount;
	private long startTime;
	
	private boolean cancelled;
	
	public Auction(Player owner, ItemStack item, int initialAmount) {
		this.owner = owner;
		this.item = item;
		amount = initialAmount;
	}
	
	public Player getOwner() {
		return owner;
	}
	
	public void cancel() {
		cancelled = true;
		owner.getInventory().addItem(item);
		if (isStarted()) {
			Bukkit.broadcastMessage(AuctionsPlugin.getInstance().getAuctionCancelMessage(owner.getName()));
		}
	}
	
	public boolean isCancelled() {
		return cancelled;
	}
	
	public boolean isStarted() {
		return startTime != 0;
	}
	
	public void start() {
		startTime = System.currentTimeMillis();
		Bukkit.broadcastMessage(AuctionsPlugin.getInstance().getAuctionStartMessage(owner, item, amount));
	}
	
	public void update() {
		
	}
}