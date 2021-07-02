package me.pugabyte.nexus.features.events.y2020.bearfair20.islands;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import eden.utils.TimeUtils.Time;
import lombok.Getter;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.events.annotations.Region;
import me.pugabyte.nexus.features.events.models.BearFairIsland;
import me.pugabyte.nexus.features.events.models.BearFairIsland.NPCClass;
import me.pugabyte.nexus.features.events.models.Talker.TalkingNPC;
import me.pugabyte.nexus.features.events.y2020.bearfair20.BearFair20;
import me.pugabyte.nexus.features.events.y2020.bearfair20.islands.SummerDownUnderIsland.SummerDownUnderNPCs;
import me.pugabyte.nexus.models.bearfair20.BearFair20User;
import me.pugabyte.nexus.models.bearfair20.BearFair20UserService;
import me.pugabyte.nexus.models.cooldown.CooldownService;
import me.pugabyte.nexus.utils.BlockUtils;
import me.pugabyte.nexus.utils.ItemBuilder;
import me.pugabyte.nexus.utils.ItemUtils;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.RandomUtils;
import me.pugabyte.nexus.utils.StringUtils;
import me.pugabyte.nexus.utils.Tasks;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static me.pugabyte.nexus.features.events.y2020.bearfair20.BearFair20.getWGUtils;
import static me.pugabyte.nexus.features.events.y2020.bearfair20.quests.BFQuests.chime;
import static me.pugabyte.nexus.features.events.y2020.bearfair20.quests.BFQuests.itemLore;

@Region("summerdownunder")
@NPCClass(SummerDownUnderNPCs.class)
public class SummerDownUnderIsland implements Listener, BearFairIsland {
	@Override
	public String getEventRegion() {
		return BearFair20.getRegion();
	}

	public static ItemStack greatNortherns = new ItemBuilder(Material.BARREL).name("&aGreat Northerns").amount(1).lore(itemLore).build();
	private static ItemStack goldNugget = new ItemBuilder(Material.GOLD_NUGGET).lore(itemLore).amount(1).build();
	private static ItemStack foolsGold = new ItemBuilder(Material.GOLD_NUGGET).name("&6Fools Gold").lore(itemLore).amount(1).build();
	public static ItemStack sifter = new ItemBuilder(Material.BOWL).name("Sifter").lore(itemLore).amount(1).build();
	//
	public static ItemStack goldenSyrup = new ItemBuilder(Material.HONEY_BOTTLE).name("Golden Syrup").lore(itemLore).amount(1).build();
	public static ItemStack peanuts = new ItemBuilder(Material.BEETROOT_SEEDS).name("Peanuts").lore(itemLore).amount(1).build();
	private static ItemStack wheat = new ItemBuilder(Material.WHEAT).lore(itemLore).amount(1).build();
	private static ItemStack sugar = new ItemBuilder(Material.SUGAR).lore(itemLore).amount(1).build();
	//
	public static ItemStack anzacBiscuit = new ItemBuilder(Material.COOKIE).name("Anzac Biscuit").lore(itemLore).amount(1).glow().build();
	//
	private static List<String> greetings = Arrays.asList("G'day", "G'day mate", "How's it hangin'");

	public SummerDownUnderIsland() {
		Nexus.registerListener(this);
	}

