package me.pugabyte.bncore.features.minigames.mechanics.custom;

import com.mewin.worldguardregionapi.events.RegionEnteredEvent;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.pugabyte.bncore.features.minigames.Minigames;
import me.pugabyte.bncore.features.minigames.managers.PlayerManager;
import me.pugabyte.bncore.features.minigames.mechanics.Murder;
import me.pugabyte.bncore.features.minigames.models.Match;
import me.pugabyte.bncore.features.minigames.models.Minigamer;
import me.pugabyte.bncore.features.minigames.models.events.matches.MatchEndEvent;
import me.pugabyte.bncore.features.minigames.models.events.matches.MatchQuitEvent;
import me.pugabyte.bncore.features.minigames.models.events.matches.MatchStartEvent;
import me.pugabyte.bncore.features.minigames.models.events.matches.minigamers.MinigamerDeathEvent;
import me.pugabyte.bncore.utils.StringUtils;
import me.pugabyte.bncore.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class RavensNestEstate extends Murder {
	// Sounds & Their Locations
	private Location musicLocation = new Location(Minigames.getGameworld(), 3075, 60, 1282);
	private Location fireplaceTrigger1 = new Location(Minigames.getGameworld(), 3087, 25, 1269);
	private Location fireplaceTrigger2 = new Location(Minigames.getGameworld(), 3090, 25, 1242);
	private Location freezerSound = new Location(Minigames.getGameworld(), 3086, 25, 1280);
	private Sound[] sounds = {Sound.AMBIENT_CAVE, Sound.ENTITY_ELDER_GUARDIAN_DEATH, Sound.ENTITY_VEX_AMBIENT,
			Sound.ENTITY_WITCH_AMBIENT, Sound.ENTITY_ILLUSION_ILLAGER_PREPARE_MIRROR, Sound.ENTITY_ILLUSION_ILLAGER_PREPARE_BLINDNESS,
			Sound.ENTITY_ILLUSION_ILLAGER_CAST_SPELL, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, Sound.ENTITY_SHULKER_AMBIENT};

	// Door Status, true = open, false = closed
	private boolean statusFireplace = false;
	private boolean statusBasement = false;
	private boolean statusCloset = false;
	private boolean statusMain = false;
	private boolean statusMaster = false;
	private boolean statusStudy = false;
	private boolean statusSmall_S = false;
	private boolean statusSmall_E = false;
	private boolean statusSmall_W = false;

	// Schematic Files
	private String schemFolder = "Animations/RavensNestEstate";
	private String schemFireplace = schemFolder + "/Fireplace/Fireplace_";
	private String schemBasement = schemFolder + "/Basement/Basement_";
	private String schemCloset = schemFolder + "/Closet/Closet_";
	private String schemMain = schemFolder + "/Main/Main_";
	private String schemMaster = schemFolder + "/Master/Master_";
	private String schemSmall = schemFolder + "/Small/Small_";
	private String schemStudy = schemFolder + "/Study/Study_";

	// Schematic Frames
	private int framesFireplace = 8;
	private int framesBasement = 3;
	private int framesCloset = 4;
	private int framesMain = 5;
	private int framesMaster = 5;
	private int framesSmall = 7;
	private int framesStudy = 6;

	//Door Schem Paste Locations
	private Location doorSmall_S = new Location(Minigames.getGameworld(), 3054, 24, 1272);
	private Location doorSmall_E = new Location(Minigames.getGameworld(), 3046, 32, 1297);
	private Location doorSmall_W = new Location(Minigames.getGameworld(), 3095, 32, 1293);
	private Location doorBasement = new Location(Minigames.getGameworld(), 3067, 24, 1270);
	private Location doorCloset = new Location(Minigames.getGameworld(), 3046, 32, 1275);
	private Location doorMain = new Location(Minigames.getGameworld(), 3069, 24, 1244);
	private Location doorMaster = new Location(Minigames.getGameworld(), 3074, 30, 1272);
	private Location doorStudy = new Location(Minigames.getGameworld(), 3079, 24, 1254);
	private Location doorFireplace = new Location(Minigames.getGameworld(), 3092, 24, 1264);

	@Override
	public void onStart(MatchStartEvent event) {
		super.onStart(event);
		Match match = event.getMatch();
		soundTasks(match);
	}

	@Override
	public void onQuit(MatchQuitEvent event) {
		stopSounds(event.getMinigamer());
		super.onQuit(event);
	}

	@Override
	public void onEnd(MatchEndEvent event) {
		resetMap(event.getMatch());
		super.onEnd(event);
	}

	@Override
	public void onDeath(MinigamerDeathEvent event) {
		Match match = event.getMatch();
		Set<ProtectedRegion> regions = WGUtils.getRegionsAt(event.getMinigamer().getPlayer().getLocation());
		for (ProtectedRegion region : regions) {
			if (region.getId().equalsIgnoreCase(match.getArena().getProtectedRegion("deathzone").getId())) {
				event.setDeathMessage(event.getMinigamer().getName() + " drowned in blood.");
				break;
			}
		}
		super.onDeath(event);
	}

	@EventHandler
	public void onEnterRegion(RegionEnteredEvent event) {
		Player player = event.getPlayer();
		Minigamer minigamer = PlayerManager.get(player);
		if (!minigamer.isPlaying(this)) return;

		Match match = minigamer.getMatch();
		if (!event.getRegion().getId().equalsIgnoreCase(match.getArena().getProtectedRegion("deathzone").getId()))
			return;
		kill(minigamer);

	}

	private void resetMap(Match match) {
		statusFireplace = false;
		String fireplaceFile = schemFireplace.replace("[1-8]", "1");
		WEUtils.paste(fireplaceFile, doorFireplace);

		Region region = match.getArena().getRegion("torches");
		List<Block> blocks = WEUtils.getBlocks(region);
		for (Block block : blocks) {
			if (block.getType().equals(Material.REDSTONE_TORCH_ON) && block.getLocation().getY() <= 17) {
				byte data = block.getData();
				block.setType(Material.AIR);
				match.getTasks().wait(1, () -> {
					block.setType(Material.REDSTONE_COMPARATOR_ON);
					block.setData(data);
				});
			}

		}
	}

	private void soundTasks(Match match) {
		int delay = 2 * 20;
		match.getTasks().repeat(delay, 350 * 20, () ->
				match.getMinigamers().stream().map(Minigamer::getPlayer).forEach(player -> {
					player.stopSound(Sound.RECORD_13);
					player.playSound(musicLocation, Sound.RECORD_13, SoundCategory.BLOCKS, 6F, 0.1F);
				}));

		match.getTasks().repeat(delay, 30 * 20, () -> {
			if (Utils.chanceOf(50)) {
				Sound sound = Utils.getRandomElement(Arrays.asList(sounds));
				match.getMinigamers().stream().map(Minigamer::getPlayer).forEach(player ->
						player.playSound(player.getLocation(), sound, 10F, 0.1F));
			}
		});

		match.getTasks().repeat(delay, 25 * 20, () -> {
			if (Utils.chanceOf(25))
				match.getMinigamers().stream().map(Minigamer::getPlayer).forEach(player ->
						player.playSound(musicLocation, Sound.ENTITY_LIGHTNING_THUNDER, 10F, 0.1F));
			else if (Utils.chanceOf(25))
				musicLocation.getWorld().strikeLightning(musicLocation);
		});

		match.getTasks().repeat(delay, 3 * 20, () -> {
			World world = freezerSound.getWorld();
			world.playSound(freezerSound, Sound.ENTITY_MINECART_RIDING, 0.7F, 0.1F);
			world.spawnParticle(Particle.FALLING_DUST, freezerSound, 100, 2.5, 1.5, 2.5, 0.000001, 80);
		});

		match.getTasks().repeat(delay, 5 * 20, () -> {
			if (!statusFireplace) {
				World world = fireplaceTrigger1.getWorld();
				world.spawnParticle(Particle.VILLAGER_HAPPY, fireplaceTrigger1, 50, 0.5, 0.5, 0.5, 0.1);
				world.spawnParticle(Particle.VILLAGER_HAPPY, fireplaceTrigger2, 50, 0.5, 0.5, 0.5, 0.1);
			}
		});
	}

	private void stopSounds(Minigamer minigamer) {
		Player player = minigamer.getPlayer();
		player.stopSound(Sound.RECORD_13);
		for (Sound sound : sounds) {
			player.stopSound(sound);
		}
	}

	@Override
	public void onPlayerInteract(Minigamer minigamer, PlayerInteractEvent event) {
		super.onPlayerInteract(minigamer, event);

		if (event.isCancelled()) return;
		if (event.getHand() != EquipmentSlot.HAND) return;

		Material material = event.getClickedBlock().getType();
		Location loc = event.getClickedBlock().getLocation();
		Match match = minigamer.getMatch();
		switch (material) {
			case RAILS:
				playPiano(loc);
				break;
			case BOOKSHELF:
				openFireplace(loc, match);
				break;
			case REDSTONE_TORCH_OFF:
			case REDSTONE_TORCH_ON:
				toggleTorch(loc, event.getClickedBlock(), match);
				break;
			case WOOD_BUTTON:
				String schematic = findDoor(loc, match);
				if (schematic != null)
					toggleDoor(schematic);
				break;
		}

	}

	private void playPiano(Location location) {
		float ran = (float) Utils.randomDouble(0.0, 2.0);
		location.getWorld().playSound(location, Sound.BLOCK_NOTE_PLING, 0.7F, ran);
	}

	private void openFireplace(Location location, Match match) {
		if (!(location.equals(fireplaceTrigger1) || location.equals(fireplaceTrigger2))) return;
		if (!statusFireplace) return;

		statusFireplace = true;
		Location loc = fireplaceTrigger1;
		World world = loc.getWorld();
		Sound sound = Sound.BLOCK_NOTE_HARP;

		int wait = 0;

		world.playSound(loc, sound, 2F, 1.05F);
		match.getTasks().wait(wait += 4, () -> world.playSound(loc, sound, 2F, 1F));
		match.getTasks().wait(wait += 4, () -> world.playSound(loc, sound, 2F, 0.85F));
		match.getTasks().wait(wait += 4, () -> world.playSound(loc, sound, 2F, 1.6F));
		match.getTasks().wait(wait += 4, () -> world.playSound(loc, sound, 2F, 1.55F));
		match.getTasks().wait(wait += 4, () -> world.playSound(loc, sound, 2F, 1.9F));
		match.getTasks().wait(wait += 4, () -> world.playSound(loc, sound, 2F, 1.125F));
		match.getTasks().wait(wait += 4, () -> {
			world.playSound(loc, sound, 2F, 1.4F);
			toggleDoor(schemFireplace);
			world.playSound(loc, Sound.ENTITY_ENDERDRAGON_GROWL, 2F, 0.1F);
		});
		match.getTasks().wait(wait += 4, () -> {
			world.playSound(loc, Sound.ENTITY_MINECART_RIDING, 2F, 0.5F);
			match.getTasks().wait((long) (2.5 * 20), () ->
					world.playSound(loc, Sound.ENTITY_VEX_AMBIENT, 2F, 0.1F));
		});

	}

	private void toggleTorch(Location location, Block block, Match match) {
		if (location.getY() > 17) return;

		Material material = block.getType();
		byte data = block.getData();
		block.setType(Material.AIR);

		if (material.equals(Material.REDSTONE_TORCH_OFF)) {
			match.getTasks().wait(1, () -> {
				block.setType(Material.REDSTONE_TORCH_ON);
				block.setData(data);
			});
		} else {
			match.getTasks().wait(1, () -> {
				block.setType(Material.REDSTONE_TORCH_OFF);
				block.setData(data);
			});
		}

	}

	private String findDoor(Location location, Match match) {
		Set<ProtectedRegion> regions = WGUtils.getRegionsAt(location);
		String key = match.getArena().getRegionBaseName() + "_door_";
		for (ProtectedRegion region : regions) {
			String regionName = region.getId();
			if (regionName.contains(key))
				return "Animations/RavensNestEstate/" + StringUtils.camelCase(regionName.replaceAll(key, ""));
		}
		return null;
	}

	private void toggleDoor(String schematic) {
		int frames;
		Location pasteLoc;
		boolean status;
		if (schematic.contains(schemFireplace)) {
			frames = framesFireplace;
			pasteLoc = doorFireplace;
			status = statusFireplace;
			statusFireplace = !statusFireplace;

		} else if (schematic.contains(schemBasement)) {
			frames = framesBasement;
			pasteLoc = doorBasement;
			status = statusBasement;
			statusBasement = !statusBasement;
		} else if (schematic.contains(schemCloset)) {
			frames = framesCloset;
			pasteLoc = doorCloset;
			status = statusCloset;
			statusCloset = !statusCloset;
		} else if (schematic.contains(schemMain)) {
			frames = framesMain;
			pasteLoc = doorMain;
			status = statusMain;
			statusMain = !statusMain;
		} else if (schematic.contains(schemMaster)) {
			frames = framesMaster;
			pasteLoc = doorMaster;
			status = statusMaster;
			statusMaster = !statusMaster;
		} else if (schematic.contains(schemStudy)) {
			frames = framesStudy;
			pasteLoc = doorStudy;
			status = statusStudy;
			statusStudy = !statusStudy;
		} else if (schematic.contains(schemSmall)) {
			frames = framesSmall;
			String direction = schematic.replaceAll(schemSmall + "_", "");
			switch (direction) {
				case "S":
					pasteLoc = doorSmall_S;
					status = statusSmall_S;
					statusSmall_S = !statusSmall_S;
					break;
				case "E":
					pasteLoc = doorSmall_E;
					status = statusSmall_E;
					statusSmall_E = !statusSmall_E;
					break;
				case "W":
					pasteLoc = doorSmall_W;
					status = statusSmall_W;
					statusSmall_W = !statusSmall_W;
					break;
				default:
					return;
			}
		} else
			return;

		if (status) {
			for (int frame = 1; frame < frames; frame++) {
				WEUtils.paste(schematic + frame, pasteLoc);
			}
		} else {
			for (int frame = frames; frame < 1; frame--) {
				WEUtils.paste(schematic + frame, pasteLoc);
			}
		}
	}
}

