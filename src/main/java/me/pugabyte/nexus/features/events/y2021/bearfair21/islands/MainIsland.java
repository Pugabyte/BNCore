package me.pugabyte.nexus.features.events.y2021.bearfair21.islands;

import eden.utils.TimeUtils.Time;
import lombok.Getter;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.events.annotations.Region;
import me.pugabyte.nexus.features.events.models.BearFairIsland.NPCClass;
import me.pugabyte.nexus.features.events.models.QuestStage;
import me.pugabyte.nexus.features.events.y2021.bearfair21.BearFair21;
import me.pugabyte.nexus.features.events.y2021.bearfair21.BearFair21.BF21PointSource;
import me.pugabyte.nexus.features.events.y2021.bearfair21.Quests;
import me.pugabyte.nexus.features.events.y2021.bearfair21.fairgrounds.Seeker;
import me.pugabyte.nexus.features.events.y2021.bearfair21.islands.MainIsland.MainNPCs;
import me.pugabyte.nexus.features.events.y2021.bearfair21.quests.BearFair21TalkingNPC;
import me.pugabyte.nexus.features.events.y2021.bearfair21.quests.clientside.ClientsideContentManager;
import me.pugabyte.nexus.features.events.y2021.bearfair21.quests.npcs.BearFair21NPC;
import me.pugabyte.nexus.features.events.y2021.bearfair21.quests.npcs.Merchants;
import me.pugabyte.nexus.features.events.y2021.bearfair21.quests.resources.fishing.FishingLoot;
import me.pugabyte.nexus.features.events.y2021.bearfair21.quests.resources.fishing.FishingLoot.FishingLootCategory;
import me.pugabyte.nexus.features.resourcepack.ResourcePack;
import me.pugabyte.nexus.models.bearfair21.BearFair21User;
import me.pugabyte.nexus.models.bearfair21.BearFair21UserService;
import me.pugabyte.nexus.models.bearfair21.ClientsideContent.Content.ContentCategory;
import me.pugabyte.nexus.models.trophy.Trophy;
import me.pugabyte.nexus.utils.AdventureUtils;
import me.pugabyte.nexus.utils.BlockUtils;
import me.pugabyte.nexus.utils.ItemBuilder;
import me.pugabyte.nexus.utils.ItemUtils;
import me.pugabyte.nexus.utils.MaterialTag;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.RandomUtils;
import me.pugabyte.nexus.utils.Tasks;
import me.pugabyte.nexus.utils.Utils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent.Status;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static me.pugabyte.nexus.features.events.y2021.bearfair21.quests.npcs.BearFair21NPC.*;
import static me.pugabyte.nexus.utils.ItemUtils.isSameHead;

@Region("main")
@NPCClass(MainNPCs.class)
public class MainIsland implements BearFair21Island {

	public MainIsland() {
		Nexus.registerListener(this);
	}

	private static final BearFair21UserService userService = new BearFair21UserService();
	//
	@Getter
	private static final ItemBuilder balloon_cyan = new ItemBuilder(Material.STICK).customModelData(21).undroppable();
	@Getter
	private static final ItemBuilder balloon_yellow = new ItemBuilder(Material.STICK).customModelData(18).undroppable();
	@Getter
	private static final ItemBuilder cakeItem = new ItemBuilder(Material.CAKE).name("Bear Fair Cake").customModelData(2).undroppable();
	@Getter
	private static final ItemBuilder gravwell = new ItemBuilder(Material.LODESTONE).name("Grav-Well").undroppable();
	@Getter
	private static final Supplier<ItemBuilder> queenLarvae = () -> ItemBuilder.fromHeadId("33827").name("Queen Larva").undroppable();
	@Getter
	private static final ItemBuilder replacementSaw = new ItemBuilder(Material.STONECUTTER).name("Replacement Saw").customModelData(2).undroppable();
	@Getter
	private static final ItemBuilder invitation = new ItemBuilder(Material.PAPER).name("Anniversary Event Invitation").customModelData(3).undroppable();
	@Getter
	private static final List<BearFair21NPC> invitees = Arrays.asList(ARCHITECT, ARTIST, BAKER, BARTENDER, BLACKSMITH, BOTANIST, CARPENTER, COLLECTOR,
		CURATOR, FISHERMAN1, INVENTOR, PASTRY_CHEF, SORCERER, LUMBERJACK, BEEKEEPER, FISHERMAN2, AERONAUT, ADMIRAL, ORGANIZER, TRADER, JAMES);

