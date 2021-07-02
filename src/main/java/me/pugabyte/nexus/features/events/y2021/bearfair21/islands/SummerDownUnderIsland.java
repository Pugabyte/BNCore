package me.pugabyte.nexus.features.events.y2021.bearfair21.islands;

import lombok.Getter;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.events.annotations.Region;
import me.pugabyte.nexus.features.events.models.BearFairIsland.NPCClass;
import me.pugabyte.nexus.features.events.models.QuestStage;
import me.pugabyte.nexus.features.events.models.Talker;
import me.pugabyte.nexus.features.events.y2021.bearfair21.BearFair21;
import me.pugabyte.nexus.features.events.y2021.bearfair21.Quests;
import me.pugabyte.nexus.features.events.y2021.bearfair21.islands.SummerDownUnderIsland.SummerDownUnderNPCs;
import me.pugabyte.nexus.features.events.y2021.bearfair21.quests.BearFair21TalkingNPC;
import me.pugabyte.nexus.features.events.y2021.bearfair21.quests.clientside.ClientsideContentManager;
import me.pugabyte.nexus.features.events.y2021.bearfair21.quests.npcs.BearFair21NPC;
import me.pugabyte.nexus.features.regionapi.events.player.PlayerEnteredRegionEvent;
import me.pugabyte.nexus.features.regionapi.events.player.PlayerLeavingRegionEvent;
import me.pugabyte.nexus.models.bearfair21.BearFair21User;
import me.pugabyte.nexus.models.bearfair21.BearFair21UserService;
import me.pugabyte.nexus.models.bearfair21.ClientsideContent;
import me.pugabyte.nexus.models.bearfair21.ClientsideContent.Content.ContentCategory;
import me.pugabyte.nexus.utils.ItemBuilder;
import me.pugabyte.nexus.utils.JsonBuilder;
import me.pugabyte.nexus.utils.LocationUtils;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.Tasks;
import me.pugabyte.nexus.utils.Utils;
import me.pugabyte.nexus.utils.WorldGroup;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.WeatherType;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// TODO BF21: place feathers
@Region("summerdownunder")
@NPCClass(SummerDownUnderNPCs.class)
public class SummerDownUnderIsland implements BearFair21Island {
	public static final ItemBuilder FEATHER = new ItemBuilder(Material.FEATHER).name("Wing Feather");
	public static final ItemBuilder SEVEN_FEATHER = FEATHER.clone().amount(7);
	public static final ItemBuilder BRIKKIES = new ItemBuilder(Material.COOKIE).name("Anzac Bikkie").amount(16);
	public static final ItemBuilder ELYTRA = new ItemBuilder(Material.ELYTRA).unbreakable().glow().itemFlags(ItemFlag.HIDE_UNBREAKABLE);
	// MILO ITEMS
	public static final ItemBuilder YARN = new ItemBuilder(Material.STRING).name("Yarn");
	public static final ItemBuilder DUST = new ItemBuilder(Material.GUNPOWDER).name("Dust Pile");
	public static final ItemBuilder NEWSPAPER = new ItemBuilder(Material.BONE_MEAL).name("Crumpled Newspaper");
	public static final ItemBuilder BOTTLE = new ItemBuilder(Material.GLASS_BOTTLE).name("Empty Bottle");
	public static final ItemBuilder PIZZA = new ItemBuilder(Material.MUSIC_DISC_11).name("Burnt Pizza");
	public static final List<ItemBuilder> MILO_ITEMS = List.of(YARN, DUST, NEWSPAPER, BOTTLE, PIZZA, FEATHER);
	public static final List<ItemBuilder> MILO_REMOVE_ITEMS = List.of(YARN, DUST, NEWSPAPER, BOTTLE, PIZZA);
	public static final Talker.TalkingNPC SERPENT = new Talker.TalkingNPC() {
		@Getter
		private final String name = "Rainbow Serpent";
		@Getter
		private final int npcId = -1;
		@Getter
		private final List<String> script = List.of(
			"Hello young one. It is nice to meet you.",
			"wait 60",
			"I must thank you for journeying this far to see me. Departing from that land was a burdensome task, and not one that came easily. But if you are ready to learn and pass on my story, I will be ready to return home.",
			"wait 200",
			"<self> I would like to hear your story, please.",
			"wait 40",
			"Long, long ago in the Dreamtime, the Earth lay flat and still. Nothing moved and nothing grew.",
			"wait 100",
			"One day, I woke from my slumber and came out from under the ground. I was known as the Rainbow Serpent.",
			"wait 100",
			"I travelled for a very long time, far and wide. As I made my way across the land, my body formed mountains, valleys, and rivers.",
			"wait 120",
			"I was the Dreamtime creature who shaped the Earth. After all of my travelling, I grew tired, and so I curled up and went to sleep.",
			"wait 120",
			"After some rest, I returned to the place that I had first appeared and called out to the frogs, \"Come out!\"",
			"wait 100",
			"The frogs woke up very slowly because they had so much water in their bellies. I tickled their stomachs, and the water began to fill the tracks that I had left. This is how the lakes and rivers were formed.",
			"wait 200",
			"After this, water, grass, and trees began to grow. All the other animals that lived in rocks, on the plains, in the trees and the air began to wake up and follow me. They were all happy with the Earth.",
			"wait 200",
			"I made rules that they all had to obey. Some did not like this and began to cause trouble.",
			"wait 100",
			"So I said, \"Those who obey will be rewarded; I shall give them human form. But, for those who don't, they will be punished and turned to stone.\"",
			"wait 160",
			"The tribes of people lived together on the land given to them by me.",
			"wait 80",
			"They knew that the land would always be theirs, as long as they took care of it. They believed that no one should ever take it away from them.",
			"wait 160",
			"Now, young one. Return to the village. Tell the people what you have heard today and I will return, and with me, I shall bring rain.",
			"wait 160",
			"Goodbye for now.",
			"wait 80"
		);
	};
	private static final Set<UUID> serpentTalkingTo = new HashSet<>();

