package me.pugabyte.nexus.features.events.y2021.bearfair21.islands;

import com.destroystokyo.paper.ParticleBuilder;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import eden.utils.TimeUtils.Time;
import eden.utils.Utils;
import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.SmartInvsPlugin;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import fr.minuskube.inv.content.SlotPos;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import me.lexikiq.HasPlayer;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.commands.staff.WorldGuardEditCommand;
import me.pugabyte.nexus.features.events.annotations.Region;
import me.pugabyte.nexus.features.events.models.BearFairIsland.NPCClass;
import me.pugabyte.nexus.features.events.models.QuestStage;
import me.pugabyte.nexus.features.events.models.Talker;
import me.pugabyte.nexus.features.events.y2021.bearfair21.BearFair21;
import me.pugabyte.nexus.features.events.y2021.bearfair21.Quests;
import me.pugabyte.nexus.features.events.y2021.bearfair21.islands.MainIsland.MainNPCs;
import me.pugabyte.nexus.features.events.y2021.bearfair21.islands.MinigameNightIsland.MinigameNightNPCs;
import me.pugabyte.nexus.features.events.y2021.bearfair21.quests.BearFair21TalkingNPC;
import me.pugabyte.nexus.features.events.y2021.bearfair21.quests.clientside.ClientsideContentManager;
import me.pugabyte.nexus.features.events.y2021.bearfair21.quests.npcs.BearFair21NPC;
import me.pugabyte.nexus.features.events.y2021.bearfair21.quests.resources.fishing.FishingLoot;
import me.pugabyte.nexus.features.menus.MenuUtils;
import me.pugabyte.nexus.features.regionapi.events.player.PlayerEnteredRegionEvent;
import me.pugabyte.nexus.models.bearfair21.BearFair21User;
import me.pugabyte.nexus.models.bearfair21.BearFair21UserService;
import me.pugabyte.nexus.models.bearfair21.ClientsideContent.Content.ContentCategory;
import me.pugabyte.nexus.models.cooldown.CooldownService;
import me.pugabyte.nexus.models.trophy.Trophy;
import me.pugabyte.nexus.utils.ActionBarUtils;
import me.pugabyte.nexus.utils.BlockUtils;
import me.pugabyte.nexus.utils.ColorType;
import me.pugabyte.nexus.utils.ItemBuilder;
import me.pugabyte.nexus.utils.ItemUtils;
import me.pugabyte.nexus.utils.LocationUtils;
import me.pugabyte.nexus.utils.LuckPermsUtils.PermissionChange;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.RandomUtils;
import me.pugabyte.nexus.utils.SoundBuilder;
import me.pugabyte.nexus.utils.SoundUtils;
import me.pugabyte.nexus.utils.StringUtils;
import me.pugabyte.nexus.utils.Tasks;
import me.pugabyte.nexus.utils.Utils.ActionGroup;
import me.pugabyte.nexus.utils.WorldGuardUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static eden.utils.StringUtils.camelCase;
import static me.pugabyte.nexus.features.events.y2021.bearfair21.BearFair21.getWGUtils;
import static me.pugabyte.nexus.utils.ItemUtils.isNullOrAir;
import static me.pugabyte.nexus.utils.ItemUtils.isSameHead;
import static me.pugabyte.nexus.utils.ItemUtils.isTypeAndNameEqual;

@Region("minigamenight")
@NPCClass(MinigameNightNPCs.class)
public class MinigameNightIsland implements BearFair21Island {
	static BearFair21UserService userService = new BearFair21UserService();

	private static final ItemStack hat = new ItemBuilder(Material.CYAN_STAINED_GLASS_PANE).name("GG Hat").customModelData(101).build();

	public MinigameNightIsland() {
		Nexus.registerListener(this);

		ParticleBuilder particles = new ParticleBuilder(Particle.REDSTONE).color(Color.RED).count(15).offset(0.3, 0.3, 0.3);
		Location gravWellLoc = BearFair21.getWGUtils().toLocation(BearFair21.getWGUtils().getProtectedRegion(gravwellRegion).getMinimumPoint());
		Tasks.repeat(0, Time.SECOND.x(5), () -> {
			for (Player player : BearFair21.getPlayers()) {
				final BearFair21User user = userService.get(player);
				for (Location soundLoc : user.getMgn_beaconsActivated()) {
					if (player.getLocation().distance(soundLoc) <= 20) {
						new SoundBuilder(Sound.BLOCK_BEACON_AMBIENT).receiver(player).location(soundLoc).volume(2.0).play();

						Block block = soundLoc.getBlock();
						if (block.getBlockData() instanceof Directional directional) {
							Location particleLoc = soundLoc.getBlock().getRelative(directional.getFacing().getOppositeFace()).getLocation().toCenterLocation();
							int wait = 0;
							for (int i = 0; i < 5; i++) {
								Tasks.wait(wait += 20, () -> particles.location(particleLoc).receivers(player).spawn());
							}
						}
					}
				}

				if (ClientsideContentManager.canSee(player, ContentCategory.GRAVWELL)) {
					new SoundBuilder(Sound.BLOCK_BEACON_AMBIENT)
						.receiver(player)
						.location(gravWellLoc)
						.volume(2.0)
						.play();
				} else {
					if (player.getInventory().containsAtLeast(MainIsland.getGravwell().build(), 1))
						new ParticleBuilder(Particle.VILLAGER_HAPPY)
							.receivers(player)
							.location(gravWellLoc.toCenterLocation())
							.count(10)
							.offset(.25, .25, .25)
							.spawn();
				}
			}
		});

		Tasks.repeat(0, 5, () -> {
			for (Player player : BearFair21.getPlayers()) {
				final BearFair21User user = userService.get(player);
				if (user.getQuestStage_MGN() == QuestStage.STEP_EIGHT) {
					if (user.getMgn_speakersFixed().size() < speakerLocations.size()) {
						for (Location location : speakerLocations) {
							if (user.getMgn_speakersFixed().contains(location)) continue;

							new ParticleBuilder(Particle.CAMPFIRE_COSY_SMOKE)
								.receivers(player)
								.location(location.toCenterLocation())
								.count(0)
								.offset(0, 1, 0)
								.extra(.04)
								.spawn();
						}
					}
				}

				if (user.getQuestStage_MGN() == QuestStage.STEP_FOUR && FixableDevice.LAPTOP.hasFixed(player))
					getPhoneParticles().receivers(player).spawn();
			}
		});
	}

