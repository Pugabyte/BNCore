package me.pugabyte.bearnation.server.features.commands.staff;

import lombok.NonNull;
import me.pugabyte.bearnation.api.framework.commands.models.CustomCommand;
import me.pugabyte.bearnation.api.framework.commands.models.annotations.Aliases;
import me.pugabyte.bearnation.api.framework.commands.models.annotations.Path;
import me.pugabyte.bearnation.api.framework.commands.models.annotations.Permission;
import me.pugabyte.bearnation.api.framework.commands.models.events.CommandEvent;
import org.bukkit.enchantments.Enchantment;

@Aliases("ench")
@Permission("group.seniorstaff")
public class EnchantCommand extends CustomCommand {

	public EnchantCommand(@NonNull CommandEvent event) {
		super(event);
	}

	@Path("<enchantment> [level]")
	void run(Enchantment enchantment, int level) {
		if (level < 1)
			remove(enchantment);
		else {
			getToolRequired().addUnsafeEnchantment(enchantment, level);
			send(PREFIX + "Added enchant " + camelCase(enchantment.getKey().getKey()) + " " + level);
		}
	}

	@Path("remove <enchantment>")
	void remove(Enchantment enchantment) {
		getToolRequired().removeEnchantment(enchantment);
		send(PREFIX + "Removed enchant " + camelCase(enchantment.getKey().getKey()));
	}

}