	public SummerDownUnderIsland() {
		Nexus.registerListener(this);
		Tasks.repeat(2, 1, () -> {
			BearFair21.getWGUtils().getEntitiesInRegionByClass("bearfair21_elytra", Player.class).stream()
				.filter(player -> player.isOnGround()
								&& !BearFair21.getWGUtils().isInRegion(player, "bearfair21_elytra_start")
								&& !BearFair21.getWGUtils().isInRegion(player, "bearfair21_elytra_finish"))
				.forEach(this::teleportToElytraCourse);
		});
	}

	private void teleportToElytraCourse(Player player) {
		player.teleportAsync(new Location(player.getWorld(), 1571.5, 194, -388.5, 120, 0));
	}

	@EventHandler
	public void onEnterRegion(PlayerEnteredRegionEvent event) {
		String regionName = event.getRegion().getId().toLowerCase();
		BearFair21UserService service = new BearFair21UserService();
		BearFair21User user = service.get(event.getPlayer());
		QuestStage stage = user.getQuestStage_SDU();
		if (regionName.equals("bearfair21_summerdownunder_elytra")) {
			if (stage == QuestStage.STEP_SEVEN) {
				teleportToElytraCourse(event.getPlayer());
				event.getPlayer().getInventory().setChestplate(ELYTRA.get());
			} else if (stage.ordinal() < QuestStage.STEP_SEVEN.ordinal())
				event.getPlayer().sendMessage(new JsonBuilder("&c&oYou feel as though you are not yet ready to travel deeper into the cave..."));
		} else if (regionName.equals("bearfair21_elytra_dialogue") && !serpentTalkingTo.contains(event.getPlayer().getUniqueId())) {
			serpentTalkingTo.add(event.getPlayer().getUniqueId());
			Talker.runScript(event.getPlayer(), SERPENT).thenRun(() -> {
				user.setQuestStage_SDU(QuestStage.STEPS_DONE);
				ClientsideContentManager.removeCategory(user, ContentCategory.SDU_BOOK);
				SummerDownUnderNPCs.setNextNpc(user, null, SummerDownUnderNPCs.BRUCE, SummerDownUnderNPCs.MILO, SummerDownUnderNPCs.KYLIE, SummerDownUnderNPCs.MEL_GIBSON);
				service.save(user);
				serpentTalkingTo.remove(event.getPlayer().getUniqueId());
				event.getPlayer().teleportAsync(new Location(event.getPlayer().getWorld(), 172.5, 99, -171.5, -180, 0));
				ClientsideContentManager.addCategory(user, ClientsideContent.Content.ContentCategory.SERPENT);
			});
		} else if (regionName.equals("bearfair21_elytra_finish") || regionName.equals("bearfair21")) {
			ItemStack elytra = ELYTRA.build();
			for (ItemStack item : event.getPlayer().getInventory().getContents())
				if (elytra.isSimilar(item))
					item.setAmount(0);
		} else if (regionName.equals("bearfair21_summerdownunder")) {
			if (stage == QuestStage.FOUND_ALL || stage == QuestStage.COMPLETE)
				event.getPlayer().setPlayerWeather(WeatherType.DOWNFALL);
		}
	}

