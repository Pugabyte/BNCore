package me.pugabyte.nexus.features.minigames.mechanics;

import lombok.Getter;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.minigames.Minigames;
import me.pugabyte.nexus.features.minigames.managers.PlayerManager;
import me.pugabyte.nexus.features.minigames.models.Match;
import me.pugabyte.nexus.features.minigames.models.Minigamer;
import me.pugabyte.nexus.features.minigames.models.Team;
import me.pugabyte.nexus.features.minigames.models.annotations.Railgun;
import me.pugabyte.nexus.features.minigames.models.annotations.Scoreboard;
import me.pugabyte.nexus.features.minigames.models.arenas.MurderArena;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchStartEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchTimerTickEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.minigamers.MinigamerDamageEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.minigamers.MinigamerDeathEvent;
import me.pugabyte.nexus.features.minigames.models.exceptions.MinigameException;
import me.pugabyte.nexus.features.minigames.models.matchdata.MurderMatchData;
import me.pugabyte.nexus.features.minigames.models.mechanics.multiplayer.teams.TeamMechanic;
import me.pugabyte.nexus.features.minigames.models.scoreboards.MinigameScoreboard.Type;
import me.pugabyte.nexus.utils.ItemBuilder;
import me.pugabyte.nexus.utils.JsonBuilder;
import me.pugabyte.nexus.utils.MaterialTag;
import me.pugabyte.nexus.utils.RandomUtils;
import me.pugabyte.nexus.utils.Tasks.Countdown;
import me.pugabyte.nexus.utils.Utils.ActionGroup;
import me.pugabyte.nexus.utils.WorldGuardUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static me.pugabyte.nexus.utils.LocationUtils.getBlockHit;
import static me.pugabyte.nexus.utils.StringUtils.stripColor;

@Railgun
@Scoreboard(teams = false, sidebarType = Type.MATCH, visibleNameTags = false)
public class Murder extends TeamMechanic {

	@Override
	public @NotNull String getName() {
		return "Murder";
	}

	@Override
	public @NotNull String getDescription() {
		return "Try to find and stop the villager who is secretly murderering your fellow villagers";
	}

	@Override
	public @NotNull ItemStack getMenuItem() {
		return new ItemStack(Material.IRON_SWORD);
	}

	@Override
	public boolean canDropItem(@NotNull ItemStack item) {
		return item.getType() == Material.IRON_INGOT;
	}

	@Override
	public boolean usesAutoBalancing() {
		return false;
	}

	@Override
	public boolean usesTeamChannels() {
		return false;
	}

	@Override
	public boolean hideTeamLoadoutColors() {
		return true;
	}

