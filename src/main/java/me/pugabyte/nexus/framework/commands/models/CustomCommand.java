package me.pugabyte.nexus.framework.commands.models;

import com.google.common.base.Strings;
import eden.interfaces.PlayerOwnedObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import me.pugabyte.nexus.features.commands.staff.MultiCommandCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.ConverterFor;
import me.pugabyte.nexus.framework.commands.models.annotations.Description;
import me.pugabyte.nexus.framework.commands.models.annotations.Fallback;
import me.pugabyte.nexus.framework.commands.models.annotations.HideFromHelp;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Switch;
import me.pugabyte.nexus.framework.commands.models.annotations.TabCompleterFor;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.framework.commands.models.events.CommandRunEvent;
import me.pugabyte.nexus.framework.exceptions.postconfigured.InvalidInputException;
import me.pugabyte.nexus.framework.exceptions.postconfigured.PlayerNotFoundException;
import me.pugabyte.nexus.framework.exceptions.postconfigured.PlayerNotOnlineException;
import me.pugabyte.nexus.framework.exceptions.preconfigured.MustBeCommandBlockException;
import me.pugabyte.nexus.framework.exceptions.preconfigured.MustBeConsoleException;
import me.pugabyte.nexus.framework.exceptions.preconfigured.MustBeIngameException;
import me.pugabyte.nexus.framework.exceptions.preconfigured.NoPermissionException;
import me.pugabyte.nexus.models.MongoService;
import me.pugabyte.nexus.models.nerd.Nerd;
import me.pugabyte.nexus.models.nerd.NerdService;
import me.pugabyte.nexus.models.nerd.Rank;
import me.pugabyte.nexus.models.nickname.Nickname;
import me.pugabyte.nexus.utils.ColorType;
import me.pugabyte.nexus.utils.ItemUtils;
import me.pugabyte.nexus.utils.JsonBuilder;
import me.pugabyte.nexus.utils.MaterialTag;
import me.pugabyte.nexus.utils.Name;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.RandomUtils;
import me.pugabyte.nexus.utils.StringUtils;
import me.pugabyte.nexus.utils.WorldGroup;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Parameter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static me.pugabyte.nexus.utils.BlockUtils.isNullOrAir;
import static me.pugabyte.nexus.utils.StringUtils.an;
import static me.pugabyte.nexus.utils.StringUtils.trimFirst;
import static me.pugabyte.nexus.utils.TimeUtils.parseDate;
import static me.pugabyte.nexus.utils.TimeUtils.parseDateTime;

@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
@SuppressWarnings({"SameParameterValue", "unused", "WeakerAccess", "UnusedReturnValue"})
public abstract class CustomCommand extends ICustomCommand {
	@NonNull
	@Getter
	protected CommandEvent event;
	public String PREFIX = StringUtils.getPrefix(getName());
	public String DISCORD_PREFIX = StringUtils.getDiscordPrefix(getName());

	public String getPrefix() {
		return PREFIX;
	}

	public String getAliasUsed() {
		return event.getAliasUsed();
	}

	public void _shutdown() {}

	protected boolean isCommandEvent() {
		return event instanceof CommandRunEvent;
	}

	protected boolean isPlayerCommandEvent() {
		return event instanceof CommandRunEvent && isPlayer();
	}

	protected String camelCase(Enum<?> _enum) {
		if (_enum == null) return "null";
		return camelCase(_enum.name());
	}

	protected String camelCase(String string) {
		return StringUtils.camelCase(string);
	}

	protected String plural(String label, Number number) {
		return StringUtils.plural(label, number);
	}

	protected String plural(String labelSingle, String labelPlural, Number number) {
		return StringUtils.plural(labelSingle, labelPlural, number);
	}

	protected ItemStack getTool() {
		return ItemUtils.getTool(player());
	}

	protected ItemStack getToolRequired() {
		return ItemUtils.getToolRequired(player());
	}