	@EventHandler
	public void onExitRegion(PlayerLeavingRegionEvent event) {
		if (event.getRegion().getId().equalsIgnoreCase("bearfair21_summerdownunder") && new BearFair21UserService().get(event.getPlayer()).getQuestStage_SDU().ordinal() >= QuestStage.FOUND_ALL.ordinal())
			event.getPlayer().resetPlayerWeather();
	}

	@EventHandler
	public void onReadBook(PlayerInteractEvent event) {
		if (BearFair21.isNotAtBearFair(event)) return;
		if (!Utils.ActionGroup.RIGHT_CLICK.applies(event)) return;
		if (!BearFair21.isInRegion(event.getPlayer(), getRegion())) return;

		BearFair21UserService service = new BearFair21UserService();
		BearFair21User user = service.get(event.getPlayer());
		Block block = event.getClickedBlock();
		if (user.getQuestStage_SDU() == QuestStage.STEP_SIX && event.getItem() != null && event.getItem().getType() == Material.WRITTEN_BOOK) {
			user.setQuestStage_SDU(QuestStage.STEP_SEVEN);
			service.save(user);
		} else if (event.getAction() == Action.RIGHT_CLICK_BLOCK && block != null && block.getX() == 169 && block.getY() == 97 && block.getZ() == -175) { // lazy fixing
			ItemStack book = ((Chest) block.getRelative(0, -9, 0).getState()).getBlockInventory().getItem(0);
			if (book != null && !Quests.hasItemsLikeFrom(user, Collections.singletonList(new ItemBuilder(book))))
				Quests.giveItem(user, book);
		}
	}

	@EventHandler
	public void onItemFrameClick(PlayerInteractEntityEvent event) {
		if (event.getHand() != EquipmentSlot.HAND) return;
		if (BearFair21.isNotAtBearFair(event)) return;
		if (!(event.getRightClicked() instanceof ItemFrame frame)) return;
		if (!BearFair21.isInRegion(event.getPlayer(), getRegion())) return;

		ItemStack item = frame.getItem();
		if (item.getType() == Material.AIR) return;

		BearFair21UserService service = new BearFair21UserService();
		BearFair21User user = service.get(event.getPlayer());
		QuestStage stage = user.getQuestStage_SDU();
		boolean isMiloItem = BearFair21.getWGUtils().isInRegion(frame.getLocation(), "bearfair21_summerdownunder_milo");
		boolean hasItem = event.getPlayer().getInventory().containsAtLeast(item, 1);
		if (FEATHER.build().isSimilar(item) && (stage == QuestStage.STEP_FIVE || (stage == QuestStage.STEP_FOUR && isMiloItem))) {
			Location loc = LocationUtils.getCenteredRotationlessLocation(frame.getLocation());
			if (!user.getFeatherLocations().contains(loc)) {
				user.getFeatherLocations().add(loc);
				service.save(user);
				Quests.giveItem(event.getPlayer(), item);
			} else {
				user.sendMessage(JsonBuilder.fromPrefix("BearFair21", "&cYou've already found this Wing Feather!"));
			}
		} else if (stage == QuestStage.STEP_FOUR && isMiloItem && !hasItem && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
			Quests.giveItem(event.getPlayer(), item);
		}
	}

