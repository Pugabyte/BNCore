package me.pugabyte.nexus;

import com.google.gson.GsonBuilder;
import dev.morphia.converters.TypeConverter;
import eden.EdenAPI;
import eden.mongodb.DatabaseConfig;
import eden.utils.Env;
import me.pugabyte.nexus.framework.persistence.serializer.mongodb.ChannelConverter;
import me.pugabyte.nexus.framework.persistence.serializer.mongodb.ChatColorConverter;
import me.pugabyte.nexus.framework.persistence.serializer.mongodb.ColorConverter;
import me.pugabyte.nexus.framework.persistence.serializer.mongodb.ItemStackConverter;
import me.pugabyte.nexus.framework.persistence.serializer.mongodb.LocationConverter;
import me.pugabyte.nexus.framework.persistence.serializer.mongodb.PrivateChannelConverter;
import me.pugabyte.nexus.framework.persistence.serializer.mongodb.PublicChannelConverter;
import me.pugabyte.nexus.framework.persistence.serializer.mongodb.QuestConverter;
import me.pugabyte.nexus.utils.SerializationUtils.JSON.LocationGsonSerializer;
import org.bukkit.Location;

import java.util.Collection;
import java.util.List;

public class API extends EdenAPI {

	public API() {
		instance = this;
	}

	@Override
	public Env getEnv() {
		return Nexus.getEnv();
	}

	@Override
	public DatabaseConfig getDatabaseConfig() {
		return DatabaseConfig.builder()
				.password(Nexus.getInstance().getConfig().getString("databases.mongodb.password"))
				.modelPath("me.pugabyte.nexus.models")
				.env(getEnv())
				.build();
	}

	@Override
	public ClassLoader getClassLoader() {
		return Nexus.getInstance().getClass().getClassLoader();
	}

	@Override
	public Collection<? extends Class<? extends TypeConverter>> getMongoConverters() {
		return List.of(
				ChannelConverter.class,
				ChatColorConverter.class,
				ColorConverter.class,
				ItemStackConverter.class,
				LocationConverter.class,
				QuestConverter.class,
				PrivateChannelConverter.class,
				PublicChannelConverter.class
		);
	}

	@Override
	public GsonBuilder getPrettyPrinter() {
		return super.getPrettyPrinter().registerTypeAdapter(Location.class, new LocationGsonSerializer());
	}

}
