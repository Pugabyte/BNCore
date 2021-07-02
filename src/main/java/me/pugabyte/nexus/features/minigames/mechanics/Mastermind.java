package me.pugabyte.nexus.features.minigames.mechanics;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.pugabyte.nexus.features.minigames.managers.PlayerManager;
import me.pugabyte.nexus.features.minigames.models.Match;
import me.pugabyte.nexus.features.minigames.models.Minigamer;
import me.pugabyte.nexus.features.minigames.models.annotations.Regenerating;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchJoinEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchTimerTickEvent;
import me.pugabyte.nexus.features.minigames.models.exceptions.MinigameException;
import me.pugabyte.nexus.features.minigames.models.matchdata.IMastermindMatchData;
import me.pugabyte.nexus.features.minigames.models.matchdata.MastermindMatchData;
import me.pugabyte.nexus.features.minigames.models.mechanics.singleplayer.SingleplayerMechanic;
import me.pugabyte.nexus.utils.ItemUtils;
import me.pugabyte.nexus.utils.MaterialTag;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;

// TODO Lobby

@Regenerating({"board", "guess"})
public final class Mastermind extends SingleplayerMechanic {

	@Override
	public @NotNull String getName() {
		return "Mastermind";
	}

	@Override
	public @NotNull String getDescription() {
		return "Try to crack the cryptic 4-color code with limited information";
	}

	@Override
	public @NotNull ItemStack getMenuItem() {
		return new ItemStack(Material.TRIPWIRE_HOOK);
	}

	@Override
	public @NotNull GameMode getGameMode() {
		return GameMode.SURVIVAL;
	}

	@Override
	public void onJoin(@NotNull MatchJoinEvent event) {
		super.onJoin(event);

		Match match = event.getMatch();
		MastermindMatchData matchData = match.getMatchData();
		matchData.giveLoadout(event.getMinigamer());
		matchData.resetResultsSign();
	}

	public void removeBlock(Minigamer minigamer, Block block) {
		if (!canBuild(minigamer, block)) return;
		ItemStack item = new ItemStack(block.getType());
		block.setType(Material.AIR);
		Player player = minigamer.getPlayer();
		player.getInventory().addItem(item);
		player.playSound(player.getLocation(), Sound.BLOCK_STONE_BREAK, 10F, 1F);
	}

	public boolean canBuild(Minigamer minigamer, Block block) {
		IMastermindMatchData matchData = minigamer.getMatch().getMatchData();
		return isInRegion(minigamer.getMatch(), block, "guess") && matchData.canGuess(minigamer);
	}

	@EventHandler
	public void onTick(MatchTimerTickEvent event) {
		if (!event.getMatch().isMechanic(this)) return;

		Match match = event.getMatch();
		MastermindMatchData matchData = match.getMatchData();

		Minigamer minigamer = match.getAliveMinigamers().iterator().next();
		if (matchData.getGuess(minigamer) > 1 && matchData.getGuess(minigamer) <= 10)
			event.getMatch().getMinigamers().forEach(Minigamer::scored);
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent event) {
		Block block = event.getClickedBlock();
		if (EquipmentSlot.HAND != event.getHand() || block == null) return;

		Minigamer minigamer = PlayerManager.get(event.getPlayer());
		if (!minigamer.isPlaying(this)) return;

		if (Action.LEFT_CLICK_BLOCK.equals(event.getAction())) {
			event.setCancelled(true);
			removeBlock(minigamer, block);
			return;
		}

		if (Action.RIGHT_CLICK_BLOCK.equals(event.getAction())) {
			Match match = minigamer.getMatch();
			MastermindMatchData matchData = match.getMatchData();
			if (isInRegion(match, block, "button")) {
				if (!MaterialTag.BUTTONS.isTagged(block.getType()))
					return;

				// TODO Cleanup
				try {
					matchData.guess(minigamer);
				} catch (MinigameException ex) {
					minigamer.tell("&cError: " + ex.getMessage());
				} catch (Exception ex) {
					minigamer.tell("&cUnknown error occurred");
					ex.printStackTrace();
				}
				return;
			}

			if (MaterialTag.SIGNS.isTagged(block.getType())) {
				int guess = matchData.getGuess(minigamer);

				if (isInRegion(match, block, "colorblind")) {
					if (guess != 1) {
						minigamer.tell("You cannot change colorblind mode in the middle of the game");
						return;
					}
					matchData.toggleColorblind(minigamer);
					matchData.giveLoadout(minigamer);
					matchData.createAnswer();
					match.getArena().regenerate();
					return;
				}
				if (isInRegion(match, block, "repeats_off")) {
					if (guess != 1) {
						minigamer.tell("You cannot change the difficulty mode in the middle of the game");
						return;
					}
					matchData.setRepeats(false);
					matchData.createAnswer();
					match.getArena().regenerate();
					minigamer.tell("Difficulty mode updated");
					return;
				}
				if (isInRegion(match, block, "repeats_on")) {
					if (guess != 1) {
						minigamer.tell("You cannot change the difficulty mode in the middle of the game");
						return;
					}
					matchData.setRepeats(true);
					matchData.createAnswer();
					match.getArena().regenerate();
					minigamer.tell("Difficulty mode updated");
					return;
				}
			}

			Block placed = event.getClickedBlock().getRelative(event.getBlockFace());
			if (!ItemUtils.isNullOrAir(event.getItem()) && !canBuild(minigamer, placed))
				event.setCancelled(true);
		}
	}

	@Getter
	@AllArgsConstructor
	public enum MastermindColor {
		RED(Material.RED_CONCRETE, Material.TNT),
		ORANGE(Material.ORANGE_CONCRETE, Material.PUMPKIN),
		YELLOW(Material.YELLOW_CONCRETE, Material.SPONGE),
		LIME(Material.LIME_CONCRETE, Material.MELON),
		LIGHT_BLUE(Material.LIGHT_BLUE_CONCRETE, Material.DIAMOND_BLOCK),
		BLUE(Material.BLUE_CONCRETE, Material.DIRT), // TODO Can we change?
		PURPLE(Material.PURPLE_CONCRETE, Material.PURPLE_WOOL),
		PINK(Material.PINK_CONCRETE, Material.BRAIN_CORAL_BLOCK);

		private final Material normal;
		private final Material colorblind;

		public static MastermindColor of(Material material) {
			for (MastermindColor color : values())
				if (color.getNormal() == material || color.getColorblind() == material)
					return color;

			return null;
		}

		public static Set<Material> getKit() {
			return getMaterials(MastermindColor::getNormal);
		}

		public static Set<Material> getColorblindKit() {
			return getMaterials(MastermindColor::getColorblind);
		}

		public static Set<Material> getMaterials(Function<MastermindColor, Material> function) {
			return new LinkedHashSet<>() {{
				for (MastermindColor color : values())
					add(function.apply(color));
			}};
		}
	}

}
