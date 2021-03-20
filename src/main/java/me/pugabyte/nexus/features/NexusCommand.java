package me.pugabyte.nexus.features;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.regions.Region;
import fr.minuskube.inv.SmartInvsPlugin;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.chat.Koda;
import me.pugabyte.nexus.features.crates.models.CrateType;
import me.pugabyte.nexus.features.discord.Discord;
import me.pugabyte.nexus.features.listeners.ResourceWorld;
import me.pugabyte.nexus.features.minigames.managers.ArenaManager;
import me.pugabyte.nexus.features.minigames.managers.MatchManager;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchEndEvent;
import me.pugabyte.nexus.features.minigames.models.mechanics.MechanicType;
import me.pugabyte.nexus.features.recipes.CustomRecipes;
import me.pugabyte.nexus.features.warps.Warps.LegacySurvivalWarp;
import me.pugabyte.nexus.features.warps.Warps.SurvivalWarp;
import me.pugabyte.nexus.features.wither.WitherChallenge;
import me.pugabyte.nexus.framework.commands.Commands;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Arg;
import me.pugabyte.nexus.framework.commands.models.annotations.Async;
import me.pugabyte.nexus.framework.commands.models.annotations.Confirm;
import me.pugabyte.nexus.framework.commands.models.annotations.ConverterFor;
import me.pugabyte.nexus.framework.commands.models.annotations.Cooldown;
import me.pugabyte.nexus.framework.commands.models.annotations.Cooldown.Part;
import me.pugabyte.nexus.framework.commands.models.annotations.Description;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.annotations.TabCompleterFor;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.framework.exceptions.postconfigured.CommandCooldownException;
import me.pugabyte.nexus.framework.exceptions.postconfigured.InvalidInputException;
import me.pugabyte.nexus.framework.features.Features;
import me.pugabyte.nexus.models.MongoService;
import me.pugabyte.nexus.models.cooldown.CooldownService;
import me.pugabyte.nexus.models.hours.HoursService;
import me.pugabyte.nexus.models.nerd.Nerd;
import me.pugabyte.nexus.models.nerd.Nerd.StaffMember;
import me.pugabyte.nexus.models.nerd.NerdService;
import me.pugabyte.nexus.models.setting.Setting;
import me.pugabyte.nexus.models.setting.SettingService;
import me.pugabyte.nexus.models.task.Task;
import me.pugabyte.nexus.models.task.TaskService;
import me.pugabyte.nexus.utils.ActionBarUtils;
import me.pugabyte.nexus.utils.BlockUtils;
import me.pugabyte.nexus.utils.JsonBuilder;
import me.pugabyte.nexus.utils.PacketUtils;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.SoundUtils;
import me.pugabyte.nexus.utils.SoundUtils.Jingle;
import me.pugabyte.nexus.utils.StringUtils;
import me.pugabyte.nexus.utils.StringUtils.ProgressBarStyle;
import me.pugabyte.nexus.utils.StringUtils.Timespan;
import me.pugabyte.nexus.utils.StringUtils.TimespanFormatType;
import me.pugabyte.nexus.utils.Tasks;
import me.pugabyte.nexus.utils.Tasks.ExpBarCountdown;
import me.pugabyte.nexus.utils.Time;
import me.pugabyte.nexus.utils.Utils;
import me.pugabyte.nexus.utils.WorldEditUtils;
import net.citizensnpcs.api.CitizensAPI;
import net.dv8tion.jda.api.entities.Member;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.data.type.RedstoneRail;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import org.inventivetalent.glow.GlowAPI;
import org.reflections.Reflections;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

import static me.pugabyte.nexus.utils.BlockUtils.getBlocksInRadius;
import static me.pugabyte.nexus.utils.BlockUtils.getDirection;
import static me.pugabyte.nexus.utils.ItemUtils.isNullOrAir;
import static me.pugabyte.nexus.utils.StringUtils.colorize;
import static me.pugabyte.nexus.utils.StringUtils.paste;
import static me.pugabyte.nexus.utils.StringUtils.shortDateFormat;
import static me.pugabyte.nexus.utils.StringUtils.timespanDiff;

@NoArgsConstructor
@Permission("group.seniorstaff")
public class NexusCommand extends CustomCommand implements Listener {
	private WorldEditUtils worldEditUtils;

