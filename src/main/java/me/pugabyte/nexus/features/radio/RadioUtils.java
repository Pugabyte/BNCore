package me.pugabyte.nexus.features.radio;

import com.xxmicloxx.NoteBlockAPI.model.FadeType;
import com.xxmicloxx.NoteBlockAPI.model.Playlist;
import com.xxmicloxx.NoteBlockAPI.model.RepeatMode;
import com.xxmicloxx.NoteBlockAPI.model.Song;
import com.xxmicloxx.NoteBlockAPI.model.SoundCategory;
import com.xxmicloxx.NoteBlockAPI.songplayer.Fade;
import com.xxmicloxx.NoteBlockAPI.songplayer.PositionSongPlayer;
import com.xxmicloxx.NoteBlockAPI.songplayer.RadioSongPlayer;
import com.xxmicloxx.NoteBlockAPI.songplayer.SongPlayer;
import me.lexikiq.HasPlayer;
import me.lexikiq.HasUniqueId;
import me.pugabyte.nexus.models.radio.RadioConfig;
import me.pugabyte.nexus.models.radio.RadioConfig.Radio;
import me.pugabyte.nexus.models.radio.RadioConfig.RadioType;
import me.pugabyte.nexus.models.radio.RadioConfigService;
import me.pugabyte.nexus.models.radio.RadioUser;
import me.pugabyte.nexus.models.radio.RadioUserService;
import me.pugabyte.nexus.utils.ActionBarUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class RadioUtils {

	public static void actionBar(HasPlayer player, Song song) {
		actionBar(player, song, false);
	}

	public static void actionBar(HasPlayer player, Song song, boolean nowPlaying) {
		String message = "&2&lCurrently Playing: &a";
		if (nowPlaying) message = "&2&lNow Playing: &a";
		message += " " + song.getTitle();
		ActionBarUtils.sendActionBar(player, message);
	}

	public static boolean isListening(UUID player, Radio radio) {
		return radio.getSongPlayer().getPlayerUUIDs().contains(player);
	}

	public static boolean isListening(HasUniqueId player, Radio radio) {
		return isListening(player.getUniqueId(), radio);
	}

	public static Radio getListenedRadio(HasPlayer player) {
		return getListenedRadio(player, false);
	}

	public static Radio getListenedRadio(HasPlayer player, boolean includeRadius) {
		for (Radio radio : getRadios()) {
			SongPlayer songPlayer = radio.getSongPlayer();

			if (songPlayer instanceof RadioSongPlayer) {
				if (isListening(player.getPlayer(), radio))
					return radio;

			} else if (songPlayer instanceof PositionSongPlayer) {
				if (isListening(player.getPlayer(), radio)) {
					if (includeRadius)
						if (isInRangeOfRadiusRadio(player, radio))
							return radio;
						else
							return radio;
				}
			}
		}

		return null;
	}

	public static Set<Radio> getRadios() {
		RadioConfigService configService = new RadioConfigService();
		RadioConfig config = configService.get0();
		return config.getRadios();
	}

	public static Set<Radio> getServerRadios() {
		Set<Radio> result = new HashSet<>();
		for (Radio radio : getRadios()) {
			if (radio.getType().equals(RadioType.SERVER))
				result.add(radio);
		}
		return result;
	}

	public static Radio getRadio(SongPlayer songPlayer) {
		for (Radio radio : getRadios()) {
			if (songPlayer.equals(radio.getSongPlayer()))
				return radio;
		}
		return null;
	}

	public static void setRadioDefaults(SongPlayer radio) {
		if (radio instanceof RadioSongPlayer)
			((RadioSongPlayer) radio).setStereo(true);

		Fade fadeIn = radio.getFadeIn();
		fadeIn.setType(FadeType.LINEAR);
		fadeIn.setFadeDuration(60);

		Fade fadeOut = radio.getFadeOut();
		fadeOut.setType(FadeType.LINEAR);
		fadeOut.setFadeDuration(60);

		radio.setRepeatMode(RepeatMode.ALL);
		radio.setCategory(SoundCategory.RECORDS);
		radio.setPlaying(true);
	}

	public static void addPlayer(HasPlayer player, Radio radio) {
		if (!RadioUtils.isListening(player.getPlayer(), radio)) {
			RadioUserService userService = new RadioUserService();
			RadioUser user = userService.get(player.getPlayer());

			if (radio.getType().equals(RadioType.SERVER)) {
				user.setServerRadioId(radio.getId());
				userService.save(user);
			}

			SongPlayer songPlayer = radio.getSongPlayer();
			songPlayer.addPlayer(player.getPlayer().getUniqueId());
			if (songPlayer instanceof RadioSongPlayer)
				actionBar(player, songPlayer.getSong());
			else if (songPlayer instanceof PositionSongPlayer) {
				if (RadioUtils.isInRangeOfRadiusRadio(player, radio))
					actionBar(player, songPlayer.getSong());
			}
		}
	}

	public static void removePlayer(HasPlayer player, Radio radio) {
		if (radio == null)
			return;

		if (RadioUtils.isListening(player.getPlayer(), radio)) {
			RadioUserService userService = new RadioUserService();
			RadioUser user = userService.get(player.getPlayer());

			if (radio.getType().equals(RadioType.SERVER)) {
				user.setServerRadioId(null);
				userService.save(user);
			}

			SongPlayer songPlayer = radio.getSongPlayer();
			songPlayer.removePlayer(player.getPlayer());
			if (radio.getSongPlayer() instanceof RadioSongPlayer)
				ActionBarUtils.sendActionBar(player, "&c&lYou have left the server radio");
		}
	}

	public static void removeRadio(Radio radio) {
		if (radio == null)
			return;

		RadioFeature.removeSongPlayer(radio.getSongPlayer());

		RadioConfigService configService = new RadioConfigService();
		RadioConfig config = configService.get0();
		config.getRadios().remove(radio);
		configService.save(config);
	}

	public static boolean isInRangeOfRadiusRadio(HasPlayer player) {
		return getRadiusRadio(player) != null;
	}

	public static Radio getRadiusRadio(HasPlayer player) {
		for (Radio radio : getRadios()) {
			if (isInRangeOfRadiusRadio(player, radio))
				return radio;
		}
		return null;
	}

	public static boolean isInRangeOfRadiusRadio(HasPlayer player, Radio radio) {
		SongPlayer songPlayer = radio.getSongPlayer();
		if (songPlayer == null) return false;
		if (!(songPlayer instanceof PositionSongPlayer positionSongPlayer)) return false;

		if (positionSongPlayer.getTargetLocation() == null) return false;
		if (positionSongPlayer.getTargetLocation().getWorld() == null) return false;
		return positionSongPlayer.getTargetLocation().getWorld().equals(player.getPlayer().getWorld()) && positionSongPlayer.isInRange(player.getPlayer());
	}

	public static Playlist shufflePlaylist(Playlist playlist) {
		List<Song> songList = playlist.getSongList();
		Collections.shuffle(songList);
		return new Playlist(songList.toArray(Song[]::new));
	}

	public static List<String> getPlaylistHover(Radio radio) {
		AtomicInteger ndx = new AtomicInteger(1);
		List<String> songList;
		if (radio.getSongPlayer() == null)
			songList = radio.getSongs().stream().toList();
		else
			songList = radio.getSongPlayer().getPlaylist().getSongList().stream().map(Song::getTitle).toList();

		return songList.stream().map(song -> "&3" + ndx.getAndIncrement() + " &e" + song).collect(Collectors.toList());
	}
}
