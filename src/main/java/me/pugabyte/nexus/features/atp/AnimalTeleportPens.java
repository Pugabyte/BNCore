package me.pugabyte.nexus.features.atp;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import lombok.NoArgsConstructor;
import me.pugabyte.nexus.features.menus.MenuUtils.ConfirmationMenu;
import me.pugabyte.nexus.models.banker.Banker;
import me.pugabyte.nexus.models.banker.BankerService;
import me.pugabyte.nexus.models.banker.Transaction.TransactionCause;
import me.pugabyte.nexus.models.shop.Shop.ShopGroup;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.StringUtils;
import me.pugabyte.nexus.utils.Tasks;
import me.pugabyte.nexus.utils.WorldGuardUtils;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

import static me.pugabyte.nexus.utils.CitizensUtils.isNPC;
import static me.pugabyte.nexus.utils.StringUtils.colorize;

@NoArgsConstructor
public class AnimalTeleportPens {
	private final String PREFIX = StringUtils.getPrefix("ATP");
	private Player player;
	private WorldGuardUtils WGUtils;

	public AnimalTeleportPens(Player player) {
		this.player = player;
		this.WGUtils = new WorldGuardUtils(player);
	}

	public boolean multiplePlayers() {
		return WGUtils.getPlayersInRegion(getRegion(player)).size() > 1;
	}

	public List<Entity> getEntities() {
		List<Entity> finalEntities = new ArrayList<>();
		for (Entity entity : WGUtils.getEntitiesInRegion(getRegion(player))) {
			if (isNPC(entity))
				continue;

			switch (entity.getType()) {
				case PIG, RABBIT, FOX, TURTLE, COW, SHEEP, MUSHROOM_COW, POLAR_BEAR,
					PANDA, GOAT, HORSE, DONKEY, MULE, LLAMA, TRADER_LLAMA, VILLAGER ->
					finalEntities.add(entity);
			}
		}
		return finalEntities;
	}

	public int getPrice(List<Entity> entities) {
		int price = 0;
		for (Entity entity : entities) {
			switch (entity.getType()) {
				case PIG, RABBIT, FOX, TURTLE ->
						price += 100;
				case COW, SHEEP, MUSHROOM_COW, POLAR_BEAR, PANDA, GOAT ->
						price += 150;
				case HORSE, DONKEY, MULE, LLAMA, TRADER_LLAMA ->
						price += 250;
				case VILLAGER ->
						price += 500;
				default ->
						price += 50;
			}
		}
		return price;
	}

	public ProtectedRegion getRegion(Player player) {
		return WGUtils.getRegionsLikeAt("atp_.*", player.getLocation()).stream().findFirst().orElse(null);
	}

	public void confirm(Player player, Location toLoc) {
		ProtectedRegion region = getRegion(player);
		if (region == null) {
			PlayerUtils.send(player, PREFIX + "&cYou are not inside an ATP region");
			return;
		}

		if (multiplePlayers()) {
			player.closeInventory();
			PlayerUtils.send(player, PREFIX + "&cDetected multiple players. Cancelling.");
			return;
		}

		List<Entity> entities = getEntities();
		if (entities.size() == 0) {
			PlayerUtils.send(player, PREFIX + "&cThere are no entities to teleport");
			return;
		}

		int price = getPrice(entities);
		if (!Banker.of(player).has(price, ShopGroup.of(player))) {
			PlayerUtils.send(player, PREFIX + "&cYou do not have enough money to use the ATP");
			return;
		}

		ConfirmationMenu.builder()
				.title(colorize("&3Teleport &e" + entities.size() + " &3entities for &e$" + price + "&3?"))
				.onConfirm(e -> Tasks.wait(4, () -> teleportAll(entities, toLoc, price)))
				.open(player);
	}

	public void teleportAll(List<Entity> entities, Location toLoc, int price) {
		if (entities.size() > 0) {
			entities.get(0).teleport(toLoc);
			Tasks.wait(1, () -> {
				entities.remove(0);
				teleportAll(entities, toLoc, price);
			});
		}
		Tasks.wait(4, () -> {
			player.teleport(toLoc);
			new BankerService().withdraw(player, price, ShopGroup.SURVIVAL, TransactionCause.ANIMAL_TELEPORT_PEN);
		});
	}

}