	protected EquipmentSlot getHandWithTool() {
		return ItemUtils.getHandWithTool(player());
	}

	protected EquipmentSlot getHandWithToolRequired() {
		return ItemUtils.getHandWithToolRequired(player());
	}

	protected Block getTargetBlock() {
		return player().getTargetBlockExact(500);
	}

	protected Block getTargetBlockRequired() {
		Block targetBlock = getTargetBlock();
		if (isNullOrAir(targetBlock))
			error("You must be looking at a block");
		return targetBlock;
	}

	protected Sign getTargetSignRequired() {
		Block targetBlock = getTargetBlock();
		Material material = targetBlock.getType();
		if (ItemUtils.isNullOrAir(material) || !MaterialTag.SIGNS.isTagged(material))
			error("You must be looking at a sign");
		return (Sign) targetBlock.getState();
	}

	protected Entity getTargetEntity() {
		return player().getTargetEntity(120);
	}

	protected Entity getTargetEntityRequired() {
		Entity targetEntity = getTargetEntity();
		if (targetEntity == null)
			error("You must be looking at an entity");
		return targetEntity;
	}

	protected Entity getTargetEntityRequired(EntityType entityType) {
		Entity targetEntity = getTargetEntity();
		if (targetEntity == null || targetEntity.getType() != entityType)
			error("You must be looking at " + an(entityType.name()) + " " + camelCase(entityType));
		return targetEntity;
	}

	protected LivingEntity getTargetLivingEntityRequired() {
		Entity targetEntity = getTargetEntity();
		if (!(targetEntity instanceof LivingEntity))
			error("You must be looking at a living entity");
		return (LivingEntity) targetEntity;
	}

	protected Player getTargetPlayerRequired() {
		Entity targetEntity = getTargetEntity();
		if (!(targetEntity instanceof Player))
			error("You must be looking at a player");
		return (Player) targetEntity;
	}

	protected Rank rank() {
		return Rank.of(player());
	}

	protected Location location() {
		if (isCommandBlock())
			return commandBlock().getBlock().getLocation();
		return player().getLocation();
	}

	protected Block block() {
		return location().getBlock();
	}

	protected World world() {
		return location().getWorld();
	}

	protected WorldGroup worldGroup() {
		return WorldGroup.of(location());
	}

	protected PlayerInventory inventory() {
		return player().getInventory();
	}

	public void giveItem(ItemStack item) {
		PlayerUtils.giveItems(player(), Collections.singletonList(item));
	}
	public void giveItems(Collection<ItemStack> items) {
		PlayerUtils.giveItems(player(), items, null);
	}

	protected void send(CommandSender sender, String message, Object... objects) {
		send(sender, json(String.format(message, objects)));
	}

	protected void send(CommandSender sender, BaseComponent... baseComponents) {
		sender.sendMessage(baseComponents);
	}

	protected void send(String uuid, String message) {
		send(UUID.fromString(uuid), message);
	}

	protected void send(UUID uuid, String message) {
		OfflinePlayer player = PlayerUtils.getPlayer(uuid.toString());
		if (player != null && player.isOnline())
			send(PlayerUtils.getPlayer(uuid), message);
	}

	protected void send(Object object) {
		if (object instanceof String)
			send(sender(), object);
		else if (object instanceof JsonBuilder)
			send(sender(), object);
		else if (object instanceof Component)
			send(sender(), object);
		else
			throw new InvalidInputException("Cannot send object: " + object.getClass().getSimpleName());
	}

	protected void send() {
		send(sender(), "");
	}

	protected void send(String message) {
		send(sender(), json(message));
	}

	protected void send(ComponentLike component) {
		send(sender(), component);
	}

	protected void send(Object sender, Object message) {
		PlayerUtils.send(sender, message);
	}

	protected void line() {
		line(1);
	}

