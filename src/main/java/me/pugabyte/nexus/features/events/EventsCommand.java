package me.pugabyte.nexus.features.events;

import eden.utils.Env;
import lombok.NonNull;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.events.store.EventStoreListener;
import me.pugabyte.nexus.features.events.store.providers.EventStoreProvider;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Aliases;
import me.pugabyte.nexus.framework.commands.models.annotations.Arg;
import me.pugabyte.nexus.framework.commands.models.annotations.Async;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.eventuser.EventUser;
import me.pugabyte.nexus.models.eventuser.EventUserService;

@Aliases("event")
public class EventsCommand extends CustomCommand {
	private final EventUserService service = new EventUserService();
	private EventUser user;

	public EventsCommand(@NonNull CommandEvent event) {
		super(event);
		if (isPlayer())
			user = service.get(player());
	}

	static {
		new EventStoreListener();
	}

	@Path("store")
	void store() {
		if (Nexus.getEnv() == Env.PROD && !isStaff())
			error("Coming Soon™");

		new EventStoreProvider().open(player());
	}

	// Token commands

	private String plural(int tokens) {
		return tokens + plural(" token", tokens);
	}

	@Path("tokens [player]")
	public void tokens(@Arg("self") EventUser user) {
		if (isSelf(user)) {
			send(PREFIX + "&3Current balance: &e" + plural(user.getTokens()));
			line();
			send("&3Event tokens are currency earned by participating in server events");
			send("&3You can spend them at the &c/event store &3in exchange for unique rewards");
		} else
			send(PREFIX + "&3" + user.getNickname() + "'s current balance: &e" + plural(user.getTokens()));
	}

	@Async
	@Path("tokens top [page]")
	public void tokensTop(@Arg("1") int page) {
		paginate(service.getTopTokens(), (user, index) -> json("&3" + (index + 1) + " &e" + user.getNickname() + " &7- " + user.getTokens()), "/event tokens top", page);
	}

	/* TODO
	@Path("tokens daily [player]")
	public void tokensDaily(@Arg("self") EventUser user) {
		if (player().equals(user.getOfflinePlayer()))
			send(PREFIX + "&3Daily tokens:");
		else
			send(PREFIX + "&3" + user.getOfflinePlayer().getName() + "'s Daily tokens:");

		for (BFtokensource tokensource : BFtokensource.values()) {
			Map<LocalDate, Integer> dailyMap = user.getTokensReceivedToday().get(tokensource);
			int tokens = 0;
			if (dailyMap != null)
				tokens = dailyMap.getOrDefault(LocalDate.now(), 0);

			int dailyMax = EventUser.DAILY_SOURCE_MAX;
			String sourceColor = tokens == dailyMax ? "&a" : "&3";
			String sourceName = StringUtils.camelCase(tokensource.name());
			send(" " + sourceColor + sourceName + " &7- &e" + tokens + "&3/&e" + dailyMax);
		}
	}
	*/

	@Path("tokens pay <player> <tokens>")
	public void tokensPay(EventUser toUser, int tokens) {
		EventUser fromUser = service.get(player());
		if (isSelf(toUser))
			error("You cannot pay yourself");

		fromUser.takeTokens(tokens);
		toUser.giveTokens(tokens);

		fromUser.sendMessage(PREFIX + "&e" + tokens + " event tokens &3have been sent to &e" + toUser.getNickname());
		toUser.sendMessage(PREFIX + "&e" + tokens + " event tokens &3have been received from &e" + fromUser.getNickname());

		service.save(fromUser);
		service.save(toUser);
	}

	@Path("tokens give <player> <tokens>")
	@Permission("group.admin")
	public void tokensGive(EventUser user, int tokens) {
		user.giveTokens(tokens);
		service.save(user);
		send(PREFIX + "&e" + plural(tokens) + " &3given to &e" + user.getNickname());
	}

	@Path("tokens take <player> <tokens>")
	@Permission("group.admin")
	public void tokensTake(EventUser user, int tokens) {
		user.takeTokens(tokens);
		service.save(user);
		send(PREFIX + "&e" + plural(tokens) + " &3taken from &e" + user.getNickname());
	}

	@Path("tokens set <player> <tokens>")
	@Permission("group.admin")
	public void tokensSet(EventUser user, int tokens) {
		user.setTokens(tokens);
		service.save(user);
		send(PREFIX + "&3Set &e" + user.getNickname() + "&3's balance to &e" + plural(tokens));
	}

	@Path("tokens reset <player>")
	@Permission("group.admin")
	public void tokensReset(EventUser user) {
		user.setTokens(0);
		user.getTokensReceivedToday().clear();
		service.save(user);
	}

	// Database commands

}
