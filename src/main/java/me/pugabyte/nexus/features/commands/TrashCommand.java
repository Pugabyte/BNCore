package me.pugabyte.nexus.features.commands;

import lombok.NoArgsConstructor;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Arg;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.dumpster.Dumpster;
import me.pugabyte.nexus.models.dumpster.DumpsterService;
import me.pugabyte.nexus.utils.ItemBuilder;
import me.pugabyte.nexus.utils.ItemBuilder.ItemSetting;
import me.pugabyte.nexus.utils.ItemUtils;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.StringUtils;
import me.pugabyte.nexus.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

@NoArgsConstructor
public class TrashCommand extends CustomCommand implements Listener {
	private static final String TITLE = StringUtils.colorize("&4Trash");

	public TrashCommand(CommandEvent event) {
		super(event);
	}

	@Path
	void trash() {
		player().openInventory(Bukkit.createInventory(null, 6 * 9, TITLE));
	}

	@Path("<materials...>")
	void trash(@Arg(type = Material.class) List<Material> materials) {
		DumpsterService dumpsterService = new DumpsterService();
		Dumpster dumpster = dumpsterService.get0();

		for (Material material : materials) {
			dumpster.add(inventory().all(material).values());
			inventory().remove(material);
		}

		dumpsterService.save(dumpster);
		send(PREFIX + "Trashed all matching materials");
	}

	@EventHandler
	public void onChestClose(InventoryCloseEvent event) {
		if (event.getInventory().getHolder() != null) return;
		if (!Utils.equalsInvViewTitle(event.getView(), TITLE)) return;

		DumpsterService service = new DumpsterService();
		Dumpster dumpster = service.get0();

		for (ItemStack item : event.getInventory().getContents()) {
			if (ItemUtils.isNullOrAir(item))
				continue;

			if (new ItemBuilder(item).isNot(ItemSetting.TRASHABLE)) {
				PlayerUtils.giveItem((Player) event.getPlayer(), item);
				continue;
			}

			dumpster.add(item);
		}

		service.save(dumpster);
	}

}
