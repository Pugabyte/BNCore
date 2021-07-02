package me.pugabyte.nexus.features.minigames.models.arenas;

import lombok.Data;
import lombok.ToString;
import me.pugabyte.nexus.features.minigames.mechanics.Thimble;
import me.pugabyte.nexus.features.minigames.models.Arena;
import me.pugabyte.nexus.utils.RandomUtils;
import org.bukkit.configuration.serialization.SerializableAs;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@ToString(callSuper = true)
@SerializableAs("ThimbleArena")
public class ThimbleArena extends Arena {
	private List<ThimbleMap> thimbleMaps;

	private ThimbleMap currentMap;
	private Thimble.ThimbleGamemode gamemode;

	public ThimbleArena(Map<String, Object> map) {
		super(map);
		this.thimbleMaps = (List<ThimbleMap>) map.get("thimbleMaps");

		if (thimbleMaps != null)
			currentMap = thimbleMaps.get(0);
	}

	@Override
	public @NotNull String getRegionBaseName() {
		return "thimble_" + currentMap.getName().toLowerCase();
	}

	@Override
	public @NotNull Map<String, Object> serialize() {
		LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>) super.serialize();
		map.put("thimbleMaps", getThimbleMaps());

		return map;
	}

	public Thimble.ThimbleGamemode getNextGamemode() {
		if (gamemode != null)
			if (gamemode instanceof Thimble.ClassicGamemode)
				return new Thimble.RiskGamemode();
			else if (gamemode instanceof Thimble.RiskGamemode)
				return new Thimble.LastManStandingGamemode();
			else if (gamemode instanceof Thimble.LastManStandingGamemode)
				return new Thimble.ClassicGamemode();

		return RandomUtils.randomElement(Arrays.asList(new Thimble.ClassicGamemode(), new Thimble.RiskGamemode(), new Thimble.LastManStandingGamemode()));
	}


}
