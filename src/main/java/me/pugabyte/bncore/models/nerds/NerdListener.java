package me.pugabyte.bncore.models.nerds;

import me.pugabyte.bncore.BNCore;
import me.pugabyte.bncore.utils.Utils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class NerdListener implements Listener {

	public NerdListener() {
		BNCore.registerListener(this);
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		NerdService service = new NerdService();
		Nerd nerd = service.get(event.getPlayer());
		nerd.setLastJoin(Utils.timestamp(System.currentTimeMillis()));
		service.save(nerd);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		NerdService service = new NerdService();
		Nerd nerd = service.get(event.getPlayer());
		nerd.setLastQuit(Utils.timestamp(System.currentTimeMillis()));
		service.save(nerd);
	}

}
