package me.pugabyte.nexus.features.events.y2021.bearfair21.islands;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.pugabyte.nexus.features.events.y2021.bearfair21.BearFair21;
import org.bukkit.Location;

import java.util.Set;

import static me.pugabyte.nexus.features.events.y2021.bearfair21.BearFair21.getWGUtils;

@Getter
@AllArgsConstructor
public enum IslandType {
	MAIN(new MainIsland(), BearFair21.locationOf(0, 0, -106)),
	HALLOWEEN(new HalloweenIsland(), BearFair21.locationOf(81, 0, -325)),
	MINIGAME_NIGHT(new MinigameNightIsland(), BearFair21.locationOf(-168, 0, -186)),
	SUMMER_DOWN_UNDER(new SummerDownUnderIsland(), BearFair21.locationOf(165, 0, -185)),
	PUGMAS(new PugmasIsland(), BearFair21.locationOf(-83, 0, -328));

	private final BearFair21Island island;
	private final Location center;

	public BearFair21Island get() {
		return island;
	}

	public static IslandType of(Location location) {
		Set<String> regions = getWGUtils().getRegionNamesAt(location);
		for (IslandType island : values())
			if (regions.contains(island.get().getRegion()))
				return island;

		return null;
	}

	public static BearFair21Island get(Location location) {
		final IslandType island = of(location);
		if (island != null)
			return island.get();
		return null;
	}


}
