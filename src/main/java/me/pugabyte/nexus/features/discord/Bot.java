package me.pugabyte.nexus.features.discord;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.vdurmont.emoji.EmojiManager;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.chat.bridge.DiscordBridgeListener;
import me.pugabyte.nexus.features.commands.NicknameCommand.NicknameApprovalListener;
import me.pugabyte.nexus.features.discord.DiscordId.User;
import me.pugabyte.nexus.features.discord.commands.TwitterDiscordCommand.TweetApprovalListener;
import me.pugabyte.nexus.utils.Tasks;
import me.pugabyte.nexus.utils.Utils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.reflections.Reflections;

import java.util.EnumSet;

import static com.google.common.base.Strings.isNullOrEmpty;

public enum Bot {

	KODA {
		@Override
		JDABuilder build() {
			return JDABuilder.createDefault(getToken())
					.addEventListeners(new DiscordListener(), new TweetApprovalListener(), new NicknameApprovalListener())
					// .addEventListeners(new DiscordCaptchaListener())
					.addEventListeners(getCommands().build());
		}

		@Override
		public String getId() {
			return User.KODA.getId();
		}
	},

	RELAY {
		@Override
		JDABuilder build() {
			return JDABuilder.createDefault(getToken())
					.addEventListeners(new DiscordBridgeListener())
					.addEventListeners(getCommands().setStatus(OnlineStatus.INVISIBLE).build());
		}

		@Override
		public String getId() {
			return User.RELAY.getId();
		}
	};

	@Getter
	@Accessors(fluent = true)
	private JDA jda;

	abstract JDABuilder build();

	@SneakyThrows
	void connect() {
		if (this.jda == null && !isNullOrEmpty(getToken())) {
			final JDA jda = build()
					.enableIntents(EnumSet.allOf(GatewayIntent.class))
					.setMemberCachePolicy(MemberCachePolicy.ALL)
					.build()
					.awaitReady();

			if (jda == null) {
				Nexus.log("Could not connect " + name() + " to Discord");
				return;
			}

			Tasks.sync(() -> {
				if (this.jda == null)
					this.jda = jda;
				else
					jda.shutdown();
			});
		}
	}

	void shutdown() {
		if (jda != null) {
			jda.cancelRequests();
			jda.shutdown();
			jda = null;
		}
	}

	protected String getToken() {
		return Nexus.getInstance().getConfig().getString("tokens.discord." + name().toLowerCase(), "");
	}

	public abstract String getId();

	@SneakyThrows
	protected CommandClientBuilder getCommands() {
		CommandClientBuilder commands = new CommandClientBuilder()
				.setPrefix("/")
				.setAlternativePrefix("!")
				.setOwnerId(User.PUGABYTE.getId())
				.setEmojis(EmojiManager.getForAlias("white_check_mark").getUnicode(), EmojiManager.getForAlias("warning").getUnicode(), EmojiManager.getForAlias("x").getUnicode())
				.setActivity(Activity.playing("Minecraft"));

		Reflections reflections = new Reflections(getClass().getPackage().getName());
		for (Class<? extends Command> command : reflections.getSubTypesOf(Command.class))
			if (Utils.canEnable(command))
				for (Class<? extends Command> superclass : Utils.getSuperclasses(command)) {
					HandledBy handledBy = superclass.getAnnotation(HandledBy.class);
					if (handledBy != null && handledBy.value() == this) {
						commands.addCommand(command.newInstance());
						break;
					}
				}
		return commands;
	}

}
