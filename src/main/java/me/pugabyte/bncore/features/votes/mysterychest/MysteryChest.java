package me.pugabyte.bncore.features.votes.mysterychest;

import fr.minuskube.inv.SmartInventory;
import lombok.Getter;
import lombok.SneakyThrows;
import me.pugabyte.bncore.BNCore;
import me.pugabyte.bncore.features.menus.rewardchests.RewardChestLoot;
import me.pugabyte.bncore.models.mysterychest.MysteryChestService;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class MysteryChest {
	private final MysteryChestService service = new MysteryChestService();
	private final OfflinePlayer player;
	@Getter
	private static final String fileName = "mysteryChestLoot.yml";
	@Getter
	private static YamlConfiguration config;

	static {
		reloadConfig();
	}

	public MysteryChest(OfflinePlayer player) {
		this.player = player;
	}

	public static SmartInventory getInv(Integer id) {
		return SmartInventory.builder()
				.title("Mystery Chest Rewards")
				.provider(new MysteryChestEditProvider(id))
				.size(id == null ? 6 : 3, 9)
				.build();
	}

	public int give(int amount) {
		me.pugabyte.bncore.models.mysterychest.MysteryChest mysteryChest = getMysteryChest();
		mysteryChest.setAmount(mysteryChest.getAmount() + amount);
		service.save(mysteryChest);
		return mysteryChest.getAmount();
	}

	public int take(int amount) {
		me.pugabyte.bncore.models.mysterychest.MysteryChest mysteryChest = getMysteryChest();
		mysteryChest.setAmount(mysteryChest.getAmount() - amount);
		service.save(mysteryChest);
		return mysteryChest.getAmount();
	}

	public me.pugabyte.bncore.models.mysterychest.MysteryChest getMysteryChest() {
		return service.get(player);
	}

	public static void reloadConfig() {
		config = BNCore.getConfig(fileName);
	}

	@SneakyThrows
	public static void saveConfig() {
		config.save(BNCore.getFile(fileName));
	}

	public static Set<String> getConfigSections() {
		return config.getKeys(false);
	}

	public static int getNextId() {
		int id = 0;
		Set<String> sections = getConfigSections();
		if (sections.size() == 0) return id;
		for (String section : sections) {
			try {
				int savedId = Integer.parseInt(section);
				if (savedId >= id) id = savedId + 1;
			} catch (Exception ex) {
				BNCore.warn("An error occurred while trying to save a Mystery Chest to file");
				ex.printStackTrace();
			}
		}
		return id;
	}

	public static RewardChestLoot[] getActiveRewards() {
		return Arrays.stream(getAllRewards()).filter(RewardChestLoot::isActive).collect(Collectors.toList()).toArray(new RewardChestLoot[0]);
	}

	public static RewardChestLoot getRewardChestLoot(int id) {
		return (RewardChestLoot) config.get(id + "");
	}

	public static RewardChestLoot[] getAllRewards() {
		RewardChestLoot[] loot = new RewardChestLoot[getConfigSections().size()];
		int i = 0;
		for (String section : getConfigSections()) {
			RewardChestLoot reward = (RewardChestLoot) config.get(section);
			reward.setId(Integer.parseInt(section));
			loot[i] = reward;
			i++;
		}
		return loot;
	}

}
