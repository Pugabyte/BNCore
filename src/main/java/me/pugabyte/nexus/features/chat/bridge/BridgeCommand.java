package me.pugabyte.nexus.features.chat.bridge;

import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.SneakyThrows;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.discord.Bot;
import me.pugabyte.nexus.features.discord.Discord;
import me.pugabyte.nexus.features.discord.DiscordId;
import me.pugabyte.nexus.features.discord.DiscordId.TextChannel;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Arg;
import me.pugabyte.nexus.framework.commands.models.annotations.Async;
import me.pugabyte.nexus.framework.commands.models.annotations.Confirm;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.discord.DiscordUser;
import me.pugabyte.nexus.models.discord.DiscordUserService;
import me.pugabyte.nexus.models.nerd.Nerd;
import me.pugabyte.nexus.models.nerd.Rank;
import me.pugabyte.nexus.utils.JsonBuilder;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.Utils;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.utils.TimeUtil;
import org.apache.commons.io.FileUtils;
import org.bukkit.OfflinePlayer;

import java.awt.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;

@Permission("group.admin")
public class BridgeCommand extends CustomCommand {
	private final DiscordUserService service;

	public BridgeCommand(CommandEvent event) {
		super(event);
		service = new DiscordUserService();
		if (isCommandEvent())
			if (Discord.getGuild() == null)
				error("Not connected to Discord");
	}

	@Path("get <player>")
	void get(@Arg("self") DiscordUser user) {
		send("User: " + user);
	}

	@Path("set <player> <roleId>")
	void set(DiscordUser user, String roleId) {
		user.setRoleId(roleId);
		service.save(user);
		RoleManager.update(user);
		send(user.getIngameName() + "'s bridge role updated");
	}

	@Path("countRoles")
	void countRoles() {
		send(PREFIX + "Found " + Discord.getGuild().getRoles().size() + " roles");
	}

	@Path("getRoleColors")
	void getRoleColors() {
		List<DiscordId.Role> roles = Arrays.asList(DiscordId.Role.OWNER, DiscordId.Role.ADMINS, DiscordId.Role.OPERATORS,
				DiscordId.Role.MODERATORS, DiscordId.Role.ARCHITECTS, DiscordId.Role.BUILDERS, DiscordId.Role.VETERAN);
		roles.forEach(role -> {
			Color color = Discord.getGuild().getRoleById(role.getId()).getColor();
			Nexus.log(role.name() + " #" + Integer.toHexString(color.getRGB()).substring(2));
		});
	}

	@Async
	@Path("updateRoleColors <rank>")
	void updateRoleColors(Rank rank) {
		int updated = 0;
		for (DiscordUser user : service.<DiscordUser>getAll()) {
			if (user.getRoleId() == null || user.getUuid() == null)
				continue;

			Rank playerRank = Rank.of(user.getOfflinePlayer());
			if (playerRank != rank)
				continue;

			net.dv8tion.jda.api.entities.Role role = Discord.getGuild().getRoleById(user.getRoleId());
			if (role == null)
				continue;

			role.getManager().setColor(rank.getDiscordColor()).queue();
			++updated;
		}

		send("Updated " + updated + " roles");
	}

	@Async
	@Path("getFirstBridgeRolePosition")
	void getFirstBridgeRolePosition() {
		int position = Discord.getGuild().getRoleById("331279736691228676").getPosition();
		send(json("Position: " + position).copy("" + position));
	}

	@Async
	@Path("setMentionableFalse [test]")
	void setMentionableFalse(boolean test) {
		int startingPosition = 186;
		int count = 0;
		for (Role role : Discord.getGuild().getRoles()) {
			if (role.getPosition() <= startingPosition && role.isMentionable()) {
				++count;
				if (!test)
					role.getManager().setMentionable(false).queue();
			}
		}

		send(PREFIX + (test ? "Will update" : "Updated") + " " + count + " roles");
	}

	private static BridgeArchive archive;
	private static BridgeChannel loadedChannel;

	@Getter
	@AllArgsConstructor
	private enum BridgeChannel {
		BRIDGE(TextChannel.BRIDGE),
		STAFF_BRIDGE(TextChannel.BRIDGE);

		private final TextChannel textChannel;
	}

	@Async
	@SneakyThrows
	@Path("archive load <channel>")
	void archive_load(BridgeChannel channel) {
		if (archive != null)
			error("Archive is already loaded. &3Use &c/bridge archive reload &3to reload");

		loadedChannel = channel;
		String data = FileUtils.readFileToString(Nexus.getFile(channel.name().toLowerCase().replace("_", "-") + "-role-archive.json"));
		archive = new Gson().fromJson(data, BridgeArchive.class);
		send(PREFIX + "Loaded " + archive.getRoleMap().size() + " roles from the archive");
	}

	@Async
	@Path("archive reload <channel>")
	void archive_reload(BridgeChannel channel) {
		archive = null;
		archive_load(channel);
	}

