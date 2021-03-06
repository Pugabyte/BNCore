package me.pugabyte.nexus.features.commands.staff;

import eden.utils.TimeUtils.Time;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.utils.LocationUtils;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.StringUtils;
import me.pugabyte.nexus.utils.Tasks;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.inventivetalent.glow.GlowAPI;

import java.util.Collections;

import static me.pugabyte.nexus.utils.LocationUtils.getCenteredLocation;

@Permission("group.staff")
public class NearestBlockCommand extends CustomCommand {

	public NearestBlockCommand(CommandEvent event) {
		super(event);
	}

	@Path("<material> <radius>")
	void nearestBlock(Material material, Integer radius) {
		if (radius > 100) {
			send(PREFIX + "Max radius is 100, limiting radius");
			radius = 100;
		}

		int finalRadius = radius;
		Tasks.async(() -> {
			Location location = location();
			double minDistance = finalRadius;
			Block nearestBlock = null;
			for (int x = -finalRadius; x <= finalRadius; x++) {
				for (int z = -finalRadius; z <= finalRadius; z++) {
					for (int y = -finalRadius; y < finalRadius; y++) {
						Block tempBlock = location.getBlock().getRelative(x, y, z);
						if (tempBlock.getType().equals(material)) {
							double tempDistance = location.distance(tempBlock.getLocation());
							if (tempDistance < minDistance) {
								minDistance = tempDistance;
								nearestBlock = tempBlock;
							}
						}
					}
				}
			}

			Block block = nearestBlock;
			Tasks.sync(() -> {
				if (block != null) {
					Location blockLoc = getCenteredLocation(block.getLocation());
					World blockWorld = blockLoc.getWorld();
					FallingBlock fallingBlock = blockWorld.spawnFallingBlock(blockLoc, block.getType().createBlockData());
					fallingBlock.setDropItem(false);
					fallingBlock.setGravity(false);
					fallingBlock.setInvulnerable(true);
					fallingBlock.setVelocity(new Vector(0, 0, 0));

					LocationUtils.lookAt(player(), block.getLocation());
					StringUtils.sendJsonLocation(PREFIX + "&3&l[Click to Teleport]", block.getLocation(), player());

					Tasks.GlowTask.builder()
							.duration(Time.SECOND.x(10))
							.entity(fallingBlock)
							.color(GlowAPI.Color.RED)
							.viewers(Collections.singletonList(player()))
							.onComplete(() -> {
								fallingBlock.remove();
								for (Player player : PlayerUtils.getOnlinePlayers(blockWorld))
									player.sendBlockChange(blockLoc, block.getType().createBlockData());
							})
							.start();

				} else
					error(player(), StringUtils.camelCase(material.toString()) + " not found");
			});
		});
	}
}
