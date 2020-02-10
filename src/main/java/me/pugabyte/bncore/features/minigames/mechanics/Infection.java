package me.pugabyte.bncore.features.minigames.mechanics;

import me.pugabyte.bncore.BNCore;
import me.pugabyte.bncore.features.minigames.models.Match;
import me.pugabyte.bncore.features.minigames.models.Minigamer;
import me.pugabyte.bncore.features.minigames.models.Team;
import me.pugabyte.bncore.features.minigames.models.mechanics.multiplayer.teams.UnbalancedTeamMechanic;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.inventory.ItemStack;

public class Infection extends UnbalancedTeamMechanic {

	@Override
	public String getName() {
		return "Infection";
	}

	@Override
	public String getDescription() {
		return "Zombies kill humans";
	}

	@Override
	public ItemStack getMenuItem() {
		return new ItemStack(Material.SKULL_ITEM, 1, (byte) SkullType.ZOMBIE.ordinal());
	}

	// TODO: Validation on start (e.g. only two teams, one has lives, balance percentages)

	@Override
	public void kill(Minigamer victim, Minigamer attacker) {
		Match match = victim.getMatch();
		Team other = match.getArena().getTeams().stream()
				.filter(team -> !team.equals(victim.getTeam()))
				.findFirst()
				.orElse(null);

		if (attacker != null)
			other = attacker.getTeam();

		if (other == null)
			BNCore.severe("[Infection] Could not find team to switch player to! (Victim: " + victim.getName() +
					" | Attacker: " + (attacker == null ? "null" : attacker.getName()) + ")");
		else
			if (victim.getTeam().getLives() != 0) {
				victim.died();
				if (victim.getLives() == 0) {
					victim.setTeam(other);
					match.broadcast(victim.getColoredName() + " has joined the " + victim.getTeam().getName());
				}
			}

		super.kill(victim, attacker);
	}

}
