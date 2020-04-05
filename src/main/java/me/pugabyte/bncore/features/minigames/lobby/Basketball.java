package me.pugabyte.bncore.features.minigames.lobby;

import com.mewin.worldguardregionapi.events.RegionEnteredEvent;
import com.mewin.worldguardregionapi.events.RegionLeftEvent;
import de.tr7zw.itemnbtapi.NBTEntity;
import de.tr7zw.itemnbtapi.NBTItem;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.pugabyte.bncore.BNCore;
import me.pugabyte.bncore.features.minigames.Minigames;
import me.pugabyte.bncore.framework.commands.models.CustomCommand;
import me.pugabyte.bncore.framework.commands.models.annotations.Arg;
import me.pugabyte.bncore.framework.commands.models.annotations.Path;
import me.pugabyte.bncore.framework.commands.models.annotations.Permission;
import me.pugabyte.bncore.framework.commands.models.events.CommandEvent;
import me.pugabyte.bncore.utils.Tasks;
import me.pugabyte.bncore.utils.WorldGuardUtils;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static me.pugabyte.bncore.utils.StringUtils.colorize;

@NoArgsConstructor
@Permission("group.staff")
public class Basketball extends CustomCommand implements Listener {
	@Getter
	static ItemStack basketball = (ItemStack) BNCore.getInstance().getConfig().get("minigames.lobby.basketball.item");
	static Map<UUID, ItemStack> basketballs = new HashMap<>();
	static World world = Minigames.getWorld();
	static String region = "minigamelobby_basketball";

	static {
		janitor();
	}

	public Basketball(CommandEvent event) {
		super(event);
	}

	@Path("save")
	void save() {
		ItemStack basketball = player().getInventory().getItemInMainHand();
		ItemMeta meta = basketball.getItemMeta();
		meta.setDisplayName(colorize("&6&lBasketball"));
		meta.setLore(Collections.singletonList(colorize("&eMinigame Lobby Basketball")));
		basketball.setItemMeta(meta);
		BNCore.getInstance().getConfig().set("minigames.lobby.basketball.item", basketball);
		BNCore.getInstance().saveConfig();
		Basketball.basketball = basketball;
		send(PREFIX + "Basketball saved");
	}

	@Path("give [player]")
	void give(@Arg("self") Player player) {
		Basketball.giveBasketball(player);
	}

	public static ItemStack getBasketball(Player player) {
		if (!basketballs.containsKey(player.getUniqueId())) {
			ItemStack basketball = getBasketball().clone();
			NBTItem nbtItem = new NBTItem(basketball);
			nbtItem.getCompound("SkullOwner").setString("Id", player.getUniqueId().toString());
			basketballs.put(player.getUniqueId(), nbtItem.getItem());
		}

		return basketballs.get(player.getUniqueId());
	}

	public static void giveBasketball(Player player) {
		if (!hasBasketball(player))
			player.getInventory().addItem(getBasketball(player));
	}

	public static void removeBasketball(Player player) {
		if (hasBasketball(player))
			player.getInventory().remove(getBasketball(player));
	}

	private static boolean ownsBasketball(Player player, ItemStack item) {
		return new NBTItem(item).asNBTString().contains(player.getUniqueId().toString());
	}

	private static boolean ownsBasketball(Player player, Entity entity) {
		return new NBTEntity(entity).asNBTString().contains(player.getUniqueId().toString());
	}

	private static boolean isBasketball(ItemStack itemStack) {
		if (itemStack.getItemMeta().getLore() == null || itemStack.getItemMeta().getLore().size() == 0)
			return false;
		return itemStack.getItemMeta().getLore().get(0).contains("Minigame Lobby Basketball");
	}

	private static boolean isBasketball(Entity entity) {
		return entity instanceof Item && new NBTEntity(entity).asNBTString().contains("Minigame Lobby Basketball");
	}

	private static boolean hasBasketball(Player player) {
		return player.getInventory().contains(getBasketball(player));
	}

