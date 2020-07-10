package me.pugabyte.bearnation.server.features.commands;

import me.pugabyte.bearnation.api.framework.commands.models.CustomCommand;
import me.pugabyte.bearnation.api.framework.commands.models.annotations.Arg;
import me.pugabyte.bearnation.api.framework.commands.models.annotations.Path;
import me.pugabyte.bearnation.api.framework.commands.models.annotations.Redirects.Redirect;
import me.pugabyte.bearnation.api.framework.commands.models.events.CommandEvent;
import me.pugabyte.bearnation.api.framework.exceptions.postconfigured.InvalidInputException;
import me.pugabyte.bearnation.api.utils.Utils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.inventivetalent.glow.GlowAPI;

import java.text.DecimalFormat;
import java.util.Collections;

import static me.pugabyte.bearnation.api.utils.StringUtils.stripColor;

@Redirect(from = "/entityhealth", to = "/health target")
public class HealthCommand extends CustomCommand {
	private DecimalFormat nf = new DecimalFormat("#.00");

	public HealthCommand(CommandEvent event) {
		super(event);
	}

	@Path("<player> [number]")
	void health(@Arg("self") Player player, Double health) {
		if (health == null)
			send(PREFIX + player.getName() + "'s health is " + nf.format(player.getHealth()));
		else {
			checkPermission("health.set");
			player.setHealth(health);
			send(PREFIX + stripColor(player.getName()) + "'s health set to " + nf.format(player.getHealth()));
		}
	}

	@Path("target [number]")
	void target(Double health) {
		LivingEntity target = Utils.getTargetEntity(player());

		if (target == null)
			throw new InvalidInputException("No target entity found");

		tasks().glowTask()
				.duration(10 * 20)
				.entity(target)
				.color(GlowAPI.Color.RED)
				.viewers(Collections.singletonList(player()))
				.start();

		if (health == null)
			send(PREFIX + stripColor(target.getName()) + "'s health is " + nf.format(target.getHealth()));
		else {
			checkPermission("health.set");
			target.setHealth(health);
			send(PREFIX + stripColor(target.getName()) + "'s health set to " + nf.format(target.getHealth()));
		}
	}
}