	@Async
	@Path("archive leastUsedRoles [page]")
	void archive_leastUsedRoles(@Arg("1") int page) {
		if (archive == null) error("No archive loaded");

		BiFunction<String, String, JsonBuilder> formatter = (roleId, index) -> {
			Role role = Discord.getGuild().getRoleById(roleId);
			DiscordUser user = new DiscordUserService().getFromRoleId(roleId);
			boolean tied = user != null;
			String name = user == null ? role == null ? roleId : role.getName() : user.getIngameName();
			int size = archive.getRoleMap().get(roleId).size();
			return json("&3" + index + " " + (tied ? "&e" : "&c") + name + " &7- " + size + " messages")
					.insert(roleId)
					.hover("Shift+Click to insert");
		};

		paginate(new ArrayList<>(Utils.sortByValue(new HashMap<String, Integer>() {{
			archive.getRoleMap().forEach((k, v) -> put(k, v.size()));
		}}).keySet()), formatter, "/bridge archive leastUsedRoles", page);
	}

	@Async
	@Path("archive editMessages removeReference <roleId> [name]")
	void archive_editMessages_removeReference(String roleId, String name) {
		if (archive == null) error("No archive loaded");

		DiscordUser user = new DiscordUserService().getFromRoleId(roleId);
		if (name == null)
			if (user == null)
				error("Role is not tied to a user, you must provide the name to use");
			else
				name = Nerd.of(user).getName();

		final String username = "**" + name + "**";

		List<String> messageIds = archive.getRoleMap().get(roleId);
		send(PREFIX + "Editing " + messageIds.size() + " messages for user " + name);
		for (String messageId : messageIds)
			updateRoleMention(roleId, username, messageId);

		send(json(PREFIX + "Done. Click here to remove the role").command("/bridge archive deleteRole " + roleId));
	}

	@Async
	@Path("archive editMessages updateReference <oldRoleId> <newRoleId>")
	void archive_editMessages_updateReference(String oldRoleId, String newRoleId) {
		if (archive == null) error("No archive loaded");

		List<String> messageIds = archive.getRoleMap().get(oldRoleId);
		send(PREFIX + "Editing " + messageIds.size() + " messages");
		for (String messageId : messageIds)
			updateRoleMention(oldRoleId, "<@&" + newRoleId + ">", messageId);

		send(json(PREFIX + "Done. Click here to remove the old role").command("/bridge archive deleteRole " + oldRoleId));
	}

	@Async
	@Confirm
	@Path("archive deleteRole <roleId>")
	void archive_deleteRole(String roleId) {
		Discord.getGuild().getRoleById(roleId).delete().queue(success -> send(PREFIX + "Deleted"), this::rethrow);
	}

	@Async
	@Path("archive findDuplicateRoles [page]")
	void archive_findDuplicateRoles(@Arg("1") int page) {
		Map<UUID, List<String>> duplicates = new HashMap<UUID, List<String>>() {{
			for (String roleId : archive.getRoleMap().keySet()) {
				Role role = Discord.getGuild().getRoleById(roleId);
				DiscordUser user = new DiscordUserService().getFromRoleId(roleId);
				String name = user == null ? role == null ? null : role.getName() : user.getIngameName();
				if (!isNullOrEmpty(name)) {
					UUID uuid = PlayerUtils.getPlayer(name).getUniqueId();
					List<String> roleIds = getOrDefault(uuid, new ArrayList<>());
					roleIds.add(roleId);
					put(uuid, roleIds);
				}
			}
		}};

		for (UUID uuid : new HashSet<>(duplicates.keySet())) {
			if (duplicates.get(uuid).size() == 1)
				duplicates.remove(uuid);
		}

		BiFunction<UUID, String, JsonBuilder> formatter = (uuid, index) -> {
			OfflinePlayer player = PlayerUtils.getPlayer(uuid);
			int size = duplicates.get(uuid).size();
			JsonBuilder json = json("&3" + index + " &e" + player.getName() + " &7- " + size + " roles")
					.newline();

			for (String roleId : duplicates.get(uuid))
				json.next("    &7" + roleId + " - " + archive.getRoleMap().get(roleId).size() + " messages");

			return json;
		};

		paginate(new ArrayList<>(Utils.sortByValue(new HashMap<UUID, Integer>() {{
			duplicates.forEach((k, v) -> put(k, v.size()));
		}}).keySet()), formatter, "/bridge archive findDuplicateRoles", page);
	}

	private static final OffsetDateTime grandfather = TimeUtil.getTimeCreated(Long.parseLong("352232748955729930"));

	private void executeOnMessage(String messageId, Consumer<Message> consumer) {
		Bot botGuess = TimeUtil.getTimeCreated(Long.parseLong(messageId)).isAfter(grandfather) ? Bot.RELAY : Bot.KODA;
		Bot otherBot = botGuess == Bot.KODA ? Bot.RELAY : Bot.KODA;

		loadedChannel.getTextChannel().get(botGuess).retrieveMessageById(messageId).queue(message -> {
			if (message.getAuthor().getId().equals(botGuess.getId()))
				consumer.accept(message);
			else
				loadedChannel.getTextChannel().get(otherBot).retrieveMessageById(messageId).queue(consumer);
		});
	}

	private void updateRoleMention(String oldRoleId, String newRoleId, String messageId) {
		executeOnMessage(messageId, message -> {
			String oldContent = message.getContentRaw();
			String newContent = oldContent.replaceFirst("<@&" + oldRoleId + ">", newRoleId);
			if (oldContent.equals(newContent))
				return;

			message.editMessage(new MessageBuilder(message)
					.setContent(newContent)
					.build()
			).queue();
		});
	}

	@Data
	private static class BridgeArchive {
		private Map<String, List<String>> roleMap;
	}

}