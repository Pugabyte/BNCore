package me.pugabyte.nexus.features.events.y2021.bearfair21.fairgrounds.minigolf.listeners;

import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.events.y2021.bearfair21.BearFair21;
import me.pugabyte.nexus.features.events.y2021.bearfair21.fairgrounds.minigolf.MiniGolf;
import me.pugabyte.nexus.features.events.y2021.bearfair21.fairgrounds.minigolf.MiniGolfUtils;
import me.pugabyte.nexus.models.bearfair21.MiniGolf21User;
import me.pugabyte.nexus.models.bearfair21.MiniGolf21UserService;
import me.pugabyte.nexus.utils.BlockUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.List;

public class ProjectileListener implements Listener {
	private final List<Material> killMaterial = Arrays.asList(Material.BARRIER, Material.CRIMSON_HYPHAE, Material.PURPLE_STAINED_GLASS, Material.WATER, Material.LAVA);

	public ProjectileListener() {
		Nexus.registerListener(this);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onProjectileHit(ProjectileHitEvent event) {
		Entity entity = event.getEntity();
		if (BearFair21.isNotAtBearFair(entity)) return;
		if (!MiniGolfUtils.isInMiniGolf(entity.getLocation())) return;

		// Check if golf ball
		if (entity instanceof Snowball) {
			// Get info
			Location loc = entity.getLocation();
			Vector vel = entity.getVelocity();
			World world = entity.getWorld();

			// Spawn new golf ball
			Snowball ball = (Snowball) world.spawnEntity(loc, EntityType.SNOWBALL);
			ball.setGravity(entity.hasGravity());

			// Update last player ball
			MiniGolf21UserService service = new MiniGolf21UserService();
			MiniGolf21User user = null;
			for (MiniGolf21User _user : service.getUsers()) {
				if (_user.getSnowball() == null)
					continue;

				if (_user.getSnowball().equals(entity)) {
					_user.setSnowball(ball);
					user = _user;
					break;
				}
			}

			if (user == null || !user.isPlaying() || !user.isOnline())
				return;

			ball.setItem(MiniGolf.getGolfBall().clone().customModelData(user.getMiniGolfColor().getCustomModelData()).build());

			// Stroke
			ball.setCustomName(MiniGolfUtils.getStrokeString(user));
			ball.setCustomNameVisible(true);
			ball.setTicksLived(entity.getTicksLived());

			// Golf ball hit entity
			if (event.getHitBlockFace() == null) {
				user.debug("ball hit an entity");
				Material _mat = loc.getBlock().getType();
				if (killMaterial.contains(_mat)) {
					user.debug("  ball is on a killMaterial, respawning...");
					MiniGolfUtils.respawnBall(ball);
					return;
				} else {
					// Bounce off of entity
					vel.multiply(-1).multiply(0.25);
				}
			}

			// Bounce off surfaces
			if (!BlockUtils.isNullOrAir(event.getHitBlock())) {
				Material mat = event.getHitBlock().getType();
				switch (event.getHitBlockFace()) {
					case NORTH:
					case SOUTH:
						if (mat == Material.SOUL_SOIL)
							vel.setZ(0);
						else if (mat == Material.SLIME_BLOCK)
							vel.setZ(Math.copySign(0.25, -vel.getZ()));
						else
							vel.setZ(-vel.getZ());
						break;

					case EAST:
					case WEST:
						if (mat == Material.SOUL_SOIL)
							vel.setX(0);
						else if (mat == Material.SLIME_BLOCK)
							vel.setX(Math.copySign(0.25, -vel.getX()));
						else
							vel.setX(-vel.getX());
						break;

					case UP:
					case DOWN:
						if (mat == Material.SOUL_SOIL || mat == Material.SMOKER) {
							vel.setY(0);
						} else if (mat == Material.SLIME_BLOCK) {
							vel.setY(0.30);
						} else {
							Material _mat = loc.getBlock().getType();
							if (killMaterial.contains(mat) || killMaterial.contains(_mat)) {
								user.debug("  ball hit out of bounds, respawning...");
								// Ball hit out of bounds
								MiniGolfUtils.respawnBall(ball);
								return;
							}

							if (vel.getY() >= 0 && vel.length() <= 0.01 && !MiniGolf.getInBounds().contains(mat)) {
								user.debug("  ball stopped out of bounds, respawning...");
								// Ball stopped in out of bounds
								MiniGolfUtils.respawnBall(ball);
								return;
							}

							vel.setY(-vel.getY());
							vel.multiply(0.7);
						}

						if (vel.getY() < 0.1) {
							vel.setY(0);
							ball.teleport(loc.add(0, MiniGolf.getFloorOffset(), 0));
							ball.setGravity(false);
						}
						break;

					default:
						break;
				}
			}

			// Friction
			ball.setVelocity(vel);
		}
	}
}