	public enum SummerDownUnderNPCs implements TalkingNPC {
		// Quest NPCs
		ROLEX(2907) {
			@Override
			public List<String> getScript(Player player) {
				BearFair20UserService service = new BearFair20UserService();
				BearFair20User user = service.get(player);
				int step = user.getQuest_SDU_Step();

				List<String> startQuest = new ArrayList<>();
				startQuest.add("Hey bro, welcome to Queen's Island!");
				startQuest.add("wait 80");
				startQuest.add("<self> Thanks, Friend! I was hoping you could tell me about this thing called an ANZAC Biscuit?");
				startQuest.add("wait 80");
				startQuest.add("Don't know too much about it bro, but you should hit up the ANZAC Statue in the centre of town. " +
						"There's bound to be someone there you can talk to!");
				startQuest.add("wait 120");
				startQuest.add("Oh and don't forget to say G'day to everyone around town.");

				if (!user.isQuest_Main_Start())
					return Collections.singletonList(RandomUtils.randomElement(greetings));

				if (step >= 1)
					return Collections.singletonList("G'day mate, have you talked to Sir Jack yet?");

				user.setQuest_SDU_Start(true);
				nextStep(player); // 1
				return startQuest;
			}
		},
		SIR_JACK(2908) {
			@Override
			public List<String> getScript(Player player) {
				BearFair20UserService service = new BearFair20UserService();
				BearFair20User user = service.get(player);
				int step = user.getQuest_SDU_Step();

				List<String> startQuest = new ArrayList<>();
				startQuest.add("Ah, you must be the one Rolex messaged me about. What can an old man such as myself do for ya?");
				startQuest.add("wait 80");
				startQuest.add("<self> I was hoping you could tell me how to get whatever an ANZAC Biscuit is?");
				startQuest.add("wait 80");
				startQuest.add("Well mate, it's a biscuit the blokes and I used to get in the trenches back in the First World War. " +
						"It was sent all the bloody way from Australia to the Western Front. " +
						"It used to be a pretty common commodity in these parts and a damn good bikkie if I don't say so myself.");
				startQuest.add("wait 200");
				startQuest.add("Unfortunately we don't have any on hand, outta supply for months! " +
						"But I can tell ya what you're gonna need to make one yourself.");
				startQuest.add("wait 120");
				startQuest.add("You'll need the following ingredients; Wheat, Sugar, Peanuts & Golden Syrup.");
				startQuest.add("wait 80");
				startQuest.add("I highly recommend you visit the Queen's Island Pie Shop once you have what you need, I'm sure they'd be happy to help.");
				startQuest.add("wait 120");
				startQuest.add("<self> Thank you for your service. Any ideas where I should start?");
				startQuest.add("wait 80");
				startQuest.add("There'll be a cheeky farm across the bridge you can have a gawk at. " +
						"Afterwards, I'd recommend talking to my grandson Lachlan at the Pub. " +
						"He should be able to sort you out with something.");

				if (!user.isQuest_SDU_Start() || step < 1)
					return Collections.singletonList(RandomUtils.randomElement(greetings));

				if (step >= 2)
					return Arrays.asList(
							"Have you gathered all the ingredients up yet? You need Wheat, Sugar, Peanuts & Golden Syrup.",
							"wait 80",
							"For some wheat, there's a cheeky farm across the bridge you can have a gawk at. " +
									"Afterwards, I'd recommend talking to my grandson Lachlan at the Pub. " +
									"He should be able to sort you out with something.",
							"wait 120",
							"And I highly recommend you visit the Queen's Island Pie Shop once you have what you need, I'm sure they'd be happy to help."
					);

				nextStep(player); // 2
				return startQuest;
			}
		},
		FARMER(2752, new ArrayList<>() {{
			add("G'day g'day, the crops are fresh and lookin' real mean");
			add("wait 80");
			add("Oh you need it for the ANZAC Biscuit? Crikey, we haven't had those around here for months! " +
					"Take all the wheat you need chief, it's for a bloody great cause.");
			add("wait 120");
			add("You'll need Sugar too. I'm pretty sure the Main Island has some sugar cane you could abscond with mate. Good luck!");
		}}),
		LACHLAN(2747) {
			@Override
			public List<String> getScript(Player player) {
				BearFair20UserService service = new BearFair20UserService();
				BearFair20User user = service.get(player);
				int step = user.getQuest_SDU_Step();

				List<String> startQuest = new ArrayList<>();
				startQuest.add("Hey mate, my grandad told me you were headed here. Look, here's the situation. " +
						"I'd love to help out; but I need you to do me a favour first.");
				startQuest.add("wait 120");
				startQuest.add("Somehow, my delivery is missing a whole case of Great Northerns. One that I need to sell at the " +
						"gatho tonight with the town. I suspect it's been stolen. If you could find it for me and bring it back, " +
						"I'll give ya all the peanuts you need.");
				startQuest.add("wait 160");
				startQuest.add("Good luck mate! Happy hunting.");

				List<String> endQuest = new ArrayList<>();
				endQuest.add("No way! You're an absolute legend mate. When the gatho starts, we'd love to have you over. " +
						"Once the boys find out you rescued the bevvies they'll love ya!");
				endQuest.add("wait 120");
				endQuest.add("Here's the Peanuts, they're pretty high quality stuff. I can't just give you anything for that ANZAC Biscuit, hey?");
				endQuest.add("wait 80");
				endQuest.add("<self> What about the Golden Syrup?");
				endQuest.add("wait 80");
				endQuest.add("Yeah nah unfortunately I don't have any on hand, but if you knock on some doors around town there'll " +
						"definitely be some available. Thanks again mate.");

				if (!user.isQuest_SDU_Start() || step < 2)
					return Collections.singletonList(RandomUtils.randomElement(greetings));

				if (player.getInventory().contains(greatNortherns) && step == 3) {
					player.getInventory().remove(greatNortherns);
					Tasks.wait(Time.SECOND.x(7), () -> {
						chime(player);
						PlayerUtils.giveItem(player, peanuts);
					});
					nextStep(player); // 4
					return endQuest;
				}

				if (step >= 4)
					return Collections.singletonList("I've already given you my peanuts, have a good day now.");

				if (step >= 3)
					return Collections.singletonList("Have you found that crate of Great Northerns yet? Once you find it, return it to me please, and then I'll give you your peanuts.");

				nextStep(player); // 3
				return startQuest;
			}
		},
		ROBERT_MENZIES(2911) {
			@Override
			public List<String> getScript(Player player) {
				BearFair20UserService service = new BearFair20UserService();
				BearFair20User user = service.get(player);
				int step = user.getQuest_SDU_Step();

				List<String> startQuest = new ArrayList<>();
				startQuest.add("Hello mate. Golden Syrup hey? Hmm well. I think I should have some for you.");
				startQuest.add("wait 80");
				startQuest.add("Aha! Here it is. Have this mate. Fresh from a place called Landfall. Not really sure where that is on " +
						"Project Eden, but their syrup is to die for!");

				if (!user.isQuest_SDU_Start() || step < 4)
					return Collections.singletonList(RandomUtils.randomElement(greetings));

				if (step >= 5)
					return Collections.singletonList("I've already given you all my Golden Syrup, g'day!");

				nextStep(player); // 5
				Tasks.wait(Time.SECOND.x(6), () -> {
					PlayerUtils.giveItem(player, goldenSyrup);
					chime(player);
				});
				return startQuest;
			}
		},
		BRI(2744) {
			@Override
			public List<String> getScript(Player player) {
				BearFair20UserService service = new BearFair20UserService();
				BearFair20User user = service.get(player);
				int step = user.getQuest_SDU_Step();

				List<String> startQuest = new ArrayList<>();
				startQuest.add("Hi! What will it be for today?");
				startQuest.add("wait 80");
				startQuest.add("<self> Uh, hey! I was wondering if you guys could bake these ANZAC Biscuits for me? I have all the ingredients " +
						"but I can't make it myself. I'm new here!");
				startQuest.add("wait 80");
				startQuest.add("If it were up to me, sure! But you'll need to speak with my Manager first. He's really picky about these things.");

				List<String> endQuest_gold = new ArrayList<>();
				endQuest_gold.add("<self> Here's the gold your manager wanted. Can you bake it now please?");
				endQuest_gold.add("wait 80");
				endQuest_gold.add("Thanks! Alright now lets see what we can do...");
				endQuest_gold.add("wait 80");
				endQuest_gold.add("Here's your Biscuit! Like we said earlier, I'll take the rest and give them out around town! Best of luck in " +
						"your future endeavours. Feel free to stick around town for a while. We love visitors!");

				List<String> endQuest_fool = new ArrayList<>();
				endQuest_fool.add("<self> Here's the gold your manager wanted. Can you bake it now please?");
				endQuest_fool.add("wait 80");
				endQuest_fool.add("I can see you have Fool's Gold there, honestly, just give me that. He won't notice!");
				endQuest_fool.add("wait 80");
				endQuest_fool.add("Thanks! Alright now lets see what we can do...");
				endQuest_fool.add("wait 80");
				endQuest_fool.add("Here's your Biscuit! Like we said earlier, I'll take the rest and give them out around town! Best of luck in " +
						"your future endeavours. Feel free to stick around town for a while. We love visitors!");

				if (!user.isQuest_SDU_Start() || step < 5 || user.isQuest_SDU_Finish())
					return Collections.singletonList(RandomUtils.randomElement(greetings));

				ItemStack gold = hasFoolsGold(player);
				boolean fool = true;
				if (gold == null) {
					gold = hasGold(player);
					fool = false;
				}

				if (step == 8 && gold != null && hasAnzacIngredients(player)) {
					ItemStack finalGold = gold;
					int wait = 4;
					if (fool)
						wait = 8;

					Tasks.wait(Time.SECOND.x(wait), () -> {
						finalGold.setAmount(finalGold.getAmount() - 1);
						removeAnzacIngredients(player);
						Tasks.wait(Time.SECOND.x(5), () -> {
							PlayerUtils.giveItem(player, anzacBiscuit);
							chime(player);
						});
					});

					user.setQuest_SDU_Finish(true);
					nextStep(player); // 9
					if (fool)
						return endQuest_fool;
					else
						return endQuest_gold;
				}

				if (step >= 8)
					return Collections.singletonList("You need the ingredients to the Anzac Biscuit and a nugget of gold, fool's gold works too, my manager can't tell the difference.");

				if (step >= 6)
					return Collections.singletonList("You'll need to speak with my Manager first.");

				nextStep(player); // 6
				return startQuest;
			}
		},
		MANAGER(2745) {
			@Override
			public List<String> getScript(Player player) {
				BearFair20UserService service = new BearFair20UserService();
				BearFair20User user = service.get(player);
				int step = user.getQuest_SDU_Step();

				List<String> startQuest = new ArrayList<>();
				startQuest.add("What? Who are you, what do you want?");
				startQuest.add("wait 80");
				startQuest.add("<self> I was wondering if you could bake these ANZAC Biscuits for me? I just need one to take back with me and " +
						"Queen's Island can keep the rest!");
				startQuest.add("wait 80");
				startQuest.add("Excuse me? You'll need to pay for that mate! No pay, no bake.");
				startQuest.add("wait 80");
				startQuest.add("If you don't have any gold, there's a whole mineshaft nearby. " +
						"They usually mine coal but recently they've been sifting for gold.");

				if (!user.isQuest_SDU_Start() || step < 6)
					return Collections.singletonList(RandomUtils.randomElement(greetings));

				if (step >= 7)
					return Collections.singletonList("Did you forget already? If you don't have any gold, there's a whole mineshaft nearby. Now get out!");

				nextStep(player); // 7
				return startQuest;
			}
		},
		PROSPECTOR(2753) {
			@Override
			public List<String> getScript(Player player) {
				BearFair20UserService service = new BearFair20UserService();
				BearFair20User user = service.get(player);
				int step = user.getQuest_SDU_Step();

				List<String> startQuest = new ArrayList<>();
				startQuest.add("Xin Chao friend! You want to sift?");
				startQuest.add("wait 80");
				startQuest.add("Tuyet Dieu! Here, take the bowl and get to work in the water. You can keep any other gold or fool's gold you find.");

				if (!user.isQuest_SDU_Start() || step < 7)
					return Collections.singletonList(RandomUtils.randomElement(greetings));

				if (step >= 8)
					return Collections.singletonList("Take the bowl and get to work in the water.");

				nextStep(player); // 8
				Tasks.wait(Time.SECOND.x(5), () -> {
					PlayerUtils.giveItem(player, sifter);
					chime(player);
				});
				return startQuest;
			}
		},
		//
		// Clutter NPCs
		DYLAN(2915, Collections.singletonList("These snags are lookin' real hot boys hope you're ready")),
		MATT(2916, Collections.singletonList("OOOOOOH BOOOY IM READY FOR SOME SNAAAAAAAAAAGSSSS")),
		MAX(2917, Collections.singletonList("Alright gents, if the snags are ready let's get Lach and the boys from the pub here ASAP!")),
		TALITHA(2933, Collections.singletonList("I'll get the plates!")),
		DECLAN(2922, Collections.singletonList("Oiiii Lachlan ch..uck as anofer beer ya dawwgg!")),
		CAMERON(2921, Collections.singletonList("Shut Up Declan we need to meet the boys at the Ablett's!")),
		JOSH(2918, Collections.singletonList("Uh, yeah, I'm the deso tonight so just some Soft Drink if you got any mate.")),
		NIKKI(2944, Collections.singletonList("This view is amazing.")),
		NICOLE(2945, Collections.singletonList("I know right… I love farming.")),
		GRIFFIN(2931, Collections.singletonList("Lest we Forget.")),
		TRINITY(2932, Collections.singletonList("We Will Remember Them.")),
		RYAN(2923, Collections.singletonList("Hey mate, wanna have a go in the ‘Lux?")),
		FOREMAN(2927, Collections.singletonList("Who the bloody hell are ya? I have work to do mate, get lost!")),
		DRIVER(2930, Collections.singletonList("Oh man… Lachlan's gonna kill me. Where is that damn case?")),
		TALISHA(2939, Collections.singletonList("Can you have a squiz at the drinks and suggest anything good? I'm new in town!")),
		TAYLOR(2940, Collections.singletonList("Hey! You must be new here, hope you're having a ball!")),
		LUCY(2941, Collections.singletonList("We should probably get going soon, I think the party's almost started. Wanna tag along?")),
		CHRIS(2942, Collections.singletonList("MmmmMmm! Just as good as I remember. Hope I'm not late for the gatho!"));

