package me.pugabyte.nexus.framework.persistence.serializer.mysql;

import com.dieselpoint.norm.serialize.DbSerializable;
import me.pugabyte.nexus.utils.SerializationUtils.JSON;
import org.bukkit.Location;

public class LocationSerializer implements DbSerializable {

	@Override
	public String serialize(Object in) {
		return JSON.serializeLocation((Location) in);
	}

	@Override
	public Location deserialize(String in) {
		return JSON.deserializeLocation(in);
	}

}
