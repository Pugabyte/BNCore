package me.pugabyte.nexus.models.discord;

import dev.morphia.annotations.Converters;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import eden.mongodb.serializers.UUIDConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.commands.PronounsCommand;
import me.pugabyte.nexus.features.discord.Bot;
import me.pugabyte.nexus.features.discord.Discord;
import me.pugabyte.nexus.models.PlayerOwnedObject;
import me.pugabyte.nexus.models.nickname.Nickname;
import me.pugabyte.nexus.utils.PlayerUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static me.pugabyte.nexus.features.discord.Discord.discordize;
import static me.pugabyte.nexus.features.discord.Discord.getGuild;

@Data
@Builder
@Entity("discord_user")
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
@Converters(UUIDConverter.class)
public class DiscordUser implements PlayerOwnedObject {
	@Id
	@NonNull
	private UUID uuid;
	private String userId;
	private String roleId;

	public DiscordUser(@NonNull UUID uuid, String userId) {
		this.uuid = uuid;
		this.userId = userId;
	}

	public String getBridgeName() {
		OfflinePlayer player = PlayerUtils.getPlayer(uuid);
		String name = "**" + discordize(Nickname.of(player)) + "**";
		if (roleId != null)
			name = "<@&&f" + roleId + ">";
		return name;
	}

	/**
	 * For DiscordUser objects, {@link #getIngameName()} or {@link #getDiscordName()} should be
	 * used instead. As of writing, this returns the same value as the former.
	 */
	@Override
	@Deprecated
	public @NotNull String getName() {
		return PlayerOwnedObject.super.getName();
	}

	public String getIngameName() {
		return getName();
	}

	public @NotNull String getDiscordName() {
		return Discord.getName(userId);
	}

	@Nullable
	public String getDiscrim() {
		User user = getUser();
		return user == null ? null : user.getDiscriminator();
	}

	public String getNameAndDiscrim() {
		return getDiscordName() + "#" + getDiscrim();
	}

	@Nullable
	public User getUser() {
		try {
			return Bot.RELAY.jda().retrieveUserById(userId).complete();
		} catch (ErrorResponseException exc) {
			Nexus.log("Failed to get Discord user for " + userId + ": " + exc.getErrorResponse().getMeaning());
			return null;
		}
	}

	@Nullable
	public Member getMember() {
		if (userId == null) return null;
		Guild guild = getGuild();
		if (guild == null) return null;
		try {
			return guild.retrieveMemberById(userId).complete();
		} catch (ErrorResponseException exc) {
			Nexus.log("Failed to get Discord member for " + userId + ": " + exc.getErrorResponse().getMeaning());
			return null;
		}
	}

	public void updatePronouns(Set<String> pronouns) {
		Member member = getMember();
		if (member == null) return;
		Guild guild = getGuild();
		if (guild == null) return;
		List<Role> currentRoles = member.getRoles().stream().filter(role -> PronounsCommand.PRONOUN_WHITELIST.contains(role.getName())).collect(Collectors.toList());
		List<Role> expectedRoles = guild.getRoles().stream().filter(role -> PronounsCommand.PRONOUN_WHITELIST.contains(role.getName()) && pronouns.contains(role.getName())).collect(Collectors.toList());
		pronouns.forEach(pronoun -> {
			if (PronounsCommand.PRONOUN_WHITELIST.contains(pronoun) && expectedRoles.stream().noneMatch(role -> role.getName().equals(pronoun)))
				expectedRoles.add(guild.createRole().setName(pronoun).setPermissions(0L).complete());
		});
		List<Role> addRoles = expectedRoles.stream().filter(role -> !currentRoles.contains(role)).collect(Collectors.toList());
		List<Role> removeRoles = currentRoles.stream().filter(role -> !expectedRoles.contains(role)).collect(Collectors.toList());
		guild.modifyMemberRoles(member, addRoles, removeRoles).complete();
	}

}
