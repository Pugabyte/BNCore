package me.pugabyte.nexus.features.events.y2021.bearfair21.commands;

import eden.utils.TimeUtils.Time;
import eden.utils.TimeUtils.Timespan;
import eden.utils.Utils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import me.pugabyte.nexus.features.events.models.Quest;
import me.pugabyte.nexus.features.events.models.QuestStage;
import me.pugabyte.nexus.features.events.y2021.bearfair21.BearFair21;
import me.pugabyte.nexus.features.events.y2021.bearfair21.BearFair21.BF21PointSource;
import me.pugabyte.nexus.features.events.y2021.bearfair21.fairgrounds.Interactables;
import me.pugabyte.nexus.features.events.y2021.bearfair21.fairgrounds.Seeker;
import me.pugabyte.nexus.features.events.y2021.bearfair21.islands.MinigameNightIsland;
import me.pugabyte.nexus.features.events.y2021.bearfair21.islands.MinigameNightIsland.RouterMenu;
import me.pugabyte.nexus.features.events.y2021.bearfair21.islands.MinigameNightIsland.ScrambledCablesMenu;
import me.pugabyte.nexus.features.events.y2021.bearfair21.quests.TreasureChests;
import me.pugabyte.nexus.features.events.y2021.bearfair21.quests.clientside.ClientsideContentManager;
import me.pugabyte.nexus.features.events.y2021.bearfair21.quests.npcs.BearFair21NPC;
import me.pugabyte.nexus.features.events.y2021.bearfair21.quests.npcs.Collector;
import me.pugabyte.nexus.features.events.y2021.bearfair21.quests.resources.fishing.FishingLoot.JunkWeight;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Aliases;
import me.pugabyte.nexus.framework.commands.models.annotations.Arg;
import me.pugabyte.nexus.framework.commands.models.annotations.Confirm;
import me.pugabyte.nexus.framework.commands.models.annotations.Description;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.bearfair21.BearFair21Config;
import me.pugabyte.nexus.models.bearfair21.BearFair21Config.BearFair21ConfigOption;
import me.pugabyte.nexus.models.bearfair21.BearFair21ConfigService;
import me.pugabyte.nexus.models.bearfair21.BearFair21User;
import me.pugabyte.nexus.models.bearfair21.BearFair21UserService;
import me.pugabyte.nexus.models.bearfair21.ClientsideContent;
import me.pugabyte.nexus.models.bearfair21.ClientsideContent.Content;
import me.pugabyte.nexus.models.bearfair21.ClientsideContent.Content.ContentCategory;
import me.pugabyte.nexus.models.bearfair21.ClientsideContentService;
import me.pugabyte.nexus.utils.BlockUtils;
import me.pugabyte.nexus.utils.JsonBuilder;
import me.pugabyte.nexus.utils.SoundBuilder;
import me.pugabyte.nexus.utils.StringUtils;
import me.pugabyte.nexus.utils.StringUtils.ProgressBarStyle;
import me.pugabyte.nexus.utils.Tasks;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Directional;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Aliases({"bf21", "bearfair"})
public class BearFair21Command extends CustomCommand {
	ClientsideContentService contentService = new ClientsideContentService();
	ClientsideContent clientsideContent = contentService.get0();

	BearFair21UserService userService = new BearFair21UserService();

	BearFair21ConfigService configService = new BearFair21ConfigService();
	BearFair21Config config = configService.get0();

	List<Content> contentList = clientsideContent.getContentList();

	public BearFair21Command(CommandEvent event) {
		super(event);
	}

	@Path
	void warp() {
		runCommand("bearfair21warp");
	}

	@Permission("group.admin")
	@Path("resetPugmas <player>")
	void resetPugmasQuest(BearFair21User user) {
		user.setQuestStage_Pugmas(QuestStage.NOT_STARTED);
		user.setPugmasCompleted(false);
		user.setPresentNdx(0);
		userService.save(user);
		send("Reset Pugmas quest variables for: " + user.getNickname());
	}