	public enum MainNPCs implements BearFair21TalkingNPC {
		WAKKAFLOCKA(BearFair21NPC.ORGANIZER) {
			@Override
			public List<String> getScript(BearFair21User user) {
				List<String> script = new ArrayList<>();
				ItemStack tool = getTool(user.getPlayer());

				if (!user.hasMet(this.getNpcId())) {
					script.add("Hello there, my name is WakkaFlocka, and I am an admin on Project Eden and the organizer for this event.");
					script.add("wait 100");
				} else if (isInviting(user, this.getNpcId(), tool)) {
					script.add(Quests.getThanks());
					invite(user, this.getNpcId(), tool);
					return script;
				}

				if (!ResourcePack.isEnabledFor(user.getOnlinePlayer())) {
					if (user.getOnlinePlayer().getResourcePackStatus() == Status.DECLINED) {
						script.add("\nI notice you declined our custom resource pack.");
						script.add("wait 60");
						script.add("I highly recommend enabling it, as there are dozens of custom models and textures used within the event.");
						script.add("wait 100");
						script.add("To enable the pack you must: &oEdit the server in your server list and change the \"Server Resource Packs\" option to either \"enabled\" or \"prompt\" \n");
						script.add("wait 160");
					} else {
						script.add("\nI notice you don't have our custom resource pack enabled.");
						script.add("wait 60");
						script.add("I highly recommend enabling it, as there are dozens of custom models and textures used within the event.");
						script.add("wait 100");
						script.add("To enable the resource pack, type: &o/rp \n");
						script.add("wait 60");
					}
				}

				script.add("Welcome to Bear Fair, Project Eden's anniversary event!");
				script.add("wait 80");
				script.add("There are several islands to explore, find easter eggs, and quests to complete!");
				script.add("wait 80");
				script.add("At the carnival, you can play daily minigames in which you can play to gain Event Points.");
				script.add("wait 80");
				script.add("At any point you can buy unique prizes and perks using those points.");
				script.add("wait 80");
				script.add("There are many ways to acquire the currency here, you should talk to the local merchants.");
				script.add("wait 80");
				script.add("If you need help figuring out where you are, check out this map to my side.");
				script.add("wait 80");
				script.add("And lastly, make sure you have sounds and particles enabled!");

				user.getNextStepNPCs().remove(this.getNpcId());
				userService.save(user);
				return script;
			}
		},
		ADMIRAL(BearFair21NPC.ADMIRAL) {
			@Override
			public List<String> getScript(BearFair21User user) {
				List<String> script = new ArrayList<>();
				final Player player = user.getOnlinePlayer();
				ItemStack tool = getTool(player);

				if (user.getQuestStage_MGN() == QuestStage.STEP_SIX) {
					if (BearFair21.isInRegion(player, "bearfair21_minigamenight_gamegallery")) {
						script.add("<self> This is <player> at the Game Gallery?");
						script.add("wait 50");
						script.add("Hello, this is Admiral Phoenix aboard the F.S.S Stellar Tides. I was wondering if I might request your assistance on a pressing matter?");
						script.add("wait 120");
						script.add("<self> How might I be of service, Sir?");
						script.add("wait 50");
						script.add("I ferry travelers to and from Bear Fair Island and on my last voyage I noticed my nav computer no longer detected the island on the star map.");
						script.add("wait 120");
						script.add("I suspect something may have happened to the nav beacons.");
						script.add("wait 70");
						script.add("<self> Oh no... What are nav beacons exactly?");
						script.add("wait 60");
						script.add("They are broadcasting stations that triangulate their position to any ship's nav computer so they can find Bear Fair.");
						script.add("wait 110");
						script.add("I need someone to check on the beacons, inspect for damage, and reboot them.");
						script.add("wait 80");
						script.add("<self> Sounds easy enough, where can I find them?");
						script.add("wait 70");
						script.add("There are three of them located at each corner of the main island.");
						script.add("wait 70");
						script.add("<self> Alright I'll see what I can find.");
						script.add("wait 50");
						script.add("Good, report back as soon as possible. I'll be on the deck of the Stellar Tides.");
						user.getNextStepNPCs().add(getNpcId());
						userService.save(user);
					} else {
						if (user.getMgn_beaconsActivated().size() == 3) {
							script.add("Welcome aboard.");
							script.add("wait 50");
							script.add("<self> All beacons are in good condition and fully operational.");
							script.add("wait 70");
							script.add("Hmmm. I'm still not getting the nav data. Perhaps there's some kind of interference...");
							script.add("wait 80");
							script.add("Scans indicate there is some significant geothermal activity currently on the island...");
							script.add("wait 80");
							script.add("That could be worse than just nav beacon interference...");
							script.add("wait 70");
							script.add("<self> Oh no... is the island becoming unstable?");
							script.add("wait 70");
							script.add("It appears to be trending that way... Here, take this. It's a portable grav-well.");
							script.add("wait 60");
							int wait = 50 + 70 + 80 + 80 + 70 + 70 + 60;
							Tasks.wait(wait, () -> {
								if (!player.getInventory().containsAtLeast(gravwell.build(), 1))
									Quests.giveItem(player, gravwell.build());
							});
							script.add("wait 30");
							script.add("<self> Whoa... This is a little heavy.");
							script.add("wait 50");
							script.add("Get as close to the magma core as you can, and place this down. It will hold the island together until we can get some professionals out here.");
							script.add("wait 120");
							script.add("On top of that, the magnetic field this device generates should boost the beacons' signal and solve the interference issue.");
							script.add("wait 110");
							script.add("<self> Aye Aye, Admiral!");
						} else {
							script.add("Any luck with those nav-beacons? Make sure to check all three for damage and reboot them.");
						}
					}
				} else if (user.getQuestStage_MGN() == QuestStage.STEP_SEVEN) {
					script.add("<self> Mission complete!");
					script.add("wait 50");
					script.add("Good work, I’m reading the nav beacons now. I’ll contact the Federation Science Division and get a team out here to settle the geothermal activity and re-stabilize the island.");
					script.add("wait 130");
					script.add("Thank you for your help <player>, You’ve saved Bear Fair and definitely earned your pay.");
					script.add("wait 70");
					int wait = 50 + 130 + 70;
					Tasks.wait(wait, () -> Quests.pay(user, Merchants.goldIngot.clone().amount(6).build()));
					script.add("wait 30");
					script.add("<self> Thank You Sir!");
					Tasks.wait(wait + 30, () -> {
						user.getNextStepNPCs().remove(getNpcId());
						user.setQuestStage_MGN(QuestStage.STEP_EIGHT);
						userService.save(user);
					});
				} else if (!user.hasMet(this.getNpcId())) {
					script.add("The name is Phoenix, Admiral Phoenix and my job is to keep all y'all people here safe.");
					script.add("wait 90");
					script.add("If you see anything suspicious simply let me know. I am more than capable of handling things myself.");
					script.add("wait 110");
					script.add("Now you just continue to have fun, got it?");
				} else if (isInviting(user, this.getNpcId(), tool)) {
					script.add(Quests.getThanks());
					invite(user, this.getNpcId(), tool);
				} else {
					script.add(Quests.getHello());
				}

				return script;
			}
		},
		ARCHITECT(BearFair21NPC.ARCHITECT) {
			@Override
			public List<String> getScript(BearFair21User user) {
				List<String> script = new ArrayList<>();
				ItemStack tool = getTool(user.getPlayer());

				if (user.getQuestStage_MGN() == QuestStage.STEP_FIVE) {
					if (BearFair21.isInRegion(user.getOnlinePlayer(), "bearfair21_minigamenight_gamegallery")) {
						script.add("<self> Thanks for calling GG! How can I help?");
						script.add("wait 50");
						script.add("Hey, this is Zach over in Honeywood, on the main island. Me and my team are building a new house on the edge of town.");
						script.add("wait 80");
						script.add("Well we're in a bit of a bind now since my electrician bailed on me this morning.");
						script.add("wait 70");
						script.add("<self> Well that wasn't very professional of them.");
						script.add("wait 50");
						script.add("Right? Now I know GG is a videogame company, but from what I've heard, y'all are pretty good with tech repair too.");
						script.add("wait 80");
						script.add("It's a bit of an odd request, but could you spare some one over here to set up the internet?");
						script.add("wait 70");
						script.add("I'll pay double whatever your typical service fee is since this isn't your normal repair job.");
						script.add("wait 70");
						script.add("<self> Uh, sure I could give it a look. Can't be more complicated than a motherboard...");
						script.add("wait 70");
						script.add("Great! It shouldn't take too long. We'll have everything ready for you when you get here.");
						user.getNextStepNPCs().add(getNpcId());
						userService.save(user);
						return script;
					} else {
						List<Component> tasks = new ArrayList<>();
						if (!user.isMgn_connectWiring())
							tasks.add(Component.text("connect the fiber cable"));
						if (!user.isMgn_unscrambledWiring())
							tasks.add(Component.text("unscramble the wiring"));
						if (!user.isMgn_setupRouter())
							tasks.add(Component.text("set up the router"));

						if (tasks.isEmpty()) {
							script.add("Awesome! That was some quick work, buddy. Here's your pay, and yes, I'm paying you double.");
							script.add("wait 40");
							Tasks.wait(40, () -> Quests.pay(user, Merchants.goldIngot.clone().amount(4).build()));
							script.add("wait 40");
							script.add("Tell your manager to consider it a donation. Take care now.");
							script.add("wait 50");
							script.add("<self> It was no problem, happy to help wherever I can!");
							Tasks.wait(40 + 40 + 50, () -> {
								user.setQuestStage_MGN(QuestStage.STEP_SIX);
								user.getNextStepNPCs().remove(getNpcId());
								userService.save(user);
							});
						} else {
							script.add("Hey thanks for coming. All we need you to do is " + AdventureUtils.asPlainText(AdventureUtils.commaJoinText(tasks)) + ".");
							script.add("wait 70");
							script.add("You'll find the main cable over by the tree, the wires are under the house- watch your step btw");
							script.add("wait 70");
							script.add("And the router station is right there on the table.");
						}

						return script;
					}
				} else if (!user.hasMet(this.getNpcId())) {
					script.add("Hm, are you admiring the scenery as well?");
					script.add("wait 70");
					script.add("There is honestly nothing more stunning than an area bustling with life.");
					script.add("wait 90");
					script.add("Nice to meet you, my name is Zach and I love architecture with all my heart.");
					return script;
				} else if (isInviting(user, this.getNpcId(), tool)) {
					script.add(Quests.getThanks());
					invite(user, this.getNpcId(), tool);
					return script;
				}

				script.add(Quests.getHello());
				return script;
			}
		},
		JAMES(BearFair21NPC.JAMES) {
			@Override
			public List<String> getScript(BearFair21User user) {
				List<String> script = new ArrayList<>();
				ItemStack tool = getTool(user.getPlayer());

				if (user.getQuestStage_MGN() == QuestStage.STEP_EIGHT && !user.isMgn_boughtCar()) {
					script.add("Hey, interested in the car? Well I gotta warn you there's no posted price because it's been totaled.");
					script.add("wait 110");
					script.add("I'm just sellin' it for salvage so if you see any parts you'd like, we can talk price.");
					script.add("wait 90");
					script.add("<self> Actually... how's the sound-system?");
					script.add("wait 60");
					script.add("Well it was totaled by water damage so the front speakers are toast.");
					script.add("wait 80");
					script.add("The rear speakers actually managed to survive though so if you're cool with half a sound-system, I'd say 1 gold block aughta' cover it.");
					script.add("wait 120");
					script.add("<self> I'll take it!");
					script.add("wait 70");
					return script;
				} else {
					if (!user.hasMet(this.getNpcId())) {
						script.add(Quests.getHello());
					} else if (isInviting(user, this.getNpcId(), tool)) {
						script.add(Quests.getThanks());
						invite(user, this.getNpcId(), tool);
					} else {
						script.add(Quests.getHello());
					}

					script.add("<exit>");
					return script;
				}
			}
		},
		CARPENTER(BearFair21NPC.CARPENTER) {
			@Override
			public List<String> getScript(BearFair21User user) {
				List<String> script = new ArrayList<>();
				ItemStack tool = getTool(user.getPlayer());

				if(user.getQuestStage_MGN() == QuestStage.STEP_FIVE) {
					if(RandomUtils.chanceOf(50))
						script.add("I only have two loves in life, woodworking, and steak.");
					else
						script.add("Give 100 percent. One-hundred-and-ten percent is impossible. Only idiots recommend that.");
					return script;
				} else if (!user.hasMet(this.getNpcId())) {
					script.add("I swear, there is nothing like the smell of sawdust to wake you up in the morning.");
					script.add("wait 80");
					script.add("How do you do friend, the name is Ron and as this town's carpenter, my job is to create masterpieces everyday!");
					return script;
				} else if (isInviting(user, this.getNpcId(), tool)) {
					script.add(Quests.getThanks());
					invite(user, this.getNpcId(), tool);
					return script;
				}

				script.add(Quests.getHello());
				return script;
			}
		},
		CURATOR(BearFair21NPC.CURATOR) {
			@Override
			public List<String> getScript(BearFair21User user) {
				List<String> script = new ArrayList<>();
				ItemStack tool = getTool(user.getPlayer());

				if (!user.hasMet(this.getNpcId())) {
					script.add("Hello! I'm the curator of these unique mechanisms.");
					script.add("wait 60");
					script.add("I don't quite understand their purpose, but they seem to have value to my boss!");
					script.add("wait 80");
					script.add("I seem to have misplaced one of the items on display.");
					script.add("wait 60");
					script.add("It looks like a generic crimson button, but it's much more valuable than that.");
					script.add("wait 80");
					script.add("Any chance you can help me find it? It's somewhere in this tent.");
					Seeker.addPlayer(user.getOnlinePlayer());
					return script;
				} else if (isInviting(user, this.getNpcId(), tool)) {
					script.add(Quests.getThanks());
					script.add("<exit>");
					invite(user, this.getNpcId(), tool);
					return script;
				} else if (BearFair21.getDailyTokensLeft(user.getPlayer(), BF21PointSource.SEEKER, 25) <= 0) {
					script.add("I seem to have lost the crimson button again. Can help me find it? It's somewhere in this tent.");

					if (!Seeker.isPlaying(user.getOnlinePlayer()))
						Seeker.addPlayer(user.getOnlinePlayer());
					return script;
				}

				script.add(Quests.getHello());
				return script;
			}
		},
		// Side Quests
		BEEKEEPER(BearFair21NPC.BEEKEEPER) {
			@Override
			public List<String> getScript(BearFair21User user) {
				List<String> script = new ArrayList<>();
				ItemStack tool = getTool(user.getPlayer());

				if (!user.hasMet(this.getNpcId())) {
					script.add("Hm did you say something? Apologies, my hearing isn't quite what it used to be.");
					script.add("wait 80");
					script.add("Feel free to call me Harold, I'm the Beekeeper around these parts and I have been for many years.");
					script.add("wait 100");
					script.add("I honestly wouldn't have it any other way.");
					script.add("wait 60");
					script.add("Are you by chance a fan of honey?");
					return script;
				} else if (isInviting(user, this.getNpcId(), tool)) {
					script.add(Quests.getThanks());
					invite(user, this.getNpcId(), tool);
					return script;
				} else if (user.getQuestStage_BeeKeeper() == QuestStage.NOT_STARTED || user.getQuestStage_BeeKeeper() == QuestStage.STARTED) {
					script.add("Would you mind doing me a favor actually? I've been looking to get myself a queen bee larvae to jump start my colony here.");
					script.add("wait 120");
					script.add("I would look myself, but my body cetainly isn't as young and energetic as it once was.");
					script.add("wait 80");
					script.add("And a quest like this, why its not exactly easy on ones self now is it?");
					script.add("wait 80");
					script.add("Haha, don't be worried, a young, strong adventurer like you will have no trouble at all.");
					script.add("wait 80");
					script.add("You should be able to grab one in the beehive on the island.");
					script.add("wait 60");
					script.add("Good luck!");

					user.setQuestStage_BeeKeeper(QuestStage.STARTED);
					user.getNextStepNPCs().add(QUEEN_BEE.getNpcId());
					userService.save(user);
					return script;
				} else if (user.getQuestStage_BeeKeeper() == QuestStage.STEPS_DONE) {
					ItemStack item = null;
					for (ItemStack itemStack : PlayerUtils.getAllInventoryContents(user.getOnlinePlayer())) {
						if (ItemUtils.isNullOrAir(itemStack)) continue;
						if (!Material.PLAYER_HEAD.equals(itemStack.getType())) continue;

						if (isSameHead(queenLarvae.get().build(), itemStack)) {
							item = itemStack;
							break;
						}
					}

					if (item == null) {
						script.add("Have you found the queen bee larvae?");
						return script;
					}

					Quests.removeItemStacks(user, Collections.singletonList(item));
					script.add(Quests.getThanks());
					Tasks.wait(Time.SECOND.x(2), () -> Quests.giveKey(user));

					user.getNextStepNPCs().remove(this.getNpcId());
					user.setQuestStage_BeeKeeper(QuestStage.COMPLETE);
					userService.save(user);
					return script;
				}

				List<String> facts = Arrays.asList(
					"Did you know, honey bees fly at up to 15 miles per hour?",
					"Did you know, bees have an exceptional sense of smell.",
					"Did you know, Bombus Dahlbomii are the world’s largest bumblebees",
					"Did you know, it takes one ounce of honey to fuel a bee’s flight around the world, they're quite efficient.",
					"Did you know, the honey bee’s wings stroke 11,400 times per minute, thus making their distinctive buzz.",
					"Did you know, the honey bee is the only insect that produces food eaten by man."
				);
				script.add(RandomUtils.randomElement(facts));
				return script;
			}
		},
		QUEEN_BEE(BearFair21NPC.QUEEN_BEE) {
			@Override
			public List<String> getScript(BearFair21User user) {
				List<String> script = new ArrayList<>();

				if (!user.isHiveAccess()) {
					script.add("Now where do you think you're going?");
					script.add("wait 40");
					script.add("Do you really think after coming here and disturbing our peace you can just waltz in here?");
					script.add("wait 100");
					script.add("You people never fail to amaze me.");
					script.add("wait 40");
					script.add("Maybe if you bring us a gift to make up for the stress you've caused here, I will consider letting you inside.");
					script.add("wait 100");
					script.add("It's honestly the least you could do.");
					script.add("wait 40");
					script.add("Come back with 1 of each small flower, and then I'll call it even.");

					return script;
				} else if (user.getQuestStage_BeeKeeper() == QuestStage.COMPLETE) {
					return script;
				} else if (user.getQuestStage_BeeKeeper() == QuestStage.STEP_ONE) {
					script.add("Head down into the nursery once you are ready to do so and take one of the larvae.");
					return script;
				} else if (user.isHiveAccess() && user.getQuestStage_BeeKeeper() != QuestStage.STEPS_DONE) {
					script.add("What brings you here to my grand halls, traveler?");
					script.add("wait 60");
					script.add("<self> Hello your majesty, I humbly request a queen bee larvae, for Harold.");
					script.add("wait 80");
					script.add("Hmm I see, that I may be able to do.");
					script.add("wait 40");
					script.add("You're gift has not gone unnoticed by my bees here.");
					script.add("wait 40");
					script.add("So while I usually wouldn't give a queen bee larvae to a random visitor, compensation for your hard work seems appropriate.");
					script.add("wait 120");
					script.add("Do me a favor and tell Harold that I expect him to take good care of his colony.");
					script.add("wait 80");
					script.add("If it wasn't for his kind reputation when it comes caring for his bees I would not have been so willing to allow your request.");
					script.add("wait 120");
					script.add("Head down into the nursery once you are ready to do so and take one.");
					script.add("wait 80");
					script.add("I wish you safe travels.");

					user.setQuestStage_BeeKeeper(QuestStage.STEP_ONE);
					user.getNextStepNPCs().remove(BEEKEEPER.getNpcId());
					userService.save(user);
					return script;
				}

				return script;
			}
		},
		FISHERMAN2(BearFair21NPC.FISHERMAN2) {
			@Override
			public List<String> getScript(BearFair21User user) {
				List<String> script = new ArrayList<>();
				ItemStack tool = getTool(user.getPlayer());

				if (!user.hasMet(this.getNpcId())) {
					script.add("I swear there is nothing more beautiful than the open water.");
					script.add("wait 80");
					script.add("It's somehow both a symbol of dangerous adventure and complete calm.");
					script.add("wait 80");
					script.add("My name is Nate by the way and honestly I can see why the fish would be happy to live out there.");
					script.add("wait 100");
					script.add("However I suppose I can't put too much thought into it, as a fisherman it is my job to pull them out of that paradise, isn't it?");
					return script;
				} else if (isInviting(user, this.getNpcId(), tool)) {
					script.add(Quests.getThanks());
					invite(user, this.getNpcId(), tool);
					return script;
				} else if (user.getQuestStage_Recycle() == QuestStage.NOT_STARTED) {
					script.add("Local kids have been throwing trash into the waters here, ruining the ecosystem.");
					script.add("wait 80");
					script.add("While fishing, would you mind throwing the trash you catch into this recycler?");
					script.add("wait 80");
					script.add("Recycling trash can get you more useful materials, and will decrease the chance of catching more trash.");

					user.setQuestStage_Recycle(QuestStage.STARTED);
					userService.save(user);
					return script;
				}

				script.add("You've recycled " + user.getRecycledItems() + " trash so far.");
				return script;
			}
		},
		// Main Quest
		MAYOR(BearFair21NPC.MAYOR) {
			@Override
			public List<String> getScript(BearFair21User user) {
				List<String> script = new ArrayList<>();

				if (!user.hasMet(this.getNpcId())) {
					script.add("Welcome to Honeywood!");
					script.add("wait 40");
					script.add("I would love to give you a tour, but I'm swamped in preparation work for the upcoming anniversary event.");
					script.add("wait 80");
					script.add("You know, if you want to give a good first impression, helping me out would certainly do the trick.");
					script.add("wait 80");
					script.add("When you get some spare time, come back and talk to me. I'd greatly appreciate the help.");

					return script;
				} else {
					switch (user.getQuestStage_Main()) {
						case NOT_STARTED -> {
							script.add("Oh splendid, I could really use some help gathering the necessary decorations, I've got a few tasks for you.");
							script.add("wait 100");
							script.add("For your first task, could you gather the materials, and craft me 4 cyan & 4 yellow banners? I've seem to forgotten the recipe.");
							user.setQuestStage_Main(QuestStage.STARTED);
							userService.save(user);
							return script;
						}
						case STARTED -> {
							List<ItemBuilder> required = Arrays.asList(
								new ItemBuilder(Material.CYAN_BANNER).amount(4),
								new ItemBuilder(Material.YELLOW_BANNER).amount(4));
							if (!Quests.hasAllItemsLikeFrom(user, required)) {
								script.add("For your first task, could you gather the materials, and craft me 4 cyan & 4 yellow banners? I've seem to forgotten the recipe.");
								return script;
							}

							Quests.removeItems(user, required);
							script.add(Quests.getThanks());
							script.add("wait 40");
							script.add("I'm gonna work on hanging these around the village, could you now gather me 16 cyan & 16 yellow balloons? Last I heard, you could get some from Skye, the Aeronaut.");

							ClientsideContentManager.addCategory(user, ContentCategory.BANNER, Time.SECOND.x(10));
							user.setQuestStage_Main(QuestStage.STEP_ONE);
							user.getNextStepNPCs().add(AERONAUT.getNpcId());
							userService.save(user);
							return script;
						}
						case STEP_ONE, STEP_TWO -> {
							List<ItemBuilder> required = Arrays.asList(balloon_cyan.clone().amount(16), balloon_yellow.clone().amount(16));
							if (!Quests.hasAllItemsLikeFrom(user, required)) {

								script.add("For your next task, could you gather me 16 cyan & 16 yellow balloons? Last I heard, you could get some from Skye, the Aeronaut.");
								return script;
							}

							Quests.removeItems(user, required);
							script.add(Quests.getThanks());
							script.add("wait 40");
							script.add("It'll take me some time to tie these up around the village. Could you gather me 32 White Wool and 8 of each red, green, and blue dyes in the meantime?");

							ClientsideContentManager.addCategory(user, ContentCategory.BALLOON, Time.SECOND.x(10));
							user.setQuestStage_Main(QuestStage.STEP_THREE);
							user.getNextStepNPCs().remove(AERONAUT.getNpcId());
							userService.save(user);
							return script;
						}
						case STEP_THREE -> {
							List<ItemBuilder> required = Arrays.asList(new ItemBuilder(Material.WHITE_WOOL).amount(32),
								new ItemBuilder(Material.RED_DYE).amount(8),
								new ItemBuilder(Material.GREEN_DYE).amount(8),
								new ItemBuilder(Material.BLUE_DYE).amount(8));

							if (!Quests.hasAllItemsLikeFrom(user, required)) {
								script.add("For your next task, could you gather me 32 White Wool and 8 of each red, green, and blue dyes?");
								return script;
							}

							Quests.removeItems(user, required);
							script.add(Quests.getThanks());
							script.add("wait 40");
							script.add("While I'm busy hanging these around the island, could you follow up with Maple the Pastry Chef about my cake order?");

							ClientsideContentManager.addCategory(user, ContentCategory.FESTOON, Time.SECOND.x(10));
							user.setQuestStage_Main(QuestStage.STEP_FOUR);
							user.getNextStepNPCs().add(PASTRY_CHEF.getNpcId());
							userService.save(user);
							return script;
						}
						case STEP_FOUR -> {
							List<ItemBuilder> required = Collections.singletonList(cakeItem);
							if (!Quests.hasItemsLikeFrom(user, required)) {
								script.add("For your next task, could you follow up with Maple the Pastry Chef about my cake order?");
								return script;
							}

							Quests.removeItems(user, required);
							script.add(Quests.getThanks());
							script.add("wait 40");
							script.add("That's almost everything, there's just one last task I need you to do, while I'm finishing up.");
							script.add("wait 80");
							script.add("I had these invitations custom made, could you go around the island and give one to each of the townspeople, while I finish up here?");
							Tasks.wait(140, () -> Quests.giveItem(user, invitation.clone().amount(invitees.size()).build()));

							ClientsideContentManager.addCategory(user, ContentCategory.FOOD, Time.SECOND.x(10));
							user.setQuestStage_Main(QuestStage.STEP_FIVE);
							user.getNextStepNPCs().remove(PASTRY_CHEF.getNpcId());
							userService.save(user);
							return script;
						}
						case STEP_FIVE -> {
							script.add("I had those invitations custom made, could you go around the island and give one to each of the townspeople?");
							return script;
						}
						case STEP_SIX -> {
							script.add("You're a life saver, thank you! And as a token of my gratitude, have this...");
							Tasks.wait(40, () -> {
								Quests.giveKey(user);
								Quests.giveTrophy(user, Trophy.BEAR_FAIR_2021);
								BearFair21.giveTokens(user, 200);
							});

							user.setQuestStage_Main(QuestStage.COMPLETE);
							user.getNextStepNPCs().remove(this.getNpcId());
							userService.save(user);
							return script;
						}
						case COMPLETE -> {
							script.add("Thanks for all the hard work!");
							return script;
						}
					}
				}

				script.add(Quests.getHello());
				return script;
			}

		},
		AERONAUT(BearFair21NPC.AERONAUT) {
			@Override
			public List<String> getScript(BearFair21User user) {
				List<String> script = new ArrayList<>();
				ItemStack tool = getTool(user.getPlayer());

				if (!user.hasMet(this.getNpcId())) {
					script.add("Hey there land dweller, how do you feel about exploring above the clouds?");
					script.add("wait 60");
					script.add("The names Skye and I'm an explorer of well... the skies! They call me an aeronaut, pretty cool right?");
					return script;
				} else if (isInviting(user, this.getNpcId(), tool)) {
					script.add(Quests.getThanks());
					script.add("<exit>");
					invite(user, this.getNpcId(), tool);
					return script;

				} else if (user.getQuestStage_Main() == QuestStage.STEP_ONE) {
					List<ItemBuilder> required = new ArrayList<>();
					Arrays.stream(FishingLoot.values())
						.filter(fishingLoot -> fishingLoot.getCategory().equals(FishingLootCategory.FISH) || fishingLoot.getCategory().equals(FishingLootCategory.UNIQUE))
						.toList().forEach(fishingLoot -> required.add(fishingLoot.getItemBuilder()));

					List<ItemStack> items = Quests.getItemsLikeFrom(user, required);
					if (Utils.isNullOrEmpty(items)) {
						script.add("Tell ya what, I spend most of my time up in the skies, but I'll trade you those balloons you want, if you can get me a catch from the depths.");
					} else {
						Quests.removeItem(user, RandomUtils.randomElement(items));
						script.add("Sweet, thanks! And here's you are.");

						Quests.giveItem(user, balloon_cyan.clone().amount(16).build());
						Quests.giveItem(user, balloon_yellow.clone().amount(16).build());

						user.setQuestStage_Main(QuestStage.STEP_TWO);
						user.getNextStepNPCs().remove(this.getNpcId());
						userService.save(user);
					}
					return script;
				}

				script.add(Quests.getHello());
				return script;
			}
		},
		// Merchants
		ARTIST(BearFair21NPC.ARTIST) {
			@Override
			public List<String> getScript(BearFair21User user) {
				List<String> script = new ArrayList<>();
				ItemStack tool = getTool(user.getPlayer());

				if (!user.hasMet(this.getNpcId())) {
					script.add("Ah, just the inspiration I was looking for. The name's Sage and I'm your local artist!");
					script.add("wait 60");
					script.add("Feel free to browse as long as you'd like. I would love to get to paint- I mean know you.");
					script.add("wait 60");
					return script;
				} else if (isInviting(user, this.getNpcId(), tool)) {
					script.add(Quests.getThanks());
					script.add("<exit>");
					invite(user, this.getNpcId(), tool);
					return script;
				}

				return script;
			}
		},
		BAKER(BearFair21NPC.BAKER) {
			@Override
			public List<String> getScript(BearFair21User user) {
				List<String> script = new ArrayList<>();
				ItemStack tool = getTool(user.getPlayer());

				if (!user.hasMet(this.getNpcId())) {
					script.add("Oh hi, my name is Rye and you may know me as the baker guy.");
					script.add("wait 60");
					script.add("I just took this loaf of bread out of the oven, if you are interested.");
					script.add("wait 60");
					return script;
				} else if (isInviting(user, this.getNpcId(), tool)) {
					script.add(Quests.getThanks());
					script.add("<exit>");
					invite(user, this.getNpcId(), tool);
					return script;
				}

				return script;
			}
		},
		BARTENDER(BearFair21NPC.BARTENDER) {
			@Override
			public List<String> getScript(BearFair21User user) {
				List<String> script = new ArrayList<>();
				ItemStack tool = getTool(user.getPlayer());

				if (!user.hasMet(this.getNpcId())) {
					script.add("Hey friend, can I interest you in a nice refreshing beverage.");
					script.add("wait 60");
					script.add("My name is Cosmo and drinks are my specialty. It's quite hot out there, and I think I've got just what you need.");
					script.add("wait 100");
					return script;
				} else if (isInviting(user, this.getNpcId(), tool)) {
					script.add(Quests.getThanks());
					script.add("<exit>");
					invite(user, this.getNpcId(), tool);
					return script;
				}

				return script;
			}
		},
		BLACKSMITH(BearFair21NPC.BLACKSMITH) {
			@Override
			public List<String> getScript(BearFair21User user) {
				List<String> script = new ArrayList<>();
				ItemStack tool = getTool(user.getPlayer());

				if (!user.hasMet(this.getNpcId())) {
					script.add("Can I help you with something?");
					script.add("wait 40");
					script.add("Oh you must be new around here, I'm Alvor and I'm a blacksmith that's pretty much all you need to know.");
					script.add("wait 100");
					script.add("If it needs a forge, I'm probably selling it, so take a look around I suppose.");
					script.add("wait 80");
					return script;
				} else if (isInviting(user, this.getNpcId(), tool)) {
					script.add(Quests.getThanks());
					script.add("<exit>");
					invite(user, this.getNpcId(), tool);
					return script;
				}

				return script;
			}
		},
		BOTANIST(BearFair21NPC.BOTANIST) {
			@Override
			public List<String> getScript(BearFair21User user) {
				List<String> script = new ArrayList<>();
				ItemStack tool = getTool(user.getPlayer());

				if (!user.hasMet(this.getNpcId())) {
					script.add("Oh hi there! I'm Fern and I absolutely love plants, probably why I decided to become a botanist in the first place.");
					script.add("wait 100");
					script.add("I have got a ton of beautiful plants for purchase and a story for each and every one of them.");
					script.add("wait 80");
					return script;
				} else if (isInviting(user, this.getNpcId(), tool)) {
					script.add(Quests.getThanks());
					script.add("<exit>");
					invite(user, this.getNpcId(), tool);
					return script;
				}

				return script;
			}
		},
		COLLECTOR(BearFair21NPC.COLLECTOR) {
			@Override
			public List<String> getScript(BearFair21User user) {
				List<String> script = new ArrayList<>();
				ItemStack tool = getTool(user.getPlayer());

				if (!user.hasMet(this.getNpcId())) {
					script.add("I'm Mercury, I'm a collector of many wondrous goods, old and new.");
					script.add("wait 60");
					script.add("Anything can be a treasure if your heart decides to give it value.");
					script.add("wait 60");
					script.add("Trade with me whenever you like, but be aware, I tend to wander.");
					script.add("wait 60");
					script.add("I may be here now, but I certainly won't be much longer.");
					script.add("wait 60");
					return script;
				} else if (isInviting(user, this.getNpcId(), tool)) {
					script.add(Quests.getThanks());
					script.add("<exit>");
					invite(user, this.getNpcId(), tool);
					return script;
				}

				return script;
			}
		},
		FISHERMAN1(BearFair21NPC.FISHERMAN1) {
			@Override
			public List<String> getScript(BearFair21User user) {
				List<String> script = new ArrayList<>();
				ItemStack tool = getTool(user.getPlayer());

				if (!user.hasMet(this.getNpcId())) {
					script.add("Have you ever been fishing before? There is nothing quite as peaceful as sitting by the water and waiting for a fish to bite.");
					script.add("wait 100");
					script.add("I'm Gage and I've been fishing for as long as I can remember. You should join me some time.");
					script.add("wait 80");
					return script;
				} else if (isInviting(user, this.getNpcId(), tool)) {
					script.add(Quests.getThanks());
					script.add("<exit>");
					invite(user, this.getNpcId(), tool);
					return script;
				}

				return script;
			}
		},
		INVENTOR(BearFair21NPC.INVENTOR) {
			@Override
			public List<String> getScript(BearFair21User user) {
				List<String> script = new ArrayList<>();
				ItemStack tool = getTool(user.getPlayer());

				if (!user.hasMet(this.getNpcId())) {
					script.add("...AH! Apologies I didn't see you there.");
					script.add("wait 40");
					script.add("Guess I was lost in my own world again. I'm Joshua and I'm always trying to come up with awesome new gadgets for people to use.");
					script.add("wait 100");
					script.add("I suppose that's why the people around here call me an inventor.");
					script.add("wait 60");
					script.add("What can I help you with?");
					script.add("wait 40");
					return script;
				} else if (isInviting(user, this.getNpcId(), tool)) {
					script.add(Quests.getThanks());
					script.add("<exit>");
					invite(user, this.getNpcId(), tool);
					return script;
				}

				return script;
			}
		},
		PASTRY_CHEF(BearFair21NPC.PASTRY_CHEF) {
			@Override
			public List<String> getScript(BearFair21User user) {
				List<String> script = new ArrayList<>();
				ItemStack tool = getTool(user.getPlayer());

				if (!user.hasMet(this.getNpcId())) {
					script.add("I'd say a little sugar never hurt anyone. A lot of sugar you say? Well I'd certainly be willing to risk it.");
					script.add("wait 100");
					script.add("I'm Maple and sweet treats are my passion! Cookies, cakes, pies, you name it, I've probably made it at some point.");
					script.add("wait 100");
					return script;
				} else if (user.getQuestStage_Main() == QuestStage.STEP_FOUR) {
					ItemStack item = Quests.getItemLikeFrom(user, new ItemBuilder(Material.CAKE));
					if (!ItemUtils.isNullOrAir(item))
						return script;

					script.add("Oh my goodness, I totally forgot! Nor do I have the necessary supplies for a cake that size.");
					script.add("wait 80");
					script.add("Would you mind baking the cake and gathering the cocoa beans for the frosting, and I'll decorate the cake to perfection?");
					script.add("wait 100");
					return script;
				} else if (isInviting(user, this.getNpcId(), tool)) {
					script.add(Quests.getThanks());
					script.add("<exit>");
					invite(user, this.getNpcId(), tool);
					return script;
				}

				return script;
			}
		},
		LUMBERJACK(BearFair21NPC.LUMBERJACK) {
			public List<String> getScript(BearFair21User user) {
				List<String> script = new ArrayList<>();
				ItemStack tool = getTool(user.getPlayer());

				if (!user.hasMet(this.getNpcId())) {
					script.add("I promise I really do love my job however chopping wood all day can be extremely tiring.");
					script.add("wait 80");
					script.add("This poor guy needs a break at some point too.");
					script.add("wait 40");
					script.add("Oh wait I don't think we've met before, sorry about that. My name is Flint and I'm a lumberjack.");
					script.add("wait 80");
					return script;
				} else if (isInviting(user, this.getNpcId(), tool)) {
					script.add(Quests.getThanks());
					script.add("<exit>");
					invite(user, this.getNpcId(), tool);
					return script;

				} else if (user.getQuestStage_Lumberjack() == QuestStage.NOT_STARTED) {
					script.add("Be careful around that broken saw mill, as you can see in the wall there.");
					script.add("wait 60");
					script.add("The mechanism can go haywire, nearly took my head off!");
					script.add("wait 60");
					script.add("Would you mind finding me a replacement saw? I'll pay handsomely.");
					script.add("wait 60");

					user.setQuestStage_Lumberjack(QuestStage.STARTED);
					userService.save(user);
					return script;
				} else if (user.getQuestStage_Lumberjack() == QuestStage.STARTED) {
					List<ItemBuilder> required = Collections.singletonList(replacementSaw.clone());
					if (!Quests.hasAllItemsLikeFrom(user, required)) {
						script.add("Would you mind finding me a replacement saw? I'll pay handsomely.");
						script.add("wait 60");
						return script;
					}

					script.add("Thanks! Here you are. I'll set this sawmill up soon.");
					script.add("<exit>");
					Quests.removeItems(user, required);
					Quests.pay(user, Merchants.goldBlock.clone().amount(2).build());
					Quests.pay(user, Merchants.goldIngot.clone().amount(4).build());
					ClientsideContentManager.addCategory(user, ContentCategory.SAWMILL, Time.SECOND.x(10));

					user.setQuestStage_Lumberjack(QuestStage.COMPLETE);
					user.getNextStepNPCs().remove(this.getNpcId());
					userService.save(user);
					return script;
				}

				return script;
			}
		},
		SORCERER(BearFair21NPC.SORCERER) {
			@Override
			public List<String> getScript(BearFair21User user) {
				List<String> script = new ArrayList<>();
				ItemStack tool = getTool(user.getPlayer());

				if (!user.hasMet(this.getNpcId())) {
					script.add("Hmm I can tell you come here seeking knowledge of the arcane.");
					script.add("wait 60");
					script.add("How do I know? Well that is simply not important right now, what is important is that I, as the great sorcerer Lucian, have knowledge of magic that far surpasses that of anyone else!");
					script.add("wait 100");
					script.add("So don't just stand there, take a look around.");
					script.add("wait 40");
					return script;
				} else if (isInviting(user, this.getNpcId(), tool)) {
					script.add(Quests.getThanks());
					script.add("<exit>");
					invite(user, this.getNpcId(), tool);
					return script;
				}

				return script;
			}
		},
		TRADER(BearFair21NPC.TRADER) {
			@Override
			public List<String> getScript(BearFair21User user) {
				List<String> script = new ArrayList<>();
				ItemStack tool = getTool(user.getPlayer());

				if (!user.hasMet(this.getNpcId())) {
					script.add("Hello there, My name is Joe, and I'm your local Event Point Trader.");
					script.add("wait 80");
					script.add("If you trade with me, you can earn 50 Event Points, everyday!");
					script.add("wait 80");
					return script;
				} else if (isInviting(user, this.getNpcId(), tool)) {
					script.add(Quests.getThanks());
					script.add("<exit>");
					invite(user, this.getNpcId(), tool);
					return script;
				}

				return script;
			}
		},
		;