	@Override
	public void onStart(@NotNull MatchStartEvent event) {
		super.onStart(event);

		List<Minigamer> list = event.getMatch().getAliveMinigamers();

		// Don't try to process any further if there's only one player; causes crashes
		if (list.size() < 2) return;

		assignGunner(event.getMatch());
		sendAssignMessages(event.getMatch());

		for (Minigamer minigamer : list) {
			if (isMurderer(minigamer))
				minigamer.getMatch().getTasks().repeat(2, 5, () -> {
					double dist = 1000;
					Player target = null;
					if (minigamer.getMatch() == null) return;

					// Find the closest player by looping all minigame
					// players and saving the shortest distance
					for (Minigamer _minigamer : minigamer.getMatch().getMinigamers())
						if (_minigamer != minigamer && _minigamer.isAlive())
							if (minigamer.getPlayer().getLocation().distance(_minigamer.getPlayer().getLocation()) < dist) {
								// New shortest distance, save data
								dist = minigamer.getPlayer().getLocation().distance(_minigamer.getPlayer().getLocation());
								target = _minigamer.getPlayer();
							}

					if (target != null)
						// Set compass location to nearest player
						minigamer.getPlayer().setCompassTarget(target.getLocation());
				});
			else {
				minigamer.getPlayer().setFoodLevel(3);
				minigamer.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 1000000, 255, true, false, false));
			}
		}
	}

	@Override
	public void onDeath(@NotNull MinigamerDeathEvent event) {
		Minigamer victim = event.getMinigamer();
		Minigamer attacker = event.getAttacker();
		spawnCorpse(victim);

		// If the attacker was a gunner and the victim was not the murderer, intoxicate
		if (isGunner(attacker) && !isMurderer(victim))
			intoxicate(attacker.getPlayer());

		// Drop a gun if they had one
		if (isGunner(victim))
			victim.getPlayer().getLocation().getWorld().dropItem(victim.getPlayer().getLocation(), gun);

		event.setDeathMessage(victim.getColoredName() + " &3died");
		victim.tell("You were killed!");
		super.onDeath(event);
	}

	@Override
	public void announceWinners(@NotNull Match match) {
		MurderMatchData matchData = match.getMatchData();
		Minigamer murderer = getMurderer(match);
		Minigamer hero = matchData.getHero();

		JsonBuilder builder = new JsonBuilder();
		if (!murderer.isAlive())
			builder.next(murderer.getNickname(), NamedTextColor.RED)
					.next(" has been stopped by ")
					.next(hero.getNickname(), NamedTextColor.BLUE)
					.next(" on ");
		else if (match.getTimer().getTime() != 0)
			builder.next(murderer.getNickname(), NamedTextColor.RED).next(" has won on ");
		else
			builder.content("The ")
					.next("innocents", NamedTextColor.BLUE)
					.next(" have won");

		Minigames.broadcast(builder.next(match.getArena()).build());
	}

	@Override
	public void onDamage(@NotNull MinigamerDamageEvent event) {
		super.onDamage(event);

		if (event.getOriginalEvent() != null && event.getOriginalEvent() instanceof EntityDamageByEntityEvent originalEvent) {
			if (
					isMurderer(event.getAttacker()) &&
					originalEvent.getCause() == DamageCause.ENTITY_ATTACK &&
					originalEvent.getEntityType() == EntityType.PLAYER &&
					event.getAttacker().getPlayer().getInventory().getItemInMainHand().getType() == Material.IRON_SWORD
			) {
				// Staby-stab
				event.setCancelled(true);
				kill(event.getMinigamer());
			}
		}
	}

	public @NotNull Map<String, Integer> getScoreboardLines(@NotNull Match match) {
		return new HashMap<>() {{
			match.getMinigamers().stream().filter(Minigamer::isAlive)
					.forEach(minigamer -> put(minigamer.getNickname(), 0));
		}};
	}

	public void spawnCorpse(Minigamer minigamer) {
		// TODO: Sleeping NPC?
		Player player = minigamer.getPlayer();
		ArmorStand armorStand = minigamer.getMatch().spawn(player.getLocation().add(0, -1.4, 0), ArmorStand.class);
		armorStand.setGravity(false);
		armorStand.setVisible(false);
		armorStand.setHelmet(new ItemBuilder(Material.PLAYER_HEAD).skullOwner(player).build());
		// armorStand.setDisableSlots?
	}

	private void assignGunner(Match match) {
		Minigamer gunner = RandomUtils.randomElement(match.getAliveMinigamers().stream()
				.filter(minigamer -> !isMurderer(minigamer))
				.collect(Collectors.toList()));

		if (gunner != null) {
			MurderMatchData matchData = match.getMatchData();
			gunner.getPlayer().getInventory().setItem(1, gun);
		}
	}

	private void sendAssignMessages(Match match) {
		match.getAliveMinigamers().forEach(minigamer -> {
			if (isMurderer(minigamer)) {
				((MurderMatchData) match.getMatchData()).setMurderer(minigamer);
				minigamer.tell("You are the &cmurderer&3! Kill everyone in your path, but don't get caught!");
			} else if (isGunner(minigamer))
				minigamer.tell("You are the &6gunner&3! Find the murderer and shoot them with your gun.");
			else
				minigamer.tell("You are an &9innocent&3! Try to find scraps to craft a gun.");
		});
	}

	@EventHandler
	private void onPlayerInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		Minigamer minigamer = PlayerManager.get(player);
		if (!minigamer.isPlaying(this)) return;

		if (!ActionGroup.RIGHT_CLICK.applies(event)) return;

		if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
			List<Material> allowedRedstone = new ArrayList<>() {{
				add(Material.LEVER);
			}};
			allowedRedstone.addAll(MaterialTag.BUTTONS.getValues());
			if (allowedRedstone.contains(event.getClickedBlock().getType()))
				return;

			if ("RavensNestEstate".equalsIgnoreCase(minigamer.getMatch().getArena().getName()))
				return;
		}

		event.setCancelled(true);

		// Gunner shooting handled in AnnotationsListener (see @Railgun)

		switch (player.getInventory().getItemInMainHand().getType()) {
			case IRON_SWORD -> throwKnife(minigamer);
			case TRIPWIRE_HOOK -> retrieveKnife(minigamer);
			case ENDER_EYE -> useBloodlust(minigamer);
			case ENDER_PEARL -> useTeleporter(minigamer);
			case SUGAR -> useAdrenaline(minigamer);
		}
	}

	private void retrieveKnife(Minigamer minigamer) {
		Player player = minigamer.getPlayer();
		if (!player.getInventory().getItemInMainHand().getItemMeta().getDisplayName().contains(" in ")) {
			player.getWorld().strikeLightningEffect(player.getLocation());
			player.getInventory().setItem(1, knife);
		}
	}

	private void useBloodlust(Minigamer minigamer) {
		minigamer.tell("You used bloodlust!");
		minigamer.getMatch().broadcast("The murderer used bloodlust!");
		minigamer.getPlayer().getInventory().remove(Material.ENDER_EYE);

		minigamer.getMatch().getTasks().countdown(Countdown.builder()
				.duration(20 * 14)
				.onSecond(i -> {
					if (i % 2 == 0)
						minigamer.getMatch().getAliveMinigamers().forEach(_minigamer -> {
							Player player = _minigamer.getPlayer();
							player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BREATH, SoundCategory.MASTER, 2F, 0.1F);
							player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 40, 1, false, false));
							// TODO SkriptFunctions.redTint(player, 0.5, 10);
						});
				}));
	}

	private void useAdrenaline(Minigamer minigamer) {
		Player player = minigamer.getPlayer();
		player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 80, 2));
		player.getInventory().remove(Material.SUGAR);
	}

	private void useTeleporter(Minigamer minigamer) {
		Team innocentTeam = getInnocentTeam(minigamer.getMatch());
		innocentTeam.toSpawnpoints(minigamer.getMatch());

		minigamer.tell("You used the teleporter!");
		minigamer.getMatch().broadcast("The murderer used the teleporter!");
		minigamer.getPlayer().getInventory().remove(Material.ENDER_PEARL);
	}

	private void throwKnife(Minigamer minigamer) {
		Player player = minigamer.getPlayer();
		player.launchProjectile(Arrow.class);
		player.getInventory().remove(Material.IRON_SWORD);

		// Retrieval mechanics
		if (player.getInventory().contains(Material.TRIPWIRE_HOOK)) {
			player.getInventory().remove(Material.TRIPWIRE_HOOK);
			player.getInventory().setItem(1, retriever);
			// Start countdown
			ItemMeta meta = player.getInventory().getItem(1).getItemMeta();
			meta.setDisplayName(ChatColor.YELLOW + "Retrieve the knife in " + ChatColor.RED + "10");
			player.getInventory().getItem(1).setItemMeta(meta);
			// Sync delayed task that cancels once it reaches 0
			new Retriever(player).runTaskTimer(Nexus.getInstance(), 0, 20);
		}
	}

	@EventHandler
	public void onProjectileHit(ProjectileHitEvent event) {
		// Make sure the shooter & victim are players
		if (!(event.getEntity().getShooter() != null && event.getEntity().getShooter() instanceof Player)) return;
		Minigamer attacker = PlayerManager.get((Player) event.getEntity().getShooter());
		Minigamer victim = null;
		if (event.getHitEntity() != null && event.getHitEntity() instanceof Player)
			victim = PlayerManager.get(event.getHitEntity());

		if (!attacker.isPlaying(this)) return;
		if (!isMurderer(attacker)) return;

		// If it was an arrow, it was from a knife throw, so we want to spawn a knife item
		if (event.getEntityType() == EntityType.ARROW) {
			World world = attacker.getPlayer().getWorld();
			Block hitBlock = getBlockHit(event);
			if (hitBlock != null) {
				world.dropItem(hitBlock.getLocation(), knife);
				event.getEntity().remove();
			} else if (event.getHitEntity() != null) {
				world.dropItem(event.getHitEntity().getLocation(), knife);
			}
		}

		if (victim != null)
			kill(victim);
	}

	@EventHandler
	public void onDrop(PlayerDropItemEvent event) {
		Minigamer minigamer = PlayerManager.get(event.getPlayer());
		if (!minigamer.isPlaying(this)) return;

		// Only allow iron ingots to be dropped
		if (event.getItemDrop().getItemStack().getType() != Material.IRON_INGOT)
			event.setCancelled(true);
	}

	@EventHandler
	public void onPickup(EntityPickupItemEvent event) {
		if (!(event.getEntity() instanceof Player player)) return;
		Minigamer minigamer = PlayerManager.get(player);
		if (!minigamer.isPlaying(this)) return;

		event.setCancelled(true);

		// Picking up scrap
		if (event.getItem().getItemStack().getType() == Material.IRON_INGOT) {

			if (isGunner(player)) return;

			boolean isMurderer = isMurderer(player);

			// Fake pick up
			event.getItem().remove();

			int amount = 0;
			try {
				amount = player.getInventory().getItem(8).getAmount();
			} catch (Exception ignore) {}

			if (amount == 0) {
				// First scrap
				player.getInventory().setItem(8, (isMurderer) ? fakeScrap : scrap);
				if (!isMurderer)
					minigamer.tell("You collected a scrap " + ChatColor.GRAY + "(1/10)");
				return;
			}

			if (amount == 9) {
				if (isMurderer(player)) return;

				// Craft a gun
				player.getInventory().remove(Material.IRON_INGOT);
				player.getInventory().setItem(1, gun);
				minigamer.tell("You collected enough scrap to craft a gun!");
				return;
			}

			// Normal pick up
			player.getInventory().addItem((isMurderer) ? fakeScrap : scrap);
			if (!isMurderer)
				minigamer.tell("You collected a scrap " + ChatColor.GRAY + "(" + (amount + 1) + "/10)");
			return;
		}

		// Picking up gun
		if (event.getItem().getItemStack().getType() == Material.IRON_HOE) {
			if (isInnocent(player)) {
				// Innocent found a gun
				event.getItem().remove();
				player.getInventory().remove(Material.IRON_INGOT);
				player.getInventory().setItem(1, gun);
			}

			return;
		}

		// Picking up knife
		if (event.getItem().getItemStack().getType() == Material.IRON_SWORD) {
			if (isMurderer(player)) {
				// If they already have a knife, don't pick it up
				if (player.getInventory().contains(Material.IRON_SWORD)) return;

				if (player.getInventory().contains(Material.TRIPWIRE_HOOK))
					// They didn't use their retriever, move it back to their inventoryContents
					player.getInventory().setItem(17, retriever);

				// Put the knife in the correct slot
				event.getItem().remove();
				player.getInventory().setItem(1, knife);
			}
		}
	}

	@EventHandler
	public void onItemMerge(ItemMergeEvent event) {
		if (new WorldGuardUtils(event.getEntity()).getRegionsLikeAt("murder_.*", event.getTarget().getLocation()).size() > 0)
			event.setCancelled(true);
	}

	public static class Retriever extends BukkitRunnable {
		private Player player;
		private int time;

		public Retriever(Player player) {
			this.player = player;
			String[] name = player.getInventory().getItem(1).getItemMeta().getDisplayName().split(" in ");
			this.time = Integer.parseInt(stripColor(name[1]));
		}

		@Override
		public void run() {
			ItemStack item = player.getInventory().getItem(1);

			// If its not the retriever item, they found the knife, so cancel
			if (item == null || item.getType() != Material.TRIPWIRE_HOOK) {
				this.cancel();
				return;
			}

			ItemMeta meta = item.getItemMeta();

			if (time > 0) {
				meta.setDisplayName(ChatColor.YELLOW + "Retrieve the knife in " + ChatColor.RED + time--);
				player.getInventory().getItem(1).setItemMeta(meta);
			} else {
				meta.setDisplayName(ChatColor.YELLOW + "Retrieve the knife");
				player.getInventory().getItem(1).setItemMeta(meta);
				this.cancel();
			}
		}
	}

	public static void intoxicate(Player player) {
		// Remove and drop gun
		player.getInventory().remove(Material.IRON_HOE);
		player.getLocation().getWorld().dropItem(player.getLocation(), gun);

		// Make drunk
		player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 1200, 2));
		player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 1200, 4));
		// Glass bottle will signify that they are 'drunk' and can't pick up guns
		player.getInventory().setItem(17, new ItemStack(Material.GLASS_BOTTLE));
	}

	private Team getInnocentTeam(Match match) {
		Minigamer murderer = getMurderer(match);
		Optional<Team> first = match.getArena().getTeams().stream().filter(team -> team != murderer.getTeam()).findFirst();
		return first.orElseThrow(() -> new MinigameException("Could not find innocents team"));
	}

	public boolean isMurderer(Minigamer minigamer) {
		if (minigamer == null) return false;
		return isMurderer(minigamer.getPlayer());
	}

	public boolean isMurderer(Player player) {
		return player.getInventory().contains(Material.COMPASS);
	}

	public boolean isGunner(Minigamer minigamer) {
		if (minigamer == null) return false;
		return isGunner(minigamer.getPlayer());
	}

	public boolean isGunner(Player player) {
		return player.getInventory().contains(Material.IRON_HOE);
	}

	public boolean isDrunk(Minigamer minigamer) {
		if (minigamer == null) return false;
		return isDrunk(minigamer.getPlayer());
	}

	public boolean isDrunk(Player player) {
		return player.getInventory().contains(Material.GLASS_BOTTLE);
	}

	public boolean isInnocent(Minigamer minigamer) {
		if (minigamer == null) return false;
		return isInnocent(minigamer.getPlayer());
	}

	public boolean isInnocent(Player player) {
		return !(isGunner(player) || isMurderer(player) || isDrunk(player));
	}

	private Minigamer getMurderer(Match match) {
		return ((MurderMatchData)match.getMatchData()).getMurderer();
	}

	private Set<Minigamer> getGunners(Match match) {
		return match.getAliveMinigamers().stream().filter(this::isGunner).collect(Collectors.toSet());
	}

	@EventHandler
	public void onTimeTick(MatchTimerTickEvent event) {
		if (!event.getMatch().isMechanic(this))
			return;

		event.getMatch().getMinigamers().forEach(minigamer -> {
			String teamName;
			if (!minigamer.isAlive())
				teamName = "&cdead";
			else if (isMurderer(minigamer))
				teamName = "the &cMurderer";
			else if (isGunner(minigamer))
				teamName = "a &6Gunner";
			else if (isDrunk(minigamer))
				teamName = "a &#ad7a13Drunkard";
			else
				teamName = "an &9Innocent";
			sendBarWithTimer(minigamer, "&3You are "+teamName);

			int foodLevel = (!minigamer.isAlive() || isMurderer(minigamer)) ? 18 : 3;
			minigamer.getPlayer().setFoodLevel(foodLevel);
		});

		int arenaDuration = event.getMatch().getArena().getSeconds();
		// get elapsed time
		int seconds;
		if (arenaDuration > 0)
			seconds = arenaDuration - event.getTime();
		else
			seconds = event.getTime();
		// calculate formula
		List<Location> scrapPoints = ((MurderArena) event.getMatch().getArena()).getScrapPoints();
		// spawns 1 scrap every 4 seconds on average at the start of the game, increasing in quantity as the round progresses
		// i had an explanation for this formula at one point but then i manually tweaked the numbers a bunch and it's not applicable anymore
		// contact lexikiq if scrap is spawning too much and numbers need tweaking lol
		double spawnChancePerPoint = ((3d/8d)/scrapPoints.size()) + ((seconds*event.getMatch().getMinigamers().size())/144000d);
		// drop scraps
		scrapPoints.forEach(location -> {
			if (RandomUtils.getRandom().nextDouble() < spawnChancePerPoint)
				location.getWorld().dropItemNaturally(location, scrap);
		});
	}

	@Getter
	public static ItemStack knife = new ItemBuilder(Material.IRON_SWORD)
			.name("&cKnife")
			.lore("&fUse this to murder innocents!", "&eLeft-click &fto attack", "&eRight-click &fto throw")
			.build();

	@Getter
	public static ItemStack gun = new ItemBuilder(Material.IRON_HOE)
			.name("&eGun")
			.lore("&fUse this to kill the murderer!", "&eRight-click &fto shoot")
			.build();

	@Getter
	public static ItemStack scrap = new ItemBuilder(Material.IRON_INGOT)
			.name("&eScrap")
			.lore("&fCollect 10 to craft a gun!")
			.build();

	@Getter
	public static ItemStack fakeScrap = new ItemBuilder(Material.IRON_INGOT)
			.name("&eFake Scrap")
			.lore("&fUse this to fool the innocents!")
			.build();

	@Getter
	public static ItemStack compass = new ItemBuilder(Material.COMPASS)
			.name("&eLocator")
			.lore("&fPoints to the closest innocent")
			.build();

	@Getter
	public static ItemStack teleporter = new ItemBuilder(Material.ENDER_PEARL)
			.name("&eTeleporter")
			.lore("&eRight-click &fto teleport all innocents to random locations")
			.build();

	@Getter
	public static ItemStack adrenaline = new ItemBuilder(Material.SUGAR)
			.name("&eAdrenaline")
			.lore("&eRight-click &fto receive a speed boost")
			.build();

	@Getter
	public static ItemStack bloodlust = new ItemBuilder(Material.ENDER_EYE)
			.name("&eBloodlust")
			.lore("&eRight-click &fto highlight all players, for a penalty!")
			.build();

	@Getter
	public static ItemStack retriever = new ItemBuilder(Material.TRIPWIRE_HOOK)
			.name("&eRetrieve knife")
			.lore("&eRight-click &fto retrieve the knife, for a penalty!")
			.build();

}
