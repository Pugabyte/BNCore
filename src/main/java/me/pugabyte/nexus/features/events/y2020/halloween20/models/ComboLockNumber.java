package me.pugabyte.nexus.features.events.y2020.halloween20.models;

import lombok.Getter;
import me.pugabyte.nexus.features.events.y2020.halloween20.Halloween20;
import me.pugabyte.nexus.features.events.y2020.halloween20.quest.menus.Halloween20Menus;
import me.pugabyte.nexus.models.halloween20.Halloween20Service;
import me.pugabyte.nexus.models.halloween20.Halloween20User;
import me.pugabyte.nexus.utils.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import static me.pugabyte.nexus.features.events.y2020.halloween20.Halloween20.PREFIX;

public enum ComboLockNumber {

	PUZZLE_TWO(2, new Location(Bukkit.getWorld("safepvp"), 327.00, 56.00, -1956.00, .00F, .00F), 2) {
		@Override
		public void onFind(Player player) {
			if (new Halloween20Service().get(player).getCombinationStage() == QuestStage.Combination.STARTED)
				Halloween20Menus.openPicturePuzzle(player, this);
		}
	},
	ZERO(0, new Location(Bukkit.getWorld("safepvp"), 319.00, 60.00, -1924.00, .00F, .00F), 0),
	SEVEN(7, new Location(Bukkit.getWorld("safepvp"), 364.00, 113.00, -1919.00, .00F, .00F), 5),
	PUZZLE_EIGHT(8, new Location(Bukkit.getWorld("safepvp"), 394.00, 103.00, -1952.00, .00F, .00F), 6) {
		@Override
		public void onFind(Player player) {
			if (new Halloween20Service().get(player).getCombinationStage() == QuestStage.Combination.STARTED)
				Halloween20Menus.openPicturePuzzle(player, this);
		}
	},
	ONE(1, new Location(Bukkit.getWorld("safepvp"), 374.00, 112.00, -1963.00, .00F, .00F), 1),
	PUZZLE_EIGHT_2(8, new Location(Bukkit.getWorld("safepvp"), 368.00, 152.00, -1933.00, .00F, .00F), 6) {
		@Override
		public void onFind(Player player) {
			if (new Halloween20Service().get(player).getCombinationStage() == QuestStage.Combination.STARTED)
				Halloween20Menus.openFlashCardPuzzle(player, this);
		}
	},
	ONE_2(1, new Location(Bukkit.getWorld("safepvp"), 329.00, 163.00, -1949.00, .00F, .00F), 1),
	SIX(6, new Location(Bukkit.getWorld("safepvp"), 391.00, 154.00, -1958.00, .00F, .00F), 4),
	ONE_3(1, new Location(Bukkit.getWorld("safepvp"), 285.00, 223.00, -1949.00, .00F, .00F), 1),
	THREE(3, new Location(Bukkit.getWorld("safepvp"), 309.00, 202.00, -1925.00, .00F, .00F), 3),
	PUZZLE_ZERO(0, new Location(Bukkit.getWorld("safepvp"), 326.00, 216.00, -1949.00, .00F, .00F), 0) {
		@Override
		public void onFind(Player player) {
			if (new Halloween20Service().get(player).getCombinationStage() == QuestStage.Combination.STARTED)
				Halloween20Menus.openFlashCardPuzzle(player, this);
		}
	};

	@Getter
	int numericalValue;
	@Getter
	Location loc;
	int yOff;

	ComboLockNumber(int numericalValue, Location loc, int yOff) {
		this.numericalValue = numericalValue;
		this.loc = loc;
		this.yOff = yOff;
	}

	public ItemStack getItem() {
		return new Location(Bukkit.getWorld("safepvp"), 279.00, 4.00, -1784.00, .00F, .00F)
				.add(0, yOff, 0).getBlock().getDrops().stream().findFirst().orElse(null);
	}

	public void onFind(Player player) {
		Halloween20Service service = new Halloween20Service();
		Halloween20User user = service.get(player);
		switch (user.getCombinationStage()) {
			case NOT_STARTED -> PlayerUtils.send(player, PREFIX + "This looks like a number from the combination lock close to where I entered this place...");
			case STARTED -> {
				if (user.getFoundComboLockNumbers().contains(this)) {
					PlayerUtils.send(player, PREFIX + "You already know of this number. Maybe there's some more.");
					break;
				}
				user.getFoundComboLockNumbers().add(this);
				PlayerUtils.send(player, PREFIX + "&e" + this.getNumericalValue() + "&3 can now be used on the combination lock at the entrance to the city.");
				service.save(user);
				player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1f);
				if (user.getFoundComboLockNumbers().size() == 11)
					PlayerUtils.send(player, Halloween20.PREFIX + "You have found all the numbers for the combination lock. Return to see if you can crack the code!");
			}
		}
	}

	public static ComboLockNumber getByLocation(Location loc) {
		for (ComboLockNumber number : values()) {
			if (loc.equals(number.loc))
				return number;
		}
		return null;
	}

}