	@Permission("group.admin")
	@Path("strengthTest")
	void strengthTest() {
		commandBlock();
		Interactables.strengthTest();
	}

	@Permission("group.admin")
	@Path("seeker")
	void seeker() {
		send("Find the crimson button");
		Seeker.addPlayer(player());
	}

	@Path("toCollector")
	@Permission("group.admin")
	public void toCollector() {
		player().teleportAsync(Collector.getCurrentLoc());
	}

	@Path("progress [player]")
	@Description("View your event progress")
	void progress(@Arg(value = "self", permission = "group.staff") BearFair21User user) {
		final LocalDate start = LocalDate.of(2021, 6, 28);
		final LocalDate now = LocalDate.now();
		int day = start.until(now).getDays() + 1;

		send(PREFIX + "Event progress (Day &e#" + day + "&7/7&3):");
		line();

		send("&6&lQuests");
		for (BearFair21UserQuestStageHelper quest : BearFair21UserQuestStageHelper.values()) {
			JsonBuilder json = json();
			final QuestStage stage = quest.getter().apply(user);
			String instructions = BearFair21Quest.valueOf(quest.name()).getInstructions(user, stage);

			if (stage == QuestStage.COMPLETE)
				json.next("&f  &a☑ &3" + camelCase(quest) + " &7- &aComplete");
			else if (stage == QuestStage.NOT_STARTED || stage == QuestStage.INELIGIBLE)
				json.next("&f  &7☐ &3" + camelCase(quest) + " &7- &cNot started" + (instructions == null ? "" : " &7- " + instructions));
			else
				json.next("&f  &7☐ &3" + camelCase(quest) + " &7- &eIn progress" + (instructions == null ? "" : " &7- " + instructions));

			send(json);
		}

		line();
		send("&6&lFairgrounds");
		for (BF21PointSource source : BF21PointSource.values()) {
			JsonBuilder json = json();
			final int dailyTokensLeft = Math.abs(BearFair21.getDailyTokensLeft(user.getOfflinePlayer(), source, 0));

			if (dailyTokensLeft == 0)
				json.next("&f  &a☑ &3" + camelCase(source) + " &7- &aComplete");
			else
				json.next("&f  &7☐ &3" + camelCase(source) + " &7- &cIncomplete &3(&e" + dailyTokensLeft + " &3tokens left)");

			send(json);
		}

		line();
		send("&6&lTreasure Chests");
		final int found = user.getTreasureChests().size();
		final int total = TreasureChests.getLocations().size();
		send("&f  " + (found == total ? "&a☑" : "&7☐") + " &3Found: " + StringUtils.progressBar(found, total, ProgressBarStyle.COUNT, 40));

		line();
		if (day < 7) {
			send("&3Next day begins in &e" + Timespan.of(now.plusDays(1)).format());
			line();
		}
	}

	// Config

	@Permission("group.admin")
	@Path("config <option> <boolean>")
	void config(BearFair21ConfigOption option, boolean enabled) {
		config.setEnabled(option, enabled);
		configService.save(config);
		send(PREFIX + (enabled ? "&aEnabled" : "&cDisabled") + " &3config option &e" + camelCase(option));
	}

	// Command Blocks

	@Path("moveCollector")
	@Permission("group.admin")
	public void moveCollector() {
		commandBlock();
		Collector.move();
	}

	@Path("yachtHorn")
	@Permission("group.admin")
	public void yachtHorn() {
		commandBlock();
		BlockCommandSender sender = (BlockCommandSender) event.getSender();
		Location loc = sender.getBlock().getLocation();
		World world = loc.getWorld();
		if (world == null)
			return;

		new SoundBuilder(Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED).location(loc).volume(4).pitch(0.1).play();
		Tasks.wait(Time.SECOND.x(2), () ->
			new SoundBuilder(Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED).location(loc).volume(4).pitch(0.1).play());
	}

