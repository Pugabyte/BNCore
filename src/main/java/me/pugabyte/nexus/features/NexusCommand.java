package me.pugabyte.nexus.features;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.regions.Region;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTEntity;
import de.tr7zw.nbtapi.NBTFile;
import eden.utils.Env;
import eden.utils.TimeUtils.Time;
import eden.utils.TimeUtils.Timespan;
import eden.utils.TimeUtils.Timespan.FormatType;
import eden.utils.TimeUtils.Timespan.TimespanBuilder;
import fr.minuskube.inv.SmartInvsPlugin;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.chat.Koda;
import me.pugabyte.nexus.features.crates.models.CrateType;
import me.pugabyte.nexus.features.discord.Discord;
import me.pugabyte.nexus.features.listeners.ResourceWorld;
import me.pugabyte.nexus.features.listeners.TemporaryListener;
import me.pugabyte.nexus.features.minigames.managers.ArenaManager;
import me.pugabyte.nexus.features.minigames.managers.MatchManager;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchEndEvent;
import me.pugabyte.nexus.features.minigames.models.mechanics.MechanicType;
import me.pugabyte.nexus.features.recipes.CustomRecipes;
import me.pugabyte.nexus.features.store.perks.NPCListener;
import me.pugabyte.nexus.features.warps.Warps.LegacySurvivalWarp;
import me.pugabyte.nexus.features.warps.Warps.SurvivalWarp;
import me.pugabyte.nexus.features.wither.WitherChallenge;
import me.pugabyte.nexus.framework.commands.CommandMapUtils;
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
import me.pugabyte.nexus.framework.exceptions.NexusException;
import me.pugabyte.nexus.framework.exceptions.postconfigured.CommandCooldownException;
import me.pugabyte.nexus.framework.exceptions.postconfigured.InvalidInputException;
import me.pugabyte.nexus.framework.features.Features;
import me.pugabyte.nexus.models.MongoService;
import me.pugabyte.nexus.models.cooldown.CooldownService;
import me.pugabyte.nexus.models.hours.HoursService;
import me.pugabyte.nexus.models.nerd.Nerd;
import me.pugabyte.nexus.models.nerd.Nerd.StaffMember;
import me.pugabyte.nexus.models.nerd.NerdService;
import me.pugabyte.nexus.models.nickname.Nickname;
import me.pugabyte.nexus.models.pride21.Pride21User;
import me.pugabyte.nexus.models.pride21.Pride21UserService;
import me.pugabyte.nexus.models.setting.Setting;
import me.pugabyte.nexus.models.setting.SettingService;
import me.pugabyte.nexus.models.task.Task;
import me.pugabyte.nexus.models.task.TaskService;
import me.pugabyte.nexus.utils.ActionBarUtils;
import me.pugabyte.nexus.utils.BlockUtils;
import me.pugabyte.nexus.utils.CitizensUtils;
import me.pugabyte.nexus.utils.ItemBuilder;
import me.pugabyte.nexus.utils.JsonBuilder;
import me.pugabyte.nexus.utils.MaterialTag;
import me.pugabyte.nexus.utils.Name;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.PlayerUtils.Dev;
import me.pugabyte.nexus.utils.SoundBuilder;
import me.pugabyte.nexus.utils.SoundUtils.Jingle;
import me.pugabyte.nexus.utils.StringUtils;
import me.pugabyte.nexus.utils.StringUtils.ProgressBarStyle;
import me.pugabyte.nexus.utils.Tasks;
import me.pugabyte.nexus.utils.Tasks.ExpBarCountdown;
import me.pugabyte.nexus.utils.Utils;
import me.pugabyte.nexus.utils.WorldEditUtils;
import net.citizensnpcs.api.npc.NPC;
import net.dv8tion.jda.api.entities.Member;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.data.type.RedstoneRail;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
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
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.inventivetalent.glow.GlowAPI;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

