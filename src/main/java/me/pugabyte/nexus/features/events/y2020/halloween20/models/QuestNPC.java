package me.pugabyte.nexus.features.events.y2020.halloween20.models;

import eden.utils.TimeUtils.Time;
import me.pugabyte.nexus.features.events.y2020.halloween20.Halloween20;
import me.pugabyte.nexus.models.banker.BankerService;
import me.pugabyte.nexus.models.banker.Transaction.TransactionCause;
import me.pugabyte.nexus.models.halloween20.Halloween20Service;
import me.pugabyte.nexus.models.halloween20.Halloween20User;
import me.pugabyte.nexus.models.shop.Shop.ShopGroup;
import me.pugabyte.nexus.utils.ItemBuilder;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.Tasks;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static me.pugabyte.nexus.utils.StringUtils.camelCase;

public enum QuestNPC {

	JEFFERY(3066) {
		@Override
		public List<String> getScript(Player player) {
			Halloween20Service service = new Halloween20Service();
			Halloween20User user = service.get(player);
			switch (user.getLostPumpkinsStage()) {
				case NOT_STARTED:
					user.setLostPumpkinsStage(QuestStage.LostPumpkins.STARTED);
					service.save(user);
					return Arrays.asList(
							"Oh no.. What am I going to tell everyone. I can't tell them that I have misplaced their pumpkins!",
							"wait 100",
							"I leave for 5 minutes and somebody stole them! Who would do such a thing?",
							"wait 80",
							"Do you think you could look for them for me? I could give you a nice reward if you find them."
					);
				case STARTED:
					return Arrays.asList(
							"I see you've found " + user.getFoundPumpkins().size() + " of my pumpkins.",
							"wait 80",
							"If you can help me find all 8, I'll give you a reward"
					);
				case FOUND_ALL:
					user.setLostPumpkinsStage(QuestStage.LostPumpkins.COMPLETE);
					service.save(user);
					Tasks.wait(160, () -> {
						ItemBuilder builder = new ItemBuilder(Material.ORANGE_SHULKER_BOX);
						builder.name("&5Pumpkin Pouch");
						for (Pumpkin pumpkin : Pumpkin.values())
							builder.shulkerBox(pumpkin.getOriginal().getBlock().getDrops().toArray(ItemStack[]::new));
						PlayerUtils.giveItem(player, builder.build());
						new BankerService().deposit(player, 5000, ShopGroup.SURVIVAL, TransactionCause.EVENT);
						PlayerUtils.send(player, "&a$5,000 has been added to your account.");
					});
					return Arrays.asList(
							"You found all my pumpkins! Thank you so much.",
							"wait 80",
							"For all your work, I'll give you these pumpkins to take home with you.",
							"wait 80",
							"I even wrapped them up nicely for you!",
							"wait 80",
							"Thanks again. Come back to see me whenever you can."
					);
				case COMPLETE:
					return Arrays.asList(
							"Thanks again for helping me find all those pumpkins!"
					);
			}
			return new ArrayList<>();
		}
	},

	PEDRO(3067) {
		@Override
		public List<String> getScript(Player player) {
			Halloween20Service service = new Halloween20Service();
			Halloween20User user = service.get(player);
			return switch (user.getCombinationStage()) {
				case NOT_STARTED -> Arrays.asList(
						"O hey there."
				);
				case STARTED -> Arrays.asList(
						"I might have a clue to the 3rd number of the combination.",
						"wait 80",
						"I think it has to do with the month of bear fair."
				);
				case COMPLETE -> Arrays.asList(
						"I heard you figured out the combination. That's great!"
				);
			};
		}
	},

	MARK(3068) {
		@Override
		public List<String> getScript(Player player) {
			Halloween20Service service = new Halloween20Service();
			Halloween20User user = service.get(player);
			return switch (user.getCombinationStage()) {
				case NOT_STARTED -> Arrays.asList(
						"Welcome to the party!"
				);
				case STARTED -> Arrays.asList(
						"If you're looking to solve the combination, I heard that the middle number is 1."
				);
				case COMPLETE -> Arrays.asList(
						"Now that you're free, it's party time!"
				);
			};
		}
	},

	ANA(3069) {
		@Override
		public List<String> getScript(Player player) {
			Halloween20Service service = new Halloween20Service();
			Halloween20User user = service.get(player);
			return switch (user.getCombinationStage()) {
				case NOT_STARTED -> Arrays.asList(
						"Hey there!",
						"wait 30",
						"I don't see many people over here anymore."
				);
				case STARTED -> Arrays.asList(
						"<self> Hi, i'm trying to get out of here but the gate I came through is locked. ",
						"wait 80",
						"<self> Do you happen to know the combination so that I can get out of here?",
						"wait 80",
						"No, but I heard it contains the date of Halloween (MM/DD), Maybe you should try that?",
						"wait 80",
						"Good luck!"
				);
				case COMPLETE -> Arrays.asList(
						"Welcome back!",
						"wait 30",
						"Feel free to visit any time, not many people hang out around here anymore."
				);
			};
		}
	},

