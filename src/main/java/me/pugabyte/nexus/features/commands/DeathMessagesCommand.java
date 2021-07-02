package me.pugabyte.nexus.features.commands;

import com.gmail.nossr50.mcMMO;
import com.google.gson.Gson;
import eden.utils.TimeUtils.Time;
import eden.utils.TimeUtils.Timespan;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;
import me.pugabyte.nexus.API;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.chat.Chat.Broadcast;
import me.pugabyte.nexus.features.chat.Chat.StaticChannel;
import me.pugabyte.nexus.features.commands.MuteMenuCommand.MuteMenuProvider.MuteMenuItem;
import me.pugabyte.nexus.features.discord.Discord;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Arg;
import me.pugabyte.nexus.framework.commands.models.annotations.ConverterFor;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.annotations.TabCompleterFor;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.framework.exceptions.postconfigured.InvalidInputException;
import me.pugabyte.nexus.framework.exceptions.postconfigured.PlayerNotFoundException;
import me.pugabyte.nexus.models.chat.Chatter;
import me.pugabyte.nexus.models.chat.ChatterService;
import me.pugabyte.nexus.models.deathmessages.DeathMessages;
import me.pugabyte.nexus.models.deathmessages.DeathMessages.Behavior;
import me.pugabyte.nexus.models.deathmessages.DeathMessagesService;
import me.pugabyte.nexus.models.mutemenu.MuteMenuUser;
import me.pugabyte.nexus.models.nickname.Nickname;
import me.pugabyte.nexus.utils.AdventureUtils;
import me.pugabyte.nexus.utils.JsonBuilder;
import me.pugabyte.nexus.utils.RandomUtils;
import me.pugabyte.nexus.utils.Tasks;
import me.pugabyte.nexus.utils.Utils;
import me.pugabyte.nexus.utils.WorldGroup;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEvent.ShowEntity;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

import static org.apache.commons.lang.StringUtils.countMatches;

@NoArgsConstructor
public class DeathMessagesCommand extends CustomCommand implements Listener {
	private static final Pattern HEART_PATTERN = Pattern.compile("^[\u2764\u25A0]+$");
	private final DeathMessagesService service = new DeathMessagesService();

	public DeathMessagesCommand(@NonNull CommandEvent event) {
		super(event);
	}

	static {
		Tasks.repeatAsync(Time.SECOND.x(10), Time.MINUTE, () -> {
			DeathMessagesService service = new DeathMessagesService();
			for (DeathMessages deathMessages : service.getExpired()) {
				deathMessages.setBehavior(Behavior.SHOWN);
				deathMessages.setExpiration(null);
				service.save(deathMessages);
			}
		});
	}

	@Data
	@Builder
	private static class DeathMessagesConfig {
		private final Map<String, CustomDeathMessage> messages;

		public static CustomDeathMessage of(String key) {
			return config.getMessages().get(key.toLowerCase());
		}
	}

	@Data
	@Builder
	private static class CustomDeathMessage {
		private transient String key;
		private String original;
		private List<String> custom;
		private List<String> suggestions;

		public String random() {
			return RandomUtils.randomElement(custom);
		}

		public int count() {
			return custom.size();
		}

		public boolean hasCustom() {
			return count() > 0;
		}
	}

	private static DeathMessagesConfig config;

	@SneakyThrows
	public static void reloadConfig() {
		config = new Gson().fromJson(FileUtils.readFileToString(getFile()), DeathMessagesConfig.class);
		config.getMessages().forEach((key, custom) -> {
			custom.setKey(key);
			if (custom.getCustom() == null)
				custom.setCustom(new ArrayList<>());
			if (custom.getSuggestions() == null)
				custom.setSuggestions(new ArrayList<>());
		});
	}

	static {
		reloadConfig();
	}

	public static File getFile() {
		return Nexus.getFile(getFileName());
	}

	@NotNull
	private static String getFileName() {
		return "death-messages.json";
	}

	@SneakyThrows
	private void save() {
		FileUtils.write(getFile(), API.get().getPrettyPrinter().create().toJson(config));
	}

	@Path("reload")
	@Permission("group.admin")
	void reload() {
		reloadConfig();
		int total = config.getMessages().values().stream().map(CustomDeathMessage::count).reduce(0, Integer::sum);
		send(PREFIX + "Loaded " + config.getMessages().size() + " keys with " + total + " custom messages");
	}