		private static void invite(BearFair21User user, int npcId, ItemStack tool) {
			tool.setAmount(tool.getAmount() - 1);
			user.getInvitees().add(npcId);
			if (user.getInvitees().size() == invitees.size()) {
				user.sendMessage("You've given out all of the invites, return to Mayor John.");
				user.setQuestStage_Main(QuestStage.STEP_SIX);
			}

			userService.save(user);
		}

		private static boolean isInviting(BearFair21User user, int npcId, ItemStack tool) {
			if (user.getInvitees().contains(npcId))
				return false;

			return !ItemUtils.isNullOrAir(tool) && ItemUtils.isFuzzyMatch(tool, invitation.build());
		}

		private final BearFair21NPC npc;
		private final List<String> script;

		@Override
		public List<String> getScript(BearFair21User user) {
			return this.script;
		}

		@Override
		public String getName() {
			return this.npc.getNpcName();
		}

		@Override
		public int getNpcId() {
			return this.npc.getId();
		}

		MainNPCs(BearFair21NPC npc) {
			this.npc = npc;
			this.script = Collections.emptyList();
		}
	}

	@EventHandler
	public void onClickHead(PlayerInteractEvent event) {
		if (BearFair21.isNotAtBearFair(event)) return;

		Block block = event.getClickedBlock();
		if (BlockUtils.isNullOrAir(block)) return;
		if (!MaterialTag.PLAYER_SKULLS.isTagged(block)) return;

		BearFair21User user = userService.get(event.getPlayer());
		if (user.getQuestStage_BeeKeeper() != QuestStage.STEP_ONE) return;

		if (BearFair21.getWGUtils().isInRegion(block.getLocation(), "bearfair21_main_beehive_nursery")) {
			Quests.giveItem(event.getPlayer(), queenLarvae.get().clone().build());
			user.setQuestStage_BeeKeeper(QuestStage.STEPS_DONE);
			user.getNextStepNPCs().add(BEEKEEPER.getId());
			user.getNextStepNPCs().remove(QUEEN_BEE.getId());
			userService.save(user);
		}
	}
}
