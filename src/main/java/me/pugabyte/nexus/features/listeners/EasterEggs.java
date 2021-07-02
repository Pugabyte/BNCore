package me.pugabyte.nexus.features.listeners;

import com.destroystokyo.paper.ParticleBuilder;
import eden.utils.TimeUtils.Time;
import me.pugabyte.nexus.models.cooldown.CooldownService;
import me.pugabyte.nexus.utils.CitizensUtils;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.SoundBuilder;
import me.pugabyte.nexus.utils.Tasks;
import me.pugabyte.nexus.utils.WorldGroup;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class EasterEggs implements Listener {

	@EventHandler
	public void onPigDamage(EntityDamageByEntityEvent event) {
		if (event.getEntity().getType() != EntityType.PIG)
			return;
		if (!(event.getDamager() instanceof Player player))
			return;

		if (!player.getName().equals("Porkeroni"))
			return;

		if (!new CooldownService().check(player, "pork-pig-easter-egg", Time.SECOND.x(5)))
			return;

		PlayerUtils.send(player, "&d&lPIG > &fFucking traitor.");
		new SoundBuilder(Sound.ENTITY_PIG_DEATH).receiver(player).play();
		Tasks.wait(3, () -> new SoundBuilder(Sound.ENTITY_PIGLIN_DEATH).receiver(player).play());
	}

	@EventHandler
	public void onClickOnPlayer(PlayerInteractEntityEvent event) {
		if (WorldGroup.of(event.getPlayer()).equals(WorldGroup.MINIGAMES))
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

		switch (clicked.getUniqueId().toString()) {
			case "pugabyte" -> griffin(clicker, heldItem, clicked);
			case "wakkaflocka" -> wakka(clicker, heldItem, clicked);
			case "porkeroni" -> dom(clicker, heldItem, clicked);
			case "ravenonacloud" -> raven(clicker, heldItem, clicked);
			case "blast" -> blast(clicker, heldItem, clicked);
		}
	}

	private void griffin(Player player, ItemStack heldItem, Player clicked) {
		if (heldItem.getType() == Material.COOKED_SALMON) {
			removeItem(heldItem);
			eatEffect(player, clicked);
		}
	}

	private void wakka(Player player, ItemStack heldItem, Player clicked) {
		if (heldItem.getType() == Material.REDSTONE) {
			removeItem(heldItem);
			eatEffect(player, clicked);
		}

	}

	private void blast(Player player, ItemStack heldItem, Player clicked) {
		if (heldItem.getType().equals(Material.TNT)) {
			removeItem(heldItem);

			new ParticleBuilder(Particle.EXPLOSION_HUGE)
					.count(10)
					.offset(.5, .5, .5)
					.location(clicked.getLocation())
					.spawn();
			player.getLocation().getWorld().playSound(clicked.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
		}
	}

	private void dom(Player player, ItemStack heldItem, Player clicked) {
		switch (heldItem.getType()) {
			case CARROT, BEETROOT -> {
				removeItem(heldItem);
				eatEffect(player, clicked, Sound.ENTITY_GENERIC_EAT, () ->
						player.getWorld().playSound(clicked.getLocation(), Sound.ENTITY_PIG_AMBIENT, 0.5F, 1F));
			}
		}
	}

	private void raven(Player player, ItemStack heldItem, Player clicked) {
		switch (heldItem.getType()) {
			case WHEAT_SEEDS, BEETROOT_SEEDS, MELON_SEEDS, PUMPKIN_SEEDS -> {
				removeItem(heldItem);
				eatEffect(player, clicked, Sound.ENTITY_PARROT_EAT, () ->
						player.getWorld().playSound(clicked.getLocation(), Sound.ENTITY_PARROT_AMBIENT, 0.5F, 1F));
			}
		}
	}

	// helpers

	private void removeItem(ItemStack heldItem) {
		heldItem.setAmount(heldItem.getAmount() - 1);
	}

	private void eatEffect(Player player, Player clicked) {
		eatEffect(player, clicked, Sound.ENTITY_GENERIC_EAT, () -> player.getWorld().playSound(clicked.getLocation(), Sound.ENTITY_PLAYER_BURP, 0.5F, 1F));
	}

	private void eatEffect(Player player, Player clicked, Sound sound, Runnable result) {
		clicked.setFoodLevel(clicked.getFoodLevel() + 2);

		World world = player.getWorld();
		Location location = clicked.getLocation();
		world.playSound(location, sound, 0.5F, 1F);
		Tasks.wait(4, () -> world.playSound(location, sound, 0.5F, 1F));
		Tasks.wait(8, () -> world.playSound(location, sound, 0.5F, 1F));
		Tasks.wait(12, () -> world.playSound(location, sound, 0.5F, 1F));
		Tasks.wait(16, () -> world.playSound(location, sound, 0.5F, 1F));

		Tasks.wait(20, result);
	}
}
