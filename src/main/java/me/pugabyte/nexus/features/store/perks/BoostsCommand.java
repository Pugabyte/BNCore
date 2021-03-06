package me.pugabyte.nexus.features.store.perks;

import com.destroystokyo.paper.event.player.PlayerPickupExperienceEvent;
import com.gmail.nossr50.events.experience.McMMOPlayerXpGainEvent;
import eden.utils.TimeUtils.Time;
import eden.utils.TimeUtils.Timespan;
import eden.utils.TimeUtils.Timespan.FormatType;
import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import me.pugabyte.nexus.features.menus.MenuUtils;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Aliases;
import me.pugabyte.nexus.framework.commands.models.annotations.Arg;
import me.pugabyte.nexus.framework.commands.models.annotations.Confirm;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.annotations.Switch;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.boost.BoostConfig;
import me.pugabyte.nexus.models.boost.BoostConfig.DiscordHandler;
import me.pugabyte.nexus.models.boost.BoostConfigService;
import me.pugabyte.nexus.models.boost.Boostable;
import me.pugabyte.nexus.models.boost.Booster;
import me.pugabyte.nexus.models.boost.Booster.Boost;
import me.pugabyte.nexus.models.boost.BoosterService;
import me.pugabyte.nexus.utils.ItemBuilder;
import me.pugabyte.nexus.utils.JsonBuilder;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.PlayerUtils.Dev;
import me.pugabyte.nexus.utils.StringUtils;
import me.pugabyte.nexus.utils.Tasks;
import me.pugabyte.nexus.utils.Utils;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

@Aliases("boost")
@NoArgsConstructor
public class BoostsCommand extends CustomCommand implements Listener {
	private final BoostConfigService configService = new BoostConfigService();
	private final BoostConfig config = configService.get0();
	private final BoosterService service = new BoosterService();
	private Booster booster;

	public BoostsCommand(@NonNull CommandEvent event) {
		super(event);
		if (isPlayerCommandEvent())
			booster = service.get(player());
	}

	static {
		Tasks.repeatAsync(0, 5, () -> {
			BoostConfig config = BoostConfig.get();

			for (Boostable boostable : config.getBoosts().keySet()) {
				Boost boost = config.getBoost(boostable);
				if (boost.isExpired())
					boost.expire();
			}
		});
		Tasks.repeatAsync(Time.MINUTE, Time.MINUTE, () -> {
			BoostConfig config = BoostConfig.get();
			if (config.getBoosts().isEmpty())
				return;

			DiscordHandler.editMessage();
		});
	}

	@Path("[page]")
	void list(@Arg("1") int page) {
		if (config.getBoosts().isEmpty())
			error("There are no active server boosts");

		line();
		send(PREFIX + "Active server boosts");

		Map<Boostable, LocalDateTime> timeLeft = new HashMap<>() {{
			for (Boostable boostable : config.getBoosts().keySet())
				put(boostable, config.getBoost(boostable).getExpiration());
		}};

		BiFunction<Boostable, String, JsonBuilder> formatter = (type, index) -> {
			Boost boost = config.getBoost(type);
			return json(" &6" + boost.getMultiplierFormatted() + " &e" + camelCase(type) + " &7- " + boost.getNickname() + " &3(" + boost.getTimeLeft() + ")");
		};

		paginate(Utils.sortByValueReverse(timeLeft).keySet(), formatter, "/boosts", page);
	}

	@Path("menu")
	void menu() {
		if (booster.getNonExpiredBoosts().isEmpty())
			error("You do not have any boosts! Purchase them at &ehttps://store.projecteden.gg/category/boosts");

		new BoostMenu().open(player());
	}

	@Path("give <player> <type> <multiplier> <duration> [amount]")
	void give(Booster booster, Boostable type, double multiplier, int duration, @Arg("1") int amount) {
		for (int i = 0; i < amount; i++)
			booster.add(type, multiplier, duration);
		service.save(booster);

		send(PREFIX + "Gave " + amount + " " + plural(camelCase(type) + " boost", amount) + " to " + booster.getNickname());
	}

