package me.pugabyte.nexus.features.events.y2020.pugmas20;

import com.destroystokyo.paper.ParticleBuilder;
import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import lombok.Getter;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.events.y2020.pugmas20.menu.AdventMenu;
import me.pugabyte.nexus.features.events.y2020.pugmas20.models.Merchants;
import me.pugabyte.nexus.features.events.y2020.pugmas20.models.QuestNPC;
import me.pugabyte.nexus.features.events.y2020.pugmas20.quests.Quests;
import me.pugabyte.nexus.models.cooldown.CooldownService;
import me.pugabyte.nexus.models.eventuser.EventUser;
import me.pugabyte.nexus.models.eventuser.EventUserService;
import me.pugabyte.nexus.models.pugmas20.Pugmas20Service;
import me.pugabyte.nexus.models.pugmas20.Pugmas20User;
import me.pugabyte.nexus.utils.CitizensUtils;
import me.pugabyte.nexus.utils.ItemBuilder;
import me.pugabyte.nexus.utils.StringUtils;
import me.pugabyte.nexus.utils.Tasks;
import me.pugabyte.nexus.utils.Time;
import me.pugabyte.nexus.utils.WorldEditUtils;
import me.pugabyte.nexus.utils.WorldGuardUtils;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Pugmas20 implements Listener {
	@Getter
	public static final String region = "pugmas20";
	@Getter
	public static final World world = Bukkit.getWorld("safepvp");
	@Getter
	public static final String PREFIX = StringUtils.getPrefix("Pugmas20");
	public static final WorldGuardUtils WGUtils = new WorldGuardUtils(world);
	public static final WorldEditUtils WEUtils = new WorldEditUtils(world);
	// Dates
	public static final LocalDateTime openingDay = LocalDateTime.of(2020, 12, 1, 0, 0, 0, 0);
	public static final LocalDateTime secondChance = LocalDateTime.of(2020, 12, 25, 0, 0, 0, 0);
	public static final LocalDateTime closingDay = LocalDateTime.of(2021, 1, 11, 0, 0, 0, 0);

	public static final List<Hologram> holograms = new ArrayList<>();
	@Getter
	private static final String questLore = "Pugmas20 Quest Item";
	@Getter
	private static final String itemLore = "Pugmas20 Item";
	// Advent Menu

	@Getter
	private static final Map<String, Integer> tokenMaxes = new HashMap<>();

	public Pugmas20() {
		Nexus.registerListener(this);

		AdventMenu.loadHeads();
		new AdventChests();
		new Quests();
		new Train();
		createNpcHolograms();
		npcParticles();
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
		Pugmas20Service service = new Pugmas20Service();
		Particle particle = Particle.VILLAGER_HAPPY;

		Tasks.repeatAsync(0, Time.SECOND.x(2), () -> {
			for (Player player : WGUtils.getPlayersInRegion(region)) {
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

	public static int checkDailyTokens(Player player, String id, int amount) {
		EventUserService service = new EventUserService();
		EventUser user = service.get(player);

		return user.checkDaily(("pugmas20_" + id).toLowerCase(), amount, tokenMaxes);
	}

	public static void giveDailyTokens(Player player, String id, int amount) {
		EventUserService service = new EventUserService();
		EventUser user = service.get(player);

		user.giveTokens(("pugmas20_" + id).toLowerCase(), amount, tokenMaxes);
		service.save(user);
	}

	public static Location location(int x, int y, int z) {
		return new Location(world, x, y, z);
	}

	public static ItemBuilder questItem(Material material) {
		return new ItemBuilder(material).lore(questLore);
	}

	public static ItemBuilder item(Material material) {
		return item(new ItemStack(material));
	}

	public static ItemBuilder item(ItemStack itemStack) {
		return new ItemBuilder(itemStack).lore(itemLore);
	}

	public static boolean isBeforePugmas(LocalDateTime localDateTime) {
		return localDateTime.isBefore(openingDay);
	}

	public static boolean isPastPugmas(LocalDateTime localDateTime) {
		return localDateTime.isAfter(closingDay);
	}

	public static boolean isSecondChance(LocalDateTime localDateTime) {
		return ((localDateTime.isEqual(secondChance) || localDateTime.isAfter(secondChance))
				&& !isPastPugmas(localDateTime));
	}

	public static boolean isAtPugmas(Player player) {
		return isAtPugmas(player.getLocation());
	}

	public static boolean isAtPugmas(Location location) {
		return WGUtils.isInRegion(location, region);
	}

	public static boolean isAtPugmas(Player player, String name) {
		return isAtPugmas(player.getLocation(), name);
	}

	public static boolean isAtPugmas(Location location, String name) {
		return !WGUtils.getRegionsLikeAt(getRegion() + "_" + name + "(_[0-9]+)?", location).isEmpty();
	}

	@EventHandler
	public void onNPCClick(NPCRightClickEvent event) {
		Player player = event.getClicker();
		if (!isAtPugmas(player)) return;
		if (!new CooldownService().check(event.getClicker(), "Pugmas20_NPC", Time.SECOND.x(2))) return;

		int id = event.getNPC().getId();
		QuestNPC.startScript(player, id);
		Merchants.openMerchant(player, id);
	}

	@EventHandler
	public void onTeleport(PlayerTeleportEvent event) {
		Pugmas20Service service = new Pugmas20Service();

		if (isAtPugmas(event.getFrom()) && !isAtPugmas(event.getTo())) {
			Pugmas20User user = service.get(event.getPlayer());
			user.storeInventory();
		}

		if (isAtPugmas(event.getTo()) && !isAtPugmas(event.getFrom())) {
			Tasks.wait(1, () -> {
				Pugmas20User user = service.get(event.getPlayer());
				user.applyInventory();
			});
		}
	}

}