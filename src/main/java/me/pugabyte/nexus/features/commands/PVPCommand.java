package me.pugabyte.nexus.features.commands;

import eden.utils.TimeUtils.Time;
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
import me.pugabyte.nexus.utils.WorldGroup;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

	/**
	 * Processes a player attacking another player. This cancels the event if the fighters aren't eligible to fight.
	 * <br>
	 * Criteria to permit the event: players must both have PVP enabled, be unvanished, be in survival, and not have godmode enabled.
	 * Event will return if the attacker is null, the victim is the attacker, or the attack is outside the survival world.
	 * @param event the originating damage event
	 * @param victim user who is getting attacked
	 * @param attacker user who is attacking
	 */
	public void processAttack(@NotNull EntityDamageEvent event, @NotNull PVP victim, @Nullable PVP attacker) {
		if (attacker == null)
			return;
		if (victim.equals(attacker))
			return;
		if (WorldGroup.get(event.getEntity()) != WorldGroup.SURVIVAL) return;

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
		Godmode victimGodmode = godmodeService.get(victim);
		Godmode attackerGodmode = godmodeService.get(attacker);

		if (victimGodmode.isEnabled() || attackerGodmode.isEnabled())
			event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerPVP(EntityDamageByEntityEvent event) {
		if (WorldGroup.get(event.getEntity()) != WorldGroup.SURVIVAL) return;

		if (!(event.getEntity() instanceof Player)) return;
		PVP victim = service.get((Player) event.getEntity());

		PVP attacker = null;
		Projectile projectile;

		if (event.getDamager() instanceof Player) {
			attacker = service.get((Player) event.getDamager());
		} else if (event.getDamager() instanceof Projectile) {
			projectile = (Projectile) event.getDamager();
			if (projectile.getShooter() instanceof Player)
				attacker = service.get((Player) projectile.getShooter());
		} else if (event.getDamager() instanceof EnderCrystal) {
			EnderCrystal crystal = (EnderCrystal) event.getDamager();
			// find last user to damage the end crystal
			EntityDamageEvent crystalDamage = crystal.getLastDamageCause();
			if (crystalDamage == null) return;
			if (!(crystalDamage instanceof EntityDamageByEntityEvent)) return;
			Entity damager = ((EntityDamageByEntityEvent) crystalDamage).getDamager();
			if (damager instanceof Player)
				attacker = service.get((Player) damager);
			// check if last damager was a projectile shot by a player
			else if (damager instanceof Projectile) {
				projectile = (Projectile) damager;
				if (projectile.getShooter() != null && projectile.getShooter() instanceof Player)
					attacker = service.get((Player) projectile.getShooter());
			}
		} else if (event.getDamager() instanceof TNTPrimed) {
			TNTPrimed tnt = (TNTPrimed) event.getDamager();
			if (tnt.getSource() instanceof Player)
				attacker = service.get((Player) tnt.getSource());
		}

		processAttack(event, victim, attacker);
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
			event.setKeepLevel(true);
			event.setDroppedExp(0);
		} else
			event.setKeepInventory(false);
	}

}
