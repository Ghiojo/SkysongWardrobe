package org.skysongdev.skysongwardrobe.storage.sql;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MySQL and MariaDB. {@code VALUES(column)} is used in upserts because MariaDB doesn't know the
 * newer row-alias form; MySQL 8 still accepts it.
 */
public final class MySqlDialect implements SqlDialect {

    @Override
    public String upsert(String table, List<String> keys, List<String> values) {
        List<String> columns = SqlDialect.concat(keys, values);
        String updates = values.stream().map(column -> column + " = VALUES(" + column + ")")
                .collect(Collectors.joining(", "));
        return "INSERT INTO " + table + " (" + String.join(", ", columns) + ") VALUES ("
                + SqlDialect.placeholders(columns.size()) + ") ON DUPLICATE KEY UPDATE " + updates;
    }

    @Override
    public String insertIfAbsent(String table, List<String> columns) {
        return "INSERT IGNORE INTO " + table + " (" + String.join(", ", columns) + ") VALUES ("
                + SqlDialect.placeholders(columns.size()) + ")";
    }

    @Override
    public String largeBlobType() {
        return "MEDIUMBLOB";
    }
}
