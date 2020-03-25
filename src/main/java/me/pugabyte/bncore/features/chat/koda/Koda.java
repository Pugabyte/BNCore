package me.pugabyte.bncore.features.chat.koda;

import lombok.Getter;
import me.pugabyte.bncore.features.chat.Chat;
import me.pugabyte.bncore.features.discord.Discord;
import me.pugabyte.bncore.features.discord.DiscordId.Channel;
import me.pugabyte.bncore.utils.Tasks;
import org.bukkit.Bukkit;

import static me.pugabyte.bncore.utils.StringUtils.colorize;

public class Koda {
	@Getter
	private static String globalFormat = "&2[G] &5&oKodaBear &2&l> &f";
	@Getter
	private static String localFormat = "&e[L] &5&oKodaBear &e&l> &f";
	@Getter
	private static String dmFormat = "&3&l[&bPM&3&l] &eFrom &3KodaBear &b&l> &e";

	public static void reply(String message) {
		Tasks.wait(10, () -> say(message));
	}

	public static void say(String message) {
		Chat.broadcast(globalFormat + message);
		// TEMP
		Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(colorize(globalFormat + message)));
		Discord.send("<@&&f331634959351545857> **>** " + message, Channel.BRIDGE);
	}

}
