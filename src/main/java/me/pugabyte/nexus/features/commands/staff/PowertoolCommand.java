package me.pugabyte.nexus.features.commands.staff;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Aliases;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.nerd.Rank;
import me.pugabyte.nexus.models.powertool.PowertoolService;
import me.pugabyte.nexus.models.powertool.PowertoolUser;
import me.pugabyte.nexus.utils.ItemUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

@NoArgsConstructor
@Permission("group.staff")
@Aliases({"pt", "powertools"})
public class PowertoolCommand extends CustomCommand implements Listener {
	private final PowertoolService service = new PowertoolService();
	private PowertoolUser user;

	public PowertoolCommand(@NonNull CommandEvent event) {
		super(event);
		if (isPlayer())
			user = service.get(player());
	}

	@Path("[string...]")
	void run(String command) {
		Material material = getToolRequired().getType();

		if (isNullOrEmpty(command))
			if (user.getPowertools().containsKey(material)) {
				user.getPowertools().remove(material);
				service.save(user);
				send(PREFIX + "Deactivated powertool on " + camelCase(material));
			} else {
				send(PREFIX + "No active powertool on " + camelCase(material));
			}
		else {
			user.getPowertools().put(material, command);
			service.save(user);
			send(PREFIX + "Activated powertool on " + camelCase(material) + ": &e" + command);
		}

		if (!user.isEnabled())
			send(PREFIX + "&cWarning: Powertools are disabled, enable with /" + getAliasUsed() + " toggle");
	}

	@Path("toggle")
	void toggle() {
		user.setEnabled(!user.isEnabled());
		service.save(user);
		send(PREFIX + (user.isEnabled() ? "&aEnabled" : "&cDisabled"));
	}

	@Path("list")
	void list() {
		if (user.getPowertools().isEmpty())
			error("No active powertools");

		user.getPowertools().forEach((material, command) ->
				send("&e" + camelCase(material) + " &7- " + command));
	}

	@Path("clear")
	void clear() {
		if (user.getPowertools().isEmpty())
			error("No active powertools");

		user.getPowertools().clear();
		service.save(user);
		send(PREFIX + "All powertools deactivated");
	}

	@EventHandler
	public void onPlayerInteract(final PlayerInteractEvent event) {
		if (!Rank.of(event.getPlayer()).isStaff()) return;

		if (Arrays.asList(Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK).contains(event.getAction())) {
			ItemStack item = ItemUtils.getTool(event.getPlayer());
			if (item == null) return;

			PowertoolUser user = new PowertoolService().get(event.getPlayer());
			if (user.getPowertools().containsKey(item.getType())) {
				user.use(item.getType());
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onEntityDamage(final EntityDamageByEntityEvent event) {
		if (!(event.getDamager() instanceof Player player)) return;
		if (!DamageCause.ENTITY_ATTACK.equals(event.getCause())) return;

		if (!Rank.of(player).isStaff()) return;

		ItemStack item = ItemUtils.getTool(player);
		if (item == null) return;

		PowertoolUser user = new PowertoolService().get(player);
		if (user.getPowertools().containsKey(item.getType())) {
			user.use(item.getType());
			event.setCancelled(true);
		}
	}

}