	public enum MinigameNightNPCs implements BearFair21TalkingNPC {
		XAVIER(BearFair21NPC.XAVIER) {
			@Override
			public List<String> getScript(BearFair21User user) {
				List<String> script = new ArrayList<>();
				script.add("Sup... What, never seen a quadruple bass pedal?");
				return script;
			}
		},
		RYAN(BearFair21NPC.RYAN) {
			@Override
			public List<String> getScript(BearFair21User user) {
				List<String> script = new ArrayList<>();

				script.add("Yo. Know any good synths?");

				return script;
			}
		},
		HEATHER(BearFair21NPC.HEATHER) {
			@Override
			public List<String> getScript(BearFair21User user) {
				List<String> script = new ArrayList<>();

				script.add("Hey! After learning 504 bass lines, I still can't decide what style I love the most... maybe all of them...");

				return script;
			}
		},
		AXEL(BearFair21NPC.AXEL) {
			@Override
			public List<String> getScript(BearFair21User user) {
				List<String> script = new ArrayList<>();

				switch (user.getQuestStage_MGN()) {
					case NOT_STARTED -> {
						int wait = 0;
						script.add("Hey! Welcome to the Game Gallery! Proud sponsor of Bear Fair 2021! ... Hold up, <player>? Is that you?");
						script.add("wait 90");
						script.add("<self> Hey, Axel!");
						script.add("wait 50");
						script.add("Yooo how ya been dude? It'd be hard to forget the hero who saved last year's arcade tourney! Thanks again for that.");
						script.add("wait 120");
						script.add("<self> Always glad to help out where I can!");
						script.add("wait 50");
						script.add("Broo, it's hard to find people as dope as you these days.");
						script.add("wait 60");
						wait += 90 + 50 + 120 + 50 + 60;
						script.add("<self> Aw, thanks! So how're things at GG?");
						script.add("wait 50");
						script.add("Pretty stressful, not gonna lie. Lots of good business, but it's hard to keep up with it all, being self employed, especially during bearfair.");
						script.add("wait 110");
						script.add("Just barely found a few moments to come out here and help the bros get set up for our Bear Fair Band-sesh' tonight.");
						script.add("wait 110");
						script.add("<self> Anything I can do to help?");
						script.add("wait 50");
						wait += 50 + 110 + 110 + 50;
						script.add("Nah I couldn't keep you from the bear fair celebration...");
						script.add("wait 70");
						script.add("<self> No really, I wouldn't mind.");
						script.add("wait 50");
						script.add("Really? Well if you're sure, we all could actually use more practice... Would you mind running the store for me?");
						script.add("wait 90");
						script.add("Just till we close tonight; and I'll totally pay you. In fact, here.");
						script.add("wait 70");
						wait += 70 + 50 + 90 + 70;
						Tasks.wait(wait, () -> Quests.giveItem(user, hat));
						script.add("You're an official employee of GG! With your tech skills, it'll be a breeze.");
						script.add("wait 70");
						script.add("<self> I got you bro, practice all you need. I wanna hear an awesome song when I get back!");
						script.add("wait 70");
						script.add("Duude, you're a lifesaver!");
						wait += (70 + 70);
						Tasks.wait(wait, () -> {
							user.getNextStepNPCs().remove(getNpcId());
							user.setQuestStage_MGN(QuestStage.STARTED);
							userService.save(user);
						});
					}

					case STEP_EIGHT -> {
						if (user.getMgn_speakersFixed().size() < speakerLocations.size()) {
							if (BearFair21.isInRegion(user.getOnlinePlayer(), "bearfair21_minigamenight_gamegallery")) {
								script.add("<self> Hello?");
								script.add("wait 50");
								script.add("Hey dude, we got a problem. You busy?");
								script.add("wait 60");
								script.add("<self> Nope, just finished up a service call, what's wrong?");
								script.add("wait 70");
								script.add("Well, we were jammin' and Ryan accidentally hit the volume slider on his keyboard. Basically blew out all the speakers!");
								script.add("wait 120");
								script.add("The whole sound-system is toast. I know I have one extra salvaged speaker down in the workshop.");
								script.add("wait 110");
								script.add("But we're gonna need more than that, otherwise we can't play the show tonight!");
								script.add("wait 90");
								script.add("<self> Oh no! What can I do?");
								script.add("wait 60");
								script.add("First grab the extra speaker and set it up on stage, then we'll have to figure out where we can snag three more...");
								script.add("wait 120");
								script.add("You might be able to find some parts at my house you could use to build another.");
								script.add("wait 90");
								script.add("After that, maybe we could borrow two from someone? I dunno man, this sucks...");
								script.add("wait 90");
								script.add("<self> Don't worry Axel, I'll find you some speakers somehow. We can't let this ruin your band's first gig!");
								script.add("wait 110");
								script.add("Thanks for the optimism dude... Don't worry about the Game Gallery, I'll close up for you.");
								user.getNextStepNPCs().add(getNpcId());
								user.getNextStepNPCs().add(MainNPCs.JAMES.getNpcId());
								userService.save(user);
							} else {
								script.add("Hey dude! Were you able to find some new speakers?");
								script.add("wait 60");
								script.add("<self> Still working on it, but don't worry, I got this!");
							}
						} else {
							script.add("<self> There, all the speakers are replaced... just don't play too loud. Some of them are in various states of quality, haha.");
							script.add("wait 70");
							script.add("Yoo! Dude! I'm stoked! You really pulled through for us! And just in time too! The show starts in just a sec! Take one of the front row seats!");
							script.add("wait 70");
							script.add("Sup everyone! Happy Bear Fair! We are Chiptune and we're happy to celebrate Bear Fair by bringing you some awesome music tonight so grab some snacks and get ready to groove!");
							int wait = 70 + 70 + 80;
							Tasks.wait(wait, () -> {
								Quests.giveKey(user);
								user.sendMessage(StringUtils.getPrefix("Songs") + "You have earned the &eDK Jungle 64 &3song! &c/songs");
								PermissionChange.set().permission("powder.powder.DK_Jungle_64").player(user).run();
								Quests.giveTrophy(user, Trophy.BEAR_FAIR_2021_MINIGAME_NIGHT_QUEST);

								user.setQuestStage_MGN(QuestStage.COMPLETE);
								userService.save(user);
								Tasks.wait(Time.SECOND.x(5), () -> new SoundBuilder("minecraft:custom.dk_jungle_64").receiver(user.getOnlinePlayer()).volume(.25).play());
							});
						}
					}

					default -> script.add(Quests.getHello());
				}

				return script;
			}
		},
		TRENT(BearFair21NPC.MGN_CUSTOMER_1) {
			@Override
			public List<String> getScript(BearFair21User user) {
				List<String> script = new ArrayList<>();
				if (user.getQuestStage_MGN() == QuestStage.STARTED) {
					script.add("Ayy yo dude. You the one I gotta talk to 'bout fixin' my xbox?");
					script.add("wait 50");
					script.add("<self> Yep! What seems to be the problem?");
					script.add("wait 50");
					Tasks.wait(75, () -> {
						if (!FixableDevice.XBOX.hasBroken(user.getOnlinePlayer()))
							PlayerUtils.giveItem(user.getOnlinePlayer(), FixableDevice.XBOX.getBroken());
					});

					script.add("So like, It's an xbox one, right, and I hit the power button and it like, flickers into a blue screen and shuts down.");
					script.add("wait 90");
					script.add("<self> Yeah that's not good... does the blue screen have an error message?");
					script.add("wait 70");
					script.add("Yuh, I took a pic. Here, dawg, says 'Critical Error. [ses.status.psWarning:warning]: DS14-Mk2-AT shelf 1 on channel 2a power warning for Power supply 2: critical status; DC overvoltage fault.'");
					script.add("wait 120");
					script.add("<self> Mmm, okay, I can fix this. Let me take a look at it and I'll be right back with you as soon as it's fixed.");
					script.add("wait 80");
					script.add("A'ight, thanks dawg. I'll be right here.");
				} else if (user.getQuestStage_MGN() == QuestStage.STEP_ONE) {
					PlayerUtils.removeItem(user.getOnlinePlayer(), FixableDevice.XBOX.getFixed());
					script.add("<self> Alright, here you are. Battery was shot. Had to replace it. Pretty simple fix so the bill won't be too bad.");
					script.add("wait 80");
					script.add("Yooo, sweet. Thanks dawg! Here, you can keep the change. Peace! Ima take a look at the new games section.");
					Quests.pay(user, new ItemStack(Material.GOLD_NUGGET, 5));
					script.add("wait 80");
					script.add("<self> Thanks for choosing GG!");
					Tasks.wait(70 + 70, () -> {
						user.setQuestStage_MGN(QuestStage.STEP_TWO);
						user.getNextStepNPCs().remove(getNpcId());
						userService.save(user);
						Tasks.wait(Time.SECOND.x(5), () -> startPhoneRinging(user.getOnlinePlayer()));
					});
				}

				return script;
			}
		},
		FREDRICKSON(BearFair21NPC.MGN_CUSTOMER_2) {
			@Override
			public List<String> getScript(BearFair21User user) {
				List<String> script = new ArrayList<>();

				if (user.getQuestStage_MGN() == QuestStage.STEP_TWO) {
					script.add("<self> Thanks for calling the Game Gallery, how can I help?");
					script.add("wait 70");
					script.add("Hello, this is Ben Fredrickson. I'm calling about a laptop that my assistant dropped off in your mailbox this morning?");
					script.add("wait 70");
					script.add("<self> Yes Sir, we have it.");
					script.add("wait 40");
					script.add("I travel a great deal and I intended it to be a birthday gift for my son when I returned home.");
					script.add("wait 70");
					script.add("Unfortunately, it appears to have been damaged by improper handling on my last flight, as it won't boot up.");
					script.add("wait 80");
					script.add("I was hoping you could find out what's wrong with it and remedy the problem?");
					script.add("wait 70");
					script.add("<self> Of course sir, I'll take a look at it.");
					script.add("wait 60");
					script.add("Wonderful, once it's fixed, if you could keep it in your back room, I'm doing business in the area and will be back by in the next few days to pick it up.");
					script.add("wait 120");
					script.add("<self> No problem sir, I'll call as soon as it's ready.");
				} else if (user.getQuestStage_MGN() == QuestStage.STEP_FOUR) {
					script.add("This is Fredrickson.");
					script.add("wait 50");
					script.add("<self> Ok Mr. Fredrickson, the laptop is ready. The motherboard and screen were cracked and had to be replaced but it works perfectly now.");
					script.add("wait 90");
					script.add("Thank you so much! I knew I could count on an establishment of your caliber! Expect me back by the fifth.");
					script.add("wait 80");
					script.add("<self> Glad to be of service! Thanks for choosing the Game Gallery!");
					int wait = 50 + 90 + 80;
					Tasks.wait(wait, () -> {
						user.getOnlinePlayer().getInventory().removeItem(FixableDevice.LAPTOP.getFixed());
						user.setQuestStage_MGN(QuestStage.STEP_FIVE);
						userService.save(user);
						Tasks.wait(Time.SECOND.x(5), () -> startPhoneRinging(user.getOnlinePlayer()));
					});
				}

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

		MinigameNightNPCs(BearFair21NPC npc) {
			this.npc = npc;
			this.script = Collections.emptyList();
		}
	}

	/**
	 * on player enter GG Store region, and mgn step == started, set step to 1, and spawn Trent.
	 */

	// Solderer

	private final String galleryRegion = getRegion("gamegallery");
	private final String solderRegion = getRegion("solder");
	@Getter
	@Setter
	private static boolean activeSolder = false;

	@EventHandler
	public void onClickSolder(PlayerInteractEvent event) {
		if (!BearFair21.canDoBearFairQuest(event.getPlayer())) return;
		Block clicked = event.getClickedBlock();
		if (BlockUtils.isNullOrAir(clicked)) return;
		ProtectedRegion region = getWGUtils().getProtectedRegion(solderRegion);
		if (!getWGUtils().isInRegion(clicked.getLocation(), region)) return;

		event.setCancelled(true);
		if (activeSolder) return;

		ArmorStand armorStand = null;
		for (Entity nearbyEntity : event.getPlayer().getNearbyEntities(7, 7, 7)) {
			if (nearbyEntity instanceof ArmorStand && getWGUtils().getRegionsAt(nearbyEntity.getLocation()).contains(region)) {
				armorStand = (ArmorStand) nearbyEntity;
				break;
			}
		}

		if (armorStand == null) return;

		solder(event.getPlayer(), event.getItem(), armorStand);
	}

	@EventHandler
	public void onClickSolder(PlayerInteractAtEntityEvent event) {
		final Player player = event.getPlayer();
		if (!BearFair21.canDoBearFairQuest(player)) return;
		Entity clicked = event.getRightClicked();
		if (!(clicked instanceof ArmorStand armorStand)) return;
		ProtectedRegion region = getWGUtils().getProtectedRegion(solderRegion);
		if (!getWGUtils().isInRegion(clicked.getLocation(), region)) return;

		event.setCancelled(true);
		if (activeSolder) return;

		solder(player, ItemUtils.getTool(player), armorStand);
	}

	private void solder(Player player, ItemStack item, ArmorStand armorStand) {
		BearFair21User user = new BearFair21UserService().get(player);

		final FixableItem fixableItem = FixableItem.ofBroken(item);
		boolean assemblingSpeaker = user.getQuestStage_MGN() == QuestStage.STEP_EIGHT && AxelSpeakerPart.hasAllItems(player);
		boolean willBeAssemblingSpeaker = user.getQuestStage_MGN() == QuestStage.STEP_EIGHT && AxelSpeakerPart.hasAnyItems(player);
		boolean fixingSpeaker = user.getQuestStage_MGN() == QuestStage.STEP_EIGHT && isSameHead(slightlyDamagedSpeaker.get().build(), item);
		if (assemblingSpeaker) {
			double wait = 0;
			for (AxelSpeakerPart part : AxelSpeakerPart.values()) {
				Tasks.wait(Time.SECOND.x(wait), () -> {
					final ItemStack displayItem = part.getDisplayItem();
					PlayerUtils.removeItem(player, displayItem);
					solderItem(armorStand, player, displayItem, null);
				});
				wait += 5.6;
			}
			Tasks.wait(Time.SECOND.x(wait), () -> {
				for (AxelSpeakerPart part : AxelSpeakerPart.values())
					PlayerUtils.removeItem(player, part.getDisplayItem());
				Quests.giveItem(player, speaker.get().build());
			});
		} else if (willBeAssemblingSpeaker) {
			user.sendMessage("&cHmm, it seems I don't have all the parts to assemble a speaker...");
		} else if (fixingSpeaker) {
			PlayerUtils.removeItem(player, ItemBuilder.oneOf(item).build());
			solderItem(armorStand, player, slightlyDamagedSpeaker.get().build(), speaker.get().build());
		} if (fixableItem != null) {
			boolean fixingXbox = user.getQuestStage_MGN() == QuestStage.STARTED && FixableDevice.XBOX == fixableItem.getDevice();
			boolean fixingLaptop = user.getQuestStage_MGN() == QuestStage.STEP_THREE && FixableDevice.LAPTOP == fixableItem.getDevice();
			if (!(fixingLaptop || fixingXbox)) return;
			PlayerUtils.removeItem(player, item);
			solderItem(armorStand, player, fixableItem.getBroken(), fixableItem.getFixed());
		}
	}

	private void solderItem(ArmorStand armorStand, Player player, ItemStack broken, ItemStack fixed) {
		activeSolder = true;
		ItemStack air = new ItemStack(Material.AIR);

		armorStand.setItem(EquipmentSlot.HAND, broken);
		Location loc = new Location(BearFair21.getWorld(), -192, 137, -194);
		loc = LocationUtils.getCenteredLocation(loc);
		loc.setY(loc.getBlockY() + 0.5);
		Location finalLoc = loc;
		World world = loc.getWorld();

		Tasks.wait(5, () -> {
			new SoundBuilder(Sound.BLOCK_ANVIL_USE).location(finalLoc).volume(0.3).pitch(0.1).play();
			new SoundBuilder(Sound.BLOCK_REDSTONE_TORCH_BURNOUT).location(finalLoc).volume(0.5).play();
			Tasks.wait(20, () -> {
				new SoundBuilder(Sound.BLOCK_REDSTONE_TORCH_BURNOUT).location(finalLoc).volume(0.5).play();
				new SoundBuilder(Sound.BLOCK_BEACON_POWER_SELECT).location(finalLoc).volume(0.5).play();
			});
		});

		for (int i = 0; i < 10; i++)
			Tasks.wait(i * 5, () -> world.spawnParticle(Particle.LAVA, finalLoc, 5, 0, 0, 0));

		Tasks.wait(Time.SECOND.x(5), () -> {
			armorStand.setItem(EquipmentSlot.HAND, air);
			if (fixed != null)
				Quests.giveItem(player, fixed);
			Tasks.wait(10, () -> activeSolder = false);
		});
	}

	// Xbox

	@EventHandler
	public void onRightClickXbox(PlayerInteractEvent event) {
		if (!BearFair21.canDoBearFairQuest(event.getPlayer())) return;
		if (!ActionGroup.CLICK.applies(event) || isNullOrAir(event.getItem())) return;
		if (!isTypeAndNameEqual(FixableDevice.XBOX.getBroken(), event.getItem())) return;

		new XboxMenu().open(event.getPlayer());
	}

	// Laptop

	@EventHandler
	public void onEnterGG(PlayerEnteredRegionEvent event) {
		if (!BearFair21.canDoBearFairQuest(event.getPlayer())) return;
		if (!event.getRegion().getId().equals(galleryRegion)) return;

		final BearFair21User user = new BearFair21UserService().get(event.getPlayer());
		if (List.of(QuestStage.STEP_TWO, QuestStage.STEP_SIX).contains(user.getQuestStage_MGN()))
			startPhoneRinging(user.getOnlinePlayer());
		else if (user.getQuestStage_MGN() == QuestStage.STEP_EIGHT && !user.isMgn_receivedAxelCall())
			startPhoneRinging(user.getOnlinePlayer());
	}

	@EventHandler
	public void onClickMailbox(PlayerInteractEvent event) {
		if (BearFair21.isNotAtBearFair(event)) return;
		if (EquipmentSlot.HAND != event.getHand()) return;
		Block clicked = event.getClickedBlock();
		if (BlockUtils.isNullOrAir(clicked) || clicked.getType() != Material.BARRIER) return;
		if (!getWGUtils().isInRegion(clicked.getLocation(), mailboxRegion)) return;
		final BearFair21User user = userService.get(event.getPlayer());
		if (user.getQuestStage_MGN() != QuestStage.STEP_TWO) return;

		if (!FixableDevice.LAPTOP.hasBroken(event.getPlayer()) && !FixableDevice.LAPTOP.hasFixed(event.getPlayer())) {
			user.setQuestStage_MGN(QuestStage.STEP_THREE);
			userService.save(user);
			Quests.giveItem(event.getPlayer(), FixableDevice.LAPTOP.getBroken());
		}
	}

	@EventHandler
	public void onRightClickLaptop(PlayerInteractEvent event) {
		if (!BearFair21.canDoBearFairQuest(event.getPlayer())) return;
		if (!ActionGroup.CLICK.applies(event) || isNullOrAir(event.getItem())) return;
		if (!isTypeAndNameEqual(FixableDevice.LAPTOP.getBroken(), event.getItem())) return;

		new LaptopMenu().open(event.getPlayer());
	}

	// Beacons

	private static final List<Location> beaconButtons = new ArrayList<>(List.of(
		BearFair21.locationOf(-9, 154, -218),
		BearFair21.locationOf(151, 139, -20),
		BearFair21.locationOf(-108, 158, 13)
	));

	@EventHandler
	public void onRightClickBeaconButton(PlayerInteractEvent event) {
		final Player player = event.getPlayer();
		if (!BearFair21.canDoBearFairQuest(player)) return;
		if (!ActionGroup.CLICK.applies(event)) return;
		final Block block = event.getClickedBlock();
		if (BlockUtils.isNullOrAir(block)) return;
		if (block.getType() != Material.STONE_BUTTON) return;
		final Location location = block.getLocation();
		if (!beaconButtons.contains(location)) return;
		final BearFair21User user = userService.get(player);
		if (user.getQuestStage_MGN() != QuestStage.STEP_SIX) return;
		if (user.getMgn_beaconsActivated().contains(location)) return;

		user.getMgn_beaconsActivated().add(location);
		userService.save(user);

		new SoundBuilder(Sound.BLOCK_BEACON_ACTIVATE).receiver(player).location(location).play();
	}

	private static final String gravwellRegion = "bearfair21_main_gravwell";

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		final Player player = event.getPlayer();
		final Block block = event.getBlock();
		if (!BearFair21.canDoBearFairQuest(player)) return;
		if (block.getType() != Material.LODESTONE) return;
		if (!getWGUtils().isInRegion(block.getLocation(), gravwellRegion)) return;
		event.setCancelled(true);

		final BearFair21User user = userService.get(player);
		ClientsideContentManager.addCategory(user, ContentCategory.GRAVWELL);
		player.getInventory().removeItem(MainIsland.getGravwell().build());
		user.setQuestStage_MGN(QuestStage.STEP_SEVEN);
		userService.save(user);

		new SoundBuilder(Sound.BLOCK_BEACON_ACTIVATE).receiver(player).location(block).play();
	}

