package nl.knokko.auctions;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import nl.knokko.auctions.plugin.AuctionsPlugin;
import nl.knokko.auctions.rayzr.JSONMessage;
import nl.knokko.auctions.rayzr.JSONMessage.*;

public class Auction {

	private final Player owner;
	private final ItemStack item;

	private final int initialAmount;
	private long startTime;
	private long prevUpdateTime;

	private Bid currentBid;

	private boolean cancelled;
	private boolean finished;

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

	public boolean isFinished() {
		return finished;
	}

	public boolean canCancel() {
		if (!isStarted()) {
			return true;
		}
		AuctionsPlugin p = AuctionsPlugin.getInstance();
		long maxCancelTime = startTime + p.getCancelTime();
		return System.currentTimeMillis() <= maxCancelTime;
	}

	public void start() {
		startTime = System.currentTimeMillis();
		prevUpdateTime = startTime;
		String startMessage = AuctionsPlugin.getInstance().getAuctionStartMessage(owner, initialAmount);
		Set<Entry<Enchantment, Integer>> enchantments = item.getEnchantments().entrySet();
		if (!enchantments.isEmpty()) {
			String[] splitted = startMessage.split("<ITEM>");
			JSONMessage message = JSONMessage.create(splitted[0]);
			for (int index = 1; index < splitted.length; index++) {
				MessagePart itemPart = message.new MessagePart(getItemName(item));
				int lastColorChar = splitted[index - 1].lastIndexOf("§");
				if (lastColorChar != -1 && lastColorChar != splitted[index - 1].length() - 1) {
					itemPart.setColor(ChatColor.getByChar(splitted[index - 1].charAt(lastColorChar + 1)));
				}
				JSONMessage hoverMessage = JSONMessage.create("Enchantments: ");
				for (Entry<Enchantment, Integer> entry : enchantments) {
					hoverMessage.newline();
					MessagePart enchantmentPart = hoverMessage.new MessagePart(
							entry.getKey().getName() + " " + entry.getValue());
					enchantmentPart.setColor(ChatColor.AQUA);
					hoverMessage.then(enchantmentPart);
				}
				MessageEvent event = HoverEvent.showText(hoverMessage);
				itemPart.setOnHover(event);
				message.then(itemPart);
				message.then(splitted[index]);
			}
			Collection<? extends Player> players = Bukkit.getOnlinePlayers();
			Player[] array = new Player[players.size()];
			int index = 0;
			for (Player player : players) {
				array[index++] = player;
			}
			message.send(array);
		} else {
			Bukkit.broadcastMessage(startMessage.replaceAll("<ITEM>", getItemName(item)));
		}
	}

	private String getItemName(ItemStack item) {
		return CraftItemStack.asNMSCopy(item).getName();
	}

	public void update() {
		long time = System.currentTimeMillis();
		AuctionsPlugin plugin = AuctionsPlugin.getInstance();
		long endTime = startTime + plugin.getAuctionTime();
		long cancelTime = startTime + plugin.getCancelTime();
		if (time >= cancelTime && prevUpdateTime < cancelTime) {
			owner.sendMessage(ChatColor.YELLOW + "You can no longer cancel your auction.");
		}
		if (time >= endTime) {
			if (currentBid != null) {
				double currentBalance = plugin.getEconomy().getBalance(currentBid.getBidder());
				if (currentBalance >= currentBid.getAmount()) {
					plugin.getEconomy().withdrawPlayer(currentBid.getBidder(), currentBid.getAmount());
					plugin.getEconomy().depositPlayer(owner, currentBid.getAmount());
					currentBid.getBidder().getInventory().addItem(item);
					currentBid.getBidder().sendMessage(ChatColor.GREEN + "You have won the auction");
					owner.sendMessage(ChatColor.GREEN + "Your item has been sent to " + currentBid.getBidder().getName()
							+ " for " + currentBid.getAmount() + " money");
				} else {
					currentBid.getBidder()
							.sendMessage(ChatColor.RED + "You no longer have enough money to pay the auction.");
					owner.getInventory().addItem(item);
					owner.sendMessage(ChatColor.YELLOW
							+ "The highest bidder no longer has enough balance to pay you, so your item has been returned to you.");
				}
			} else {
				owner.getInventory().addItem(item);
				owner.sendMessage(ChatColor.YELLOW
						+ "Your item has been returned to you because nobody has done a sufficient bid.");
			}
			finished = true;
		}
		prevUpdateTime = time;
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