	private static Collection<Entity> getLobbyEntities() {
		return world.getNearbyEntities(Minigames.getLobby(), 200, 200, 200);
	}

	private static void cleanupBasketballs() {
		getLobbyEntities().forEach(entity -> {
			if (!Minigames.getWorldGuardUtils().isInRegion(entity.getLocation(), region))
				if (isBasketball(entity))
					entity.remove();
		});
	}

	private static void janitor() {
		WorldGuardUtils wgUtils = Minigames.getWorldGuardUtils();
		Tasks.repeat(0, 20 * 20, () -> {
			cleanupBasketballs();

			List<Player> players = Bukkit.getOnlinePlayers().stream()
					.filter(player -> player.getWorld() == world)
					.collect(Collectors.toList());

			players.forEach(player -> {
				if (wgUtils.isInRegion(player.getLocation(), region)) {
					if (!hasBasketball(player)) {
						boolean found = false;
						for (Entity entity : getLobbyEntities())
							if (wgUtils.isInRegion(entity.getLocation(), region)) {
								found = true;
								break;
							}

						if (!found)
							giveBasketball(player);
					}
				} else {
					removeBasketball(player);
				}
			});
		});
	}

	private class BasketballThrowWatcher {
		private int taskId;
		private int iteration;
		private Player player;
		private Item entity;
		private WorldGuardUtils wgUtils = Minigames.getWorldGuardUtils();

		BasketballThrowWatcher(Player player, Item item) {
			this.player = player;
			this.entity = item;
			start();
		}

		void start() {
			taskId = Tasks.repeat(0, 1, () -> {
				++iteration;

				if (!wgUtils.isInRegion(entity.getLocation(), region)) {
					entity.remove();
					giveBasketball(player);
					stop();
				} else if (wgUtils.isInRegion(entity.getLocation(), region + "_hoop")) {
					entity.remove();
					giveBasketball(player);
					player.sendMessage(colorize("&eTouchdown!!"));
					wgUtils.getPlayersInRegion(region).forEach(loopPlayer ->
							loopPlayer.spawnParticle(Particle.LAVA, entity.getLocation(), 50, 2, 2, 2, .01));
					stop();
				} else if (wgUtils.isInRegion(entity.getLocation(), region + "_backboard")) {
					entity.remove();
					giveBasketball(player);
					player.sendMessage(colorize("&eSo close..."));
					stop();
				}

				if (iteration == 60) {
					if (wgUtils.isInRegion(entity.getLocation(), region + "_stuck")) {
						entity.remove();
						giveBasketball(player);
					}
					stop();
				}
			});
		}

		void stop() {
			Tasks.cancel(taskId);
		}
	}

	@EventHandler
	public void onPickup(EntityPickupItemEvent event) {
		if (!(event.getEntity() instanceof Player)) return;
		Player player = (Player) event.getEntity();
		if (player.getWorld() != world) return;
		if (!isBasketball(event.getItem().getItemStack())) return;

		if (!Minigames.getWorldGuardUtils().isInRegion(player.getLocation(), region))
			event.setCancelled(true);
		else if (!ownsBasketball(player, event.getItem().getItemStack()))
			event.setCancelled(true);
	}

	@EventHandler
	public void onDrop(PlayerDropItemEvent event) {
		if (event.getPlayer().getWorld() != world) return;
		if (!isBasketball(event.getItemDrop().getItemStack())) return;

		new BasketballThrowWatcher(event.getPlayer(), event.getItemDrop());
	}

	@EventHandler
	public void onRegionEnter(RegionEnteredEvent event) {
		if (!event.getRegion().getId().equalsIgnoreCase(region)) return;
		giveBasketball(event.getPlayer());
	}

	@EventHandler
	public void onRegionLeave(RegionLeftEvent event) {
		if (!event.getRegion().getId().equalsIgnoreCase(region)) return;
		removeBasketball(event.getPlayer());
	}


}
