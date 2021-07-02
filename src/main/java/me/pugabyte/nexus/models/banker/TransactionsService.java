package me.pugabyte.nexus.models.banker;

import eden.mongodb.annotations.PlayerClass;
import me.pugabyte.nexus.models.MongoService;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@PlayerClass(Transactions.class)
public class TransactionsService extends MongoService<Transactions> {
	private final static Map<UUID, Transactions> cache = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> saveQueue = new ConcurrentHashMap<>();

	public Map<UUID, Transactions> getCache() {
		return cache;
	}

	protected Map<UUID, Integer> getSaveQueue() {
		return saveQueue;
	}

	@Override
	protected void beforeSave(Transactions transactions) {
		transactions.getTransactions().removeIf(transaction -> transaction.getTimestamp().isBefore(LocalDateTime.now().minusDays(60)));
	}

}
