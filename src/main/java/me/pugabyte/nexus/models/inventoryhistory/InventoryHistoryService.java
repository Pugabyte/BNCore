package me.pugabyte.nexus.models.inventoryhistory;

import eden.mongodb.annotations.PlayerClass;
import me.pugabyte.nexus.models.MongoService;
import me.pugabyte.nexus.models.inventoryhistory.InventoryHistory.InventorySnapshot;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@PlayerClass(InventoryHistory.class)
public class InventoryHistoryService extends MongoService<InventoryHistory> {
	private final static Map<UUID, InventoryHistory> cache = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> saveQueue = new ConcurrentHashMap<>();

	public Map<UUID, InventoryHistory> getCache() {
		return cache;
	}

	protected Map<UUID, Integer> getSaveQueue() {
		return saveQueue;
	}

	@Override
	protected void beforeSave(InventoryHistory history) {
		history.janitor();
		history.getSnapshots().sort(Comparator.comparing(InventorySnapshot::getTimestamp).reversed());
	}

}
