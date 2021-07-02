package me.pugabyte.nexus.features.events.y2020.pugmas20;

import com.destroystokyo.paper.ParticleBuilder;
import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import eden.utils.TimeUtils.Time;
import lombok.Getter;
import lombok.Setter;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.events.y2020.pugmas20.menu.AdventMenu;
import me.pugabyte.nexus.features.events.y2020.pugmas20.models.AdventChest;
import me.pugabyte.nexus.features.events.y2020.pugmas20.models.Merchants;
import me.pugabyte.nexus.features.events.y2020.pugmas20.models.QuestNPC;
import me.pugabyte.nexus.features.events.y2020.pugmas20.quests.Quests;
import me.pugabyte.nexus.models.cooldown.CooldownService;
import me.pugabyte.nexus.models.eventuser.EventUser;
import me.pugabyte.nexus.models.eventuser.EventUserService;
import me.pugabyte.nexus.models.pugmas20.Pugmas20User;
import me.pugabyte.nexus.models.pugmas20.Pugmas20UserService;
import me.pugabyte.nexus.utils.BlockUtils;
import me.pugabyte.nexus.utils.CitizensUtils;
import me.pugabyte.nexus.utils.ItemBuilder;
import me.pugabyte.nexus.utils.LocationUtils;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.RandomUtils;
import me.pugabyte.nexus.utils.StringUtils;
import me.pugabyte.nexus.utils.Tasks;
import me.pugabyte.nexus.utils.WorldEditUtils;
import me.pugabyte.nexus.utils.WorldGuardUtils;
import net.citizensnpcs.api.event.NPCLeftClickEvent;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.inventivetalent.glow.GlowAPI;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static me.pugabyte.nexus.utils.LocationUtils.getCenteredLocation;

public class Pugmas20 implements Listener {
	@Getter
	public static final String region = "pugmas20";
	public static final String PREFIX = StringUtils.getPrefix("Pugmas 2020");
	// Dates
	public static final LocalDate openingDay = LocalDate.of(2020, 12, 1);
	public static final LocalDate secondChance = LocalDate.of(2020, 12, 25);
	public static final LocalDate closingDay = LocalDate.of(2021, 1, 11);

	public static final List<Hologram> holograms = new ArrayList<>();
	@Getter
	private static final String questLore = "&ePugmas 2020 Quest Item";
	@Getter
	private static final String itemLore = "&ePugmas 2020 Item";
	@Getter
	private static final String adventLore = "&ePugmas 2020 Advent Item";

	@Getter
	private static final Location initialSpawn = location(898.5, 52, 356.5);
	@Getter
	private static final Location subsequentSpawn = location(909.5, 52, 368.5);
	@Getter
	@Setter
	private static boolean treeAnimating = false;

	@Getter
	private static final Map<String, Integer> tokenMaxes = new HashMap<>();

	public Pugmas20() {
		Nexus.registerListener(this);

		AdventMenu.loadHeads();
		new AdventChests();
		new Quests();
		new Train();
//		new Minecarts();
		createNpcHolograms();
		npcParticles();
	}

	public static World getWorld() {
		return Bukkit.getWorld("safepvp");
	}

	public static WorldGuardUtils getWGUtils() {
		return new WorldGuardUtils(getWorld());
	}

	public static WorldEditUtils getWEUtils() {
		return new WorldEditUtils(getWorld());
	}

	public static Location getInitialSpawn() {
		return location(898.5, 52, 356.5);
	}

	public static Location getSubsequentSpawn() {
		return location(909.5, 52, 368.5);
	}

	public void shutdown() {
		deleteNpcHolograms();
	}

	public static void deleteNpcHolograms() {
		holograms.forEach(Hologram::delete);
	}

	public static void createNpcHolograms() {
		deleteNpcHolograms();
		for (QuestNPC questNPC : QuestNPC.values()) {
			NPC npc = CitizensUtils.getNPC(questNPC.getId());
			Hologram hologram = HologramsAPI.createHologram(Nexus.getInstance(), npc.getStoredLocation().clone().add(0, 3.15, 0));
			hologram.appendItemLine(new ItemStack(Material.EMERALD));
			holograms.add(hologram);
		}
	}

	private void npcParticles() {
		Pugmas20UserService service = new Pugmas20UserService();
		Particle particle = Particle.VILLAGER_HAPPY;

		Tasks.repeatAsync(0, Time.SECOND.x(2), () -> {
			for (Player player : getWGUtils().getPlayersInRegion(region)) {
				Pugmas20User user = service.get(player);
				for (Integer npcId : user.getNextStepNPCs()) {
					NPC npc = CitizensUtils.getNPC(npcId);
					if (npc.isSpawned()) {
						Location loc = npc.getEntity().getLocation().add(0, 1, 0);
						new ParticleBuilder(particle)
								.location(loc)
								.offset(.25, .5, .25)
								.count(10)
								.receivers(player)
								.spawn();
					}
				}
			}
		});
	}

	public static void addTokenMax(String id, int amount) {
		tokenMaxes.put(("pugmas20_" + id).toLowerCase(), amount);
	}

	public static int checkDailyTokens(OfflinePlayer player, String id, int amount) {
		EventUserService service = new EventUserService();
		EventUser user = service.get(player);

		return user.getDailyTokensLeft(("pugmas20_" + id).toLowerCase(), amount, tokenMaxes);
	}

	public static void giveDailyTokens(Player player, String id, int amount) {
		EventUserService service = new EventUserService();
		EventUser user = service.get(player);

		user.giveTokens(("pugmas20_" + id).toLowerCase(), amount, tokenMaxes);
		service.save(user);
	}

