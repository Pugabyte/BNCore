package me.pugabyte.nexus.features.events.models;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.pugabyte.nexus.features.events.y2020.bearfair20.BearFair20;
import me.pugabyte.nexus.features.events.y2021.bearfair21.BearFair21;
import me.pugabyte.nexus.utils.WorldGuardUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Set;

public class BearFairTalker extends Talker {

	public static TalkingNPC getTalkingNPC(Player player, int id) {
		BearFairIsland island = getIslandType(player);
		if (island == null)
			return null;

		return island.getNPC(id);
	}

	public static void startScript(Player player, int id) {
		TalkingNPC talker = getTalkingNPC(player, id);
		if (talker == null)
			return;

		sendScript(player, talker);
	}


	private static BearFairIsland getIslandType(Player player) {
		WorldGuardUtils WGUtils = new WorldGuardUtils(player);
		Location location = player.getLocation();
		Set<ProtectedRegion> regions = WGUtils.getRegionsLikeAt("bearfair.*", location);
		for (ProtectedRegion region : regions) {
			if (region.getId().equals(BearFair20.getRegion()))
				return me.pugabyte.nexus.features.events.y2020.bearfair20.islands.IslandType.getFromLocation(location);
			if (region.getId().equals(BearFair21.getRegion()))
				return me.pugabyte.nexus.features.events.y2021.bearfair21.islands.IslandType.get(location);
		}
		return null;
	}
}