	protected void line(CommandSender player) {
		line(player, 1);
	}

	protected void line(int count) {
		line(sender(), count);
	}

	protected void line(CommandSender player, int count) {
		for (int i = 0; i < count; i++)
			send(player, "");
	}

	protected JsonBuilder json() {
		return json("");
	}

	protected JsonBuilder json(String message) {
		return new JsonBuilder(message);
	}

	protected String toPrettyString(Object object) {
		return StringUtils.toPrettyString(object);
	}

	@Contract("_ -> fail")
	public void error(String error) {
		throw new InvalidInputException(error);
	}

	@Contract("_ -> fail")
	public void error(JsonBuilder error) {
		throw new InvalidInputException(error);
	}

	@Contract("_ -> fail")
	public void error(ComponentLike error) {
		throw new InvalidInputException(error);
	}

	@Deprecated
	public void error(Player player, String error) {
		player.sendMessage(new JsonBuilder(NamedTextColor.RED).next(error));
	}

	public void rethrow(Throwable ex) {
		throw new RuntimeException(ex);
	}

	public void permissionError() {
		throw new NoPermissionException();
	}

	public boolean hasPermission(String permission) {
		if (isCommandBlock() || isConsole())
			return true;
		else
			return player().hasPermission(permission);
	}

	public void showUsage() {
		error(((CommandRunEvent) event).getUsageMessage());
	}

	protected CommandSender sender() {
		return event.getSender();
	}

	protected Player player() {
		if (!isPlayer())
			throw new MustBeIngameException();

		return (Player) event.getSender();
	}

	protected OfflinePlayer offlinePlayer() {
		if (!isPlayer())
			throw new MustBeIngameException();

		return Bukkit.getOfflinePlayer(uuid());
	}

	protected Nerd nerd() {
		return Nerd.of(player());
	}

	protected UUID uuid() {
		if (isPlayer())
			return player().getUniqueId();
		return StringUtils.getUUID0();
	}

	protected String name() {
		if (!isPlayer())
			return camelCase(sender().getName());
		else
			return nerd().getName();
	}

	protected String nickname() {
		if (!isPlayer())
			return sender().getName();
		else
			return Nickname.of(player());
	}

	protected String nickname(OfflinePlayer player) {
		return Nickname.of(player);
	}

	protected String nickname(PlayerOwnedObject player) {
		return Nickname.of(player);
	}

	protected ConsoleCommandSender console() {
		if (!isConsole())
			throw new MustBeConsoleException();

		return (ConsoleCommandSender) event.getSender();
	}

	protected BlockCommandSender commandBlock() {
		if (!isCommandBlock())
			throw new MustBeCommandBlockException();

		return (BlockCommandSender) event.getSender();
	}

	protected boolean isPlayer() {
		return isPlayer(sender());
	}

	private boolean isPlayer(Object object) {
		return object instanceof Player;
	}

	protected boolean isOfflinePlayer() {
		return isOfflinePlayer(sender());
	}

	private boolean isOfflinePlayer(Object object) {
		return object instanceof OfflinePlayer;
	}

	protected boolean isConsole() {
		return isConsole(sender());
	}

	private boolean isConsole(Object object) {
		return object instanceof ConsoleCommandSender;
	}

	protected boolean isCommandBlock() {
		return isCommandBlock(sender());
	}

	private boolean isCommandBlock(Object object) {
		return object instanceof BlockCommandSender;
	}

	protected boolean isSelf(PlayerOwnedObject object) {
		return isPlayer() && isSelf(PlayerUtils.getPlayer(object.getUuid()));
	}

	protected boolean isSelf(OfflinePlayer player) {
		return isPlayer() && isSelf(player(), player);
	}

	protected boolean isSelf(Player player) {
		return isPlayer() && isSelf(player(), player);
	}

	protected boolean isSelf(OfflinePlayer self, OfflinePlayer player) {
		return isPlayer() && self.getUniqueId().equals(player.getUniqueId());
	}

