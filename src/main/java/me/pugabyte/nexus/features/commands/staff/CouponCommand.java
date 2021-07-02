package me.pugabyte.nexus.features.commands.staff;

import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.util.player.UserManager;
import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.events.y2020.pugmas20.Pugmas20;
import me.pugabyte.nexus.features.mcmmo.menus.McMMOResetProvider;
import me.pugabyte.nexus.features.menus.MenuUtils;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Aliases;
import me.pugabyte.nexus.framework.commands.models.annotations.Arg;
import me.pugabyte.nexus.framework.commands.models.annotations.ConverterFor;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.annotations.TabCompleterFor;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.banker.BankerService;
import me.pugabyte.nexus.models.banker.Transaction.TransactionCause;
import me.pugabyte.nexus.models.coupon.CouponService;
import me.pugabyte.nexus.models.coupon.Coupons;
import me.pugabyte.nexus.models.coupon.Coupons.Coupon;
import me.pugabyte.nexus.models.eventuser.EventUser;
import me.pugabyte.nexus.models.eventuser.EventUserService;
import me.pugabyte.nexus.models.shop.Shop.ShopGroup;
import me.pugabyte.nexus.models.vote.Voter;
import me.pugabyte.nexus.utils.ItemBuilder;
import me.pugabyte.nexus.utils.ItemUtils;
import me.pugabyte.nexus.utils.JsonBuilder;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.StringUtils;
import me.pugabyte.nexus.utils.Utils.ActionGroup;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@NoArgsConstructor
@Aliases("coupons")
@Permission("group.admin")
public class CouponCommand extends CustomCommand implements Listener {
	private final CouponService service = new CouponService();
	private final Coupons coupons = service.get0();

	@Getter
	@AllArgsConstructor
	public enum CouponEvent {
		ECO(false) {
			@Override
			void use(PlayerInteractEvent event) {
				int amount = extractValue(event.getItem());
				new BankerService().deposit(event.getPlayer(), amount, ShopGroup.of(event.getPlayer()), TransactionCause.COUPON);
				ItemStack item = event.getItem();
				item.setAmount(item.getAmount() - 1);
				PlayerUtils.send(event.getPlayer(), "&e$" + amount + " &3has been added to your account");
			}
		},
		VPS(false) {
			@Override
			void use(PlayerInteractEvent event) {
				int amount = extractValue(event.getItem());
				new Voter(event.getPlayer()).givePoints(amount);
				PlayerUtils.send(event.getPlayer(), "&3You have been given &e" + amount + "&3 vote points");
				ItemStack item = event.getItem();
				item.setAmount(item.getAmount() - 1);
			}
		},
		MCMMO(false) {
			@Override
			void use(PlayerInteractEvent event) {
				int amount = extractValue(event.getItem());
				SmartInventory.builder()
						.title("Select McMMO Skill")
						.provider(new McMMOLevelCouponProvider(amount))
						.size(6, 9)
						.build().open(event.getPlayer());
			}
		},
		EVENT_TOKENS(false) {
			@Override
			void use(PlayerInteractEvent event) {
				int amount = extractValue(event.getItem());
				EventUserService eventUserService = new EventUserService();
				EventUser user = eventUserService.get(event.getPlayer());
				user.giveTokens(amount);
				eventUserService.save(user);
				ItemStack item = event.getItem();
				item.setAmount(item.getAmount() - 1);
			}
		},
		PUGMAS20_ADVENT_PAINTING(false) {
			@Override
			void use(PlayerInteractEvent event) {
				PlayerUtils.send(event.getPlayer(), Pugmas20.PREFIX + "This coupon will be claimable at the end of the month");
			}
		},
		PUGMAS20_ADVENT_SONG(false) {
			@Override
			void use(PlayerInteractEvent event) {
				PlayerUtils.send(event.getPlayer(), Pugmas20.PREFIX + "This coupon will be claimable at the end of the month");
			}
		},
		PUGMAS20_ADVENT_5000(true) {
			@Override
			void use(PlayerInteractEvent event) {
				new BankerService().deposit(event.getPlayer(), 5000, ShopGroup.of(event.getPlayer()), TransactionCause.COUPON);
			}
		},
		PUGMAS20_ITEM_PAINTING(false) {
			@Override
			void use(PlayerInteractEvent event) {
				PlayerUtils.send(event.getPlayer(), Pugmas20.PREFIX + "This coupon will be claimable at the end of the month");
			}
		},
		PUGMAS20_ITEM_SONG(false) {
			@Override
			void use(PlayerInteractEvent event) {
				PlayerUtils.send(event.getPlayer(), Pugmas20.PREFIX + "This coupon will be claimable at the end of the month");
			}
		},
		PUGMAS20_100_EVENT_TOKENS(true) {
			@Override
			void use(PlayerInteractEvent event) {
				EventUserService eventUserService = new EventUserService();
				EventUser user = eventUserService.get(event.getPlayer());
				user.giveTokens(100);
				eventUserService.save(user);
			}
		},
		EXPERIENCE_75(true) {
			@Override
			void use(PlayerInteractEvent event) {
				event.getPlayer().giveExpLevels(75);
			}
		};

		private final boolean autoremove;

		public void removeItem(Player player) {
			Coupon coupon = getCoupon();
			if (coupon == null) {
				Nexus.log("Tried to remove coupon item " + name().toLowerCase() + " from " + player.getName() + " which does not exist");
				return;
			}

			player.getInventory().removeItem(coupon.getItem());
		}

		public Coupons getCoupons() {
			final CouponService service = new CouponService();
			return service.get0();
		}

		public Coupon getCoupon() {
			return getCoupons().of(name());
		}

