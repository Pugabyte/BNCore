package me.pugabyte.nexus.features.minigames.mechanics.common;

import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import eden.utils.TimeUtils.Time;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.pugabyte.nexus.features.minigames.models.Arena;
import me.pugabyte.nexus.features.minigames.models.Match;
import me.pugabyte.nexus.features.minigames.models.Minigamer;
import me.pugabyte.nexus.features.minigames.models.events.matches.minigamers.MinigamerDeathEvent;
import me.pugabyte.nexus.features.minigames.models.mechanics.Mechanic;
import me.pugabyte.nexus.framework.exceptions.postconfigured.InvalidInputException;
import me.pugabyte.nexus.utils.BlockUtils;
import me.pugabyte.nexus.utils.LocationUtils;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class AntiCampingTask {
	@NonNull
	private final Match match;
	private int taskId;
	private final int anticampingWarn = 6;
	private final int anticampingTeleport = 10;

	private final @NotNull Map<Minigamer, Integer> secondsCamping = new HashMap<>();
	private final @NotNull Map<Minigamer, Location> recentLocations = new HashMap<>();

	public void start() {
		taskId = match.getTasks().repeat(Time.SECOND, Time.SECOND, () -> {
			for (Minigamer minigamer : match.getAliveMinigamers()) {
				if (match.isEnded()) {
					stop();
					return;
				}

				if (recentLocations.containsKey(minigamer)) {
					Location recent = recentLocations.get(minigamer);
					Location now = minigamer.getPlayer().getLocation();

					double dx = Math.abs(now.getX() - recent.getX());
					double dy = Math.abs(now.getY() - recent.getY());
					double dz = Math.abs(now.getZ() - recent.getZ());

					if ((dx < 1D && dy < 2D && dz < 1D) || minigamer.getPlayer().isSneaking()) {
						int seconds = secondsCamping.containsKey(minigamer) ? secondsCamping.get(minigamer) + 1 : 1;

						if (seconds == anticampingWarn)
							minigamer.tell("&4Warning: &cYou will be teleported down if you continue to camp");

						if (seconds == anticampingTeleport) {
							minigamer.tell("&cYou have been teleported down for camping");
							teleport(minigamer);
							secondsCamping.remove(minigamer);
						} else {
							secondsCamping.put(minigamer, seconds);
						}
					} else {
						secondsCamping.remove(minigamer);
					}
				}

				recentLocations.put(minigamer, minigamer.getPlayer().getLocation());
			}
		});
	}

	void stop() {
		match.getTasks().cancel(taskId);
	}

	private void teleport(@NotNull Minigamer minigamer) {
		teleport(minigamer, -1);
	}

	private void teleport(@NotNull Minigamer minigamer, int floorId) {
		Arena arena = minigamer.getMatch().getArena();
		Mechanic mechanic = arena.getMechanic();
		ProtectedRegion floorAt;

		// Floor ID not provided, get it
		if (floorId == -1) {
			floorAt = getFloorAt(minigamer);

			if (floorAt == null)
				throw new InvalidInputException("Could not find floor for player " + minigamer.getNickname());

			floorId = Arena.getRegionNumber(floorAt);
		} else {
			// Floor ID provided (probably recursively trying to find next floor down)
			floorAt = arena.getProtectedRegion("floor_" + floorId);
		}

		if (floorId < 2) {
			MinigamerDeathEvent deathEvent = new MinigamerDeathEvent(minigamer);
			if (!deathEvent.callEvent()) return;
			mechanic.onDeath(deathEvent);
		} else {
			Location location = minigamer.getPlayer().getLocation();
			location.setY(floorAt.getMinimumPoint().getY() - 1);

			Region floorTo = arena.getRegion("floor_" + --floorId);
			Location to = null;

			while (location.getY() >= floorTo.getMinimumPoint().getY()) {
				location.add(0, -1, 0);
				if (!location.getBlock().getType().isSolid())
					continue;

				to = location;
				break;
			}

			if (to != null) {
				teleport(minigamer, to);
				return;
			}

			// Set up correct origin and search radius for nearest block check
			int yDiff = floorTo.getMaximumPoint().getBlockY() - floorTo.getMinimumPoint().getBlockY();
			location.setY(floorTo.getMaximumPoint().getBlockY());
			if (yDiff > 0) {
				yDiff = (yDiff / 2) + 1;
				location.add(0, -yDiff, 0);
			}

			List<Block> blocks = BlockUtils.getBlocksInRadius(location.getBlock(), 4, yDiff, 4).stream()
					.filter(block -> block.getType().isSolid())
					.sorted((block1, block2) -> {
						Double distance1 = block1.getLocation().distance(minigamer.getPlayer().getLocation());
						Double distance2 = block2.getLocation().distance(minigamer.getPlayer().getLocation());
						return distance1.compareTo(distance2);
					})
					.collect(Collectors.toList());

			// No blocks within required radius on below floor, try next one
			if (blocks.size() == 0) {
				teleport(minigamer, floorId);
				return;
			}

			teleport(minigamer, blocks.get(0).getLocation());
		}
	}

	private void teleport(@NotNull Minigamer minigamer, @NotNull Location to) {
		to = LocationUtils.getCenteredLocation(to);
		to.setYaw(minigamer.getPlayer().getLocation().getYaw());
		to.setPitch(minigamer.getPlayer().getLocation().getPitch());
		minigamer.teleport(to.add(0, 1, 0));
	}

	private @Nullable ProtectedRegion getFloorAt(@NotNull Minigamer minigamer) {
		final String floorRegex = match.getArena().getRegionTypeRegex("floor");
		Location location = minigamer.getPlayer().getLocation();
		ProtectedRegion floor = null;
		for (int i = 0; i < 3; i++) {
			Set<ProtectedRegion> regionsAt = minigamer.getMatch().getArena().getWGUtils().getRegionsAt(location);
			for (ProtectedRegion region : regionsAt) {
				if (region.getId().matches(floorRegex)) {
					floor = region;
					break;
				}
			}

			if (floor != null)
				break;

			location.add(0, -1, 0);
		}
		return floor;
	}
}