	protected boolean isStaff() {
		return !isPlayer() || isStaff(player());
	}

	protected boolean isStaff(Player player) {
		return isPlayer(player) && Rank.of(player).isStaff();
	}

	protected boolean isStaff(OfflinePlayer player) {
		return isOfflinePlayer(player) && Rank.of(player).isStaff();
	}

	protected boolean isSeniorStaff() {
		return !isPlayer() || isSeniorStaff(player());
	}

	protected boolean isSeniorStaff(Player player) {
		return isPlayer(player) && Rank.of(player).isSeniorStaff();
	}

	protected boolean isSeniorStaff(OfflinePlayer player) {
		return isOfflinePlayer(player) && Rank.of(player).isSeniorStaff();
	}

	protected boolean isAdmin() {
		return !isPlayer() || isAdmin(player());
	}

	protected boolean isAdmin(Player player) {
		return isPlayer(player) && Rank.of(player).isAdmin();
	}

	protected boolean isAdmin(OfflinePlayer player) {
		return isOfflinePlayer(player) && Rank.of(player).isAdmin();
	}

	protected boolean isNullOrEmpty(String string) {
		return Strings.isNullOrEmpty(string);
	}

	protected void runCommand(String commandNoSlash) {
		runCommand(sender(), commandNoSlash);
	}

	protected void runCommand(CommandSender sender, String commandNoSlash) {
		PlayerUtils.runCommand(sender, commandNoSlash);
	}

	protected void runMultiCommand(String... commands) {
		runMultiCommand(sender(), commands);
	}

	protected void runMultiCommand(CommandSender sender, String... commands) {
		MultiCommandCommand.run(sender, List.of(commands));
	}

	protected void runCommandAsOp(String commandNoSlash) {
		runCommandAsOp(sender(), commandNoSlash);
	}

	protected void runCommandAsOp(CommandSender sender, String commandNoSlash) {
		PlayerUtils.runCommandAsOp(sender, commandNoSlash);
	}

	protected void runCommandAsConsole(String commandNoSlash) {
		PlayerUtils.runCommandAsConsole(commandNoSlash);
	}

	protected void checkPermission(String permission) {
		if (isPlayer())
			if (!sender().hasPermission(permission))
				throw new NoPermissionException();
	}

	protected String argsString() {
		return argsString(args());
	}

	protected String argsString(List<String> args) {
		if (args() == null || args().size() == 0) return "";
		return String.join(" ", args());
	}

	protected List<String> args() {
		return event.getArgs();
	}

	protected void setArgs(List<String> args) {
		event.setArgs(args);
	}

	protected String arg(int i) {
		return arg(i, false);
	}

	protected String arg(int i, boolean rest) {
		if (event.getArgs().size() < i) return null;
		if (rest)
			return String.join(" ", event.getArgs().subList(i - 1, event.getArgs().size()));

		String result = event.getArgs().get(i - 1);
		if (Strings.isNullOrEmpty(result)) return null;
		return result;
	}

	protected boolean isIntArg(int i) {
		if (event.getArgs().size() < i) return false;
		return isInt(arg(i));
	}

	protected boolean isInt(String input) {
		try {
			Integer.parseInt(input);
			return true;
		} catch (NumberFormatException ex) {
			return false;
		}
	}

	protected int asInt(String input) {
		try {
			return Integer.parseInt(input);
		} catch (NumberFormatException ex) {
			throw new InvalidInputException("Argument &e" + input + " &cis not a valid integer");
		}
	}

	protected Integer intArg(int i) {
		if (event.getArgs().size() < i) return null;
		try {
			return Integer.parseInt(arg(i));
		} catch (NumberFormatException ex) {
			throw new InvalidInputException("Argument &e#" + i + " &cis not a valid integer");
		}
	}

