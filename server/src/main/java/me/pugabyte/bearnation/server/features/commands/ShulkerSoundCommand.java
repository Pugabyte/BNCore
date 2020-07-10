package me.pugabyte.bearnation.server.features.commands;

import me.pugabyte.bearnation.api.framework.commands.models.CustomCommand;
import me.pugabyte.bearnation.api.framework.commands.models.annotations.Path;
import me.pugabyte.bearnation.api.framework.commands.models.events.CommandEvent;
import me.pugabyte.bearnation.api.utils.RandomUtils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class ShulkerSoundCommand extends CustomCommand {

	public ShulkerSoundCommand(CommandEvent event) {
		super(event);
	}

	@Path("<player>")
	void shulkerSound(Player player) {
		commandBlock();
		float pitch = RandomUtils.randomInt(0, 2);
		player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_AMBIENT, 10F, pitch);
	}
}
