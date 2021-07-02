package me.pugabyte.nexus.features.events.y2021.bearfair21.islands;

import eden.utils.RandomUtils;
import eden.utils.TimeUtils.Time;
import eden.utils.TimeUtils.Timespan;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.events.annotations.Region;
import me.pugabyte.nexus.features.events.models.BearFairIsland.NPCClass;
import me.pugabyte.nexus.features.events.models.QuestStage;
import me.pugabyte.nexus.features.events.y2021.bearfair21.BearFair21;
import me.pugabyte.nexus.features.events.y2021.bearfair21.Quests;
import me.pugabyte.nexus.features.events.y2021.bearfair21.islands.PugmasIsland.PugmasNPCs;
import me.pugabyte.nexus.features.events.y2021.bearfair21.quests.BearFair21Talker;
import me.pugabyte.nexus.features.events.y2021.bearfair21.quests.BearFair21TalkingNPC;
import me.pugabyte.nexus.features.events.y2021.bearfair21.quests.clientside.ClientsideContentManager;
import me.pugabyte.nexus.features.events.y2021.bearfair21.quests.npcs.BearFair21NPC;
import me.pugabyte.nexus.features.regionapi.events.common.EnteringRegionEvent;
import me.pugabyte.nexus.models.bearfair21.BearFair21User;
import me.pugabyte.nexus.models.bearfair21.BearFair21UserService;
import me.pugabyte.nexus.models.bearfair21.ClientsideContent;
import me.pugabyte.nexus.models.bearfair21.ClientsideContent.Content;
import me.pugabyte.nexus.models.bearfair21.ClientsideContentService;
import me.pugabyte.nexus.utils.ActionBarUtils;
import me.pugabyte.nexus.utils.BlockUtils;
import me.pugabyte.nexus.utils.LocationUtils;
import me.pugabyte.nexus.utils.SoundBuilder;
import me.pugabyte.nexus.utils.Tasks;
import me.pugabyte.nexus.utils.Tasks.Countdown;
import me.pugabyte.nexus.utils.TitleUtils;
import net.citizensnpcs.api.npc.NPC;
import net.minecraft.world.entity.EntityTypes;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Region("pugmas")
@NPCClass(PugmasNPCs.class)
public class PugmasIsland implements BearFair21Island {
	private static final ClientsideContentService contentService = new ClientsideContentService();
	private static final ClientsideContent contentList = contentService.get0();
	private static final BearFair21UserService userService = new BearFair21UserService();
	private static final List<Location> contentIndex = Arrays.asList(loc(-45, 143, -325), loc(-49, 139, -300),
			loc(-76, 138, -305), loc(-101, 139, -290), loc(-111, 142, -321), loc(-104, 154, -350),
			loc(-72, 157, -364), loc(-78, 152, -346), loc(-79, 134, -322), loc(-59, 145, -343),
			loc(-40, 153, -353), loc(-59, 166, -374), loc(-78, 174, -384), loc(-106, 174, -381),
			loc(-67, 139, -308)
	);
	private static final Location endLocation = new Location(BearFair21.getWorld(), -72.5, 138, -306.5, -26, 0);
	private static final List<String> scaredVillager = Arrays.asList("Is he s-still here?", "Be careful.", "*shaking*", "I-I bet the M-mayor has e-everything under cont-t-trol...",
			"That Grinch doesn't have any other way to spend his time? Ugh!", "He's so mean!", "Maybe the Mayor could use your help...",
			"The Grinch stole everything! ...Again!", "I can't believe it!");
	private static final List<String> thankfulVillager = Arrays.asList("Woo!", "Thank you <player>!", "Thank you so much!",
			"I'm never letting my bike out of my sight ever again!", "I knew you could do it!", "Come back any time!", "Nice!",
			"My new phone barely has a scratch on it thanks to you!", "Yay! My socks are back!",
			"I'm so happy the Grinch didn't know I got my puppy for Christmas...");
	private static final List<UUID> usingGrinch = new ArrayList<>();
	private static final String grinchCaveRegion = "bearfair21_pugmas_grinchcave";

