package me.pugabyte.nexus.features.discord;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.dv8tion.jda.api.entities.Member;

public class DiscordId {

	@Getter
	@AllArgsConstructor
	public enum TextChannel {
		INFO("819817990249644032"),
		ANNOUNCEMENTS("133970047382061057"),
		CHANGELOG("819818214313689098"),
		BOOSTS("846814263754620938"),

		GENERAL("132680070480396288"),
		BOT_COMMANDS("223897739082203137"),

		BRIDGE("331277920729432065"),
		STAFF_BRIDGE("331842528854802443"),
		STAFF_OPERATORS("151881902813478914"),
		STAFF_ADMINS("133950052249894913"),

		STAFF_LOG("256866302176526336"),
		ADMIN_LOG("390751748261937152"),

		STAFF_SOCIAL_MEDIA("525363810811248666"),
		STAFF_PROMOTIONS("133949148251553792"),
		STAFF_WATCHLIST("134162415536308224"),
		STAFF_NICKNAME_QUEUE("824454559756713994"),

		ARCHIVED_OPS_BRIDGE("331846903266279424"),
		TEST("241774576822910976");

		private final String id;

		public net.dv8tion.jda.api.entities.TextChannel get() {
			return get(Bot.KODA);
		}

		public net.dv8tion.jda.api.entities.TextChannel get(Bot bot) {
			return bot.jda().getGuildById(DiscordId.Guild.PROJECT_EDEN.getId()).getTextChannelById(id);
		}

		public static TextChannel of(net.dv8tion.jda.api.entities.TextChannel textChannel) {
			return of(textChannel.getId());
		}

		public static TextChannel of(String id) {
			for (TextChannel textChannel : values())
				if (textChannel.getId().equals(id))
					return textChannel;

			return null;
		}
	}

	@Getter
	@AllArgsConstructor
	public enum VoiceChannel {
		MINIGAMES("133782271822921728"),
		RED("133785819432353792"),
		BLUE("133785864890351616"),
		GREEN("133785943772495872"),
		YELLOW("133785902680899585"),
		WHITE("360496040501051392");

		private final String id;
	}

	@Getter
	@AllArgsConstructor
	public enum User {
		PUGABYTE("115552359458799616"),
		POOGATEST("719574999673077912"),
		RELAY("352231755551473664"),
		KODA("223794142583455744"),
		UBER("85614143951892480");

		private final String id;

		public net.dv8tion.jda.api.entities.User get() {
			Member member = getMember();
			return member == null ? null : member.getUser();
		}

		public net.dv8tion.jda.api.entities.Member getMember() {
			return Discord.getGuild().retrieveMemberById(id).complete();
		}
	}

	@Getter
	@AllArgsConstructor
	public enum Guild {
		PROJECT_EDEN("132680070480396288");

		private final String id;
	}

	@Getter
	@AllArgsConstructor
	public enum Role {
		OWNER("133668441717604352"),
		ADMINS("133751307096817664"),
		OPERATORS("133668040553267208"),
		SENIOR_STAFF("230043171407527938"),
		MODERATORS("133686548959985664"),
		STAFF("230043287782817792"),
		ARCHITECTS("363273789527818242"),
		BUILDERS("194866089761570816"),
		VETERAN("244865244512518146"),
		SUPPORTER("269916102199476236"),
		VERIFIED("411658569444884495"),
		NERD("387029729401896980"),
		KODA("331634959351545857"),
		MINIGAME_NEWS("404031494453985282"),
		MOVIE_GOERS("583293370085015553"),
		CODING_LESSONS("847258306150268951"),
		BEAR_FAIR_PARTICIPANT("469666444888506378");

		private final String id;

		public net.dv8tion.jda.api.entities.Role get() {
			return Discord.getGuild().getRoleById(id);
		}

		public static Role of(net.dv8tion.jda.api.entities.Role role) {
			return of(role.getId());
		}

		public static Role of(String id) {
			for (Role role : values())
				if (role.getId().equals(id))
					return role;

			return null;
		}
	}
}
