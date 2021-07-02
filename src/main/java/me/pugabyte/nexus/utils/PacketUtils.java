package me.pugabyte.nexus.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import me.lexikiq.HasPlayer;
import me.pugabyte.nexus.Nexus;
import net.minecraft.core.EnumDirection;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.protocol.game.PacketPlayOutEntity;
import net.minecraft.network.protocol.game.PacketPlayOutEntity.PacketPlayOutEntityLook;
import net.minecraft.network.protocol.game.PacketPlayOutEntity.PacketPlayOutRelEntityMove;
import net.minecraft.network.protocol.game.PacketPlayOutEntityDestroy;
import net.minecraft.network.protocol.game.PacketPlayOutEntityEquipment;
import net.minecraft.network.protocol.game.PacketPlayOutEntityHeadRotation;
import net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata;
import net.minecraft.network.protocol.game.PacketPlayOutEntityTeleport;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.decoration.EntityArmorStand;
import net.minecraft.world.entity.decoration.EntityItemFrame;
import net.minecraft.world.entity.item.EntityFallingBlock;
import net.minecraft.world.entity.monster.EntitySlime;
import net.minecraft.world.level.block.state.IBlockData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_17_R1.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftArmorStand;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftItemFrame;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@UtilityClass
public class PacketUtils {

	public static BlockPosition toBlockPosition(Location destination) {
		return new BlockPosition(destination.getBlockX(), destination.getBlockY(), destination.getBlockZ());
	}

	// Common

	public static void entityDestroy(@NonNull HasPlayer player, @NonNull Entity entity) {
		PacketPlayOutEntityDestroy destroyPacket = new PacketPlayOutEntityDestroy(entity.getId());
		sendPacket(player, destroyPacket);
	}

	public static void entityInvisible(@NonNull HasPlayer player, @NonNull org.bukkit.entity.Entity bukkitEntity, boolean invisible) {
		Entity entity = ((CraftEntity) bukkitEntity).getHandle();
		entity.setInvisible(invisible);
		PacketPlayOutEntityMetadata metadataPacket = new PacketPlayOutEntityMetadata(entity.getId(), entity.getDataWatcher(), true);
		sendPacket(player, metadataPacket);
	}

	public static void entityLook(@NonNull HasPlayer player, @NonNull org.bukkit.entity.Entity bukkitEntity, float yaw, float pitch) {
		Entity entity = ((CraftEntity) bukkitEntity).getHandle();
		PacketPlayOutEntityHeadRotation headRotationPacket = new PacketPlayOutEntityHeadRotation(entity, (byte) (yaw * 256 / 360));
		PacketPlayOutEntityLook lookPacket = new PacketPlayOutEntity.PacketPlayOutEntityLook(entity.getId(), (byte) (yaw * 256 / 360), (byte) (pitch * 256 / 360), true);
		sendPacket(player, headRotationPacket, lookPacket);
	}

	/**
	 * Gets the slot int corresponding to an {@link EnumItemSlot}. Returns -1 for {@link EnumItemSlot#a MAINHAND}.
	 * @param slot an item slot
	 * @return integer slot
	 * @deprecated enum fields are now obfuscated, use {@link #getSlotInt(EnumWrappers.ItemSlot)}
	 */
	@Deprecated
	public int getSlotInt(EnumItemSlot slot) {
		return switch (slot) {
			case a -> -1;
			case b -> 45;
			case c -> 8;
			case d -> 7;
			case e -> 6;
			case f -> 5;
		};
	}

	/**
	 * Gets the slot int corresponding to an {@link EnumWrappers.ItemSlot}. Returns -1 for {@link EnumWrappers.ItemSlot#MAINHAND MAINHAND}.
	 * @param slot an item slot
	 * @return integer slot
	 */
	public int getSlotInt(EnumWrappers.ItemSlot slot) {
		return switch (slot) {
			case MAINHAND -> -1;
			case OFFHAND -> 45;
			case FEET -> 8;
			case LEGS -> 7;
			case CHEST -> 6;
			case HEAD -> 5;
		};
	}

