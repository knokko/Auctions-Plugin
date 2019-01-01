package nl.knokko.auctions.plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.milkbowl.vault.economy.Economy;
import nl.knokko.auctions.AuctionManager;
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
	
	private static final String AUCTION_START_DEFAULT = "§c<PLAYER> §9has started the auction.Item §c<ITEM>§9.Type /bid §c<PRICE>";
	private static final String AUCTION_QUEUE_DEFAULT = "§9Your item has been placed in queue!";
	private static final String BID_TOO_LOW_DEFAULT = "§9Your bid has to be higher than §c<PRICE>.";
	private static final String BID_PLACED_DEFAULT = "§c<PLAYER> &9has placed bid §c<PRICE>!";
	private static final String AUCTION_CANCEL_DEFAULT = "§c<PLAYER> &9cancelled the auction.";
	private static final String AUCTION_END_DEFAULT = "§c<PLAYER> §9Has won the auction with §c<PRICE>!";
	
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
	
	private AuctionManager manager;
	private Economy economy;
	private Map<UUID,Location> locationMap;
	
	public void setLogoutLocation(UUID id, Location location) {
		locationMap.put(id, location);
	}
	
	public Location getLogoutLocation(UUID id) {
		return locationMap.get(id);
	}
	
	public int getAuctionTime() {
		return auctionTime;
	}
	
	public int getCancelTime() {
		return cancelTime;
	}
	
	public int getQueuedTime() {
		return queuedTime;
	}
	
	public String getAuctionStartMessage(OfflinePlayer owner, int startAmount) {
		return auctionStartMessage.replaceAll("<PLAYER>", owner.getName()).replaceAll("<PRICE>", startAmount + "");
	}
	
	public String getAuctionQueueMessage() {
		return auctionQueueMessage;
	}
	
	public String getBidTooLowMessage(int price) {
		return bidTooLowMessage.replaceAll("<PRICE>", price + "");
	}
	
	public String getBidPlacedMessage(OfflinePlayer player, int price) {
		return bidPlacedMessage.replaceAll("<PLAYER>", player.getName()).replaceAll("<PRICE>", price + "");
	}
	
	public String getAuctionCancelMessage(String playerName) {
		return auctionCancelMessage.replaceAll("<PLAYER>", playerName);
	}
	
	public String getAuctionEndMessage(OfflinePlayer winner, int price) {
		return auctionEndMessage.replaceAll("<PLAYER>", winner.getName()).replaceAll("<PRICE>", price + "");
	}
	
	public AuctionManager getManager() {
		return manager;
	}
	
	public Economy getEconomy() {
		return economy;
	}
	
	@Override
	public void onEnable() {
		instance = this;
		locationMap = new HashMap<UUID,Location>();
		Bukkit.getPluginManager().registerEvents(new AuctionsEventHandler(), this);
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
        	Bukkit.getLogger().severe("Can't find Vault plug-in; disabling Auctions...");
        	Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        economy = rsp.getProvider();
		manager = new AuctionManager();
		getCommand("auction").setExecutor(new CommandAuction());
		getCommand("bid").setExecutor(new CommandBid());
		loadConfig();
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
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
			manager.update();
		}, 40, 10);
	}
	
	@Override
	public void onDisable() {
		// manager can be null if plug-in is stopping because vault is not found
		if (manager != null) {
			manager.onQuit();
		}
		super.onDisable();
	}
	
	public void reloadConfigAndMessages() {
		loadMessages();
		reloadConfig();
		loadConfig();
	}
	
	private void loadConfig() {
		FileConfiguration config = getConfig();
		auctionTime = loadConfigValue(config, AUCTION_TIME_KEY) * 1000;
		cancelTime = loadConfigValue(config, CANCEL_TIME_KEY) * 1000;
		queuedTime = loadConfigValue(config, QUEUED_TIME_KEY) * 20;
	}
	
	private int loadConfigValue(FileConfiguration config, String key) {
		return config.getInt(key);
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