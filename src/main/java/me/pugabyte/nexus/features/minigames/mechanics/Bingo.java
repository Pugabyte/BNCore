package me.pugabyte.nexus.features.minigames.mechanics;

import de.tr7zw.nbtapi.NBTItem;
import eden.utils.TimeUtils.Time;
import lombok.Getter;
import me.pugabyte.nexus.features.listeners.Misc.FixedCraftItemEvent;
import me.pugabyte.nexus.features.listeners.Misc.LivingEntityDamageByPlayerEvent;
import me.pugabyte.nexus.features.minigames.managers.MatchManager;
import me.pugabyte.nexus.features.minigames.managers.PlayerManager;
import me.pugabyte.nexus.features.minigames.models.Match;
import me.pugabyte.nexus.features.minigames.models.Minigamer;
import me.pugabyte.nexus.features.minigames.models.events.matches.minigamers.MinigamerDeathEvent;
import me.pugabyte.nexus.features.minigames.models.matchdata.BingoMatchData;
import me.pugabyte.nexus.features.minigames.models.mechanics.MechanicType;
import me.pugabyte.nexus.features.minigames.models.mechanics.custom.bingo.challenge.StructureChallenge;
import me.pugabyte.nexus.features.minigames.models.mechanics.custom.bingo.challenge.common.Challenge;
import me.pugabyte.nexus.features.minigames.models.mechanics.custom.bingo.progress.BiomeChallengeProgress;
import me.pugabyte.nexus.features.minigames.models.mechanics.custom.bingo.progress.BreakChallengeProgress;
import me.pugabyte.nexus.features.minigames.models.mechanics.custom.bingo.progress.ConsumeChallengeProgress;
import me.pugabyte.nexus.features.minigames.models.mechanics.custom.bingo.progress.CraftChallengeProgress;
import me.pugabyte.nexus.features.minigames.models.mechanics.custom.bingo.progress.DimensionChallengeProgress;
import me.pugabyte.nexus.features.minigames.models.mechanics.custom.bingo.progress.KillChallengeProgress;
import me.pugabyte.nexus.features.minigames.models.mechanics.custom.bingo.progress.ObtainChallengeProgress;
import me.pugabyte.nexus.features.minigames.models.mechanics.custom.bingo.progress.StructureChallengeProgress;
import me.pugabyte.nexus.features.minigames.models.mechanics.multiplayer.teamless.TeamlessVanillaMechanic;
import me.pugabyte.nexus.utils.ItemBuilder;
import me.pugabyte.nexus.utils.ItemUtils;
import me.pugabyte.nexus.utils.MaterialUtils;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.Tasks;
import me.pugabyte.nexus.utils.TitleUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.StructureType;
import org.bukkit.block.Biome;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static me.pugabyte.nexus.utils.ItemUtils.isNullOrAir;
import static org.bukkit.Material.CRAFTING_TABLE;

public final class Bingo extends TeamlessVanillaMechanic {

	private static final String NBT_KEY = "nexus.bingo.obtained";

	@Override
	public @NotNull String getName() {
		return "Bingo";
	}

	@Override
	public @NotNull String getDescription() {
		return "Fill out your &c/bingo &eboard from doing unique survival challenges";
	}

	@Override
	public @NotNull ItemStack getMenuItem() {
		return new ItemStack(CRAFTING_TABLE);
	}

	public final int matchRadius = 3000;
	@Getter
	public final int worldDiameter = 7000;
	@Getter
	public final String worldName = "bingo";

	@Override
	public void onDeath(@NotNull MinigamerDeathEvent event) {
		final Player player = event.getMinigamer().getPlayer();

		for (ItemStack itemStack : player.getInventory())
			if (!isNullOrAir(itemStack))
				player.getWorld().dropItemNaturally(player.getLocation(), itemStack);

		super.onDeath(event);
	}

