package nl.knokko.auctions.plugin.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import nl.knokko.auctions.Auction;
import nl.knokko.auctions.AuctionManager;
import nl.knokko.auctions.Bid;
import nl.knokko.auctions.plugin.AuctionsPlugin;

public class CommandBid implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (sender instanceof Player) {
			if (args.length == 1) {
				try {
					int amount = Integer.parseInt(args[0]);
					if (amount <= 0) {
						sender.sendMessage(ChatColor.RED + "The amount must be greater than 0.");
						return true;
					}
					AuctionsPlugin plugin = AuctionsPlugin.getInstance();
					Player player = (Player) sender;
					double available = plugin.getEconomy().getBalance(player);
					if (available >= amount) {
						AuctionManager manager = plugin.getManager();
						Auction current = manager.getCurrentAuction();
						if (current != null && current.isStarted()) {
							if (!player.getUniqueId().equals(current.getOwner().getUniqueId())) {
								Bid highest = current.getHighestBid();
								if (highest == null || !highest.getBidder().getUniqueId().equals(player.getUniqueId())) {
									if ((highest != null && amount > highest.getAmount())
											|| (highest == null && amount >= current.getInitialAmount())) {
										current.setHighestBid(new Bid(player, amount));
									} else {
										player.sendMessage(plugin.getBidTooLowMessage(
												highest != null ? highest.getAmount() : current.getInitialAmount()));
									}
								} else {
									player.sendMessage(ChatColor.RED + "You can't bid against yourself.");
								}
							} else {
								player.sendMessage(ChatColor.RED + "You can't bid on your own auction");
							}
						} else {
							player.sendMessage(ChatColor.RED + "There is no auction at the moment");
						}
					} else {
						player.sendMessage(ChatColor.RED + "Your bid (" + amount
								+ ") can't be greater than your balance (" + available + ")");
					}
				} catch (NumberFormatException nfe) {
					sender.sendMessage(ChatColor.RED + "Your bid amount must be an integer");
				}
			} else {
				sender.sendMessage(ChatColor.RED + "You should use /bid <AMOUNT>");
			}
		} else {
			sender.sendMessage("Only players can bid");
		}
		return true;
	}
}