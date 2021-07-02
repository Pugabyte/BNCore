package me.pugabyte.nexus.features.events.y2021.bearfair21.quests;

import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.commands.staff.WorldGuardEditCommand;
import me.pugabyte.nexus.features.events.y2021.bearfair21.BearFair21;
import me.pugabyte.nexus.models.radio.RadioConfig.Radio;
import me.pugabyte.nexus.models.radio.RadioUser;
import me.pugabyte.nexus.models.radio.RadioUserService;
import me.pugabyte.nexus.utils.BlockUtils;
import me.pugabyte.nexus.utils.ItemUtils;
import me.pugabyte.nexus.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;


public class RadioHeads implements Listener {
	public RadioHeads() {
		Nexus.registerListener(this);
	}

	@EventHandler
	public void onClickRadio(PlayerInteractEvent event) {
		if (BearFair21.isNotAtBearFair(event)) return;
		if (WorldGuardEditCommand.canWorldGuardEdit(event.getPlayer())) return;

		Block block = event.getClickedBlock();
		if (BlockUtils.isNullOrAir(block) || !block.getType().equals(Material.PLAYER_HEAD)) return;

		Location radioHeadLoc = new Location(BearFair21.getWorld(), 16, 119, -19);
		ItemStack item = ItemUtils.getItem(block);
		// item.equals(Nexus.getHeadAPI().getItemHead("17150"))
		if (item.equals(ItemUtils.getItem(radioHeadLoc.getBlock()))) {
			event.setCancelled(true);

			RadioUserService userService = new RadioUserService();
			RadioUser radioUser = userService.get(event.getPlayer());

			Radio radio = radioUser.getServerRadio();
			if (radioUser.getVolume() == 100) {
				radioUser.setVolume((byte) 25);
				userService.save(radioUser);
			}

			if (radio == null || !radio.getId().equalsIgnoreCase("bearfair"))
				PlayerUtils.runCommand(event.getPlayer(), "radio join bearfair");
			else if (radio.getId().equalsIgnoreCase("bearfair"))
				PlayerUtils.runCommand(event.getPlayer(), "radio leave");
		}

	}
}