	public PugmasIsland() {
		Nexus.registerListener(this);
	}

	public static void startup() {
		NPC npc = BearFair21NPC.GRINCH_1.getNPC();
		if (npc != null && npc.isSpawned())
			npc.despawn();
	}

	public enum PugmasNPCs implements BearFair21TalkingNPC {
		VILLAGER_1(BearFair21NPC.PUGMAS_VILLAGER_1) {
			@Override
			public List<String> getScript(BearFair21User user) {
				if (user.getQuestStage_Pugmas() == QuestStage.COMPLETE)
					return getThankfulVillager();
				return getScaredVillager();
			}
		},
		VILLAGER_2(BearFair21NPC.PUGMAS_VILLAGER_2) {
			@Override
			public List<String> getScript(BearFair21User user) {
				if (user.getQuestStage_Pugmas() == QuestStage.COMPLETE)
					return getThankfulVillager();
				return getScaredVillager();
			}
		},
		VILLAGER_3(BearFair21NPC.PUGMAS_VILLAGER_3) {
			@Override
			public List<String> getScript(BearFair21User user) {
				if (user.getQuestStage_Pugmas() == QuestStage.COMPLETE)
					return getThankfulVillager();
				return getScaredVillager();
			}
		},
		VILLAGER_4(BearFair21NPC.PUGMAS_VILLAGER_4) {
			@Override
			public List<String> getScript(BearFair21User user) {
				if (user.getQuestStage_Pugmas() == QuestStage.COMPLETE)
					return getThankfulVillager();
				return getScaredVillager();
			}
		},
		VILLAGER_5(BearFair21NPC.PUGMAS_VILLAGER_5) {
			@Override
			public List<String> getScript(BearFair21User user) {
				if (user.getQuestStage_Pugmas() == QuestStage.COMPLETE)
					return getThankfulVillager();
				return getScaredVillager();
			}
		},
		VILLAGER_6(BearFair21NPC.PUGMAS_VILLAGER_6) {
			@Override
			public List<String> getScript(BearFair21User user) {
				if (user.getQuestStage_Pugmas() == QuestStage.COMPLETE)
					return getThankfulVillager();
				return getScaredVillager();
			}
		},
		VILLAGER_7(BearFair21NPC.PUGMAS_VILLAGER_7) {
			@Override
			public List<String> getScript(BearFair21User user) {
				if (user.getQuestStage_Pugmas() == QuestStage.COMPLETE)
					return getThankfulVillager();
				return getScaredVillager();
			}
		},
		VILLAGER_8(BearFair21NPC.PUGMAS_VILLAGER_8) {
			@Override
			public List<String> getScript(BearFair21User user) {
				if (user.getQuestStage_Pugmas() == QuestStage.COMPLETE)
					return getThankfulVillager();
				return getScaredVillager();
			}
		},
		VILLAGER_9(BearFair21NPC.PUGMAS_VILLAGER_9) {
			@Override
			public List<String> getScript(BearFair21User user) {
				if (user.getQuestStage_Pugmas() == QuestStage.COMPLETE)
					return getThankfulVillager();
				return getScaredVillager();
			}
		},
		VILLAGER_10(BearFair21NPC.PUGMAS_VILLAGER_10) {
			@Override
			public List<String> getScript(BearFair21User user) {
				if (user.getQuestStage_Pugmas() == QuestStage.COMPLETE)
					return getThankfulVillager();
				return getScaredVillager();
			}
		},
		VILLAGER_11(BearFair21NPC.PUGMAS_VILLAGER_11) {
			@Override
			public List<String> getScript(BearFair21User user) {
				if (user.getQuestStage_Pugmas() == QuestStage.COMPLETE)
					return getThankfulVillager();
				return getScaredVillager();
			}
		},
		VILLAGER_12(BearFair21NPC.PUGMAS_VILLAGER_12) {
			@Override
			public List<String> getScript(BearFair21User user) {
				if (user.getQuestStage_Pugmas() == QuestStage.COMPLETE)
					return getThankfulVillager();
				return getScaredVillager();
			}
		},
		VILLAGER_13(BearFair21NPC.PUGMAS_VILLAGER_13) {
			@Override
			public List<String> getScript(BearFair21User user) {
				if (user.getQuestStage_Pugmas() == QuestStage.COMPLETE)
					return getThankfulVillager();
				return getScaredVillager();
			}
		},
		VILLAGER_14(BearFair21NPC.PUGMAS_VILLAGER_14) {
			@Override
			public List<String> getScript(BearFair21User user) {
				if (user.getQuestStage_Pugmas() == QuestStage.COMPLETE)
					return getThankfulVillager();
				return getScaredVillager();
			}
		},
		MAYOR(BearFair21NPC.PUGMAS_MAYOR) {
			@Override
			public List<String> getScript(BearFair21User user) {
				List<String> script = new ArrayList<>();

				switch (user.getQuestStage_Pugmas()) {
					case NOT_STARTED, STARTED -> {
						script.add("Thank goodness you're here <player>! That darn Grinch has done it again!");
						script.add("wait 80");
						script.add("The citizens have told me that he's stolen their gifts from *last Christmas*... Can you believe that?!");
						script.add("wait 100");
						script.add("I've never come across anyone with pride that fragile!");
						script.add("wait 80");
						script.add("<self> That's terrible!");
						script.add("wait 50");
						script.add("I know! There were so many presents too... My townsfolk are beside themselves...");
						script.add("wait 80");
						script.add("<self> Is there anything I can do to help? Do you know where the Grinch may have gone?");
						script.add("wait 80");
						script.add("There might be something you could do for us... although it'll be hard to negotiate with him...");
						script.add("wait 90");
						script.add("The citizens have insisted that the sheer weight of the presents he took was enough to cause his escape rocket to go haywire!");
						script.add("wait 100");
						script.add("So he sought refuge in the cave beneath us while he attempts to fix it, perhaps.");
						script.add("wait 80");
						script.add("We are all too shaken to even think about confronting him ourselves. Who knows what he might do!");
						script.add("wait 90");
						script.add("Would you be able to venture down there and talk to him?  The entrance to the cave is to the east of the island.");
						script.add("wait 100");
						script.add("Please be careful! As you may know, he's quite... temperamental.");
						script.add("wait 80");
						script.add("We can't waste any time <player>. I couldn't bear the thought of having another year ruined by that goof!");
						script.add("wait 80");
						script.add("<self> Leave it to me!");

						user.setQuestStage_Pugmas(QuestStage.STARTED);
						user.getNextStepNPCs().add(GRINCH.getNpcId());
						userService.save(user);
						return script;
					}

					case COMPLETE -> {
						script.add("Thank you again <player>! Good luck on your adventures!");
						return script;
					}
				}

				script.add("Hello!");
				return script;
			}
		},
		GRINCH(BearFair21NPC.GRINCH) {
			@Override
			public List<String> getScript(BearFair21User user) {
				List<String> script = new ArrayList<>();

				switch (user.getQuestStage_Pugmas()) {
					case STARTED -> {
						int wait = 0;
						script.add("Agh! Gosh darn it, blasted thing! I never should have- WHO GOES THERE?");
						script.add("wait 100");
						script.add("You dare to enter my cave unannounced! WHO DO YOU THINK YOU ARE?!");
						script.add("wait 100");
						script.add("<self> I'm <player> and this isn't your cave!");
						script.add("wait 100");
						script.add("Pshh! frivolous details...");
						script.add("wait 60");
						script.add("I wouldn't even be in this position if the Present Pincher 3000 hadn't malfunctioned!");
						script.add("wait 100");
						script.add("And I was so sure it was going to work this time... *sigh*");
						script.add("wait 100");
						wait += (100 + 100 + 100 + 60 + 100 + 100);
						script.add("<self> How dare you try and steal from these kind citizens! What have they ever done to you?! " +
							"You'll never get away with this...");
						script.add("wait 140");
						script.add("Oh boo hoo! You're already too late! I have devised an ingenious plan that is completely fool-proof " +
							"and has no flaws what-so-ever!");
						script.add("wait 140");
						script.add("My make-shift Present Pincher 4000 will NEVER let me down! MUAHAHAHA!");
						script.add("wait 100");
						wait += (140 + 140 + 100);

						// Rocket rumbling sound
						;
						Tasks.wait(wait, () -> new SoundBuilder(Sound.ENTITY_MINECART_RIDING).receiver(user.getPlayer()).pitch(0.1).play());
						script.add("<self> No please! Don't do it!");
						script.add("wait 70");
						wait += (70);

						// Rocket launching sound
						Tasks.wait(wait, () -> new SoundBuilder(Sound.ENTITY_FIREWORK_ROCKET_LAUNCH).receiver(user.getPlayer()).pitch(0.1).play());
						script.add("MUAHAHAHA-");
						script.add("wait 60");
						wait += (60);

						// Rocket exploding
						Tasks.wait(wait, () -> {
							new SoundBuilder(Sound.ENTITY_FIREWORK_ROCKET_BLAST).receiver(user.getPlayer()).volume(2.0).pitch(0.1).play();

							new SoundBuilder(Sound.ENTITY_GENERIC_EXPLODE).receiver(user.getPlayer()).volume(2.0).pitch(0.1).play();

							new SoundBuilder(Sound.ENTITY_FIREWORK_ROCKET_TWINKLE).receiver(user.getPlayer()).volume(2.0).play();
							Tasks.wait(8, () -> {
								new SoundBuilder(Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST).receiver(user.getPlayer()).volume(2.0).pitch(0.1).play();
								new SoundBuilder(Sound.ENTITY_FIREWORK_ROCKET_TWINKLE).receiver(user.getPlayer()).pitch(0.1).play();
								Tasks.wait(8, () -> {
									new SoundBuilder(Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST_FAR).receiver(user.getPlayer()).pitch(0.1).play();
								});
							});
						});
						script.add("wait 20");
						script.add("... oh no");
						script.add("wait 60");
						wait += (20 + 60);

						//
						script.add("<self> .... Soooo... how about that flawless plan hm?");
						script.add("wait 80");
						script.add("<self> There's no way you'll be able to steal those presents now.");
						script.add("wait 80");
						script.add("<self> I bet you'll still be stuck down here by the time I'm finished collecting them all! haha!");
						script.add("wait 100");
						script.add("Alright then, TWERP! I'll take you up on that!");
						script.add("wait 60");
						script.add("<self> No wait, I wasn't serious-");
						script.add("wait 60");
						script.add("You have &l4 MINUTES&f to pick up ALL &o15&f presents in the town.");
						script.add("wait 80");
						script.add("HOWEVER, Whichever ones you miss, I'LL STEAL FOR MYSELF!");
						script.add("wait 80");
						script.add("<self> I guess I don't have too much of a choice, do I?");
						script.add("wait 100");
						script.add("Nope! Absolutely Not!");
						script.add("wait 60");
						script.add("I'll start my timer once you leave my cave!");
						script.add("wait 40");
						wait += (80 + 80 + 100 + 60 + 60 + 80 + 80 + 100 + 60 + 40);

						user.setQuestStage_Pugmas(QuestStage.STEP_ONE);
						user.getNextStepNPCs().remove(MAYOR.getNpcId());
						user.getNextStepNPCs().add(this.getNpcId());
						userService.save(user);

						Tasks.wait(wait, () -> {
							user.setQuestStage_Pugmas(QuestStage.STEP_TWO);
							user.getNextStepNPCs().remove(this.getNpcId());
							userService.save(user);
						});

						return script;
					}

					case STEPS_DONE -> {
						int wait = 0;
						if (user.isPugmasCompleted()) {
							script.add("*huff...huff* Drats! I really should have cut back on the leftover pudding...");
							script.add("wait 60");
							script.add("*sigh* Alright <player>, you got me beat...");
							script.add("wait 40");
							script.add("<self> Yes!!");
							script.add("wait 20");
							wait += (60 + 40 + 20);
						} else {
							script.add("*huff...huff* That was harder than I planned... I really should have cut back on the leftover pudding...");
							script.add("wait 80");
							script.add("*sigh* At least I got out of it with something... barely...");
							script.add("wait 60");
							wait += (80 + 60);
						}

						script.add("<name:Mayor> GRINCH! You really thought you could get off scot-free by making a bet?!");
						script.add("wait 80");
						script.add("I could have, if it weren't for this meddling kid!");
						script.add("wait 60");
						script.add("<name:Mayor> That's it, I've had enough of your antics! Scram! And I don't want to see you back here at yuletide either!");
						script.add("wait 100");
						script.add("Pfft... I'll be gone... but not for long, Mr. Mayor.");
						script.add("wait 60");
						wait += (80 + 60 + 100 + 60);

						if (!user.isPugmasCompleted()) {
							script.add("As for you, <player>. Take these darn, treacherous things! I won't be needing them anymore.");
							script.add("wait 60");
							wait += (60);
						}

						script.add("<self> Phew! Well I'm glad to be back! I suppose I should be giving these to you.");
						script.add("wait 80");
						script.add("<name:Mayor> <player> Your help has been vital for us. I couldn't possibly thank you enough!");
						script.add("wait 80");
						script.add("<name:Mayor> I may not have much to offer, but please, take this. It's the least I can do");
						script.add("wait 80");
						script.add("<name:Mayor> Thank you again <player>! Good luck on your adventures!");
						script.add("wait 80");
						wait += (80 + 80 + 80 + 80);

						Tasks.wait(wait, () -> {
							Quests.giveKey(user);
							BearFair21.giveTokens(user, 200);
						});

						user.setQuestStage_Pugmas(QuestStage.COMPLETE);
						userService.save(user);
						return script;
					}
				}

				script.add("What do YOU want?");
				return script;
			}
		},
		;

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

