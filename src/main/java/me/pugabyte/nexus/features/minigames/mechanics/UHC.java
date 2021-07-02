package me.pugabyte.nexus.features.minigames.mechanics;

import com.destroystokyo.paper.event.player.PlayerPickupExperienceEvent;
import eden.utils.TimeUtils;
import eden.utils.TimeUtils.Timespan;
import lombok.Getter;
import me.pugabyte.nexus.features.minigames.models.Match;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchStartEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.minigamers.MinigamerDeathEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.minigamers.sabotage.MinigamerDisplayTimerEvent;
import me.pugabyte.nexus.features.minigames.models.matchdata.UHCMatchData;
import me.pugabyte.nexus.features.minigames.models.mechanics.multiplayer.teamless.TeamlessVanillaMechanic;
import me.pugabyte.nexus.utils.ActionBarUtils;
import me.pugabyte.nexus.utils.AdventureUtils;
import me.pugabyte.nexus.utils.JsonBuilder;
import me.pugabyte.nexus.utils.MaterialTag;
import me.pugabyte.nexus.utils.MaterialUtils;
import me.pugabyte.nexus.utils.RandomUtils;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.title.Title;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.LocalDateTime;

@Getter
public class UHC extends TeamlessVanillaMechanic {
	@Override
	public @NotNull ItemStack getMenuItem() {
		return new ItemStack(Material.GOLDEN_APPLE);
	}

	@Override
	public @NotNull String getName() {
		return "Ultra Hardcore";
	}

	@Override
	public @NotNull String getDescription() {
		return "Be the last person alive as you fight other players to death and escape the world border, all without regenerating health";
	}

	private final int worldDiameter = 4001;
	private final String worldName = "uhc";
	private static final long shrinksAtMinutes = 40;
	private static final int shrinksAt = TimeUtils.Time.MINUTE.x(shrinksAtMinutes);
	private static final Duration shrinksAtDuration = TimeUtils.Time.MINUTE.duration(shrinksAtMinutes);
	private static final int warnMinutes = 3;
	private static final int warnAt = shrinksAt - TimeUtils.Time.MINUTE.x(warnMinutes);
	private static final int shrinksAtSecondsLeft = (int) (shrinksAt/10f);
	public static final int finalDiameter = 49;
	private static final String diameterString = finalDiameter + "x" + finalDiameter;

	private final void announce(Audience to, ComponentLike message) {
		Component msg = message.asComponent();
		Title title = Title.title(Component.empty(), msg, AdventureUtils.BASIC_TIMES);
		to.sendMessage(JsonBuilder.fromPrefix("UHC", msg));
		to.showTitle(title);
	}

	@Override
	public void onStart(@NotNull MatchStartEvent event) {
		super.onStart(event);
		final Match match = event.getMatch();
		match.<UHCMatchData>getMatchData().setStartTime(LocalDateTime.now());
		match.getTasks().wait(warnAt, () -> announce(match, new JsonBuilder("&cThe border will begin shrinking in &e"+warnMinutes+" minutes")));
		match.getTasks().wait(shrinksAt, () -> {
			getWorld().getWorldBorder().setSize(finalDiameter, Duration.ofMinutes(17).toSeconds());
			getWorld().setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
			announce(match, new JsonBuilder("&cThe border is now shrinking to &e"+diameterString));
		});
	}

	@Override
	public void onDisplayTimer(MinigamerDisplayTimerEvent event) {
		super.onDisplayTimer(event);
		Timespan timespan = Timespan.of(LocalDateTime.now(), event.getMatch().<UHCMatchData>getMatchData().getStartTime().plus(shrinksAtDuration));
		String timespanText = timespan.format();
		String wbText = timespanText.startsWith("-")
			? "&cWorld Border is shrinking to &6"+diameterString // TODO: should display a different message after border finishes shrinking
			: "World Border shrinks in &e" + Timespan.of(LocalDateTime.now(), event.getMatch().<UHCMatchData>getMatchData().getStartTime().plus(shrinksAtDuration)).format();
		event.setContents(new JsonBuilder(event.getContents())
			.next(" | Y: &e" + (int) Math.floor(event.getMinigamer().getPlayer().getLocation().getY()))
			.next(" | ").next(wbText));
	}

	@Override
	public void onDeath(@NotNull MinigamerDeathEvent event) {
		if (event.getAttacker() != null)
			event.getAttacker().scored();
		event.getMinigamer().getMatch().<UHCMatchData>getMatchData().died(event.getMinigamer());
		dropItems(event.getMinigamer());
		super.onDeath(event);
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onItemDrop(BlockDropItemEvent event) {
		if (!event.getPlayer().getWorld().getName().equals(worldName)) return;
		for (Item item : event.getItems()) {
			ItemStack stack = item.getItemStack();
			Material mat = stack.getType();

			// apples from all trees - part 1
			if (mat == Material.APPLE) return;

			// ore auto-smelter
			Material ingot = MaterialUtils.oreToIngot(stack.getType());
			if (ingot == null) continue;
			stack.setType(ingot);
			item.setItemStack(stack);
			return;
		}
		// apples from all trees - part 2
		if (MaterialTag.LEAVES.isTagged(event.getBlockState().getType()) && RandomUtils.randomInt(1, 200) == 1)
			event.getItems().add(event.getPlayer().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(Material.APPLE)));
	}

	// TODO: fast leaf decay + apples from leaf decay

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onRegainHealth(EntityRegainHealthEvent event) {
		if (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED
		&& event.getEntity() instanceof HumanEntity entity
		&& entity.getWorld().getName().equals(worldName))
			event.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onExperience(PlayerPickupExperienceEvent event) {
		if (event.getPlayer().getWorld().getName().equals(worldName))
			event.getExperienceOrb().setExperience(event.getExperienceOrb().getExperience() * 2);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlaceBlock(BlockPlaceEvent event) {
		// TODO: replace this with damaging the player if they go too high (after 5-10 seconds)
		if (event.getPlayer().getWorld().getName().equals(worldName) && event.getBlock().getY() > 150) {
			event.setCancelled(true);
			ActionBarUtils.sendActionBar(event, "&cYou have reached the height limit!");
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onEntityKill(EntityDeathEvent event) {
		if (!event.getEntity().getWorld().getName().equals(worldName)) return;
		for (ItemStack item : event.getDrops()) {
			Material cooked = MaterialUtils.rawToCooked(item.getType());
			if (cooked == null) continue;
			item.setType(cooked);
			return;
		}
	}

}
