package me.pugabyte.nexus.features.events.y2020.pugmas20.commands;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import eden.utils.TimeUtils.Timespan;
import lombok.NoArgsConstructor;
import me.pugabyte.nexus.features.events.models.QuestStage;
import me.pugabyte.nexus.features.events.y2020.pugmas20.AdventChests;
import me.pugabyte.nexus.features.events.y2020.pugmas20.Pugmas20;
import me.pugabyte.nexus.features.events.y2020.pugmas20.Train;
import me.pugabyte.nexus.features.events.y2020.pugmas20.menu.AdventMenu;
import me.pugabyte.nexus.features.events.y2020.pugmas20.models.AdventChest;
import me.pugabyte.nexus.features.events.y2020.pugmas20.models.AdventChest.District;
import me.pugabyte.nexus.features.events.y2020.pugmas20.models.Merchants.MerchantNPC;
import me.pugabyte.nexus.features.events.y2020.pugmas20.quests.LightTheTree;
import me.pugabyte.nexus.features.events.y2020.pugmas20.quests.OrnamentVendor;
import me.pugabyte.nexus.features.events.y2020.pugmas20.quests.OrnamentVendor.Ornament;
import me.pugabyte.nexus.features.events.y2020.pugmas20.quests.OrnamentVendor.PugmasTreeType;
import me.pugabyte.nexus.features.events.y2020.pugmas20.quests.Quests.Pugmas20Quest;
import me.pugabyte.nexus.features.events.y2020.pugmas20.quests.Quests.Pugmas20QuestStageHelper;
import me.pugabyte.nexus.features.events.y2020.pugmas20.quests.TheMines;
import me.pugabyte.nexus.features.events.y2020.pugmas20.quests.TheMines.OreType;
import me.pugabyte.nexus.features.events.y2020.pugmas20.quests.ToyTesting;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Aliases;
import me.pugabyte.nexus.framework.commands.models.annotations.Arg;
import me.pugabyte.nexus.framework.commands.models.annotations.Description;
import me.pugabyte.nexus.framework.commands.models.annotations.HideFromHelp;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.annotations.Redirects.Redirect;
import me.pugabyte.nexus.framework.commands.models.annotations.TabCompleteIgnore;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.eventuser.EventUser;
import me.pugabyte.nexus.models.eventuser.EventUserService;
import me.pugabyte.nexus.models.pugmas20.Pugmas20User;
import me.pugabyte.nexus.models.pugmas20.Pugmas20UserService;
import me.pugabyte.nexus.utils.JsonBuilder;
import me.pugabyte.nexus.utils.MerchantBuilder.TradeBuilder;
import me.pugabyte.nexus.utils.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static me.pugabyte.nexus.features.events.y2020.pugmas20.Pugmas20.isBeforePugmas;
import static me.pugabyte.nexus.features.events.y2020.pugmas20.Pugmas20.isPastPugmas;
import static me.pugabyte.nexus.features.events.y2020.pugmas20.Pugmas20.isSecondChance;
import static me.pugabyte.nexus.features.events.y2020.pugmas20.Pugmas20.showWaypoint;
import static me.pugabyte.nexus.features.events.y2020.pugmas20.models.QuestNPC.getUnplayedToysList;

@Aliases("pugmas")
@NoArgsConstructor
@Redirect(from = "/advent", to = "/pugmas advent")
@Redirect(from = "/district", to = "/pugmas district")
@Redirect(from = "/waypoint", to = "/pugmas waypoint")
public class PugmasCommand extends CustomCommand implements Listener {
	private final String timeLeft = Timespan.of(Pugmas20.openingDay).format();
	private final Pugmas20UserService pugmasService = new Pugmas20UserService();
	private Pugmas20User pugmasUser;
	private final EventUserService eventUserService = new EventUserService();
	private EventUser eventUser;

	public PugmasCommand(CommandEvent event) {
		super(event);
		PREFIX = Pugmas20.PREFIX;
		if (isPlayer()) {
			pugmasUser = pugmasService.get(player());
			eventUser = eventUserService.get(player());
		}
	}

