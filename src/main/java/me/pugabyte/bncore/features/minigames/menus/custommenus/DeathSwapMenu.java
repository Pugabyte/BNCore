package me.pugabyte.bncore.features.minigames.menus.custommenus;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import me.pugabyte.bncore.features.menus.MenuUtils;
import me.pugabyte.bncore.features.minigames.menus.MinigamesMenus;
import me.pugabyte.bncore.features.minigames.models.Arena;
import org.bukkit.entity.Player;

public class DeathSwapMenu extends MenuUtils implements InventoryProvider {

    Arena arena;
    MinigamesMenus menus = new MinigamesMenus();
    public DeathSwapMenu(Arena arena){
        this.arena = arena;
    }

    @Override
    public void init(Player player, InventoryContents contents) {
        contents.set(0, 0, ClickableItem.of(backItem(), e -> menus.openArenaMenu(player, arena)));
    }

    @Override
    public void update(Player player, InventoryContents inventoryContents) {

    }
}
