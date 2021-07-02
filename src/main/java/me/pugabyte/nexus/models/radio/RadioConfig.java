package me.pugabyte.nexus.models.radio;

import com.xxmicloxx.NoteBlockAPI.model.Playlist;
import com.xxmicloxx.NoteBlockAPI.model.Song;
import com.xxmicloxx.NoteBlockAPI.songplayer.SongPlayer;
import com.xxmicloxx.NoteBlockAPI.utils.NBSDecoder;
import dev.morphia.annotations.Converters;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import eden.mongodb.serializers.UUIDConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.radio.RadioFeature;
import me.pugabyte.nexus.framework.exceptions.postconfigured.InvalidInputException;
import me.pugabyte.nexus.framework.persistence.serializer.mongodb.LocationConverter;
import me.pugabyte.nexus.models.PlayerOwnedObject;
import org.bukkit.Location;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@Entity("radio")
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
@Converters(UUIDConverter.class)
public class RadioConfig implements PlayerOwnedObject {
	@Id
	@NonNull
	private UUID uuid;

	@Embedded
	private Set<Radio> radios = new HashSet<>();

	public Radio getById(String id) {
		return radios.stream().filter(radio -> radio.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
	}

	public void add(Radio radio) {
		if (getById(radio.getId()) != null)
			throw new InvalidInputException("Radio &e" + radio.getId() + " &calready exists");

		radios.add(radio);
	}

	public void addSong(Radio radio, RadioSong song) {
		Radio _radio = getById(radio.getId());
		Set<String> songs = _radio.getSongs();
		songs.add(song.getName());
		_radio.setSongs(songs);
	}

	public void removeSong(Radio radio, RadioSong song) {
		Radio _radio = getById(radio.getId());
		Set<String> songs = _radio.getSongs();
		songs.remove(song.getName());
		_radio.setSongs(songs);
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@Converters({UUIDConverter.class, LocationConverter.class})
	public static class Radio {
		private String id;
		private RadioType type;
		private boolean enabled = false;
		private boolean particles = false;
		private Location location;
		private int radius;
		@Embedded
		private Set<String> songs = new HashSet<>();

		private transient SongPlayer songPlayer;

		public Location getLocation() {
			if (this.location == null)
				return null;
			return this.location.clone();
		}

		public void setEnabled(boolean bool) {
			this.enabled = bool;

			if (bool) {
				RadioFeature.createSongPlayer(this, getPlaylist());
			} else
				getSongPlayer().setPlaying(false);
		}

		public Set<String> getSongs() {
			if (this.songs == null)
				return new HashSet<>();
			return this.songs;
		}

		public Playlist getPlaylist() {
			Set<String> unloadedSongs = new HashSet<>(songs);
			Set<File> loadedSongs = new HashSet<>() {{
				for (String songName : songs)
					for (RadioSong song : RadioFeature.getAllSongs())
						if (song.getName().equals(songName)) {
							add(song.getFile());
							unloadedSongs.remove(songName);
							break;
						}
			}};

			if (!unloadedSongs.isEmpty())
				Nexus.log("[Radio] [" + id + "] " + loadedSongs.size() + " unloaded songs: " + String.join(", ", unloadedSongs));

			ArrayList<File> list = new ArrayList<>(loadedSongs);
			Collections.shuffle(list);
			Set<File> shuffled = new HashSet<>(list);

			List<Song> songList = new ArrayList<>();
			for (File songFile : shuffled) {
				Song song = NBSDecoder.parse(new File(songFile.getPath()));
				songList.add(song);
			}

			if (songList.isEmpty())
				return null;

			return new Playlist(songList.toArray(Song[]::new));
		}

		public void reload() {
			setEnabled(false);
			setEnabled(true);
		}
	}


	public enum RadioType {
		SERVER,
		RADIUS
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class RadioSong {
		private String name;
		private File file;

	}
}
