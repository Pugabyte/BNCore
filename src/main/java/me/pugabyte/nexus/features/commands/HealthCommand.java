package me.pugabyte.nexus.features.commands;

import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Arg;
import me.pugabyte.nexus.framework.commands.models.annotations.Description;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Redirects.Redirect;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.utils.Tasks;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.inventivetalent.glow.GlowAPI;

import java.text.DecimalFormat;
import java.util.Collections;

import static me.pugabyte.nexus.utils.StringUtils.stripColor;

@Redirect(from = "/entityhealth", to = "/health target")
@Description("View the health of a player or entity")
public class HealthCommand extends CustomCommand {
	private static final DecimalFormat nf = new DecimalFormat("#.00");

	public HealthCommand(CommandEvent event) {
		super(event);
	}

	@Path("<player> [number]")
	void health(@Arg("self") Player player, @Arg(permission = "group.staff", min = 0.0, max = 20.0) Double health) {
		if (health == null)
			send(PREFIX + player.getName() + "'s health is " + nf.format(player.getHealth()));
		else {
			player.setHealth(health);
			send(PREFIX + stripColor(player.getName()) + "'s health set to " + nf.format(player.getHealth()));
		}
	}

	@Path("target [number]")
	void target(@Arg(permission = "group.staff", min = 0.0, max = 20.0) Double health) {
		LivingEntity target = getTargetLivingEntityRequired();

		Tasks.GlowTask.builder()
			.duration(10 * 20)
			.entity(target)
			.color(GlowAPI.Color.RED)
			.viewers(Collections.singletonList(player()))
			.start();

		if (health == null)
			send(PREFIX + stripColor(target.getName()) + "'s health is " + nf.format(target.getHealth()));
		else {
			target.setHealth(health);
			send(PREFIX + stripColor(target.getName()) + "'s health set to " + nf.format(target.getHealth()));
		}
	}
}