	@Path
	void pugmas() {
		LocalDate now = LocalDate.now();
		if (isBeforePugmas(now) && !isStaff())
			error("Soon™ (" + timeLeft + ")");

		if (pugmasUser.isWarped()) {
			player().teleportAsync(Pugmas20.getSubsequentSpawn(), TeleportCause.COMMAND);
		} else {
			player().teleportAsync(Pugmas20.getInitialSpawn(), TeleportCause.COMMAND);
			pugmasUser.setWarped(true);
			pugmasService.save(pugmasUser);
		}
	}

	@Path("muteTrain [muted]")
	@Description("Mute train sounds")
	void muteTrain(Boolean muted) {
		if (muted == null)
			muted = !pugmasUser.isMuteTrain();

		pugmasUser.setMuteTrain(muted);
		pugmasService.save(pugmasUser);
		send(PREFIX + "Train " + (muted ? "muted" : "unmuted"));
	}

	@Path("progress [player]")
	@Description("View your event progress")
	void progress(@Arg(value = "self", permission = "group.staff") Pugmas20User user) {
		LocalDate now = LocalDate.now();

		if (isBeforePugmas(now))
			now = now.withYear(2020).withMonth(12).withDayOfMonth(1);

		if (isPastPugmas(now))
			error("Next year!");

		if (isSecondChance(now))
			now = now.withYear(2020).withMonth(12).withDayOfMonth(25);

		int day = now.getDayOfMonth();

		line(2);

		send(PREFIX + "Event progress (Day &e#" + day + "&3):");
		line();

		String advent;
		AdventChest adventChest = AdventChests.getAdventChest(day);

		if (user.getFoundDays().size() == 25)
			advent = "&a☑ &3Complete";
		else if (user.getFoundDays().contains(day))
			advent = "&a☑ &3Found today's chest";
		else if (day == 25)
			if (user.getFoundDays().size() != 24)
				advent = "&7☐ &3Find all chests before #25";
			else
				advent = "&7☐ &3Find the last chest";
		else
			advent = "&7☐ &3Find today's chest (&e#" + day + " &3in the &e" + adventChest.getDistrict().getName() + " District&3)";

		send("&6&lAdvent Chests");
		send(json("&f  " + advent + " &7- Click for info").hover("Click to open the Advent menu").command("/pugmas advent"));

		line();
		send("&6&lQuests");

		for (Pugmas20QuestStageHelper quest : Pugmas20QuestStageHelper.values()) {
			QuestStage stage = quest.getter().apply(user);
			String instructions = Pugmas20Quest.valueOf(quest.name()).getInstructions(user, stage);
			JsonBuilder json = json();
			if (quest == Pugmas20QuestStageHelper.THE_MINES && stage == QuestStage.STARTED) {
				List<String> tradesLeft = getIngotTradesLeft(user);
				if (tradesLeft.isEmpty()) {
					json.next("&f  &a☑ &3" + camelCase(quest) + " &7- &aCompleted daily quest &7- Come back tomorrow for more");
				} else {
					json.next("&f  &7☐ &3" + camelCase(quest) + " &7- &eIn progress &7- " + instructions);
					tradesLeft.add(0, "&6Today's available trades:");
					json.next(" &7&o(Hover for info)").hover(String.join("\n", tradesLeft));
				}
			} else {
				if (stage == QuestStage.COMPLETE) {
					json.next("&f  &a☑ &3" + camelCase(quest) + " &7- &aComplete");
				} else if (stage == QuestStage.NOT_STARTED || stage == QuestStage.INELIGIBLE) {
					json.next("&f  &7☐ &3" + camelCase(quest) + " &7- &cNot started" + (instructions == null ? "" : " &7- " + instructions));
				} else {
					json.next("&f  &7☐ &3" + camelCase(quest) + " &7- &eIn progress &7- ");
					if (instructions == null)
						json.next("&c???").hover(camelCase(stage));
					else
						json.next("&7" + instructions);
				}

				if (quest == Pugmas20QuestStageHelper.TOY_TESTING && stage == QuestStage.STARTED) {
					List<String> toysLeft = getUnplayedToysList(user);
					toysLeft.add(0, "&6Toys left to test:");
					json.next(" &7&o(Hover for info)").hover(String.join("\n&f", toysLeft));
				}

				if (quest == Pugmas20QuestStageHelper.ORNAMENT_VENDOR && stage != QuestStage.NOT_STARTED) {
					List<String> lore = getOrnamentTradesLeft(user);
					if (!lore.isEmpty()) {
						lore.add(0, "&6Available ornament trades:");
						if (stage == QuestStage.COMPLETE)
							json.next(" &7- More trades available");
						lore.add("&f");
						lore.add("&fYou get to keep any extra ornaments");
						json.next(" &7&o(Hover for info)").hover(String.join("\n&f", lore));
					}
				}
			}

			send(json);
		}

		line();
		if (day < 25) {
			send("&3Next day begins in &e" + Timespan.of(now.plusDays(1)).format());
			line();
		}
	}

