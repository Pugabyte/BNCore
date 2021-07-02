package me.pugabyte.nexus.features.events;

import lombok.NonNull;
import me.pugabyte.nexus.features.events.models.QuestStage;
import me.pugabyte.nexus.features.events.y2020.pugmas20.quests.Quests.Pugmas20Quest;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.testquestuser.TestQuestUser;
import me.pugabyte.nexus.models.testquestuser.TestQuestUserService;

@Permission("group.admin")
public class TestQuestUserCommand extends CustomCommand {
	private TestQuestUserService service = new TestQuestUserService();
	private TestQuestUser user;

	public TestQuestUserCommand(@NonNull CommandEvent event) {
		super(event);
		user = service.get(player());
	}

	@Path("getStage <quest>")
	void getStage(Pugmas20Quest quest) {
		send(PREFIX + "Stage for quest " + camelCase(quest) + ": " + camelCase(user.getQuestProgress(quest).getQuestStage()));
	}

	@Path("setStage <quest> <stage>")
	void setStage(Pugmas20Quest quest, QuestStage stage) {
		user.getQuestProgress(quest).setQuestStage(stage);
		service.save(user);
		send(PREFIX + "Set stage to " + camelCase(stage) + " for quest " + camelCase(quest));
	}

}