	public NexusCommand(CommandEvent event) {
		super(event);
		if (isPlayer())
			worldEditUtils = new WorldEditUtils(player());
	}

	@Override
	public void _shutdown() {
		shutdownBossBars();
	}
	@Path("cancelReload")
	@Cooldown(@Part(value = Time.SECOND, x = 15))
	void cancelReload() {
		reloader = null;
		send(PREFIX + "Reload unqueued");
	}

	@Path("reload")
	void reload() {
		try {
			ReloadCondition.tryReload();
		} catch (Exception ex) {
			reloader = uuid();
			error(json(ex.getMessage()).next(", reload queued ").group().next("&e⟳").hover("&eClick to retry manually").command("/nexus reload"));
		}

		for (Player player : Bukkit.getOnlinePlayers())
			if (player.equals(PlayerUtils.wakka()) || player.equals(PlayerUtils.blast()))
				SoundUtils.playSound(player, Sound.ENTITY_EVOKER_PREPARE_WOLOLO);

		CooldownService cooldownService = new CooldownService();
		if (!cooldownService.check(Nexus.getUUID0(), "reload", Time.SECOND.x(15)))
			throw new CommandCooldownException(Nexus.getUUID0(), "reload");

		runCommand("plugman reload Nexus");
	}

	private static UUID reloader;

	@Getter
	@AllArgsConstructor
	public enum ReloadCondition {
		FILE_NOT_FOUND(() -> {
			File file = Paths.get("plugins/Nexus.jar").toFile();
			if (!file.exists())
				throw new InvalidInputException("Nexus.jar doesn't exist");
		}),
		FILE_NOT_COMPLETE(() -> {
			File file = Paths.get("plugins/Nexus.jar").toFile();
			try {
				new ZipFile(file).entries();
			} catch (IOException ex) {
				throw new InvalidInputException("Nexus.jar is not complete");
			}
		}),
		MINIGAMES(() -> {
			long matchCount = MatchManager.getAll().stream().filter(match -> match.isStarted() && !match.isEnded()).count();
			if (matchCount > 0)
				throw new InvalidInputException("There are " + matchCount + " active matches");
		}),
		SMARTINVS(() -> {
			long invCount = Bukkit.getOnlinePlayers().stream().filter(player -> SmartInvsPlugin.manager().getInventory(player).isPresent()).count();
			if (invCount > 0)
				throw new InvalidInputException("There are " + invCount + " SmartInvs menus open");
		}),
		TEMP_LISTENERS(() -> {
			if (Nexus.getTempListenerCount() > 0)
				throw new InvalidInputException("There are " + Nexus.getTempListenerCount() + " temporary listeners registered");
		}),
		SIGN_MENUS(() -> {
			if (!Nexus.getSignMenuFactory().getInputReceivers().isEmpty())
				throw new InvalidInputException("There are " + Nexus.getSignMenuFactory().getInputReceivers().size() + " sign menus open");
		}),
		CRATES(() -> {
			for (CrateType crateType : Arrays.stream(CrateType.values()).filter(crateType -> crateType != CrateType.ALL).collect(Collectors.toList()))
				if (crateType.getCrateClass().isInUse())
					throw new InvalidInputException("Someone is opening a crate");
		}),
		WITHER(() -> {
			if (WitherChallenge.currentFight != null)
				throw new InvalidInputException("The wither is currently being fought");
		});

		public static boolean canReload() {
			try {
				tryReload();
			} catch (Exception ex) {
				return false;
			}

			return true;
		}

		public static void tryReload() {
			for (ReloadCondition condition : ReloadCondition.values())
				condition.getRunnable().run();
		}