	private List<String> getOrnamentTradesLeft(Pugmas20User user) {
		List<String> lore = new ArrayList<>();
		for (Ornament ornament : Ornament.values()) {
			if (!user.canTradeOrnament(ornament))
				continue;

			int tradesLeft = user.ornamentTradesLeft(ornament);
			lore.add("&e" + tradesLeft + " &f" + plural(camelCase(ornament) + " Ornament", tradesLeft));
		}
		return lore;
	}

	private List<String> getIngotTradesLeft(Pugmas20User user) {
		List<String> tradesLeft = new ArrayList<>();
		List<TradeBuilder> trades = MerchantNPC.THEMINES_SELLCRATE.getTrades(user);

		for (OreType oreType : OreType.values()) {
			int ingotsLeft = getIngotsLeft(user, trades, oreType);
			if (ingotsLeft > 0)
				tradesLeft.add("&e" + ingotsLeft + " &f" + camelCase(oreType));
		}
		return tradesLeft;
	}

	private int getIngotsLeft(Pugmas20User user, List<TradeBuilder> trades, OreType oreType) {
		Optional<Integer> amount = trades.stream()
				.map(tradeBuilder -> tradeBuilder.getIngredients().iterator().next())
				.filter(ingredient -> ingredient.getType() == oreType.getIngot().getType())
				.map(ItemStack::getAmount)
				.findFirst();

		int tokensLeft = Math.abs(Pugmas20.checkDailyTokens(user.getOfflinePlayer(), "themines_" + oreType.name(), 0));
		int perToken = amount.orElse(0);

		return tokensLeft * perToken;
	}

	@Path("advent")
	@Description("Open the Advent menu")
	void advent() {
		LocalDate now = LocalDate.now();

		if (isBeforePugmas(now))
			error("Soon™ (" + timeLeft + ")");

		if (isPastPugmas(now))
			error("Next year!");

		if (isSecondChance(now))
			now = now.withYear(2020).withMonth(12).withDayOfMonth(25);

		AdventMenu.openAdvent(player(), now);
	}

	@Permission("group.admin")
	@Path("advent give <day>")
	void adventGive(int day) {
		AdventChests.giveAdventHead(player(), day);
	}

	@Permission("group.admin")
	@Path("advent open <day>")
	void adventOpenDay(int day) {
		AdventChests.openAdventLootInv(player(), day);
	}

	@Permission("group.admin")
	@Path("advent foundCounts")
	void adventFoundAll() {
		send(PREFIX + "Found counts:");
		new HashMap<Integer, List<String>>() {{
			pugmasService.getAll().forEach(user -> {
				List<String> names = getOrDefault(user.getLocatedDays().size(), new ArrayList<>());
				names.add(user.getName());
				put(user.getLocatedDays().size(), names);
			});
		}}.forEach(((day, names) -> send("&3" + day + " &e" + String.join(", ", names))));
	}

	@Permission("group.admin")
	@Path("advent addDay <player> <day>")
	void adventAddDay(Pugmas20User user, int day) {
		user.getFoundDays().add(day);
		pugmasService.save(user);

		send("Added day " + day + " to " + user.getName());
	}

	@Path("district")
	@Description("View which district you are currently in")
	void district() {
		District district = District.of(location());
		send(PREFIX + "You are " + (district == District.UNKNOWN ? "not in a district" : "in the &e" + district.getName() + " District"));
	}

	@Permission("group.admin")
	@Path("waypoint give <day>")
	void waypointGive(int day) {
		pugmasUser.getLocatedDays().add(day);
		pugmasService.save(pugmasUser);
	}

