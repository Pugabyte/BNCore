package me.pugabyte.nexus.features.minigames.mechanics;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import eden.utils.TimeUtils.Time;
import me.pugabyte.nexus.features.chat.Censor;
import me.pugabyte.nexus.features.chat.Chat.StaticChannel;
import me.pugabyte.nexus.features.chat.events.MinecraftChatEvent;
import me.pugabyte.nexus.features.chat.events.PublicChatEvent;
import me.pugabyte.nexus.features.minigames.managers.PlayerManager;
import me.pugabyte.nexus.features.minigames.models.Match;
import me.pugabyte.nexus.features.minigames.models.Minigamer;
import me.pugabyte.nexus.features.minigames.models.arenas.PixelDropArena;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchEndEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchJoinEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchStartEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.MinigamerQuitEvent;
import me.pugabyte.nexus.features.minigames.models.matchdata.PixelDropMatchData;
import me.pugabyte.nexus.features.minigames.models.mechanics.multiplayer.teamless.TeamlessMechanic;
import me.pugabyte.nexus.models.chat.Chatter;
import me.pugabyte.nexus.models.chat.ChatterService;
import me.pugabyte.nexus.utils.ActionBarUtils;
import me.pugabyte.nexus.utils.LocationUtils;
import me.pugabyte.nexus.utils.RandomUtils;
import me.pugabyte.nexus.utils.StringUtils;
import me.pugabyte.nexus.utils.Tasks;
import me.pugabyte.nexus.utils.Tasks.Countdown;
import me.pugabyte.nexus.utils.Tasks.Countdown.CountdownBuilder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

// TODO:
//  - Scoreboards
//	- block pasted needs to copy blockdata (directional / slab side)
//	- players can guess the previous rounds word after the round has ended

public class PixelDrop extends TeamlessMechanic {
	private static final String PREFIX = StringUtils.getPrefix("PixelDrop");
	private static final int MAX_ROUNDS = 10;
	private static final int TIME_BETWEEN_ROUNDS = Time.SECOND.x(8);
	private static final int ROUND_COUNTDOWN = Time.SECOND.x(45);

	@Override
	public @NotNull String getName() {
		return "Pixel Drop";
	}

	@Override
	public @NotNull String getDescription() {
		return "Try to guess the picture being drawn by the falling pixels by posting in chat";
	}

	@Override
	public @NotNull ItemStack getMenuItem() {
		return new ItemStack(Material.LIGHT_BLUE_CONCRETE);
	}

	@Override
	public void onJoin(@NotNull MatchJoinEvent event) {
		super.onJoin(event);
		Match match = event.getMatch();
		PixelDropMatchData matchData = match.getMatchData();
		if (!matchData.isAnimateLobby()) {
			matchData.setAnimateLobby(true);
			matchData.startLobbyAnimation(match);
		}
	}

	@Override
	public void onQuit(@NotNull MinigamerQuitEvent event) {
		super.onQuit(event);
		Match match = event.getMatch();
		PixelDropMatchData matchData = match.getMatchData();
		if (matchData.isAnimateLobby() && match.getMinigamers().size() == 0)
			matchData.setAnimateLobby(false);
	}

	@Override
	public void onStart(@NotNull MatchStartEvent event) {
		super.onStart(event);
		Match match = event.getMatch();
		PixelDropMatchData matchData = match.getMatchData();
		match.getTasks().cancel(matchData.getNextFrameTaskId());
		match.getTasks().cancel(matchData.getAnimateLobbyId());

		matchData.setupGame(match);
		endOfRound(match);
	}

	@Override
	public void kill(@NotNull Minigamer victim, @Nullable Minigamer attacker) {
		victim.spawn();
	}

	@Override
	public void onEnd(@NotNull MatchEndEvent event) {
		Match match = event.getMatch();
		PixelDropMatchData matchData = match.getMatchData();
		matchData.clearFloor(match);
		super.onEnd(event);
	}

	public void endTheRound(Match match) {
		cancelCountdown(match);
		stopDesignTask(match);
		match.getTasks().wait(2 * 20, () -> endOfRound(match));
	}

