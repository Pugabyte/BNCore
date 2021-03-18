package me.pugabyte.nexus.features.commands;

import lombok.NoArgsConstructor;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Aliases;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.godmode.Godmode;
import me.pugabyte.nexus.models.godmode.GodmodeService;
import me.pugabyte.nexus.models.pvp.PVP;
import me.pugabyte.nexus.models.pvp.PVPService;
import me.pugabyte.nexus.utils.Tasks;
import me.pugabyte.nexus.utils.Time;
import me.pugabyte.nexus.utils.WorldGroup;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import static me.pugabyte.nexus.utils.PlayerUtils.isVanished;
import static me.pugabyte.nexus.utils.StringUtils.colorize;

@NoArgsConstructor
@Aliases({"spvp", "duel", "fight"})
public class PVPCommand extends CustomCommand implements Listener {
	public final PVPService service = new PVPService();
	public PVP pvp;

	static {
		Tasks.repeatAsync(5, Time.SECOND.x(2), () -> {
			PVPService service = new PVPService();
			for (Player player : Bukkit.getOnlinePlayers()) {
				PVP pvp = service.get(player);
				if (pvp.isEnabled())
					player.sendActionBar(colorize("&cPVP is enabled"));
			}
		});
	}

	public PVPCommand(CommandEvent event) {
		super(event);
		if (isPlayer())
			pvp = service.get(player());
	}

	@Path("on")
	void on() {
		pvp.setEnabled(true);
		service.save(pvp);
		send(PREFIX + "&aEnabled");
	}

	@Path("off")
	void off() {
		pvp.setEnabled(false);
		service.save(pvp);
		send(PREFIX + "&cDisabled");
	}

	@Path("keepInventory")
	void keepInventory() {
		pvp.setKeepInventory(!pvp.isKeepInventory());
		service.save(pvp);
		send(PREFIX + "Keep inventory on PVP death " + (pvp.isKeepInventory() ? "&aenabled" : "&cdisabled"));
	}

	@EventHandler
	public void onPlayerPVP(EntityDamageByEntityEvent event) {
		if (!(event.getEntity() instanceof Player)) return;
		if (!(event.getDamager() instanceof Player)) return;
		if (WorldGroup.get(event.getEntity()) != WorldGroup.SURVIVAL) return;

		PVP victim = service.get((Player) event.getEntity());
		PVP attacker = service.get((Player) event.getDamager());

		// Cancel if both players do not have pvp on
		if (!victim.isEnabled() || !attacker.isEnabled()) {
			event.setCancelled(true);
			return;
		}

		if (isVanished(victim.getPlayer()) || isVanished(attacker.getPlayer())) {
			event.setCancelled(true);
			return;
		}

		if (victim.getPlayer().getGameMode() != GameMode.SURVIVAL || attacker.getPlayer().getGameMode() != GameMode.SURVIVAL) {
			event.setCancelled(true);
			return;
		}

		GodmodeService godmodeService = new GodmodeService();
		Godmode victimGodmode = godmodeService.get(victim.getPlayer());
		Godmode attackerGodmode = godmodeService.get(attacker.getPlayer());

		if (victimGodmode.isEnabled() || attackerGodmode.isEnabled()) {
			event.setCancelled(true);
			return;
		}
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		if (WorldGroup.get(event.getEntity()) != WorldGroup.SURVIVAL) return;
		if (event.getEntity().getKiller() == null) return;
		PVP victim = service.get(event.getEntity());
		if (!victim.isEnabled()) return;
		// For some reason, spigots PlayerDeathEvent#setKeepInventory() method
		// duplicates the items, and md_5 does not see this as a bug
		// We must clear the drops as well to keep them from duping
		if (victim.isKeepInventory()) {
			event.setKeepInventory(true);
			event.getDrops().clear();
		} else
			event.setKeepInventory(false);
	}

}