	@Path("waypoint <day>")
	@Description("Get directions to a chest you have already found")
	void waypoint(int day) {
		if (!pugmasUser.getLocatedDays().contains(day))
			error("You have not located that chest yet");

		AdventChest adventChest = AdventChests.getAdventChest(day);
		if (adventChest == null)
			error("Advent chest is null");

		showWaypoint(adventChest, player());
	}

	@Permission("group.admin")
	@Path("waypoints")
	void waypoint() {
		for (AdventChest adventChest : AdventChests.adventChestList)
			showWaypoint(adventChest, player());
	}

	@HideFromHelp
	@Path("toys")
	void toys() {
		if (pugmasUser.getToyTestingStage() == QuestStage.NOT_STARTED)
			error("You cannot use this");

		player().teleport(ToyTesting.getBackLocation(), TeleportCause.COMMAND);
	}

	@Path("train")
	@Permission("group.admin")
	void train() {
		if (Train.animating())
			error("Train is animating!");

		if (Bukkit.getTPS()[0] < 19)
			error("TPS is too low, must be 19+");

		Train.animate();
	}

	@Permission("group.admin")
	@Path("tree build <treeType> <id>")
	void treeSchematic(PugmasTreeType treeType, int id) {
		treeType.build(id);
	}

	@Permission("group.admin")
	@Path("tree feller <treeType> <id>")
	void treeFeller(PugmasTreeType treeType, int id) {
		treeType.feller(player(), id);
	}

	@Permission("group.admin")
	@Path("tree feller all")
	void treeFeller() {
		for (PugmasTreeType treeType : PugmasTreeType.values())
			for (Integer id : treeType.getPasters().keySet())
				treeType.feller(player(), id);
	}

	@Permission("group.admin")
	@Path("tree copy <treeType>")
	void treeCopy(PugmasTreeType treeType) {
		runCommand("/copy -m orange_wool,snow," + treeType.getAllMaterialsString());
	}

	@Permission("group.admin")
	@Path("tree save <treeType> <id>")
	void treeSave(PugmasTreeType treeType, int id) {
		runCommand("mcmd /copy -m snow," + treeType.getAllMaterialsString() + " ;; /schem save pugmas20/trees/" + treeType.name().toLowerCase() + "/" + id + " -f");
	}

	@Permission("group.admin")
	@Path("tree region <treeType> <id>")
	void treeRegion(PugmasTreeType treeType, int id) {
		ProtectedRegion region = treeType.getRegion(id);
		String command = region == null ? "define" : "redefine";
		runCommand("mcmd /here ;; rg " + command + " pugmas20_trees_" + treeType.name().toLowerCase() + "_" + id);
	}

	@Permission("group.admin")
	@Path("tree get")
	void treeGet() {
		Material logs = getTargetBlockRequired().getType();
		PugmasTreeType treeType = PugmasTreeType.of(logs);
		if (treeType == null)
			error("Pugmas Tree with logs " + camelCase(logs) + " not found");

		send(PREFIX + "You are looking at a " + camelCase(treeType) + " tree");
	}

	@Permission("group.admin")
	@Path("tree counts")
	void treeCounts() {
		int total = 0;
		JsonBuilder json = json(PREFIX + "Pugmas tree counts:");
		for (PugmasTreeType treeType : PugmasTreeType.values()) {
			Set<Integer> ids = treeType.getPasters().keySet();
			if (ids.size() == 0)
				continue;

			String collect = ids.stream().map(String::valueOf).collect(Collectors.joining(", "));
			json.newline().next("&e " + camelCase(treeType) + " &7- " + ids.size() + " &3[" + collect + "]");
			total += ids.size();
		}

		if (total == 0)
			error("No pugmas trees found");

		send(json.newline().next("&3Total: &e" + total));
	}

	@Permission("group.admin")
	@Path("kit the_mines pickaxe")
	void kitMinersPickaxe() {
		PlayerUtils.giveItem(player(), TheMines.getMinersPickaxe());
	}

	@Permission("group.admin")
	@Path("kit the_mines sieve")
	void kitMinersSieve() {
		PlayerUtils.giveItem(player(), TheMines.getMinersSieve());
	}

	@Permission("group.admin")
	@Path("kit the_mines ores")
	void kitMinersOres() {
		for (OreType oreType : OreType.values())
			PlayerUtils.giveItem(player(), oreType.getOre());
	}

