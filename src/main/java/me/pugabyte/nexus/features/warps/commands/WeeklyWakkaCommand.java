package me.pugabyte.nexus.features.warps.commands;

import lombok.NoArgsConstructor;
import me.pugabyte.nexus.features.crates.models.CrateType;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.nickname.Nickname;
import me.pugabyte.nexus.models.warps.Warp;
import me.pugabyte.nexus.models.warps.WarpService;
import me.pugabyte.nexus.models.warps.WarpType;
import me.pugabyte.nexus.models.weeklywakka.WeeklyWakka;
import me.pugabyte.nexus.models.weeklywakka.WeeklyWakkaService;
import me.pugabyte.nexus.utils.CitizensUtils;
import me.pugabyte.nexus.utils.JsonBuilder;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.RandomUtils;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Permission("group.staff")
@NoArgsConstructor
public class WeeklyWakkaCommand extends _WarpCommand implements Listener {

	private static final int npcId = 3362;
	private static final int stationaryNPCId = 3361;

	private static final List<JsonBuilder> tips = new ArrayList<>() {{
		add(new JsonBuilder("&3You can reset your McMMO stats when maxed with &c/mcmmo reset &3for unique gear and in-game money.").command("/mcmmo reset").hover("&eClick to run the command!"));
		add(new JsonBuilder("&3Considering a donor perk, but not sure? You can test many of the commands in the donor test world- find it in the &c/warps &3menu!").command("/warps"));
		add(new JsonBuilder("&3Each month the community has a group goal for voting." +
				" You can see progress on our website (&eprojecteden.gg/vote&3). Reaching the goal means a prize for the whole community the following month!").url("https://projecteden.gg/vote").hover("&eClick to visit the site!"));
		add(new JsonBuilder("&3Have you checked out our YouTube channel yet? We highlight our server, updates, and our dedicated staff members! &eClick here to visit!").url("https://yt.projecteden.gg").hover("&eClick to visit the site!"));
		add(new JsonBuilder("&3You can buy extra plots in the creative world through the vote point store or the donor perk store. If you have more than one plot, you can merge adjacent plots to form larger plots."));
		add(new JsonBuilder("&3Want a schematic of your creative plot? You can request one with &c/dlrequest"));
		add(new JsonBuilder("&3Have you visited the resource world &c/market &3yet? You can earn a large profit selling farmed resources!").command("/market").hover("&eClick to run the command!"));
		add(new JsonBuilder("&3The podiums at spawn are updated periodically showing a variety of achievements and leaderboards. Have you made it to any of the leaderboards?").command("/warp leaderboards").hover("&eClick to run the command!"));
		add(new JsonBuilder("&3Been to our banner store? Warp to &e/warp banners &3to find a big selection of banners available for vote points!").command("/warp banners").hover("&eClick to run the command!"));
		add(new JsonBuilder("&3We hold many events during the year! Check back frequently for holiday fun all year round."));
		add(new JsonBuilder("&3Have you thanked a code nerd today? ;)").suggest("Thank you code nerds for your hard work! <3").hover("&eClick to thank the code nerds!"));
		add(new JsonBuilder("&3Our server has hundreds of hours of custom code thanks to the work of our code nerds- but many of the most loved features came from community suggestions. " +
				"Head to the Discord (&ediscord.projecteden.gg&3) if you have an idea for a feature.").url("discord.projecteden.gg").hover("&eClick to visit the site!"));
		add(new JsonBuilder("&3If you see a bug, please report it in the #bugs-support-and-suggestions channel on our Discord server (&ediscord.projecteden.gg&3).").url("https://discord.projecteden.gg").hover("&eClick to visit the site!"));
		add(new JsonBuilder("&3Tired of logs and stairs placing the wrong way?  Try ")
				.next("&c/swl ").command("/swl").hover("&eClick to run the command!").group()
				.next("&3(sideways logs) and ").group()
				.next("&c/sws").command("/swl").hover("&eClick to run the command!").group()
				.next(" &3(sideways stairs) to \"lock\" your placement direction while you build.").group());
		add(new JsonBuilder("&3Don't forget that you can set multiple homes which you can warp back to at any time! To add a new one, just use &c/sethome name").suggest("/sethome ").hover("&eClick to run the command!"));
		add(new JsonBuilder("&3Did you know you can lock or unlock your homes to change if other people can access them? Try it out in the &c/homes edit &3menu!").command("/homes edit").hover("&eClick to run the command!"));
		add(new JsonBuilder("&3Complete Discord verification with Koda to unlock several commands, like &c/pay&3, from the Discord's #bridge channel. You can even be reminded to vote!"));
		add(new JsonBuilder("&3Did you know you can ").group()
				.next("&c/vote").command("/vote").hover("&eClick to run the command!").group()
				.next("&3 for the server for free rewards? After voting, you can redeem your points in our vote point store with ").group()
				.next("&c/vps!").command("/vps").hover("&eClick to run the command!").group());
		add(new JsonBuilder("&3The walls of grace (&c/wog&3) are a great way to share your love for the server. Leave a sign for others to read").command("/wog").hover("&eClick to run the command!"));
	}};

