package nl.knokko.auctions;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import nl.knokko.auctions.plugin.AuctionsPlugin;

public class Auction {
	
	private final Player owner;
	private final ItemStack item;
	
	private final int initialAmount;
	private long startTime;
	
	private Bid currentBid;
	
	private boolean cancelled;
	
	public Auction(Player owner, ItemStack item, int initialAmount) {
		this.owner = owner;
		this.item = item;
		this.initialAmount = initialAmount;
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
	
	public boolean canCancel() {
		if (!isStarted()) {
			return true;
		}
		AuctionsPlugin p = AuctionsPlugin.getInstance();
		long endTime = startTime + 1000 * p.getAuctionTime();
		long maxCancelTime = endTime - 1000 * p.getCancelTime();
		return System.currentTimeMillis() <= maxCancelTime;
	}
	
	public void start() {
		startTime = System.currentTimeMillis();
		Bukkit.broadcastMessage(AuctionsPlugin.getInstance().getAuctionStartMessage(owner, item, initialAmount));
	}
	
	public void update() {
		// TODO this is one of the last things I need to do...
	}
	
	public Bid getHighestBid() {
		return currentBid;
	}
	
	public void setHighestBid(Bid bid) {
		currentBid = bid;
		Bukkit.broadcastMessage(AuctionsPlugin.getInstance().getBidPlacedMessage(bid.getBidder(), bid.getAmount()));
	}
	
	public int getInitialAmount() {
		return initialAmount;
	}
}