	// Speakers

	@Getter
	@AllArgsConstructor
	private enum AxelSpeakerPart {
		SUBWOOFER(BearFair21.locationOf(-165, 149, -215), Material.LODESTONE),
		TANGLED_WIRE(BearFair21.locationOf(-167, 148, -214), Material.CRIMSON_ROOTS),
		SPEAKER_HEAD(BearFair21.locationOf(-169, 148, -218), Material.HOPPER),
		AUX_PORT(BearFair21.locationOf(-167, 146, -214), Material.CONDUIT),
		;

		private final Location location;
		private final Material material;

		private static AxelSpeakerPart of(Location location) {
			for (AxelSpeakerPart part : values())
				if (part.getLocation().equals(location))
					return part;
			return null;
		}

		public static boolean hasAllItems(Player player) {
			for (AxelSpeakerPart part : values())
				if (!player.getInventory().containsAtLeast(part.getDisplayItem(), 1))
					return false;
			return true;
		}

		public static boolean hasAnyItems(Player player) {
			for (AxelSpeakerPart part : values())
				if (player.getInventory().containsAtLeast(part.getDisplayItem(), 1))
					return true;
			return false;
		}

		private ItemStack getDisplayItem() {
			return new ItemBuilder(material).name(camelCase(name())).undroppable().unplaceable().build();
		}
	}

