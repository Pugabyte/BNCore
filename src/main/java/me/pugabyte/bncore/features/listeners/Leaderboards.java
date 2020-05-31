package me.pugabyte.bncore.features.listeners;

import com.gmail.nossr50.events.experience.McMMOPlayerLevelUpEvent;
import com.gmail.nossr50.mcMMO;
import com.vexsoftware.votifier.model.VotifierEvent;
import lombok.NoArgsConstructor;
import me.pugabyte.bncore.BNCore;
import me.pugabyte.bncore.features.shops.ShopUtils;
import me.pugabyte.bncore.features.votes.EndOfMonth.TopVoterData;
import me.pugabyte.bncore.framework.exceptions.postconfigured.CooldownException;
import me.pugabyte.bncore.models.cooldown.CooldownService;
import me.pugabyte.bncore.models.hours.Hours;
import me.pugabyte.bncore.models.hours.HoursService;
import me.pugabyte.bncore.models.hours.HoursService.PageResult;
import me.pugabyte.bncore.models.nerd.Nerd;
import me.pugabyte.bncore.utils.CitizensUtils;
import me.pugabyte.bncore.utils.StringUtils;
import me.pugabyte.bncore.utils.Tasks;
import me.pugabyte.bncore.utils.Time;
import me.pugabyte.bncore.utils.Utils;
import net.ess3.api.events.UserBalanceUpdateEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static me.pugabyte.bncore.utils.StringUtils.colorize;
import static me.pugabyte.bncore.utils.Utils.runConsoleCommand;

@NoArgsConstructor
public class Leaderboards implements Listener {

	public enum Leaderboard {
		PLAYTIME_TOTAL(2709, 2708, 2707) {
			@Override
			Map<UUID, String> getTop() {
				return new HoursService().getPage(1).subList(0, 3).stream()
						.collect(Collectors.toMap(
								PageResult::getUuid,
								hours -> StringUtils.timespanFormat(hours.getTotal()),
								(h1, h2) -> h1, LinkedHashMap::new
						));
			}
		},
//		PLAYTIME_MONTHLY(2712, 2711, 2710) {
//			@Override
//			Map<UUID, String> getTop() {
//				return new HoursService().getPage(1).subList(0, 3).stream()
//						.collect(Collectors.toMap(
//								hours -> UUID.fromString(hours.getUuid()),
//								hours -> StringUtils.timespanFormat(hours.getMonthly()),
//								(h1, h2) -> h1, LinkedHashMap::new
//						));
//			}
//		},
		VOTES(2700, 2699, 2698) {
			@Override
			Map<UUID, String> getTop() {
				return new TopVoterData(LocalDateTime.now().getMonth()).getTopVoters().subList(0, 3).stream()
						.collect(Collectors.toMap(
								topVoter -> UUID.fromString(topVoter.getUuid()),
								topVoter -> String.valueOf(topVoter.getCount()),
								(h1, h2) -> h1, LinkedHashMap::new
						));
			}
		},
		BALANCE(2703, 2702, 2701) {
			@Override
			Map<UUID, String> getTop() {
				return new HoursService().getActivePlayers().stream()
						.collect(Collectors.toMap(Hours::getUuid, hours -> BNCore.getEcon().getBalance(Utils.getPlayer(hours.getUuid()))))
						.entrySet()
						.stream()
						.sorted(Entry.<UUID, Double>comparingByValue().reversed())
						.limit(3)
						.collect(Collectors.toMap(
								Entry::getKey,
								entry -> "$" + ShopUtils.pretty(entry.getValue()),
								(h1, h2) -> h1, LinkedHashMap::new
						));
			}
		},
		MCMMO(2706, 2705, 2704) {
			@Override
			Map<UUID, String> getTop() {
				return mcMMO.getDatabaseManager().readLeaderboard(null, 1, 3).subList(0, 3).stream()
						.collect(Collectors.toMap(
								playerStat -> Utils.getPlayer(playerStat.name).getUniqueId(),
								playerStat -> String.valueOf(playerStat.statVal),
								(h1, h2) -> h1, LinkedHashMap::new
						));
			}
		};

		int[] ids;

		Leaderboard(int... ids) {
			this.ids = ids;
			if (ids.length != 3)
				BNCore.warn(name() + " did not define 3 NPC ids (" + ids.length + ")");
		}

		abstract Map<UUID, String> getTop();

		public void update() {
			Tasks.async(() -> {
				try {
					new CooldownService().check("leaderboards", name(), Time.MINUTE.x(5));
					Map<UUID, String> top = getTop();
					if (top.size() != 3)
						BNCore.warn(name() + " leaderboard top query did not return 3 results (" + top.size() + ")");
					else
						Tasks.sync(() -> {
							AtomicInteger i = new AtomicInteger(0);
							top.entrySet().iterator().forEachRemaining(entry -> {
								Nerd nerd = new Nerd(entry.getKey());
								CitizensUtils.updateSkin(ids[i.get()], nerd.getName());
								CitizensUtils.updateName(ids[i.get()], colorize("&e" + entry.getValue()));
								runConsoleCommand("hd setline leaderboards_" + name().toLowerCase() + "_" + i.incrementAndGet() + " 1 " + nerd.getRankFormat());
							});
						});
				} catch (CooldownException ignore) {}
			});
		}
	}

	static {
		for (Leaderboard value : Leaderboard.values())
			Tasks.repeat(10, Time.HOUR, value::update);
	}

	@EventHandler
	public void onVote(VotifierEvent event) {
		Tasks.wait(1, Leaderboard.VOTES::update);
	}

	@EventHandler
	public void onMoneyChange(UserBalanceUpdateEvent event) {
		if (event.getNewBalance().doubleValue() >= 1000000)
			Leaderboard.BALANCE.update();
	}

	@EventHandler
	public void onMcMMOLevelUp(McMMOPlayerLevelUpEvent event) {
		Leaderboard.MCMMO.update();
	}
}
