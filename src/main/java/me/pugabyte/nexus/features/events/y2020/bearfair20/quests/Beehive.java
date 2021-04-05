package me.pugabyte.nexus.features.events.y2020.bearfair20.quests;

import com.mewin.worldguardregionapi.events.RegionEnteredEvent;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.events.y2020.bearfair20.BearFair20;
import me.pugabyte.nexus.features.events.y2020.bearfair20.islands.MainIsland;
import me.pugabyte.nexus.models.bearfair.BearFairService;
import me.pugabyte.nexus.models.bearfair.BearFairUser;
import me.pugabyte.nexus.utils.ItemUtils;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.Tasks;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collections;

import static me.pugabyte.nexus.features.events.y2020.bearfair20.BearFair20.getWGUtils;
import static me.pugabyte.nexus.features.events.y2020.bearfair20.BearFair20.send;
import static me.pugabyte.nexus.features.events.y2020.bearfair20.quests.BFQuests.chime;

// NPC BEES: 2730, 2731
public class Beehive implements Listener {
	private String allowedMsg = "The defending swarm seems calmed by the flower's pleasant aroma. The queen beckons you to enter.";
	private String deniedMsg = "The swarming hoards of bees seem disturbed by your presence. Perhaps a peace offering would calm them, maybe their most favorite flower?";
	private Location enterLoc = new Location(BearFair20.getWorld(), -1084, 135, -1548, 228, 20);
	private Location exitLoc = new Location(BearFair20.getWorld(), -1088, 136, -1548, 40, 0);
	private ItemStack key = MainIsland.rareFlower.clone();
	public static String beehiveRg = "bearfair2020_beehive";
	private String enterRg = beehiveRg + "_enter";
	private String exitRg = beehiveRg + "_exit";
	private String queenRg = beehiveRg + "_queen";

	public Beehive() {
		Nexus.registerListener(this);
	}

	@EventHandler
	public void onHiveRegionsEnter(RegionEnteredEvent event) {
		String id = event.getRegion().getId();
		Player player = event.getPlayer();

		if (id.equalsIgnoreCase(enterRg)) {
			BearFairService service = new BearFairService();
			BearFairUser user = service.get(player);
			if (user.isQuest_Hive_Access())
				allowed(player);
			else {
				if (player.getInventory().contains(key)) {
					allowed(player);
				} else {
					denied(player);
				}
			}

		} else if (id.equalsIgnoreCase(exitRg)) {
			player.teleport(exitLoc);

		} else if (id.equalsIgnoreCase(queenRg)) {
			String queen = "&3Queen Bee &7> &f";
			String you = "&b&lYOU &7> &f";
			ItemStack honeyBottle = getHoneyBottle(player);
			ItemStack rareFlower = getRareFlower(player);

			if (honeyBottle == null && rareFlower != null) {
				send(queen + "What brings you to my grand halls, traveler?", player);
				player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1F, 1F);

				Tasks.wait(80, () -> {
					send(you + "I humbly request a blessing of honey from you, your grace, " +
							"so I may make the greatest stroopwafel.", player);
					player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1F, 1F);
				});

				Tasks.wait(160, () -> {
					send(queen + "I would gladly give you what you seek, but alas, your timing " +
							"is poor for we are currently building our honey reserves for the winter and the new generation. " +
							"If you would bring me a bottle of honey from the surface, I can bless it for you.", player);
					player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1F, 1F);
				});

				Tasks.wait(240, () -> {
					send(you + "Of course, I will return soon.", player);
					player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1F, 1F);
				});
			} else if (honeyBottle != null && rareFlower != null) {
				player.getInventory().remove(MainIsland.rareFlower.clone());
				player.getInventory().remove(honeyBottle);

				send(queen + "May this blessing grant you a divine Stroopwafel. Now please, feel free to visit any time. " +
						"You've shown yourself to be a benefactor in the ways of the golden nectar.", player);
				player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1F, 1F);

				BearFairService service = new BearFairService();
				BearFairUser user = service.get(player);
				user.setQuest_Hive_Access(true);
				MainIsland.setStep(player, 3);
				service.save(user);

				Tasks.wait(60, () -> {
					PlayerUtils.giveItem(player, MainIsland.blessedHoneyBottle.clone());
					chime(player);
				});
			}
		}
	}

	@EventHandler
	public void onUseFlower(PlayerInteractEntityEvent event) {
		if (event.getHand() != EquipmentSlot.HAND) return;

		ProtectedRegion region = getWGUtils().getProtectedRegion(BearFair20.getRegion());
		if (!getWGUtils().getRegionsAt(event.getPlayer().getLocation()).contains(region)) return;

		ItemStack tool = ItemUtils.getTool(event.getPlayer());
		if (!BearFair20.isBFItem(tool)) return;

		if (event.getRightClicked().getType().equals(EntityType.BEE)) {
			event.setCancelled(true);
			return;
		}

		if (tool.equals(MainIsland.rareFlower))
			event.setCancelled(true);
	}

	private void allowed(Player player) {
		player.teleport(enterLoc);
		send(allowedMsg, player);
	}

	private void denied(Player player) {
		player.addPotionEffects(Collections.singletonList
				(new PotionEffect(PotionEffectType.BLINDNESS, 40, 250, false, false, false)));
		player.teleport(exitLoc);
		player.playSound(enterLoc, Sound.ENTITY_BEE_LOOP_AGGRESSIVE, 0.5F, 1F);
		Tasks.wait(5, () -> send(deniedMsg, player));
	}

	private ItemStack getHoneyBottle(Player player) {
		for (ItemStack itemStack : player.getInventory()) {
			if (!BearFair20.isBFItem(itemStack)) continue;
			if (!itemStack.getType().equals(Material.HONEY_BOTTLE)) continue;
			return itemStack;
		}
		return null;
	}

	private ItemStack getRareFlower(Player player) {
		for (ItemStack itemStack : player.getInventory()) {
			if (!BearFair20.isBFItem(itemStack)) continue;
			if (!itemStack.getType().equals(Material.BLUE_ORCHID)) continue;
			return itemStack;
		}
		return null;
	}

}
