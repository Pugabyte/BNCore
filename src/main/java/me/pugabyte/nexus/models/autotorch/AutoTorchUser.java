package me.pugabyte.nexus.models.autotorch;

import dev.morphia.annotations.Converters;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import lombok.*;
import me.pugabyte.nexus.framework.persistence.serializer.mongodb.UUIDConverter;
import me.pugabyte.nexus.models.PlayerOwnedObject;
import org.bukkit.block.Block;

import java.util.UUID;

@Data
@Entity("autotorch")
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
@Converters(UUIDConverter.class)
public class AutoTorchUser extends PlayerOwnedObject {
    @Id
    @NonNull
    private UUID uuid;
    private int lightLevel = 7;
    private boolean enabled = true;

    /**
     * Whether or not auto torches should apply at the supplied light level
     * @param lightLevel int from 0 to 15
     * @return whether or not to use auto torches
     */
    public boolean applies(int lightLevel) {
        return enabled && lightLevel <= this.lightLevel;
    }

    public boolean applies(Block block) {
        return applies(block.getLightLevel()) && block.isReplaceable() && block.getRelative(0, -1, 0).isBuildable()
    }
}
