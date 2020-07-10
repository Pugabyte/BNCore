package me.pugabyte.bearnation.api.models.warps;

import me.pugabyte.bearnation.api.framework.persistence.service.MySQLService;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class WarpService extends MySQLService {

	public WarpService(Plugin plugin) {
		super(plugin);
	}

	public Warp get(String name, WarpType type) {
		Warp warp = database.where("type = ? AND name = ?", type.name(), name).first(Warp.class);
		if (warp.getName() == null) return null;
		return warp;
	}

	public Warp getNormalWarp(String name) {
		Warp warp = database.where("type = ? AND name = ?", WarpType.NORMAL.name(), name).first(Warp.class);
		if (warp.getName() == null) return null;
		return warp;
	}

	public Warp getStaffWarp(String name) {
		Warp warp = database.where("type = ? AND name = ?", WarpType.STAFF.name(), name).first(Warp.class);
		if (warp.getName() == null) return null;
		return warp;
	}

	public List<Warp> getAllWarps() {
		return database.select("*").results(Warp.class);
	}

	public List<Warp> getWarpsByType(WarpType type) {
		return database.where("type = ?", type.name()).results(Warp.class);
	}

	public void delete(Warp warp) {
		database.table("warp").where("type = ? AND name = ?", warp.getType(), warp.getName()).delete();
	}

}
