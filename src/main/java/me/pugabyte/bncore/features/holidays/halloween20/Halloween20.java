package me.pugabyte.bncore.features.holidays.halloween20;

import lombok.Getter;
import me.pugabyte.bncore.BNCore;
import me.pugabyte.bncore.features.holidays.halloween20.models.QuestNPC;
import me.pugabyte.bncore.models.cooldown.CooldownService;
import me.pugabyte.bncore.utils.Time;
import me.pugabyte.bncore.utils.Utils;
import net.citizensnpcs.api.event.NPCClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class Halloween20 implements Listener {
	@Getter
	public static String region = "halloween20";
	@Getter
	public static World world = Bukkit.getWorld("safepvp");

	public Halloween20() {
		new LostPumpkins();
		BNCore.registerListener(this);
	}

	@EventHandler
	public void onNPCClick(NPCClickEvent event) {
		QuestNPC npc = QuestNPC.getByID(event.getNPC().getId());
		Utils.blast(npc.toString());
		if (npc == null) return;
		CooldownService cooldownService = new CooldownService();
		if (!cooldownService.check(event.getClicker(), "Halloween20_NPC", Time.SECOND.x(2)))
			return;
		npc.sendScript(event.getClicker());
	}


}