	private static final Supplier<ItemBuilder> speaker = () -> ItemBuilder.fromHeadId("2126").name("Speaker").undroppable().unplaceable();

	private static final List<Location> speakerLocations = new ArrayList<>(List.of(
		BearFair21.locationOf(-182, 142, -156),
		BearFair21.locationOf(-178, 142, -156),
		BearFair21.locationOf(-177, 144, -150),
		BearFair21.locationOf(-183, 144, -150)
	));

	@EventHandler
	public void onClickSpeaker(PlayerInteractEvent event) {
		final Player player = event.getPlayer();
		if (!BearFair21.canDoBearFairQuest(player)) return;
		if (!ActionGroup.CLICK.applies(event) || isNullOrAir(event.getItem())) return;
		final Block block = event.getClickedBlock();
		if (BlockUtils.isNullOrAir(block)) return;
		if (block.getType() != Material.PLAYER_HEAD) return;
		final Location location = block.getLocation();
		if (!speakerLocations.contains(location)) return;
		event.setCancelled(true);

		if (!isSameHead(event.getItem(), speaker.get().build())) return;

		final BearFair21User user = userService.get(player);
		if (user.getMgn_speakersFixed().contains(location)) return;
		user.getMgn_speakersFixed().add(location);
		userService.save(user);
		player.getInventory().removeItem(new ItemBuilder(event.getItem()).amount(1).build());
	}

