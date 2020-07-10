package me.pugabyte.bearnation.server.models.safecracker;

import me.pugabyte.bearnation.api.framework.persistence.annotations.PlayerClass;
import me.pugabyte.bearnation.api.framework.persistence.service.MongoService;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@PlayerClass(SafeCrackerPlayer.class)
public class SafeCrackerPlayerService extends MongoService {
	private final static Map<UUID, SafeCrackerPlayer> cache = new HashMap<>();

	public SafeCrackerPlayerService(Plugin plugin) {
		super(plugin);
	}

	public Map<UUID, SafeCrackerPlayer> getCache() {
		return cache;
	}

	public LinkedHashMap<OfflinePlayer, Integer> getScores(SafeCrackerEvent.SafeCrackerGame game) {
		LinkedHashMap<OfflinePlayer, Integer> scores = new LinkedHashMap<>();
		List<SafeCrackerPlayer> temp = new ArrayList<>();
		List<SafeCrackerPlayer> players = getAll();
		players.forEach(player -> {
			if (player.getGames().containsKey(game.getName()))
				if (player.getGames().get(game.getName()).isFinished())
					temp.add(player);
		});
		temp.sort(Comparator.comparing(player -> player.getGames().get(game.getName()).getScore()));
		temp.forEach(player -> scores.put(player.getOfflinePlayer(), player.getGames().get(game.getName()).getScore()));
		return scores;
	}


}
