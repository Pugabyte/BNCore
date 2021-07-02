package me.pugabyte.nexus.features.minigames.mechanics;

import com.sk89q.worldedit.regions.Region;
import me.pugabyte.nexus.features.minigames.managers.PlayerManager;
import me.pugabyte.nexus.features.minigames.models.Minigamer;
import me.pugabyte.nexus.features.minigames.models.annotations.Regenerating;
import me.pugabyte.nexus.features.minigames.models.events.matches.minigamers.MinigamerDeathEvent;
import me.pugabyte.nexus.features.minigames.models.mechanics.multiplayer.teams.TeamMechanic;
import me.pugabyte.nexus.utils.ColorType;
import me.pugabyte.nexus.utils.MaterialTag;
import me.pugabyte.nexus.utils.WorldGuardUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@Regenerating("regen")
public final class Paintball extends TeamMechanic {

	@Override
	public @NotNull String getName() {
		return "Paintball";
	}

	@Override
	public @NotNull String getDescription() {
		return "Defeat the enemy team with your one-hit-kill, rapid-fire paintballs";
	}

	@Override
	public @NotNull ItemStack getMenuItem() {
		return new ItemStack(Material.SNOWBALL);
	}

	@Override
	public void onDeath(@NotNull MinigamerDeathEvent event) {
		if (event.getAttacker() == null) return;
		event.getAttacker().scored();
		event.getAttacker().getMatch().scored(event.getAttacker().getTeam());

		if (event.getAttacker().getTeam().getScore(event.getMatch()) == event.getMatch().getArena().getCalculatedWinningScore(event.getMatch()) - 1)
			event.getMatch().broadcast(event.getAttacker().getColoredName() + " &3took the final kill from " + event.getMinigamer().getColoredName());

		super.onDeath(event);
	}

	@EventHandler
	public void onSnowballHit(ProjectileHitEvent event) {
		if (event.getHitEntity() == null) return;
		if (!(event.getEntity() instanceof Snowball)) return;
		if (!(event.getHitEntity() instanceof Player)) return;
		if (!(event.getEntity().getShooter() instanceof Player)) return;
		Minigamer victim = PlayerManager.get(event.getHitEntity());
		Minigamer attacker = PlayerManager.get((Player) event.getEntity().getShooter());
		if (victim.equals(attacker)) return;
		if (victim.isPlaying(this) && attacker.isPlaying(this) && !victim.getTeam().equals(attacker.getTeam()))
			kill(victim, attacker);
	}

	@EventHandler
	public void onSnowBallHitBlock(ProjectileHitEvent event) {
		if (!(event.getEntity() instanceof Snowball)) return;
		if (!(event.getEntity().getShooter() instanceof Player)) return;
		Minigamer minigamer = PlayerManager.get((Player) event.getEntity().getShooter());
		if (!minigamer.isPlaying(this)) return;
		Block hitBlock = event.getHitBlock();
		if (hitBlock == null) return;
		Region region = minigamer.getMatch().getArena().getRegion("regen");
		if (region == null) return;
		if (!region.contains(WorldGuardUtils.toBlockVector3(hitBlock.getLocation()))) return;
		for (BlockFace face : BlockFace.values()) {
			Block relative = hitBlock.getRelative(face);
			if (!region.contains(WorldGuardUtils.toBlockVector3(relative.getLocation()))) continue;
			changeBlockColor(minigamer, relative);
		}
	}

	public void changeBlockColor(Minigamer minigamer, Block block) {
		ColorType colorType = ColorType.of(minigamer.getTeam().getChatColor());
		if (colorType == null) return;
		Material type = block.getType();
		Material replacement = null;
		if (MaterialTag.COLORABLE.isTagged(type))
			replacement = colorType.switchColor(type);
		else
			switch (type) {
				case SAND, RED_SAND -> replacement = ColorType.switchColor(Material.WHITE_CONCRETE_POWDER, colorType.getSimilarDyeColor());
				case QUARTZ_BLOCK, SNOW_BLOCK, SANDSTONE -> replacement = ColorType.switchColor(Material.WHITE_CONCRETE, colorType.getSimilarDyeColor());
				case TERRACOTTA, PACKED_ICE, ICE -> replacement = ColorType.switchColor(Material.WHITE_TERRACOTTA, colorType.getSimilarDyeColor());
			}

		if (replacement != null)
			block.setType(replacement);
	}

	// TODO:
	// Guns
	// Different objects to throw (enderpearl/snowball/armour stand with colored blocks)

}
