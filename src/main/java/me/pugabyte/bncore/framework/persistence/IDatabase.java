package me.pugabyte.bncore.framework.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public interface IDatabase {
	default void close(ResultSet result) {
		try {
			if (result != null) result.close();
		} catch (Exception ignored) {}
	}

	default void close(PreparedStatement statement) {
		try {
			if (statement != null) statement.close();
		} catch (Exception ignored) {}
	}

	default void close(Connection connection) {
		try {
			if (connection != null) connection.close();
		} catch (Exception ignored) {}
	}

}