	public enum SummerDownUnderNPCs implements BearFair21TalkingNPC {
		BRUCE(BearFair21NPC.BRUCE) {
			@Override
			public List<String> getScript(BearFair21User user) {
				QuestStage stage = user.getQuestStage_SDU();
				int ordinal = stage.ordinal();
				if (ordinal < QuestStage.STEP_FIVE.ordinal() || (stage == QuestStage.STEP_FIVE && !user.getOnlinePlayer().getInventory().containsAtLeast(FEATHER.build(), 7))) {
					if (stage == QuestStage.NOT_STARTED) {
						setStage(user, QuestStage.STARTED);
						setNextNpc(user, KYLIE);
					}
					return List.of(
						"Struth! I’m glad you’re here!",
						"wait 60",
						"<self> Is something wrong?",
						"wait 60",
						"You bet there is! Our crops haven't seen a drought this devastating in... well... EVER!",
						"wait 80",
						"We hardly have enough water to keep our livestock going!",
						"wait 60",
						"<self> Oh no! Do you know what could have caused this?",
						"wait 60",
						"No idea, mate. This is definitely out of the ordinary - not just something that could be the result of a bad year.",
						"wait 100",
						"By all predictions, we were set to have a bonza wet season. The blokes over on channel 9 are as surprised as we are!",
						"wait 100",
						"I'd like ya to question the locals, find out anything ya can so we can get to the bottom of this. We're countin' on ya!",
						"wait 100",
						"<self> Don't worry! I've got this."
					);
				} else if (stage == QuestStage.STEP_FIVE) {
					Quests.removeItem(user, SEVEN_FEATHER.build());
					final int delay = 60;
					Tasks.wait(delay, () -> {
						user.getOnlinePlayer().getInventory().removeItemAnySlot(SEVEN_FEATHER.build());
						PlayerUtils.giveItemAndMailExcess(user.getOnlinePlayer(), ELYTRA.clone().damage(431)
							.lore("&3Take these down to the darkest depths of the cave and perhaps they will fly again")
							.loreize(true).build(), WorldGroup.EVENTS); // prevent player from getting free elytra in survival lol
						setNextNpc(user, null);
					});
					List<String> text = new ArrayList<>(setStageGetScript(user, QuestStage.STEP_SIX));
					ClientsideContentManager.addCategory(user, ContentCategory.SDU_BOOK);
					// reverse order:
					text.add(0, "wait " + delay);
					text.add(0, "Ah right! I had been seein' these around the place. I know just the thing that'll help!");
					return text;
				} else if (ordinal >= QuestStage.STEP_SIX.ordinal() && ordinal < QuestStage.STEPS_DONE.ordinal()) {
					return Collections.singletonList("That should do it. It should help you traverse those caves a bit better. Head down there and let me know if you find anything to make sense of all this.");
				} else if (stage == QuestStage.STEPS_DONE) {
					taught(user);
					return Collections.singletonList("Thank you <player>. It really does humble you some.");
				} else if (stage == QuestStage.FOUND_ALL) {
					removeNextNpc(user);
					List<String> text = new ArrayList<>(setStageGetScript(user, QuestStage.COMPLETE));
					text.add("wait 40");
					text.add("Please, take this. It's the least we could do for ya. See ya 'round, mate.");
					Tasks.wait(99, () -> {
						Quests.giveKey(user);
						BearFair21.giveTokens(user, 200);
					});
					return text;
				} else {
					return List.of(
						"You beauty! The rain really did come back! Thank you <player>!",
						"wait 60",
						"<self> Yay! I'm glad I could help!"
					);
				}
			}
		},
		KYLIE(BearFair21NPC.KYLIE) {
			@Override
			public List<String> getScript(BearFair21User user) {
				QuestStage stage = user.getQuestStage_SDU();
				int ordinal = stage.ordinal();
				if (stage == QuestStage.NOT_STARTED)
					return greeting;
				else if (ordinal < QuestStage.STEP_TWO.ordinal()) {
					if (stage == QuestStage.STARTED)
						setStage(user, QuestStage.STEP_ONE);
					else if (user.getOnlinePlayer().getInventory().containsAtLeast(new ItemStack(Material.WHEAT), 10)) {
						Quests.giveItem(user, new ItemStack(Material.BUCKET));
						Quests.removeItem(user, new ItemBuilder(Material.WHEAT).amount(10).build());
						return setStageGetScript(user, QuestStage.STEP_TWO);
					}

					return List.of(
						"Hey there! A little kookaburra told me you're the one goin' 'round trynna help us out here! Thank you so much!",
						"wait 80",
						"<self> No problem! Is there anything I can do for you?",
						"wait 50",
						"There sure is. Could you grab some wheat from the field at the back there? Gotta get these biscuits ready before the lunch-time rush. A bundle of 'bout 10 should do it."
					);
				} else if (stage == QuestStage.STEP_TWO) {
					if (user.getOnlinePlayer().getInventory().containsAtLeast(new ItemStack(Material.MILK_BUCKET), 1)) {
						setNextNpc(user, MEL_GIBSON);
						Quests.removeItem(user, new ItemStack(Material.MILK_BUCKET));
						return setStageGetScript(user, QuestStage.STEP_THREE);
					}
					return Collections.singletonList(
						"Thank you! One last thing now, could you run over to Daisy the cow and fetch a bucket of milk? After that, we should be set!"
					);
				} else if (ordinal >= QuestStage.STEP_THREE.ordinal() && ordinal < QuestStage.STEPS_DONE.ordinal()) {
					if (!user.isReceivedBrikkies()) {
						user.setReceivedBrikkies(true);
						Quests.giveItem(user, BRIKKIES.build());
						new BearFair21UserService().save(user);
					}
					return List.of(
						"Thanks again <player>! As appreciation, please help yourself to some of my famous Anzac Bikkies.",
						"wait 60",
						"<self> Thank you! By the way, would you happen to have heard anything about the cause of the drought?",
						"wait 60",
						"Hmm... I heard Mr. Gibson muttering on about a... snake?... and water? Oh... and also how the townsfolk have no respect for the elders... " +
							"I think he's just grumpy though. But maybe you should head on over to his house and have a chat - he might have a snake problem or something.",
						"wait 100",
						"<self> Alright, and thank you again for the cookies- I mean, biscuits!",
						"No worries!"
					);
				} else if (stage == QuestStage.STEPS_DONE) {
					taught(user);
					return List.of(
						"Oh wow, I had no idea... I wish my grandparents had've told me about my history a bit more...",
						"wait 80",
						"Thank you for sharing this with me <player>! I will pass it on too!"
					);
				} else {
					return List.of(
						"No way! Daisy, we’re saved!",
						"wait 40",
						"<name:Daisy>Moo!"
					);
				}
			}
		},
		MEL_GIBSON(BearFair21NPC.MEL_GIBSON) {
			@Override
			public List<String> getScript(BearFair21User user) {
				QuestStage stage = user.getQuestStage_SDU();
				int ordinal = stage.ordinal();
				if (ordinal < QuestStage.STEP_THREE.ordinal()) {
					return greeting;
				} else if (ordinal < QuestStage.STEPS_DONE.ordinal()) {
					if (stage == QuestStage.STEP_THREE) {
						setNextNpc(user, MILO);
						setStage(user, QuestStage.STEP_FOUR);
					}
					return List.of(
						"G'day. I take it you're <player>? The one Bruce has sent out to get to the bottom of the issue we're facin' here?",
						"wait 100",
						"<self> I sure am!",
						"wait 40",
						"Good on ya. I'm too old to be dealin' with it myself.",
						"wait 60",
						"If only I had the foresight to see where we'd be now...",
						"wait 60",
						"<self> What? You knew this would happen?",
						"wait 50",
						"Huh? What? Oh no... not at all... No idea...",
						"wait 60",
						"<self> For an actor, you're not too good at lying...",
						"wait 60",
						"Okay well... I do know... but I can't be the one to tell ya. This has to be your own adventure.",
						"wait 90",
						"For now, visit my son Milo. I believe his RV has something that might be useful to you.",
						"wait 80",
						"<self> Okay, I'll head on over there.",
						"wait 40",
						"Good luck out there, <player>."
					);
				} else if (stage == QuestStage.STEPS_DONE) {
					taught(user);
					return Collections.singletonList("Ah yes, I recall it clearly now. Thank you for your effort. We couldn't have done it without ya.");
				} else {
					return Collections.singletonList("Ha! This is all thanks to you <player>! Good on ya!");
				}
			}
		},
		MILO(BearFair21NPC.MILO) {
			@Override
			public List<String> getScript(BearFair21User user) {
				QuestStage stage = user.getQuestStage_SDU();
				int ordinal = stage.ordinal();
				if (ordinal < QuestStage.STEP_FOUR.ordinal())
					return greeting;
				if (stage == QuestStage.STEP_FOUR) {
					if (Quests.hasAllItemsLikeFrom(user, MILO_ITEMS)) {
						setNextNpc(user, BRUCE);
						Quests.removeItems(user, MILO_REMOVE_ITEMS);
						return setStageGetScript(user, QuestStage.STEP_FIVE);
					}
					return List.of(
						"Oh hey <player>! My dad just called to tell me you'd be over here soon.",
						"wait 60",
						"My van is absolutely filthy! I haven't taken 'er out in 'bout 2 years now.",
						"wait 60",
						"Would ya mind helping me clean up a bit? Just pick up all the junk lying 'round in there and bring it back to me. I'll throw it away for ya.",
						"wait 80",
						"<self> No problem, I'll be back soon!"
					);
				} else if (ordinal >= QuestStage.STEP_FIVE.ordinal() && ordinal < QuestStage.STEPS_DONE.ordinal()) {
					return List.of(
						"Sweet! Thanks a bunch, mate.",
						"wait 40",
						"Oh uh... hold on to that feather though. I've seen a few more of those lyin' around lately. Collect six more of them and bring them to Bruce, he'll be able to make them into something useful!",
						"wait 140",
						"<self> Alright, see you later!"
					);
				} else if (stage == QuestStage.STEPS_DONE) {
					taught(user);
					return Collections.singletonList("Woah, that’s why there was no rain? She was sad we had forgotten ‘er? I feel kinda bad now… I won’t forget about this.");
				} else {
					return Collections.singletonList("Sick! Any more of that drought and we’d be goners! Thanks!");
				}
			}
		}
		;

