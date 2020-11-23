package me.pugabyte.nexus.models.socialmedia;

import me.pugabyte.nexus.framework.persistence.annotations.PlayerClass;
import me.pugabyte.nexus.models.MongoService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@PlayerClass(SocialMediaUser.class)
public class SocialMediaService extends MongoService {
	private final static Map<UUID, SocialMediaUser> cache = new HashMap<>();

	public Map<UUID, SocialMediaUser> getCache() {
		return cache;
	}

}