	@Path("list [page]")
	void list(@Arg("1") int page) {
		final List<CustomDeathMessage> messages = config.getMessages().values().stream().filter(CustomDeathMessage::hasCustom).toList();
		if (messages.isEmpty())
			error("No custom death messages configured");

		BiFunction<CustomDeathMessage, String, JsonBuilder> formatter = (config, index) -> {
			final JsonBuilder json = json("&3" + index + " &e" + config.getKey() + " &3(" + config.count() + ") &7- " + config.getOriginal());
			int shown = 0;
			for (String customMessage : config.getCustom()) {
				if (++shown > 5) {
					json.hover("&7And " + (config.count() - 5) + " more...");
					break;
				}

				json.hover("&7" + customMessage);
			}
			return json.command("/deathmessages messages " + config.getKey());
		};

		paginate(messages, formatter, "/deathmessages list", page);
	}

	@Path("messages <key> [page]")
	void list(CustomDeathMessage config, @Arg("1") int page) {
		final List<String> customMessages = config.getCustom();
		if (Utils.isNullOrEmpty(customMessages))
			error("No custom messages for key &e" + config.getKey());

		send(PREFIX + "Custom messages for key &e" + config.getKey());

		paginate(customMessages, (message, index) -> json("&3" + index + " &7" + message), "/deathmessages list " + config.getKey(), page);
	}

	@Path("suggest <key> <message...>")
	void suggest(CustomDeathMessage config, String message) {
		final int expectedVariables = countMatches(config.getOriginal(), "%s");
		final int foundVariables = countMatches(message, "%s");

		if (expectedVariables != foundVariables)
			error("Your message must contain the same amount of variables (&e%s&c) as the default message " +
					"(Found &e" + foundVariables + "&c, expected &e" + expectedVariables + "&c: &7" + config.getOriginal() + "&c)");

		config.getSuggestions().add(message);
		save();

		send(PREFIX + "Added suggestion for key " + config.getKey());
	}