		PugmasNPCs(BearFair21NPC npc) {
			this.npc = npc;
			this.script = Collections.emptyList();
		}
	}

	private static Location loc(int x, int y, int z) {
		return new Location(BearFair21.getWorld(), x, y, z);
	}

	private static List<String> getScaredVillager() {
		return Collections.singletonList(RandomUtils.randomElement(scaredVillager));
	}

	private static List<String> getThankfulVillager() {
		return Collections.singletonList(RandomUtils.randomElement(thankfulVillager));
	}

	private static void startChallenge(BearFair21User user) {
		TitleUtils.sendSubtitle(user.getPlayer(), "&cGo!");
		if (user.getActiveTaskId() != -1) {
			user.sendMessage("Error: You have an active task running");
			user.setQuestStage_Pugmas(QuestStage.STARTED);
			userService.save(user);
			return;
		}

		Countdown countdown = Countdown.builder()
			.duration(Time.MINUTE.x(4))
			.onSecond(i -> {
				if (user.isOnline()) {
					ActionBarUtils.sendActionBar(user.getPlayer(),
						"&3Time Left: &e" + Timespan.of(i).format() + " &3(&e" + (user.getPresentNdx() - 1) + "&3/15)");
				}
			})
				.onComplete(() -> endChallenge(user, false))
				.start();

		user.setActiveTaskId(countdown.getTaskId());
		user.setPresentNdx(1);
		userService.save(user);
		showNext(user);
	}

