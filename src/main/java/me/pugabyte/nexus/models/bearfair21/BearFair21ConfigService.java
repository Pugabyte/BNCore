package me.pugabyte.nexus.models.bearfair21;

import eden.mongodb.annotations.PlayerClass;
import me.pugabyte.nexus.models.MongoService;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@PlayerClass(BearFair21Config.class)
public class BearFair21ConfigService extends MongoService<BearFair21Config> {
	private final static Map<UUID, BearFair21Config> cache = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> saveQueue = new ConcurrentHashMap<>();

	public Map<UUID, BearFair21Config> getCache() {
		return cache;
	}

	protected Map<UUID, Integer> getSaveQueue() {
		return saveQueue;
	}
}
