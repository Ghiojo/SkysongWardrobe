package org.skysongdev.skysongwardrobe.storage.sql;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * The few statements MySQL and SQLite spell differently. Everything else the SQL stores send is
 * plain SQL both understand, so a new database needs only a new dialect.
 */
public interface SqlDialect {

    /** Inserts a row, or updates {@code values} of the row that already has these {@code keys}. */
    String upsert(String table, List<String> keys, List<String> values);

    /** Inserts a row unless one with the same key already exists, which is then left as it was. */
    String insertIfAbsent(String table, List<String> columns);

    /** The column type for a few megabytes of bytes, such as a serialized item. */
    String largeBlobType();

    /** {@code ?, ?, ?} for {@code count} values. */
    static String placeholders(int count) {
        return String.join(", ", Collections.nCopies(count, "?"));
    }

    static List<String> concat(List<String> first, List<String> second) {
        return Stream.concat(first.stream(), second.stream()).toList();
    }
}