	/**
	 * Gets the {@link EnumItemSlot} corresponding to an {@link EnumWrappers.ItemSlot}.
	 * @param slot an item slot
	 * @return enum item slot
	 */
	public EnumItemSlot getEnumItemSlot(EnumWrappers.ItemSlot slot) {
		return EnumItemSlot.valueOf(slot.name());
	}

	/**
	 * Sends a fake packet for an armor piece or main/off-hand item for a player.
	 * <p>
	 * To avoid sending a packet to the item owner, remove them from <code>recipients</code>.
	 * @param owner player to "give" the item
	 * @param recipients packet recipients
	 * @param item item to "give"
	 * @param slot slot to "set"
	 * @deprecated enum names are now obfuscated, use {@link #sendFakeItem(org.bukkit.entity.Entity, HasPlayer, ItemStack, EnumWrappers.ItemSlot)}
	 */
	@Deprecated
	public void sendFakeItem(org.bukkit.entity.Entity owner, Collection<? extends HasPlayer> recipients, ItemStack item, EnumItemSlot slot) {
		// self packet avoids playing the armor equip sound effect
		PacketContainer selfPacket = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SET_SLOT);
		selfPacket.getIntegers().write(0, 0); // inventory ID (0 = player)
		int slotInt = getSlotInt(slot);
		if (slotInt == -1 && owner instanceof HumanEntity player)
			slotInt = player.getInventory().getHeldItemSlot() + 36;
		selfPacket.getIntegers().write(1, slotInt);
		selfPacket.getItemModifier().write(0, item);

		// other packet is sent to all other players to show the armor piece
		List<Pair<EnumItemSlot, net.minecraft.world.item.ItemStack>> equipmentList = new ArrayList<>();
		equipmentList.add(new Pair<>(slot, CraftItemStack.asNMSCopy(item)));
		PacketPlayOutEntityEquipment rawPacket = new PacketPlayOutEntityEquipment(owner.getEntityId(), equipmentList);
		PacketContainer otherPacket = PacketContainer.fromPacket(rawPacket);

