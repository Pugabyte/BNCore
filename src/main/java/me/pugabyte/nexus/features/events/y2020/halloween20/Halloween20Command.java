package me.pugabyte.nexus.features.events.y2020.halloween20;

import me.pugabyte.nexus.features.events.y2020.halloween20.models.ComboLockNumber;
import me.pugabyte.nexus.features.events.y2020.halloween20.models.Pumpkin;
import me.pugabyte.nexus.features.events.y2020.halloween20.models.QuestStage;
import me.pugabyte.nexus.features.events.y2020.halloween20.models.QuestStage.Combination;
import me.pugabyte.nexus.features.events.y2020.halloween20.models.SoundButton;
import me.pugabyte.nexus.features.events.y2020.halloween20.quest.Gate;
import me.pugabyte.nexus.features.events.y2020.halloween20.quest.menus.Halloween20Menus;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Aliases;
import me.pugabyte.nexus.framework.commands.models.annotations.Arg;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.halloween20.Halloween20Service;
import me.pugabyte.nexus.models.halloween20.Halloween20User;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Aliases("h20")
public class Halloween20Command extends CustomCommand {

	public Halloween20Command(CommandEvent event) {
		super(event);
	}

	@Path
	void tp() {
		player().resetPlayerTime();
		Halloween20User user = new Halloween20Service().get(player());
		if (user.getCombinationStage() == QuestStage.Combination.NOT_STARTED)
			Halloween20.start(player());
		else
			new Gate(player()).teleportIn();
	}

	@Path("progress [player]")
	void progress(@Arg("self") OfflinePlayer player) {
		Halloween20Service service = new Halloween20Service();
		Halloween20User user = service.get(player);
		send(PREFIX + "Progress:");

		if (user.getCombinationStage() == QuestStage.Combination.NOT_STARTED)
			send("&3Combination numbers: &cNot started");
		else if (user.getCombinationStage() == QuestStage.Combination.STARTED)
			send("&3Combination numbers: &e" + user.getFoundComboLockNumbers().size() + "/" + ComboLockNumber.values().length);
		else if (user.getCombinationStage() == QuestStage.Combination.COMPLETE)
			send("&3Combination numbers: &aComplete");

		if (user.getLostPumpkinsStage() == QuestStage.LostPumpkins.NOT_STARTED)
			send("&3Pumpkins: &cNot started (Talk to Jeffery)");
		else if (user.getLostPumpkinsStage() == QuestStage.LostPumpkins.STARTED)
			send("&3Pumpkins: &e" + user.getFoundPumpkins().size() + "/" + Pumpkin.values().length);
		else if (user.getLostPumpkinsStage() == QuestStage.LostPumpkins.FOUND_ALL)
			send("&3Pumpkins: &eFound all (Talk to Jeffery)");
		else if (user.getLostPumpkinsStage() == QuestStage.LostPumpkins.COMPLETE)
			send("&3Pumpkins: &aComplete");

		if (user.getFoundButtons().size() == SoundButton.values().length)
			send("&3Spooky Buttons: &aComplete");
		else
			send("&3Spooky Buttons: &e" + user.getFoundButtons().size() + "/" + SoundButton.values().length);
	}

	@Path("stats")
	@Permission("group.admin")
	void stats(@Arg("self") OfflinePlayer player) {
		Halloween20Service service = new Halloween20Service();
		List<Halloween20User> users = service.getAll();

		int foundButtons = 0;
		int foundButtonsComplete = 0;

		int foundPumpkins = 0;
		int foundPumpkinsComplete = 0;

		int foundComboNumbers = 0;
		int foundComboNumbersComplete = 0;

		long totalUsers = users.stream().filter(user ->
				user.getFoundButtons().size() > 0 || user.getFoundPumpkins().size() > 0 || user.getFoundComboLockNumbers().size() > 0
		).count();

		for (Halloween20User user : users) {
			foundButtons += user.getFoundButtons().size();
			if (user.getFoundButtons().size() == SoundButton.values().length)
				++foundButtonsComplete;

			foundPumpkins += user.getFoundPumpkins().size();
			if (user.getFoundPumpkins().size() == Pumpkin.values().length)
				++foundPumpkinsComplete;

			foundComboNumbers += user.getFoundComboLockNumbers().size();
			if (user.getCombinationStage() == Combination.COMPLETE)
				++foundComboNumbersComplete;
		}

		send("Total users: " + totalUsers);
		send("Found Buttons: " + (foundButtons / totalUsers) + " avg, " + foundButtonsComplete + " complete");
		send("Found Pumpkins: " + (foundPumpkins / totalUsers) + " avg, " + foundPumpkinsComplete + " complete");
		send("Found Combo Numbers: " + (foundComboNumbers / totalUsers) + " avg, " + foundComboNumbersComplete + " complete");
	}

	@Path("reset [player]")
	@Permission("group.admin")
	void reset(@Arg("self") OfflinePlayer player) {
		Halloween20Service service = new Halloween20Service();
		Halloween20User user = service.get(player);
		user.setLostPumpkinsStage(QuestStage.LostPumpkins.NOT_STARTED);
		user.setFoundPumpkins(new ArrayList<>());
		user.setCombinationStage(QuestStage.Combination.NOT_STARTED);
		user.setFoundComboLockNumbers(new ArrayList<>());
		service.save(user);
	}

	@Path("tp pumpkin original <number>")
	@Permission("group.admin")
	void teleportPumpkinOriginal(Pumpkin pumpkin) {
		player().teleport(pumpkin.getOriginal(), TeleportCause.COMMAND);
	}

	@Path("tp pumpkin end <number>")
	@Permission("group.admin")
	void teleportPumpkinEnd(Pumpkin pumpkin) {
		player().teleport(pumpkin.getEnd(), TeleportCause.COMMAND);
	}

	@Path("tp comboLockNumber <number>")
	@Permission("group.admin")
	void teleportComboLockNumber(ComboLockNumber number) {
		player().teleport(number.getLoc(), TeleportCause.COMMAND);
	}

	@Path("tp button <number>")
	@Permission("group.admin")
	void teleportButton(SoundButton number) {
		player().teleport(number.getLocation(), TeleportCause.COMMAND);
	}

	@Path("picture")
	@Permission("group.admin")
	void picture() {
		Halloween20Menus.openPicturePuzzle(player(), null);
	}

	@Path("flashCard")
	@Permission("group.admin")
	void flash() {
		Halloween20Menus.openFlashCardPuzzle(player(), null);
	}

	@Path("gate open")
	@Permission("group.admin")
	void openGate() {
		new Gate(player()).open();
	}

	@Path("gate close")
	@Permission("group.admin")
	void closeGate() {
		new Gate(player()).close();
	}

	@Path("test combo")
	@Permission("group.admin")
	void testCombo() {
		Halloween20Service service = new Halloween20Service();
		Halloween20User user = service.get(player());
		user.getFoundComboLockNumbers().clear();
		user.getFoundComboLockNumbers().addAll(Arrays.asList(ComboLockNumber.values()));
		service.save(user);
		Halloween20Menus.openComboLock(player());
	}

}