	protected boolean isDoubleArg(int i) {
		if (event.getArgs().size() < i) return false;
		return isDouble(arg(i));
	}

	protected boolean isDouble(String input) {
		try {
			Double.parseDouble(input);
			return true;
		} catch (NumberFormatException ex) {
			return false;
		}
	}

	protected double asDouble(String input) {
		try {
			return Double.parseDouble(input);
		} catch (NumberFormatException ex) {
			throw new InvalidInputException("Argument &e" + input + " &cis not a valid double");
		}
	}

	protected Double doubleArg(int i) {
		if (event.getArgs().size() < i) return null;
		try {
			return Double.parseDouble(arg(i));
		} catch (NumberFormatException ex) {
			throw new InvalidInputException("Argument &e#" + i + " &cis not a valid double");
		}
	}

	protected boolean isFloatArg(int i) {
		if (event.getArgs().size() < i) return false;
		return isFloat(arg(i));
	}

	protected boolean isFloat(String input) {
		try {
			Float.parseFloat(input);
			return true;
		} catch (NumberFormatException ex) {
			return false;
		}
	}

	protected float asFloat(String input) {
		try {
			return Float.parseFloat(input);
		} catch (NumberFormatException ex) {
			throw new InvalidInputException("Argument &e" + input + " &cis not a valid float");
		}
	}

	protected Float floatArg(int i) {
		if (event.getArgs().size() < i) return null;
		try {
			return Float.parseFloat(arg(i));
		} catch (NumberFormatException ex) {
			throw new InvalidInputException("Argument &e#" + i + " &cis not a valid float");
		}
	}

	protected Boolean booleanArg(int i) {
		if (event.getArgs().size() < i) return null;
		String value = arg(i);
		if (Arrays.asList("enable", "on", "yes", "1").contains(value)) value = "true";
		return Boolean.parseBoolean(value);
	}

	protected boolean isOfflinePlayerArg(int i) {
		if (event.getArgs().size() < i) return false;
		try {
			PlayerUtils.getPlayer(arg(i));
			return true;
		} catch (PlayerNotFoundException ex) {
			return false;
		}
	}

	protected OfflinePlayer offlinePlayerArg(int i) {
		if (event.getArgs().size() < i) return null;
		return PlayerUtils.getPlayer(arg(i));
	}

	protected boolean isPlayerArg(int i) {
		if (event.getArgs().size() < i) return false;
		try {
			return PlayerUtils.getPlayer(arg(i)).isOnline();
		} catch (PlayerNotFoundException ex) {
			return false;
		}
	}

	protected Player playerArg(int i) {
		if (event.getArgs().size() < i) return null;
		OfflinePlayer player = PlayerUtils.getPlayer(arg(i));
		if (!player.isOnline())
			throw new PlayerNotOnlineException(player);
		return player.getPlayer();
	}

	protected void fallback() {
		Fallback fallback = getClass().getAnnotation(Fallback.class);
		if (fallback != null)
			PlayerUtils.runCommand(sender(), fallback.value() + ":" + event.getAliasUsed() + " " + event.getArgsString());
		else
			throw new InvalidInputException("Nothing to fallback to");
	}

	@SneakyThrows
	protected PlayerOwnedObject convertToPlayerOwnedObject(String value, Class<? extends PlayerOwnedObject> type) {
		Class<? extends MongoService> service = (Class<? extends MongoService>) MongoService.ofObject(type);
		if (service != null) {
			final OfflinePlayer player = convertToOfflinePlayer(value);
			if (player != null)
				return service.newInstance().get(player);
		}
		return null;
	}

	@ConverterFor(OfflinePlayer.class)
	public OfflinePlayer convertToOfflinePlayer(String value) {
		if (value == null) return null;
		if ("null".equalsIgnoreCase(value)) return null;
		if ("self".equalsIgnoreCase(value)) value = uuid().toString();
		return PlayerUtils.getPlayer(value.replaceFirst("[pP]:", ""));
	}

