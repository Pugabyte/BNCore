package me.pugabyte.nexus.features.crates.models;

import fr.minuskube.inv.SmartInventory;
import lombok.Getter;
import me.pugabyte.nexus.features.crates.Crates;
import me.pugabyte.nexus.features.crates.crates.BearFair21Crate;
import me.pugabyte.nexus.features.crates.crates.BossCrate;
import me.pugabyte.nexus.features.crates.crates.FebVoteRewardCrate;
import me.pugabyte.nexus.features.crates.crates.MysteryCrate;
import me.pugabyte.nexus.features.crates.crates.VoteCrate;
import me.pugabyte.nexus.features.crates.crates.WeeklyWakkaCrate;
import me.pugabyte.nexus.features.crates.menus.CratePreviewProvider;
import me.pugabyte.nexus.models.mail.Mailer.Mail;
import me.pugabyte.nexus.utils.ItemBuilder;
import me.pugabyte.nexus.utils.ItemUtils;
import me.pugabyte.nexus.utils.LocationUtils;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.StringUtils;
import me.pugabyte.nexus.utils.WorldGroup;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@Getter
public enum CrateType {
	ALL(null, null),
	VOTE(new VoteCrate(), new Location(Bukkit.getWorld("survival"), 8.00, 15.00, 11.00, .00F, .00F)),
	MYSTERY(new MysteryCrate(), new Location(Bukkit.getWorld("survival"), 11.00, 15.00, 8.00, .00F, .00F)),
	WEEKLY_WAKKA(new WeeklyWakkaCrate(), new Location(Bukkit.getWorld("survival"), 15.00, 15.00, -8.00, .00F, .00F)),
	FEB_VOTE_REWARD(new FebVoteRewardCrate(), null),
	BOSS(new BossCrate(), new Location(Bukkit.getWorld("survival"), -9.00, 15.00, 12.00, .00F, .00F)),
	BEAR_FAIR_21(new BearFair21Crate(), new Location(Bukkit.getWorld("survival"), -12.00, 15.00, 9.00, .00F, .00F));

	Crate crateClass;
	Location location;

	CrateType(Crate crateClass, Location location) {
		this.crateClass = crateClass;
		this.location = location;
	}

	public Location getCenteredLocation() {
		return LocationUtils.getCenteredLocation(this.location.clone());
	}

	@Getter
	private final ItemStack key = new ItemBuilder(Material.TRIPWIRE_HOOK).name("&eCrate Key").glow()
			.lore(" ").lore("&3Type: &e" + StringUtils.camelCase(name()))
			.lore("&7Use me on the Crate at").lore("&7spawn to receive a reward").build();

	public ItemStack getKey() {
		return key.clone();
	}

	public static CrateType fromLocation(Location location) {
		for (CrateType type : values())
			if (location.equals(type.location))
				return type;
		return null;
	}

	public static CrateType fromKey(ItemStack item) {
		if (ItemUtils.isNullOrAir(item)) return null;
		for (CrateType type : values())
			if (ItemUtils.isFuzzyMatch(item, type.getKey()))
				return type;
		return null;
	}

	public SmartInventory previewDrops(CrateLoot loot) {
		return SmartInventory.builder()
				.size(6, 9)
				.provider(new CratePreviewProvider(this, loot))
				.title(StringUtils.camelCase(name()) + " Crate Rewards")
				.build();
	}

	public void give(OfflinePlayer player) {
		give(player, 1);
	}

	public boolean giveVPS(Player player, int amount) {
		ItemStack item = getKey().clone();
		item.setAmount(amount);

		if (WorldGroup.of(player) != WorldGroup.SURVIVAL) {
			PlayerUtils.send(player, "&cYou must be in survival to buy this");
			return false;
		}

		if (!PlayerUtils.hasRoomFor(player.getPlayer(), item)) {
			PlayerUtils.send(player, "&cYou don't have enough space in your inventory");
			return false;
		}

		player.getInventory().addItem(item);
		return true;
	}

	public void give(OfflinePlayer player, int amount) {
		ItemStack item = getKey().clone();
		item.setAmount(amount);
		if (player.isOnline() && player.getPlayer() != null && WorldGroup.of(player.getPlayer()) == WorldGroup.SURVIVAL && PlayerUtils.hasRoomFor(player.getPlayer(), item))
			player.getPlayer().getInventory().addItem(item);
		else {
			Mail.fromServer(player.getUniqueId(), WorldGroup.SURVIVAL, item).send();

			if (player.isOnline()) {
				String error = WorldGroup.of(player.getPlayer()) == WorldGroup.SURVIVAL ? "&3 but your inventory was full. Use &c/delivery &3to claim it." : "&3. Use &c/delivery&3 in the survival world to claim it.";
				PlayerUtils.send(player.getPlayer(), Crates.PREFIX + "You have been given &e" + amount + " " +
						StringUtils.camelCase(name()) + " Crate Key" + ((amount > 1) ? "s" : "") + error);
			}
		}
	}

}
