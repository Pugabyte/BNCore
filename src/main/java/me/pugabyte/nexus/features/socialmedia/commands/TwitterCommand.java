package me.pugabyte.nexus.features.socialmedia.commands;

import eden.utils.Env;
import eden.utils.TimeUtils.Time;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.discord.Discord;
import me.pugabyte.nexus.features.discord.DiscordId.TextChannel;
import me.pugabyte.nexus.features.socialmedia.SocialMedia;
import me.pugabyte.nexus.features.socialmedia.SocialMedia.EdenSocialMediaSite;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Async;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.socialmedia.TwitterData;
import me.pugabyte.nexus.models.socialmedia.TwitterService;
import me.pugabyte.nexus.utils.Tasks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import twitter4j.Query;
import twitter4j.Status;

import java.util.List;

public class TwitterCommand extends CustomCommand {

	public TwitterCommand(CommandEvent event) {
		super(event);
	}

	@Path
	void run() {
		send(json().next("&e" + EdenSocialMediaSite.TWITTER.getUrl()));
	}

	@Async
	@Path("lookForNewTweets")
	void lookForNewTweets() {
		lookForNewTweets0();
	}

	static {
		if (Nexus.getEnv() == Env.PROD)
			Tasks.repeatAsync(Time.MINUTE, Time.MINUTE.x(5), TwitterCommand::lookForNewTweets0);
	}

	private static void lookForNewTweets0() {
		try {
			TwitterService service = new TwitterService();
			TwitterData data = service.get0();

			List<Status> tweets = SocialMedia.getTwitter().search().search(new Query("from:ProjectEdenGG")).getTweets();
			for (Status tweet : tweets) {
				if (data.getKnownTweets().contains(tweet.getId()))
					continue;

				EmbedBuilder embed = new EmbedBuilder().setTitle("New tweet! <:twitter:829474002586173460>")
						.appendDescription(tweet.getText() + System.lineSeparator() + System.lineSeparator() + "[View on Twitter](" + SocialMedia.getUrl(tweet) + ")");

				MessageBuilder content = new MessageBuilder().setEmbed(embed.build());

				Discord.send(content, TextChannel.GENERAL);
				data.getKnownTweets().add(tweet.getId());
				service.save(data);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}