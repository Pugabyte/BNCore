package me.pugabyte.nexus.models.powertool;

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
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.models.PlayerOwnedObject;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static me.pugabyte.nexus.utils.PlayerUtils.runCommand;

@Data
@Builder
@Entity("powertool")
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
@Converters(UUIDConverter.class)
public class PowertoolUser implements PlayerOwnedObject {
	@Id
	@NonNull
	private UUID uuid;
	private boolean enabled = true;
	private Map<Material, String> powertools = new HashMap<>();

	public void use(Material material) {
		if (getOfflinePlayer().isOnline() && getOnlinePlayer() != null) {
			String command = powertools.get(material);
			runCommand(getOnlinePlayer(), command);
			Nexus.log("[PT] " + getOnlinePlayer().getName() + " issued server command: /" + command);
		}
	}

}
