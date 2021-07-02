package me.pugabyte.nexus.features.minigames.mechanics;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import eden.utils.TimeUtils.Time;
import me.pugabyte.nexus.features.minigames.Minigames;
import me.pugabyte.nexus.features.minigames.managers.PlayerManager;
import me.pugabyte.nexus.features.minigames.models.Match;
import me.pugabyte.nexus.features.minigames.models.Minigamer;
import me.pugabyte.nexus.features.minigames.models.annotations.Regenerating;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchEndEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchStartEvent;
import me.pugabyte.nexus.features.minigames.models.matchdata.HoliSpleggMatchData;
import me.pugabyte.nexus.features.minigames.models.mechanics.multiplayer.teamless.TeamlessMechanic;
import me.pugabyte.nexus.utils.ItemBuilder;
import me.pugabyte.nexus.utils.MaterialTag;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Egg;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.BlockIterator;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@Regenerating("floor")
public final class HoliSplegg extends TeamlessMechanic {

	@Override
	public @NotNull String getName() {
		return "Holi Splegg";
	}

	@Override
	public @NotNull String getDescription() {
		return "Shoot blocks with eggs to break them and extinguish the armor stand";
	}

	@Override
	public @NotNull ItemStack getMenuItem() {
		return new ItemStack(Material.FLINT_AND_STEEL);
	}

	@Override
	public void onStart(@NotNull MatchStartEvent event) {
		super.onStart(event);
		HoliSpleggMatchData matchData = event.getMatch().getMatchData();
		matchData.setArmorStand(summonArmorStand(event.getMatch()));

		event.getMatch().getTasks().repeat(Time.SECOND, Time.SECOND, () -> {
			matchData.setTime(matchData.getTime() + 1);
			if (matchData.getArmorStand().isInWater())
				event.getMatch().end();
		});
	}

	@Override
	public @NotNull Map<String, Integer> getScoreboardLines(@NotNull Match match) {
		HoliSpleggMatchData matchData = match.getMatchData();
		Map<String, Integer> lines = new HashMap<>();
		lines.put("Time", matchData.getTime());
		return lines;
	}

	@Override
	public void onEnd(@NotNull MatchEndEvent event) {
		HoliSpleggMatchData matchData = event.getMatch().getMatchData();
		for (Minigamer minigamer : event.getMatch().getMinigamers()) {
			minigamer.setScore(matchData.getTime());
		}
		super.onEnd(event);
		if (matchData.getArmorStand() != null)
			matchData.getArmorStand().remove();
	}

	private ArmorStand summonArmorStand(Match match) {
		ArmorStand armorStand = match.spawn(new Location(Minigames.getWorld(), 2548, 29, 710), ArmorStand.class);
		armorStand.setInvulnerable(true);
		armorStand.setFireTicks(9999999);
		armorStand.setCustomNameVisible(true);
		armorStand.setCustomName("Holika");
		armorStand.setArms(true);

		ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
		LeatherArmorMeta chestplateMeta = (LeatherArmorMeta) chestplate.getItemMeta();
		chestplateMeta.setColor(Color.YELLOW);
		chestplate.setItemMeta(chestplateMeta);
		armorStand.setChestplate(chestplate);

		ItemStack pants = new ItemStack(Material.LEATHER_LEGGINGS);
		LeatherArmorMeta pantsMeta = (LeatherArmorMeta) pants.getItemMeta();
		chestplateMeta.setColor(Color.RED);
		pants.setItemMeta(pantsMeta);
		armorStand.setLeggings(pants);

		ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
		LeatherArmorMeta bootsMeta = (LeatherArmorMeta) boots.getItemMeta();
		bootsMeta.setColor(Color.RED);
		boots.setItemMeta(bootsMeta);
		armorStand.setBoots(boots);
		armorStand.setHelmet(new ItemBuilder(Material.PLAYER_HEAD).skullOwner("9da0817559824f2aa231209164201e0d").build());
		armorStand.setItemInHand(new ItemStack(Material.BOOK));

		return armorStand;
	}

	@Override
	public void onPlayerInteract(Minigamer minigamer, PlayerInteractEvent event) {
		super.onPlayerInteract(minigamer, event);

		if (event.getHand() != EquipmentSlot.HAND) return;
		if (!event.getAction().name().contains("RIGHT_CLICK")) return;

		Material hand = minigamer.getPlayer().getInventory().getItemInMainHand().getType();
		if (MaterialTag.SHOVELS.isTagged(hand))
			throwEgg(minigamer);
	}

	private void throwEgg(Minigamer minigamer) {
		Location location = minigamer.getPlayer().getLocation().add(0, 1.5, 0);
		location.add(minigamer.getPlayer().getLocation().getDirection());
		Egg egg = (Egg) minigamer.getPlayer().getWorld().spawnEntity(location, EntityType.EGG);
		egg.setVelocity(location.getDirection().multiply(1.75));
		egg.setShooter(minigamer.getPlayer());
		minigamer.getPlayer().playSound(minigamer.getPlayer().getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.5F, 2F);
	}

	@EventHandler
	public void onProjectileHit(ProjectileHitEvent event) {
		Projectile projectile = event.getEntity();
		if (!(projectile instanceof Egg)) return;

		ProjectileSource source = projectile.getShooter();
		if (!(source instanceof Player)) return;

		Minigamer minigamer = PlayerManager.get((Player) source);
		if (!minigamer.isPlaying(this)) return;

		projectile.remove();
		BlockIterator blockIter = new BlockIterator(projectile.getWorld(), projectile.getLocation().toVector(), projectile.getVelocity().normalize(), 0, 4);
		Block blockHit = null;

		while (blockIter.hasNext()) {
			blockHit = blockIter.next();
			if (blockHit.getType() != Material.AIR) break;
		}

		if (blockHit == null) return;

		breakBlock(minigamer.getMatch(), blockHit.getLocation());
	}

	public boolean breakBlock(Match match, Location location) {
		for (ProtectedRegion region : match.getWGUtils().getRegionsAt(location.clone().add(0, .1, 0))) {
			if (!match.getArena().ownsRegion(region.getId(), "floor")) continue;

			Material type = location.getBlock().getType();
			if (!match.getArena().canUseBlock(type))
				return false;

			playBlockBreakSound(location);
			location.getBlock().setType(Material.AIR);

			return true;
		}
		return false;
	}

	public void playBlockBreakSound(Location location) {
		location.getWorld().playSound(location, Sound.ENTITY_CHICKEN_EGG, 1.0F, 0.7F);
	}


}
