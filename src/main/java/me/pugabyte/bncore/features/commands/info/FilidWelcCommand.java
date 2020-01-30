package me.pugabyte.bncore.features.commands.info;

import me.pugabyte.bncore.framework.commands.models.CustomCommand;
import me.pugabyte.bncore.framework.commands.models.annotations.Path;
import me.pugabyte.bncore.framework.commands.models.events.CommandEvent;

public class FilidWelcCommand extends CustomCommand {

	public FilidWelcCommand(CommandEvent event) {
		super(event);
	}

	@Path
	void run() {
		line(2);
		send("&eGreetings &b" + player().getName() + "&e!");
		send("&3Welcome to Bear Nation. My name is Filid. Glad to meet you :)");
		send("&3Here are some of the most important aspects of our &ecommunity &3and what makes Bear Nation unique.");
		line();
		json("&3[+] &eMinigames||cmd:/minigames||ttp:&3Join us on &eSaturdays&3, &e4&3-&e6 &ePM &eEST " +
				"\n&3for &eMinigame Night&3!\n" +
				"\n&eFor different timezones:" +
				"\n&3California: &eSat. 1-3 PM" +
				"\n&3London: &eSat. 9-11 PM" +
				"\n&3Sydney: &eSun. 6-8 AM" +
				"\n&eMake sure to join &c/discord &etoo!");
		json("&3[+] &eDiscord||cmd:/discord||ttp:&eA community skype-like program including " +
				"\n&etext chats and voice chats");
//		json("&3[+] &eDubtrack||cmd:/dubtrack||ttp:&eShare your music with the community");
		json("&3[+] &eWebsite||url:https://bnn.gg/||ttp:&eThe homepage for the server");
		json("&3[+] &eVoting||cmd:/vote||ttp:&eVote for our server to support us!");
		line();
		runCommand("curiositycake");

	}

}
