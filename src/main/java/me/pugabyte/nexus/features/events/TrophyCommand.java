package me.pugabyte.nexus.features.events;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import me.pugabyte.nexus.features.menus.MenuUtils;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Aliases;
import me.pugabyte.nexus.framework.commands.models.annotations.Confirm;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.trophy.Trophy;
import me.pugabyte.nexus.models.trophy.TrophyHolder;
import me.pugabyte.nexus.models.trophy.TrophyHolderService;
import me.pugabyte.nexus.utils.ItemBuilder;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.StringUtils;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@Aliases("trophies")
public class TrophyCommand extends CustomCommand {
	private final TrophyHolderService service = new TrophyHolderService();
	private TrophyHolder holder;

	public TrophyCommand(@NonNull CommandEvent event) {
		super(event);
		if (isPlayerCommandEvent())
			holder = service.get(player());
	}

	@Path
	void menu() {
		if (holder.getEarned().isEmpty())
			error("You have not earned any event trophies! Participate in server hosted events to earn them");

		new TrophyMenu().open(player());
	}

	@Permission("group.admin")
	@Path("reward <player> <trophy>")
	void reward(TrophyHolder holder, Trophy trophy) {
		if (holder.earn(trophy)) {
			send(PREFIX + "Rewarded " + camelCase(trophy) + " trophy to " + holder.getNickname());
			service.save(holder);
		} else
			error(holder.getNickname() + " has already earned that trophy");
	}

	@Confirm
	@Permission("group.admin")
	@Path("remove <player> <trophy>")
	void remove(TrophyHolder holder, Trophy trophy) {
		holder.getEarned().remove(trophy);
		holder.getClaimed().remove(trophy);
		service.save(holder);
		send(PREFIX + "Reset " + holder.getNickname() + "'s trophies");
	}

	@Confirm
	@Permission("group.admin")
	@Path("reset <player>")
	void reward(TrophyHolder holder) {
		holder.getEarned().clear();
		holder.getClaimed().clear();
		service.save(holder);
		send(PREFIX + "Reset " + holder.getNickname() + "'s trophies");
	}

	@Permission("group.admin")
	@Path("get <trophy>")
	void get(Trophy trophy) {
		PlayerUtils.giveItem(player(), trophy.getItem().build());
		send(PREFIX + "Gave " + camelCase(trophy) + " trophy");
	}

	@AllArgsConstructor
	private static class TrophyMenu extends MenuUtils implements InventoryProvider {
		private final String event;
		private final TrophyMenu previousMenu;

		@Override
		public void open(Player viewer, int page) {
			SmartInventory.builder()
					.provider(this)
					.title("Trophies")
					.size(6, 9)
					.build()
					.open(viewer, page);
		}

		public TrophyMenu() {
			this(null);
		}

		public TrophyMenu(String event) {
			this(event, null);
		}

		@Override
		public void init(Player player, InventoryContents contents) {
			final TrophyHolderService service = new TrophyHolderService();
			final TrophyHolder holder = service.get(player);

			if (previousMenu == null)
				addCloseItem(contents);
			else
				addBackItem(contents, e -> previousMenu.open(player));

			List<ClickableItem> items = new ArrayList<>();

			if (StringUtils.isNullOrEmpty(event)) {
				for (String event : Trophy.getEvents()) {
					if (Trophy.getEarnedTrophies(holder, event).isEmpty())
						continue;

					ItemBuilder item = Trophy.getDisplayItem(holder, event).name(StringUtils.camelCase(event));
					items.add(ClickableItem.from(item.build(), e -> new TrophyMenu(event, this).open(player)));
				}
			} else {
				for (Trophy trophy : Trophy.getEarnedTrophies(holder, event)) {
					ItemBuilder item = trophy.getItem();
					if (holder.hasClaimed(trophy)) {
						item.lore("", "&cClaimed");
						items.add(ClickableItem.empty(item.build()));
					} else {
						item.lore("", "&eClick to receive a copy");
						items.add(ClickableItem.from(item.build(), e -> {
							holder.claim(trophy);
							open(player);
							service.save(holder);
						}));
					}
				}
			}

			addPagination(player, contents, items);
		}

	}

}
