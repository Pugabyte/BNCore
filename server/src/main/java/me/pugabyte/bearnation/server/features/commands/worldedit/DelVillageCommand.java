package me.pugabyte.bearnation.server.features.commands.worldedit;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.world.block.BlockTypes;
import lombok.SneakyThrows;
import me.pugabyte.bearnation.api.framework.commands.models.CustomCommand;
import me.pugabyte.bearnation.api.framework.commands.models.annotations.DoubleSlash;
import me.pugabyte.bearnation.api.framework.commands.models.annotations.Path;
import me.pugabyte.bearnation.api.framework.commands.models.events.CommandEvent;
import me.pugabyte.bearnation.api.utils.WorldEditUtils;
import org.bukkit.Bukkit;

@DoubleSlash
public class DelVillageCommand extends CustomCommand {

	public DelVillageCommand(CommandEvent event) {
		super(event);
	}

	@SneakyThrows
	@Path
	void delVillage() {
		WorldEditPlugin worldEditPlugin = (WorldEditPlugin) Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
		LocalSession session = worldEditPlugin.getSession(player());
		WorldEditUtils worldEditUtils = new WorldEditUtils(player());
		worldEditUtils.setSelection(player(), player().getLocation());
		ExpandAllCommand.expandAll(player(), 75);
		worldEditUtils.set(worldEditUtils.getPlayerSelection(player()), BlockTypes.AIR);
		worldEditUtils.fixFlat(session, worldEditUtils.getPlayerSelection(player()));
		send("&3Successfully removed the village");
	}
}