	@Path("metNPCs")
	@Permission("group.admin")
	public void metNPCs() {
		Set<Integer> npcs = userService.get(player()).getMetNPCs();
		if (Utils.isNullOrEmpty(npcs))
			error("User has not met any npcs");

		send("Has met: ");
		for (Integer npcId : npcs) {
			BearFair21NPC npc = BearFair21NPC.from(npcId);
			if (npc != null)
				send(" - " + npc.getNpcNameAndJob() + " (" + npcId + ")");
		}
	}

	@Path("nextStepNPCs")
	@Permission("group.admin")
	public void nextStepNPCs() {
		Set<Integer> npcs = userService.get(player()).getNextStepNPCs();
		if (Utils.isNullOrEmpty(npcs))
			error("User has not have any nextStepNPCs");

		send("Next Step NPCs: ");
		for (Integer npcId : npcs) {
			BearFair21NPC npc = BearFair21NPC.from(npcId);
			if (npc != null)
				send(" - " + npc.getNpcNameAndJob() + " (" + npcId + ")");
		}
	}

	@Getter
	@AllArgsConstructor
	@Accessors(fluent = true)
	public enum BearFair21UserQuestStageHelper {
		MAIN(BearFair21User::getQuestStage_Main, BearFair21User::setQuestStage_Main),
		RECYCLER(BearFair21User::getQuestStage_Recycle, BearFair21User::setQuestStage_Recycle),
		BEEKEEPER(BearFair21User::getQuestStage_BeeKeeper, BearFair21User::setQuestStage_BeeKeeper),
		LUMBERJACK(BearFair21User::getQuestStage_Lumberjack, BearFair21User::setQuestStage_Lumberjack),
		MINIGAME_NIGHT(BearFair21User::getQuestStage_MGN, BearFair21User::setQuestStage_MGN),
		PUGMAS(BearFair21User::getQuestStage_Pugmas, BearFair21User::setQuestStage_Pugmas),
		HALLOWEEN(BearFair21User::getQuestStage_Halloween, BearFair21User::setQuestStage_Halloween),
		SUMMER_DOWN_UNDER(BearFair21User::getQuestStage_SDU, BearFair21User::setQuestStage_SDU),
		;

		private final Function<BearFair21User, QuestStage> getter;
		private final BiConsumer<BearFair21User, QuestStage> setter;
	}