	private static final Location basementSpeakerLocation = BearFair21.locationOf(-188, 137, -188);

	@EventHandler
	public void onClickBasementSpeaker(PlayerInteractEvent event) {
		final Player player = event.getPlayer();
		if (!BearFair21.canDoBearFairQuest(player)) return;
		if (!ActionGroup.CLICK.applies(event)) return;
		final Block block = event.getClickedBlock();
		if (BlockUtils.isNullOrAir(block)) return;
		if (block.getType() != Material.PLAYER_HEAD) return;
		final Location location = block.getLocation();
		if (!basementSpeakerLocation.equals(location)) return;

		final BearFair21User user = userService.get(player);
		if (user.getQuestStage_MGN() != QuestStage.STEP_EIGHT) return;
		if (user.isMgn_foundSpeaker()) return;
		ClientsideContentManager.addCategory(user, ContentCategory.SPEAKER);
		user.setMgn_foundSpeaker(true);
		userService.save(user);
		Quests.giveItem(player, speaker.get().build());
	}

	@EventHandler
	public void onClickSpeakerPart(PlayerInteractEvent event) {
		if (!BearFair21.canDoBearFairQuest(event)) return;
		final Player player = event.getPlayer();
		if (WorldGuardEditCommand.canWorldGuardEdit(player)) return;
		if (BlockUtils.isNullOrAir(event.getClickedBlock())) return;
		Block block = event.getClickedBlock().getRelative(event.getBlockFace());

		final BearFair21User user = new BearFair21UserService().get(event.getPlayer());
		if (user.getQuestStage_MGN() != QuestStage.STEP_EIGHT) return;

		final Location location = block.getLocation();
		final AxelSpeakerPart part = AxelSpeakerPart.of(location);
		if (part == null) return;

		ClientsideContentManager.removeCategory(user, ContentCategory.valueOf("SPEAKER_PART_" + part.name()));
		final ItemStack item = part.getDisplayItem();
		if (!player.getInventory().containsAtLeast(item, 1))
			Quests.giveItem(player, item);
	}

	@Getter
	private static final ItemBuilder carKey = new ItemBuilder(Material.TRIPWIRE_HOOK).name("Car Key").undroppable().unplaceable();
	private static final String trunkRegion = "bearfair21_main_trunk";

	@EventHandler
	public void onClickTrunk(PlayerInteractEvent event) {
		if (!BearFair21.canDoBearFairQuest(event)) return;
		final Player player = event.getPlayer();
		if (WorldGuardEditCommand.canWorldGuardEdit(player)) return;
		final Block block = event.getClickedBlock();
		if (BlockUtils.isNullOrAir(block)) return;
		if (!getWGUtils().isInRegion(block.getLocation(), trunkRegion)) return;
		final BearFair21User user = new BearFair21UserService().get(event.getPlayer());
		if (user.getQuestStage_MGN() != QuestStage.STEP_EIGHT) return;
		if (user.isMgn_openedTrunk()) return;
		if (!player.getInventory().containsAtLeast(carKey.build(), 1)) return;

		player.getInventory().removeItem(carKey.build());
		new TrunkMenu().open(player);
	}

	private static final Supplier<ItemBuilder> slightlyDamagedSpeaker = () -> ItemBuilder.fromHeadId("2126").name("&cSlightly Damaged Speaker").undroppable().unplaceable();

	private static class TrunkMenu extends MenuUtils implements InventoryProvider {

		@Override
		public void open(Player viewer, int page) {
			SmartInventory.builder()
				.provider(this)
				.title("Car Trunk")
				.size(3, 9)
				.build()
				.open(viewer, page);
		}

