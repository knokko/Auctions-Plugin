package nl.knokko.auctions.plugin.command;

import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import nl.knokko.auctions.Auction;
import nl.knokko.auctions.AuctionManager;
import nl.knokko.auctions.Bid;
import nl.knokko.auctions.plugin.AuctionsPlugin;
import nl.knokko.auctions.rayzr.JSONMessage;
import nl.knokko.auctions.rayzr.JSONMessage.MessagePart;

public class CommandAuction implements CommandExecutor {

	private void sendUseage(CommandSender sender) {
		if (sender.hasPermission("auctions.reload"))
			sender.sendMessage(ChatColor.RED + "You should use /auc start/cancel/reload");
		else
			sender.sendMessage(ChatColor.RED + "You should use /auc start/cancel");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length > 0) {
			if (args[0].equals("start")) {
				if (sender instanceof Player) {
					if (args.length == 1 || args.length == 2) {
						int minimumAmount;
						if (args.length == 1) {
							minimumAmount = 1;
						} else {
							try {
								minimumAmount = Integer.parseInt(args[1]);
							} catch (NumberFormatException nfe) {
								sender.sendMessage(ChatColor.RED + "The minimum amount should be an integer,"
										+ " but you entered '" + args[1] + "'");
								return true;
							}
							if (minimumAmount <= 0) {
								sender.sendMessage(ChatColor.RED + "The minimum amount must be greater than 0.");
								return true;
							}
						}
						Player player = (Player) sender;
						if (!AuctionsPlugin.getInstance().getManager().hasAuction(player)) {
							ItemStack item = player.getItemInHand();
							if (item != null && item.getType() != Material.AIR && item.getAmount() > 0) {
								AuctionsPlugin plugin = AuctionsPlugin.getInstance();
								Auction auction = new Auction(player, item, minimumAmount);
								player.setItemInHand(null);
								plugin.getManager().requestAuction(auction);
							} else {
								player.sendMessage(ChatColor.RED + "Hold the item you want to sell in your hand.");
							}
						} else {
							sender.sendMessage(ChatColor.RED + "You can only have 1 auction at the same time");
						}
					} else {
						sender.sendMessage(ChatColor.RED + "You should use /auc start [minimum amount]");
					}
				} else {
					sender.sendMessage("Only players can start auctions");
				}
			} else if (args[0].equals("cancel")) {
				if (sender instanceof Player) {
					Player player = (Player) sender;
					AuctionManager manager = AuctionsPlugin.getInstance().getManager();
					Auction auction = manager.getAuction(player);
					if (auction != null) {
						if (auction.isStarted()) {
							if (auction.canCancel()) {
								manager.cancelAuction(auction);
								player.sendMessage(ChatColor.GREEN + "Your auction has been cancelled");
							} else {
								player.sendMessage(ChatColor.RED + "You can no longer cancel your auction");
							}
						} else {
							manager.cancelAuction(auction);
							player.sendMessage(ChatColor.GREEN + "Your auction has been removed from the queue");
						}
					} else {
						player.sendMessage(ChatColor.RED + "You don't have any auctions to cancel.");
					}
				}
			} else if (args[0].equals("reload")) {
				if (sender.hasPermission("auctions.reload")) {
					AuctionsPlugin.getInstance().reloadConfigAndMessages();
					sender.sendMessage(ChatColor.GREEN + "Config and messages have been reloaded");
				} else {
					sender.sendMessage(ChatColor.DARK_RED + "You do not have access to this command");
				}
			} else if (args[0].equals("info")) {
				Auction current = AuctionsPlugin.getInstance().getManager().getCurrentAuction();
				if (current == null) {
					sender.sendMessage(ChatColor.YELLOW + "There is no auction at the moment.");
				} else if (current.isStarted()) {
					if (sender instanceof Player) {
						JSONMessage message = JSONMessage.create(current.getOwner().getName() + " is selling ");
						message.color(ChatColor.YELLOW);
						ItemStack item = current.getItem();
						Set<Entry<Enchantment, Integer>> enchantments = item.getEnchantments().entrySet();
						MessagePart itemPart = Auction.createItemPart(message, item, enchantments,
								enchantments.isEmpty() ? ChatColor.WHITE : ChatColor.AQUA);
						message.then(itemPart);
						message.send((Player) sender);
					} else {
						sender.sendMessage(current.getOwner().getName() + " is selling " + Auction.getItemName(current.getItem()));
					}
					Bid bid = current.getHighestBid();
					if (bid == null) {
						sender.sendMessage(ChatColor.YELLOW + "Bid at least " + current.getInitialAmount() + " to be the first bidder.");
					} else {
						sender.sendMessage(ChatColor.YELLOW + "The highest bid of " + bid.getAmount() + " was done by " + bid.getBidder().getName());
					}
				} else {
					sender.sendMessage(ChatColor.YELLOW + "The next auction will start soon");
				}
			} else {
				sendUseage(sender);
			}
		} else {
			sendUseage(sender);
		}
		return false;
	}
}