import static me.pugabyte.nexus.utils.BlockUtils.getBlocksInRadius;
import static me.pugabyte.nexus.utils.BlockUtils.getDirection;
import static me.pugabyte.nexus.utils.ItemUtils.isNullOrAir;
import static me.pugabyte.nexus.utils.StringUtils.colorize;
import static me.pugabyte.nexus.utils.StringUtils.paste;
import static me.pugabyte.nexus.utils.TimeUtils.shortDateFormat;
import static me.pugabyte.nexus.utils.TimeUtils.shortDateTimeFormat;

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

	@Path("soundTest")
	void soundTest() {
		new SoundBuilder("minecraft:custom.dk_jungle_64").receiver(player()).volume(.25).play();
	}

	@Path("cancelReload")
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
			JsonBuilder json = new JsonBuilder(ex.getMessage(), NamedTextColor.RED);
			if (ex instanceof NexusException nex)
				json = new JsonBuilder(nex.getJson());

			error(json.next(", reload queued ").group().next("&e⟳").hover("&eClick to retry manually").command("/nexus reload"));
		}

		for (Player player : PlayerUtils.getOnlinePlayers())
			if (Dev.WAKKA.is(player) || Dev.BLAST.is(player))
				new SoundBuilder(Sound.ENTITY_EVOKER_PREPARE_WOLOLO).receiver(player).play();

		CooldownService cooldownService = new CooldownService();
		if (!cooldownService.check(StringUtils.getUUID0(), "reload", Time.SECOND.x(15)))
			throw new CommandCooldownException(StringUtils.getUUID0(), "reload");

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
			long invCount = PlayerUtils.getOnlinePlayers().stream().filter(player -> SmartInvsPlugin.manager().getInventory(player).isPresent()).count();
			if (invCount > 0)
				throw new InvalidInputException(new JsonBuilder("There are " + invCount + " SmartInvs menus open").command("/nexus smartInvs").hover("&eClick to view"));
		}),
		TEMP_LISTENERS(() -> {
			if (!Nexus.getTemporaryListeners().isEmpty())
				throw new InvalidInputException(new JsonBuilder("There are " + Nexus.getTemporaryListeners().size() + " temporary listeners registered").command("/nexus temporaryListeners").hover("&eClick to view"));
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
				condition.run();
		}

		public void run() {
			runnable.run();
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
		send(PREFIX + "Last reloaded &e" + Timespan.of(lastReload).format() + " ago");
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
		PlayerUtils.getOnlinePlayers().stream()
				.filter(player -> SmartInvsPlugin.manager().getInventory(player).isPresent())
				.forEach(player -> playerInventoryMap.put(player.getName(),
						SmartInvsPlugin.manager().getInventory(player).get().getTitle()));

		if (playerInventoryMap.isEmpty())
			error("No SmartInvs open");

		send(PREFIX + "Open SmartInvs:");
		for (Map.Entry<String, String> entry : playerInventoryMap.entrySet())
			send(" &7- " + entry.getKey() + " - " + entry.getValue());
	}

	@Path("temporaryListeners unregisterOffline")
	void temporaryListeners_unregisterOffline() {
		List<TemporaryListener> unregistered = new ArrayList<>() {{
			for (TemporaryListener temporaryListener : Nexus.getTemporaryListeners())
				if (!temporaryListener.getPlayer().isOnline())
					add(temporaryListener);
		}};

		if (unregistered.isEmpty())
			error("Could not unregister any temporary listeners");

		send(PREFIX + "Unregistered:");
		for (TemporaryListener temporaryListener : unregistered) {
			Nexus.unregisterTemporaryListener(temporaryListener);
			send("&c" + Nickname.of(temporaryListener.getPlayer()) + " &7- " + temporaryListener.getClass().getSimpleName());
		}
	}

	@Path("temporaryListeners")
	void temporaryListeners() {
		if (Nexus.getTemporaryListeners().isEmpty())
			error("No temporary listeners registered");

		send(PREFIX + "Temporary listeners");

		for (TemporaryListener temporaryListener : Nexus.getTemporaryListeners()) {
			final Player player = temporaryListener.getPlayer();

			send((player.isOnline() ? "&e" : "&c") + Nickname.of(player) + " &7- " + temporaryListener.getClass().getSimpleName());
		}
	}

	@Path("stats")
	void stats() {
		send("Features: " + Features.getFeatures().size());
		send("Commands: " + new HashSet<>(Commands.getCommands().values()).size());
		send("Listeners: " + Nexus.getListeners().size());
		send("Temporary Listeners: " + Nexus.getTemporaryListeners().size());
		send("Event Handlers: " + Nexus.getEventHandlers().size());
		send("Services: " + MongoService.getServices().size());
		send("Arenas: " + ArenaManager.getAll().size());
		send("Mechanics: " + MechanicType.values().length);
		send("Recipes: " + CustomRecipes.getRecipes().size());
	}

	@Path("stats commands [page]")
	void statsCommands(@Arg("1") int page) {
		CommandMapUtils mapUtils = Nexus.getInstance().getCommands().getMapUtils();

		Map<Plugin, Integer> commands = new HashMap<>();
		Set<String> keys = new HashSet<>();

		for (Command value : mapUtils.getKnownCommandMap().values()) {
			if (!(value instanceof PluginCommand command))
				continue;

			Plugin plugin = command.getPlugin();

			String commandName = command.getName();
			if (commandName.contains(":"))
				commandName = commandName.split(":")[1];

			String key = plugin.getName() + "-" + commandName;
			if (keys.contains(key))
				continue;

			keys.add(key);
			commands.put(plugin, commands.getOrDefault(plugin, 0) + 1);
		}

		send(PREFIX + "Commands by plugin");
		paginate(Utils.sortByValueReverse(commands).keySet(), (plugin, index) ->
				json("&3" + index + " &e" + plugin.getName() + " &7- " + commands.get(plugin)), "/nexus stats commands", page);
	}

	@Path("stats eventHandlers [page]")
	void statsEventHandlers(@Arg("1") int page) {
		Map<Class<? extends Event>, Integer> counts = new HashMap<>();
		for (Class<? extends Event> eventHandler : Nexus.getEventHandlers())
			counts.put(eventHandler, counts.getOrDefault(eventHandler, 0) + 1);

		if (counts.isEmpty())
			error("No event handlers found");

		Map<Class<? extends Event>, Integer> sorted = Utils.sortByValueReverse(counts);

		send(PREFIX + "Event Handlers");
		BiFunction<Class<? extends Event>, String, JsonBuilder> formatter = (clazz, index) ->
				json("&3" + index + " &e" + clazz.getSimpleName() + " &7- " + counts.get(clazz));
		paginate(sorted.keySet(), formatter, "/nexus stats eventHandlers", page);
	}

	@Confirm
	@Path("resourceWorld reset <test>")
	void resourceWorldReset(boolean test) {
		ResourceWorld.reset(test);
	}

	@Confirm
	@Path("resourceWorld setup <test>")
	void resourceWorldSetup(boolean test) {
		ResourceWorld.setup(test);
	}

	@Path("listTest <player...>")
	void listTest(@Arg(type = OfflinePlayer.class) List<OfflinePlayer> players) {
		send(players.stream().map(Name::of).collect(Collectors.joining(", ")));
	}

	static {
		Tasks.repeatAsync(Time.SECOND, Time.SECOND.x(30), () -> {
			TaskService service = new TaskService();
			service.process("command-test").forEach(task ->
					Tasks.wait(Time.MINUTE.x(2), () -> {
						Map<String, Object> data = task.getJson();
						OfflinePlayer player = PlayerUtils.getPlayer((String) data.get("uuid"));
						PlayerUtils.send(player, data.get("message"));
						service.complete(task);
					}));
		});
	}

	@Path("taskTest <message...>")
	void taskTest(String message) {
		new TaskService().save(new Task("command-test", new HashMap<>() {{
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

	@Path("setFirstJoin <player> <date>")
	void setFirstJoin(Nerd nerd, LocalDateTime firstJoin) {
		nerd.setFirstJoin(firstJoin);
		new NerdService().save(nerd);
		send(PREFIX + "Set " + Nickname.of(nerd) + "'s first join date to &e" + shortDateTimeFormat(firstJoin));
	}

	@Path("setPromotionDate <player> <date>")
	void setPromotionDate(Nerd nerd, LocalDate promotionDate) {
		nerd.setPromotionDate(promotionDate);
		new NerdService().save(nerd);
		send(PREFIX + "Set " + Nickname.of(nerd) + "'s promotion date to &e" + shortDateFormat(promotionDate));
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
		send(player.getRank().getColoredName());
	}

	@Path("getPlayer [player]")
	void getPlayer(@Arg("self") OfflinePlayer player) {
		send(Name.of(player));
	}

	@Path("getClientBrandName <player>")
	void getClientBrandName(@Arg("self") Player player) {
		send(player.getClientBrandName());
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

	@Async
	@Path("cooldown janitor")
	void cooldownJanitor() {
		send(PREFIX + "Janitored " + new CooldownService().janitor() + " records");
	}

	@Path("cooldown forceCME <iterations>")
	void cooldownForceCME(int iterations) {
		CooldownService service = new CooldownService();
		for (int i = 0; i < iterations; i++)
			service.check(uuid(), UUID.randomUUID().toString(), Time.SECOND);
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
	void timespanFormatter(int seconds, FormatType formatType) {
		send(TimespanBuilder.of(seconds).formatType(formatType).format());
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

	private static final String originalMotd = "&f &f &f &f &f &f &f &f &f &f &f &f &f &f &f &f &f " +
			"&a&l⚘ &f &#ffff44&lProject Eden" + (Nexus.getEnv() == Env.PROD ? "" : " &6[" + Nexus.getEnv().name() + "]") + " &f &a&l⚘\n" +
			"&f &f &3Survival &7| &3Creative &7| &3Minigames &7| &3Close Community";
	private static String motd = originalMotd;

	@Path("motd <text...>")
	void motd(String text) {
		motd = colorize(text.replace("\\n", System.lineSeparator()));
		send(PREFIX + "Motd updated");
	}

	@Path("motd reset")
	void motdReset() {
		motd = originalMotd;
		send(PREFIX + "Motd Reset");
	}

	@EventHandler
	public void onServerListPing(ServerListPingEvent event) {
		if (motd != null)
			event.setMotd(colorize(motd));
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
		if (!(block.getState() instanceof Container state))
			return;

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

	@SneakyThrows
	@Path("getOfflineVehicle <player>")
	void getOfflineVehicle(Nerd nerd) {
		NBTFile dataFile = nerd.getDataFile();
		NBTCompound rootVehicle = dataFile.getCompound("RootVehicle");
		if (rootVehicle == null)
			error("RootVehicle compound is null");

		NBTCompound entityCompound = rootVehicle.getCompound("Entity");
		if (entityCompound == null)
			error("Entity compound is null");

		Block air = getTargetBlock().getRelative(BlockFace.UP);
		if (!MaterialTag.ALL_AIR.isTagged(air.getType()))
			error("You must be looking at the ground");

		String id = entityCompound.getString("id");
		EntityType type = EntityType.valueOf(id.replace("minecraft:", "").toUpperCase());

		Entity horse = world().spawnEntity(air.getLocation(), type);
		NBTEntity nbt = new NBTEntity(horse);
		nbt.mergeCompound(entityCompound);

		dataFile.setObject("RootVehicle", null);
		dataFile.save();
		send(PREFIX + "Respawned " + camelCase(type) + " and deleted original");
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

			json.hover("&eAwarded Criteria:");
			for (String criteria : progress.getAwardedCriteria()) {
				String text = "&7- &e" + criteria;
				Date dateAwarded = progress.getDateAwarded(criteria);
				if (dateAwarded != null)
					text += " &7- " + shortDateFormat(dateAwarded.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
				json.hover(text);
			}

			json.hover(" ");
			json.hover("&cRemaining Criteria:");
			for (String criteria : progress.getRemainingCriteria())
				json.hover("&7- &c" + criteria);

			return json;
		};

		paginate(PlayerUtils.getAdvancements().values(), formatter, "/nexus advancements " + player.getName(), page);
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

	@Path("testNewHasRoomFor")
	void hasRoomFor() {
		send("" + PlayerUtils.hasRoomFor(player(), new ItemStack(Material.DIRT, 64), new ItemStack(Material.SNOWBALL, 8)));
	}

	@Path("resetPrideRewardsClaimed")
	void resetPrideRewardsClaimed() {
		Pride21UserService pride21UserService = new Pride21UserService();
		Pride21User pride21User = pride21UserService.get(uuid());
		pride21User.setRewardsClaimed(0);
		pride21UserService.save(pride21User);
	}

	@Path("playJingle <jingle>")
	void playJingle(Jingle jingle) {
		jingle.play(player());
	}

	@Path("forceTpNPC")
	void forceTpNPC() {
		NPC npc = CitizensUtils.getSelectedNPC(player());
		NPCListener.allowNPC(npc);
		runCommand("npc tphere");
	}

	@Path("testTradeable [tradeable]")
	void testTradeable(Boolean tradeable) {
		ItemBuilder item = new ItemBuilder(inventory().getItemInMainHand());
		if (tradeable != null)
			item.tradeable(tradeable);
		Tasks.wait(1, () -> inventory().setItemInMainHand(item.build()));
		Tasks.wait(2, () -> send(String.valueOf(new ItemBuilder(inventory().getItemInMainHand()).isTradeable())));
	}

	@ConverterFor(Nerd.class)
	Nerd convertToNerd(String value) {
		return Nerd.of(convertToOfflinePlayer(value));
	}

	@TabCompleterFor(Nerd.class)
	List<String> tabCompleteNerd(String value) {
		return tabCompletePlayer(value);
	}

	@ConverterFor(StaffMember.class)
	StaffMember convertToStaffMember(String value) {
		OfflinePlayer player = convertToOfflinePlayer(value);
		Nerd nerd = Nerd.of(player);
		if (!nerd.getRank().isStaff())
			error(Nickname.of(nerd) + " is not staff");
		return new StaffMember(player.getUniqueId());
	}

	@TabCompleterFor(StaffMember.class)
	List<String> tabCompleteStaffMember(String filter) {
		return new HoursService().getActivePlayers().stream()
				.filter(player -> Nerd.of(player).getRank().isStaff())
				.map(PlayerUtils::getPlayer)
				.map(Name::of)
				.filter(name -> name != null && name.toLowerCase().startsWith(filter.toLowerCase()))
				.collect(Collectors.toList());
	}

	@ConverterFor(Timespan.class)
	Timespan convertToTimespan(String input) {
		return Timespan.of(input);
	}

}
