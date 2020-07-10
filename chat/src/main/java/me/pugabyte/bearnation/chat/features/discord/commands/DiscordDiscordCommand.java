package me.pugabyte.bearnation.chat.features.discord.commands;

import com.google.common.base.Strings;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import me.pugabyte.bearnation.api.framework.exceptions.BNException;
import me.pugabyte.bearnation.api.framework.exceptions.postconfigured.InvalidInputException;
import me.pugabyte.bearnation.api.framework.exceptions.preconfigured.NoPermissionException;
import me.pugabyte.bearnation.api.models.setting.Setting;
import me.pugabyte.bearnation.api.models.setting.SettingService;
import me.pugabyte.bearnation.api.utils.Utils;
import me.pugabyte.bearnation.chat.Chat;
import me.pugabyte.bearnation.chat.features.Koda;
import me.pugabyte.bearnation.chat.features.discord.Bot;
import me.pugabyte.bearnation.chat.features.discord.Bot.HandledBy;
import me.pugabyte.bearnation.chat.features.discord.Discord;
import me.pugabyte.bearnation.chat.features.discord.DiscordId.User;
import me.pugabyte.bearnation.chat.models.discord.DiscordService;
import me.pugabyte.bearnation.chat.models.discord.DiscordUser;
import net.dv8tion.jda.api.Permission;
import org.apache.commons.lang.RandomStringUtils;
import org.bukkit.OfflinePlayer;

import static me.pugabyte.bearnation.api.utils.StringUtils.stripColor;

@HandledBy(Bot.KODA)
public class DiscordDiscordCommand extends Command {

	public DiscordDiscordCommand() {
		this.name = "discord";
		this.guildOnly = false;
	}

	protected void execute(CommandEvent event) {
		Chat.tasks().async(() -> {
			try {
				String[] args = event.getArgs().split(" ");
				if (args.length == 0)
					throw new InvalidInputException("Correct usage: `/discord link <name>`");

				switch (args[0].toLowerCase()) {
					case "lockdown":
						if (!event.getMember().hasPermission(Permission.KICK_MEMBERS))
							throw new NoPermissionException();

						SettingService service = new SettingService(Chat.inst());
						Setting setting = service.get("discord", "lockdown");
						setting.setBoolean(!setting.getBoolean());
						service.save(setting);

						event.reply("Discord lockdown " + (setting.getBoolean() ? "enabled, new members will be automatically kicked" : "disabled"));

						break;
					case "link":
						if (args.length < 2)
							throw new InvalidInputException("Correct usage: `/discord link <name>`");
						DiscordUser author = new DiscordService(Chat.inst()).getFromUserId(event.getAuthor().getId());

						OfflinePlayer player = Utils.getPlayer(args[1]);
						DiscordUser fromInput = new DiscordService(Chat.inst()).get(player);

						if (author != null && !Strings.isNullOrEmpty(author.getUuid()))
							// Author already linked
							if (!Strings.isNullOrEmpty(fromInput.getUserId()))
								if (author.getUserId().equals(fromInput.getUserId()))
									throw new InvalidInputException("You are already linked to that minecraft account");
								else
									throw new InvalidInputException("That minecraft account is already linked to a different discord account. Type `/discord unlink` ingame to remove the link.");
							else
								throw new InvalidInputException("You are already linked to a different minecraft account. Use `/discord unlink` ingame to remove the link.");
						if (!Strings.isNullOrEmpty(fromInput.getUserId()))
							// Provided name is already linked
							if (fromInput.getUserId().equals(event.getAuthor().getId()))
								throw new InvalidInputException("This should never happen <@" + User.PUGABYTE.getId() + ">"); // Lookup by user id failed?
							else
								throw new InvalidInputException("That minecraft account is already linked to a different discord account. Type `/discord unlink` ingame to remove the link.");

						String code = RandomStringUtils.randomAlphabetic(6);
						Discord.getCodes().put(code, new DiscordUser(player.getUniqueId().toString(), event.getAuthor().getId()));
						String name = Discord.getName(event.getMember().getId());
						Koda.console("Generated key " + code + " for " + name);

						event.getAuthor().openPrivateChannel().complete().sendMessage("Hey there " + name + "! I've successfully found that minecraft account. " +
								"Please copy and paste the following command into minecraft to confirm the link. (You can use `ctrl+v` or `cmd+v`) " +
								"```/discord link " + code + "```").queue();
						if (event.getMessage().getChannel().getType().isGuild())
							event.reply(event.getAuthor().getAsMention() + " Check your direct messages with " + Bot.KODA.jda().getSelfUser().getAsMention() + " for a confirmation code! (top left of the screen)");
						break;
				}
			} catch (Exception ex) {
				event.reply(stripColor(ex.getMessage()));
				if (!(ex instanceof BNException))
					ex.printStackTrace();
			}
		});

	}

}
