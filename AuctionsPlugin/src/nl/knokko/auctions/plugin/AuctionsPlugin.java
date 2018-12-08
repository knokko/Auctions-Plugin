package nl.knokko.auctions.plugin;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import nl.knokko.auctions.plugin.command.*;

public class AuctionsPlugin extends JavaPlugin {
	
	private static final String AUCTION_START_KEY = "started-auction";
	private static final String AUCTION_QUEUE_KEY = "placed-on-queue";
	private static final String BID_TOO_LOW_KEY = "bid-too-low";
	
	private File messagesFile;
	
	private String auctionStartMessage;
	private String auctionQueueMessage;
	private String bidTooLowMessage;
	private String bidPlacedMessage;
	private String auctionCancelMessage;
	private String auctionEndMessage;
	
	@Override
	public void onEnable() {
		getCommand("auction").setExecutor(new CommandAuction());
		getCommand("bid").setExecutor(new CommandBid());
		messagesFile = new File(getDataFolder() + "/messages.yml");
		if (messagesFile.exists()) {
			loadMessages();
		} else {
			Bukkit.getLogger().warning("Can't find the messages.yml file of the auctions plugin; using default values...");
		}
	}
	
	private void loadMessages() {
		FileConfiguration messages = YamlConfiguration.loadConfiguration(messagesFile);
		auctionStartMessage = messages.getString(AUCTION_START_KEY);
		auctionQueueMessage = messages.getString(AUCTION_QUEUE_KEY);
		bidTooLowMessage = messages.getString(BID_TOO_LOW_KEY);
	}
}