	@Path("behavior <behavior> [player] [duration...]")
	void toggle(Behavior behavior, @Arg(value = "self", permission = "group.staff") OfflinePlayer player, @Arg(permission = "group.staff") Timespan duration) {
		final DeathMessages deathMessages = service.get(player);

		deathMessages.setBehavior(behavior);
		if (!duration.isNull())
			deathMessages.setExpiration(duration.fromNow());

		service.save(deathMessages);
		send(PREFIX + "Set " + (isSelf(deathMessages) ? "your" : "&e" + player.getName() + "'s") + " &3death message " +
				"behavior to &e" + camelCase(behavior) + (duration.isNull() ? "" : " &3for &e" + duration.format()));
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onDeath(PlayerDeathEvent event) {
		String deathString = event.getDeathMessage();
		Player player = event.getEntity();
		DeathMessages deathMessages = service.get(player);

		Component deathMessageRaw = event.deathMessage();
		if (deathMessageRaw == null)
			return;

		JsonBuilder output = new JsonBuilder("☠ ", NamedTextColor.RED);

		if (deathMessageRaw instanceof TranslatableComponent deathMessage) {
			List<Component> args = deathMessage.args().stream().map(arg -> handleArgument(deathMessages, arg)).toList();
			output.next(deathMessage.args(args));
		} else {
			Nexus.warn("Death message ("+deathMessageRaw.examinableName()+") is not translatable: " + AdventureUtils.asPlainText(deathMessageRaw));
			output.next(deathMessageRaw);
		}

		event.deathMessage(null);

		if (deathMessages.getBehavior() == Behavior.SHOWN) {
			Broadcast.ingame().sender(player).message(output).messageType(MessageType.CHAT).muteMenuItem(MuteMenuItem.DEATH_MESSAGES).send();

			if (WorldGroup.of(player) == WorldGroup.SURVIVAL)
				discord(deathString, player);
		} else if (deathMessages.getBehavior() == Behavior.LOCAL) {
			local(player, output);
		}
	}

	private Component handleArgument(DeathMessages player, Component component) {
		Component originalComponent = component;

		ShowEntity hover = getEntityHover(component);

		String playerName;
		// for some reason, these children differ depending on system. lexi's local test server triggers the
		// first if block, the BN server triggered the 2nd, unsure of which eden fires
		if (!component.children().isEmpty() && component.children().get(0) instanceof TextComponent textComponent)
			playerName = textComponent.content();
		else if (component instanceof TextComponent textComponent && !textComponent.content().isEmpty()) {
			playerName = textComponent.content();
			component = textComponent.content(""); // preserves hover text/click events but clears the content as we manually add it back it
		} else {
			return component;
		}

		Component finalComponent = cleanup(player, originalComponent, hover, playerName);

		return component.children(Collections.singletonList(finalComponent));
	}

	private void discord(String deathString, Player player) {
		// workaround for dumb Adventure bug (ProjectEdenGG/Issues#657)
		if (deathString == null)
			deathString = "☠ " + Nickname.of(player) + " died";
		else {
			deathString = ("☠ " + HEART_PATTERN.matcher(deathString).replaceAll("a mob"))
					.replace(" " + player.getName() + " ", " " + Nickname.of(player) + " ");

			if (player.getKiller() != null)
				deathString = deathString.replace(player.getKiller().getName(), Nickname.of(player.getKiller()));
		}
		Broadcast.discord().message(Discord.discordize(deathString)).send();
	}

	private void local(Player player, JsonBuilder output) {
		Chatter chatter = new ChatterService().get(player);
		for (Chatter recipient : StaticChannel.LOCAL.getChannel().getRecipients(chatter))
			if (!MuteMenuUser.hasMuted(recipient.getOnlinePlayer(), MuteMenuItem.DEATH_MESSAGES))
				recipient.sendMessage(player, output, MessageType.CHAT);
	}

	private Component cleanup(DeathMessages deathMessages, Component originalComponent, ShowEntity hover, String playerName) {
		Component finalComponent;
		if (HEART_PATTERN.matcher(playerName).matches() && hover != null)
			finalComponent = fixMcMMOHearts(hover);
		else if (hover == null || !hover.type().value().equalsIgnoreCase("player"))
			// ignore non-mcMMO, non-player entities
			finalComponent = originalComponent;
		else
			finalComponent = nickname(deathMessages, playerName);
		return finalComponent;
	}

	@Nullable
	private ShowEntity getEntityHover(Component component) {
		HoverEvent<?> _hoverEvent = component.hoverEvent();
		if (_hoverEvent != null && _hoverEvent.value() instanceof ShowEntity _hover)
			return _hover;
		else
			return null;
	}

	@NotNull
	private Component fixMcMMOHearts(ShowEntity hover) {
		Component failsafeComponent = Component.text("A Very Scary Mob");
		Component finalComponent = failsafeComponent;

		Entity entity = Bukkit.getEntity(hover.id());

		if (entity != null) {
			// get mcMMO's saved entity name
			if (entity.hasMetadata(mcMMO.customNameKey)) {
				String name = entity.getMetadata(mcMMO.customNameKey).get(0).asString();
				if (!name.isEmpty())
					finalComponent = new JsonBuilder().content(name).hover(HoverEvent.showEntity(hover.type(), hover.id(), finalComponent)).build();
			}

			// if that failed or was empty, get a translatable text component instead
			if (finalComponent == failsafeComponent) {
				String key = Bukkit.getUnsafe().getTranslationKey(entity.getType());
				if (key != null)
					finalComponent = Component.translatable(key);
			}
		}
		return finalComponent;
	}

	// display player (nick)names + colors
	@NotNull
	private Component nickname(DeathMessages deathMessages, String playerName) {
		JsonBuilder playerComponent = new JsonBuilder(playerName, NamedTextColor.YELLOW);

		if (playerName.equals(deathMessages.getName()))
			playerComponent.content(deathMessages.getNickname());
		else {
			try {
				playerComponent.content(Nickname.of(playerName));
			} catch (PlayerNotFoundException|InvalidInputException ignored) {}
		}
		return playerComponent.build();
	}

	@ConverterFor(CustomDeathMessage.class)
	CustomDeathMessage convertToCustomDeathMessage(String input) {
		final CustomDeathMessage config = DeathMessagesConfig.of(input);
		if (config == null)
			throw new InvalidInputException("Death message config from key &e" + input + " not found");
		return config;
	}

	@TabCompleterFor(CustomDeathMessage.class)
	List<String> tabCompleteCustomDeathMessage(String filter) {
		return config.getMessages().values().stream()
				.map(CustomDeathMessage::getKey)
				.filter(key -> key.toLowerCase().startsWith(filter.toLowerCase()))
				.toList();
	}

}
