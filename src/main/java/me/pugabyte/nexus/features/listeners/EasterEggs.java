package me.pugabyte.nexus.features.listeners;

import me.pugabyte.nexus.utils.CitizensUtils;
import me.pugabyte.nexus.utils.ItemBuilder;
import me.pugabyte.nexus.utils.Tasks;
import me.pugabyte.nexus.utils.WorldGroup;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class EasterEggs implements Listener {

	@EventHandler
	public void onClickOnPlayer(PlayerInteractEntityEvent event) {
		if (WorldGroup.get(event.getPlayer()).equals(WorldGroup.MINIGAMES))
			return;

		if (!event.getRightClicked().getType().equals(EntityType.PLAYER))
			return;

		if (CitizensUtils.isNPC(event.getRightClicked()))
			return;

		if (!event.getHand().equals(EquipmentSlot.HAND))
			return;

		Player clicked = (Player) event.getRightClicked();
		Player clicker = event.getPlayer();
		ItemStack heldItem = clicker.getInventory().getItemInMainHand();

		switch (clicked.getName().toLowerCase()) {
			case "pugabyte":
			case "vargskati":
				pug(clicker, heldItem, clicked);
				break;
			case "wakkaflocka":
				wakka(clicker, heldItem, clicked);
				break;
			case "porkeroni":
				pork(clicker, heldItem, clicked);
				break;
			case "ravenonacloud":
			case "chaioty":
				raven(clicker, heldItem, clicked);
				break;
			case "warrior_tark":
				tark(clicker, heldItem, clicked);
				break;
		}

	}

	private void pug(Player player, ItemStack heldItem, Player clicked) {
		switch (heldItem.getType()) {
			case BONE:
			case PORKCHOP:
			case COOKED_PORKCHOP:
			case BEEF:
			case COOKED_BEEF:
			case CHICKEN:
			case COOKED_CHICKEN:
			case RABBIT:
			case COOKED_RABBIT:
				heldItem.setAmount(heldItem.getAmount() - 1);

				eatSound(player, clicked.getLocation(), Sound.ENTITY_GENERIC_EAT);
				addFoodLevel(clicked);
				Tasks.wait(20, () -> player.getWorld().playSound(clicked.getLocation(), Sound.ENTITY_WOLF_AMBIENT, 0.5F, 1F));
				break;
			case ROTTEN_FLESH:
				player.getWorld().playSound(clicked.getLocation(), Sound.ENTITY_WOLF_GROWL, 0.5F, 1F);
				break;
		}
	}

	private void wakka(Player player, ItemStack heldItem, Player clicked) {
		switch (heldItem.getType()) {
			case BUCKET:
				heldItem.setAmount(heldItem.getAmount() - 1);

				player.getWorld().playSound(clicked.getLocation(), Sound.ENTITY_COW_MILK, 0.5F, 1F);
				Tasks.wait(4, () -> player.getWorld().playSound(clicked.getLocation(), Sound.ENTITY_COW_AMBIENT, 0.5F, 1F));

				ItemStack milkBucket = new ItemBuilder(Material.MILK_BUCKET).lore("Wakka's milk").build();
				player.getInventory().addItem(milkBucket);
				break;
			case WHEAT:
				heldItem.setAmount(heldItem.getAmount() - 1);

				eatSound(player, clicked.getLocation(), Sound.ENTITY_GENERIC_EAT);
				addFoodLevel(clicked);
				Tasks.wait(20, () -> player.getWorld().playSound(clicked.getLocation(), Sound.ENTITY_COW_AMBIENT, 0.5F, 1F));
				break;
		}

	}

	private void pork(Player player, ItemStack heldItem, Player clicked) {
		switch (heldItem.getType()) {
			case CARROT:
			case BEETROOT:
				heldItem.setAmount(heldItem.getAmount() - 1);

				eatSound(player, clicked.getLocation(), Sound.ENTITY_GENERIC_EAT);
				addFoodLevel(clicked);
				Tasks.wait(20, () -> player.getWorld().playSound(clicked.getLocation(), Sound.ENTITY_PIG_AMBIENT, 0.5F, 1F));
				break;
		}
	}

	private void raven(Player player, ItemStack heldItem, Player clicked) {
		switch (heldItem.getType()) {
			case WHEAT_SEEDS:
			case BEETROOT_SEEDS:
			case MELON_SEEDS:
			case PUMPKIN_SEEDS:
				heldItem.setAmount(heldItem.getAmount() - 1);

				eatSound(player, clicked.getLocation(), Sound.ENTITY_PARROT_EAT);
				addFoodLevel(clicked);
				Tasks.wait(20, () -> player.getWorld().playSound(clicked.getLocation(), Sound.ENTITY_PARROT_AMBIENT, 0.5F, 1F));
				break;
		}
	}

	private void tark(Player player, ItemStack heldItem, Player clicked) {
		if (heldItem.getType().equals(Material.STONE)) {
			heldItem.setAmount(heldItem.getAmount() - 1);

			eatSound(player, clicked.getLocation(), Sound.ENTITY_PARROT_EAT);
			addFoodLevel(clicked);
			Tasks.wait(20, () -> player.getWorld().playSound(clicked.getLocation(), Sound.ENTITY_RAVAGER_CELEBRATE, 0.5F, 0.1F));
		}
	}

	private void eatSound(Player player, Location location, Sound sound) {
		World world = player.getWorld();
		world.playSound(location, sound, 0.5F, 1F);
		Tasks.wait(4, () -> world.playSound(location, sound, 0.5F, 1F));
		Tasks.wait(8, () -> world.playSound(location, sound, 0.5F, 1F));
		Tasks.wait(12, () -> world.playSound(location, sound, 0.5F, 1F));
		Tasks.wait(16, () -> world.playSound(location, sound, 0.5F, 1F));
	}

	private void addFoodLevel(Player clicked) {
		int foodlvl = clicked.getFoodLevel();
		clicked.setFoodLevel(foodlvl + 2);
	}
}