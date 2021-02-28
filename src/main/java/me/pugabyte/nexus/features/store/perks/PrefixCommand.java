package me.pugabyte.nexus.features.store.perks;

import me.pugabyte.nexus.features.chat.Emotes;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.nerd.Nerd;
import me.pugabyte.nexus.models.nerd.NerdService;
import me.pugabyte.nexus.utils.StringUtils;
import me.pugabyte.nexus.utils.StringUtils.Gradient;
import me.pugabyte.nexus.utils.StringUtils.Rainbow;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.OfflinePlayer;

import java.util.regex.Matcher;

import static me.pugabyte.nexus.utils.StringUtils.decolorize;
import static me.pugabyte.nexus.utils.StringUtils.stripColor;
import static me.pugabyte.nexus.utils.StringUtils.stripFormat;

public class PrefixCommand extends CustomCommand {
	private final NerdService service = new NerdService();
	private Nerd nerd;

	public PrefixCommand(CommandEvent event) {
		super(event);
		if (isPlayer())
			nerd = service.get(player());
	}

	@Path("checkmark")
	@Permission("donated")
	void checkmark() {
		nerd.setCheckmark(!nerd.isCheckmark());
		service.save(nerd);
		send(PREFIX + "Check mark " + (nerd.isCheckmark() ? "enabled" : "disabled"));
	}

	@Path("reset")
	@Permission("set.my.prefix")
	void reset() {
		nerd.setPrefix(null);
		service.save(nerd);
		send(PREFIX + "Reset prefix");
	}

	@Path("expire <player>")
	void expire(OfflinePlayer player) {
		console();
		nerd = service.get(player);
		nerd.setPrefix(null);
		service.save(nerd);
		send(PREFIX + "Reset prefix");
	}

	@Path("<prefix...>")
	@Permission("set.my.prefix")
	void prefix(String input) {
		if (player().hasPermission("emoticons.use"))
			input = Emotes.process(input);

		if (stripColor(input).length() > 10)
			error("Your prefix cannot be more than 10 characters");

		input = stripFormat(input);

		nerd.setPrefix(input);
		service.save(nerd);
		send(PREFIX + "Your prefix has been set to &8&l[&f" + input + "&8&l]");
	}

	@Path("gradient <color1> <color2> <prefix...>")
	@Permission("set.my.prefix")
	void gradient(ChatColor color1, ChatColor color2, String input) {
		prefix(Gradient.of(color1, color2).apply(input));
	}

	@Path("rainbow <prefix...>")
	@Permission("set.my.prefix")
	void rainbow(String input) {
		prefix(Rainbow.apply(input));
	}

	@Path("copy")
	void copy() {
		String prefix = nerd.getPrefix();

		if (isNullOrEmpty(prefix))
			prefix = new Nerd(player()).getRank().getPrefix();

		if (isNullOrEmpty(prefix))
			error("You do not have a prefix");

		String original = prefix;

		while (true) {
			Matcher matcher = StringUtils.getHexColorizedPattern().matcher(prefix);
			if (!matcher.find()) break;

			String group = matcher.group();
			prefix = prefix.replace(group, group.replaceAll(StringUtils.getColorChar() + "x", "&#").replaceAll(StringUtils.getColorChar(), ""));
		}

		send(json(PREFIX + "Click here to copy your current prefix: &f" + original).copy(decolorize(prefix)).hover("&7Click to copy"));
	}

}