		@Override
		public void init(Player player, InventoryContents contents) {
			contents.set(0, 3, ClickableItem.empty(slightlyDamagedSpeaker.get().build()));
			contents.set(2, 5, ClickableItem.empty(slightlyDamagedSpeaker.get().build()));
			contents.set(1, 2, ClickableItem.empty(FishingLoot.BROKEN_CD.getItem()));
			contents.set(0, 8, ClickableItem.empty(FishingLoot.BROKEN_CD.getItem()));
			contents.set(1, 6, ClickableItem.empty(FishingLoot.OLD_BOOTS.getItem()));
			contents.setEditable(SlotPos.of(0, 3), true);
			contents.setEditable(SlotPos.of(2, 5), true);
			contents.setEditable(SlotPos.of(1, 2), true);
			contents.setEditable(SlotPos.of(0, 8), true);
			contents.setEditable(SlotPos.of(1, 6), true);
			new BearFair21UserService().edit(player, user -> user.setMgn_openedTrunk(true));
		}

	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onInvClose(InventoryCloseEvent event) {
		Player player = (Player) event.getPlayer();
		Optional<SmartInventory> inv = SmartInvsPlugin.manager().getInventory(player);
		if (inv.isEmpty()) return;
		if (!(inv.get().getProvider() instanceof TrunkMenu)) return;

		ItemStack[] menuContents = event.getInventory().getContents();
		for (ItemStack item : menuContents)
			PlayerUtils.giveItem(player, item);
	}

	// Phone

	@NotNull
	private static Location getPhoneLocation() {
		return BearFair21.getWGUtils().toLocation(BearFair21.getWGUtils().getProtectedRegion("bearfair21_minigamenight_phone").getMinimumPoint());
	}

	private static ParticleBuilder getPhoneParticles() {
		return new ParticleBuilder(Particle.VILLAGER_HAPPY)
			.location(getPhoneLocation().toCenterLocation())
			.offset(.25, .25, .25)
			.count(2)
			.extra(.01);
	}

	private static final Consumer<Player> ringingSound = player -> {
		ActionBarUtils.sendActionBar(player, "&c*ring ring*");
		Location location = getPhoneLocation();

		int wait = 0;
		for (int i = 0; i < 5; i++) {
			addTaskId(player, Tasks.wait(wait += 2, () -> {
				new SoundBuilder(Sound.BLOCK_NOTE_BLOCK_BELL).receiver(player).location(location).volume(0.5).pitchStep(0).play();
				getPhoneParticles().receivers(player).spawn();
			}));
		}
	};

	private static final Map<UUID, List<Integer>> taskIds = new HashMap<>();

	public static void addTaskId(Player player, int taskId) {
		taskIds.computeIfAbsent(player.getUniqueId(), $ -> new ArrayList<>()).add(taskId);
	}

	public static void startPhoneRinging(Player player) {
		if (new CooldownService().check(player, "bf21-phone", Time.SECOND.x(15)))
			for (int i = 0; i < 5; i++)
				addTaskId(player, Tasks.wait(Time.SECOND.x(i * 2), () -> ringingSound.accept(player)));
	}

	public static void startOutgoingPhoneCall(Player player, Runnable pickup) {
		ringingSound.accept(player);
		Tasks.wait(Time.SECOND.x(2), () -> ringingSound.accept(player));
		Tasks.wait(Time.SECOND.x(4), pickup);
	}

	public static void stopPhoneRinging(Player player) {
		taskIds.computeIfAbsent(player.getUniqueId(), $ -> new ArrayList<>()).forEach(Tasks::cancel);
		SoundUtils.stopSound(player, Sound.BLOCK_NOTE_BLOCK_BELL);
	}

	@EventHandler
	public void onClickPhone(PlayerInteractEntityEvent event) {
		if (!BearFair21.canDoBearFairQuest(event)) return;
		Entity entity = event.getRightClicked();
		final Player player = event.getPlayer();
		if (WorldGuardEditCommand.canWorldGuardEdit(player)) return;
		if (entity.getType() != EntityType.ITEM_FRAME) return;
		if (!getWGUtils().isInRegion(entity.getLocation(), phoneRegion)) return;
		event.setCancelled(true);

		final BearFair21User user = new BearFair21UserService().get(event.getPlayer());
		if (user.getQuestStage_MGN() == QuestStage.STEP_TWO) {
			stopPhoneRinging(event.getPlayer());
			Talker.sendScript(event.getPlayer(), MinigameNightNPCs.FREDRICKSON);
		} else if (user.getQuestStage_MGN() == QuestStage.STEP_FOUR) {
			startOutgoingPhoneCall(event.getPlayer(), () -> Talker.sendScript(event.getPlayer(), MinigameNightNPCs.FREDRICKSON));
		} else if (user.getQuestStage_MGN() == QuestStage.STEP_FIVE) {
			stopPhoneRinging(event.getPlayer());
			Talker.sendScript(event.getPlayer(), MainNPCs.ARCHITECT);
		} else if (user.getQuestStage_MGN() == QuestStage.STEP_SIX) {
			stopPhoneRinging(event.getPlayer());
			Talker.sendScript(event.getPlayer(), MainNPCs.ADMIRAL);
		}else if (user.getQuestStage_MGN() == QuestStage.STEP_EIGHT) {
			stopPhoneRinging(event.getPlayer());
			Talker.sendScript(event.getPlayer(), MinigameNightNPCs.AXEL);
			user.setMgn_receivedAxelCall(true);
			userService.save(user);
		}
	}

	// Menus

	public static class XboxMenu extends MenuUtils implements InventoryProvider {

		@Override
		public void open(Player viewer, int page) {
			SmartInventory.builder()
				.provider(this)
				.title("Xbox Parts")
				.size(3, 9)
				.build()
				.open(viewer, page);
		}

		@Override
		public void init(Player player, InventoryContents contents) {
			addCloseItem(contents);
			contents.set(1, 1, ClickableItem.empty(new ItemBuilder(Material.GREEN_CARPET).name("Motherboard").customModelData(1).undroppable().unplaceable().build()));
			contents.set(1, 5, ClickableItem.empty(new ItemBuilder(Material.LIGHT_GRAY_CARPET).name("CPU").customModelData(1).undroppable().unplaceable().build()));
			contents.set(1, 7, ClickableItem.empty(new ItemBuilder(Material.LIGHT_GRAY_CARPET).name("Hard Drive").customModelData(2).undroppable().unplaceable().build()));

			fixableItemSlot(player, contents, SlotPos.of(1, 3), FixableItem.BATTERY);
		}

	}

	public static class LaptopMenu extends MenuUtils implements InventoryProvider {

		@Override
		public void open(Player viewer, int page) {
			SmartInventory.builder()
				.provider(this)
				.title("Laptop Parts")
				.size(3, 9)
				.build()
				.open(viewer, page);
		}

