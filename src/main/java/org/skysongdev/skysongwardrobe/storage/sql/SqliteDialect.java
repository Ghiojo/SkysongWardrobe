package org.skysongdev.skysongwardrobe.storage.sql;

import java.util.List;
import java.util.stream.Collectors;

/** SQLite 3.24 or newer, which every Paper 1.21 server ships. */
public final class SqliteDialect implements SqlDialect {

    @Override
    public String upsert(String table, List<String> keys, List<String> values) {
        List<String> columns = SqlDialect.concat(keys, values);
        String updates = values.stream().map(column -> column + " = excluded." + column)
                .collect(Collectors.joining(", "));
        return "INSERT INTO " + table + " (" + String.join(", ", columns) + ") VALUES ("
                + SqlDialect.placeholders(columns.size()) + ") ON CONFLICT (" + String.join(", ", keys)
                + ") DO UPDATE SET " + updates;
    }

    @Override
    public String insertIfAbsent(String table, List<String> columns) {
        return "INSERT OR IGNORE INTO " + table + " (" + String.join(", ", columns) + ") VALUES ("
                + SqlDialect.placeholders(columns.size()) + ")";
    }

    @Override
    public String largeBlobType() {
        return "BLOB";
    }
}
