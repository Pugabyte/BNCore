package me.pugabyte.bncore.features.chat;

import me.pugabyte.bncore.BNCore;
import me.pugabyte.bncore.features.chat.alerts.AlertsFeature;
import me.pugabyte.bncore.features.chat.translator.Translator;
import org.bukkit.plugin.Plugin;

public class Chat {
	public static AlertsFeature alertsFeature;
	public static Translator translator;

	public Chat() {
		Plugin herochat = BNCore.getInstance().getServer().getPluginManager().getPlugin("Herochat");

		if (herochat != null && herochat.isEnabled()) {
			new ChatListener();
			alertsFeature = new AlertsFeature();
			translator = new Translator();
		}
	}

}
