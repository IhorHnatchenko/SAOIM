package org.example;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Optional integration smoke test. It is not executed by the normal test run.
 * Run it with: mvn -P integration-tests verify
 */
class DatabaseConnectionIT {
    @Test
    void connectsInitializesSchemaAndFindsRequiredTables() throws Exception {
        DatabaseManager.initDatabase();

        try (Connection connection = DatabaseManager.getConnection()) {
            assertTrue(connection.isValid(5), "MySQL connection must be valid");
            DatabaseMetaData metadata = connection.getMetaData();
            assertTrue(tableExists(metadata, "accounts"));
            assertTrue(tableExists(metadata, "profiles"));
            assertTrue(tableExists(metadata, "app_categories"));
            assertTrue(tableExists(metadata, "app_shortcuts"));
        }
    }

    private boolean tableExists(DatabaseMetaData metadata, String tableName) throws Exception {
        try (ResultSet tables = metadata.getTables(null, null, tableName, new String[]{"TABLE"})) {
            return tables.next();
        }
    }
}
