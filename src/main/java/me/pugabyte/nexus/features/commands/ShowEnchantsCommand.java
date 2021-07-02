package me.pugabyte.nexus.features.commands;

import eden.utils.TimeUtils.Time;
import lombok.Data;
import lombok.NonNull;
import me.pugabyte.nexus.features.chat.Chat.Broadcast;
import me.pugabyte.nexus.features.chat.bridge.IngameBridgeListener;
import me.pugabyte.nexus.features.discord.Discord;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Cooldown;
import me.pugabyte.nexus.framework.commands.models.annotations.Cooldown.Part;
import me.pugabyte.nexus.framework.commands.models.annotations.Description;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.framework.exceptions.postconfigured.InvalidInputException;
import me.pugabyte.nexus.models.chat.Chatter;
import me.pugabyte.nexus.models.chat.ChatterService;
import me.pugabyte.nexus.models.chat.PublicChannel;
import me.pugabyte.nexus.models.discord.DiscordUser;
import me.pugabyte.nexus.models.discord.DiscordUserService;
import me.pugabyte.nexus.utils.ItemUtils;
import me.pugabyte.nexus.utils.JsonBuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.kyori.adventure.audience.MessageType;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static me.pugabyte.nexus.features.discord.Discord.discordize;
import static me.pugabyte.nexus.utils.StringUtils.stripColor;

@Description("Display an item's enchants in chat")
public class ShowEnchantsCommand extends CustomCommand {

	@Data
	private class ItemData {
		private Map<Enchantment, Integer> enchantsMap = new HashMap<>();
		private List<String> customEnchantsList = new ArrayList<>();;
		private List<String> loreList = new ArrayList<>();;
	}

	public ShowEnchantsCommand(@NonNull CommandEvent event) {
		super(event);
	}

	@Path("<hand|offhand|helmet|chestplate|leggings|boots> [message...]")
	@Permission("showenchants.use")
	@Cooldown(value = @Part(Time.MINUTE), bypass = "showenchants.bypasscooldown")
	void run(String type, String message) {
		Player player = player();
		ItemStack item = getItem(player, type);
		if (ItemUtils.isNullOrAir(item))
			error("You're not holding anything in that slot");

		ItemData data = new ItemData();
		if (message == null) message = "";

		String itemId = item.getType().name();
		if (item.getItemMeta() == null)
			error("Your item has no meta");

		String itemName = item.getItemMeta().getDisplayName();
		String material = getPrettyName(itemId);
		if (isNullOrEmpty(itemName))
			itemName = material;

		int amount = item.getAmount();

		Chatter chatter = new ChatterService().get(player);
		if (!(chatter.getActiveChannel() instanceof PublicChannel))
			error("You can't show enchants in private channels");
		PublicChannel channel = (PublicChannel) chatter.getActiveChannel();

		// Ingame
		ChatColor color = channel.getMessageColor();
		JsonBuilder json = json()
				.next(channel.getChatterFormat(chatter))
				.group()
				.next((isNullOrEmpty(message) ? "" : message + " "))
				.next(color + "&l[" + itemName + color + (amount > 1 ? " x" + amount : "") + "&l]")
				.hover(item);

		Broadcast.ingame().channel(channel).sender(chatter).message(json).messageType(MessageType.CHAT).send();

		// Discord
		if (channel.getDiscordTextChannel() != null) {
			getEnchants(item, data);
			String enchants = getEnchantsDiscord(data);

			String durability = String.valueOf(((int) item.getType().getMaxDurability()) - ((Damageable) item.getItemMeta()).getDamage());
			durability += "/" + item.getType().getMaxDurability();

			String discordName = stripColor(itemName) + " ";
			if (itemName.equalsIgnoreCase(item.getItemMeta().getDisplayName()))
				discordName += "(" + material + ")";
			if (amount > 1) discordName += " x" + amount;

			EmbedBuilder embed = new EmbedBuilder()
					.setTitle(discordName)
					.appendDescription(enchants);

			if (!durability.equals("0/0"))
				embed.setFooter(durability);

			DiscordUser user = new DiscordUserService().get(player);

			String discordMessage = discordize(message);
			discordMessage = IngameBridgeListener.parseMentions(discordMessage);

			MessageBuilder content = new MessageBuilder()
					.setContent(stripColor(user.getBridgeName() + " **>** " + discordMessage))
					.setEmbed(embed.build());

			Discord.send(content, channel.getDiscordTextChannel());
		}
	}

