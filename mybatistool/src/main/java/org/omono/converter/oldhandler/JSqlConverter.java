package org.omono.converter.oldhandler;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.create.table.CreateTable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SQL converter using JSQLParser for additional type conversions
 */
public class JSqlConverter {

    private static final Set<String> TO_REMOVE = new HashSet<>(Arrays.asList("UNSIGNED", "ZEROFILL"));
    private static final Map<String, Integer> CLEAR_MAP = new HashMap<>();

    static {
        CLEAR_MAP.put("CHARACTER", 3);
        CLEAR_MAP.put("COLLATE", 2);
        CLEAR_MAP.put("COMMENT", 2);
        CLEAR_MAP.put("AUTO_INCREMENT", 1);
    }

    /**
     * Convert CREATE TABLE statement from MySQL to PostgreSQL format
     * @param sql the MySQL CREATE TABLE SQL
     * @return list of converted PostgreSQL SQL statements
     */
    public static List<String> convertCreate(String sql) throws JSQLParserException {
        Statements statements = CCJSqlParserUtil.parseStatements(sql);
        List<CreateTable> collect = statements.getStatements().stream()
            .filter(statement -> statement instanceof CreateTable)
            .map(CreateTable.class::cast)
            .collect(Collectors.toList());

        if (collect.isEmpty()) {
            throw new IllegalArgumentException("not create table !:" + sql);
        }

        collect.forEach(createTable -> {
            createTable.setTableOptionsStrings(Collections.emptyList());
            setTableName(createTable.getTable());

            // Handle indexes (may be null)
            if (createTable.getIndexes() != null) {
                createTable.getIndexes().forEach(index ->
                    index.setColumnsNames(index.getColumnsNames().stream()
                        .map(ColumnNameHandler::convert)
                        .collect(Collectors.toList()))
                );
            }

            createTable.getColumnDefinitions().forEach(columnDefinition -> {
                columnDefinition.setColumnName(ColumnNameHandler.convert(columnDefinition.getColumnName()));
                ColDataType colDataType = columnDefinition.getColDataType();
                String dataType = colDataType.getDataType().toUpperCase();

                // Convert integer types
                if (dataType.contains("INT")) {
                    colDataType.setArgumentsStringList(null);
                    if (dataType.startsWith("BIGINT")) {
                        dataType = "BIGINT";
                    } else if (dataType.startsWith("INT")) {
                        dataType = "INTEGER";
                    } else {
                        dataType = "SMALLINT";
                    }
                } else if (dataType.contains("TEXT")) {
                    // Convert text types
                    colDataType.setArgumentsStringList(null);
                    dataType = "TEXT";
                }

                // Process column specs
                List<String> spec = columnDefinition.getColumnSpecs() == null ? Collections.emptyList() :
                    columnDefinition.getColumnSpecs().stream()
                        .map(String::toUpperCase)
                        .filter(str -> !TO_REMOVE.contains(str))
                        .collect(Collectors.toList());

                // Handle AUTO_INCREMENT
                boolean autoIncrement = spec.stream().anyMatch("AUTO_INCREMENT"::equals);
                if (autoIncrement) {
                    dataType = "INTEGER".equals(dataType) ? "SERIAL" : "BIGSERIAL";
                }
                colDataType.setDataType(dataType);

                // Clear MySQL-specific specs
                if (spec.stream().anyMatch(CLEAR_MAP.keySet()::contains)) {
                    List<String> newSpec = new ArrayList<>(spec.size());
                    for (int i = 0; i < spec.size(); i++) {
                        String current = spec.get(i);
                        Integer skipCount = CLEAR_MAP.get(current);
                        if (null == skipCount) {
                            newSpec.add(current);
                        } else {
                            i += skipCount - 1;
                        }
                    }
                    spec = newSpec;
                }
                columnDefinition.setColumnSpecs(spec.isEmpty() ? null : spec);
            });
        });

        return collect.stream().map(CreateTable::toString).collect(Collectors.toList());
    }

    private static void setTableName(net.sf.jsqlparser.schema.Table table) {
        table.setName(Optional.ofNullable(table.getName())
            .map(TableNameHandler::convert)
            .orElse(null));
        if (table.getDatabase() != null) {
            table.getDatabase().setDatabaseName(Optional.ofNullable(table.getDatabase().getDatabaseName())
                .map(TableNameHandler::convert)
                .orElse(null));
        }
    }
}
