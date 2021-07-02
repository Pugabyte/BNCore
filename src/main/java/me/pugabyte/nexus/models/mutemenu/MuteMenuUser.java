package me.pugabyte.nexus.models.mutemenu;

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
import me.lexikiq.HasUniqueId;
import me.pugabyte.nexus.features.chat.Chat.StaticChannel;
import me.pugabyte.nexus.features.commands.MuteMenuCommand.MuteMenuProvider.MuteMenuItem;
import me.pugabyte.nexus.models.PlayerOwnedObject;
import me.pugabyte.nexus.models.chat.ChatterService;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@Entity("mute_menu_user")
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
@Converters(UUIDConverter.class)
public class MuteMenuUser implements PlayerOwnedObject {
	@Id
	@NonNull
	private UUID uuid;
	private Set<MuteMenuItem> muted = new HashSet<>();
	private HashMap<MuteMenuItem, Integer> volumes = new HashMap<>();

	public boolean hasMuted(MuteMenuItem item) {
		if (volumes.containsKey(item))
			return getVolume(item) == 0.0;

		if (item.name().startsWith("CHANNEL_"))
			return !new ChatterService().get(uuid).hasJoined(StaticChannel.valueOf(item.name().replace("CHANNEL_", "")).getChannel());
		else
			return muted.contains(item);
	}

	public void setVolume(MuteMenuItem item, int volume) {
		volumes.put(item, volume);
	}

	public int getVolume(MuteMenuItem item) {
		return volumes.getOrDefault(item, item.getDefaultVolume());
	}

	public void unMute(MuteMenuItem item) {
		muted.remove(item);
	}

	public void mute(MuteMenuItem item) {
		muted.add(item);
	}

	public static boolean hasMuted(HasUniqueId player, MuteMenuItem item) {
		if (item == null) return false;
		MuteMenuService service = new MuteMenuService();
		MuteMenuUser user = service.get(player);
		return user.hasMuted(item);
	}

	public static Integer getVolume(HasUniqueId player, MuteMenuItem item) {
		if (item == null) return null;
		MuteMenuService service = new MuteMenuService();
		MuteMenuUser user = service.get(player);
		return user.getVolume(item);
	}
}