		@Getter
		private final int npcId;
		@Getter
		private final List<String> script;

		@Override
		public String getName() {
			return this.name();
		}

		SummerDownUnderNPCs(int npcId) {
			this.npcId = npcId;
			this.script = new ArrayList<>();
		}

		SummerDownUnderNPCs(int npcId, List<String> script) {
			this.npcId = npcId;
			this.script = script;
		}
	}

	private static void nextStep(Player player) {
		BearFair20UserService service = new BearFair20UserService();
		BearFair20User user = service.get(player);
		int step = user.getQuest_SDU_Step() + 1;
		user.setQuest_SDU_Step(step);
		service.save(user);
	}

	private static ItemStack hasFoolsGold(Player player) {
		ItemStack[] contents = player.getInventory().getContents();
		for (ItemStack content : contents) {
			if (ItemUtils.isNullOrAir(content)) continue;
			if (!BearFair20.isBFItem(content)) continue;
			if (!content.getType().equals(Material.GOLD_NUGGET)) continue;

			String foolsGoldName = StringUtils.stripColor(foolsGold.getItemMeta().getDisplayName());
			String contentName = StringUtils.stripColor(content.getItemMeta().getDisplayName());
			if (contentName.equals(foolsGoldName)) {
				return content;
			}
		}

		return null;
	}