	@Getter
	@AllArgsConstructor
	public enum BearFair21Quest implements Quest<BearFair21User> {
		MAIN(user -> new HashMap<>() {{
			put(QuestStage.NOT_STARTED, "Find " + BearFair21NPC.MAYOR.getNpcNameAndJob() + " in Honeywood village");
			for (QuestStage stage : List.of(QuestStage.STARTED, QuestStage.STEP_ONE, QuestStage.STEP_TWO, QuestStage.STEP_THREE, QuestStage.STEP_FOUR, QuestStage.STEP_FIVE, QuestStage.STEP_SIX))
				put(stage, "Talk to " + BearFair21NPC.MAYOR.getNpcNameAndJob());
		}}),
		RECYCLER(user -> new HashMap<>() {{
			put(QuestStage.NOT_STARTED, "Find " + BearFair21NPC.FISHERMAN2.getNpcNameAndJob() + " by the lake");
			put(QuestStage.STARTED, "Recycled trash: " + StringUtils.progressBar(user.getRecycledItems(), JunkWeight.MIN.getAmount(), ProgressBarStyle.PERCENT, 40));
		}}),
		BEEKEEPER(user -> new HashMap<>() {{
			put(QuestStage.NOT_STARTED, "Find " + BearFair21NPC.BEEKEEPER.getNpcNameAndJob() + " by the bee colony");
			put(QuestStage.STARTED, "Find the beehive");
			put(QuestStage.STEP_ONE, "Talk to the Queen Bee");
			put(QuestStage.STEPS_DONE, "Talk to " + BearFair21NPC.BEEKEEPER.getNpcNameAndJob());
		}}),
		LUMBERJACK(user -> new HashMap<>() {{
			put(QuestStage.NOT_STARTED, "Find " + BearFair21NPC.LUMBERJACK.getNpcNameAndJob() + " in the saw mill");
			put(QuestStage.STARTED, "Talk to " + BearFair21NPC.LUMBERJACK.getNpcNameAndJob());
		}}),
		MINIGAME_NIGHT(user -> new HashMap<>() {{
			put(QuestStage.NOT_STARTED, "Find " + BearFair21NPC.AXEL.getNpcNameAndJob() + " by the Game Gallery");
			put(QuestStage.STARTED, "Talk to " + BearFair21NPC.MGN_CUSTOMER_1);
			put(QuestStage.STEP_ONE, "Talk to " + BearFair21NPC.MGN_CUSTOMER_1);
			put(QuestStage.STEP_TWO, "Answer the phone");
			put(QuestStage.STEP_THREE, "Repair the laptop");
			put(QuestStage.STEP_FOUR, "Call " + BearFair21NPC.MGN_CUSTOMER_2.getNpcNameAndJob() + " back");
			put(QuestStage.STEP_FIVE, "Answer the phone");
			put(QuestStage.STEP_SIX, "Answer the phone");
			put(QuestStage.STEP_SEVEN, "Talk to " + BearFair21NPC.ADMIRAL.getNpcNameAndJob());
			put(QuestStage.STEP_EIGHT, "Answer the phone");
		}}),
		PUGMAS(user -> new HashMap<>() {{
			put(QuestStage.NOT_STARTED, "Find " + BearFair21NPC.PUGMAS_MAYOR.getNpcNameAndJob() + " by the Pugmas Tree");
			for (QuestStage stage : List.of(QuestStage.STARTED, QuestStage.STEP_ONE))
				put(stage, "Talk to the " + BearFair21NPC.GRINCH.getNpcNameAndJob());
			put(QuestStage.STEP_TWO, "Find the presents");
		}}),
		HALLOWEEN(user -> new HashMap<>() {{
			put(QuestStage.NOT_STARTED, "Find " + BearFair21NPC.JOSE.getNpcNameAndJob() + " outside the Coco village");
			put(QuestStage.STARTED, "Talk to " + BearFair21NPC.SANTIAGO.getNpcNameAndJob());
			put(QuestStage.STEP_ONE, "Find " + BearFair21NPC.ANA.getNpcNameAndJob());
			put(QuestStage.STEP_TWO, "Talk to " + BearFair21NPC.ANA.getNpcNameAndJob());
			put(QuestStage.STEP_THREE, "Talk to " + BearFair21NPC.ANA.getNpcNameAndJob());
			put(QuestStage.STEPS_DONE, "Talk to " + BearFair21NPC.JOSE.getNpcNameAndJob());
		}}),
		SUMMER_DOWN_UNDER(user -> new HashMap<>() {{
			put(QuestStage.NOT_STARTED, "Find " + BearFair21NPC.BRUCE.getNpcNameAndJob() + " by the ute");
			put(QuestStage.STARTED, "Talk to " + BearFair21NPC.KYLIE.getNpcNameAndJob());
			put(QuestStage.STEP_ONE, "Get wheat for " + BearFair21NPC.KYLIE.getNpcNameAndJob());
			put(QuestStage.STEP_TWO, "Milk Daisy the cow");
			put(QuestStage.STEP_THREE, "Talk to " + BearFair21NPC.MEL_GIBSON.getNpcNameAndJob());
			put(QuestStage.STEP_FOUR, "Talk to " + BearFair21NPC.MILO.getNpcNameAndJob());
			put(QuestStage.STEP_FIVE, "Collect 7 feathers and bring them to " + BearFair21NPC.BRUCE.getNpcNameAndJob());
			put(QuestStage.STEP_SIX, "Head down to the cave");
			put(QuestStage.STEP_SEVEN, "Head deeper into the cave");
			put(QuestStage.STEPS_DONE, "Talk to the townsfolk");
			put(QuestStage.FOUND_ALL, "Talk to " + BearFair21NPC.BRUCE.getNpcNameAndJob());
		}});

