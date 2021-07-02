package me.pugabyte.nexus.features.minigames.menus.custom;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import me.pugabyte.nexus.features.menus.MenuUtils;
import me.pugabyte.nexus.features.minigames.managers.ArenaManager;
import me.pugabyte.nexus.features.minigames.mechanics.UncivilEngineers;
import me.pugabyte.nexus.features.minigames.menus.annotations.CustomMechanicSettings;
import me.pugabyte.nexus.features.minigames.models.Arena;
import me.pugabyte.nexus.features.minigames.models.arenas.UncivilEngineersArena;
import me.pugabyte.nexus.features.minigames.models.arenas.UncivilEngineersArena.MobPoint;
import me.pugabyte.nexus.features.mobheads.MobHeadType;
import me.pugabyte.nexus.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static eden.utils.StringUtils.camelCase;
import static me.pugabyte.nexus.features.minigames.Minigames.menus;
import static me.pugabyte.nexus.utils.ItemUtils.isNullOrAir;

@CustomMechanicSettings(UncivilEngineers.class)
public class UncivilEngineersMenu extends MenuUtils implements InventoryProvider {
	private final UncivilEngineersArena arena;

	public UncivilEngineersMenu(Arena arena) {
		this.arena = ArenaManager.convert(arena, UncivilEngineersArena.class);
	}

	@Override
	public void open(Player viewer, int page) {
		SmartInventory.builder()
			.provider(new MobPointsMenu())
			.title("Mob Points")
			.size(6, 9)
			.build()
			.open(viewer);
	}

	@Override
	public void init(Player player, InventoryContents contents) {
		contents.set(0, 0, ClickableItem.from(backItem(), e -> menus.openArenaMenu(player, arena)));

		contents.set(1, 0, ClickableItem.from(nameItem(new ItemStack(Material.ZOMBIE_SPAWN_EGG), "&eMob Points"), e -> new MobPointsMenu().open(player)));
	}

	public class MobPointsMenu extends MenuUtils implements InventoryProvider {

		@Override
		public void open(Player viewer, int page) {
			SmartInventory.builder()
				.provider(this)
				.title("Mob Points")
				.size(6, 9)
				.build()
				.open(viewer);
		}

		@Override
		public void init(Player player, InventoryContents contents) {
			contents.set(0, 0, ClickableItem.from(backItem(), e -> menus.openCustomSettingsMenu(player, arena)));

			contents.set(0, 4, ClickableItem.from(nameItem(Material.EMERALD_BLOCK, "&aAdd Mob Point"), e -> new AddMobPointMenu().open(player)));

			List<ClickableItem> items = new ArrayList<>();

			for (MobPoint mobPoint : arena.getMobPoints()) {
				final MobHeadType mobHeadType = MobHeadType.of(mobPoint.getType());
				ItemStack skull;
				if (mobHeadType == null)
					skull = new ItemStack(Material.BARRIER);
				else {
					skull = mobHeadType.getGeneric();
					if (isNullOrAir(skull))
						skull = new ItemStack(Material.BARRIER);
				}

				final String lore = getLocationLore(mobPoint.getLocation()) + "|| ||&7Click to remove";
				final ItemStack item = nameItem(skull, "&3" + camelCase(mobPoint.getType()), lore);

				items.add(ClickableItem.from(item, e -> {
					arena.getMobPoints().remove(mobPoint);
					arena.write();
					new MobPointsMenu().open(player);
				}));
			}

			addPagination(player, contents, items);
		}

	}

	public class AddMobPointMenu extends MenuUtils implements InventoryProvider {

		@Override
		public void open(Player viewer, int page) {
			SmartInventory.builder()
				.provider(this)
				.title("Add Mob Point")
				.size(6, 9)
				.build()
				.open(viewer, page);
		}

		@Override
		public void init(Player player, InventoryContents contents) {
			contents.set(0, 0, ClickableItem.from(backItem(), e -> menus.openCustomSettingsMenu(player, arena)));

			List<ClickableItem> items = new ArrayList<>();

			for (EntityType type : EntityType.values()) {
				if (type.getEntityClass() == null)
					continue;
				if (!LivingEntity.class.isAssignableFrom(type.getEntityClass()))
					continue;

				final MobHeadType mobHeadType = MobHeadType.of(type);
				if (mobHeadType == null)
					continue;
				final ItemStack skull = mobHeadType.getGeneric();
				if (isNullOrAir(skull))
					continue;

				final ItemStack item = new ItemBuilder(skull).name("&e" + camelCase(type)).build();

				items.add(ClickableItem.from(item, e -> {
					arena.getMobPoints().add(new MobPoint(player.getLocation(), type));
					arena.write();
					new MobPointsMenu().open(player);
				}));
			}

			addPagination(player, contents, items);
		}

	}

}
