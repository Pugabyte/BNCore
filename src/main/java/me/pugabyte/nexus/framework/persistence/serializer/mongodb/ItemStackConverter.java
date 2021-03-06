package me.pugabyte.nexus.framework.persistence.serializer.mongodb;

import com.mongodb.BasicDBObject;
import dev.morphia.converters.SimpleValueConverter;
import dev.morphia.converters.TypeConverter;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import me.pugabyte.nexus.utils.SerializationUtils.JSON;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import static me.pugabyte.nexus.utils.SerializationUtils.JSON.serialize;

public class ItemStackConverter extends TypeConverter implements SimpleValueConverter {

	public ItemStackConverter(Mapper mapper) {
		super(ItemStack.class, CraftItemStack.class);
	}

	@Override
	public Object encode(Object value, MappedField optionalExtraInfo) {
		if (value == null) return null;

		return BasicDBObject.parse(JSON.toString(serialize((ItemStack) value)));
	}

	@Override
	public Object decode(Class<?> aClass, Object value, MappedField mappedField) {
		if (value == null) return null;
		return JSON.deserializeItemStack(((BasicDBObject) value).toJson());
	}

}
