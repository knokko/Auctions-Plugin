package nl.knokko.auctions;

import org.bukkit.entity.Player;

public class Bid {
	
	private final int amount;
	private final Player bidder;
	
	public Bid(Player bidder, int amount) {
		this.bidder = bidder;
		this.amount = amount;
	}
	
	public Player getBidder() {
		return bidder;
	}
	
	public int getAmount() {
		return amount;
	}
}