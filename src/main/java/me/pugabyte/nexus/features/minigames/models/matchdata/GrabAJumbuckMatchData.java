package me.pugabyte.nexus.features.minigames.models.matchdata;

import lombok.Data;
import me.pugabyte.nexus.features.minigames.mechanics.GrabAJumbuck;
import me.pugabyte.nexus.features.minigames.models.Match;
import me.pugabyte.nexus.features.minigames.models.MatchData;
import me.pugabyte.nexus.features.minigames.models.annotations.MatchDataFor;
import org.bukkit.entity.Entity;

import java.util.ArrayList;

@Data
@MatchDataFor(GrabAJumbuck.class)
public class GrabAJumbuckMatchData extends MatchData {
	public ArrayList<Entity> sheeps = new ArrayList<>();
	public ArrayList<Entity> items = new ArrayList<>();

	public GrabAJumbuckMatchData(Match match) {
		super(match);
	}
}