		private final Runnable runnable;
	}

	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event) {
		Tasks.wait(5, NexusCommand::tryReload);
	}

	@EventHandler
	public void onMatchEnd(MatchEndEvent event) {
		Tasks.wait(5, NexusCommand::tryReload);
	}

	static {
		Tasks.repeat(Time.SECOND.x(5), Time.SECOND.x(5), NexusCommand::tryReload);
	}

	private static void tryReload() {
		if (reloader == null)
			return;

		if (!ReloadCondition.canReload())
			return;

		OfflinePlayer player = Bukkit.getOfflinePlayer(reloader);
		if (!player.isOnline())
			return;

		PlayerUtils.runCommand(player.getPlayer(), "nexus reload");
	}

	@Path("debug")
	void debug() {
		Nexus.setDebug(!Nexus.isDebug());
		send(PREFIX + "Debugging " + (Nexus.isDebug() ? "&aenabled" : "&cdisabled"));
	}

	private static final LocalDateTime lastReload = LocalDateTime.now();

	@Path("lastReload")
	void lastReload() {
		send(PREFIX + "Last reloaded &e" + timespanDiff(lastReload) + " ago");
	}

	@Path("gc")
	void gc() {
		send("Collecting garbage...");
		System.gc();
		send("Garbage collected");
	}

	@Path("smartInvs")
	void smartInvs() {
		Map<String, String> playerInventoryMap = new HashMap<>();
		Bukkit.getOnlinePlayers().stream()
				.filter(player -> SmartInvsPlugin.manager().getInventory(player).isPresent())
				.forEach(player -> playerInventoryMap.put(player.getName(),
						SmartInvsPlugin.manager().getInventory(player).get().getTitle()));

		if (playerInventoryMap.isEmpty())
			error("No SmartInvs open");

		send(PREFIX + "Open SmartInvs:");
		for (Map.Entry<String, String> entry : playerInventoryMap.entrySet())
			send(" &7- " + entry.getKey() + " - " + entry.getValue());
	}

	@Path("stats")
	void stats() {
		send("Features: " + Features.getFeatures().size());
		send("Commands: " + Commands.getCommands().size());
		send("Listeners: " + Nexus.getListenerCount());
		send("Temp Listeners: " + Nexus.getTempListenerCount());
		send("EventHandlers: " + Nexus.getEventHandlers().size());
		send("Arenas: " + ArenaManager.getAll().size());
		send("Mechanics: " + MechanicType.values().length);
		send("Recipes: " + CustomRecipes.getRecipes().size());
	}

	@Path("stats eventHandlers [page]")
	void eventHandlers(@Arg("1") int page) {
		Map<Class<? extends Event>, Integer> counts = new HashMap<>();
		for (Class<? extends Event> eventHandler : Nexus.getEventHandlers())
			counts.put(eventHandler, counts.getOrDefault(eventHandler, 0) + 1);

		if (counts.isEmpty())
			error("No event handlers found");

		Map<Class<? extends Event>, Integer> sorted = Utils.sortByValueReverse(counts);

		send(PREFIX + "Event Handlers");
		BiFunction<Class<? extends Event>, String, JsonBuilder> formatter = (clazz, index) ->
				json("&3" + index + " &e" + clazz.getSimpleName() + " &7- " + counts.get(clazz));
		paginate(new ArrayList<>(sorted.keySet()), formatter, "/nexus stats eventHandlers", page);
	}

	@Confirm
	@Path("resourceWorld reset <test>")
	void resourceWorldReset(boolean test) {
		ResourceWorld.reset(test);
	}

	@Path("listTest <player...>")
	void listTest(@Arg(type = OfflinePlayer.class) List<OfflinePlayer> players) {
		send(players.stream().map(OfflinePlayer::getName).collect(Collectors.joining(", ")));
	}

	static {
		Tasks.repeatAsync(Time.SECOND, Time.SECOND.x(30), () -> {
			TaskService service = new TaskService();
			service.process("command-test").forEach(task ->
					Tasks.wait(Time.MINUTE.x(2), () -> {
						Map<String, Object> data = task.getJson();
						OfflinePlayer player = PlayerUtils.getPlayer((String) data.get("uuid"));
						if (player.isOnline() && player.getPlayer() != null)
							PlayerUtils.send(player, (String) data.get("message"));
						service.complete(task);
					}));
		});
	}

	@Path("taskTest <message...>")
	void taskTest(String message) {
		new TaskService().save(new Task("command-test", new HashMap<String, Object>() {{
			put("uuid", uuid().toString());
			put("message", message);
		}}, LocalDateTime.now().plusMinutes(1)));
	}

	@Path("boosts")
	void boosts() {
		List<Member> boosters = Discord.getGuild().getBoosters();
		for (Member booster : boosters) {
			send(" - " + booster.getEffectiveName());
		}
	}

	@Path("getEnv")
	void getEnv() {
		send(Nexus.getEnv().name());
	}

	@Path("clearCache <service>")
	void clearCache(MongoService service) {
		service.clearCache();
		send(PREFIX + service.getClass().getSimpleName() + " cached cleared");
	}

	@Async
	@Path("getDataFile [player]")
	void getDataFile(@Arg("self") Nerd nerd) {
		send(json().next(paste(nerd.getDataFile().asNBTString())));
	}

	@Path("koda <message...>")
	void koda(String message) {
		Koda.say(message);
	}

	@Path("getRank <player>")
	void getRank(Nerd player) {
		send(player.getRank().withColor());
	}

	@Path("getPlayer [player]")
	void getPlayer(@Arg("self") OfflinePlayer player) {
		send(player.getName());
	}

	@Description("Generate an sample exp bar countdown")
	@Path("expBarCountdown <duration>")
	void expBarCountdown(@Arg("20") int duration) {
		ExpBarCountdown.builder()
				.duration(duration)
				.player(player())
				.restoreExp(true)
				.start();
	}

	@Confirm
	@Path("breakNaturally")
	void breakNaturally() {
		WorldEditUtils worldEditUtils = new WorldEditUtils(player());
		Region selection = worldEditUtils.getPlayerSelection(player());
		if (selection.getArea() > 50000)
			error("Max selection size is 50000");

		for (Block block : worldEditUtils.getBlocks(selection)) {
			if (block.getType() == Material.AIR)
				continue;

			block.breakNaturally();
		}
	}

	@Path("smartReplace <from> <to>")
	void smartReplace(Material from, Material to) {
		WorldEditUtils worldEditUtils = new WorldEditUtils(player());
		Region selection = worldEditUtils.getPlayerSelection(player());

		for (Block block : worldEditUtils.getBlocks(selection)) {
			if (block.getType() != from)
				continue;

			block.setBlockData(Bukkit.createBlockData(block.getBlockData().getAsString().replace(from.name().toLowerCase(), to.name().toLowerCase())));
		}
	}

	@Path("movingSchematicTest <schematic> <seconds> <velocity>")
	void movingSchematicTest(String schematic, int seconds, double velocity) {
		List<FallingBlock> fallingBlocks = worldEditUtils.paster()
				.file(schematic)
				.at(location().add(-10, 0, 0))
				.buildEntities();

		Tasks.wait(Time.SECOND.x(5), () -> {
			Tasks.Countdown.builder()
					.duration(Time.SECOND.x(seconds))
					.onTick(i -> fallingBlocks.forEach(fallingBlock -> fallingBlock.setVelocity(new Vector(velocity, 0, 0))))
					.start();

			Tasks.wait(Time.SECOND.x(seconds), () -> fallingBlocks.forEach(Entity::remove));
		});
	}

	@Path("schem buildQueue <schematic> <seconds>")
	void schemBuildQueue(String schematic, int seconds) {
		worldEditUtils.paster()
				.file(schematic)
				.at(location().add(-10, 0, 0))
				.duration(Time.SECOND.x(seconds))
				.buildQueue();
	}

	public void shutdownBossBars() {
		bossBars.forEach((player, bossBar) -> {
			bossBar.setVisible(false);
			bossBar.removeAll();
		});
	}

	private static Map<Player, BossBar> bossBars = new HashMap<>();

	@Path("bossBar add <color> <style> <title...>")
	void bossBarAdd(BarColor color, BarStyle barStyle, String title) {
		if (bossBars.containsKey(player()))
			error("You already have a boss bar");

		BossBar bossBar = Bukkit.createBossBar(title, color, barStyle);
		bossBar.addPlayer(player());
		bossBars.put(player(), bossBar);
	}

	@Path("bossBar remove")
	void bossBarRemove() {
		if (!bossBars.containsKey(player()))
			error("You do not have a boss bar");

		BossBar bossBar = bossBars.remove(player());
		bossBar.setVisible(false);
		bossBar.removeAll();
	}

	@Path("setExp <number>")
	void setExp(float exp) {
		player().setExp(exp);
	}

	@Path("setTotalExperience <number>")
	void setTotalExperience(int exp) {
		player().setTotalExperience(exp);
	}

	@Path("setLevel <number>")
	void setLevel(int exp) {
		player().setLevel(exp);
	}

	@Path("giveExpLevels <number>")
	void giveExpLevels(int exp) {
		player().giveExpLevels(exp);
	}

	@Path("updateCommands")
	void updateCommands() {
		player().updateCommands();
	}

	@Path("actionBar <duration> <message...>")
	void actionBar(int duration, String message) {
		ActionBarUtils.sendActionBar(player(), message, duration);
	}

	@Path("progressBar <progres> <goal> [style] [length]")
	void progressBar(int progress, int goal, @Arg("NONE") ProgressBarStyle style, @Arg("25") int length) {
		send(StringUtils.progressBar(progress, goal, style, length));
	}

	@Path("setting <type> [value]")
	void setting(String type, String value) {
		if (!isNullOrEmpty(value))
			new SettingService().save(new Setting(player(), type, value));
		send("Setting: " + new SettingService().get(player(), type));
	}

	@Path("getBlockStandingOn")
	void getBlockStandingOn() {
		Block block = BlockUtils.getBlockStandingOn(player());
		if (BlockUtils.isNullOrAir(block))
			send("Nothing");
		else
			send(block.getType().name());
	}

	@Path("setTabListName <text...>")
	void setTabListName(String text) {
		player().setPlayerListName(colorize(text));
		send("Updated");
	}

	@Path("schem saveReal <name>")
	void schemSaveReal(String name) {
		worldEditUtils.save(name, worldEditUtils.getPlayerSelection(player()));
		send("Saved schematic " + name);
	}

	@Path("schem save <name>")
	void schemSave(String name) {
		WorldEditUtils worldEditUtils = new WorldEditUtils(player());
		GameMode originalGameMode = player().getGameMode();
		Location originalLocation = location().clone();
		Location location = worldEditUtils.toLocation(worldEditUtils.getPlayerSelection(player()).getMinimumPoint());
		player().setGameMode(GameMode.SPECTATOR);
		player().teleport(location);
		runCommand("mcmd /copy ;; wait 10 ;; /schem save " + name + " -f");
		Tasks.wait(20, () -> {
			player().teleport(originalLocation);
			player().setGameMode(originalGameMode);
		});

		send("Saved schematic " + name);
	}

	@Path("schem paste <name>")
	void schemPaste(String name) {
		worldEditUtils.paster().file(name).at(location()).pasteAsync();
		send("Pasted schematic " + name);
	}

	private static final Map<UUID, Clipboard> clipboards = new HashMap<>();

	@Path("clipboard copy")
	void clipboardCopy() {
		clipboards.put(uuid(), worldEditUtils.copy(worldEditUtils.getPlayerSelection(player())));
		send("Copied selection");
	}

	@Path("clipboard paste")
	void clipboardPaste() {
		if (!clipboards.containsKey(uuid()))
			error("You have not copied anything");

		worldEditUtils.paster().clipboard(clipboards.get(uuid())).at(location()).pasteAsync();
		send("Pasted clipboard");
	}

	@Path("allowedRegionsTest")
	void allowedRegionsTest() {
		worldEditUtils.paster().file("allowedRegionsTest").at(location()).regions("allowedRegionsTest").pasteAsync();
		send("Pasted schematic allowedRegionsTest");
	}

	@Path("copyTileEntityClientTest")
	void copyTileEntityClient() {
		PacketUtils.copyTileEntityClient(player(), location().getBlock(), location().add(1, 0, 0));
	}

	@Path("removeTest")
	void removeTest() {
		PlayerInventory inventory = inventory();
		inventory.remove(new ItemStack(Material.DIRT, 2));
		inventory.removeItem(new ItemStack(Material.COBBLESTONE, 4));
		inventory.removeItemAnySlot(new ItemStack(Material.STONE, 6));
	}

	@Path("signgui")
	void signgui() {
		Nexus.getSignMenuFactory()
				.lines("1", "2", "3", "4")
				.prefix(PREFIX)
				.response(lines -> {
					for (String string : lines)
						send(string);
				})
				.open(player());
	}

	@Path("loreizeTest")
	void loreizeTest() {
		String lorem = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut " +
				"labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris " +
				"nisi ut aliquip ex ea commodo consequat.";

		send(json("Test 1").hover(lorem));
		send(json("Test 2").hover(lorem).loreize(false));
	}

	@Description("A command with a 5.75s cooldown")
	@Path("cooldown")
	@Cooldown({
			@Part(value = Time.SECOND, x = 5),
			@Part(value = Time.TICK, x = 15)
	})
	void cooldown() {
		send("Hello!");
	}

	@Path("argPermTest [one] [two] [three] [four] [five]")
	void argPermTest(
			@Arg(tabCompleter = Player.class) String one,
			@Arg(value = "2", tabCompleter = Player.class) String two,
			@Arg(permission = "group.staff", tabCompleter = Player.class) String three,
			@Arg(value = "4", permission = "group.staff", tabCompleter = Player.class) String four,
			@Arg(value = "5", permission = "group.admin", tabCompleter = Player.class) String five
	) {
		send(one + " / " + two + " / " + three + " / " + four + " / " + five);
	}

	private static final Map<UUID, Location> directionTestMap = new HashMap<>();

	@Path("directionTest <blockNumber>")
	void directionTest(int blockNumber) {
		Block targetBlockExact = getTargetBlockRequired();

		if (blockNumber == 1) {
			directionTestMap.put(uuid(), targetBlockExact.getLocation());
			send(PREFIX + "Set second position to calcluate direction");
		} else {
			if (!directionTestMap.containsKey(uuid()))
				error("You must set the first position");

			send(PREFIX + camelCase(getDirection(directionTestMap.remove(uuid()), targetBlockExact.getLocation())));
		}
	}

	@Path("timespanFormatter <seconds> <formatType>")
	void timespanFormatter(int seconds, TimespanFormatType formatType) {
		send(Timespan.of(seconds).formatType(formatType).format());
	}

	@Path("voidNpc")
	void voidNpc() {
		CitizensAPI.getNPCRegistry().forEach(npc -> {
			if (npc.getEntity() != null && npc.getEntity().getLocation().getY() < 0)
				send(npc.getId());
		});
	}

	@Path("jingles <jingle>")
	void jingles(Jingle jingle) {
		jingle.play(player());
	}

	// Doesnt work
	@Path("setPowered [boolean] [doPhysics]")
	void setPowered(@Arg("true") boolean powered, @Arg("false") boolean doPhysics) {
		line();
		Block block = location().getBlock();
		RedstoneRail rail = ((RedstoneRail) block.getBlockData());
		send("Before: " + (rail.isPowered() ? "is" : "not") + " powered");
		rail.setPowered(powered);
		block.setBlockData(rail, doPhysics);
		player().sendBlockChange(block.getLocation(), rail);
		send("After: " + (((RedstoneRail) block.getBlockData()).isPowered() ? "is" : "not") + " powered");
	}

	@Path("getPowered")
	void getPowered() {
		Block block = location().getBlock();
		RedstoneRail rail = ((RedstoneRail) block.getBlockData());
		send((rail.isPowered() ? "is" : "not") + " powered");
	}

	@Path("clientSideBlock <material>")
	void clientSideBlock(Material material) {
		player().sendBlockChange(location().add(0, -1, 0), Bukkit.createBlockData(material));
	}

	private static String motd = null;

	@Path("motd <text...>")
	void motd(String text) {
		motd = colorize(text.replace("\\n", System.lineSeparator()));
		send(PREFIX + "Motd updated");
	}

	@Path("motd reset")
	void motdReset() {
		motd = null;
		send(PREFIX + "Motd Reset");
	}

	@EventHandler
	public void onServerListPing(ServerListPingEvent event) {
		if (motd != null)
			event.setMotd(motd);
	}

	@Path("radiusTest")
	void radiusTest() {
		Location origin = location();
		for (Block block : getBlocksInRadius(origin, 3)) {
			double distance = block.getLocation().distance(origin);
			if (distance < 1)
				block.setType(Material.RED_CONCRETE);
			else if (distance < 2)
				block.setType(Material.ORANGE_CONCRETE);
			else if (distance < 3)
				block.setType(Material.YELLOW_CONCRETE);
		}
	}

	@Path("trimItemNames")
	void trimItemNames() {
		Block block = getTargetBlock();
		if (!(block.getState() instanceof Container))
			return;

		Container state = (Container) block.getState();

		for (ItemStack content : state.getInventory().getContents()) {
			if (isNullOrAir(content))
				continue;

			ItemMeta itemMeta = content.getItemMeta();
			String displayName = itemMeta.getDisplayName();

			if (!displayName.matches(StringUtils.getColorGroupPattern() + " .*"))
				continue;

			String trimmed = displayName.replaceFirst(" ", "");

			send("\"" + displayName + "&f\" -> \"" + trimmed + "&f\"");

			itemMeta.setDisplayName(trimmed);
			content.setItemMeta(itemMeta);
		}
	}

	@Path("nonLivingEntities")
	void nonLivingEntities() {
		for (EntityType value : EntityType.values())
			if (value.getEntityClass() != null && !LivingEntity.class.isAssignableFrom(value.getEntityClass()))
				send(camelCase(value));
	}

	@Path("hasPlayedBefore")
	void hasPlayedBefore(Player player) {
		send("hasPlayedBefore: " + player.hasPlayedBefore());
	}

	@Path("closeInventory [player]")
	void closeInventory(@Arg("self") Player player) {
		player.closeInventory();
	}

	@Async
	@Path("sha1 <url>")
	void sha1(String url) {
		send(Utils.createSha1(url));
	}

	@Path("glow getColor <player>")
	void getGlowColor(Player player) {
		send(GlowAPI.getGlowColor(player, player()).name());
	}

	@Path("glow set <player> <color>")
	void getGlowColor(Player player, GlowAPI.Color color) {
		GlowAPI.setGlowing(player, color, player());
	}

	@Path("advancements [player] [page]")
	void advancements(@Arg("self") Player player, @Arg("1") int page) {
		BiFunction<Advancement, String, JsonBuilder> formatter = (advancement, index) -> {
			JsonBuilder json = json(" ");
			AdvancementProgress progress = player.getAdvancementProgress(advancement);
			json.next((progress.isDone() ? "&e" : "&c") + advancement.getKey().getKey());

			json.addHover("&eAwarded Criteria:");
			for (String criteria : progress.getAwardedCriteria()) {
				String text = "&7- &e" + criteria;
				Date dateAwarded = progress.getDateAwarded(criteria);
				if (dateAwarded != null)
					text += " &7- " + shortDateFormat(dateAwarded.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
				json.addHover(text);
			}

			json.addHover(" ");
			json.addHover("&cRemaining Criteria:");
			for (String criteria : progress.getRemainingCriteria())
				json.addHover("&7- &c" + criteria);

			return json;
		};

		paginate(new ArrayList<>(PlayerUtils.getAdvancements().values()), formatter, "/nexus advancements " + player.getName(), page);
	}

	@Path("updateWarpFlags")
	void updateWarpFlags() {
		for (SurvivalWarp warp : SurvivalWarp.values()) {
			if (warp == SurvivalWarp.SPAWN) continue;
			runCommand("rg flag -w \"survival\" warp_" + warp.name().toLowerCase() + " greeting");
			runCommand("rg flag -w \"survival\" warp_" + warp.name().toLowerCase() + " farewell");
			runCommand("rg flag -w \"survival\" warp_" + warp.name().toLowerCase() + " nexus-title-fade 10");
			runCommand("rg flag -w \"survival\" warp_" + warp.name().toLowerCase() + " nexus-title-ticks 30");
			runCommand("rg flag -w \"survival\" warp_" + warp.name().toLowerCase() + " nexus-actionbar-ticks 80");
			runCommand("rg flag -w \"survival\" warp_" + warp.name().toLowerCase() + " nexus-farewell-actionbar &4&lPlease move 100+ blocks away");
			runCommand("rg flag -w \"survival\" warp_" + warp.name().toLowerCase() + " nexus-farewell-subtitle &eExiting &3the &6" + camelCase(warp).replace(" ", " #") + " Warp");
			runCommand("rg flag -w \"survival\" warp_" + warp.name().toLowerCase() + " nexus-greeting-subtitle &eEntering &3the &6" + camelCase(warp).replace(" ", " #") + " Warp");
		}

		runCommand("rg flag -w \"survival\" spawn greeting");
		runCommand("rg flag -w \"survival\" spawn farewell");
		runCommand("rg flag -w \"survival\" spawn nexus-title-fade 10");
		runCommand("rg flag -w \"survival\" spawn nexus-title-ticks 30");
		runCommand("rg flag -w \"survival\" spawn nexus-actionbar-ticks 80");
		runCommand("rg flag -w \"survival\" spawn nexus-farewell-actionbar &4&lPlease move 100+ blocks away");
		runCommand("rg flag -w \"survival\" spawn nexus-farewell-subtitle &eExiting &6Spawn");
		runCommand("rg flag -w \"survival\" spawn nexus-greeting-subtitle &eEntering &6Spawn");

		for (LegacySurvivalWarp warp : LegacySurvivalWarp.values()) {
			if (warp == LegacySurvivalWarp.NETHER || warp == LegacySurvivalWarp.SPAWN) continue;
			runCommand("rg flag -w \"world\" warp_" + warp.name().toLowerCase().replace("_", "") + " greeting");
			runCommand("rg flag -w \"world\" warp_" + warp.name().toLowerCase().replace("_", "") + " farewell");
			runCommand("rg flag -w \"world\" warp_" + warp.name().toLowerCase().replace("_", "") + " nexus-title-fade 10");
			runCommand("rg flag -w \"world\" warp_" + warp.name().toLowerCase().replace("_", "") + " nexus-title-ticks 30");
			runCommand("rg flag -w \"world\" warp_" + warp.name().toLowerCase().replace("_", "") + " nexus-actionbar-ticks 80");
			runCommand("rg flag -w \"world\" warp_" + warp.name().toLowerCase().replace("_", "") + " nexus-farewell-actionbar &4&lPlease move 100+ blocks away");
			runCommand("rg flag -w \"world\" warp_" + warp.name().toLowerCase().replace("_", "") + " nexus-farewell-subtitle &eExiting &3the &6" + camelCase(warp).replace(" ", " #") + " Warp");
			runCommand("rg flag -w \"world\" warp_" + warp.name().toLowerCase().replace("_", "") + " nexus-greeting-subtitle &eEntering &3the &6" + camelCase(warp).replace(" ", " #") + " Warp");
		}

		runCommand("rg flag -w \"world\" spawn greeting");
		runCommand("rg flag -w \"world\" spawn farewell");
		runCommand("rg flag -w \"world\" spawn nexus-title-fade 10");
		runCommand("rg flag -w \"world\" spawn nexus-title-ticks 30");
		runCommand("rg flag -w \"world\" spawn nexus-actionbar-ticks 80");
		runCommand("rg flag -w \"world\" spawn nexus-farewell-actionbar &4&lPlease move 100+ blocks away");
		runCommand("rg flag -w \"world\" spawn nexus-farewell-subtitle &eExiting &6Spawn");
		runCommand("rg flag -w \"world\" spawn nexus-greeting-subtitle &eEntering &6Spawn");
	}

	@ConverterFor(Nerd.class)
	Nerd convertToNerd(String value) {
		return new NerdService().get(convertToOfflinePlayer(value));
	}

	@TabCompleterFor(Nerd.class)
	List<String> tabCompleteNerd(String value) {
		return tabCompletePlayer(value);
	}

	private static final Map<String, MongoService> services = new HashMap<>();

	static {
		Reflections reflections = new Reflections(Nexus.class.getPackage().getName() + ".models");
		for (Class<? extends MongoService> service : reflections.getSubTypesOf(MongoService.class)) {
			try {
				services.put(service.getSimpleName(), service.newInstance());
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}

	@ConverterFor(MongoService.class)
	MongoService convertToMongoService(String value) {
		if (!services.containsKey(value))
			error("Service &e" + value + " &cnot found");
		return services.get(value);
	}

	@TabCompleterFor(MongoService.class)
	List<String> tabCompleteMongoService(String value) {
		return services.keySet().stream()
				.filter(serviceName -> serviceName.toLowerCase().startsWith(value.toLowerCase()))
				.collect(Collectors.toList());
	}

	@ConverterFor(StaffMember.class)
	StaffMember convertToStaffMember(String value) {
		OfflinePlayer player = convertToOfflinePlayer(value);
		if (!new Nerd(player).getRank().isStaff())
			error(player.getName() + " is not staff");
		return new StaffMember(player.getUniqueId());
	}

	@TabCompleterFor(StaffMember.class)
	List<String> tabCompleteStaffMember(String filter) {
		return new HoursService().getActivePlayers().stream()
				.filter(player -> new Nerd(player).getRank().isStaff())
				.map(OfflinePlayer::getName)
				.filter(name -> name != null && name.toLowerCase().startsWith(filter.toLowerCase()))
				.collect(Collectors.toList());
	}

}
