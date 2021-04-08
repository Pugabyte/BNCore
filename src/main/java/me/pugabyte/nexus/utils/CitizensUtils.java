package me.pugabyte.nexus.utils;

import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.models.nerd.Nerd;
import me.pugabyte.nexus.models.nickname.Nickname;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.npc.skin.SkinnableEntity;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import static me.pugabyte.nexus.utils.StringUtils.stripColor;

public class CitizensUtils {

	public static NPC getNPC(int id) {
		return CitizensAPI.getNPCRegistry().getById(id);
	}

	public static void updateNameAndSkin(int id, String name) {
		updateNameAndSkin(getNPC(id), name);
	}

	public static void updateNameAndSkin(NPC npc, String name) {
		updateName(npc, name);
		updateSkin(npc, name);
	}

	/**
	 * Sets an NPC to a player's (nick)name and skin
	 * @param npc NPC to update
	 * @param player a server member
	 */
	public static void updateNameAndSkin(NPC npc, OfflinePlayer player) {
		updateName(npc, Nickname.of(player));
		updateSkin(npc, player.getName());
	}

	/**
	 * Sets an NPC to a player's (nick)name and skin
	 * @param npc NPC to update
	 * @param nerd a server member
	 */
	public static void updateNameAndSkin(NPC npc, Nerd nerd) {
		updateName(npc, Nickname.of(nerd));
		updateSkin(npc, nerd.getOfflinePlayer().getName());
	}

	public static void updateName(int id, String name) {
		updateName(getNPC(id), name);
	}

	public static void updateName(NPC npc, String name) {
		Tasks.sync(() -> npc.setName(name));
	}

	public static void updateSkin(int id, String name) {
		updateSkin(getNPC(id), name);
	}

	public static void updateSkin(NPC npc, String name) {
		updateSkin(npc, name, false);
	}

	public static void updateSkin(NPC npc, String name, boolean useLatest) {
		Tasks.sync(() -> {
			npc.data().setPersistent(NPC.PLAYER_SKIN_UUID_METADATA, stripColor(name));
			npc.data().setPersistent(NPC.PLAYER_SKIN_USE_LATEST, useLatest);

			Entity npcEntity = npc.getEntity();
			if (npcEntity instanceof SkinnableEntity) {
				((SkinnableEntity) npcEntity).getSkinTracker().notifySkinChange(npc.data().get(NPC.PLAYER_SKIN_USE_LATEST));
			}
		});
	}

	public static boolean isNPC(Entity entity) {
		return entity.hasMetadata("NPC");
	}

	public static NPC getSelectedNPC(Player player) {
		return Nexus.getCitizens().getNPCSelector().getSelected(player);
	}

	/* Doesnt work
	public static void setSelectedNPC(Player player, NPC npc) {
		Nexus.getCitizens().getNPCSelector().select(player, npc);
	}
	*/
}