	private void getEnchants(ItemStack item, ItemData data) {
		if (item.getType().equals(Material.ENCHANTED_BOOK)) {
			EnchantmentStorageMeta book = (EnchantmentStorageMeta) item.getItemMeta();
			data.setEnchantsMap(book.getStoredEnchants());
		} else {
			data.setEnchantsMap(item.getEnchantments());
		}
		getCustomEnchants(item, data);
	}

	private String getEnchantsDiscord(ItemData data) {
		StringBuilder string = new StringBuilder();
		for (Map.Entry<Enchantment, Integer> entry : data.getEnchantsMap().entrySet()) {
			String enchant = entry.getKey().getKey().getKey();
			int level = entry.getValue();
			string.append(getEnchantNameAndLevel(enchant, level)).append(System.lineSeparator());
		}
		string.append(getCustomEnchantsDiscord(data));
		return stripColor(string.toString());
	}

	private void getCustomEnchants(ItemStack item, ItemData data) {
		// Check lore
		if (item.getItemMeta().hasLore()) {
			List<String> lore = item.getItemMeta().getLore();
			if (lore == null) return;

			for (String line : lore) {
				// TODO: Figure out a better way to determine custom enchantments, lore can be colored too.
				// if the lore is colored, its a custom enchantment, so add it to the list
				if (line.length() != stripColor(line).length()) {
					data.getCustomEnchantsList().add(line);
				} else {
					// else, it's just lore
					data.getLoreList().add(line);
				}
			}
		}
	}

	private String getCustomEnchantsDiscord(ItemData data) {
		StringBuilder string = new StringBuilder();
		for (String customEnchant : data.getCustomEnchantsList()) {
			string.append(customEnchant).append(System.lineSeparator());
		}
		return string.toString();
	}

	private void checkItem(ItemStack item) throws InvalidInputException {
		if (item == null || item.getType().equals(Material.AIR))
			throw new InvalidInputException("You selected nothing!");
		if (!item.getType().equals(Material.ENCHANTED_BOOK)) {
			if (!item.getItemMeta().hasEnchants()) {
				throw new InvalidInputException("That item doesn't have any enchants!");
			}
		} else {
			EnchantmentStorageMeta book = (EnchantmentStorageMeta) item.getItemMeta();
			if (!book.hasStoredEnchants()) {
				throw new InvalidInputException("That book doesn't have any enchants!");
			}
		}
	}

	private ItemStack getItem(Player player, String arg) throws InvalidInputException {
		ItemStack item = new ItemStack(Material.AIR);
		PlayerInventory inv = player.getInventory();
		switch (arg.toLowerCase()) {
			case "offhand":
				if (!inv.getItemInOffHand().getType().equals(Material.AIR))
					item = inv.getItemInOffHand();
				break;
			case "mainhand":
			case "hand":
				if (!inv.getItemInMainHand().getType().equals(Material.AIR))
					item = inv.getItemInMainHand();
				break;
			case "hat":
			case "head":
			case "helm":
			case "helmet":
				if (inv.getHelmet() != null)
					item = inv.getHelmet();
				break;
			case "chest":
			case "chestplate":
				if (inv.getChestplate() != null)
					item = inv.getChestplate();
				break;
			case "pants":
			case "legs":
			case "leggings":
				if (inv.getLeggings() != null)
					item = inv.getLeggings();
				break;
			case "boots":
			case "feet":
			case "shoes":
				if (inv.getBoots() != null)
					item = inv.getBoots();
				break;
		}
		checkItem(item);
		return item;
	}

	public static String getPrettyName(String item) {
		String out = "";
		if (item.contains("_")) {
			String[] parts = item.split("_");
			for (String part : parts) {
				String temp = part.substring(0, 1).toUpperCase();
				String temp2 = part.substring(1).toLowerCase();
				out += temp + temp2 + " ";
			}
		} else {
			out = item.substring(0, 1).toUpperCase() + item.substring(1).toLowerCase();
		}
		return out.trim();
	}

	static String getEnchantNameAndLevel(String enchantment, int lvl) {
		String level = intToRoman(lvl);
		String enchant = getPrettyName(enchantment);
		return enchant + " " + level;
	}

	static String intToRoman(int num) {
		String[] str = new String[]{"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
		int[] val = new int[]{1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};

		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < val.length; i++) {
			while (num >= val[i]) {
				num -= val[i];
				sb.append(str[i]);
			}
		}
		return sb.toString();
	}

}