	private static ItemStack hasGold(Player player) {
		ItemStack[] contents = player.getInventory().getContents();
		for (ItemStack content : contents) {
			if (ItemUtils.isNullOrAir(content)) continue;
			if (!BearFair20.isBFItem(content)) continue;
			if (!content.getType().equals(Material.GOLD_NUGGET)) continue;
			return content;
		}
		return null;
	}

	private static boolean hasAnzacIngredients(Player player) {
		boolean hasWheat = player.getInventory().containsAtLeast(wheat, 1);
		boolean hasSugar = player.getInventory().containsAtLeast(sugar, 1);
		boolean hasPeanuts = player.getInventory().containsAtLeast(peanuts, 1);
		boolean hasSyrup = player.getInventory().containsAtLeast(goldenSyrup, 1);

		return hasWheat && hasSugar && hasPeanuts && hasSyrup;
	}

	private static void removeAnzacIngredients(Player player) {
		boolean wheatBool = true;
		boolean sugarBool = true;
		boolean peanutsBool = true;
		boolean syrupBool = true;
		for (ItemStack content : player.getInventory().getContents()) {
			if (ItemUtils.isNullOrAir(content)) continue;

			ItemStack item = content.clone();
			item.setAmount(1);
			if (item.equals(wheat) && wheatBool) {
				content.setAmount(content.getAmount() - 1);
				wheatBool = false;
			}

			if (item.equals(sugar) && sugarBool) {
				content.setAmount(content.getAmount() - 1);
				sugarBool = false;
			}

			if (item.equals(peanuts) && peanutsBool) {
				content.setAmount(content.getAmount() - 1);
				peanutsBool = false;
			}

			if (item.equals(goldenSyrup) && syrupBool) {
				content.setAmount(content.getAmount() - 1);
				syrupBool = false;
			}
		}
	}

