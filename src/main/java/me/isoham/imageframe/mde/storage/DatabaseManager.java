package me.isoham.imageframe.mde.storage;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DatabaseManager {
    private final Connection connection;

    public DatabaseManager(JavaPlugin plugin) throws Exception {
        File dataFolder = plugin.getDataFolder();

        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File dbFile = new File(dataFolder, "database.db");

        String jdbcUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        connection = DriverManager.getConnection(jdbcUrl);

        configureSQLite();
        createTables();
    }

    private void configureSQLite() throws Exception {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL;");
            stmt.execute("PRAGMA synchronous=NORMAL;");
            stmt.execute("PRAGMA foreign_keys=ON;");
        }
    }

    private void createTables() throws Exception {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS urls (
                        hash TEXT PRIMARY KEY,
                        url TEXT NOT NULL,
                        status TEXT NOT NULL,
                        moderated_by TEXT,
                        created_at INTEGER NOT NULL
                    )
                    """);

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS requests (
                        id TEXT PRIMARY KEY,
                        player_uuid TEXT NOT NULL,
                        player_name TEXT NOT NULL,
                        command TEXT NOT NULL,
                        url TEXT NOT NULL,
                        hash TEXT NOT NULL,
                        status TEXT NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                    """);

            stmt.execute("""
                    CREATE INDEX IF NOT EXISTS idx_requests_player_status
                    ON requests(player_uuid, status)
                    """);
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() throws Exception {
        connection.close();
    }
}