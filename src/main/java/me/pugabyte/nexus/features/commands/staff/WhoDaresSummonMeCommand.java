package me.pugabyte.nexus.features.commands.staff;

import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.utils.Tasks;
import me.pugabyte.nexus.utils.Time;
import org.bukkit.World;
import org.bukkit.entity.Bat;

import java.util.ArrayList;
import java.util.List;

@Permission("pv.see")
public class WhoDaresSummonMeCommand extends CustomCommand {

	public WhoDaresSummonMeCommand(CommandEvent event) {
		super(event);
	}

	@Path
	void run() {
		runCommand("vanish off");
		World world = player().getWorld();
		world.strikeLightning(player().getLocation().clone().add(0, 5, 0));
		world.strikeLightningEffect(player().getLocation().clone().add(5, 0, 0));
		world.strikeLightningEffect(player().getLocation().clone().add(0, 0, 5));
		world.strikeLightningEffect(player().getLocation().clone().subtract(5, 0, 0));
		world.strikeLightningEffect(player().getLocation().clone().subtract(0, 0, 5));
		List<Bat> bats = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			bats.add(world.spawn(player().getLocation(), Bat.class));
		}
		Tasks.wait(Time.SECOND.x(5), () -> bats.forEach(Bat::remove));
	}


}