		public void handle(PlayerInteractEvent event) {
			use(event);
			if (autoremove) {
				getCoupon().use();
				new CouponService().save(getCoupons());
				removeItem(event.getPlayer());
			}
		}

		abstract void use(PlayerInteractEvent event);

		public static CouponEvent of(String id) {
			try {
				return valueOf(id.toUpperCase());
			} catch (IllegalArgumentException ex) {
				return null;
			}
		}

		public int extractValue(ItemStack item) {
			List<String> lore = item.getLore();
			return Integer.parseInt(lore.get(0).split(StringUtils.colorize(": &e"))[1]);
		}


	}

	public CouponCommand(CommandEvent event) {
		super(event);
	}

	@Path("list [page]")
	void list(@Arg("1") int page) {
		if (coupons.getCoupons().isEmpty())
			error("No coupons have been created");

		send(PREFIX + "Created coupons:");

		BiFunction<Coupon, String, JsonBuilder> json = (coupon, index) -> json()
				.next(" &e" + coupon.getId() + " &7- " + coupon.getUses())
				.command("/coupons get " + coupon.getId())
				.hover(coupon.getItem());

		paginate(coupons.getCoupons(), json, "/coupon list", page);
	}

	@Path("save <id>")
	void save(@Arg(tabCompleter = Coupon.class, regex = "^[a-zA-Z0-9_]+$") String id) {
		id = id.toLowerCase();
		if (coupons.of(id) != null)
			error("Coupon &e" + id + " &calready exists, use /coupon update <id>");

		Coupon coupon = new Coupon(id, getToolRequired());
		coupons.getCoupons().add(coupon);
		service.save(coupons);
		send(json(PREFIX + "Coupon &e" + id + " &3created").hover("Add your coupon logic to the code\nClick to copy the coupon's ID").copy(id));
	}

	@Path("update <id>")
	void update(Coupon coupon) {
		coupons.getCoupons().remove(coupon);
		save(coupon.getId());
	}

	@Path("delete <id>")
	void delete(Coupon coupon) {
		coupons.getCoupons().remove(coupon);
		service.save(coupons);
		send(json(PREFIX + "Coupon &e" + coupon.getId() + " &3deleted"));
	}

	@Path("get <id>")
	void get(Coupon coupon) {
		giveItem(coupon.getItem());
		send(PREFIX + "Giving coupon &e" + coupon.getId() + " &3(" + coupon.getUses() + " uses)");
	}

	@Path("get (eco|vps|mcmmo|event_tokens) <amount>")
	void generic(Integer amount) {
		String type = arg(2);
		ItemStack coupon = getGenericCoupon(type, amount);
		inventory().addItem(coupon);
	}

	public static ItemStack getGenericCoupon(String type, Integer amount) {
		CouponService service = new CouponService();
		Coupons coupons = service.get0();

		Coupon coupon = coupons.of(type);
		ItemStack itemStack = coupon.getItem().clone();
		ItemMeta meta = itemStack.getItemMeta();
		List<String> lore = meta.getLore();
		lore.set(0, StringUtils.colorize("&3Amount: &e" + amount));
		meta.setLore(lore);
		itemStack.setItemMeta(meta);

		return itemStack;
	}

	@ConverterFor(Coupon.class)
	Coupon convertToCoupon(String value) {
		Coupon coupon = coupons.of(value);
		if (coupon == null)
			error("Coupon from ID &e" + value + " &cnot found");
		return coupon;
	}

	@TabCompleterFor(Coupon.class)
	List<String> tabCompleteCoupon(String filter) {
		return coupons.getCoupons().stream()
				.map(Coupon::getId)
				.filter(id -> id.startsWith(filter.toLowerCase()))
				.collect(Collectors.toList());
	}

	@EventHandler
	public void onClick(PlayerInteractEvent event) {
		if (!ActionGroup.CLICK.applies(event)) return;
		if (ItemUtils.isNullOrAir(event.getItem())) return;

		final CouponService service = new CouponService();
		final Coupons coupons = service.get0();

		Coupon coupon = coupons.of(event.getItem());
		if (coupon == null) return;

		CouponEvent couponEvent = CouponEvent.of(coupon.getId());
		if (couponEvent != null)
			couponEvent.handle(event);
	}

	public static class McMMOLevelCouponProvider extends MenuUtils implements InventoryProvider {

		int levels;

		public McMMOLevelCouponProvider(int levels) {
			this.levels = levels;
		}

		@Override
		public void init(Player player, InventoryContents contents) {
			ItemStack coupon = player.getInventory().getItemInMainHand();
			coupon.setAmount(1);
			McMMOPlayer mcmmoPlayer = UserManager.getPlayer(player);
			for (McMMOResetProvider.ResetSkillType skill : McMMOResetProvider.ResetSkillType.values()) {
				ItemStack item = new ItemBuilder(skill.getMaterial()).name("&e" + StringUtils.camelCase(skill.name()))
						.lore("&3Level: &e" + mcmmoPlayer.getSkillLevel(PrimarySkillType.valueOf(skill.name()))).build();
				contents.set(skill.getRow(), skill.getColumn(), ClickableItem.from(item, e -> {
					int mcMMOLevel = mcmmoPlayer.getSkillLevel(PrimarySkillType.valueOf(skill.name()));
					if (mcMMOLevel >= 100) return;
					levels = Math.min(100 - mcMMOLevel, levels);
					PlayerUtils.runCommandAsConsole("addlevels " + player.getName() + " " + skill.name().toLowerCase() + " " + levels);
					player.getInventory().removeItem(coupon);
					player.closeInventory();
				}));
			}

		}
	}

}
