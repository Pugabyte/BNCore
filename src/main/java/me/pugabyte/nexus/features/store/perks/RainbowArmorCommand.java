package me.pugabyte.nexus.features.store.perks;

import lombok.NoArgsConstructor;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchJoinEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.MinigamerQuitEvent;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Aliases;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.rainbowarmor.RainbowArmor;
import me.pugabyte.nexus.models.rainbowarmor.RainbowArmorService;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import static me.pugabyte.nexus.features.store.perks.RainbowArmorCommand.PERMISSION;
import static me.pugabyte.nexus.models.rainbowarmor.RainbowArmor.isLeatherArmor;

@NoArgsConstructor
@Permission(PERMISSION)
@Aliases({"rainbowarmour", "rba"})
public class RainbowArmorCommand extends CustomCommand implements Listener {
	public static final String PERMISSION = "rainbowarmor.use";
	private final RainbowArmorService service = new RainbowArmorService();
	private RainbowArmor rbaPlayer;

	public RainbowArmorCommand(CommandEvent event) {
		super(event);
		if (isPlayerCommandEvent())
			rbaPlayer = service.get(player());
	}

	@Path
	void toggle() {
		if (!rbaPlayer.canUse())
			error("You cannot use Rainbow Armor here");

		if (rbaPlayer.isEnabled()) {
			rbaPlayer.stopArmor();
			send("&cRainbow armor unequipped!");
			rbaPlayer.setEnabled(false);
		} else {
			rbaPlayer.startArmor();
			rbaPlayer.setEnabled(true);
			send("&cR&6a&ei&an&bb&5o&dw &earmor equipped!");
		}

		service.save(rbaPlayer);
	}

	// Remove color
	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player player))
			return;
		if (event.getSlotType() != InventoryType.SlotType.ARMOR)
			return;

		ItemStack item = event.getCurrentItem();
		if (player.getGameMode() != GameMode.SURVIVAL)
			return;

		RainbowArmor rbaPlayer = new RainbowArmorService().get(player);
		if (rbaPlayer.isEnabled())
			if (isLeatherArmor(item))
				rbaPlayer.removeColor(item);
	}

	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		RainbowArmor rbaPlayer = new RainbowArmorService().get(event.getEntity());
		if (rbaPlayer.isEnabled())
			for (ItemStack itemStack : event.getDrops())
				if (isLeatherArmor(itemStack))
					rbaPlayer.removeColor(itemStack);
	}


	// Stop
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		stop(event.getPlayer());
	}

	@EventHandler
	public void onMatchJoin(MatchJoinEvent event) {
		stop(event.getMinigamer().getPlayer());
	}

	// Start
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		start(event.getPlayer());
	}

	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent event) {
		start(event.getPlayer());
	}

	@EventHandler
	public void onMatchQuit(MinigamerQuitEvent event) {
		start(event.getMinigamer().getPlayer());
	}

	private void start(Player player) {
		RainbowArmor rbaPlayer = new RainbowArmorService().get(player);
		if (rbaPlayer.isEnabled())
			rbaPlayer.startArmor();
	}

	private void stop(Player player) {
		new RainbowArmorService().get(player).stopArmor();
	}

}
