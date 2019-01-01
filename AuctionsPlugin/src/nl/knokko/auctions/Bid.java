package nl.knokko.auctions;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class Bid {
	
	private final int amount;
	private final UUID bidder;
	
	public Bid(Player bidder, int amount) {
		this.bidder = bidder.getUniqueId();
		this.amount = amount;
	}
	
	public UUID getBidderID() {
		return bidder;
	}
	
	public OfflinePlayer getBidder() {
		return Bukkit.getOfflinePlayer(bidder);
	}
	
	public Player getOnlineBidder() {
		return Bukkit.getPlayer(bidder);
	}
	
	public int getAmount() {
		return amount;
	}
}