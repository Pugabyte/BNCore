package me.pugabyte.bncore.features.minigames.menus.teams;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import me.pugabyte.bncore.BNCore;
import me.pugabyte.bncore.features.menus.MenuUtils;
import me.pugabyte.bncore.features.minigames.managers.ArenaManager;
import me.pugabyte.bncore.features.minigames.menus.MinigamesMenus;
import me.pugabyte.bncore.features.minigames.models.Arena;
import me.pugabyte.bncore.features.minigames.models.Team;
import me.pugabyte.bncore.utils.Utils;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class TeamEditorMenu extends MenuUtils implements InventoryProvider {

    Arena arena;
    Team team;
    MinigamesMenus menus = new MinigamesMenus();
    TeamMenus teamMenus = new TeamMenus();
    public TeamEditorMenu(Arena arena, Team team){
        this.arena = arena;
        this.team = team;
    }

    @Override
    public void init(Player player, InventoryContents contents) {
        //Back Item
        contents.set(0, 0, ClickableItem.of(backItem(), e-> teamMenus.openTeamsMenu(player, arena)));

        //Name Item
        contents.set(1, 0, ClickableItem.of(nameItem(new ItemStack(Material.BOOK),
                "&eTeam Name", " ||&3Current Name:||&e" + team.getName()), e -> {
            player.closeInventory();
            new AnvilGUI.Builder()
                    .onClose(p -> teamMenus.openTeamsEditorMenu(player, arena, team))
                    .onComplete((p, text) -> {
                        List<Team> teams = new ArrayList<>(arena.getTeams());
                        teams.remove(team);
                        team.setName(text);
                        teams.add(team);
                        arena.setTeams(teams);
                        ArenaManager.write(arena);
                        ArenaManager.add(arena);
                        teamMenus.openTeamsEditorMenu(player, arena, team);
                        return AnvilGUI.Response.text(text);
                    })
                    .text("Team Name")
                    .plugin(BNCore.getInstance())
                    .open(player);
        }));
        //Objective Item
        contents.set(1, 2, ClickableItem.of(nameItem(new ItemStack(Material.SIGN),
                "Team Objective", "||&3Current Objective:||&e" + team.getObjective()), e->{
            new AnvilGUI.Builder()
                    .onClose(p -> teamMenus.openTeamsEditorMenu(player, arena, team))
                    .onComplete((p, text) -> {
                        List<Team> teams = new ArrayList<>(arena.getTeams());
                        teams.remove(team);
                        team.setObjective(text);
                        teams.add(team);
                        arena.setTeams(teams);
                        ArenaManager.write(arena);
                        ArenaManager.add(arena);
                        teamMenus.openTeamsEditorMenu(player, arena, team);
                        return AnvilGUI.Response.text(text);
                    })
                    .text("Team Objective")
                    .plugin(BNCore.getInstance())
                    .open(player);
                }));
        //Team Color Item
        contents.set(1, 4, ClickableItem.of(nameItem(new ItemStack(Material.WOOL, 1, (byte) Utils.getColorInt(Utils.getColor(team.getColor()))),
                "Team Color", "&7Set the color of the team"), e-> teamMenus.openTeamsColorMenu(player, arena, team)));
        //Spawnpoints Item
        contents.set(1, 6, ClickableItem.empty(nameItem(new ItemStack(Material.COMPASS),
                "Spawnpoint Locations", "&7Set locations the players||&7on the team can spawn.")));
        //Balance Percentage Item
        contents.set(1, 0, ClickableItem.of(nameItem(new ItemStack(Material.IRON_PLATE),
                "&eBalance Percentage", "&7Set to -1 to disable||&7team balancing.|| ||&3Current Percentage:||&e" + team.getBalancePercentage()), e -> {
            player.closeInventory();
            new AnvilGUI.Builder()
                    .onClose(p -> teamMenus.openTeamsEditorMenu(player, arena, team))
                    .onComplete((p, text) -> {
                        if(!Utils.isInt(text)){
                            player.closeInventory();
                            player.sendMessage(Utils.getPrefix("JMinigames") + "The balance percentage must be an integer.");
                            return AnvilGUI.Response.close();
                        }
                        List<Team> teams = new ArrayList<>(arena.getTeams());
                        teams.remove(team);
                        team.setBalancePercentage(Integer.parseInt(text));
                        teams.add(team);
                        arena.setTeams(teams);
                        ArenaManager.write(arena);
                        ArenaManager.add(arena);
                        teamMenus.openTeamsEditorMenu(player, arena, team);
                        return AnvilGUI.Response.text(text);
                    })
                    .text("Team Name")
                    .plugin(BNCore.getInstance())
                    .open(player);
        }));
        //Loadout Item
        contents.set(2, 4, ClickableItem.of(nameItem(new ItemStack(Material.CHEST),
                "Loadout"), e->{

        }));
    }

    @Override
    public void update(Player player, InventoryContents inventoryContents) {

    }
}
