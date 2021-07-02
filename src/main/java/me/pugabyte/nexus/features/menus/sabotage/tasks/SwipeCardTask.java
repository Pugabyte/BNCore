package me.pugabyte.nexus.features.menus.sabotage.tasks;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.SlotPos;
import lombok.Getter;
import me.pugabyte.nexus.features.minigames.managers.PlayerManager;
import me.pugabyte.nexus.features.minigames.models.mechanics.custom.sabotage.Task;
import me.pugabyte.nexus.utils.ItemBuilder;
import me.pugabyte.nexus.utils.SoundBuilder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class SwipeCardTask extends AbstractTaskMenu {
	public SwipeCardTask(Task task) {
		super(task);
	}
	private static final Supplier<ItemStack> KEY_CARD = () -> new ItemBuilder(Material.ICE).customModelData(903).name("Key Card").lore("Grab this key card and place it on the right").build();

	@Getter
	private final SmartInventory inventory = SmartInventory.builder()
			.title("Swipe Card")
			.provider(this)
			.size(3, 9)
			.build();

	@Override
	public void init(Player player, InventoryContents inventoryContents) {
		Runnable reset = () -> {
			player.setItemOnCursor(null);
			init(player, inventoryContents);
		};
		SlotPos cardPos = SlotPos.of(1, 1);
		inventoryContents.fill(ClickableItem.empty(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build()));

		AtomicReference<LocalDateTime> time = new AtomicReference<>();
		inventoryContents.set(cardPos, ClickableItem.from(KEY_CARD.get(), click -> {
			if (!isLeftClick(click) || !KEY_CARD.get().isSimilar(click.getItem()) || !(click.getPlayer().getItemOnCursor().getType() == Material.AIR || click.getPlayer().getItemOnCursor().isSimilar(KEY_CARD.get()))) {
				reset.run();
			} else {
				time.set(LocalDateTime.now());
				ClickableItem item = inventoryContents.get(cardPos).orElse(null);
				if (item != null)
					inventoryContents.set(cardPos, item.clone(new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE).name(" ").build()));
				click.getPlayer().setItemOnCursor(KEY_CARD.get());
			}
		}));
		SlotPos destination = SlotPos.of(1, 7);
		inventoryContents.set(destination, ClickableItem.from(new ItemBuilder(Material.GREEN_STAINED_GLASS_PANE).name(" ").build(), click -> {
			if (time.get() == null || !KEY_CARD.get().isSimilar(click.getPlayer().getItemOnCursor())) {
				reset.run();
				return;
			}
			Duration duration = Duration.between(time.get(), LocalDateTime.now());
			double sec = duration.getSeconds() + (duration.getNano() / 1000000000d);
			if (!(sec >= .9d && sec <= 1.2d)) {
				reset.run();
				String fmt = sec < .9d ? "fast" : "slow";
				inventoryContents.set(destination, inventoryContents.get(destination).get().clone(new ItemBuilder(Material.RED_STAINED_GLASS_PANE).name("Too "+fmt+", try again").build()));
				new SoundBuilder(Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO).receiver(player).category(SoundCategory.MASTER).volume(2f).play();
			} else {
				getTask().partCompleted(PlayerManager.get(player));
				inventory.close(player);
			}
		}));
	}
}
