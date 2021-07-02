package me.pugabyte.nexus.models.bearfair21;

import dev.morphia.annotations.Converters;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import eden.mongodb.serializers.UUIDConverter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.pugabyte.nexus.framework.persistence.serializer.mongodb.ItemStackConverter;
import me.pugabyte.nexus.framework.persistence.serializer.mongodb.LocationConverter;
import me.pugabyte.nexus.models.PlayerOwnedObject;
import me.pugabyte.nexus.utils.LocationUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Rotation;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static eden.utils.StringUtils.isNullOrEmpty;

@Data
@Entity("bearfair21_clientside_content")
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
@Converters({UUIDConverter.class, LocationConverter.class, ItemStackConverter.class})
public class ClientsideContent implements PlayerOwnedObject {
	@Id
	@NonNull
	private UUID uuid;
	//
	private List<Content> contentList = new ArrayList<>();

	@Nullable
	public Content from(Location location) {
		// Try exact locations
		for (Content content : this.contentList) {
			if (LocationUtils.locationsEqual(location, content.getLocation()))
				return content;
		}

		// Try block locations
		for (Content content : this.contentList) {
			if (LocationUtils.blockLocationsEqual(location, content.getLocation()))
				return content;
		}

		return null;
	}

	@Data
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Content {
		private Location location;
		private ContentCategory category;
		//
		private String schematic;
		//
		private Material material;
		private ItemStack itemStack;
		private BlockFace blockFace;
		// item frame specific
		private Integer rotation;

		public void setRotation(Rotation rotation) {
			this.rotation = rotation.ordinal();
		}

		public boolean isBlock() {
			return !isSchematic() && !isItemFrame();
		}

		public boolean isSchematic() {
			return !isNullOrEmpty(schematic);
		}

		public boolean isItemFrame() {
			return Material.ITEM_FRAME.equals(material);
		}

		public enum ContentCategory {
			// MAIN
			FOOD,
			BALLOON,
			FESTOON,
			BANNER,
			// PUGMAS
			PRESENT,
			// SIDE
			SAWMILL,
			// MGN
			CABLE,
			GRAVWELL,
			SPEAKER,
			SPEAKER_PART_SUBWOOFER,
			SPEAKER_PART_TANGLED_WIRE,
			SPEAKER_PART_SPEAKER_HEAD,
			SPEAKER_PART_AUX_PORT,
			// SDU
			SERPENT,
			SDU_BOOK,
		}
	}


}