		private final Function<BearFair21User, Map<QuestStage, String>> instructions;
	}

	@Permission("group.admin")
	@Path("setQuestStage <quest> <stage> [player]")
	void setQuestStage(BearFair21UserQuestStageHelper quest, QuestStage stage, @Arg("self") BearFair21User player) {
		userService.edit(player, user -> quest.setter.accept(user, stage));
		send(PREFIX + (isSelf(player) ? "Your" : player.getNickname() + "'s") + " " + camelCase(quest) + " quest stage to set to " + camelCase(stage));
	}

	@Permission("group.admin")
	@Path("mgn scrambledCables")
	void scrambledCables() {
		new ScrambledCablesMenu().open(player());
	}

	@Permission("group.admin")
	@Path("mgn router")
	void router() {
		new RouterMenu().open(player());
	}

	@Permission("group.admin")
	@Path("mgn solder reset")
	void solderReset() {
		MinigameNightIsland.setActiveSolder(false);
		send("Solder reset");
	}

	@Confirm
	@Permission("group.admin")
	@Path("clientside category remove [category]")
	void clientsideClear(ContentCategory category) {
		BearFair21User user = userService.get(uuid());
		if (category == null) {
			user.getContentCategories().clear();
			userService.save(user);

			send("removed all locations from " + user.getNickname());
			return;
		} else {
			Set<ContentCategory> categories = user.getContentCategories();
			categories.remove(category);
			user.setContentCategories(categories);
		}

		userService.save(user);
		send("removed " + category + " Content Category");
	}

	@Permission("group.admin")
	@Path("clientside category add <category> [player]")
	void clientsideAddAll(ContentCategory category, @Arg("self") Player player) {
		BearFair21User user = userService.get(player);
		Set<ContentCategory> categories = user.getContentCategories();

		categories.add(category);
		user.setContentCategories(categories);
		userService.save(user);

		send(player.getName() + " visible categories: " + Arrays.toString(user.getContentCategories().toArray()));

		ClientsideContentManager.sendSpawnContent(player, contentService.getList(category));
	}

	@Confirm
	@Permission("group.admin")
	@Path("clientside clear <category>")
	void clientsideClearCategory(ContentCategory category) {
		clientsideContent.getContentList().removeIf(content -> content.getCategory() == category);
		contentService.save(clientsideContent);
		send("Cleared category");
	}

	@Permission("group.admin")
	@Path("clientside new <category>")
	void clientsideNew(ContentCategory category) {
		Entity entity = getTargetEntity();
		if (entity == null) {
			Block block = getTargetBlock();
			if (BlockUtils.isNullOrAir(block))
				error("Entity is null && Block is null or air");

			setupBlockContent(block, category);
			send("Added block: " + block.getType());
		} else if (entity instanceof ItemFrame) {
			setupItemFrameContent((ItemFrame) entity, category);
			send("Added item frame");
		} else {
			error("That's not a supported entity type: " + entity.getType().name());
		}
	}

	@Permission("group.admin")
	@Path("clientside new schematic <category> <schematic>")
	void clientsideNew(ContentCategory category, String schematic) {
		setupSchematicContent(location(), schematic, category);
		send("Added schematic " + schematic);
	}

	@Permission("group.admin")
	@Path("clientside new current <category>")
	void clientsideNewCurrent(ContentCategory category) {
		setupBlockContent(block(), category);
		send("Added block: " + block().getType());
	}

