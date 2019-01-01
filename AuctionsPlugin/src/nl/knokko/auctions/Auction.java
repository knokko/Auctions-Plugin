package nl.knokko.auctions;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import nl.knokko.auctions.plugin.AuctionsPlugin;
import nl.knokko.auctions.rayzr.JSONMessage;
import nl.knokko.auctions.rayzr.JSONMessage.*;

public class Auction {

	private final UUID owner;
	private final ItemStack item;

	private final int initialAmount;
	private long startTime;
	private long cancelTime;
	private long endTime;
	private long last30SecTime;
	private long last15SecTime;
	private long last3SecTime;
	private long last2SecTime;
	private long last1SecTime;

	private long prevUpdateTime;

	private Bid currentBid;

	private boolean cancelled;
	private boolean finished;

	public Auction(Player owner, ItemStack item, int initialAmount) {
		this.owner = owner.getUniqueId();
		this.item = item;
		this.initialAmount = initialAmount;
	}

	public OfflinePlayer getOwner() {
		return Bukkit.getOfflinePlayer(owner);
	}

	public Player getOnlineOwner() {
		return Bukkit.getPlayer(owner);
	}

	public ItemStack getItem() {
		return item;
	}

	void cancel() {
		cancelled = true;
		Player owner = getOnlineOwner();
		if (owner != null) {
			owner.getInventory().addItem(item);
		} else {
			Location loc = AuctionsPlugin.getInstance().getLogoutLocation(this.owner);
			loc.getWorld().dropItem(loc, item);
		}
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
		return !isStarted() || System.currentTimeMillis() <= cancelTime;
	}

	public void start() {
		startTime = System.currentTimeMillis();
		AuctionsPlugin plugin = AuctionsPlugin.getInstance();
		endTime = startTime + plugin.getAuctionTime();
		cancelTime = startTime + plugin.getCancelTime();
		last30SecTime = endTime - 30000;
		last15SecTime = endTime - 15000;
		last3SecTime = endTime - 3000;
		last2SecTime = endTime - 2000;
		last1SecTime = endTime - 1000;
		prevUpdateTime = startTime;
		String startMessage = AuctionsPlugin.getInstance().getAuctionStartMessage(getOwner(), initialAmount);
		Set<Entry<Enchantment, Integer>> enchantments = item.getEnchantments().entrySet();
		if (!enchantments.isEmpty()) {
			String[] splitted = startMessage.split("<ITEM>");
			JSONMessage message = JSONMessage.create(splitted[0]);
			for (int index = 1; index < splitted.length; index++) {
				int lastColorChar = splitted[index - 1].lastIndexOf("§");
				ChatColor color = ChatColor.WHITE;
				if (lastColorChar != -1 && lastColorChar != splitted[index - 1].length() - 1) {
					color = ChatColor.getByChar(splitted[index - 1].charAt(lastColorChar + 1));
				}
				MessagePart itemPart = createItemPart(message, item, enchantments, color);
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

	public static MessagePart createItemPart(JSONMessage message, ItemStack item,
			Set<Entry<Enchantment, Integer>> enchantments, ChatColor color) {
		MessagePart itemPart = message.new MessagePart(getItemName(item));
		itemPart.setColor(color);
		if (!enchantments.isEmpty()) {
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
		}
		return itemPart;
	}

	public static String getItemName(ItemStack item) {
		return CraftItemStack.asNMSCopy(item).getName() + " x" + (item.getAmount() == 1 ? "" : item.getAmount() + "");
	}

	public void update() {
		long time = System.currentTimeMillis();
		if (time >= cancelTime && prevUpdateTime < cancelTime) {
			Player owner = getOnlineOwner();
			if (owner != null) {
				owner.sendMessage(ChatColor.YELLOW + "You can no longer cancel your auction.");
			}
		}
		if (time >= last30SecTime && prevUpdateTime < last30SecTime) {
			Bukkit.broadcastMessage(ChatColor.YELLOW + "The current auction ends in 30 seconds");
		}
		if (time >= last15SecTime && prevUpdateTime < last15SecTime) {
			Bukkit.broadcastMessage(ChatColor.YELLOW + "The current auction ends in 15 seconds");
		}
		if (time >= last3SecTime && prevUpdateTime < last3SecTime) {
			Bukkit.broadcastMessage(ChatColor.YELLOW + "The current auction ends in 3 seconds");
		}
		if (time >= last2SecTime && prevUpdateTime < last2SecTime) {
			Bukkit.broadcastMessage(ChatColor.YELLOW + "The current auction ends in 2 seconds");
		}
		if (time >= last1SecTime && prevUpdateTime < last1SecTime) {
			Bukkit.broadcastMessage(ChatColor.YELLOW + "The current auction ends in 1 second");
		}
		if (time >= endTime) {
			if (currentBid != null) {
				AuctionsPlugin plugin = AuctionsPlugin.getInstance();
				double currentBalance = plugin.getEconomy().getBalance(currentBid.getBidder());
				Player bidder = currentBid.getOnlineBidder();
				if (currentBalance >= currentBid.getAmount()) {
					plugin.getEconomy().withdrawPlayer(currentBid.getBidder(), currentBid.getAmount());
					plugin.getEconomy().depositPlayer(getOwner(), currentBid.getAmount());
					if (bidder != null) {
						bidder.getInventory().addItem(item);
					} else {
						Location loc = plugin.getLogoutLocation(currentBid.getBidderID());
						loc.getChunk().load();
						loc.getWorld().dropItem(loc, item);
					}
					Bukkit.broadcastMessage(
							plugin.getAuctionEndMessage(currentBid.getBidder(), currentBid.getAmount()));
				} else {
					if (bidder != null) {
						bidder.sendMessage(ChatColor.RED + "You no longer have enough money to pay the auction.");
					}
					Player owner = getOnlineOwner();
					if (owner != null) {
						owner.getInventory().addItem(item);
						owner.sendMessage(ChatColor.YELLOW
								+ "The highest bidder no longer has enough balance to pay you, so your item has been returned to you.");
					} else {
						Location loc = plugin.getLogoutLocation(this.owner);
						loc.getChunk().load();
						loc.getWorld().dropItem(loc, item);
					}
				}
			} else {
				Player owner = getOnlineOwner();
				Bukkit.broadcastMessage(ChatColor.YELLOW + "The auction has been cancelled because nobody did a sufficient bid.");
				if (owner != null) {
					owner.getInventory().addItem(item);
				} else {
					Location loc = AuctionsPlugin.getInstance().getLogoutLocation(this.owner);
					loc.getChunk().load();
					loc.getWorld().dropItem(loc, item);
				}
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