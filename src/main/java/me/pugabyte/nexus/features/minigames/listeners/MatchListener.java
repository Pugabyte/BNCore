package me.pugabyte.nexus.features.minigames.listeners;

import com.destroystokyo.paper.event.entity.ProjectileCollideEvent;
import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.minigames.Minigames;
import me.pugabyte.nexus.features.minigames.managers.ArenaManager;
import me.pugabyte.nexus.features.minigames.managers.MatchManager;
import me.pugabyte.nexus.features.minigames.managers.PlayerManager;
import me.pugabyte.nexus.features.minigames.models.Arena;
import me.pugabyte.nexus.features.minigames.models.Match;
import me.pugabyte.nexus.features.minigames.models.Minigamer;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchJoinEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchStartEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.MinigamerQuitEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.minigamers.MinigamerDamageEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.minigamers.MinigamerDeathEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.minigamers.MinigamerLoadoutEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.minigamers.sabotage.MinigamerDisplayTimerEvent;
import me.pugabyte.nexus.features.minigames.models.mechanics.Mechanic;
import me.pugabyte.nexus.features.minigames.models.perks.ParticleProjectile;
import me.pugabyte.nexus.features.minigames.models.perks.common.ParticleProjectilePerk;
import me.pugabyte.nexus.features.regionapi.events.player.PlayerEnteredRegionEvent;
import me.pugabyte.nexus.models.perkowner.PerkOwner;
import me.pugabyte.nexus.models.perkowner.PerkOwnerService;
import me.pugabyte.nexus.utils.MaterialTag;
import me.pugabyte.nexus.utils.PlayerUtils;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.text.DecimalFormat;

import static me.pugabyte.nexus.utils.PlayerUtils.runCommand;
import static me.pugabyte.nexus.utils.PlayerUtils.runCommandAsOp;
import static me.pugabyte.nexus.utils.StringUtils.getShortLocationString;

public class MatchListener implements Listener {

	@EventHandler
	public void onInventoryOpen(InventoryOpenEvent event) {
		Minigamer minigamer = PlayerManager.get(event.getPlayer());
		if (minigamer.getMatch() == null) return;
		if (event.getInventory().getLocation() == null) return;
		if (minigamer.getMatch().getMechanic().canOpenInventoryBlocks()) return;
		event.setCancelled(true);
	}

	@EventHandler
	public void onMatchStart(MatchStartEvent event) {
		event.getMatch().getPlayers().forEach(this::disableCheats);
	}

	@EventHandler
	public void onMatchJoin(MatchJoinEvent event) {
		disableCheats(event.getMinigamer().getPlayer());
	}

	public void disableCheats(Player player) {
		if (player.hasPermission("voxelsniper.sniper"))
			runCommand(player, "b paint");
		if (player.hasPermission("worldguard.region.bypass.*"))
			runCommand(player, "wgedit off");
		if (PlayerUtils.isVanished(player))
			runCommand(player, "vanish off");
		runCommandAsOp(player, "pweather clear");
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onTeleport(PlayerTeleportEvent event) {
		Minigamer minigamer = PlayerManager.get(event.getPlayer());
		if (minigamer.getMatch() == null) return;
		if (minigamer.canTeleport()) return;
		if (event.getFrom().getWorld().equals(event.getTo().getWorld()))
			if (event.getFrom().distance(event.getTo()) < 2)
				return;

		event.setCancelled(true);
		Nexus.debug("Cancelled minigamer " + minigamer.getNickname() + " teleporting from " + getShortLocationString(event.getFrom()) + " to " + getShortLocationString(event.getTo()));
		minigamer.tell("&cYou cannot teleport while in a game! &3If you are trying to leave, use &c/mgm quit");
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		Minigamer minigamer = PlayerManager.get(event.getPlayer());
		if (minigamer.getMatch() == null) return;

		minigamer.quit();
	}

	@EventHandler
	public void onMatchQuit(MinigamerQuitEvent event) {
		MatchManager.janitor();
	}

	// TODO: Break and place events

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		Minigamer minigamer = PlayerManager.get(player);
		if (!minigamer.isPlaying()) return;

		minigamer.getMatch().getMechanic().onPlayerInteract(minigamer, event);
	}

