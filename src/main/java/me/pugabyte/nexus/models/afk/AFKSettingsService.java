package me.pugabyte.nexus.models.afk;

import me.pugabyte.nexus.framework.persistence.annotations.PlayerClass;
import me.pugabyte.nexus.models.MongoService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@PlayerClass(AFKSettings.class)
public class AFKSettingsService extends MongoService {
	private final static Map<UUID, AFKSettings> cache = new HashMap<>();

	public Map<UUID, AFKSettings> getCache() {
		return cache;
	}

}