	@ConverterFor(Player.class)
	public Player convertToPlayer(String value) {
		if (isCommandBlock() || isAdmin()) {
			if ("@p".equals(value))
				return PlayerUtils.getNearestPlayer(location()).getObject();
			if ("@r".equals(value))
				return RandomUtils.randomElement(PlayerUtils.getOnlinePlayers(isPlayer() ? player() : null));
			if ("@s".equals(value))
				return player();
		}

		OfflinePlayer offlinePlayer = convertToOfflinePlayer(value);
		return convertToPlayer(offlinePlayer);
	}

	public Player convertToPlayer(OfflinePlayer offlinePlayer) {
		if (!offlinePlayer.isOnline() || offlinePlayer.getPlayer() == null)
			throw new PlayerNotOnlineException(offlinePlayer);
		if (isPlayer() && !PlayerUtils.canSee(player(), offlinePlayer.getPlayer()))
			throw new PlayerNotOnlineException(offlinePlayer);

		return offlinePlayer.getPlayer();
	}

	@TabCompleterFor({Player.class, OfflinePlayer.class})
	public List<String> tabCompletePlayer(String filter) {
		return PlayerUtils.getOnlinePlayers().stream()
				.filter(player -> PlayerUtils.canSee(player(), player))
				.map(Nickname::of)
				.filter(name -> name.toLowerCase().startsWith(filter.replaceFirst("[pP]:", "").toLowerCase()))
				.collect(toList());
	}

//	@TabCompleterFor(OfflinePlayer.class)
	public List<String> tabCompleteOfflinePlayer(String filter) {
		List<String> online = tabCompletePlayer(filter);
		if (!online.isEmpty() || filter.length() < 3)
			return online;

		return new NerdService().find(filter).stream()
				.map(Nerd::getName)
				.filter(name -> name.toLowerCase().startsWith(filter.replaceFirst("[pP]:", "").toLowerCase()))
				.collect(toList());
	}

	@ConverterFor(World.class)
	World convertToWorld(String value) {
		if ("current".equalsIgnoreCase(value))
			return world();
		if ("null".equalsIgnoreCase(value))
			return null;
		World world = Bukkit.getWorld(value);
		if (world == null)
			throw new InvalidInputException("World from " + value + " not found");
		return world;
	}

	@TabCompleterFor(World.class)
	List<String> tabCompleteWorld(String filter) {
		return Bukkit.getWorlds().stream()
				.map(World::getName)
				.filter(name -> name.toLowerCase().startsWith(filter.toLowerCase()))
				.collect(toList());
	}

	@TabCompleterFor({Boolean.class})
	List<String> tabCompleteBoolean(String filter) {
		return Stream.of("true", "false")
				.filter(bool -> bool.toLowerCase().startsWith(filter.toLowerCase()))
				.collect(toList());
	}

	@ConverterFor(Material.class)
	Material convertToMaterial(String value) {
		if (isInt(value))
			return Material.values()[Integer.parseInt(value)];

		Material material = Material.matchMaterial(value);
		if (material == null)
			throw new InvalidInputException("Material from " + value + " not found");
		return material;
	}

	@ConverterFor(ChatColor.class)
	ChatColor convertToChatColor(String value) {
		if (StringUtils.getHexPattern().matcher(value).matches())
			return ChatColor.of(value.replaceFirst("&", ""));

		try {
			return ChatColor.of(value.toUpperCase());
		} catch (IllegalArgumentException ex) {
			throw new InvalidInputException("Color &e" + value + "&c not found");
		}
	}

	@TabCompleterFor(ChatColor.class)
	List<String> tabCompleteChatColor(String filter) {
		return Arrays.stream(ChatColor.values())
				.filter(color -> color.getColor() != null)
				.map(ChatColor::getName)
				.filter(name -> name.startsWith(filter.toLowerCase()))
				.collect(toList());
	}

