package me.pugabyte.nexus.features.listeners;

import eden.utils.TimeUtils.Time;
import me.pugabyte.nexus.features.minigames.Minigames;
import me.pugabyte.nexus.utils.ItemUtils;
import me.pugabyte.nexus.utils.MaterialTag;
import me.pugabyte.nexus.utils.Tasks;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.material.MaterialData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static me.pugabyte.nexus.utils.LocationUtils.getCenteredLocation;

public class LaunchPads implements Listener {
	private static final Map<Player, Integer> taskIDs = new HashMap<>();
	private static final Map<Player, FallingBlock> launchPadPlayers = new HashMap<>();
	private static final List<UUID> launchPadBlockUUIDs = new ArrayList<>();

	@EventHandler
	public void onPressurePlatePress(PlayerInteractEvent event) {
		Block block = event.getClickedBlock();
		if (block == null) return;
		if (!event.getAction().equals(Action.PHYSICAL)) return;
		if (!block.getType().equals(Material.LIGHT_WEIGHTED_PRESSURE_PLATE)) return;

		Player player = event.getPlayer();
		if (player.isSneaking()) return;

		Block below = block.getRelative(0, -1, 0).getLocation().getBlock();
		if (!(below.getType().equals(Material.REDSTONE_ORE))) return;

		event.setCancelled(true);
		event.setUseInteractedBlock(Event.Result.DENY);

		Block belowBelow = below.getRelative(0, -1, 0).getLocation().getBlock();
		Material belowBelowType = belowBelow.getType();

		if (!ItemUtils.isNullOrAir(belowBelowType) && MaterialTag.SIGNS.isTagged(belowBelowType)) {
			Sign sign = (Sign) belowBelow.getState();
			String[] lines = sign.getLines();

			if (!lines[0].equalsIgnoreCase("[LaunchPad]")) return;
			if (lines[1].equalsIgnoreCase("") || lines[2].equalsIgnoreCase("")) return;

			double power = Double.parseDouble(lines[1]);
			double angle = Double.parseDouble(lines[2]);

			if (!lines[3].equalsIgnoreCase("")) {
				double direction = Double.parseDouble(lines[3]);
				launchPlayer(player, power, angle, direction);
			}

			launchPlayer(player, power, angle);
		} else if (Minigames.isMinigameWorld(player.getWorld()))
			launchPlayer(player);
	}

	public void launchPlayer(Player player) {
		launchPlayer(player, 10.0, 45.0, -1.0);
	}

	public void launchPlayer(Player player, double power, double angle) {
		launchPlayer(player, power, angle, -1.0);
	}

	public void launchPlayer(Player player, double power, double angle, double direction) {
		if (launchPadPlayers.get(player) != null)
			return;

		if (direction == -1.0)
			direction = player.getLocation().getYaw();
		power *= 2;

		Location launchLocation = getCenteredLocation(player.getLocation());
		launchLocation.setPitch((float) -angle);
		launchLocation.setYaw((float) direction);

		MaterialData PISTON = new MaterialData(Material.LEGACY_PISTON_MOVING_PIECE);

		FallingBlock fallingBlock = launchLocation.getWorld().spawnFallingBlock(launchLocation, PISTON.getItemType(), PISTON.getData());
		launchPadPlayers.put(player, fallingBlock);
		launchPadBlockUUIDs.add(fallingBlock.getUniqueId());
		fallingBlock.setVelocity(launchLocation.getDirection().normalize().multiply(power / 15.0));

		playerVelTask(player);
		player.getWorld().createExplosion(player.getLocation(), -1);
	}

	private void playerVelTask(Player player) {
		taskIDs.put(player, Tasks.repeat(0, 1, () -> {
			boolean endFlight = false;
			FallingBlock fBlock = launchPadPlayers.get(player);

			if (fBlock != null) {
				fBlock.setDropItem(false);

				fBlock.getWorld();
				if (!player.getLocation().getWorld().equals(fBlock.getWorld()) || fBlock.getLocation().clone().add(fBlock.getLocation().getDirection().clone().multiply(0.5)).getBlock().isLiquid())
					endFlight = true;

				if (!fBlock.isDead() && !endFlight)
					player.setVelocity(fBlock.getVelocity());
			}

			if (fBlock == null || fBlock.isOnGround() || fBlock.isDead() || fBlock.getLocation().getY() < -10.0
					|| fBlock.getVelocity().length() == 0.0 || endFlight || player.isOnGround()) {


//				if (Dev.WAKKA.is(player) || Dev.GRIFFIN.is(player)) {
//					if (!player.isOnGround()) {
//						player.sendMessage("");
//						player.sendMessage("Ending launch because:");
//						if (fBlock == null)
//							player.sendMessage("  block is null");
//						else if (fBlock.isOnGround())
//							player.sendMessage("  block is on ground");
//						else if (fBlock.isDead())
//							player.sendMessage("  block is dead");
//						else if (fBlock.getLocation().getY() < -10.0)
//							player.sendMessage("  block is in void");
//						else if (fBlock.getVelocity().length() == 0.0)
//							player.sendMessage("  block velocity is 0");
//						else if (endFlight)
//							player.sendMessage("  either player world and block world are different OR block is in liquid");
//					}
//				}

				if (player.isOnGround()) {
					Tasks.wait(Time.SECOND, () -> {
						if (player.isOnGround()) {
//							if (Dev.WAKKA.is(player) || Dev.GRIFFIN.is(player)) {
//								player.sendMessage("");
//								player.sendMessage("Ending launch because:");
//								player.sendMessage("  player is on ground");
//							}
							cancelLaunch(player);
						}
					});
				} else {
					Tasks.wait(Time.SECOND, () -> {
						if (fBlock != null) {
							fBlock.remove();
							launchPadBlockUUIDs.remove(fBlock.getUniqueId());
						}

						cancelLaunch(player);
					});
				}
			}
		}));
	}

	private void cancelLaunch(Player player) {
		launchPadPlayers.remove(player);
		cancelPlayerVelTask(player);
	}

	private void cancelPlayerVelTask(Player player) {
		Tasks.cancel(taskIDs.get(player));
	}

	@EventHandler
	public void onEntityChangeBlockEvent(final EntityChangeBlockEvent event) {
		if (event == null) return;

		if (launchPadBlockUUIDs.contains(event.getEntity().getUniqueId()))
			event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onDamage(final EntityDamageEvent event) {
		Entity entity = event.getEntity();
		if (!(entity instanceof Player player)) return;

		if (launchPadPlayers.get(player) != null)
			event.setCancelled(true);
	}

	@EventHandler
	public void onFlightToggle(final PlayerToggleFlightEvent event) {
		if (launchPadPlayers.get(event.getPlayer()) != null)
			event.setCancelled(true);
	}

	@EventHandler
	public void onTeleport(final PlayerTeleportEvent event) {
		if (launchPadPlayers.get(event.getPlayer()) != null) {
			event.setCancelled(true);
		}
	}

}