	@EventHandler
	public void onClickBarrel(PlayerInteractEvent event) {
		if (event.getHand() != EquipmentSlot.HAND) return;

		ProtectedRegion region = getWGUtils().getProtectedRegion(getRegion());
		if (!getWGUtils().getRegionsAt(event.getPlayer().getLocation()).contains(region)) return;

		if (!BearFair20.enableQuests) return;
		Block clicked = event.getClickedBlock();
		if (BlockUtils.isNullOrAir(clicked)) return;

		Material material = clicked.getType();
		if (!material.equals(Material.BARREL)) return;

		Material under = clicked.getRelative(0, -1, 0).getType();
		if (!under.equals(Material.EMERALD_BLOCK)) return;

		Player player = event.getPlayer();
		BearFair20UserService service = new BearFair20UserService();
		BearFair20User user = service.get(player);
		int step = user.getQuest_SDU_Step();

		if (step == 3 && !player.getInventory().contains(greatNortherns)) {
			PlayerUtils.giveItem(player, greatNortherns);
			player.playSound(clicked.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 10, 2);
		}
		event.setCancelled(true);
	}

	@EventHandler
	public void onSift(PlayerInteractEvent event) {
		if (event.getHand() != EquipmentSlot.HAND) return;

		ProtectedRegion region = getWGUtils().getProtectedRegion(getRegion());
		if (!getWGUtils().getRegionsAt(event.getPlayer().getLocation()).contains(region)) return;

		if (!BearFair20.enableQuests) return;

		boolean water = false;

		Block lookingAt = event.getPlayer().getTargetBlockExact(5, FluidCollisionMode.ALWAYS);
		if (lookingAt != null && lookingAt.getType().equals(Material.WATER))
			water = true;
		else {
			Block clicked = event.getClickedBlock();
			if (!BlockUtils.isNullOrAir(clicked) && clicked.getBlockData() instanceof Waterlogged waterlogged) {
				if (waterlogged.isWaterlogged())
					water = true;
			}
		}

		if (!water) return;

		ItemStack tool = event.getItem();
		if (ItemUtils.isNullOrAir(tool)) return;
		if (!tool.equals(sifter)) return;

		// Player is sifting
		Player player = event.getPlayer();
		CooldownService cooldownService = new CooldownService();
		if (!cooldownService.check(player, "BF20_SDU_Sifting", Time.SECOND.x(2)))
			return;

		Location loc = player.getLocation();
		player.playSound(loc, Sound.ENTITY_HORSE_SADDLE, 0.5F, 0.5F);
		player.playSound(loc, Sound.UI_STONECUTTER_TAKE_RESULT, 0.5F, 0.5F);
		Tasks.wait(5, () -> {
			player.playSound(loc, Sound.ENTITY_HORSE_SADDLE, 0.5F, 0.5F);
			player.playSound(loc, Sound.UI_STONECUTTER_TAKE_RESULT, 0.5F, 0.5F);

			if (RandomUtils.chanceOf(10)) {
				if (RandomUtils.chanceOf(75))
					PlayerUtils.giveItem(player, foolsGold);
				else
					PlayerUtils.giveItem(player, goldNugget);
			}
		});
	}

}
