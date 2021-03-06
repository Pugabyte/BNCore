package me.pugabyte.nexus.models.aeveonproject;

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
import me.pugabyte.nexus.framework.persistence.serializer.mongodb.ColorConverter;
import me.pugabyte.nexus.models.PlayerOwnedObject;
import org.bukkit.Color;

import java.util.UUID;

@Data
@Builder
@Entity("aeveonproject_user")
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
@Converters({UUIDConverter.class, ColorConverter.class})
public class AeveonProjectUser implements PlayerOwnedObject {
	@Id
	@NonNull
	private UUID uuid;
	private Color shipColor;
}
