package me.pugabyte.nexus.features.events.y2021.bearfair21.fairgrounds.minigolf;

import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.utils.ActionBarUtils;
import me.pugabyte.nexus.utils.BlockUtils;
import me.pugabyte.nexus.utils.ItemUtils;
import me.pugabyte.nexus.utils.MaterialTag;
import me.pugabyte.nexus.utils.Time;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

public class PuttListener implements Listener {

	public PuttListener() {
		Nexus.registerListener(this);
	}

	@EventHandler
	public void onPutt(PlayerInteractEvent event) {
		if (isInteracting(event))
			return;

		ItemStack item = event.getItem();
		if (ItemUtils.isNullOrAir(item))
			return;

		// quick fix
		ItemStack clone = item.clone();
		clone.setAmount(1);
		boolean stop = true;
		for (ItemStack _item : MiniGolf.getKit()) {
			if (ItemUtils.isFuzzyMatch(clone, _item))
				stop = false;
		}
		if (stop)
			return;
		//

		// Get info
		Player player = event.getPlayer();
		World world = player.getWorld();
		Action action = event.getAction();
		Block block = event.getClickedBlock();
		ItemMeta meta = item.getItemMeta();

		// Get type of golf club
		boolean putter = MiniGolf.hasKey(meta, MiniGolf.getPutterKey());
		boolean wedge = MiniGolf.hasKey(meta, MiniGolf.getWedgeKey());

		if (putter || wedge) {
			// Cancel original tool
			event.setCancelled(true);

			// Find entities
			List<Entity> entities = player.getNearbyEntities(5.5, 5.5, 5.5);

			Location eye = player.getEyeLocation();
			Vector dir = eye.getDirection();
			Vector loc = eye.toVector();

			for (Entity entity : entities) {
				// Look for golf balls
				PersistentDataContainer c = entity.getPersistentDataContainer();

				if (entity instanceof Snowball && c.has(MiniGolf.getParKey(), PersistentDataType.INTEGER)) {
					// Is golf ball in player's view?
					Location entityLoc = entity.getLocation();
					Vector vec = entityLoc.toVector().subtract(loc);

					if (dir.angle(vec) < 0.15f) {
						// Are we allowed to hit this ball?
						boolean skip = false;
						for (Entry<UUID, Snowball> entry : MiniGolf.getLastPlayerBall().entrySet()) {
							// Find the ball and check the owner
							if (entry.getValue().equals(entity) && !entry.getKey().equals(player.getUniqueId())) {
								skip = true;
								break;
							}
						}
						if (skip)
							continue;

						// Are we hitting or picking up the golf ball?
						if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
							// Hit golf ball
							dir.setY(0).normalize();

							double power = player.getExp();
							if (power >= 0.90)
								power = 1.0;
							else if (power < 0.16)
								power = 0.16;

							dir.multiply(power);
							if (wedge)
								dir.setY(0.25);

							String color = "&a";
							if (power >= 0.7)
								color = "&c";
							else if (power >= 0.5)
								color = "&e";

							DecimalFormat df = new DecimalFormat("#0.00");
							ActionBarUtils.sendActionBar(player, "&6Power: " + color + df.format(power), Time.SECOND.x(3));
							entity.setVelocity(dir);

							// Update par
							int par = c.get(MiniGolf.getParKey(), PersistentDataType.INTEGER) + 1;
							c.set(MiniGolf.getParKey(), PersistentDataType.INTEGER, par);
							entity.setCustomName("Par " + par);

							// Update last pos
							c.set(MiniGolf.getXKey(), PersistentDataType.DOUBLE, entityLoc.getX());
							c.set(MiniGolf.getYKey(), PersistentDataType.DOUBLE, entityLoc.getY());
							c.set(MiniGolf.getZKey(), PersistentDataType.DOUBLE, entityLoc.getZ());

							// Add to map
							MiniGolf.getGolfBalls().add((Snowball) entity);
							entity.setTicksLived(1);

							world.playSound(entityLoc, Sound.BLOCK_METAL_HIT, 0.75f, 1.25f);

						} else if (entity.isValid()) {
							// Give golf ball
							entity.remove();
							MiniGolf.giveBall(player);
						}
					}
				}
			}
		} else if (MiniGolf.hasKey(meta, MiniGolf.getBallKey())) {
			// Cancel original tool
			event.setCancelled(true);

			// Is player placing golf ball?
			if (action == Action.RIGHT_CLICK_BLOCK) {

				// Is placing on start position
				if (BlockUtils.isNullOrAir(block) || block.getType() != Material.GREEN_WOOL)
					return;

				// Get spawn location
				Location loc;
				if (MiniGolf.isBottomSlab(block))
					loc = block.getLocation().add(0.5, 0.5 + MiniGolf.getFloorOffset(), 0.5);
				else
					loc = block.getLocation().add(0.5, 1 + MiniGolf.getFloorOffset(), 0.5);

				// Spawn golf ball and set data
				Snowball ball = (Snowball) world.spawnEntity(loc, EntityType.SNOWBALL);

				ball.setGravity(false);

				PersistentDataContainer c = ball.getPersistentDataContainer();
				c.set(MiniGolf.getXKey(), PersistentDataType.DOUBLE, loc.getX());
				c.set(MiniGolf.getYKey(), PersistentDataType.DOUBLE, loc.getY());
				c.set(MiniGolf.getZKey(), PersistentDataType.DOUBLE, loc.getZ());
				c.set(MiniGolf.getParKey(), PersistentDataType.INTEGER, 0);

				ball.setCustomName("Par 0");
				ball.setCustomNameVisible(true);

				MiniGolf.getGolfBalls().add(ball);

				// Remove golf ball from inventory
				ItemStack itemInHand = event.getItem();
				itemInHand.setAmount(itemInHand.getAmount() - 1);

				// Add last player ball
				MiniGolf.getLastPlayerBall().put(player.getUniqueId(), ball);
			}
		} else if (MiniGolf.hasKey(meta, MiniGolf.getWhistleKey())) {
			event.setCancelled(true);
			// Return ball
			if (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR) {
				// Get last player ball
				Snowball ball = MiniGolf.getLastPlayerBall().get(player.getUniqueId());
				if (ball == null || !ball.isValid()) {
					// Clean up
					MiniGolf.getLastPlayerBall().remove(player.getUniqueId());
					return;
				}

				PersistentDataContainer container = ball.getPersistentDataContainer();

				// Read persistent data
				double x = container.get(MiniGolf.getXKey(), PersistentDataType.DOUBLE);
				double y = container.get(MiniGolf.getYKey(), PersistentDataType.DOUBLE);
				double z = container.get(MiniGolf.getZKey(), PersistentDataType.DOUBLE);

				// Move ball to last location
				ball.setVelocity(new Vector(0, 0, 0));
				ball.teleport(new Location(world, x, y, z));
				ball.setGravity(false);

				// Sound
				world.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.9f, 1.9f);
			}
		}

	}

	// TODO: better way?
	public boolean isInteracting(PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getPlayer().isSneaking())
			return false;

		Block block = event.getClickedBlock();
		return MaterialTag.INTERACTABLES.isTagged(block != null ? block.getType() : null);
	}

}
