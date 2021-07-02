package me.pugabyte.nexus.features.minigames.mechanics;

import de.tr7zw.nbtapi.NBTItem;
import eden.utils.TimeUtils.Time;
import lombok.Getter;
import me.pugabyte.nexus.features.listeners.Misc.FixedCraftItemEvent;
import me.pugabyte.nexus.features.listeners.Misc.LivingEntityDamageByPlayerEvent;
import me.pugabyte.nexus.features.minigames.managers.PlayerManager;
import me.pugabyte.nexus.features.minigames.models.Match;
import me.pugabyte.nexus.features.minigames.models.Minigamer;
import me.pugabyte.nexus.features.minigames.models.events.matches.minigamers.MinigamerDeathEvent;
import me.pugabyte.nexus.features.minigames.models.matchdata.BingoMatchData;
import me.pugabyte.nexus.features.minigames.models.mechanics.custom.bingo.progress.BreakChallengeProgress;
import me.pugabyte.nexus.features.minigames.models.mechanics.custom.bingo.progress.CraftChallengeProgress;
import me.pugabyte.nexus.features.minigames.models.mechanics.custom.bingo.progress.KillChallengeProgress;
import me.pugabyte.nexus.features.minigames.models.mechanics.custom.bingo.progress.ObtainChallengeProgress;
import me.pugabyte.nexus.features.minigames.models.mechanics.multiplayer.teamless.TeamlessVanillaMechanic;
import me.pugabyte.nexus.utils.TitleUtils;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

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
		return "Fill out your Bingo board from doing unique survival challenges";
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
		TitleUtils.sendTitle(victim.getPlayer(), "&cYou died!");

		final Location bed = victim.getPlayer().getBedSpawnLocation();
		if (bed != null && getWorld().equals(bed.getWorld()))
			victim.teleport(bed);
		else
			victim.teleport(victim.getMatch().<BingoMatchData>getMatchData().getSpawnpoints().get(victim.getUniqueId()));
	}

	@Override
	public @NotNull CompletableFuture<Void> onRandomTeleport(@NotNull Match match, @NotNull Minigamer minigamer, @NotNull Location location) {
		return minigamer.getMatch().<BingoMatchData>getMatchData().spawnpoint(minigamer, location);
	}

	@EventHandler
	public void onBlock(BlockBreakEvent event) {
		final Minigamer minigamer = PlayerManager.get(event.getPlayer());
		if (!minigamer.isPlaying(this)) return;

		final BingoMatchData matchData = minigamer.getMatch().getMatchData();
		final BreakChallengeProgress progress = matchData.getProgress(minigamer, BreakChallengeProgress.class);

		progress.getItems().add(new ItemStack(event.getBlock().getType(), 1));
	}

	@EventHandler
	public void onObtain(EntityPickupItemEvent event) {
		if (!(event.getEntity() instanceof Player player)) return;

		final Minigamer minigamer = PlayerManager.get(player);
		if (!minigamer.isPlaying(this)) return;

		final BingoMatchData matchData = minigamer.getMatch().getMatchData();
		final ObtainChallengeProgress progress = matchData.getProgress(minigamer, ObtainChallengeProgress.class);

		final ItemStack itemStack = event.getItem().getItemStack();
		final NBTItem nbtItem = new NBTItem(itemStack);
		if (nbtItem.hasKey(NBT_KEY)) return;

		nbtItem.setBoolean(NBT_KEY, true);
		progress.getItems().add(nbtItem.getItem());
	}

	@EventHandler
	public void onCraft(FixedCraftItemEvent event) {
		final Minigamer minigamer = PlayerManager.get(event.getWhoClicked());
		if (!minigamer.isPlaying(this)) return;

		final BingoMatchData matchData = minigamer.getMatch().getMatchData();
		final CraftChallengeProgress progress = matchData.getProgress(minigamer, CraftChallengeProgress.class);

		final ItemStack result = event.getResultItemStack();
		progress.getItems().add(result);
	}

	@EventHandler
	public void onKill(LivingEntityDamageByPlayerEvent event) {
		final Minigamer minigamer = PlayerManager.get(event.getAttacker());
		if (!minigamer.isPlaying(this)) return;

		final LivingEntity entity = (LivingEntity) event.getOriginalEvent().getEntity();
		if (entity.getHealth() - event.getOriginalEvent().getFinalDamage() > 0) return;

		final BingoMatchData matchData = minigamer.getMatch().getMatchData();
		final KillChallengeProgress progress = matchData.getProgress(minigamer, KillChallengeProgress.class);

		progress.getKills().add(event.getEntity().getType());
	}

}