		// send packets
		recipients.stream().filter(player -> owner.getWorld().equals(player.getPlayer().getWorld())).forEach(player -> {
			PacketContainer packet = player.getPlayer().getUniqueId().equals(owner.getUniqueId()) ? selfPacket : otherPacket;
			sendPacket(player, packet);
		});
	}

	/**
	 * Sends a fake packet for an armor piece or main/off-hand item for a player.
	 * <p>
	 * To avoid sending a packet to the item owner, remove them from <code>recipients</code>.
	 * @param owner player to "give" the item
	 * @param recipient packet recipient
	 * @param item item to "give"
	 * @param slot slot to "set"
	 * @deprecated enum names are now obfuscated, use {@link #sendFakeItem(org.bukkit.entity.Entity, HasPlayer, ItemStack, EnumWrappers.ItemSlot)}
	 */
	@Deprecated
	public void sendFakeItem(org.bukkit.entity.Entity owner, HasPlayer recipient, ItemStack item, EnumItemSlot slot) {
		sendFakeItem(owner, Collections.singletonList(recipient), item, slot);
	}

	/**
	 * Sends fake packets for an armor piece or main/off-hand item for a player.
	 * <p>
	 * To avoid sending a packet to the item owner, remove them from <code>recipients</code>.
	 * @param owner player to "give" the item
	 * @param recipients packet recipients
	 * @param item item to "give"
	 * @param slot slot to "set"
	 */
	public void sendFakeItem(org.bukkit.entity.Entity owner, Collection<? extends HasPlayer> recipients, ItemStack item, EnumWrappers.ItemSlot slot) {
		sendFakeItem(owner, recipients, item, getEnumItemSlot(slot));
	}

	/**
	 * Sends a fake packet for an armor piece or main/off-hand item for a player.
	 * <p>
	 * To avoid sending a packet to the item owner, remove them from <code>recipients</code>.
	 * @param owner player to "give" the item
	 * @param recipient packet recipient
	 * @param item item to "give"
	 * @param slot slot to "set"
	 */
	public void sendFakeItem(org.bukkit.entity.Entity owner, HasPlayer recipient, ItemStack item, EnumWrappers.ItemSlot slot) {
		sendFakeItem(owner, Collections.singletonList(recipient), item, slot);
	}


	// untested
	public static void entityRelativeMove(@NonNull HasPlayer player, @NonNull org.bukkit.entity.Entity bukkitEntity, Vector delta, boolean onGround) {
		Entity entity = ((CraftEntity) bukkitEntity).getHandle();
		PacketPlayOutRelEntityMove movePacket = new PacketPlayOutRelEntityMove(entity.getId(),
				encodePosition(delta.getX()), encodePosition(delta.getY()), encodePosition(delta.getZ()), onGround);

		sendPacket(player, movePacket);
	}

	public static void entityTeleport(@NonNull HasPlayer player, @NonNull org.bukkit.entity.Entity bukkitEntity, Location location, boolean onGround) {
		Entity entity = ((CraftEntity) bukkitEntity).getHandle();
		entity.setPositionRotation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
		entity.setOnGround(onGround);

		sendPacket(player, new PacketPlayOutEntityTeleport(entity));
		entityLook(player, bukkitEntity, location.getYaw(), location.getPitch());
	}

	// NPC


	// TODO: if possible
	public static void entityName(@NonNull HasPlayer player, org.bukkit.entity.NPC entity, String name) {
		EntityPlayer entityPlayer = ((CraftPlayer) entity).getHandle();
		GameProfile profile = new GameProfile(UUID.randomUUID(), name);
//		entityPlayer.setCustomName(new ChatComponentText(name));
//		PacketPlayOutEntityMetadata entityMetadataPacket = new PacketPlayOutEntityMetadata();

//		DataWatcher dataWatcher = entityPlayer.getDataWatcher();
//		dataWatcher.set(DataWatcherRegistry. );

//		entityMetadataPacket
	}

	public static List<EntityArmorStand> entityNameFake(@NonNull HasPlayer player, org.bukkit.entity.Entity bukkitEntity, String... customNames) {
		return entityNameFake(player, bukkitEntity, 0.3, customNames);
	}

	public static List<EntityArmorStand> entityNameFake(@NonNull HasPlayer player, org.bukkit.entity.Entity bukkitEntity, double distance, String... customNames) {
		int index = 0;
		List<EntityArmorStand> armorStands = new ArrayList<>();
		for (String customName : customNames)
			armorStands.add(entityNameFake(player, bukkitEntity, distance, customName, index++));

		if (armorStands.isEmpty())
			armorStands = null;

		return armorStands;
	}

	public static EntityArmorStand entityNameFake(@NonNull HasPlayer player, org.bukkit.entity.Entity bukkitEntity, String customName) {
		return entityNameFake(player, bukkitEntity, customName, 0);
	}

	public static EntityArmorStand entityNameFake(@NonNull HasPlayer player, org.bukkit.entity.Entity bukkitEntity, String customName, int index) {
		return entityNameFake(player, bukkitEntity, 0.3, customName, index);
	}

	public static EntityArmorStand entityNameFake(@NonNull HasPlayer player, org.bukkit.entity.Entity bukkitEntity, double distance, String customName, int index) {
		EntityPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
		EntityArmorStand armorStand = new EntityArmorStand(EntityTypes.c, nmsPlayer.getWorld());
		Location loc = bukkitEntity.getLocation();
		double y = (loc.getY() + 1.8) + (distance * index);

		armorStand.setLocation(loc.getX(), y, loc.getZ(), 0, 0);
		armorStand.setMarker(true);
		armorStand.setInvisible(true);
		armorStand.setBasePlate(true);
		armorStand.setSmall(true);
		if (customName != null) {
			armorStand.setCustomName(new ChatComponentText(StringUtils.colorize(customName)));
			armorStand.setCustomNameVisible(true);
		}

		PacketPlayOutSpawnEntity spawnArmorStand = new PacketPlayOutSpawnEntity(armorStand, getObjectId(armorStand));
		PacketPlayOutEntityMetadata rawMetadataPacket = new PacketPlayOutEntityMetadata(armorStand.getId(), armorStand.getDataWatcher(), true);
		PacketPlayOutEntityEquipment rawEquipmentPacket = new PacketPlayOutEntityEquipment(armorStand.getId(), getEquipmentList());

		sendPacket(player, spawnArmorStand, rawMetadataPacket, rawEquipmentPacket);
		return armorStand;
	}


	/*
	public void addNPCPacket(EntityPlayer npc, Player player) {
		PlayerConnection connection = ((CraftPlayer)player).getHandle().playerConnection;
		connection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, npc)); // "Adds the player data for the client to use when spawning a player" - https://wiki.vg/Protocol#Spawn_Player
		connection.sendPacket(new PacketPlayOutNamedEntitySpawn(npc)); // Spawns the NPC for the player client.
		connection.sendPacket(new PacketPlayOutEntityHeadRotation(npc, (byte) (npc.yaw * 256 / 360))); // Correct head rotation when spawned in player look direction.
	}
	 */

	// Slime
	public static EntitySlime spawnSlime(Player player, Location location, int size, boolean invisible, boolean glowing) {
		EntityPlayer nmsPlayer = ((CraftPlayer) player).getHandle();

		EntitySlime slime = new EntitySlime(EntityTypes.aD, nmsPlayer.getWorld());
		slime.setLocation(location.getBlockX(), location.getBlockY(), location.getBlockZ(), 0, 0);
		slime.setSize(size, true);
		slime.setInvisible(invisible);
		slime.getBukkitEntity().setGlowing(glowing);
		slime.setNoGravity(true);
		slime.setPersistent();

		PacketPlayOutSpawnEntity rawSpawnPacket = new PacketPlayOutSpawnEntity(slime, getObjectId(slime));
		PacketPlayOutEntityMetadata rawMetadataPacket = new PacketPlayOutEntityMetadata(slime.getId(), slime.getDataWatcher(), true);

		sendPacket(player, rawSpawnPacket, rawMetadataPacket);
		return slime;
	}

	// Falling Block
	// needs more testing, seemed to only spawn an iron ore block
	public static EntityFallingBlock spawnFallingBlock(@NonNull HasPlayer player, @NonNull Location location, Block block) {
		EntityPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
		IBlockData blockData = ((CraftBlockData) block.getBlockData()).getState();

		EntityFallingBlock fallingBlock = new EntityFallingBlock(nmsPlayer.getWorld(), location.getX(), location.getY(), location.getZ(), blockData);
		fallingBlock.setInvulnerable(true);
		fallingBlock.setNoGravity(true);
		fallingBlock.setInvisible(true);
		fallingBlock.getBukkitEntity().setGlowing(true);
		fallingBlock.getBukkitEntity().setVelocity(new Vector(0, 0, 0));

		PacketPlayOutSpawnEntity rawSpawnPacket = new PacketPlayOutSpawnEntity(fallingBlock, getObjectId(fallingBlock));
		PacketPlayOutEntityMetadata rawMetadataPacket = new PacketPlayOutEntityMetadata(fallingBlock.getId(), fallingBlock.getDataWatcher(), true);

		sendPacket(player, rawSpawnPacket, rawMetadataPacket);
		return fallingBlock;
	}

	// Item Frame
	public static EntityItemFrame spawnItemFrame(@NonNull HasPlayer player, @NonNull Location location, BlockFace blockFace, ItemStack content, int rotation, boolean makeSound, boolean invisible) {
		if (content == null) content = new ItemStack(Material.AIR);
		if (blockFace == null) blockFace = BlockFace.NORTH;

		EnumDirection direction = CraftBlock.blockFaceToNotch(blockFace);
		EntityPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
		EntityItemFrame itemFrame = new EntityItemFrame(EntityTypes.R, nmsPlayer.getWorld());
		itemFrame.setLocation(location.getBlockX(), location.getBlockY(), location.getBlockZ(), 0, 0);
		itemFrame.setItem(CraftItemStack.asNMSCopy(content), true, makeSound);
		itemFrame.setDirection(direction);
		itemFrame.setInvisible(invisible);
		itemFrame.setRotation(rotation);

		PacketPlayOutSpawnEntity rawSpawnPacket = new PacketPlayOutSpawnEntity(itemFrame, getObjectId(itemFrame));
		PacketPlayOutEntityMetadata rawMetadataPacket = new PacketPlayOutEntityMetadata(itemFrame.getId(), itemFrame.getDataWatcher(), true);

		sendPacket(player, rawSpawnPacket, rawMetadataPacket);
		return itemFrame;
	}

	public static void updateItemFrame(@NonNull HasPlayer player, @NonNull ItemFrame entity, ItemStack content, int rotation) {
		if (content == null) content = new ItemStack(Material.AIR);

		EntityItemFrame itemFrame = ((CraftItemFrame) entity).getHandle();

		DataWatcher dataWatcher = itemFrame.getDataWatcher();
		dataWatcher.set(DataWatcherRegistry.g.a(7), CraftItemStack.asNMSCopy(content));
		dataWatcher.set(DataWatcherRegistry.b.a(8), rotation != -1 ? rotation : itemFrame.getRotation());

		PacketPlayOutEntityMetadata rawMetadataPacket = new PacketPlayOutEntityMetadata(
				itemFrame.getId(), itemFrame.getDataWatcher(), true);

		sendPacket(player, rawMetadataPacket);
	}

	// Armor Stand -- TODO: Needs to be turned into a builder

	public static EntityArmorStand spawnArmorStand(HasPlayer player, Location location, boolean invisible) {
		return spawnArmorStand(player, location, invisible, null);
	}

	public static EntityArmorStand spawnArmorStand(HasPlayer player, Location location, boolean invisible, String customName) {
		return spawnArmorStand(player, location, null, invisible, customName);
	}

	public static EntityArmorStand spawnArmorStand(HasPlayer player, Location location,
												   List<Pair<EnumItemSlot, net.minecraft.world.item.ItemStack>> equipment,
												   boolean invisible) {
		return spawnArmorStand(player, location, equipment, invisible, null);
	}

	public static EntityArmorStand spawnArmorStand(HasPlayer player, Location location,
												   List<Pair<EnumItemSlot, net.minecraft.world.item.ItemStack>> equipment,
												   boolean invisible, String customName) {
		if (equipment == null) equipment = getEquipmentList();

		EntityPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
		EntityArmorStand armorStand = new EntityArmorStand(EntityTypes.c, nmsPlayer.getWorld());
		armorStand.setLocation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
		armorStand.setInvisible(invisible);
		if (customName != null) {
			armorStand.setCustomName(new ChatComponentText(customName));
			armorStand.setCustomNameVisible(true);
		}

		PacketPlayOutSpawnEntity rawSpawnPacket = new PacketPlayOutSpawnEntity(armorStand, getObjectId(armorStand));
		PacketPlayOutEntityMetadata rawMetadataPacket = new PacketPlayOutEntityMetadata(armorStand.getId(), armorStand.getDataWatcher(), true);
		PacketPlayOutEntityEquipment rawEquipmentPacket = new PacketPlayOutEntityEquipment(armorStand.getId(), equipment);

		sendPacket(player, rawSpawnPacket, rawMetadataPacket, rawEquipmentPacket);
		return armorStand;
	}

	public static EntityArmorStand spawnBeaconArmorStand(Player player, Location location) {
		EntityPlayer nmsPlayer = ((CraftPlayer) player).getHandle();

		EntityArmorStand armorStand = new EntityArmorStand(EntityTypes.c, nmsPlayer.getWorld());
		location = location.toCenterLocation();
		armorStand.setLocation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
		armorStand.setInvisible(true);
		armorStand.setBasePlate(true);
		armorStand.setSmall(true);
		armorStand.getBukkitEntity().setGlowing(true);

		PacketPlayOutSpawnEntity rawSpawnPacket = new PacketPlayOutSpawnEntity(armorStand, getObjectId(armorStand));
		PacketPlayOutEntityMetadata rawMetadataPacket = new PacketPlayOutEntityMetadata(armorStand.getId(), armorStand.getDataWatcher(), true);
		PacketPlayOutEntityEquipment rawEquipmentPacket = new PacketPlayOutEntityEquipment(armorStand.getId(), getEquipmentList());

		sendPacket(player, rawSpawnPacket, rawMetadataPacket, rawEquipmentPacket);

		return armorStand;
	}


	public static void updateArmorStandArmor(HasPlayer player, ArmorStand entity, List<Pair<EnumItemSlot, net.minecraft.world.item.ItemStack>> equipment) {
		if (equipment == null) equipment = getEquipmentList(null, null, null, null);
		EntityArmorStand armorStand = ((CraftArmorStand) entity).getHandle();

		PacketPlayOutEntityEquipment rawEquipmentPacket = new PacketPlayOutEntityEquipment(armorStand.getId(), equipment);

		sendPacket(player, rawEquipmentPacket);
	}


	public static List<Pair<EnumItemSlot, net.minecraft.world.item.ItemStack>> getEquipmentList() {
		return getEquipmentList(null, null, null, null);
	}

	public static List<Pair<EnumItemSlot, net.minecraft.world.item.ItemStack>> getEquipmentList(ItemStack head, ItemStack chest, ItemStack legs, ItemStack feet) {
		List<Pair<EnumItemSlot, net.minecraft.world.item.ItemStack>> equipmentList = new ArrayList<>();
		equipmentList.add(new Pair<>(EnumItemSlot.f, CraftItemStack.asNMSCopy(head)));
		equipmentList.add(new Pair<>(EnumItemSlot.e, CraftItemStack.asNMSCopy(chest)));
		equipmentList.add(new Pair<>(EnumItemSlot.d, CraftItemStack.asNMSCopy(legs)));
		equipmentList.add(new Pair<>(EnumItemSlot.c, CraftItemStack.asNMSCopy(feet)));
		return equipmentList;
	}

	//

	public static void sendPacket(HasPlayer player, Collection<?> packets) {
		packets.forEach(packet -> sendPacket(player, packet));
	}

	public static void sendPacket(HasPlayer player, Object... packets) {
		for (Object packet : packets) {
			if (packet instanceof PacketContainer container)
				sendPacket(player, container);
			else
				sendPacket(player, PacketContainer.fromPacket(packet));
		}
	}

	public static void sendPacket(HasPlayer player, PacketContainer... packets) {
		for (PacketContainer packet : packets) {
			try {
				ProtocolLibrary.getProtocolManager().sendServerPacket(player.getPlayer(), packet);
			} catch (InvocationTargetException e) {
				Nexus.log("Error trying to send " + packet + " packet to " + player.getPlayer().getName());
				e.printStackTrace();
			}
		}
	}

	public static byte encodeAngle(float angle) {
		return (byte) (angle * 256f / 360f);
	}

	private static int encodeVelocity(double v) {
		return (int) (v * 8000D);
	}

	private static short encodePosition(double d) {
		return (short) (d * 4096D);
	}

	public static Integer getObjectId(EntityType entity) {
		if (entity == null)
			return null;
		return Bukkit.getUnsafe().entityID(entity);
	}

	public static Integer getObjectId(org.bukkit.entity.Entity entity) {
		if (entity == null)
			return null;
		return getObjectId(entity.getType());
	}

	public static Integer getObjectId(Entity entity) {
		if (entity == null)
			return null;
		return getObjectId(entity.getBukkitEntity());
	}
}
