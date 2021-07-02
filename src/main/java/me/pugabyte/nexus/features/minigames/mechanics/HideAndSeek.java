package me.pugabyte.nexus.features.minigames.mechanics;

import com.destroystokyo.paper.block.TargetBlockInfo;
import eden.utils.TimeUtils.Time;
import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MiscDisguise;
import me.pugabyte.nexus.features.menus.MenuUtils;
import me.pugabyte.nexus.features.minigames.Minigames;
import me.pugabyte.nexus.features.minigames.managers.PlayerManager;
import me.pugabyte.nexus.features.minigames.models.Match;
import me.pugabyte.nexus.features.minigames.models.Minigamer;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchEndEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchJoinEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchStartEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.MinigamerQuitEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.minigamers.MinigamerDeathEvent;
import me.pugabyte.nexus.features.minigames.models.matchdata.HideAndSeekMatchData;
import me.pugabyte.nexus.features.minigames.models.perks.Perk;
import me.pugabyte.nexus.utils.ItemBuilder;
import me.pugabyte.nexus.utils.JsonBuilder;
import me.pugabyte.nexus.utils.MaterialTag;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.SoundBuilder;
import me.pugabyte.nexus.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static me.pugabyte.nexus.utils.LocationUtils.blockLocationsEqual;
import static me.pugabyte.nexus.utils.LocationUtils.getCenteredLocation;
import static me.pugabyte.nexus.utils.StringUtils.camelCase;
import static me.pugabyte.nexus.utils.StringUtils.colorize;
import static me.pugabyte.nexus.utils.StringUtils.plural;

public class HideAndSeek extends Infection {
	private static final int SOLIDIFY_PLAYER_AT = Time.SECOND.x(5);

	@Override
	public @NotNull String getName() {
		return "Hide and Seek";
	}

	@Override
	public @NotNull String getDescription() {
		return "Disguise as a block and hide from the hunters";
	}

	@Override
	public @NotNull ItemStack getMenuItem() {
		return new ItemStack(Material.GRASS_BLOCK);
	}

	@Override
	public @NotNull GameMode getGameMode() {
		return GameMode.SURVIVAL;
	}

	@Override
	public boolean usesPerk(@NotNull Class<? extends Perk> perk, @NotNull Minigamer minigamer) {
		return isZombie(minigamer);
	}

	@Override
	public boolean canMoveArmor() {
		return false;
	}

	@Override
	public void onJoin(@NotNull MatchJoinEvent event) {
		super.onJoin(event);
		Minigamer minigamer = event.getMinigamer();
		Player player = minigamer.getPlayer();
		ItemStack menuItem = new ItemBuilder(Material.NETHER_STAR).name("&3&lSelect your Block").build();
		player.getInventory().setItem(0, menuItem);
	}

	// Select unique concrete blocks
	@EventHandler
	public void setPlayerBlock(PlayerInteractEvent event) {
		if (event.getItem() == null) return;
		if (event.getItem().getType() != Material.NETHER_STAR) return;
		if (!Utils.ActionGroup.CLICK_AIR.applies(event)) return;

		Player player = event.getPlayer();
		if (!player.getWorld().equals(Minigames.getWorld())) return;

		Minigamer minigamer = PlayerManager.get(player);
		if (!minigamer.isInLobby(this)) return;

		Match match = minigamer.getMatch();
		if (match.isStarted()) return;

		new HideAndSeekMenu(match).open(player);
	}