	public void endOfRound(Match match) {
		PixelDropMatchData matchData = match.getMatchData();
		List<Minigamer> minigamers = match.getMinigamers();
		matchData.endRound();

		if (matchData.getCurrentRound() != 0) {
			stopDesignTask(match);
			matchData.stopWordTask(match);

			dropRemainingBlocks(match);
			match.broadcastNoPrefix(PREFIX + "&c&lRound Over! &3The word was: &e" + matchData.getRoundWord());

			minigamers.stream().map(Minigamer::getPlayer).forEach(player ->
					player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 10F, 0.7F));
		}

		matchData.resetRound();

		if (matchData.getCurrentRound() >= MAX_ROUNDS) {
			match.getTasks().wait(3 * 20, () -> {
				minigamers.stream().map(Minigamer::getPlayer).forEach(player -> {
					player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 10F, 1F);
				});
				match.broadcastNoPrefix(PREFIX + "&c&lGame Over!");
				match.getTasks().wait(3 * 20, match::end);
			});
		} else {
			// Start countdown to new round
			match.getTasks().wait(TIME_BETWEEN_ROUNDS / 2, () -> {
				matchData.clearFloor(match);
				match.getTasks().countdown(Countdown.builder()
						.duration(TIME_BETWEEN_ROUNDS)
						.onSecond(i -> minigamers.stream().map(Minigamer::getPlayer).forEach(player -> {
							if (match.isEnded())
								return;

							matchData.setTimeLeft(i);
							match.getScoreboard().update();

							ActionBarUtils.sendActionBar(player, "&cNext round starts in...&c&l " + i + " second" + (i != 1 ? "s" : ""));
							if (i <= 3)
								player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 10F, 0.5F);
						}))
						.onComplete(() -> newRound(match)));
			});
		}
	}

	public void newRound(Match match) {
		if (match.isEnded()) return; // just in case
		PixelDropMatchData matchData = match.getMatchData();
		matchData.setupRound(match);
		startDesignTask(match);
	}

	public void startDesignTask(Match match) {
		PixelDropMatchData matchData = match.getMatchData();
		PixelDropArena arena = match.getArena();
		if (matchData.getDesignTaskId() > 0) return;

		// Get Random Design
		Region designsRegion = arena.getDesignRegion();

		int designCount = matchData.getDesignCount();
		int design = RandomUtils.randomInt(1, designCount);
		for (int i = 0; i < designCount; i++) {
			design = RandomUtils.randomInt(1, designCount);
			if (matchData.getDesign() != design)
				break;
		}
		matchData.setDesign(design);
		matchData.startWordTask(match);

		// Get min point from current chosen design
		BlockVector3 designMin = designsRegion.getMinimumPoint().subtract(0, 1, 0).add(0, design, 0);

		// Builds the map
		for (int x = 0; x < 15; x++) {
			for (int z = 0; z < 15; z++) {
				Block block = match.getWGUtils().toLocation(designMin.add(x, 0, z)).getBlock();
				if (block.getType().equals(Material.BARRIER)) continue;
				String key = x + "_" + z;
				matchData.getDesignMap().put(key, block);
				matchData.getDesignKeys().add(key);
			}
		}

		// Random Paste
		int nextDesignTaskId = match.getTasks().repeat(0, 8, () -> {
			if (matchData.getDesignKeys().size() == 0) {
				stopDesignTask(match);
				return;
			}

			String key = RandomUtils.randomElement(matchData.getDesignKeys());
			matchData.getDesignKeys().remove(key);

			String[] xz = key.split("_");
			Block block = matchData.getDesignMap().get(key);
			drop(match, block, Integer.parseInt(xz[0]), Integer.parseInt(xz[1]));
		});
		matchData.setDesignTaskId(nextDesignTaskId);
	}

	public void stopDesignTask(Match match) {
		PixelDropMatchData matchData = match.getMatchData();
		match.getTasks().cancel(matchData.getDesignTaskId());
		matchData.setDesignTaskId(-1);
	}

	public void dropRemainingBlocks(Match match) {
		PixelDropMatchData matchData = match.getMatchData();

		for (String key : matchData.getDesignKeys()) {
			Block block = matchData.getDesignMap().get(key);

			String[] xz = key.split("_");
			drop(match, block, Integer.parseInt(xz[0]), Integer.parseInt(xz[1]));
		}
		matchData.getDesignKeys().clear();
	}

	public void drop(Match match, Block block, int x, int z) {
		PixelDropArena arena = match.getArena();
		Location pasteMin = match.getWGUtils().toLocation(arena.getDropRegion().getMinimumPoint());
		Location blockCenter = LocationUtils.getBlockCenter(new Location(block.getWorld(), x, pasteMin.getY(), z));
		Location loc = pasteMin.add(blockCenter.getX(), 0, blockCenter.getZ());

		FallingBlock fallingBlock = loc.getWorld().spawnFallingBlock(loc, block.getType().createBlockData());
		fallingBlock.setDropItem(false);
		fallingBlock.setInvulnerable(true);
		fallingBlock.setVelocity(new org.bukkit.util.Vector(0, -0.5, 0));
	}

	@EventHandler
	public void onChat(MinecraftChatEvent event) {
		Player player = event.getChatter().getOnlinePlayer();
		Minigamer minigamer = PlayerManager.get(player);
		if (!minigamer.isPlaying(this)) return;
		event.setCancelled(true);
	}

	@EventHandler
	public void onPlayerChat(AsyncPlayerChatEvent event) {
		Player player = event.getPlayer();
		Minigamer minigamer = PlayerManager.get(player);
		if (!minigamer.isPlaying(this)) return;

		// Only block actual chat, not commands
		if (!event.isAsynchronous()) return;

		Match match = minigamer.getMatch();
		PixelDropMatchData matchData = match.getMatchData();

		event.setCancelled(true);

		ChatterService chatService = new ChatterService();
		Set<Chatter> recipients = match.getMinigamers().stream()
				.map(_minigamer -> chatService.get(_minigamer.getPlayer()))
				.collect(toSet());

		PublicChatEvent publicChatEvent = new PublicChatEvent(
				chatService.get(minigamer.getPlayer()),
				StaticChannel.GLOBAL.getChannel(),
				event.getMessage(),
				event.getMessage(),
				true,
				false,
				recipients);

		Censor.process(publicChatEvent);

		if (publicChatEvent.isCancelled())
			return;

		Tasks.sync(() -> {
			String message = publicChatEvent.getMessage();
			if (matchData.getGuessed().contains(minigamer) && !matchData.isRoundOver()) {
				matchData.getGuessed().forEach(recipient -> sendChat(recipient, minigamer, "&7" + message));
				return;
			}

			if (!message.equalsIgnoreCase(matchData.getRoundWord()) || matchData.isRoundOver()) {
				match.getMinigamers().forEach(recipient -> sendChat(recipient, minigamer, "&f" + message));
				return;
			}

			player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 10F, 0.5F);
			match.broadcastNoPrefix(PREFIX + "&e" + minigamer.getNickname() + " &3guessed the word!");
			matchData.getGuessed().add(minigamer);
			minigamer.scored(Math.max(1, 1 + (4 - matchData.getGuessed().size())));
			match.getScoreboard().update();

			if (matchData.getGuessed().size() == 1)
				startRoundCountdown(match);

			if (match.getMinigamers().size() == matchData.getGuessed().size()) {
				endTheRound(match);
			}
		});
	}

	private String getChatFormat(Minigamer sender, String message) {
		return StringUtils.getPrefix("PixelDrop") + sender.getNickname() + " > " + message;
	}

	private void sendChat(Minigamer recipient, Minigamer sender, String message) {
		recipient.tell(getChatFormat(sender, message), false);
	}

	public void startRoundCountdown(Match match) {
		PixelDropMatchData matchData = match.getMatchData();

		List<Minigamer> minigamers = match.getMinigamers();
		minigamers.stream().map(Minigamer::getPlayer).forEach(player ->
				player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 10F, 0.5F));

		CountdownBuilder countdown = Countdown.builder()
				.duration(ROUND_COUNTDOWN)
				.onSecond(i -> minigamers.forEach(minigamer -> {

					if (match.isEnded()) return;
					matchData.setTimeLeft(i);
					match.getScoreboard().update();

					if (Arrays.asList(1, 2, 3, 10).contains(i))
						minigamer.tell(PREFIX + "&3Round ends in " + i, false);

					if (i <= 3) {
						Player player = minigamer.getPlayer();
						player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 10F, 0.5F);
					}
				}))
				.onComplete(() -> endOfRound(match));

		matchData.setRoundCountdownId(match.getTasks().countdown(countdown));
	}

	public void cancelCountdown(Match match) {
		PixelDropMatchData matchData = match.getMatchData();
		match.getTasks().cancel(matchData.getRoundCountdownId());
		matchData.setTimeLeft(0);
		match.getScoreboard().update();
	}
}
