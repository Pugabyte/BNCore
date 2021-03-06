package me.pugabyte.nexus.features.minigames.menus.custom;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import me.pugabyte.nexus.features.menus.MenuUtils;
import me.pugabyte.nexus.features.minigames.managers.ArenaManager;
import me.pugabyte.nexus.features.minigames.mechanics.Thimble;
import me.pugabyte.nexus.features.minigames.menus.annotations.CustomMechanicSettings;
import me.pugabyte.nexus.features.minigames.models.Arena;
import me.pugabyte.nexus.features.minigames.models.arenas.ThimbleArena;
import org.bukkit.entity.Player;

import static me.pugabyte.nexus.features.minigames.Minigames.menus;

@CustomMechanicSettings(Thimble.class)
public class ThimbleMenu extends MenuUtils implements InventoryProvider {
	ThimbleArena arena;

	public ThimbleMenu(Arena arena) {
		this.arena = ArenaManager.convert(arena, ThimbleArena.class);
		this.arena.write();
	}

	@Override
	public void init(Player player, InventoryContents contents) {
		contents.set(0, 0, ClickableItem.from(backItem(), e -> menus.openArenaMenu(player, arena)));
	}

}
