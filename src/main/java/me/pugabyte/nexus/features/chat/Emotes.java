package me.pugabyte.nexus.features.chat;

import lombok.Getter;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.chat.events.ChatEvent;
import me.pugabyte.nexus.models.emote.EmoteService;
import me.pugabyte.nexus.models.emote.EmoteUser;
import me.pugabyte.nexus.utils.RandomUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang.StringUtils.indexOfIgnoreCase;

public enum Emotes {
	CHECKMARK("&2✔", ":yes:"),
	X("&c✖", ":no:"),
	COFFEE("&6☕", ":coffee:"),
	TEA("&6☕", ":tea:"),
	ARROW("&2➜", ":arrow:"),
	SNOWFLAKE("&b❊", ":snowflake:"),
	SNOWMAN("&b☃", ":snowman:"),
	MUSIC1("&5♪", ":music1:"),
	MUSIC2("&5♫", ":music2:"),
	MUSIC3("&5♩", ":music3:"),
	MUSIC4("&5♬", ":music4:"),
	NOU("&2↙↗", ":nou:"),
	YAY("&6\\&eo&6/", "\\o/"),
	WAVING_RIGHT("&d( &dﾟ◡ﾟ)/", "o/"),
	WAVING_LEFT("&b\\(ﾟ◡ﾟ &b)", "\\o"),
	SALUTE1("&eo&6>", "o>"),
	SALUTE2("&eo&67", "o7"),
	SHRUG("&e¯\\_(ツ)_/¯", ":shrug:"),
	FACEPALM("&e(&6ლ&e‸－)", ":facepalm:"),
	TABLEFLIP("&c(╯°□°）╯&7︵ &6┻━┻", ":tableflip:"),
	ANGRY("&c┗(&4`&cO&4'&c)┛", ":angry:"),
	HAPPY("&e(&d✿&e◠‿◠)", ":happy:"),
	FLOWER("&d⚘", ":flower:"),
	TYPING("&6✎&8...", ":typing:"),
	CONFUSED("&e(&30&e.&3o&6?&e)", ":confused:"),
	SNAIL("&e@&a'&e-&a'", ":snail:"),
	DROPBEAR("&7ʕ&c•&7ᴥ&c•&7ʔ", ":dropbear:"),
	FIGHTME("&e(ง'-')ง", ":fightme:"),
	MONEY("&2[$&a100&2$]", ":money:"),
	BIRD("&3\\(&f˘&6▾&f˘&3)/", ":bird:"),
	FISH("&b<º))))><", "<><"),
	EYES("&b◔&e‿ &b◔", ":eyes:"),
	HEART("&4❤", ":heart:"),
	HEART_COLORED("❤", "<3",
			ChatColor.RED,
			ChatColor.LIGHT_PURPLE,
			ChatColor.DARK_PURPLE,
			ChatColor.BLACK,
			ChatColor.AQUA,
			ChatColor.GREEN);

	@Getter
	private final String emote;
	@Getter
	private final String key;
	@Getter
	private final List<ChatColor> colors = new ArrayList<>();

	Emotes(String emote, String key) {
		this.emote = emote;
		this.key = key;
	}

	Emotes(String emote, String key, ChatColor... colors) {
		this.emote = emote;
		this.key = key;
		this.colors.addAll(Arrays.asList(colors));
	}

	public static void process(ChatEvent event) {
		if (event.getChatter() == null) return;
		OfflinePlayer player = event.getChatter().getOfflinePlayer();
		if (!Nexus.getPerms().playerHas(null, player, "emoticons.use"))
			return;
		if (!((EmoteUser) new EmoteService().get(player)).isEnabled())
			return;

		event.setMessage(process(event.getMessage(), event.getChannel().getMessageColor()));
	}

	public static String process(String message) {
		return process(message, null);
	}

	public static String process(String message, ChatColor resetColor) {
		for (Emotes value : values())
			while (indexOfIgnoreCase(message, value.getKey()) > -1) {
				String result = value.getEmote();

				if (value.getColors().size() > 0)
					result = RandomUtils.randomElement(value.getColors()) + result;

				int index = indexOfIgnoreCase(message, value.getKey());
				message = message.substring(0, index) + result + (resetColor != null ? resetColor : "") + message.substring(index + value.getKey().length());
			}
		return message;
	}
}