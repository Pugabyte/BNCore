package me.pugabyte.nexus.models.punishments;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import me.pugabyte.nexus.framework.interfaces.ColoredAndNamed;
import me.pugabyte.nexus.models.nickname.Nickname;
import me.pugabyte.nexus.utils.JsonBuilder;
import me.pugabyte.nexus.utils.TimeUtils.Timespan;
import net.md_5.bungee.api.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.UUID;
import java.util.function.Function;

import static me.pugabyte.nexus.utils.StringUtils.camelCase;
import static me.pugabyte.nexus.utils.TimeUtils.shortDateTimeFormat;

@Getter
@AllArgsConstructor
public enum PunishmentType implements ColoredAndNamed {
	BAN("banned", ChatColor.DARK_RED, true, true, false, true) {
		@Override
		public void action(Punishment punishment) {
			kick(punishment);
		}

		@Override
		public String getDisconnectMessage(Punishment punishment) {
			return punishment.getReason();
		}
	},
	ALT_BAN("alt-banned", ChatColor.DARK_RED, true, true, false, true) {
		@Override
		public void action(Punishment punishment) {
			kick(punishment);
			// TODO look for alts, kick
		}

		@Override
		public String getDisconnectMessage(Punishment punishment) {
			return punishment.getReason();
		}
	},
	KICK("kicked", ChatColor.YELLOW, false, false, true, true) {
		@Override
		public void action(Punishment punishment) {
			kick(punishment);
		}

		@Override
		public String getDisconnectMessage(Punishment punishment) {
			return punishment.getReason();
		}
	},
	MUTE("muted", ChatColor.GOLD, true, true, false, false) {
		@Override
		public void action(Punishment punishment) {
			punishment.send("You have been muted"); // TODO
		}

		@Override
		public void onExpire(Punishment punishment) {
			punishment.send("Your mute has expired");
		}
	},
	WARN("warned", ChatColor.RED, false, false, false, false) {
		@Override
		public void action(Punishment punishment) {
			Punishments.of(punishment).tryShowWarns();
		}
	},
	FREEZE("froze", ChatColor.AQUA, false, true, true, true) {
		@Override
		public void action(Punishment punishment) {
			punishment.send("&cYou have been frozen! This likely means you are breaking a rule; please pay attention to staff in chat");
		}

		@Override
		public void onExpire(Punishment punishment) {
			punishment.send("&cYou have been unfrozen");
		}
	},
	WATCHLIST("watchlisted", ChatColor.LIGHT_PURPLE, false, true, true, true) {
		@Override
		public void action(Punishment punishment) {}
	};

	private final String pastTense;
	private final ChatColor chatColor;
	@Accessors(fluent = true)
	private final boolean hasTimespan;
	private final boolean onlyOneActive;
	private final boolean automaticallyReceived;
	private final boolean receivedIfAfk;

	public abstract void action(Punishment punishment);

	public void onExpire(Punishment punishment) {
	}

	public String getDisconnectMessage(Punishment punishment) {
		throw new UnsupportedOperationException("Punishment type " + camelCase(this) + " does not have a disconnect message");
	}

	void kick(Punishment punishment) {
		if (punishment.isOnline()) {
			punishment.getPlayer().kick(punishment.getDisconnectMessage());
			punishment.received();
		}
	}

	public JsonBuilder getHistoryDisplay(Punishment punishment) {
		int seconds = punishment.getSeconds();
		Function<UUID, String> staff = uuid -> "&f&#dddddd" + Nickname.of(uuid);

		JsonBuilder json = new JsonBuilder("- " + getColoredName() + " &fby " + staff.apply(punishment.getPunisher()) + " ")
				.group()
				.next("&f" + punishment.getTimeSince())
				.hover("&e" + shortDateTimeFormat(punishment.getTimestamp()))
				.group()
				.next(hasTimespan && punishment.isActive() ? " &c[Active]" : "");

		if (punishment.hasReason())
			json.newline().next("&7   Reason &f" + punishment.getReason());

		if (hasTimespan) {
			json.newline().next("&7   Duration &f" + (seconds > 0 ? Timespan.of(seconds).format() : "forever"));

			if (seconds > 0 && punishment.isActive())
				json.newline().next("&7   Time left &f" + punishment.getTimeLeft());
		}

		if (!automaticallyReceived && punishment.isActive())
			json.newline().next("&7   Received &f" + (punishment.hasBeenReceived() ? Timespan.of(punishment.getReceived()).format() + " ago" : "false"));

		if (punishment.hasBeenRemoved()) {
			json.newline().next("&7   Removed by " + staff.apply(punishment.getRemover()) + " ")
					.group()
					.next("&f" + punishment.getTimeSinceRemoved())
					.hover("&e" + shortDateTimeFormat(punishment.getRemoved()))
					.group();
		}
		if (punishment.hasBeenReplaced()) {
			Punishment replacedBy = Punishments.of(punishment.getUuid()).getById(punishment.getReplacedBy());
			if (replacedBy == null)
				json.newline().next("&7   Replaced by &cnull");
			else
				json.newline().next("&7   Replaced by " + staff.apply(replacedBy.getPunisher()) + " ")
						.group()
						.next("&f" + replacedBy.getTimeSince())
						.hover("&e" + shortDateTimeFormat(punishment.getTimestamp()))
						.group();
		}

		return json;
	}

	@Override
	public @NotNull Color getColor() {
		return chatColor.getColor();
	}

	@Override
	public @NotNull String getName() {
		return camelCase(pastTense);
	}

}