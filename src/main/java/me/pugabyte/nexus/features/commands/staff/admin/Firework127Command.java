package me.pugabyte.nexus.features.commands.staff.admin;

import lombok.NonNull;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Aliases;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.utils.ItemBuilder;
import me.pugabyte.nexus.utils.PlayerUtils;
import org.bukkit.Material;

@Aliases("fw127")
@Permission("group.admin")
public class Firework127Command extends CustomCommand {

	public Firework127Command(@NonNull CommandEvent event) {
		super(event);
	}

	@Path
	void run() {
		PlayerUtils.giveItem(player(), new ItemBuilder(Material.FIREWORK_ROCKET).fireworkPower(127).build());
	}

}