		@Override
		public void init(Player player, InventoryContents contents) {
			addCloseItem(contents);
			contents.set(0, 3, ClickableItem.empty(new ItemBuilder(Material.NETHERITE_INGOT).name("Battery").customModelData(1).undroppable().unplaceable().build()));
			contents.set(1, 7, ClickableItem.empty(new ItemBuilder(Material.LIGHT_GRAY_CARPET).name("CPU").customModelData(1).undroppable().unplaceable().build()));
			contents.set(2, 3, ClickableItem.empty(new ItemBuilder(Material.IRON_TRAPDOOR).name("Keyboard").customModelData(0).undroppable().unplaceable().build()));
			contents.set(2, 5, ClickableItem.empty(new ItemBuilder(Material.LIGHT_GRAY_CARPET).name("Hard Drive").customModelData(2).undroppable().unplaceable().build()));

			fixableItemSlot(player, contents, SlotPos.of(0, 5), FixableItem.SCREEN);
			fixableItemSlot(player, contents, SlotPos.of(1, 1), FixableItem.MOTHERBOARD);
		}

	}

	public static class ScrambledCablesMenu extends MenuUtils implements InventoryProvider {

		@Override
		public void open(Player viewer, int page) {
			SmartInventory.builder()
				.provider(this)
				.title("Scrambled Cables")
				.size(3, 9)
				.build()
				.open(viewer, page);
		}

		@Getter
		@AllArgsConstructor
		private enum Cable {
			GREEN, YELLOW, RED, BLUE, WHITE;

			private ItemStack getDisplayItem() {
				return new ItemBuilder(ColorType.of(name()).switchColor(Material.WHITE_CONCRETE)).name(camelCase(name()) + " Cable").undroppable().unplaceable().build();
			}

			private static List<Cable> randomized() {
				final ArrayList<Cable> cables = new ArrayList<>(List.of(values()));
				Collections.shuffle(cables);
				return cables;
			}
		}

		private static final List<Integer> allowedColumns = List.of(2, 3, 4, 5, 6);

		public boolean choose(AtomicInteger column, List<Integer> choices, List<Integer> exclude) {
			ArrayList<Integer> _choices = new ArrayList<>(choices);

			_choices.removeAll(exclude);
			if (_choices.isEmpty())
				return false;

			column.set(RandomUtils.randomElement(_choices));

			if (!allowedColumns.contains(column.get()))
				column.set(0);

			choices.remove(Integer.valueOf(column.get()));
			exclude.add(column.get());
			return true;
		}

		@Override
		public void init(Player player, InventoryContents contents) {
			addCloseItem(contents);

			Runnable validate = () -> Tasks.wait(2, () -> {
				final Inventory inventory = player.getOpenInventory().getTopInventory();

				for (Integer checking : allowedColumns) {
					List<Material> items = new ArrayList<>();
					for (int i = 0; i < 3; i++) {
						final ItemStack item = inventory.getItem(i * 9 + checking);
						if (isNullOrAir(item)) return;
						items.add(item.getType());
					}

					if (!(items.get(0) == items.get(1) && items.get(1) == items.get(2)))
						return;
				}

				Tasks.wait(Time.SECOND, () -> {
					player.closeInventory();
					userService.edit(player, user -> user.setMgn_unscrambledWiring(true));
					Quests.sound_obtainItem(player);
				});
			});

			Utils.attempt(100, () -> {
				final List<List<Integer>> rows = List.of(new ArrayList<>(allowedColumns), new ArrayList<>(allowedColumns), new ArrayList<>(allowedColumns));
				for (Cable cable : Cable.randomized()) {
					final ItemStack item = cable.getDisplayItem();
					final List<AtomicInteger> columns = List.of(new AtomicInteger(), new AtomicInteger(), new AtomicInteger());

					Utils.attempt(100, () -> {
						try {
							final List<Integer> exclude = new ArrayList<>();
							if (!choose(columns.get(0), rows.get(0), exclude)) return false;
							if (!choose(columns.get(1), rows.get(1), exclude)) return false;
							if (!choose(columns.get(2), rows.get(2), exclude)) return false;
							return true;
						} catch (Exception ex) {
							return false;
						}
					});

					final Iterator<AtomicInteger> iterator = columns.iterator();

					for (int row = 0; row < 3; row++) {
						final int column = iterator.next().get();
						if (!allowedColumns.contains(column))
							return false;

						final SlotPos slot = SlotPos.of(row, column);
						contents.set(slot, ClickableItem.from(item, e -> validate.run()));
						contents.setEditable(slot, true);
					}
				}
				return true;
			});
		}

	}

	public static class RouterMenu extends MenuUtils implements InventoryProvider {

		@Override
		public void open(Player viewer, int page) {
			SmartInventory.builder()
				.provider(this)
				.title("Router Parts")
				.size(3, 9)
				.build()
				.open(viewer, page);
		}

		@Getter
		@AllArgsConstructor
		private enum RouterParts {
			POWER_CORD(Material.REDSTONE, 0, SlotPos.of(2, 3), SlotPos.of(1, 7)),
			ETHERNET_CABLE(Material.END_ROD, 0, SlotPos.of(2, 4), SlotPos.of(1, 1)),
			FIBER_OPTIC_CABLE(Material.TRIPWIRE_HOOK, 0, SlotPos.of(2, 5), SlotPos.of(0, 4)),
			;

			private final Material material;
			private final int customModelData;
			private final SlotPos from, to;

			private ItemStack getDisplayItem() {
				return new ItemBuilder(material).customModelData(customModelData).name(camelCase(name())).undroppable().unplaceable().build();
			}
		}

		@Override
		public void init(Player player, InventoryContents contents) {
			addCloseItem(contents);

			for (RouterParts part : RouterParts.values()) {
				ItemStack item = part.getDisplayItem();
				contents.set(part.getFrom(), ClickableItem.empty(item));
				contents.setEditable(part.getFrom(), true);

				contents.set(part.getTo(), ClickableItem.from(new ItemStack(Material.BARRIER), e -> {
					if (item.equals(player.getItemOnCursor())) {
						player.setItemOnCursor(new ItemStack(Material.AIR));
						contents.set(part.getTo(), ClickableItem.empty(item));

						for (RouterParts checking : RouterParts.values()) {
							final Optional<ClickableItem> destination = contents.get(checking.getTo());
							if (destination.isPresent())
								if (!destination.get().getItem().equals(checking.getDisplayItem()))
									return;
						}

						Tasks.wait(Time.SECOND, () -> {
							player.closeInventory();
							userService.edit(player, user -> user.setMgn_setupRouter(true));
							Quests.sound_obtainItem(player);
						});
					}
				}));
			}
		}

	}

	// Main island

	private static final String routerRegion = "bearfair21_main_router";
	private static final String fiberCableRegion = "bearfair21_main_fibercable";
	private static final String scrambledCablesRegion = "bearfair21_main_scrambledcables";

