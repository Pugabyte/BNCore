package me.pugabyte.bearnation.survival.features.commands;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import me.pugabyte.bearnation.api.framework.commands.models.CustomCommand;
import me.pugabyte.bearnation.api.framework.commands.models.annotations.Arg;
import me.pugabyte.bearnation.api.framework.commands.models.annotations.Path;
import me.pugabyte.bearnation.api.framework.commands.models.annotations.Permission;
import me.pugabyte.bearnation.api.framework.commands.models.events.CommandEvent;
import me.pugabyte.bearnation.api.framework.exceptions.postconfigured.InvalidInputException;
import me.pugabyte.bearnation.api.utils.Utils;
import me.pugabyte.bearnation.features.menus.MenuUtils;
import me.pugabyte.bearnation.models.dumpster.Dumpster;
import me.pugabyte.bearnation.models.dumpster.DumpsterService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static me.pugabyte.bearnation.api.utils.StringUtils.colorize;

@NoArgsConstructor
@Permission("group.staff")
public class DumpsterDivingCommand extends CustomCommand implements Listener {
	private final DumpsterService service = new DumpsterService();
	private final Dumpster dumpster = service.get();

	public DumpsterDivingCommand(@NonNull CommandEvent event) {
		super(event);
	}

	@Path
	void run() {
		DumpsterProvider.open(player());
	}

	@Path("debug")
	void debug() {
		send(dumpster.toString());
	}

	@Path("test add <material> [amount]")
	void addTest(Material material, @Arg("1") int amount) {
		dumpster.add(new ItemStack(material, amount));
		service.save(dumpster);
		send("Saved");
	}

	@Path("clear")
	void clear() {
		service.clearCache();
		service.deleteAll();
		service.clearCache();
		send("Deleted all dumpster items");
	}

	@NoArgsConstructor
	private static class DumpsterProvider extends MenuUtils implements InventoryProvider {
		private final DumpsterService service = new DumpsterService();
		private final Dumpster dumpster = service.get();

		public static void open(Player player) {
			if (new DumpsterService().get().getItems().size() == 0)
				throw new InvalidInputException("Dumpster is empty");

			SmartInventory.builder()
					.provider(new DumpsterProvider())
					.size(6, 9)
					.title(colorize("&2Dumpster"))
					.build()
					.open(player);
		}

		@Override
		public void init(Player player, InventoryContents contents) {
			addCloseItem(contents);

			contents.set(0, 8, ClickableItem.from(nameItem(Material.IRON_SHOVEL, "Refresh"), e -> {
				try {
					open(player);
				} catch (Exception ex) {
					handleException(player, ex);
				}
			}));

			List<ItemStack> items = new ArrayList<>(dumpster.getItems()).subList(0, Math.min(dumpster.getItems().size(), 5 * 9));
			Collections.shuffle(items);

			for (int row = 1; row <= 5; row++) {
				for (int column = 0; column <= 8; column++) {
					ItemStack item = items.remove(0);
					contents.set(row, column, ClickableItem.from(item, e -> {
						try {
							contents.set(e.getSlot(), ClickableItem.NONE);
							dumpster.getItems().remove(item);
							service.save(dumpster);
							Utils.giveItem(player, item);
						} catch (Exception ex) {
							handleException(player, ex);
						}
					}));

					if (items.size() == 0)
						return;
				}
			}
		}

		@Override
		public void update(Player player, InventoryContents contents) {}

	}


}