	ISSAC(3070) {
		@Override
		public List<String> getScript(Player player) {
			Halloween20Service service = new Halloween20Service();
			Halloween20User user = service.get(player);
			return switch (user.getCombinationStage()) {
				case NOT_STARTED -> Arrays.asList(
						"Welcome! There's some food at the stall over there if you want some."
				);
				case STARTED -> Arrays.asList(
						"<self> Hi, I seem to be stuck in this city.",
						"wait 40",
						"<self> As soon as I went through that gate down there, it locked behind me.",
						"wait 80",
						"Ah yes, that gate only opens one way.",
						"wait 40",
						"Only the living can escape, and even then, you have to know the correct combination in order to leave.",
						"wait 100",
						"I can't be of much help, but I can tell you that the last number of the combination is an even number. That is all I know"
				);
				case COMPLETE -> Arrays.asList(
						"So you figured out the combination huh? That's good to hear."
				);
			};
		}
	},

	CARLOS(3073) {
		@Override
		public List<String> getScript(Player player) {
			Halloween20Service service = new Halloween20Service();
			Halloween20User user = service.get(player);
			return switch (user.getCombinationStage()) {
				case NOT_STARTED -> Arrays.asList(
						"I built a fire over there. Feel free to sit down and relax a bit."
				);
				case STARTED -> Arrays.asList(
						"Hey there, I'm carlos! Who are you?",
						"wait 40",
						"<self> I am <player>, It's nice to meet you carlos.",
						"wait 40",
						"<self> Do you by chance know how to get out of here? I can't get back through that gate I entered through.",
						"wait 100",
						"<self> All I know is that there's a combination, but I don't know what it is.",
						"wait 100",
						"I can give you a hint: How many staff members are there?",
						"wait 80",
						"While you try to figure that out, why don't you sit down and relax for a while? There's a nice fire over there."
				);
				case COMPLETE -> Arrays.asList(
						"Hey, glad to see your back! Did you figure out how to get out of here?",
						"wait 60",
						"<self> Yes, I did actually!",
						"wait 40",
						"Well, that's great!"
				);
			};
		}
	},

	AMELIA(3074) {
		@Override
		public List<String> getScript(Player player) {
			Halloween20Service service = new Halloween20Service();
			Halloween20User user = service.get(player);
			return switch (user.getCombinationStage()) {
				case NOT_STARTED -> Arrays.asList(
						"Hey, Welcome to our city."
				);
				case STARTED -> Arrays.asList(
						"Hey, I'm Amelia. What are you doing all the way up here?",
						"wait 60",
						"<self> I'm trying to find clues on how to get out of here. I appear to be stuck here.",
						"wait 60",
						"<self> I need to figure out the combination to the gate below.",
						"wait 60",
						"<self> Do you have any information that could be helpful?",
						"wait 60",
						"I believe somebody told me before that the number 7 is in there somewhere.",
						"wait 60",
						"I'm not sure where, but I think it's in there somewhere.",
						"wait 60",
						"<self> Ok, thanks!"
				);
				case COMPLETE -> Arrays.asList(
						"I'm glad to hear you figured out how to escape.",
						"wait 60",
						"Feel free to come back and visit some time, not many people venture all the way up here!"
				);
			};
		}
	},
	DIEGO(3075) {
		@Override
		public List<String> getScript(Player player) {
			Halloween20Service service = new Halloween20Service();
			Halloween20User user = service.get(player);
			switch (user.getCombinationStage()) {
				case NOT_STARTED:
					user.setCombinationStage(QuestStage.Combination.STARTED);
					service.save(user);
					return Arrays.asList(
							"Hello, welcome to the land of the Dead!",
							"wait 60",
							"O yes, that door. It doesn't open. It has been locked for a long time now. Most of us cannot leave here anyways.",
							"wait 100",
							"If you want to open it, you will have to unlock it. I hear there are clues laying around in the land of the dead. Maybe you can find some clues on how to unlock it.",
							"wait 100",
							"Try looking around here or in the city above."
					);
				case STARTED:
					Tasks.wait(Time.SECOND.x(10), () -> Halloween20.sendInstructions(player));
					return Arrays.asList(
							"Have you found any clues to try to open that gate?",
							"wait 60",
							"You might have some luck getting it open if you look for some numbers around here and on the platforms above."
					);
				case COMPLETE:
					return Arrays.asList(
							"Hey look, you figured out how to open that gate!",
							"wait 80",
							"Well since you can get out of here now, don't be afraid to come back and visit some time!"
					);
			}
			return new ArrayList<>();
		}
	};


	int npcId;

	QuestNPC(int id) {
		npcId = id;
	}

	public static QuestNPC getByID(int id) {
		for (QuestNPC value : QuestNPC.values())
			if (value.npcId == id) return value;
		return null;
	}

	public void sendScript(Player player) {
		List<String> script = getScript(player);
		if (script == null) return;
		AtomicReference<String> npcName = new AtomicReference<>("");

		AtomicInteger wait = new AtomicInteger(0);
		script.forEach(line -> {
			npcName.set(camelCase(name().replaceAll("_", " ")));
			npcName.set(npcName.get().replaceAll("[0-9]+", ""));
			if (line.toLowerCase().matches("^wait \\d+$"))
				wait.getAndAdd(Integer.parseInt(line.toLowerCase().replace("wait ", "")));
			else {
				line = line.replaceAll("<player>", player.getName());
				if (line.contains("<self>")) {
					npcName.set("&b&lYOU&f");
					line = line.replaceAll("<self> ", "");
				}
				String message = "&3" + npcName.get() + " &7> &f" + line;
				Tasks.wait(wait.get(), () -> {
					PlayerUtils.send(player, message);
					player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1F, 1F);
				});
			}
		});
	}

	public abstract List<String> getScript(Player player);

}
