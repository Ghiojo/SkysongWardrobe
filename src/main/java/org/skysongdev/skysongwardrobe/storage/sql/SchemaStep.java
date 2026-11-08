package org.skysongdev.skysongwardrobe.storage.sql;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * One numbered change to the tables, such as creating them or later adding a column. Steps are
 * never edited once released: a change is always a new step with the next number.
 *
 * @param version    1 for the first step, then 2, 3 and so on
 * @param statements the SQL to run, given the database so table names carry its prefix
 */
public record SchemaStep(int version, String description, Function<SqlDatabase, List<String>> statements) {

    public SchemaStep {
        if (version < 1) {
            throw new IllegalArgumentException("Schema versions start at 1");
        }
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(statements, "statements");
    }
}