	@Override
	public void onDeath(@NotNull Minigamer victim) {
		victim.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Time.SECOND.x(3), 10, false, false));
		TitleUtils.sendTitle(victim.getPlayer(), "&cYou died!", 150);

		final Location bed = victim.getPlayer().getBedSpawnLocation();
		if (bed != null && getWorld().equals(bed.getWorld()))
			victim.teleport(bed);
		else
			victim.teleport(victim.getMatch().<BingoMatchData>getMatchData().getData(victim).getSpawnpoint());
	}

	@Override
	public @NotNull CompletableFuture<Void> onRandomTeleport(@NotNull Match match, @NotNull Minigamer minigamer, @NotNull Location location) {
		super.onRandomTeleport(match, minigamer, location);
		return minigamer.getMatch().<BingoMatchData>getMatchData().spawnpoint(minigamer, location);
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onItemDrop(BlockDropItemEvent event) {
		if (!event.getPlayer().getWorld().getName().startsWith(worldName))
			return;

		for (Item item : event.getItems()) {
			ItemStack stack = item.getItemStack();
			Material ingot = MaterialUtils.oreToIngot(stack.getType());
			if (ingot == null)
				continue;

			stack.setType(ingot);
			item.setItemStack(stack);
			return;
		}
	}

	@EventHandler
	public void onBlock(BlockBreakEvent event) {
		final Minigamer minigamer = PlayerManager.get(event.getPlayer());
		if (!minigamer.isPlaying(this))
			return;

		final BingoMatchData matchData = minigamer.getMatch().getMatchData();
		final BreakChallengeProgress progress = matchData.getProgress(minigamer, BreakChallengeProgress.class);

		progress.getItems().add(new ItemStack(event.getBlock().getType(), 1));
	}

	@EventHandler
	public void onObtain(EntityPickupItemEvent event) {
		if (!(event.getEntity() instanceof Player player))
			return;

		final Minigamer minigamer = PlayerManager.get(player);
		if (!minigamer.isPlaying(this))
			return;

		final BingoMatchData matchData = minigamer.getMatch().getMatchData();
		final ObtainChallengeProgress progress = matchData.getProgress(minigamer, ObtainChallengeProgress.class);

		final ItemStack itemStack = event.getItem().getItemStack();
		final NBTItem nbtItem = new NBTItem(itemStack);
		if (nbtItem.hasKey(NBT_KEY))
			return;

		nbtItem.setBoolean(NBT_KEY, true);
		progress.getItems().add(nbtItem.getItem());
	}

	@EventHandler
	public void onObtain(InventoryCloseEvent event) {
		final Player player = (Player) event.getPlayer();
		final Minigamer minigamer = PlayerManager.get(player);
		if (!minigamer.isPlaying(this))
			return;

		final BingoMatchData matchData = minigamer.getMatch().getMatchData();
		final ObtainChallengeProgress progress = matchData.getProgress(minigamer, ObtainChallengeProgress.class);

		for (ItemStack itemStack : PlayerUtils.getAllInventoryContents(player)) {
			if (ItemUtils.isNullOrAir(itemStack))
				continue;

			final NBTItem nbtItem = new NBTItem(itemStack, true);
			if (nbtItem.hasKey(NBT_KEY))
				continue;

			nbtItem.setBoolean(NBT_KEY, true);
			progress.getItems().add(nbtItem.getItem());
		}
	}

	@EventHandler
	public void onCraft(FixedCraftItemEvent event) {
		final Minigamer minigamer = PlayerManager.get(event.getWhoClicked());
		if (!minigamer.isPlaying(this))
			return;

		final BingoMatchData matchData = minigamer.getMatch().getMatchData();
		final CraftChallengeProgress progress = matchData.getProgress(minigamer, CraftChallengeProgress.class);

		final ItemStack result = event.getResultItemStack();
		progress.getItems().add(result);
	}

	@EventHandler
	public void onKill(LivingEntityDamageByPlayerEvent event) {
		final Minigamer minigamer = PlayerManager.get(event.getAttacker());
		if (!minigamer.isPlaying(this))
			return;

		final LivingEntity entity = (LivingEntity) event.getOriginalEvent().getEntity();
		if (entity.getHealth() - event.getOriginalEvent().getFinalDamage() > 0)
			return;

		final BingoMatchData matchData = minigamer.getMatch().getMatchData();
		final KillChallengeProgress progress = matchData.getProgress(minigamer, KillChallengeProgress.class);

		progress.getKills().add(event.getEntity().getType());
	}

	@EventHandler
	public void onConsume(PlayerItemConsumeEvent event) {
		final Minigamer minigamer = PlayerManager.get(event.getPlayer());
		if (!minigamer.isPlaying(this))
			return;

		final BingoMatchData matchData = minigamer.getMatch().getMatchData();
		final ConsumeChallengeProgress progress = matchData.getProgress(minigamer, ConsumeChallengeProgress.class);

		if (event.getItem().getItemMeta() instanceof PotionMeta meta)
			if (meta.getCustomEffects().isEmpty())
				return;

		progress.getItems().add(ItemBuilder.oneOf(event.getItem()).build());
	}

	private static List<Minigamer> getActiveBingoMinigamers() {
		return new ArrayList<>() {{
			for (Match match : MatchManager.getAll()) {
				if (!match.isStarted())
					continue;
				if (match.getArena().getMechanicType() != MechanicType.BINGO)
					continue;

				addAll(match.getAliveMinigamers());
			}
		}};
	}

	static {
		Tasks.repeat(Time.SECOND.x(10), Time.SECOND.x(5), () -> {
			for (Minigamer minigamer : getActiveBingoMinigamers()) {
				final Match match = minigamer.getMatch();
				final BingoMatchData matchData = match.getMatchData();
				final BiomeChallengeProgress progress = matchData.getProgress(minigamer, BiomeChallengeProgress.class);
				final Biome biome = minigamer.getPlayer().getLocation().getBlock().getBiome();
				progress.getBiomes().add(biome);
			}
		});
	}

	@EventHandler
	public void onDimensionChange(PlayerChangedWorldEvent event) {
		final Minigamer minigamer = PlayerManager.get(event.getPlayer());
		if (!minigamer.isPlaying(this))
			return;

		final BingoMatchData matchData = minigamer.getMatch().getMatchData();
		final DimensionChallengeProgress progress = matchData.getProgress(minigamer, DimensionChallengeProgress.class);

		progress.getDimensions().add(event.getPlayer().getWorld().getEnvironment());
	}

	static {
		Tasks.repeat(Time.SECOND.x(10), Time.SECOND.x(15), () -> {
			for (Minigamer minigamer : getActiveBingoMinigamers()) {
				final Match match = minigamer.getMatch();
				final BingoMatchData matchData = match.getMatchData();
				for (Challenge challenge : matchData.getAllChallenges(StructureChallenge.class)) {
					final StructureChallenge structureChallenge = (StructureChallenge) challenge.getChallenge();
					final StructureType structureType = structureChallenge.getStructureType();
					final Location location = minigamer.getPlayer().getLocation();
					final Location found = location.getWorld().locateNearestStructure(location, structureType, 2, false);

					if (found == null)
						continue;

					if (found.distance(location) > 32)
						continue;

					final StructureChallengeProgress progress = matchData.getProgress(minigamer, StructureChallengeProgress.class);
					progress.getStructures().add(structureType);
				}
			}
		});
	}

}
