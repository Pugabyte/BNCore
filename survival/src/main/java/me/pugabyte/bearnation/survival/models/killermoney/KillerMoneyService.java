package me.pugabyte.bearnation.survival.models.killermoney;

import lombok.Getter;
import me.pugabyte.bearnation.api.framework.persistence.annotations.PlayerClass;
import me.pugabyte.bearnation.models.MongoService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@PlayerClass(KillerMoney.class)
public class KillerMoneyService extends MongoService {

	@Getter
	private final static Map<UUID, KillerMoney> cache = new HashMap<>();

	@Override
	public Map<UUID, KillerMoney> getCache() {
		return cache;
	}
}
