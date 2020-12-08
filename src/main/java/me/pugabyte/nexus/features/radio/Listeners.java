package me.pugabyte.nexus.features.radio;

import com.xxmicloxx.NoteBlockAPI.event.SongNextEvent;
import com.xxmicloxx.NoteBlockAPI.model.Song;
import com.xxmicloxx.NoteBlockAPI.songplayer.PositionSongPlayer;
import com.xxmicloxx.NoteBlockAPI.songplayer.SongPlayer;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.models.radio.RadioConfig.Radio;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Set;
import java.util.UUID;

import static me.pugabyte.nexus.features.radio.RadioUtils.removePlayer;

public class Listeners implements Listener {

	public Listeners() {
		Nexus.registerListener(this);
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		Radio radio = RadioUtils.getListenedRadio(player);
		if (radio != null)
			removePlayer(player, radio);
	}

	@EventHandler
	public void onSongNext(SongNextEvent event) {
		SongPlayer songPlayer = event.getSongPlayer();
		Song song = songPlayer.getSong();
		Set<UUID> UUIDList = songPlayer.getPlayerUUIDs();

		for (UUID uuid : UUIDList) {
			Player player = Bukkit.getPlayer(uuid);
			if (player == null || !player.isOnline()) continue;

			if (songPlayer instanceof PositionSongPlayer) {
				Radio radio = RadioUtils.getRadio(songPlayer);
				if (radio != null) {
					if (RadioUtils.isInRangeOfRadiusRadio(player, radio))
						RadioUtils.actionBar(player, song, true);
				}
			} else
				RadioUtils.actionBar(player, song, true);
		}
	}

}
