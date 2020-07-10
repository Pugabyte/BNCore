package me.pugabyte.bearnation.server.features.commands.staff;

import lombok.NonNull;
import me.pugabyte.bearnation.api.framework.commands.models.CustomCommand;
import me.pugabyte.bearnation.api.framework.commands.models.annotations.Path;
import me.pugabyte.bearnation.api.framework.commands.models.annotations.Permission;
import me.pugabyte.bearnation.api.framework.commands.models.events.CommandEvent;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Snow;

@Permission("group.staff")
public class SnowLayersCommand extends CustomCommand {

	public SnowLayersCommand(@NonNull CommandEvent event) {
		super(event);
	}

	@Path("<layers>")
	void layers(int layers) {
		Block block = player().getLocation().getBlock();
		block.setType(Material.SNOW, false);
		Snow snow = (Snow) block.getBlockData();
		snow.setLayers(layers);
		block.setBlockData(snow);
	}
}