	@Confirm
	@Permission("group.staff")
	@Path("cancel <type> [--refund]")
	void cancel(Boostable type, @Switch boolean refund) {
		if (!config.hasBoost(type))
			error("There is no active " + camelCase(type) + " boost");

		Boost boost = config.getBoost(type);
		boost.cancel();

		if (refund)
			boost.getBooster().add(type, boost.getMultiplier(), boost.getDurationLeft());

		service.save(boost.getBooster());

		send(PREFIX + "Cancelled " + boost.getNickname() + "'s " + boost.getMultiplierFormatted() + " "
				+ camelCase(type) + " boost" + (refund ? " and refunded the time left" : ""));
	}

	@Confirm
	@Permission("group.seniorstaff")
	@Path("start <type> <multiplier> <duration>")
	void start(Boostable type, double multiplier, int duration) {
		if (config.hasBoost(type))
			cancel(type, true);

		Booster booster = service.get(Dev.KODA.getUuid());
		Boost boost = booster.add(type, multiplier, duration);
		boost.activate();

		service.save(booster);

		send(PREFIX + "Started a server " + boost.getMultiplierFormatted() + " " + camelCase(type)
				+ " boost for " + Timespan.of(boost.getDuration()).format(FormatType.LONG));
	}

	@AllArgsConstructor
	private static class BoostMenu extends MenuUtils implements InventoryProvider {
		private final Boostable type;
		private final BoostMenu previousMenu;

		@Override
		public void open(Player viewer, int page) {
			SmartInventory.builder()
					.provider(this)
					.title("Boosts")
					.size(6, 9)
					.build()
					.open(viewer, page);
		}

		public BoostMenu() {
			this(null);
		}

		public BoostMenu(Boostable type) {
			this.type = type;
			this.previousMenu = null;
		}

		@Override
		public void init(Player player, InventoryContents contents) {
			final BoostConfigService configService = new BoostConfigService();
			final BoostConfig config = configService.get0();
			final BoosterService service = new BoosterService();
			final Booster booster = service.get(player);

			if (previousMenu == null)
				addCloseItem(contents);
			else
				addBackItem(contents, e -> previousMenu.open(player));

			List<ClickableItem> items = new ArrayList<>();

			if (type == null)
				for (Boostable boostable : Boostable.values()) {
					int boosts = booster.getNonExpiredBoosts(boostable).size();
					if (boosts > 0) {
						ItemBuilder item = boostable.getDisplayItem().lore("&3" + StringUtils.plural(boosts + " boost", boosts) + " available");
						items.add(ClickableItem.from(item.build(), e -> new BoostMenu(boostable, this).open(player)));
					}
				}
			else
				for (Boost boost : booster.get(type)) {
					ItemBuilder item = boost.getDisplayItem();
					if (boost.isActive()) {
						item.lore("", "&6&lActive &7- &e" + boost.getTimeLeft());
						contents.set(0, 4, ClickableItem.empty(item.build()));
					} else if (boost.canActivate()) {
						if (config.hasBoost(boost.getType())) {
							item.lore("", "&cCannot activate, another boost is already active");
							items.add(ClickableItem.empty(item.build()));
						} else {
							item.lore("&3Duration: &e" + Timespan.of(boost.getDuration()).format(FormatType.LONG), "", "&eClick to activate");
							items.add(ClickableItem.from(item.build(), e -> ConfirmationMenu.builder()
									.title("Activate " + StringUtils.camelCase(boost.getType()) + " Boost")
									.onConfirm(e2 -> {
										boost.activate();
										open(player);
									})
									.onCancel(e2 -> open(player))
									.open(player)));
						}
					}
				}


			addPagination(player, contents, items);
		}

	}

	private double get(Boostable experience) {
		return BoostConfig.multiplierOf(experience);
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onMcMMOExpGain(McMMOPlayerXpGainEvent event) {
		event.setRawXpGained((float) (event.getRawXpGained() * get(Boostable.MCMMO_EXPERIENCE)));
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onExpGain(PlayerPickupExperienceEvent event) {
		ExperienceOrb orb = event.getExperienceOrb();
		orb.setExperience((int) Math.round(orb.getExperience() * get(Boostable.EXPERIENCE)));
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onJoin(PlayerJoinEvent event) {
		Tasks.wait(Time.SECOND.x(2), () -> {
			if (!event.getPlayer().isOnline())
				return;

			Set<Boostable> boosts = BoostConfig.get().getBoosts().keySet();
			if (boosts.isEmpty())
				return;

			PlayerUtils.runCommand(event.getPlayer(), "boosts");
		});
	}

}
