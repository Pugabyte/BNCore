package me.pugabyte.nexus.features.justice.misc;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import me.pugabyte.nexus.features.menus.MenuUtils.ConfirmationMenu;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.nickname.Nickname;
import me.pugabyte.nexus.models.punishments.Punishment;
import me.pugabyte.nexus.models.punishments.PunishmentType;
import me.pugabyte.nexus.models.punishments.Punishments;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@NoArgsConstructor
public abstract class _PunishmentCommand extends _JusticeCommand {

	public _PunishmentCommand(@NonNull CommandEvent event) {
		super(event);
	}

	protected void punish(List<Punishments> players) {
		punish(players, null);
	}

	protected void punish(List<Punishments> players, String input) {
		punish(players, input, false);
	}

	protected void punish(List<Punishments> players, String input, boolean now) {
		List<Punishments> toPunish = new ArrayList<>(players);
		AtomicReference<Runnable> loop = new AtomicReference<>();
		loop.set(() -> {
			if (toPunish.isEmpty())
				return;

			try {
				Punishments player = toPunish.remove(0);

				Runnable punish = () -> player.add(Punishment.ofType(getType())
						.punisher(uuid())
						.input(input)
						.now(now));

				Optional<Punishment> cooldown = player.getCooldown(uuid());
				if (cooldown.isPresent())
					ConfirmationMenu.builder()
							.title(camelCase(getType()) + " " + player.getNickname())
							.confirmLore("&c" + player.getNickname() + " was recently " + System.lineSeparator()
									+ "&c" + cooldown.get().getType().getColoredName().toLowerCase() + " by " + Nickname.of(cooldown.get().getPunisher()))
							.onConfirm($ -> punish.run())
							.onFinally(e -> loop.get().run())
							.open(player());
				else {
					punish.run();
					loop.get().run();
				}
			} catch (Exception ex) {
				event.handleException(ex);
			}
		});

		loop.get().run();
	}

	protected void deactivate(List<Punishments> players) {
		for (Punishments player : players) {
			try {
				Optional<Punishment> punishment = player.getMostRecentActive(getType());
				if (punishment.isPresent())
					punishment.get().deactivate(uuid());
				else
					error(player.getNickname() + " is not " + getType().getPastTense());
			} catch (Exception ex) {
				event.handleException(ex);
			}
		}
	}

	abstract protected PunishmentType getType();

}