	public WeeklyWakkaCommand(CommandEvent event) {
		super(event);
	}

	@Override
	public WarpType getWarpType() {
		return WarpType.WEEKLY_WAKKA;
	}

	@Path("info")
	void info() {
		send("&3Hey there, my name is &eWakka&3, an admin of this server. &eI have hidden a version of myself somewhere in spawn " +
				"&3for you to find. If you find it, you can get a key to open up one of my crates here. My clone will move location " +
				"once a week, so you can get a new reward each week! If you find it, &eI'll tell you a tip about the server and give " +
				"you a key to this crate.");
	}

	@Path("found")
	@Permission(value = "group.admin", absolute = true)
	void whoFound() {
		send(PREFIX + "Found: " + new WeeklyWakkaService().get()
				.getFoundPlayers().stream()
				.map(Nickname::of)
				.collect(Collectors.joining(", ")));
	}

	@Path("tp")
	@Permission(value = "group.admin", absolute = true)
	void tp() {
		send(PREFIX + "Teleporting to location #" + new WeeklyWakkaService().get().getCurrentLocation());
		player().teleportAsync(getNPC().getStoredLocation(), TeleportCause.COMMAND);
	}

	@EventHandler
	public void onStationaryNPCClick(NPCRightClickEvent event) {
		if (event.getNPC().getId() != stationaryNPCId) return;
		PlayerUtils.runCommandAsOp(event.getClicker(), "weeklywakka info");
	}

	@EventHandler
	public void onNPCClick(NPCRightClickEvent event) {
		if (event.getNPC().getId() != npcId) return;
		WeeklyWakkaService service = new WeeklyWakkaService();
		WeeklyWakka weeklyWakka = service.get0();
		Player player = event.getClicker();
		if (weeklyWakka.getFoundPlayers().contains(player.getUniqueId())) {
			send(player, "&cYou have already found this week's Weekly Wakka. Come back next week!");
			return;
		}
		weeklyWakka.getFoundPlayers().add(player.getUniqueId());
		service.save(weeklyWakka);

		tips.get(Integer.parseInt(weeklyWakka.getCurrentTip())).send(player);
		CrateType.WEEKLY_WAKKA.give(player);
	}

	@Path("move")
	@Permission(value = "group.admin", absolute = true)
	public void move() {
		List<Warp> warps = new WarpService().getWarpsByType(WarpType.WEEKLY_WAKKA);
		WeeklyWakkaService service = new WeeklyWakkaService();
		WeeklyWakka weeklyWakka = service.get0();
		Warp currentWarp = warps.stream().filter(warp -> warp.getName().equals(weeklyWakka.getCurrentLocation())).findFirst().orElse(null);
		if (currentWarp != null)
			warps.remove(currentWarp);
		List<JsonBuilder> newTips = new ArrayList<>();
		for (JsonBuilder tip : tips) {
			if (weeklyWakka.getCurrentTip() == null) {
				newTips.addAll(tips);
				break;
			}
			if (!(tips.indexOf(tip) + "").equals(weeklyWakka.getCurrentTip()))
				newTips.add(tip);
		}
		JsonBuilder newTip = RandomUtils.randomElement(newTips);
		Warp newWarp = RandomUtils.randomElement(warps);
		weeklyWakka.setCurrentLocation(newWarp.getName());
		weeklyWakka.setCurrentTip(String.valueOf(tips.indexOf(newTip)));
		weeklyWakka.getFoundPlayers().clear();
		service.save(weeklyWakka);
		NPC npc = getNPC();
		npc.teleport(newWarp.getLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
		if (sender() != null)
			send(json("The Weekly Wakka NPC has moved to location #" + newWarp.getName()).command("/weeklywakka " + newWarp.getName()));
	}

	private NPC getNPC() {
		return CitizensUtils.getNPC(npcId);
	}

}