		private final BearFair21NPC npc;
		private final List<String> script;
		static final List<String> greeting = Collections.singletonList("G'day!");

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

		void setNextNpc(BearFair21User user, SummerDownUnderNPCs npc) {
			setNextNpc(user, this, npc);
		}

		static void setNextNpc(BearFair21User user, SummerDownUnderNPCs old, SummerDownUnderNPCs... next) {
			Set<Integer> nextNpcs = user.getNextStepNPCs();
			if (old != null)
				nextNpcs.remove(old.getNpcId());
			if (next != null && next.length > 0)
				nextNpcs.addAll(Arrays.stream(next).filter(Objects::nonNull).map(SummerDownUnderNPCs::getNpcId).collect(Collectors.toList()));
			new BearFair21UserService().save(user);
		}

		void removeNextNpc(BearFair21User user) {
			Set<Integer> nextNpcs = user.getNextStepNPCs();
			nextNpcs.remove(getNpcId());
			new BearFair21UserService().save(user);
		}

		void setStage(BearFair21User user, QuestStage stage) {
			user.setQuestStage_SDU(stage);
			new BearFair21UserService().save(user);
		}

		List<String> setStageGetScript(BearFair21User user, QuestStage stage) {
			user.setQuestStage_SDU(stage);
			new BearFair21UserService().save(user);
			return getScript(user);
		}

		void taught(BearFair21User user) {
			user.getTaughtNpcIds().add(getNpcId());
			if (user.getTaughtNpcIds().size() == SummerDownUnderNPCs.values().length) {
				setStage(user, QuestStage.FOUND_ALL);
				setNextNpc(user, BRUCE);
				user.getOnlinePlayer().setPlayerWeather(WeatherType.DOWNFALL);
			} else {
				removeNextNpc(user);
			}
			new BearFair21UserService().save(user);
		}

		SummerDownUnderNPCs(BearFair21NPC npc) {
			this.npc = npc;
			this.script = Collections.emptyList();
		}
	}
}
