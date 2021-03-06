package me.pugabyte.nexus.features.recipes.functionals.birdhouses;

import lombok.NoArgsConstructor;
import me.pugabyte.nexus.features.recipes.functionals.birdhouses.Birdhouse.BirdhouseType;
import me.pugabyte.nexus.utils.ItemBuilder;
import me.pugabyte.nexus.utils.Tasks;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

import static me.pugabyte.nexus.utils.ItemUtils.isNullOrAir;

@NoArgsConstructor
public class BirdhouseListener implements Listener {

	@EventHandler
	public void onPlaceBirdhouse(PlayerInteractEntityEvent event) {
		if (!(event.getRightClicked() instanceof ItemFrame itemFrame))
			return;

		Tasks.wait(1, () -> {
			if (!itemFrame.isValid())
				return;

			final ItemStack item = itemFrame.getItem();

			if (isNullOrAir(item))
				return;

			if (item.getType() != Material.OAK_WOOD)
				return;

			final ItemBuilder itemBuilder = new ItemBuilder(item);
			int customModelData = itemBuilder.customModelData();
			BirdhouseType type = BirdhouseType.of(customModelData);
			if (type == null)
				return;

			customModelData = type.baseModel();

			final BlockFace face = itemFrame.getAttachedFace();

			if (face == BlockFace.UP)
				customModelData += 2;
			else if (face != BlockFace.DOWN)
				customModelData += 1;

			itemFrame.setSilent(true);
			itemFrame.setItem(itemBuilder.resetName().customModelData(customModelData).build());
			itemFrame.setSilent(false);
		});
	}

	@EventHandler
	public void onItemSpawn(ItemSpawnEvent event) {
		final ItemStack item = event.getEntity().getItemStack();
		if (isNullOrAir(item))
			return;

		if (item.getType() != Material.OAK_WOOD)
			return;

		final ItemBuilder itemBuilder = new ItemBuilder(item);
		int customModelData = itemBuilder.customModelData();
		BirdhouseType type = BirdhouseType.of(customModelData);
		if (type == null)
			return;

		event.getEntity().setItemStack(new ItemBuilder(type.getDisplayItem()).amount(item.getAmount()).build());
	}

}
