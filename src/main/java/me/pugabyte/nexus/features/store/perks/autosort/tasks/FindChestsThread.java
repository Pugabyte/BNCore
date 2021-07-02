package me.pugabyte.nexus.features.store.perks.autosort.tasks;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.pugabyte.nexus.features.store.perks.autosort.AutoSort;
import me.pugabyte.nexus.models.autosort.AutoSortUser;
import me.pugabyte.nexus.models.tip.Tip.TipType;
import me.pugabyte.nexus.utils.MaterialTag;
import me.pugabyte.nexus.utils.PlayerUtils.FakePlayerInteractEvent;
import me.pugabyte.nexus.utils.Tasks;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Nameable;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static me.pugabyte.nexus.utils.PlayerUtils.send;

public class FindChestsThread extends Thread {
	private final World world;
	private final ChunkSnapshot[][] snapshots;
	private final int minY;
	private int maxY;
	private final int startX;
	private final int startY;
	private final int startZ;
	private final Player player;
	private final ChunkSnapshot smallestChunk;

	private final boolean[][][] seen;

	public FindChestsThread(World world, ChunkSnapshot[][] snapshots, int minY, int maxY, int startX, int startY, int startZ, Player player) {
		this.world = world;
		this.snapshots = snapshots;
		this.minY = minY;
		this.maxY = maxY;
		this.smallestChunk = this.snapshots[0][0];
		this.startX = startX - this.smallestChunk.getX() * 16;
		this.startY = startY;
		this.startZ = startZ - this.smallestChunk.getZ() * 16;
		if (this.maxY >= world.getMaxHeight()) this.maxY = world.getMaxHeight() - 1;
		this.player = player;
		this.seen = new boolean[48][this.maxY - this.minY + 1][48];
	}

	@Override
	public void run() {
		Queue<Location> chestLocations = new ConcurrentLinkedQueue<>();
		Queue<Vector> leftToVisit = new ConcurrentLinkedQueue<>();
		Vector start = new Vector(startX, startY, startZ);
		leftToVisit.add(start);
		markSeen(start);

		while (!leftToVisit.isEmpty()) {
			Vector current = leftToVisit.remove();
			Location currentLocation = toLocation(current);

			Material type = getType(current);
			if (isChest(type) && AutoSort.canOpen(currentLocation.getBlock()))
				chestLocations.add(currentLocation);

			if (isPassable(type)) {
				Vector[] adjacents = new Vector[]{
						new Vector(current.getBlockX() + 1, current.getBlockY(), current.getBlockZ()),
						new Vector(current.getBlockX() - 1, current.getBlockY(), current.getBlockZ()),
						new Vector(current.getBlockX(), current.getBlockY() + 1, current.getBlockZ()),
						new Vector(current.getBlockX(), current.getBlockY() - 1, current.getBlockZ()),
						new Vector(current.getBlockX(), current.getBlockY(), current.getBlockZ() + 1),
						new Vector(current.getBlockX(), current.getBlockY(), current.getBlockZ() - 1),
				};

				for (Vector adjacent : adjacents)
					if (!alreadySeen(adjacent)) {
						leftToVisit.add(adjacent);
						markSeen(adjacent);
					}
			}
		}

		QuickDepositChain chain = new QuickDepositChain(AutoSortUser.of(player), chestLocations, new DepositRecord(), true);
		Tasks.wait(1, chain);
	}

	private Location toLocation(Vector location) {
		return new Location(world,
				smallestChunk.getX() * 16 + location.getBlockX(),
				location.getBlockY(),
				smallestChunk.getZ() * 16 + location.getBlockZ());
	}

	private Material getType(Vector location) {
		if (outOfBounds(location)) return null;
		int chunkx = location.getBlockX() / 16;
		int chunkz = location.getBlockZ() / 16;
		ChunkSnapshot chunk = snapshots[chunkx][chunkz];
		int x = location.getBlockX() % 16;
		int z = location.getBlockZ() % 16;
		return chunk.getBlockType(x, location.getBlockY(), z);
	}

	private boolean alreadySeen(Vector location) {
		if (outOfBounds(location)) return true;
		int y = location.getBlockY() - minY;
		return seen[location.getBlockX()][y][location.getBlockZ()];
	}

	private void markSeen(Vector location) {
		if (outOfBounds(location)) return;
		int y = location.getBlockY() - minY;
		seen[location.getBlockX()][y][location.getBlockZ()] = true;
	}

	private boolean outOfBounds(Vector location) {
		if (location.getBlockY() > this.maxY) return true;
		if (location.getBlockY() < this.minY) return true;
		if (location.getBlockX() >= 48) return true;
		if (location.getBlockX() < 0) return true;
		if (location.getBlockZ() >= 48) return true;
		return location.getBlockZ() < 0;
	}

	private static final MaterialTag chests = new MaterialTag(MaterialTag.SHULKER_BOXES)
			.append(Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL);

	private boolean isChest(Material material) {
		return chests.isTagged(material);
	}

	private boolean isPassable(Material material) {
		if (material == null)
			return false;
		return switch (material) {
			case AIR, CHEST, TRAPPED_CHEST, HOPPER, CAVE_AIR, VOID_AIR -> true;
			default -> MaterialTag.ALL_SIGNS.isTagged(material);
		};
	}

	@AllArgsConstructor
	static class QuickDepositChain implements Runnable {
		private final AutoSortUser autoSortUser;
		private final Queue<Location> remainingChestLocations;
		private final DepositRecord runningDepositRecord;
		private final boolean respectExclusions;

		@Override
		public void run() {
			if (!autoSortUser.isOnline())
				return;

			Location chestLocation = this.remainingChestLocations.poll();
			if (chestLocation == null) {
				send(autoSortUser, AutoSort.PREFIX + "Deposited &e%d &3items into nearby chests", runningDepositRecord.totalItems);
				autoSortUser.tip(TipType.AUTOSORT_DEPOSIT_QUICK);
			} else {
				Player player = autoSortUser.getOnlinePlayer();

				Block block = chestLocation.getBlock();
				PlayerInventory inventory = player.getInventory();
				PlayerInteractEvent fakeEvent = new FakePlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, inventory.getItemInMainHand(), block, BlockFace.UP);

				if (fakeEvent.callEvent()) {
					BlockState state = block.getState();
					if (state instanceof InventoryHolder chest) {
						Inventory chestInventory = chest.getInventory();
						String title = state instanceof Nameable ? ((Nameable) state).getCustomName() : null;
						boolean isSortable = AutoSort.isSortableChestInventory(player, chestInventory, title);
						if (!respectExclusions || isSortable) {
							DepositRecord deposits = AutoSort.depositMatching(autoSortUser, chestInventory, false);
							runningDepositRecord.totalItems += deposits.totalItems;
						}
					}
				}

				QuickDepositChain chain = new QuickDepositChain(autoSortUser, remainingChestLocations, runningDepositRecord, respectExclusions);
				Tasks.wait(1, chain);
			}
		}

	}

	@NoArgsConstructor
	@Data
	public static class DepositRecord {
		private int totalItems;
		private boolean destinationFull;

	}

}