	private static void endChallenge(BearFair21User user, boolean completed) {
		user.cancelActiveTask();
		user.setQuestStage_Pugmas(QuestStage.STEPS_DONE);
		user.setPugmasCompleted(completed);
		userService.save(user);

		removeContent(user);

		NPC npc = BearFair21NPC.GRINCH_1.getNPC();
		if (npc != null) {
			if (isGrinchNotBeingUsed()) {
				if (!npc.isSpawned())
					npc.spawn(npc.getStoredLocation());
				usingGrinch.add(user.getUuid());
			}

			user.getOnlinePlayer().teleport(endLocation);
			BearFair21Talker.runScript(user, PugmasNPCs.GRINCH.getNpcId()).thenAccept(bool -> {
				if (isGrinchNotBeingUsed()) {
					Quests.poof(npc.getStoredLocation());
					if (npc.isSpawned())
						npc.despawn();
					usingGrinch.remove(user.getUuid());
				}
			});
		}
	}

	private static boolean isGrinchNotBeingUsed() {
		return usingGrinch.size() <= 1;
	}

	private void clickedPresent(BearFair21User user, Content content) {
		if (content != null)
			removeContent(user, content);

		int userNdx = user.getPresentNdx() + 1;
		user.setPresentNdx(userNdx);
		userService.save(user);

		if (userNdx > contentIndex.size()) {
			endChallenge(user, true);
			return;
		}

		Quests.sound_obtainItem(user.getPlayer());
		showNext(user);
	}

