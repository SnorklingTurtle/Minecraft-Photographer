package com.snorklingturtle.photographer;

import com.snorklingturtle.photographer.util.ByteArrayCompression;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.UUID;

public class Storage {

    final static String tableName = "photos";

    public static Connection connect(Photographer plugin) {
        String folder = plugin.getDataFolder().getAbsolutePath();
        Connection conn = null;

        try
        {
            String url = String.format("jdbc:sqlite:%s/photos.db", folder) ;
            conn = DriverManager.getConnection(url);
        }
        catch (SQLException e)
        {
            plugin.getLogger().info(e.getMessage());
        }

        return conn;
    }

    public static void disconnect(Photographer plugin, Connection conn) {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException ex) {
            plugin.getLogger().info(ex.getMessage());
        }
    }

    public static void createTable(Photographer plugin, Connection conn) {
        String query = String.format( "CREATE TABLE IF NOT EXISTS %s (\n"
                + " id              INTEGER         PRIMARY KEY,\n"
                + " map_id          INTEGER         NOT NULL,\n"
                + " seed            INTEGER         NOT NULL,\n"
                + " counter         INTEGER         DEFAULT 1,\n"
                + " data            BLOB            NOT NULL,\n"
                + " photographer    TEXT,\n"
                + " tag             TEXT,\n"
                + " tagger          TEXT,\n"
                + " created         INTEGER         NOT NULL,\n"
                + " UNIQUE(map_id, seed) ON CONFLICT IGNORE,\n"
                + " UNIQUE(tag) ON CONFLICT IGNORE\n"
                + ");", Storage.tableName);

        try
        {
            PreparedStatement statement = conn.prepareStatement(query);
            statement.execute();
        }
        catch (SQLException e)
        {
            plugin.getLogger().info(e.getMessage());
        }
    }

    public static void createCleanUpTrigger(Photographer plugin, Connection conn) {
        String query = String.format("CREATE TRIGGER IF NOT EXISTS photo_cleanup\n" +
                "   AFTER UPDATE\n" +
                "   ON photos\n" +
                "   WHEN NEW.counter <= 0\n" +
                " BEGIN\n" +
                "   DELETE FROM %s WHERE id=NEW.id;\n" +
                " END;\n", Storage.tableName);

        try
        {
            PreparedStatement statement = conn.prepareStatement(query);
            statement.execute();
        }
        catch (SQLException e)
        {
            plugin.getLogger().info(e.getMessage());
        }
    }



    public static void store(Photographer plugin, Connection conn, int id, long seed, byte[] data, UUID photographer, int counter) {
        store(plugin, conn, id, seed, data, photographer, counter, null, null);
    }

    public static void store(Photographer plugin, Connection conn, int id, long seed, byte[] data, UUID photographer, int counter, String tag, UUID tagger) {
        String query = String.format("INSERT INTO %s (map_id, seed, data, created, tag, tagger, photographer, counter) VALUES(?,?,?,?,?,?,?,?);", Storage.tableName);

        try
        {
            PreparedStatement statement = conn.prepareStatement(query);
            statement.setInt(1, id);
            statement.setLong(2, seed);
            statement.setBytes(3, ByteArrayCompression.compress(data));
            statement.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            statement.setString(5, tag);
            statement.setString(6, tagger != null ? tagger.toString() : null);
            statement.setString(7, photographer != null ? photographer.toString() : null);
            statement.setInt(8, counter);
            statement.executeUpdate();
        }
        catch (SQLException e)
        {
            boolean isUniqueConstraintError = e.getErrorCode() == 19;
            if (!isUniqueConstraintError)
                plugin.getLogger().info(e.getErrorCode() + ": " + e.getMessage());
        }
    }

    public static ResultSet getAll(Photographer plugin, Connection conn) {
        String query = String.format("SELECT map_id,data FROM %s;", Storage.tableName);
        ResultSet rs = null;

        try
        {
            PreparedStatement statement = conn.prepareStatement(query);
            rs = statement.executeQuery();
        }
        catch (SQLException e)
        {
            plugin.getLogger().info(e.getMessage());
        }

        return rs;
    }

    public static ResultSet getBySeed(Photographer plugin, Connection conn, long seed) {
        String query = String.format("SELECT map_id,data FROM %s WHERE seed=?;", Storage.tableName);
        ResultSet rs = null;

        try
        {
            PreparedStatement statement = conn.prepareStatement(query);
            statement.setLong(1, seed);
            rs = statement.executeQuery();
        }
        catch (SQLException e)
        {
            plugin.getLogger().info(e.getMessage());
        }

        return rs;
    }

    public static void updateCounter(Photographer plugin, Connection conn, Integer map_id, long world_seed, int amount) {
        String query = String.format("UPDATE %s SET counter=(counter+?) WHERE map_id=? AND seed=?;", Storage.tableName);

        try
        {
            PreparedStatement statement = conn.prepareStatement(query);
            statement.setInt(1, amount);
            statement.setInt(2, map_id);
            statement.setLong(3, world_seed);
            statement.executeUpdate();
        }
        catch (SQLException e)
        {
            plugin.getLogger().info(e.getMessage());
        }
    }

    public static boolean updateTag(Photographer plugin, Connection conn, Integer map_id, long world_seed, String tag, UUID taggerUUID) {
        String query = String.format("UPDATE %s SET tag=?, tagger=? WHERE map_id=? AND seed=?;", Storage.tableName);

        try
        {
            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, tag != null ? tag.toLowerCase() : null);
            statement.setString(2, taggerUUID != null ? taggerUUID.toString() : null);
            statement.setInt(3, map_id);
            statement.setLong(4, world_seed);
            return statement.executeUpdate() > 0;
        }
        catch (SQLException e)
        {
            plugin.getLogger().info(e.getMessage());
        }
        return false;
    }

    public static ResultSet getById(Photographer plugin, Connection conn, Integer map_id) {
        String query = String.format("SELECT map_id,data,tag FROM %s WHERE map_id=? LIMIT 1;", Storage.tableName);
        ResultSet rs = null;

        try
        {
            PreparedStatement statement = conn.prepareStatement(query);
            statement.setInt(1, map_id);
            rs = statement.executeQuery();
        }
        catch (SQLException e)
        {
            plugin.getLogger().info(e.getMessage());
        }

        return rs;
    }

    public static ResultSet getByTag(Photographer plugin, Connection conn, String tag) {
        String query = String.format("SELECT map_id,data,tag,seed,photographer FROM %s WHERE tag=? LIMIT 1;", Storage.tableName);
        ResultSet rs = null;

        try
        {
            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, tag.toLowerCase());
            rs = statement.executeQuery();
        }
        catch (SQLException e)
        {
            plugin.getLogger().info(e.getMessage());
        }

        return rs;
    }

    public static ResultSet getTagsByPlayer(Photographer plugin, Connection conn, UUID playerUUID, int amount) {
        String query = String.format("SELECT tag FROM %s WHERE tagger=? ORDER BY RANDOM() LIMIT ?;", Storage.tableName);
        ResultSet rs = null;

        try
        {
            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, playerUUID.toString());
            statement.setInt(2, amount);
            rs = statement.executeQuery();
        }
        catch (SQLException e)
        {
            plugin.getLogger().info(e.getMessage());
        }

        return rs;
    }
}
