package me.pugabyte.nexus.models.pugmas20;

import dev.morphia.annotations.Converters;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.pugabyte.nexus.features.events.models.QuestStage;
import me.pugabyte.nexus.features.events.y2020.pugmas20.Pugmas20;
import me.pugabyte.nexus.features.events.y2020.pugmas20.models.QuestNPC;
import me.pugabyte.nexus.framework.persistence.serializer.mongodb.UUIDConverter;
import me.pugabyte.nexus.models.PlayerOwnedObject;
import me.pugabyte.nexus.utils.ItemUtils;
import me.pugabyte.nexus.utils.JsonBuilder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static me.pugabyte.nexus.features.events.y2020.pugmas20.Pugmas20.isAtPugmas;
import static me.pugabyte.nexus.utils.ItemUtils.isNullOrAir;

@Data
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
@Entity("pugmas20_user")
@Converters({UUIDConverter.class})
public class Pugmas20User extends PlayerOwnedObject {
	@Id
	@NonNull
	private UUID uuid;

	// Advent
	@Embedded
	private List<Integer> foundDays = new ArrayList<>();

	// Active Quest NPCs
	@Embedded
	private List<Integer> nextStepNPCs = Arrays.asList(QuestNPC.ELF2.getId(), QuestNPC.QA_ELF.getId(), QuestNPC.ELF3.getId());

	// Quest - Light The Tree
	private QuestStage giftGiverStage = QuestStage.NOT_STARTED;

	// Quest - Light The Tree
	private QuestStage lightTreeStage = QuestStage.NOT_STARTED;
	private int torchesLit = 0;

	// Quest - Toy Testing
	private QuestStage toyTestingStage = QuestStage.NOT_STARTED;
	private boolean masterMind = false;
	private boolean connectFour = false;
	private boolean ticTacToe = false;
	private boolean battleship = false;

	// Quest - Ornament Vendor
	private QuestStage ornamentVendorStage = QuestStage.NOT_STARTED;

	// Quest - The Mines
	private QuestStage minesStage = QuestStage.NOT_STARTED;

	@Embedded
	private List<ItemStack> inventory = new ArrayList<>();

	public void storeInventory() {
		if (!isOnline()) return;

		PlayerInventory playerInventory = getPlayer().getInventory();
		for (ItemStack item : playerInventory.getContents()) {
			if (isNullOrAir(item) || item.getLore() == null || item.getLore().isEmpty())
				continue;

			if (item.getLore().get(0).contains(Pugmas20.getQuestLore())) {
				playerInventory.remove(item);
				inventory.add(item);
			}
		}
	}

	public void applyInventory() {
		if (!isOnline()) return;
		if (!isAtPugmas(getPlayer())) return;
		if (this.inventory.isEmpty()) return;

		ArrayList<ItemStack> inventory = new ArrayList<>(this.inventory);
		this.inventory.clear();
		this.inventory.addAll(ItemUtils.giveItemsGetExcess(getPlayer(), inventory));

		if (this.inventory.isEmpty())
			send(Pugmas20.PREFIX + "Inventory applied");
		else
			send(new JsonBuilder(Pugmas20.PREFIX + "Could not give all event items, clear up some inventory space and click here or re-enter the world")
					.hover("Click to collect the rest of your event items")
					.command("/pugmas20 inventory apply"));

	}
}