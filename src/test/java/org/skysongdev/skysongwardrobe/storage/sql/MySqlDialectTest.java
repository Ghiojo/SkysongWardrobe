package org.skysongdev.skysongwardrobe.storage.sql;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MySqlDialectTest extends SqlDialectContract {

    @Override
    SqlDatabase emptyDatabase() {
        return TestDatabases.mysql();
    }

    @Test
    void upsertUsesTheValuesFunctionMariaDbUnderstands() {
        assertEquals("INSERT INTO t (a, b, c) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE b = VALUES(b), c = VALUES(c)",
                new MySqlDialect().upsert("t", List.of("a"), List.of("b", "c")));
    }
}
