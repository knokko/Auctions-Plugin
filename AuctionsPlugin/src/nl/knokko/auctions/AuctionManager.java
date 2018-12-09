package nl.knokko.auctions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.entity.Player;

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
		if (currentAuction == null) {
			currentAuction = auction;
			currentAuction.start();
		} else {
			queuedAuctions.add(auction);
		}
	}
	
	public void update() {
		if (currentAuction != null) {
			currentAuction.update();
		}
	}
	
	public void onDisconnect(Player leaver) {
		if (currentAuction.getOwner().getUniqueId().equals(leaver.getUniqueId())) {
			currentAuction.cancel();
			currentAuction = null;
		}
		Iterator<Auction> iterator = queuedAuctions.iterator();
		while (iterator.hasNext()) {
			if (iterator.next().getOwner().getUniqueId().equals(leaver.getUniqueId())) {
				iterator.remove();
			}
		}
		if (currentAuction == null) {
			flushQueue();
		}
	}
	
	private void flushQueue() {
		if (!queuedAuctions.isEmpty()) {
			currentAuction = queuedAuctions.remove(0);
			currentAuction.start();
		}
	}
}