	public static Location location(double x, double y, double z) {
		return new Location(getWorld(), x, y, z);
	}

	public static Location location(double x, double y, double z, float yaw, float pitch) {
		return new Location(getWorld(), x, y, z, yaw, pitch);
	}

	public static ItemBuilder questItem(Material material) {
		return questItem(new ItemStack(material));
	}

	public static ItemBuilder questItem(ItemStack itemStack) {
		return new ItemBuilder(itemStack).lore(questLore);
	}

	public static ItemBuilder adventItem(Material material) {
		return adventItem(new ItemStack(material));
	}

	public static ItemBuilder adventItem(ItemStack itemStack) {
		return new ItemBuilder(itemStack).lore(adventLore);
	}

	public static ItemBuilder item(Material material) {
		return item(new ItemStack(material));
	}

	public static ItemBuilder item(ItemStack itemStack) {
		return new ItemBuilder(itemStack).lore(itemLore);
	}

	public static boolean isBeforePugmas(LocalDate localDate) {
		return localDate.isBefore(openingDay);
	}

	public static boolean isPastPugmas(LocalDate localDate) {
		return localDate.isAfter(closingDay);
	}

	public static boolean isSecondChance(LocalDate localDate) {
		return ((localDate.isEqual(secondChance) || localDate.isAfter(secondChance))
				&& !isPastPugmas(localDate));
	}

	public static boolean isInPugmasWorld(Player player) {
		return isInPugmasWorld(player);
	}

	public static boolean isInPugmasWorld(Location location) {
		return location.getWorld().equals(getWorld());
	}

	public static boolean isAtPugmas(Player player) {
		return isAtPugmas(player.getLocation());
	}

	public static boolean isAtPugmas(Location location) {
		return isInPugmasWorld(location) && getWGUtils().isInRegion(location, region);
	}

	public static boolean isAtPugmas(Player player, String name) {
		return isAtPugmas(player.getLocation(), name);
	}

	public static boolean isAtPugmas(Location location, String name) {
		return isInPugmasWorld(location) && !getWGUtils().getRegionsLikeAt(getRegion() + "_" + name + "(_[0-9]+)?", location).isEmpty();
	}

	@EventHandler
	public void onNPCClick(NPCRightClickEvent event) {
		Player player = event.getClicker();
		if (!isAtPugmas(player)) return;
		if (!new CooldownService().check(event.getClicker(), "Pugmas20_NPC", Time.SECOND.x(10))) return;

		int id = event.getNPC().getId();
		QuestNPC.startScript(player, id);
		Merchants.openMerchant(player, id);
	}

//	@EventHandler
	public void onTeleport(PlayerTeleportEvent event) {
		Pugmas20UserService service = new Pugmas20UserService();

		if (isAtPugmas(event.getFrom()) && !isAtPugmas(event.getTo())) {
			Pugmas20User user = service.get(event.getPlayer());
			user.storeInventory();
			service.save(user);
		}

		if (isAtPugmas(event.getTo()) && !isAtPugmas(event.getFrom())) {
			Tasks.wait(Time.SECOND, () -> {
				if (isAtPugmas(event.getPlayer())) {
					Pugmas20User user = service.get(event.getPlayer());
					user.applyInventory();
					service.save(user);
				}
			});
		}
	}

	public static void showWaypoint(AdventChest adventChest, Player player) {
		Location chestLoc = adventChest.getLocation();
		Block chest = chestLoc.getBlock();
		if (!BlockUtils.isNullOrAir(chest)) {
			Location blockLoc = getCenteredLocation(chestLoc);
			World blockWorld = blockLoc.getWorld();
			FallingBlock fallingBlock = blockWorld.spawnFallingBlock(blockLoc, Material.RED_CONCRETE.createBlockData());
			fallingBlock.setDropItem(false);
			fallingBlock.setGravity(false);
			fallingBlock.setInvulnerable(true);
			fallingBlock.setVelocity(new Vector(0, 0, 0));

			LocationUtils.lookAt(player, blockLoc);

			Tasks.GlowTask.builder()
					.duration(Time.SECOND.x(10))
					.entity(fallingBlock)
					.color(GlowAPI.Color.RED)
					.viewers(Collections.singletonList(player))
					.onComplete(() -> {
						fallingBlock.remove();
						for (Player _player : PlayerUtils.getOnlinePlayers(blockWorld))
							_player.sendBlockChange(chestLoc, chest.getBlockData());
					})
					.start();
		}
	}

	@EventHandler
	public void onNpcRightClick(NPCRightClickEvent event) {
		Player player = event.getClicker();
		if (!isAtPugmas(player))
			return;

		if (!Arrays.asList("Mrs. Paws", "Santa", "Elf", "Miner Elf").contains(event.getNPC().getName()))
			return;

		PlayerUtils.send(player, QuestNPC.format(event.getNPC().getName(), QuestNPC.getGreeting()));
		Quests.sound_npcAlert(player);
	}

	@EventHandler
	public void onNpcLeftClick(NPCLeftClickEvent event) {
		Player player = event.getClicker();
		if (!isAtPugmas(player))
			return;

		if (!new CooldownService().check(player, "pugmas20-elf-punch", Time.SECOND.x(3)))
			return;

		String message = RandomUtils.randomElement("Ow!", "Stop that!", "Rude!");
		PlayerUtils.send(player, QuestNPC.format(event.getNPC().getName(), message));
	}

}
