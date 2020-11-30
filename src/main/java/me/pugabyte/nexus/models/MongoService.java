package me.pugabyte.nexus.models;

import dev.morphia.Datastore;
import dev.morphia.query.UpdateException;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.framework.exceptions.BNException;
import me.pugabyte.nexus.framework.persistence.MongoDBDatabase;
import me.pugabyte.nexus.framework.persistence.MongoDBPersistence;
import me.pugabyte.nexus.models.pugmas20.Pugmas20Service;
import me.pugabyte.nexus.utils.PlayerUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static me.pugabyte.nexus.utils.StringUtils.isV4Uuid;

public abstract class MongoService extends DatabaseService {
	protected static Datastore database;
	protected static String _id = "_id";

	static {
		database = MongoDBPersistence.getConnection(MongoDBDatabase.BEARNATION);
		if (database != null)
			database.ensureIndexes();
	}

	public abstract <T> Map<UUID, T> getCache();

	public void clearCache() {
		getCache().clear();
	}

	public <T extends PlayerOwnedObject> void cache(T object) {
		getCache().put(object.getUuid(), object);
	}

	@Override
	@NotNull
	public <T> T get(UUID uuid) {
		if (this.getClass() == Pugmas20Service.class)
			log("Get " + PlayerUtils.getPlayer(uuid).getName() + " [" + uuid.toString() + "]");
//		if (isEnableCache())
			return (T) getCache(uuid);
//		else
//			return getNoCache(uuid);
	}

	@NotNull
	protected <T extends PlayerOwnedObject> T getCache(UUID uuid) {
		Validate.notNull(getPlayerClass(), "You must provide a player owned class or override get(UUID)");
		if (getCache().containsKey(uuid) && getCache().get(uuid) == null)
			getCache().remove(uuid);
		getCache().computeIfAbsent(uuid, $ -> getNoCache(uuid));
		return (T) getCache().get(uuid);
	}

	protected <T extends PlayerOwnedObject> T getNoCache(UUID uuid) {
		Object object = database.createQuery(getPlayerClass()).field(_id).equal(uuid).first();
		if (object == null) {
			if (this.getClass() == Pugmas20Service.class)
				log("Creating new object " + PlayerUtils.getPlayer(uuid).getName() + " [" + uuid.toString() + "]");
			object = createPlayerObject(uuid);
		}
		if (object == null)
			Nexus.log("New instance of " + getPlayerClass().getSimpleName() + " is null");
		return (T) object;
	}

	protected Object createPlayerObject(UUID uuid) {
		try {
			Constructor<? extends PlayerOwnedObject> constructor = getPlayerClass().getDeclaredConstructor(UUID.class);
			constructor.setAccessible(true);
			return constructor.newInstance(uuid);
		} catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException ex) {
			throw new BNException(this.getClass().getSimpleName() + " not implemented correctly");
		}
	}

	@Override
	public <T> List<T> getAll() {
		return (List<T>) database.createQuery(getPlayerClass()).find().toList();
	}

	@Override
	public <T> void saveSync(T object) {
		PlayerOwnedObject playerOwnedObject = (PlayerOwnedObject) object;
		if (!isV4Uuid(playerOwnedObject.getUuid()))
			return;

		try {
			database.merge(object);
		} catch (UpdateException doesntExistYet) {
			try {
				database.save(object);
			} catch (Exception ex2) {
				Nexus.log("Error saving " + object.getClass().getSimpleName() + ": " + object.toString());
				ex2.printStackTrace();
			}
		} catch (Exception ex3) {
			Nexus.log("Error updating " + object.getClass().getSimpleName() + ": " + object.toString());
			ex3.printStackTrace();
		}
	}

	@Override
	public <T> void deleteSync(T object) {
		PlayerOwnedObject playerOwnedObject = (PlayerOwnedObject) object;

		if (!isV4Uuid(playerOwnedObject.getUuid()))
			return;

		database.delete(object);
		getCache().remove(playerOwnedObject.getUuid());
	}

	@Override
	public void deleteAllSync() {
		database.getCollection(getPlayerClass()).drop();
		clearCache();
	}

	public void log(String name) {
		try {
			try {
				throw new BNException("Stacktrace");
			} catch (BNException ex) {
				StringWriter sw = new StringWriter();
				ex.printStackTrace(new PrintWriter(sw));
				Bukkit.isPrimaryThread();
				Nexus.fileLogSync("pugmas-db-debug", "[Primary thread: " + Bukkit.isPrimaryThread() + "] MongoDB Pugmas20 " + name + "\n" + sw.toString() + "\n");
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
