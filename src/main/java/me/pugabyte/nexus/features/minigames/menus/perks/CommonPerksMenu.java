package me.pugabyte.nexus.features.minigames.menus.perks;

import lombok.Getter;
import me.pugabyte.nexus.features.menus.MenuUtils;
import me.pugabyte.nexus.features.minigames.Minigames;
import me.pugabyte.nexus.features.minigames.models.perks.Perk;
import me.pugabyte.nexus.features.minigames.models.perks.PerkOwner;
import me.pugabyte.nexus.features.minigames.models.perks.PerkOwnerService;
import me.pugabyte.nexus.features.minigames.models.perks.PerkType;
import me.pugabyte.nexus.features.minigames.models.perks.common.LoadoutPerk;
import me.pugabyte.nexus.models.nerd.Nerd;
import me.pugabyte.nexus.utils.ItemBuilder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static me.pugabyte.nexus.utils.StringUtils.colorize;
import static me.pugabyte.nexus.utils.StringUtils.loreize;
import static me.pugabyte.nexus.utils.StringUtils.splitLore;

public abstract class CommonPerksMenu extends MenuUtils {
	public static final PerkOwnerService service = new PerkOwnerService();

	protected static List<String> getLore(Player player, Perk perk) {
		List<String> lore = new ArrayList<>();
		lore.add("&e"+perk.getPerkCategory().toString());
		lore.addAll(splitLore(loreize("&3" + String.format(perk.getDescription(), Nerd.of(player).getNickname()))));
		return lore;
	}

	protected static ItemStack getItem(Perk perk, List<String> lore) {
		try {
			return new ItemBuilder(perk.getMenuItem()).name("&b" + perk.getName()).lore(lore).build();
		} catch (LoadoutPerk.IncompleteLoadout e) {
			throw new IllegalArgumentException(perk.getName() + " has no valid menu item");
		}
	}

	protected static void send(Player player, String message) {
		player.sendMessage(Minigames.PREFIX + colorize(message));
	}

	@Getter
	protected static class PerkSortWrapper {
		private final boolean owned;
		private final int category;
		private final String name;
		private final PerkType perkType;
		private final int price;

		public PerkSortWrapper(boolean owned, PerkType perkType) {
			this.owned = owned;
			category = perkType.getPerkCategory().ordinal();
			name = perkType.getPerk().getName();
			this.perkType = perkType;
			price = -1 * perkType.getPerk().getPrice(); // lazy reversal lol
		}

		public static PerkSortWrapper of(PerkOwner owner, PerkType perkType) {
			return new PerkSortWrapper(owner.getPurchasedPerks().containsKey(perkType), perkType);
		}
	}
}