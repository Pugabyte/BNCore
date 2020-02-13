package me.pugabyte.bncore.features.homes;

import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.variables.Variables;
import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import com.earth2me.essentials.UserMap;
import lombok.SneakyThrows;
import me.pugabyte.bncore.BNCore;
import me.pugabyte.bncore.framework.commands.models.CustomCommand;
import me.pugabyte.bncore.framework.commands.models.annotations.Path;
import me.pugabyte.bncore.framework.commands.models.annotations.Permission;
import me.pugabyte.bncore.framework.commands.models.events.CommandEvent;
import me.pugabyte.bncore.models.homes.Home;
import me.pugabyte.bncore.models.homes.HomeOwner;
import me.pugabyte.bncore.models.homes.HomeService;
import me.pugabyte.bncore.utils.Tasks;
import me.pugabyte.bncore.utils.Utils;
import net.ess3.api.InvalidWorldException;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import ru.tehkode.permissions.bukkit.PermissionsEx;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class HomesCommand extends CustomCommand {
	HomeService service = new HomeService();
	HomeOwner homeOwner;

	public HomesCommand(CommandEvent event) {
		super(event);
		homeOwner = service.get(player());
	}


	@Path("getHomeOwner")
	void getHomeOwner() {
		send("Home owner: " + homeOwner);
	}

	@Path("getHome <name>")
	void getHome(String name) {
		send("Home: " + homeOwner.getHome(name).get());
	}

	@SneakyThrows
	@Path("migrateperms")
	void migrateperms() {
		Tasks.async(() -> {
			long startTime = System.currentTimeMillis();
			send(PREFIX + "Starting migration");

			Essentials essentials = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
			UserMap userMap = essentials.getUserMap();
			AtomicInteger count = new AtomicInteger(0);

			userMap.getAllUniqueUsers().forEach(uuid -> {
				try {
					User user = userMap.getUser(uuid);
					if (user.getHomes() == null || user.getHomes().size() == 0)
						return;

					HomeOwner homeOwner = service.get(uuid);

					PermissionsEx.getUser(homeOwner.getUuid().toString()).addPermission("homes.limit." + homeOwner.getLegacyMaxHomes());
				} catch (Exception ex) {
					BNCore.log("Error migrating user " + Bukkit.getOfflinePlayer(uuid).getName());
					ex.printStackTrace();
				}

				count.getAndIncrement();
				if (count.get() % 100 == 0)
					send(PREFIX + "Migrated " + count.get() + " users...");
			});

			send(PREFIX + "Migrated " + count.get() + " users, took " + (System.currentTimeMillis() - startTime) + "ms");
			service.clearCache();
		});
	}

	@SneakyThrows
	@Path("migrate")
	void migrate() {
		Tasks.async(() -> {
			long startTime = System.currentTimeMillis();
			send(PREFIX + "Starting migration");

			Essentials essentials = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
			UserMap userMap = essentials.getUserMap();
			AtomicInteger count = new AtomicInteger(0);

//			Bukkit.getOnlinePlayers().stream().map(Player::getUniqueId).forEach(uuid -> {
			userMap.getAllUniqueUsers().forEach(uuid -> {
				try {
					User user = userMap.getUser(uuid);
					if (user.getHomes() == null || user.getHomes().size() == 0)
						return;

					HomeOwner homeOwner = service.get(uuid);
					homeOwner.getHomes().clear();

					Boolean autolock = (Boolean) getSkriptVariable("homes::" + uuid.toString() + "::autolock");
					if (autolock != null)
						homeOwner.setAutoLock(autolock);

					Map allowAll = (Map) getSkriptVariable("homes::" + uuid.toString() + "::allowAll::*");
					if (allowAll != null)
						allowAll.keySet().forEach(allowAllUuid ->
								homeOwner.getFullAccessList().add(UUID.fromString((String) allowAllUuid)));

					for (String homeName : user.getHomes()) {
						try {
							Home home = Home.builder()
									.uuid(uuid)
									.name(homeName)
									.location(user.getHome(homeName))
									.build();

							Boolean locked = (Boolean) getSkriptVariable("homes::" + uuid.toString() + "::locked::" + homeName);
							if (locked != null)
								home.setLocked(locked);

							Object item = getSkriptVariable("homes::" + uuid.toString() + "::item::" + homeName);
							if (item != null) {
								if (item instanceof ItemStack)
									home.setItem((ItemStack) item);
								else if (item instanceof ItemType)
									home.setItem(((ItemType) item).getRandom());
								else if (item instanceof String) {
									OfflinePlayer skullOwner = Bukkit.getOfflinePlayer(UUID.fromString((String) item));
									ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (byte) 3);
									SkullMeta meta = (SkullMeta) skull.getItemMeta();
									meta.setOwningPlayer(skullOwner);
									skull.setItemMeta(meta);
									home.setItem(skull);
								} else
									BNCore.log("Home " + homeOwner.getOfflinePlayer().getName() + " / " + homeName + " has unknown item of " + item);
							}

							Set<UUID> accessList = new HashSet<>();
							Map allowed = (Map) getSkriptVariable("homes::" + uuid.toString() + "::allowed::" + homeName + "::*");
							if (allowed != null)
								allowed.keySet().stream()
										.map(allowedUuid -> UUID.fromString((String) allowedUuid))
										.filter(allowedUuid -> !homeOwner.getFullAccessList().contains(allowedUuid))
										.forEach(allowedUuid -> accessList.add((UUID) allowedUuid));
							home.setAccessList(accessList);

							homeOwner.add(home);
						} catch (InvalidWorldException ignore) {}
					}

					service.save(homeOwner);
				} catch (Exception ex) {
					BNCore.log("Error migrating user " + Bukkit.getOfflinePlayer(uuid).getName());
					ex.printStackTrace();
				}

				count.getAndIncrement();
				if (count.get() % 100 == 0)
					send(PREFIX + "Migrated " + count.get() + " users...");
			});

			send(PREFIX + "Migrated " + count.get() + " users, took " + (System.currentTimeMillis() - startTime) + "ms");
			service.clearCache();
		});
	}

	private Object getSkriptVariable(String id) {
		return Variables.getVariable(id, null, false);
	}


	@Path
	void list() {
		send("Homes: " + homeOwner.getNames());
	}

	@Path("<player>")
	void list(Player player) {
		homeOwner = service.get(player);
		list();
	}

	@Path("edit [home]")
	void edit(Home home) {
		if (home == null)
			HomesMenu.edit(homeOwner);
		else
			HomesMenu.edit(home);
	}

	@Path("allowAll [player]")
	void allowAll(Player player) {
		if (player == null)
			HomesMenu.allowAll(homeOwner, (owner, response) -> {
				if (response[0].length() > 0)
					send(PREFIX + "&e" + Utils.getPlayer(response[0]).getName() + " &3has been granted access to your homes");
			});
		else {
			homeOwner.allowAll(player);
			new HomeService().save(homeOwner);
			send(PREFIX + "&e" + player.getName() + " &3has been granted access to your homes");
		}
	}

	@Path("removeAll [player]")
	void removeAll(Player player) {
		if (player == null)
			HomesMenu.removeAll(homeOwner, (owner, response) -> {
				if (response[0].length() > 0)
					send(PREFIX + "&e" + Utils.getPlayer(response[0]).getName() + " &3no longer has access to your homes");
			});
		else {
			homeOwner.removeAll(player);
			new HomeService().save(homeOwner);
			send(PREFIX + "&e" + player.getName() + " &3no longer has access to your homes");
		}
	}

	@Path("allow <home> [player]")
	void allow(Home home, Player player) {
		if (player == null)
			HomesMenu.allow(home, (owner, response) -> {
				if (response[0].length() > 0)
					send(PREFIX + "&e" + Utils.getPlayer(response[0]).getName() + " &3has been granted access to your home &e" + home.getName());
			});
		else {
			home.allow(player);
			new HomeService().save(homeOwner);
			send(PREFIX + "&e" + player.getName() + " &3has been granted access to your home &e" + home.getName());
		}
	}

	@Path("remove <home> [player]")
	void remove(Home home, Player player) {
		if (player == null)
			HomesMenu.remove(home, (owner, response) -> {
				if (response[0].length() > 0)
					send(PREFIX + "&e" + Utils.getPlayer(response[0]).getName() + " &3no longer has access to your home &e" + home.getName());
			});
		else {
			homeOwner.removeAll(player);
			new HomeService().save(homeOwner);
			send(PREFIX + "&e" + player.getName() + " &3no longer has access to your home &e" + home.getName());
		}
	}

	@Path("reload")
	@Permission("group.seniorstaff")
	void reload() {
		service.clearCache();
	}

}