	@Permission("group.admin")
	@Path("clientside list")
	void clientsideList() {
		List<Content> food = new ArrayList<>();
		List<Content> balloons = new ArrayList<>();
		List<Content> festoon = new ArrayList<>();
		List<Content> banners = new ArrayList<>();
		List<Content> presents = new ArrayList<>();
		List<Content> sawmill = new ArrayList<>();
		List<Content> cable = new ArrayList<>();
		List<Content> unlisted = new ArrayList<>();

		for (Content content : contentList) {
			switch (content.getCategory()) {
				case FOOD -> food.add(content);
				case BALLOON -> balloons.add(content);
				case FESTOON -> festoon.add(content);
				case BANNER -> banners.add(content);
				case PRESENT -> presents.add(content);
				case SAWMILL -> sawmill.add(content);
				case CABLE -> cable.add(content);
				default -> unlisted.add(content);
			}
		}

		send("Food: " + food.size());
		send("Balloons: " + balloons.size());
		send("Festoon: " + festoon.size());
		send("Banner: " + banners.size());
		send("Present: " + presents.size());
		send("SawMill: " + sawmill.size());
		send("Cable: " + cable.size());
		send("Unlisted: " + unlisted.size());

		List<Content> contentList = new ArrayList<>();
		contentList.addAll(food);
		contentList.addAll(balloons);
		contentList.addAll(festoon);
		contentList.addAll(banners);
		contentList.addAll(presents);
		contentList.addAll(sawmill);
		contentList.addAll(cable);
		contentList.addAll(unlisted);

		StringBuilder string = new StringBuilder();
		for (Content content : contentList) {
			string.append("\n")
					.append(StringUtils.camelCase(content.getCategory().name()))
					.append(": ")
					.append(StringUtils.camelCase(content.getMaterial().name()))
					.append(" - ")
					.append(StringUtils.getShortLocationString(content.getLocation()));
		}

		String url = StringUtils.paste(string.toString());
		send(json("&e&l[Click to Open]").url(url).hover(url));
	}

//	@Permission("group.admin")
//	@Path("clientside remove")
//	void clientsideRemove() {
//		int count = 0;
//		for (Content content : contentList) {
//			if (content.getLocation().equals(location().toBlockLocation())) {
//				contentList.remove(content);
//				count++;
//			}
//		}
//
//		if (count == 0)
//			error("There is no content at " + StringUtils.getShortLocationString(location().toBlockLocation()));
//
//		clientsideContent.setContentList(contentList);
//		contentService.save(clientsideContent);
//	}

	private void setupBlockContent(Block block, ContentCategory category) {
		ClientsideContent.Content content = new Content();
		content.setLocation(block.getLocation().toBlockLocation());
		content.setCategory(category);
		content.setMaterial(block.getType());
		if (block.getBlockData() instanceof Directional)
			content.setBlockFace(((Directional) block.getBlockData()).getFacing());
		addContent(content);
	}

	private void setupSchematicContent(Location location, String schematic, ContentCategory category) {
		ClientsideContent.Content content = new Content();
		content.setLocation(location.toBlockLocation());
		content.setCategory(category);
		content.setSchematic(schematic);
		addContent(content);
	}

	private void setupItemFrameContent(ItemFrame itemFrame, ContentCategory category) {
		ClientsideContent.Content content = new Content();
		content.setLocation(itemFrame.getLocation().toBlockLocation());
		content.setCategory(category);
		content.setMaterial(Material.ITEM_FRAME);
		content.setItemStack(itemFrame.getItem());
		content.setBlockFace(itemFrame.getFacing());
		content.setRotation(itemFrame.getRotation());
		addContent(content);

	}

	private void addContent(ClientsideContent.Content content) {
		for (Content _content : contentList) {
			if (_content.getLocation().equals(content.getLocation()))
				error("Duplicate content location");
		}

		contentList.add(content);
		clientsideContent.setContentList(contentList);
		contentService.save(clientsideContent);
	}

	@Path("stats")
	@Permission("group.admin")
	void stats() {
		// Total time played
		// % of time spent at bf vs other worlds
		// unique visitors
		// % of people who logged in that visited bf
		// % of playtime per world during bf
		// % at each quest stage of each quest
	}
}
