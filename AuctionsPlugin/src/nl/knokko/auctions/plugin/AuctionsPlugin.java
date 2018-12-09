package nl.knokko.auctions.plugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import nl.knokko.auctions.plugin.command.*;

public class AuctionsPlugin extends JavaPlugin {
	
	private static final String AUCTION_TIME_KEY = "auction-time";
	private static final String CANCEL_TIME_KEY = "cancel-time";
	private static final String QUEUED_TIME_KEY = "queued-item";
	
	private static final int AUCTION_TIME_DEFAULT = 60;
	private static final int CANCEL_TIME_DEFAULT = 45;
	private static final int QUEUED_TIME_DEFAULT = 10;
	
	private static final String AUCTION_START_KEY = "started-auction";
	private static final String AUCTION_QUEUE_KEY = "placed-on-queue";
	private static final String BID_TOO_LOW_KEY = "bid-too-low";
	private static final String BID_PLACED_KEY = "placed-bid";
	private static final String AUCTION_CANCEL_KEY = "auction-cancel";
	private static final String AUCTION_END_KEY = "auction-end";
	
	private static final String AUCTION_START_DEFAULT = "&c<PLAYER> &9has started the auction.Item &c<ITEM>&9.Type /bid &c<PRICE>";
	private static final String AUCTION_QUEUE_DEFAULT = "&9Your item has been placed in queue!";
	private static final String BID_TOO_LOW_DEFAULT = "&9Your bid has to be higher than &c<PRICE>.";
	private static final String BID_PLACED_DEFAULT = "&c<PLAYER> &9has placed bid &c<PRICE>!";
	private static final String AUCTION_CANCEL_DEFAULT = "&c<PLAYER> &9cancelled the auction.";
	private static final String AUCTION_END_DEFAULT = "&c<PLAYER> &9Has won the auction with &c<PRICE>!";
	
	private static AuctionsPlugin instance;
	
	public static AuctionsPlugin getInstance() {
		return instance;
	}
	
	private File messagesFile;
	
	private int auctionTime;
	private int cancelTime;
	private int queuedTime;
	
	private String auctionStartMessage;
	private String auctionQueueMessage;
	private String bidTooLowMessage;
	private String bidPlacedMessage;
	private String auctionCancelMessage;
	private String auctionEndMessage;
	
	public int getAuctionTime() {
		return auctionTime;
	}
	
	public int getCancelTime() {
		return cancelTime;
	}
	
	public int getQueuedTime() {
		return queuedTime;
	}
	
	private String getItemName(ItemStack item) {
		if (item.hasItemMeta()) {
			ItemMeta meta = item.getItemMeta();
			if (meta.hasDisplayName()) {
				if (item.getAmount() > 1)
					return meta.getDisplayName() + " X " + item.getAmount();
				else
					return meta.getDisplayName();
			}
		}
		if (item.getAmount() > 1)
			return item.getType().name().toLowerCase() + " X " + item.getAmount();
		else
			return item.getType().name().toLowerCase();
	}
	
	public String getAuctionStartMessage(Player owner, ItemStack item, int startAmount) {
		return auctionStartMessage.replaceAll("<PLAYER>", owner.getName()).replaceAll("<ITEM>", getItemName(item))
				.replaceAll("<PRICE>", startAmount + "");
	}
	
	public String getAuctionQueueMessage() {
		return auctionQueueMessage;
	}
	
	public String getBidTooLowMessage() {
		return bidTooLowMessage;
	}
	
	public String getBidPlacedMessage() {
		return bidPlacedMessage;
	}
	
	public String getAuctionCancelMessage(String playerName) {
		return auctionCancelMessage.replaceAll("<PLAYER>", playerName);
	}
	
	public String getAuctionEndMessage() {
		return auctionEndMessage;
	}
	
	@Override
	public void onEnable() {
		instance = this;
		getCommand("auction").setExecutor(new CommandAuction());
		getCommand("bid").setExecutor(new CommandBid());
		messagesFile = new File(getDataFolder() + "/messages.yml");
		if (messagesFile.exists()) {
			loadMessages();
		} else {
			Bukkit.getLogger().warning("Can't find the messages.yml file of the auctions plugin; using default values...");
			auctionStartMessage = AUCTION_START_DEFAULT;
			auctionQueueMessage = AUCTION_QUEUE_DEFAULT;
			bidTooLowMessage = BID_TOO_LOW_DEFAULT;
			bidPlacedMessage = BID_PLACED_DEFAULT;
			auctionCancelMessage = AUCTION_CANCEL_DEFAULT;
			auctionEndMessage = AUCTION_END_DEFAULT;
			saveDefaultMessages();
			saveDefaultConfigValues();
		}
	}
	
	@Override
	public void onDisable() {
		// TODO cancel all auctions
	}
	
	public void reloadConfigAndMessages() {
		loadMessages();
		reloadConfig();
		loadConfig();
	}
	
	private void loadConfig() {
		FileConfiguration config = getConfig();
		auctionTime = loadConfigValue(config, AUCTION_TIME_KEY);
		cancelTime = loadConfigValue(config, CANCEL_TIME_KEY);
		queuedTime = loadConfigValue(config, QUEUED_TIME_KEY);
	}
	
	private int loadConfigValue(FileConfiguration config, String key) {
		return config.getInt(key) * 1000;
	}
	
	private void saveDefaultConfigValues() {
		FileConfiguration config = getConfig();
		config.set(AUCTION_TIME_KEY, AUCTION_TIME_DEFAULT);
		config.set(CANCEL_TIME_KEY, CANCEL_TIME_DEFAULT);
		config.set(QUEUED_TIME_KEY, QUEUED_TIME_DEFAULT);
		saveConfig();
	}
	
	private void loadMessages() {
		FileConfiguration messages = YamlConfiguration.loadConfiguration(messagesFile);
		auctionStartMessage = getMessage(messages, AUCTION_START_KEY);
		auctionQueueMessage = getMessage(messages, AUCTION_QUEUE_KEY);
		bidTooLowMessage = getMessage(messages, BID_TOO_LOW_KEY);
		bidPlacedMessage = getMessage(messages, BID_PLACED_KEY);
		auctionCancelMessage = getMessage(messages, AUCTION_CANCEL_KEY);
		auctionEndMessage = getMessage(messages, AUCTION_END_KEY);
	}
	
	private String getMessage(FileConfiguration messages, String key) {
		return messages.getString(key).replace('&', ChatColor.COLOR_CHAR);
	}
	
	private void saveDefaultMessages() {
		FileConfiguration messages = new YamlConfiguration();
		messages.set(AUCTION_START_KEY, AUCTION_START_DEFAULT);
		messages.set(AUCTION_QUEUE_KEY, AUCTION_QUEUE_DEFAULT);
		messages.set(BID_TOO_LOW_KEY, BID_TOO_LOW_DEFAULT);
		messages.set(BID_PLACED_KEY, BID_PLACED_DEFAULT);
		messages.set(AUCTION_CANCEL_KEY, AUCTION_CANCEL_DEFAULT);
		messages.set(AUCTION_END_KEY, AUCTION_END_DEFAULT);
		try {
			messages.save(messagesFile);
		} catch (IOException e) {
			Bukkit.getLogger().log(Level.SEVERE, "Can't save default auctions messages", e);
		}
	}
}