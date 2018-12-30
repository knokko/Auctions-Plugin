package nl.knokko.auctions;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import nl.knokko.auctions.plugin.AuctionsPlugin;

public class AuctionManager {
	
	private List<Auction> queuedAuctions;
	
	private Auction currentAuction;
	
	public AuctionManager() {
		queuedAuctions = new ArrayList<Auction>();
	}
	
	public Auction getCurrentAuction() {
		return currentAuction;
	}
	
	public void requestAuction(Auction auction) {
		if (currentAuction == null && queuedAuctions.isEmpty()) {
			currentAuction = auction;
			currentAuction.start();
		} else {
			queuedAuctions.add(auction);
		}
	}
	
	public void update() {
		if (currentAuction != null) {
			currentAuction.update();
			if (currentAuction.isCancelled() || currentAuction.isFinished()) {
				currentAuction = null;
				flushQueue();
			}
		}
	}
	
	public void onDisconnect(Player leaver) {
		Auction auction = getAuction(leaver);
		if (auction != null && auction.canCancel()) {
			auction.cancel();
			if (auction == currentAuction) {
				currentAuction = null;
				flushQueue();
			} else {
				queuedAuctions.remove(auction);
			}
		}
	}
	
	public void onQuit() {
		if (currentAuction != null) {
			currentAuction.cancel();
		}
		for (Auction auction : queuedAuctions) {
			auction.cancel();
		}
	}
	
	private void flushQueue() {
		if (!queuedAuctions.isEmpty()) {
			currentAuction = queuedAuctions.remove(0);
			Bukkit.getScheduler().scheduleSyncDelayedTask(AuctionsPlugin.getInstance(), () -> {
				currentAuction.start();
			}, AuctionsPlugin.getInstance().getQueuedTime());
		}
	}
	
	public Auction getAuction(Player owner) {
		if (currentAuction != null && currentAuction.getOwner().getUniqueId().equals(owner.getUniqueId())) {
			return currentAuction;
		}
		for (Auction auction : queuedAuctions) {
			if (auction.getOwner().getUniqueId().equals(owner.getUniqueId())) {
				return auction;
			}
		}
		return null;
	}
	
	public boolean hasAuction(Player player) {
		return getAuction(player) != null;
	}
}