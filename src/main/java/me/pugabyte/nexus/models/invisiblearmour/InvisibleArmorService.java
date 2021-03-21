package me.pugabyte.nexus.models.invisiblearmour;

import me.pugabyte.nexus.framework.persistence.annotations.PlayerClass;
import me.pugabyte.nexus.models.MongoService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@PlayerClass(InvisibleArmor.class)
public class InvisibleArmorService extends MongoService {
	private final static Map<UUID, InvisibleArmor> cache = new HashMap<>();

	public Map<UUID, InvisibleArmor> getCache() {
		return cache;
	}

}