	@ConverterFor(ColorType.class)
	ColorType convertToColorType(String value) {
		ColorType color = ColorType.of(value.replace('_', ' ').toLowerCase());
		if (color == null)
			throw new InvalidInputException("Color &e" + value + "&c not found");
		return color;
	}

	@TabCompleterFor(ColorType.class)
	List<String> tabCompleteColorType(String filter) {
		return Arrays.stream(ColorType.values())
				.map(ColorType::getName)
				.filter(name -> name.replace(' ', '_').startsWith(filter.toLowerCase()))
				.collect(toList());
	}

	@ConverterFor(LocalDate.class)
	public LocalDate convertToLocalDate(String value) {
		return parseDate(value);
	}

	@ConverterFor(LocalDateTime.class)
	public LocalDateTime convertToLocalDateTime(String value) {
		return parseDateTime(value);
	}

	@ConverterFor(Enchantment.class)
	Enchantment convertToEnchantment(String value) {
		Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(value));
		if (enchantment == null)
			throw new InvalidInputException("Enchantment from &e" + value + "&c not found");
		return enchantment;
	}

	@TabCompleterFor(Enchantment.class)
	List<String> applicableEnchantmentsTabCompleter(String filter) {
		List<String> results = new ArrayList<>();
		try {
			List<String> enchants = ItemUtils.getApplicableEnchantments(getToolRequired()).stream()
					.map(enchantment -> enchantment.getKey().getKey())
					.filter(enchantment -> enchantment.toLowerCase().startsWith(filter.toLowerCase()))
					.collect(toList());
			if (enchants.size() == 0)
				return getAllEnchants(filter);
			else
				return enchants;
		} catch (InvalidInputException ignore) {
			return getAllEnchants(filter);
		}
	}

	public List<String> getAllEnchants(String filter) {
		return Arrays.stream(Enchantment.values())
				.filter(enchantment -> enchantment.getKey().getKey().toLowerCase().startsWith(filter.toLowerCase()))
				.map(enchantment -> enchantment.getKey().getKey())
				.collect(toList());
	}

	@TabCompleterFor(LivingEntity.class)
	protected List<String> tabCompleteLivingEntity(String value) {
		return new ArrayList<>() {{
			for (EntityType entityType : EntityType.values()) {
				Class<? extends Entity> entityClass = entityType.getEntityClass();
				if (entityClass != null && LivingEntity.class.isAssignableFrom(entityClass))
					if (entityType.name().toLowerCase().startsWith(value.toLowerCase()))
						add(entityType.name().toLowerCase());
			}
		}};
	}

	protected <T> void paginate(Collection<T> values, BiFunction<T, String, JsonBuilder> formatter, String command, int page) {
		paginate(values, formatter, command, page, 10);
	}

	protected <T> void paginate(Collection<T> values, BiFunction<T, String, JsonBuilder> formatter, String command, int page, int amount) {
		if (page < 1)
			error("Page number must be 1 or greater");

		int start = (page - 1) * amount;
		if (values.size() < start)
			error("No results on page " + page);

		int end = Math.min(values.size(), start + amount);

		line();
		AtomicInteger index = new AtomicInteger(start);
		new LinkedList<>(values).subList(start, end).forEach(t -> send(formatter.apply(t, getLeadingZeroIndex(index))));
		line();

		boolean first = page == 1;
		boolean last = end == values.size();

		if (first && last)
			return;

		JsonBuilder buttons = json();
		if (first)
			buttons.next("&7 « Previous  &3");
		else
			buttons.next("&e « Previous  &3").command(command + " " + (page - 1));

		buttons.group().next("&3|&3|").group();

		if (last)
			buttons.next("  &7Next »");
		else
			buttons.next("  &eNext »").command(command + " " + (page + 1));

		send(buttons.group());
	}

	@NotNull
	private String getLeadingZeroIndex(AtomicInteger index) {
		int nextIndex = index.incrementAndGet();
		int nextMagnitude = Integer.parseInt("1" + Strings.repeat("0", String.valueOf(nextIndex).length()));
		int needsLeading0 = nextMagnitude - 10;

		String string = String.valueOf(nextIndex);
		if (nextIndex > 1 && nextIndex == nextMagnitude / 10)
			return string;

		if (nextIndex > needsLeading0)
			string = "0" + string;
		return string;
	}

	@Path("help")
	protected void help() {
		List<String> aliases = getAllAliases();
		if (aliases.size() > 1)
			send(PREFIX + "Aliases: " + String.join("&e, &3", aliases));

		List<JsonBuilder> lines = new ArrayList<>();
		getPathMethods(event).forEach(method -> {
			Path path = method.getAnnotation(Path.class);
			Description desc = method.getAnnotation(Description.class);
			HideFromHelp hide = method.getAnnotation(HideFromHelp.class);

			if (hide != null) return;
			if ("help".equals(path.value()) || "?".equals(path.value())) return;

			String usage = "/" + getAliasUsed().toLowerCase() + " " + (isNullOrEmpty(path.value()) ? "" : path.value());
			String description = (desc == null ? "" : " &7- " + desc.value());
			StringBuilder suggestion = new StringBuilder();
			for (String word : usage.split(" ")) {
				if (word.startsWith("[") || word.startsWith("<"))
					break;
				if (word.startsWith("("))
					suggestion.append(trimFirst(word.split("\\|")[0]));
				else
					suggestion.append(word).append(" ");
			}

			lines.add(json("&c" + usage + description).suggest(suggestion.toString()));
		});

		if (lines.size() == 0)
			error("No usage available");

		send(PREFIX + "Usage:");
		lines.forEach(this::send);
	}


	@AllArgsConstructor
	public class WhoFormatter {
		private final OfflinePlayer self, target;
		private final WhoType whoType;

		public String format() {
			boolean self = isSelf(this.self, target);
			String name = Objects.requireNonNullElse(Name.of(target), "Unknown");

			if (whoType == WhoType.POSSESSIVE_UPPER || whoType == WhoType.POSSESSIVE_LOWER)
				return self ? (whoType == WhoType.POSSESSIVE_UPPER ? "Y" : "y") + "our" : name + "'" + (name.endsWith("s") ? "" : "s");
			else if (whoType == WhoType.ACTIONARY_UPPER || whoType == WhoType.ACTIONARY_LOWER)
				return self ? (whoType == WhoType.ACTIONARY_UPPER ? "Y" : "y") + "ou do" : name + " does";

			throw new InvalidInputException("Unknown format action");
		}
	}

	public String formatWho(PlayerOwnedObject target, WhoType whoType) {
		return formatWho(player(), target, whoType);
	}

	public String formatWho(OfflinePlayer target, WhoType whoType) {
		return formatWho(player(), target, whoType);
	}

	public String formatWho(Player self, PlayerOwnedObject target, WhoType whoType) {
		return formatWho(PlayerUtils.getPlayer(target.getUuid()), whoType);
	}

	public String formatWho(Player self, OfflinePlayer target, WhoType whoType) {
		return new WhoFormatter(self, target, whoType).format();
	}

	public enum WhoType {
		POSSESSIVE_UPPER,
		POSSESSIVE_LOWER,
		ACTIONARY_UPPER,
		ACTIONARY_LOWER
	}

	public static Pattern getSwitchPattern(Parameter parameter) {
		Switch annotation = parameter.getDeclaredAnnotation(Switch.class);
		String regex = "(?i)^(--" + parameter.getName();
		char shorthand = annotation.shorthand();
		if (shorthand != '-')
			regex += "|-" + shorthand;

		return Pattern.compile(regex + ")(=[^\\s]+)?$");
	}

}
