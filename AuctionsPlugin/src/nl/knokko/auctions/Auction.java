package nl.knokko.auctions;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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
	
	public void takeItem() {
		owner.getInventory().remove(item);
	}
	
	public void cancel() {
		cancelled = true;
		owner.getInventory().addItem(item);
		if (isStarted()) {
			//Bukkit.broadcastMessage(ChatColor.YELLOW + "")
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
	}
}