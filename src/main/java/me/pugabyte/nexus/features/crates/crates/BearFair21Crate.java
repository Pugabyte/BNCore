package me.pugabyte.nexus.features.crates.crates;

import me.pugabyte.nexus.features.chat.Chat.Broadcast;
import me.pugabyte.nexus.features.commands.MuteMenuCommand.MuteMenuProvider.MuteMenuItem;
import me.pugabyte.nexus.features.crates.models.Crate;
import me.pugabyte.nexus.features.crates.models.CrateLoot;
import me.pugabyte.nexus.features.crates.models.CrateType;
import me.pugabyte.nexus.features.crates.models.events.CrateSpawnItemEvent;
import me.pugabyte.nexus.models.nickname.Nickname;
import me.pugabyte.nexus.utils.ColorType;
import org.bukkit.Color;
import org.bukkit.event.EventHandler;

import java.util.ArrayList;
import java.util.List;

public class BearFair21Crate extends Crate {
	@Override
	public CrateType getCrateType() {
		return CrateType.BEAR_FAIR_21;
	}

	@Override
	public List<String> getCrateHologramLines() {
		return new ArrayList<>() {{
			add("&3&l--=[+]=--");
			add("&3[+] &6&lBear Fair 21 Crate &3[+]");
			add("&3&l--=[+]=--");
		}};
	}

	@Override
	public Color[] getBandColors() {
		return new Color[]{ColorType.YELLOW.getBukkitColor(), ColorType.CYAN.getBukkitColor()};
	}

	@EventHandler
	public void onItemSpawn(CrateSpawnItemEvent event) {
		CrateLoot loot = event.getCrateLoot();
		if (getCrateType().equals(event.getCrateType()) && loot.getItems().contains(CrateType.MYSTERY.getKey())) {
			String message = "&e" + Nickname.of(event.getPlayer()) + " &3has received a &eMystery Crate Key &3from the &eBear Fair 21 Crate";
			Broadcast.all().prefix("Crates").message(message).muteMenuItem(MuteMenuItem.CRATES).send();
		}
	}
}