	@Override
	public void onStart(@NotNull MatchStartEvent event) {
		super.onStart(event);
		Match match = event.getMatch();
		HideAndSeekMatchData matchData = match.getMatchData();
		if (matchData.getMapMaterials().size() == 0) {
			error("Arena has no blocks whitelisted!", match);
			return;
		}

		for (Minigamer minigamer : match.getMinigamers()) {
			if (isZombie(minigamer)) {
				continue;
			}

			MiscDisguise disguise = new MiscDisguise(DisguiseType.FALLING_BLOCK, matchData.getBlockChoice(minigamer));
			disguise.setEntity(minigamer.getPlayer());
			disguise.startDisguise();
			matchData.getDisguises().put(minigamer.getPlayer().getUniqueId(), disguise);
			DisguiseAPI.setActionBarShown(minigamer.getPlayer(), false);
		}

		int taskId = match.getTasks().repeat(0, 1, () -> {
			for (Minigamer minigamer : getHumans(match)) {
				Player player = minigamer.getPlayer();
				UUID userId = player.getUniqueId();
				Map<Minigamer, Location> solidPlayers = matchData.getSolidPlayers();
				int immobileTicks = minigamer.getImmobileTicks();
				Material blockChoice = matchData.getBlockChoice(userId);
				Component blockName = Component.translatable(blockChoice.getTranslationKey());

				// if player just moved, break their disguise
				if (immobileTicks < SOLIDIFY_PLAYER_AT && solidPlayers.containsKey(minigamer)) {
					blockChange(minigamer, solidPlayers.remove(minigamer), Material.AIR);
					if (player.hasPotionEffect(PotionEffectType.INVISIBILITY))
						player.removePotionEffect(PotionEffectType.INVISIBILITY);
					FallingBlock fallingBlock = matchData.getSolidBlocks().remove(minigamer.getPlayer().getUniqueId());
					if (fallingBlock != null)
						fallingBlock.remove();
					matchData.getDisguises().get(minigamer.getPlayer().getUniqueId()).startDisguise();
				}

				// check how long they've been still
				if (immobileTicks < Time.SECOND.x(2)) {
					sendBarWithTimer(minigamer, new JsonBuilder("&bYou are currently partially disguised as a ").next(blockName));
				} else if (immobileTicks < SOLIDIFY_PLAYER_AT) {
					// countdown until solidification
					int seconds = (int) Math.ceil((SOLIDIFY_PLAYER_AT - immobileTicks) / 20d);
					String display = String.format(plural("&dFully disguising in %d second", seconds) + "...", seconds);
					sendBarWithTimer(minigamer, display);
				} else {
					if (!solidPlayers.containsKey(minigamer)) {
						Location location = minigamer.getPlayer().getLocation();
						if (immobileTicks == SOLIDIFY_PLAYER_AT && MaterialTag.ALL_AIR.isTagged(location.getBlock().getType())) {
							// save fake block location
							solidPlayers.put(minigamer, location);
							// create a falling block to render on the hider's client
							if (blockChoice.isSolid() && blockChoice.isOccluding()) {
								FallingBlock fallingBlock = minigamer.getPlayer().getWorld().spawnFallingBlock(getCenteredLocation(location), blockChoice.createBlockData());
								fallingBlock.setGravity(false);
								fallingBlock.setHurtEntities(false);
								fallingBlock.setDropItem(false);
								fallingBlock.setVelocity(new Vector());
								matchData.getSolidBlocks().put(minigamer.getPlayer().getUniqueId(), fallingBlock);
								// stop their disguise (as otherwise the hider sees 2 of their block)
								matchData.getDisguises().get(minigamer.getPlayer().getUniqueId()).stopDisguise();
							}
							// add invisibility to hide them/their falling block disguise
							player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 1000000, 1, true, false, false));
							// run usual ticking
							disguisedBlockTick(minigamer);
						} else
							sendBarWithTimer(minigamer, "&cYou cannot fully disguise inside non-air blocks!");
					} else {
						disguisedBlockTick(minigamer);
					}
				}
			}
		});
		match.getTasks().register(taskId);

		// separate task so this doesn't run as often
		int hunterTaskId = match.getTasks().repeat(0, 5, () -> getZombies(match).forEach(minigamer -> {
			Block block = minigamer.getPlayer().getTargetBlock(4, TargetBlockInfo.FluidMode.NEVER);
			if (block == null) return;
			Material type = block.getType();
			if (MaterialTag.ALL_AIR.isTagged(type)) return;
			Component name = Component.translatable(type.getTranslationKey());

			// this will create some grammatically weird messages ("Oak Planks is a possible hider")
			// idk what to do about that though
			JsonBuilder message = new JsonBuilder();
			if (matchData.getMapMaterials().contains(type))
				message.color(NamedTextColor.GREEN).next(name).next(" is a possible hider");
			else
				message.color(NamedTextColor.RED).next(name).next(" is not a possible hider");
			sendBarWithTimer(minigamer, message);
		}));
		match.getTasks().register(hunterTaskId);
	}

	private void disguisedBlockTick(Minigamer minigamer) {
		HideAndSeekMatchData matchData = minigamer.getMatch().getMatchData();
		Material blockChoice = matchData.getBlockChoice(minigamer);
		blockChange(minigamer, matchData.getSolidPlayers().get(minigamer), blockChoice);

		// todo: use a localization string for proper block name
		JsonBuilder message = new JsonBuilder("&aYou are currently fully disguised as a ").next(Component.translatable(blockChoice.getTranslationKey()));
		if (matchData.getSolidBlocks().containsKey(minigamer.getPlayer().getUniqueId())) {
			matchData.getSolidBlocks().get(minigamer.getPlayer().getUniqueId()).setTicksLived(1);
			if (!MaterialTag.ALL_AIR.isTagged(minigamer.getPlayer().getInventory().getItemInMainHand().getType()))
				message = new JsonBuilder("&cWarning: Your weapon is visible!");
		}
		sendBarWithTimer(minigamer, message);
	}

	protected void blockChange(Minigamer origin, Location location, Material block) {
		origin.getMatch().getMinigamers().forEach(minigamer -> {
			if (!minigamer.equals(origin))
				minigamer.getPlayer().sendBlockChange(location, block.createBlockData());
		});
	}

	@Override
	public @NotNull Map<String, Integer> getScoreboardLines(@NotNull Match match) {
		if (!match.isStarted() || match.getTimer().getTime() > match.getArena().getSeconds()/2)
			return super.getScoreboardLines(match);
		HideAndSeekMatchData matchData = match.getMatchData();
		Map<String, Integer> lines = new HashMap<>();
		List<Minigamer> humans = getHumans(match);
		lines.put("", 0);
		lines.put("&3&lPlayer Count", 0);
		lines.put("- " + getZombieTeam(match).getVanillaColoredName(), -1 * getZombies(match).size());
		lines.put("- " + getHumanTeam(match).getVanillaColoredName(), -1 * humans.size());

		lines.put("&3&lSurviving Blocks", 99);
		Map<Material, Integer> blockCounts = new HashMap<>();
		humans.forEach(minigamer -> {
			Material blockChoice = matchData.getBlockChoice(minigamer);
			blockCounts.compute(blockChoice, ($, integer) -> integer == null ? 1 : integer+1);
		});
		blockCounts.forEach((material, integer) -> lines.put(camelCase(material), integer));
		return lines;
	}

	public void cleanup(Minigamer minigamer) {
		DisguiseAPI.undisguiseToAll(minigamer.getPlayer());
	}

	public void cleanup(Match match) {
		match.getMinigamers().forEach(this::cleanup);
		((HideAndSeekMatchData) match.getMatchData()).getSolidBlocks().forEach(($, fallingBlock) -> fallingBlock.remove());
	}

	@Override
	public void onQuit(@NotNull MinigamerQuitEvent event) {
		super.onQuit(event);
		cleanup(event.getMinigamer());
	}

	@Override
	public void onEnd(@NotNull MatchEndEvent event) {
		super.onEnd(event);
		cleanup(event.getMatch());
	}

	@Override
	public void onDeath(@NotNull MinigamerDeathEvent event) {
		super.onDeath(event);
		cleanup(event.getMinigamer());
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		// this method is basically checking to see if a hunter has swung at a hider's fake block
		Minigamer minigamer = PlayerManager.get(event.getPlayer());

		if (
				minigamer.isPlaying(this) &&
						isZombie(minigamer) &&
						event.getAction() == Action.LEFT_CLICK_BLOCK &&
						event.getHand() != null &&
						event.getHand().equals(EquipmentSlot.HAND)
		) {
			HideAndSeekMatchData matchData = minigamer.getMatch().getMatchData();
			Location blockLocation;
			if (event.getClickedBlock() != null)
				blockLocation = event.getClickedBlock().getLocation();
			else {
				return;
			}

			for (Map.Entry<Minigamer, Location> entry : matchData.getSolidPlayers().entrySet()) {
				Minigamer target = entry.getKey();
				Location location = entry.getValue();
				if (blockLocationsEqual(blockLocation, location)) {
					EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(minigamer.getPlayer(), target.getPlayer(), EntityDamageEvent.DamageCause.ENTITY_ATTACK, 1);
					e.callEvent();
					if (e.isCancelled()) return;

					minigamer.getPlayer().attack(target.getPlayer());
					target.setImmobileTicks(0);
					new SoundBuilder(Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK).receiver(minigamer.getPlayer()).category(SoundCategory.PLAYERS).play();
					new SoundBuilder(Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK).receiver(target.getPlayer()).category(SoundCategory.PLAYERS).play();
					return;
				}
			}
		}
	}

	@Override
	public boolean canUseBlock(@NotNull Minigamer minigamer, @NotNull Block block) {
		return false;
	}

	public static class HideAndSeekMenu extends MenuUtils implements InventoryProvider {
		private final Match match;
		public HideAndSeekMenu(Match match) {
			this.match = match;
		}

		@Override
		public void open(Player viewer, int page) {
			SmartInventory.builder()
					.provider(this)
					.title(colorize("&3&lSelect your Block"))
					.size(getRows(match.getArena().getBlockList().size(), 1), 9)
					.build()
					.open(viewer, page);
		}

		@Override
		public void init(Player player, InventoryContents contents) {
			addCloseItem(contents);
			HideAndSeekMatchData matchData = match.getMatchData();
			List<Material> materials = matchData.getMapMaterials();
			List<ClickableItem> clickableItems = new ArrayList<>();
			materials.forEach(material -> {
				ItemStack itemStack = new ItemStack(material);
				clickableItems.add(ClickableItem.from(itemStack, e -> {
					matchData.getBlockChoices().put(player.getUniqueId(), material);
					player.closeInventory();
					PlayerUtils.send(player, new JsonBuilder("&3You have selected ").next(Component.translatable(material.getTranslationKey())));
				}));
			});
			addPagination(player, contents, clickableItems);
		}
	}
}