	@EventHandler(ignoreCancelled = true)
	public void onInventoryClick(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player)) return;
		Minigamer minigamer = PlayerManager.get(event.getWhoClicked());
		if (!minigamer.isPlaying()) return;
		Mechanic mechanic = minigamer.getMatch().getMechanic();
		ItemStack item = event.getCurrentItem();

		if (event.getClickedInventory() instanceof PlayerInventory) {
			if (event.getSlotType() == InventoryType.SlotType.ARMOR && (!mechanic.canMoveArmor() || (item != null && !MaterialTag.WEARABLE.isTagged(item.getType()))))
				event.setCancelled(true);
		} else if (!mechanic.canOpenInventoryBlocks()) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onItemDrop(PlayerDropItemEvent event) {
		Minigamer minigamer = PlayerManager.get(event.getPlayer());
		if (!minigamer.isPlaying()) return;

		ItemStack item = event.getItemDrop().getItemStack();
		if (!minigamer.getMatch().getMechanic().canDropItem(item))
			event.setCancelled(true);
	}

	// TODO: Prevent damage of hanging entities/armor stands/etc
	public void onDamage(EntityDamageByEntityEvent event) {
		if (event.isCancelled()) return;
		if (!(event.getEntity() instanceof Player)) return;

		Minigamer victim = PlayerManager.get(event.getEntity());
		// block damage while in lobby

		if ((victim.isPlaying() && !victim.getMatch().isStarted()) || !victim.isAlive() || victim.isRespawning()) {
			event.setCancelled(true);
			return;
		}

		Minigamer attacker = null;
		Projectile projectile = null;
		if (event.getDamager() instanceof Player) {
			attacker = PlayerManager.get(event.getDamager());
		} else if (event.getDamager() instanceof Projectile) {
			projectile = (Projectile) event.getDamager();
			if (projectile.getShooter() instanceof Player) {
				attacker = PlayerManager.get((Player) projectile.getShooter());
			}
		}

		if (victim.getMatch() == null || victim.getTeam() == null) {
			if (attacker != null && (attacker.getMatch() == null || attacker.getTeam() == null)) {
				if (victim.getMatch() != null && attacker.getMatch() == null) {
					// Normal player damaging someone in a minigame
					event.setCancelled(true);
				}
				if (victim.getMatch() == null && attacker.getMatch() != null) {
					// Minigamer damaging normal player
					event.setCancelled(true);
				}
			}
			// Neither in minigames, ignore
			return;
		}

		if (attacker != null) {
			if (!(victim.isPlaying() && attacker.isPlaying())) return;

			if ((victim.isRespawning() || attacker.isRespawning()) || victim.equals(attacker)) {
				event.setCancelled(true);
				return;
			}

			if (!victim.getMatch().isStarted()) {
				event.setCancelled(true);
				return;
			}
		}

		if (attacker == null || victim.getMatch().equals(attacker.getMatch())) {
			// Same match
			Mechanic mechanic = victim.getMatch().getMechanic();
			if (attacker != null && victim.getTeam().equals(attacker.getTeam()) && mechanic.isTeamGame()) {
				// Friendly fire
				event.setCancelled(true);
			} else {
				// Damaged by opponent
				double newHealth = victim.getPlayer().getHealth() - event.getFinalDamage();

				if (newHealth > 0) {
					MinigamerDamageEvent damageEvent = new MinigamerDamageEvent(victim, attacker, event);
					if (!damageEvent.callEvent()) {
						event.setCancelled(true);
						return;
					}

					if (attacker != null && event.getDamager() instanceof Arrow)
						attacker.tell("&7" + victim.getNickname() + " is on &c" + new DecimalFormat("#.0").format(newHealth) + " &7HP");

					mechanic.onDamage(damageEvent);

					if (damageEvent.isCancelled())
						event.setCancelled(true);
					return;
				}

				event.setCancelled(true);

				if (projectile != null)
					projectile.remove();

				if (attacker != null && event.getDamager() instanceof Arrow)
					attacker.getPlayer().playSound(attacker.getPlayer().getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 0.3F, 0.1F);

				MinigamerDeathEvent deathEvent = new MinigamerDeathEvent(victim, attacker, event);
				if (!deathEvent.callEvent()) return;

				if (!victim.getMatch().isEnded())
					mechanic.onDeath(deathEvent);
			}
		} else {
			// Different matches
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onDamage(EntityDamageEvent event) {
		if (event instanceof EntityDamageByEntityEvent entityDamageByEntityEvent) {
			if (!(entityDamageByEntityEvent.getDamager() instanceof FallingBlock)) {
				onDamage(entityDamageByEntityEvent);
				return;
			}
		}

		if (!(event.getEntity() instanceof Player)) return;

		Minigamer victim = PlayerManager.get(event.getEntity());

		if (!victim.isPlaying()) return;

		// block damage while in lobby
		if (!victim.isAlive() || victim.isRespawning() || !victim.getMatch().isStarted()) {
			event.setCancelled(true);
			return;
		}

		Mechanic mechanic = victim.getMatch().getMechanic();

		if (event.getFinalDamage() < victim.getPlayer().getHealth()) {
			MinigamerDamageEvent damageEvent = new MinigamerDamageEvent(victim, event);
			if (!damageEvent.callEvent()) {
				event.setCancelled(true);
				return;
			}

			mechanic.onDamage(damageEvent);

			if (damageEvent.isCancelled())
				event.setCancelled(true);
			return;
		}

		event.setCancelled(true);

		MinigamerDeathEvent deathEvent = new MinigamerDeathEvent(victim, event);
		if (!deathEvent.callEvent())
			return;

		if (!victim.getMatch().isEnded())
			mechanic.onDeath(deathEvent);
	}

	@EventHandler
	public void onEnterKillRegion(PlayerEnteredRegionEvent event) {
		Minigamer minigamer = PlayerManager.get(event.getPlayer());
		if (!minigamer.isPlaying()) return;
		if (!minigamer.getMatch().isStarted() || !minigamer.isAlive()) return;
		Mechanic mechanic = minigamer.getMatch().getMechanic();

		Arena arena = minigamer.getMatch().getArena();
		if (arena.ownsRegion(event.getRegion(), "kill"))
			mechanic.kill(minigamer);
	}

	@EventHandler
	public void onItemPickup(EntityPickupItemEvent event) {
		if (!Minigames.isMinigameWorld(event.getEntity().getWorld())) return;
		// TODO: Entity pickups?
		if (!(event.getEntity() instanceof Player player)) return;

		Arena arena = ArenaManager.getFromLocation(event.getItem().getLocation());
		if (arena == null) return;
		Match match = MatchManager.find(arena);
		if (match == null) return;
		Minigamer minigamer = PlayerManager.get(player);
		if (!minigamer.isIn(match))
			event.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityRegainHealth(EntityRegainHealthEvent event) {
		if (!(event.getEntity() instanceof Player)) return;

		RegainReason regainReason = event.getRegainReason();
		if (regainReason != RegainReason.REGEN && regainReason != RegainReason.SATIATED)
			return;

		Minigamer minigamer = PlayerManager.get(event.getEntity());
		if (!minigamer.isPlaying()) return;
		if (!minigamer.getMatch().isStarted() || !minigamer.isAlive()) return;
		Mechanic mechanic = minigamer.getMatch().getMechanic();

		if (mechanic.usesAlternativeRegen())
			event.setCancelled(true);
	}

	public void onShootProjectile(Player player, Projectile projectile) {
		Minigamer minigamer = PlayerManager.get(player);
		if (!minigamer.isPlaying())
			return;
		Minigames.getModifier().onProjectileSpawn(projectile);
		PerkOwner owner = new PerkOwnerService().get(player);
		owner.getEnabledPerksByClass(ParticleProjectilePerk.class).forEach(perk -> new ParticleProjectile(perk, projectile, minigamer.getMatch()));
	}

	@EventHandler(ignoreCancelled = true)
	public void onBowShoot(EntityShootBowEvent event) {
		if (!(event.getEntity() instanceof Player))
			return;
		if (!(event.getProjectile() instanceof Projectile))
			return;
		onShootProjectile((Player) event.getEntity(), (Projectile) event.getProjectile());
	}

	@EventHandler(ignoreCancelled = true)
	public void onProjectileFire(PlayerLaunchProjectileEvent event) {
		onShootProjectile(event.getPlayer(), event.getProjectile());
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onProjectileCollide(ProjectileCollideEvent event) {
		if (!(event.getCollidedWith() instanceof Player)) return;
		if (!(event.getEntity().getShooter() instanceof Player)) return;
		Minigamer victim = PlayerManager.get(event.getCollidedWith());
		if (!victim.isPlaying()) return;
		if (victim.isRespawning() || !victim.isAlive()) {
			event.setCancelled(true);
			return;
		}
		if (victim.getTeam() == null) return;
		if (!victim.getMatch().getMechanic().isTeamGame()) return;
		Minigamer attacker = PlayerManager.get((Player) event.getEntity().getShooter());
		if (!attacker.isPlaying(victim.getMatch())) return;
		if (!victim.getTeam().equals(attacker.getTeam())) return;
		event.setCancelled(true);
	}

	@EventHandler
	public void onLoadout(MinigamerLoadoutEvent event) {
		Minigames.getModifier().afterLoadout(event.getMinigamer());
	}

	@EventHandler
	public void onDisplayTimer(MinigamerDisplayTimerEvent event) {
		event.getMatch().getMechanic().onDisplayTimer(event);
	}
}