	private static void showNext(BearFair21User user) {
		Content next = contentList.from(contentIndex.get(user.getPresentNdx() - 1));
		if (next != null)
			addContent(user, next);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onClickPresent(PlayerInteractEvent event) {
		if (BearFair21.isNotAtBearFair(event)) return;
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		if (!BearFair21.isInRegion(event.getPlayer(), getRegion())) return;

		Block block = event.getClickedBlock();
		if (BlockUtils.isNullOrAir(block)) return;

		BearFair21User user = userService.get(event.getPlayer());
		if (user.getQuestStage_Pugmas() != QuestStage.STEP_TWO) return;

		event.setCancelled(true);

		Location location = block.getRelative(event.getBlockFace()).getLocation();
		Content content = contentList.from(location);
		if (content == null)
			return;

		Location contentLoc = content.getLocation().toBlockLocation();
		int locNdx = getLocationIndex(contentLoc) + 1;
		int userNdx = user.getPresentNdx();
		if (locNdx == userNdx)
			clickedPresent(user, content);
	}

	private int getLocationIndex(Location location) {
		contentIndex.indexOf(location);

		int ndx = 0;
		for (Location _location : contentIndex) {
			if (LocationUtils.isFuzzyEqual(location, _location))
				return ndx;
			++ndx;
		}

		return -1;
	}

	private static void removeContent(BearFair21User user) {
		for (Location location : contentIndex) {
			Content content = contentList.from(location);
			if (content == null) continue;

			removeContent(user, content);
		}

	}

	private static void addContent(BearFair21User user, Content content) {
		ClientsideContentManager.sendSpawnContent(user.getPlayer(), Collections.singletonList(content), true);
		ClientsideContentManager.sendSpawnArmorStand(user.getPlayer(), content.getLocation().getBlock().getRelative(0, 1, 0).getLocation());

	}

	private static void removeContent(BearFair21User user, Content content) {
		ClientsideContentManager.sendRemoveContent(user.getOnlinePlayer(), Collections.singletonList(content));
		ClientsideContentManager.sendRemoveEntityFrom(user.getOnlinePlayer(),
				content.getLocation().getBlock().getRelative(0, 1, 0).getLocation(), EntityTypes.c);
	}

	@EventHandler
	public void onRegionEnter(EnteringRegionEvent event) {
		if (!event.getRegion().getId().equalsIgnoreCase(grinchCaveRegion)) return;
		if (!(event.getEntity() instanceof Player player)) return;

		BearFair21User user = userService.get(player);
		QuestStage questStage = user.getQuestStage_Pugmas();

		if (questStage == QuestStage.STEP_ONE)
			event.setCancelled(true);
		else if (questStage == QuestStage.STEP_TWO)
			startChallenge(user);
	}
}
