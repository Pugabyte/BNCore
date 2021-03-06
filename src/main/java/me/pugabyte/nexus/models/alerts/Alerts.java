package me.pugabyte.nexus.models.alerts;

import dev.morphia.annotations.Converters;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import eden.mongodb.serializers.UUIDConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.pugabyte.nexus.models.PlayerOwnedObject;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.SoundUtils.Jingle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Data
@Builder
@Entity("alerts")
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
@Converters(UUIDConverter.class)
public class Alerts implements PlayerOwnedObject {
	@Id
	@NonNull
	private UUID uuid;
	private List<Highlight> highlights = new ArrayList<>();
	private boolean muted;

	public boolean add(String highlight) {
		return add(highlight, true);
	}

	public boolean add(String highlight, boolean partialMatching) {
		if (!has(highlight)) {
			highlights.add(new Highlight(highlight, partialMatching));
			sort();
			return true;
		}
		return false;
	}

	public boolean delete(String highlight) {
		if (has(highlight)) {
			highlights.remove(get(highlight).get());
			sort();
			return true;
		}
		return false;
	}

	private void sort() {
		Collections.sort(highlights);
	}

	public boolean has(String highlight) {
		return get(highlight).isPresent();
	}

	public Optional<Highlight> get(String highlight) {
		return highlights.stream().filter(_highlight -> _highlight.getHighlight().equalsIgnoreCase(highlight)).findFirst();
	}

	public void clear() {
		highlights.clear();
	}

	public boolean isMuted() {
		return muted;
	}

	public void setMuted(boolean muted) {
		this.muted = muted;
	}

	public void playSound() {
		if (!isMuted())
			Jingle.PING.play(PlayerUtils.getPlayer(uuid).getPlayer());
	}

	public void tryAlerts(String message) {
		for (Highlight highlight : getHighlights())
			if (highlight.test(message)) {
				playSound();
				break;
			}
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Highlight implements Comparable<Highlight> {
		@NonNull
		private String highlight;
		@NonNull
		private boolean partialMatching;

		@Override
		public int compareTo(Highlight other) {
			return highlight.compareTo(other.getHighlight());
		}

		public boolean test(String message) {
			if (partialMatching) {
				return message.toLowerCase().contains(highlight.toLowerCase());
			} else {
				String _message = message;
				// Allow partial matching to work with special chars (ie quotes)
				if (highlight.replaceAll("[a-zA-Z0-9 ]+", "").length() == 0)
					_message = message.replaceAll("[^a-zA-Z0-9 ]+", " ");

				return (" " + _message + " ").toLowerCase().contains(" " + highlight.toLowerCase() + " ");
			}
		}
	}

}
