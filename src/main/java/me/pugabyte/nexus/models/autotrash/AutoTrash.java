package me.pugabyte.nexus.models.autotrash;

import dev.morphia.annotations.Converters;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.pugabyte.nexus.framework.persistence.serializer.mongodb.UUIDConverter;
import me.pugabyte.nexus.models.PlayerOwnedObject;
import org.bukkit.Material;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@Entity("autotrash")
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
@Converters(UUIDConverter.class)
public class AutoTrash extends PlayerOwnedObject {
	@Id
	@NonNull
	private UUID uuid;
	private boolean enabled = true;
	private Behavior behavior = Behavior.TRASH;
	private Set<Material> materials = new HashSet<>();

/*
	@Embedded
	private List<AutoTrashItem> items = new ArrayList<>();

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Converters(UUIDConverter.class)
	public static class AutoTrashItem {
		@NonNull
		private UUID uuid;
		private Material material;
		private Behavior behavior = Behavior.TRASH;
		private MatchMode matchMode = MatchMode.MATERIAL;

		public enum MatchMode {
			MATERIAL,
			META
		}
	}
*/

	public enum Behavior {
		NO_PICKUP,
		TRASH
	}

}