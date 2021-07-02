package me.pugabyte.nexus.features.events.y2020.halloween20;

import eden.utils.TimeUtils.Time;
import lombok.Getter;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.events.y2020.halloween20.models.ComboLockNumber;
import me.pugabyte.nexus.features.events.y2020.halloween20.models.QuestNPC;
import me.pugabyte.nexus.features.events.y2020.halloween20.models.QuestStage;
import me.pugabyte.nexus.features.events.y2020.halloween20.models.SoundButton;
import me.pugabyte.nexus.features.events.y2020.halloween20.quest.Gate;
import me.pugabyte.nexus.features.events.y2020.halloween20.quest.menus.Halloween20Menus;
import me.pugabyte.nexus.features.regionapi.events.player.PlayerEnteredRegionEvent;
import me.pugabyte.nexus.models.cooldown.CooldownService;
import me.pugabyte.nexus.models.halloween20.Halloween20Service;
import me.pugabyte.nexus.models.halloween20.Halloween20User;
import me.pugabyte.nexus.utils.LuckPermsUtils.PermissionChange;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.SoundBuilder;
import me.pugabyte.nexus.utils.StringUtils;
import me.pugabyte.nexus.utils.Tasks;
import me.pugabyte.nexus.utils.Utils;
import me.pugabyte.nexus.utils.Utils.ActionGroup;
import me.pugabyte.nexus.utils.WorldEditUtils;
import me.pugabyte.nexus.utils.WorldGuardUtils;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Halloween20 implements Listener {
	@Getter
	public static final String region = "halloween20";
	@Getter
	public static final String PREFIX = StringUtils.getPrefix("Halloween 2020");

	public Halloween20() {
		new LostPumpkins();
		new ShootingRange();
		Nexus.registerListener(this);
	}

	public static World getWorld() {
		return Bukkit.getWorld("safepvp");
	}

	public static WorldGuardUtils getWGUtils() {
		return new WorldGuardUtils(getWorld());
	}

	public static WorldEditUtils getWEUtils() {
		return new WorldEditUtils(getWorld());
	}

	public static void start(Player player) {
		player.teleport(new Location(Bukkit.getWorld("safepvp"), 297.50, 161.00, -2034.50, .00F, .00F));
		player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Time.SECOND.x(3), 1));
		player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, Time.SECOND.x(14), 1));
		player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 1f, 1f);
	}

	public static void sendInstructions(Player player) {
		PlayerUtils.send(player, "&7&oTo unlock the gate, you will need to find numbers that you can use to unlock it. " +
				"You can find these numbers laying around, or inside of special chests (ender chests) located anywhere from this level or above. " +
				"You will also find clues hidden around the map that will help you figure out the combination. " +
				"Try asking people in the city for help.");
	}

	// Talking NPCs Handler
	@EventHandler
	public void onNPCClick(NPCRightClickEvent event) {
		QuestNPC npc = QuestNPC.getByID(event.getNPC().getId());
		if (npc == null) return;
		if (!new CooldownService().check(event.getClicker(), "Halloween20_NPC", Time.SECOND.x(2)))
			return;
		npc.sendScript(event.getClicker());
	}

	// ComboLock Handler
	@EventHandler
	public void onInteract(PlayerInteractEvent event) {
		if (!Utils.ActionGroup.CLICK_BLOCK.applies(event)) return;
		if (!getWGUtils().getPlayersInRegion(region).contains(event.getPlayer())) return;
		if (event.getHand() != EquipmentSlot.HAND) return;
		ComboLockNumber number = ComboLockNumber.getByLocation(event.getClickedBlock().getLocation());
		if (number == null) return;
		event.setCancelled(true);
		Tasks.wait(1, () -> number.onFind(event.getPlayer()));
	}

	// Open Combo Lock
	@EventHandler
	public void onComboLockInteract(PlayerInteractEvent event) {
		if (!Utils.ActionGroup.CLICK_BLOCK.applies(event)) return;
		if (getWGUtils().isInRegion(event.getClickedBlock().getLocation(), region + "_combolock"))
			Halloween20Menus.openComboLock(event.getPlayer());
	}

	@EventHandler
	public void onEnterGateRegion(PlayerEnteredRegionEvent event) {
		if (!event.getRegion().getId().equalsIgnoreCase(region + "_gate_open")) return;
		Halloween20User user = new Halloween20Service().get(event.getPlayer());
		if (user.getCombinationStage() != QuestStage.Combination.NOT_STARTED) return;
		Gate gate = new Gate(event.getPlayer());
		gate.open();
		Tasks.wait(Time.SECOND.x(4), gate::teleportIn);
		Tasks.wait(Time.SECOND.x(5), gate::close);
		Tasks.wait(Time.SECOND.x(8), () -> PlayerUtils.send(event.getPlayer(), "&7&oYou enter the land of the dead, and the large gate shuts behind you. You try to open it, but it appears to be locked."));
		Tasks.wait(Time.SECOND.x(14), () -> QuestNPC.DIEGO.sendScript(event.getPlayer()));
		Tasks.wait(Time.SECOND.x(15) + 300, () -> sendInstructions(event.getPlayer()));
	}


	@EventHandler
	public void onButtonClick(PlayerInteractEvent event) {
		if (!ActionGroup.CLICK_BLOCK.applies(event)) return;
		if (event.getHand() != EquipmentSlot.HAND) return;
		SoundButton button = SoundButton.getByLocation(event.getClickedBlock().getLocation());
		if (button == null) return;

		new SoundBuilder(button.getSound()).receiver(event.getPlayer()).play();

		Halloween20Service service = new Halloween20Service();
		Halloween20User user = service.get(event.getPlayer());
		if (user.getFoundButtons().contains(button)) {
			if (new CooldownService().check(event.getPlayer(), "halloween20-button-alreadyfound", Time.SECOND.x(10)))
				user.sendMessage(PREFIX + "You've already found this button!");
			return;
		}

		user.getFoundButtons().add(button);
		service.save(user);

		user.sendMessage(PREFIX + "You have found a spooky button! &e(" + user.getFoundButtons().size() + "/" + SoundButton.values().length + ")");

		if (user.getFoundButtons().size() != SoundButton.values().length)
			return;

		PermissionChange.set().player(event.getPlayer()).permission("powder.powder.spookyscaryskeletons").run();
		user.sendMessage(PREFIX + "You have unlocked the Spooky Scary Skeletons song! &c/songs");
	}

}
