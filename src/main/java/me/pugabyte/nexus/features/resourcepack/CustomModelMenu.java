package me.pugabyte.nexus.features.resourcepack;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.pugabyte.nexus.features.menus.MenuUtils;
import me.pugabyte.nexus.utils.ItemBuilder;
import me.pugabyte.nexus.utils.PlayerUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static me.pugabyte.nexus.utils.StringUtils.colorize;

@RequiredArgsConstructor
public class CustomModelMenu extends MenuUtils implements InventoryProvider {
	@NonNull
	private final CustomModelFolder folder;
	private final CustomModelMenu previousMenu;

	public CustomModelMenu() {
		this(ResourcePack.getRootFolder(), null);
	}

	public CustomModelMenu(@NonNull CustomModelFolder folder) {
		this(folder, null);
	}

	@Override
	public void open(Player viewer, int page) {
		String title = "Custom Models";
		if (!folder.getPath().equals("/"))
			title = folder.getDisplayPath();

		SmartInventory.builder()
				.provider(this)
				.title(colorize("&0" + title))
				.size(6, 9)
				.build()
				.open(viewer, page);
	}

	@Override
	public void init(Player player, InventoryContents contents) {
		if (previousMenu == null)
			addCloseItem(contents);
		else
			addBackItem(contents, e -> previousMenu.open(player));

		List<ClickableItem> items = new ArrayList<>();

		for (CustomModelFolder folder : folder.getFolders()) {
			CustomModel firstModel = folder.getIcon();
			ItemStack item = new ItemStack(Material.BARRIER);
			if (firstModel != null)
				item = firstModel.getDisplayItem();

			ItemBuilder builder = new ItemBuilder(item).name(folder.getDisplayPath()).glow();
			items.add(ClickableItem.from(builder.build(), e -> new CustomModelMenu(folder, this).open(player)));
		}

		if (!items.isEmpty()) {
			while (items.size() % 9 != 0)
				items.add(ClickableItem.empty(new ItemStack(Material.AIR)));

			for (int i = 0; i < 9; i++)
				items.add(ClickableItem.empty(new ItemStack(Material.AIR)));
		}

		for (CustomModel model : folder.getModels()) {
			if (model.getFileName().equals("icon"))
				continue;

			ItemBuilder item = new ItemBuilder(model.getDisplayItem())
					.lore("")
					.lore("&7Click to obtain item")
					.lore("&7Shift+Click to obtain item with name");

			items.add(ClickableItem.from(item.build(), e ->
					PlayerUtils.giveItem(player, isShiftClick(e) ? model.getDisplayItem() : model.getItem())));
		}

		addPagination(player, contents, items);
	}

	static void load() {
		ResourcePack.setModelGroups(new ArrayList<>());
		ResourcePack.setFolders(new ArrayList<>());
		ResourcePack.setModels(new ArrayList<>());
		ResourcePack.setRootFolder(new CustomModelFolder("/"));

		CustomModelGroup.load();

		for (String path : getFolderPaths())
			addFoldersRecursively(path);
	}

	private static Set<String> getFolderPaths() {
		Set<String> paths = new HashSet<>();

		for (CustomModelGroup group : ResourcePack.getModelGroups())
			for (CustomModelGroup.Override override : group.getOverrides())
				paths.add(override.getFolderPath());

		return new TreeSet<>(paths);
	}

	private static void addFoldersRecursively(String path) {
		String[] folders = path.split("/");

		String walk = "";
		for (String folder : folders) {
			if (folder.isEmpty() || folder.equals("/"))
				continue;

			String parent = walk;
			walk += "/" + folder;
			addFolder(walk, folder, parent);
		}
	}

	private static void addFolder(String walk, String folder, String parent) {
		CustomModelFolder existing = ResourcePack.getRootFolder().getFolder(walk);
		if (existing == null)
			if (parent.isEmpty())
				ResourcePack.getRootFolder().addFolder(folder);
			else
				ResourcePack.getRootFolder().getFolder(parent).addFolder(folder);
	}


}