	@EventHandler
	public void onClickConstructionSiteItemFrame(PlayerInteractEntityEvent event) {
		if (!BearFair21.canDoBearFairQuest(event)) return;

		final Player player = event.getPlayer();
		if (WorldGuardEditCommand.canWorldGuardEdit(player)) return;

		Entity entity = event.getRightClicked();
		if (entity.getType() != EntityType.ITEM_FRAME) return;
		event.setCancelled(true);

		final BearFair21User user = new BearFair21UserService().get(player);
		if (user.getQuestStage_MGN() != QuestStage.STEP_FIVE) return;

		final WorldGuardUtils WGUtils = getWGUtils();
		if (WGUtils.isInRegion(entity.getLocation(), fiberCableRegion)) {
			if (!user.isMgn_connectWiring()) {
				ClientsideContentManager.addCategory(user, ContentCategory.CABLE);
				user.setMgn_connectWiring(true);
				Quests.sound_obtainItem(player);
				userService.save(user);
			}
		} else if (WGUtils.isInRegion(entity.getLocation(), scrambledCablesRegion)) {
			if (!user.isMgn_unscrambledWiring())
				new ScrambledCablesMenu().open(player);
		} else if (WGUtils.isInRegion(entity.getLocation(), routerRegion))
			if (!user.isMgn_setupRouter())
				new RouterMenu().open(player);
	}

	// Common

	private final String phoneRegion = getRegion("phone");
	private final String mailboxRegion = getRegion("mailbox");

	private interface Fixable {

		default boolean hasBroken(HasPlayer player) {
			return player.getPlayer().getInventory().containsAtLeast(getBroken(), 1);
		}

		default boolean hasFixed(HasPlayer player) {
			return player.getPlayer().getInventory().containsAtLeast(getFixed(), 1);
		}

		ItemStack getBroken();

		ItemStack getFixed();

	}

	@Getter
	@AllArgsConstructor
	private enum FixableDevice implements Fixable {
		XBOX(
			ItemBuilder.fromHeadId("43417").name("&cTrent's Broken Xbox One").lore("&eClick to open").undroppable().unplaceable().build(),
			ItemBuilder.fromHeadId("43417").name("&aTrent's Fixed Xbox One").lore("&eClick to open").undroppable().unplaceable().build(),
			null,
			user -> {
				user.getNextStepNPCs().add(MinigameNightNPCs.TRENT.getNpcId());
				user.setQuestStage_MGN(QuestStage.STEP_ONE);
			}
		),
		LAPTOP(
			new ItemBuilder(Material.POLISHED_BLACKSTONE_PRESSURE_PLATE).customModelData(1).name("&cFredrickson's Broken Laptop").lore("&eClick to open").undroppable().unplaceable().build(),
			new ItemBuilder(Material.POLISHED_BLACKSTONE_PRESSURE_PLATE).customModelData(1).name("&aFredrickson's Fixed Laptop").lore("&eClick to open").undroppable().unplaceable().build(),
			user -> user.isMgn_laptopScreen() && user.isMgn_laptopMotherboard(),
			user -> user.setQuestStage_MGN(QuestStage.STEP_FOUR)
		),
		;

		private final ItemStack broken, fixed;
		private final Predicate<BearFair21User> finalizePredicate;
		private final Consumer<BearFair21User> onFinalize;
	}

	@Getter
	@AllArgsConstructor
	private enum FixableItem implements Fixable {
		BATTERY(
			FixableDevice.XBOX,
			new ItemBuilder(Material.NETHERITE_INGOT).customModelData(1).name("&cTrent's Broken Xbox One Battery").undroppable().unplaceable().build(),
			new ItemBuilder(Material.NETHERITE_INGOT).customModelData(1).name("&aTrent's Fixed Xbox One Battery").undroppable().unplaceable().build(),
			null,
			null
		),
		SCREEN(
			FixableDevice.LAPTOP,
			new ItemBuilder(Material.GLASS_PANE).customModelData(1).name("&cFredrickson's Broken Laptop Screen").undroppable().unplaceable().build(),
			new ItemBuilder(Material.GLASS_PANE).customModelData(1).name("&aFredrickson's Fixed Laptop Screen").undroppable().unplaceable().build(),
			user -> user.setMgn_laptopScreen(true),
			BearFair21User::isMgn_laptopScreen
		),
		MOTHERBOARD(
			FixableDevice.LAPTOP,
			new ItemBuilder(Material.GREEN_CARPET).customModelData(1).name("&cFredrickson's Broken Laptop Motherboard").undroppable().unplaceable().build(),
			new ItemBuilder(Material.GREEN_CARPET).customModelData(1).name("&aFredrickson's Fixed Laptop Motherboard").undroppable().unplaceable().build(),
			user -> user.setMgn_laptopMotherboard(true),
			BearFair21User::isMgn_laptopMotherboard
		),
		;

		private final FixableDevice device;
		private final ItemStack broken, fixed;
		private final Consumer<BearFair21User> onFix;
		private final Predicate<BearFair21User> alreadyFixedPredicate;

		@Contract("null -> null")
		public static FixableItem ofBroken(ItemStack itemStack) {
			if (!isNullOrAir(itemStack))
				for (FixableItem item : values())
					if (isTypeAndNameEqual(item.getBroken(), itemStack))
						return item;
			return null;
		}
	}

	protected static void fixableItemSlot(Player player, InventoryContents contents, SlotPos slot, FixableItem item) {
		final BearFair21UserService userService = new BearFair21UserService();
		final BearFair21User user = userService.get(player);
		final PlayerInventory inv = player.getInventory();

		final ItemStack broken = item.getBroken();
		final ItemStack fixed = item.getFixed();
		if (item.getAlreadyFixedPredicate() != null && item.getAlreadyFixedPredicate().test(user))
			contents.set(slot, ClickableItem.empty(fixed));
		else if (item.hasBroken(player) || item.hasFixed(player)) {
			contents.set(slot, ClickableItem.from(new ItemBuilder(Material.BARRIER).name("&f" + camelCase(item)).build(), e -> {
				if (fixed.equals(player.getItemOnCursor())) {
					player.setItemOnCursor(new ItemStack(Material.AIR));
					contents.set(slot, ClickableItem.empty(fixed));

					final Consumer<BearFair21User> onFix = item.getOnFix();
					final Predicate<BearFair21User> finalizePredicate = item.getDevice().getFinalizePredicate();
					final Consumer<BearFair21User> onFinalize = item.getDevice().getOnFinalize();

					Runnable finalize = () -> Tasks.wait(Time.SECOND, () -> {
						player.closeInventory();
						inv.removeItem(item.getDevice().getBroken());
						Quests.giveItem(player, item.getDevice().getFixed());
						if (onFinalize != null)
							userService.edit(user, onFinalize);
					});

					if (onFix != null)
						userService.edit(user, onFix);

					if (finalizePredicate != null) {
						if (finalizePredicate.test(user))
							finalize.run();
					} else
						finalize.run();
				}
			}));
		} else {
			contents.set(slot, ClickableItem.empty(broken));
			contents.setEditable(slot, true);
		}

	}

}