	@Permission("group.admin")
	@Path("kit the_mines ingots")
	void kitMinersIngot() {
		for (OreType oreType : OreType.values())
			PlayerUtils.giveItem(player(), oreType.getIngot());
	}

	@Permission("group.admin")
	@Path("kit ornament_vendor ornaments")
	void kitOrnamentVendorOrnaments() {
		for (Ornament ornament : Ornament.values())
			PlayerUtils.giveItem(player(), ornament.getSkull());
	}

	@Permission("group.admin")
	@Path("kit ornament_vendor axe")
	void kitOrnamentVendorAxe() {
		PlayerUtils.giveItem(player(), OrnamentVendor.getLumberjacksAxe());
	}

	@Permission("group.admin")
	@Path("kit ornament_vendor logs")
	void kitOrnamentVendorLogs() {
		for (Ornament ornament : Ornament.values())
			PlayerUtils.giveItem(player(), ornament.getTreeType().getLog(64));
	}

	@Permission("group.admin")
	@Path("kit light_the_tree")
	void kitLightTheTree() {
		PlayerUtils.giveItem(player(), LightTheTree.lighter);
		PlayerUtils.giveItem(player(), LightTheTree.lighter_broken);
		PlayerUtils.giveItem(player(), LightTheTree.steel_ingot);
	}

	@Permission("group.admin")
	@Path("inventory store")
	void inventoryStore() {
		pugmasUser.storeInventory();
		pugmasService.save(pugmasUser);
		send(PREFIX + "Stored inventory");
	}

	@Permission("group.admin")
	@Path("inventory apply")
	void inventoryApply() {
		pugmasUser.applyInventory();
		pugmasService.save(pugmasUser);
	}

	@Permission("group.admin")
	@Path("npcs emeralds")
	void npcsHolograms() {
		Pugmas20.createNpcHolograms();
	}

	@Permission("group.admin")
	@Path("quests stage set <quest> <stage>")
	void questStageSet(Pugmas20QuestStageHelper quest, QuestStage stage) {
		quest.setter().accept(pugmasUser, stage);
		pugmasService.save(pugmasUser);
		send(PREFIX + "Quest stage for Quest " + camelCase(quest) + " set to " + camelCase(stage));
	}

	@Permission("group.admin")
	@Path("quests stage get <quest>")
	void questStageSet(Pugmas20QuestStageHelper quest) {
		send(PREFIX + "Quest stage for Quest " + camelCase(quest) + ": " + quest.getter().apply(pugmasUser));
	}

	@Permission("group.admin")
	@Path("quests light_the_tree setTorchesLit <int>")
	void questLightTheTreeSetLit(int lit) {
		pugmasUser.setTorchesLit(lit);
		pugmasService.save(pugmasUser);
		send(PREFIX + "Set torches lit to " + lit);
	}

	@Permission("group.admin")
	@Path("quests light_the_tree reset")
	void questLightTheTreeReset() {
		pugmasUser.resetLightTheTree();
		pugmasService.save(pugmasUser);
		send(PREFIX + "Reset Light The Tree quest variables");
	}

	@Permission("group.admin")
	@Path("quests ornament_vendor reset")
	void questOrnamentVendorReset() {
		pugmasUser.getOrnamentTradeCount().clear();
		pugmasService.save(pugmasUser);
		send(PREFIX + "Reset Ornament Vendor quest variables");
	}

	@Permission("group.admin")
	@Path("quests ornament_vendor reloadHeads")
	void questOrnamentVendorReloadHeads() {
		Ornament.loadHeads();
		send(PREFIX + "Reloaded Ornament Vendor heads");
	}

	@HideFromHelp
	@TabCompleteIgnore
	@Path("quests light_the_tree teleportToStart")
	void questLightTheTreeTeleportToStart() {
		if (pugmasUser.isLightingTorches())
			error("You cannot teleport during the lighting ceremony");

		player().teleport(LightTheTree.getResetLocation(), TeleportCause.COMMAND);
		send(PREFIX + "Teleported to ceremony start");
	}

	@Permission("group.admin")
	@Path("debug <player>")
	void debugUser(@Arg("self") Pugmas20User user) {
		send(user.toString());
	}

}
