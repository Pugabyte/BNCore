package me.pugabyte.nexus.features.ambience.effects.particles;

import eden.utils.TimeUtils.Time;
import lombok.NoArgsConstructor;
import me.pugabyte.nexus.features.ambience.Wind;
import me.pugabyte.nexus.features.ambience.effects.particles.common.ParticleEffect;
import me.pugabyte.nexus.features.ambience.effects.particles.common.ParticleEffectType;
import me.pugabyte.nexus.features.particles.effects.DotEffect;
import me.pugabyte.nexus.models.ambience.AmbienceUser;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@NoArgsConstructor
public class DustWind extends ParticleEffect {
	private Material material;
	private double x;
	private double y;
	private double z;

	private static final int LIFE = Time.SECOND.x(3);

	public DustWind(AmbienceUser user, Block block, double chance) {
		super(user, ParticleEffectType.DUST_WIND, Particle.ITEM_CRACK, LIFE, chance);

		this.material = block.getType();
		this.x = block.getX();
		this.y = block.getY();
		this.z = block.getZ();
	}

	@Override
	public void play() {
		Player player = user.getPlayer();
		if (player == null)
			return;

		double xRange = x - 2 + Math.random() * 5;
		double yRange = y + 1 + Math.random() * 2;
		double zRange = z - 2 + Math.random() * 5;

		double scale = 1 + Math.random() * 0.2;
		double xVel = Wind.getX() * scale;
		double yVel = 0;
		double zVel = Wind.getZ() * scale;

		player.spawnParticle(particle, xRange, yRange, zRange, 0, xVel, yVel, zVel, 1, new ItemStack(material));

		if (user.isDebug())
			DotEffect.builder()
				.player(player)
				.location(new Location(player.getWorld(), xRange, yRange, zRange))
				.clientSide(true)
				.color(Color.ORANGE)
				.speed(.1)
				.ticks(Time.SECOND.get())
				.start();
	}

}
