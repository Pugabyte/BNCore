package me.pugabyte.bearnation.minigames.features.models.arenas;

import com.sk89q.worldedit.regions.Region;
import lombok.Data;
import me.pugabyte.bearnation.minigames.features.models.Arena;
import org.bukkit.configuration.serialization.SerializableAs;

import java.util.Map;

@Data
@SerializableAs("PixelPaintersArena")
public class PixelPaintersArena extends Arena {
	private Region designRegion = getRegion("designs");
	private Region nextDesignRegion = getRegion("nextdesign");
	private Region lobbyDesignRegion = getRegion("lobbynextdesign");
	private Region logoRegion = getRegion("logo");
	private Region lobbyAnimationRegion = getRegion("lobbyanimation");

	public PixelPaintersArena(Map<String, Object> map) {
		super(map);
	}

	@Override
	public String getRegionBaseName() {
		return "pixelpainters";
	}

}
