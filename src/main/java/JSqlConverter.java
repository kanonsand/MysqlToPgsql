import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.ItemsListVisitorAdapter;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.drop.Drop;
import net.sf.jsqlparser.statement.insert.Insert;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class JSqlConverter {


    static Set<String> TO_REMOVE = new HashSet<>(Arrays.asList("UNSIGNED", "ZEROFILL"));
    static Map<String, Integer> CLEAR_MAP = new HashMap<>();

    static {
        CLEAR_MAP.put("CHARACTER", 3);
        CLEAR_MAP.put("COLLATE", 2);
        CLEAR_MAP.put("COMMENT", 2);
        CLEAR_MAP.put("AUTO_INCREMENT", 1);
    }

    public static List<String> convertCreate(String sql) throws JSQLParserException {
        Statements statements = CCJSqlParserUtil.parseStatements(sql);
        List<CreateTable> collect = statements.getStatements().stream().filter(statement -> statement instanceof CreateTable).map(CreateTable.class::cast).collect(Collectors.toList());
        if (collect.isEmpty()) {
            throw new IllegalArgumentException("not create table !:" + sql);
        }
        collect.forEach(createTable -> {
            createTable.setTableOptionsStrings(Collections.emptyList());
            setTableName(createTable.getTable());
            createTable.getIndexes().forEach(index ->
                    index.setColumnsNames(index.getColumnsNames().stream()
                            .map(JSqlConverter::replaceBackQuote)
                            .collect(Collectors.toList())));
            createTable.getColumnDefinitions().forEach(columnDefinition -> {
                columnDefinition.setColumnName(columnDefinition.getColumnName().replace("`", "\""));
                ColDataType colDataType = columnDefinition.getColDataType();
                String dataType = colDataType.getDataType().toUpperCase();

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
                    colDataType.setArgumentsStringList(null);
                    dataType = "TEXT";
                }
                List<String> spec = columnDefinition.getColumnSpecs() == null ? Collections.emptyList() :
                        columnDefinition.getColumnSpecs().stream()
                                .map(String::toUpperCase).filter(str -> !TO_REMOVE.contains(str))
                                .collect(Collectors.toList());
                boolean autoIncrement = spec.stream().anyMatch("AUTO_INCREMENT"::equals);
                if (autoIncrement) {
                    dataType = "INTEGER".equals(dataType) ? "SERIAL" : "BIGSERIAL";
                }
                colDataType.setDataType(dataType);

                if (spec.stream().anyMatch(CLEAR_MAP.keySet()::contains)) {
                    List<String> newSpec = new ArrayList<>(spec.size());
                    for (int i = 0; i < spec.size(); i++) {
                        String current = spec.get(i);
                        Integer integer = CLEAR_MAP.get(current);
                        if (null == integer) {
                            newSpec.add(current);
                        } else {
                            i += integer - 1;
                        }
                    }
                    spec = newSpec;
                }
                columnDefinition.setColumnSpecs(spec.isEmpty() ? null : spec);
            });
        });
        return collect.stream().map(CreateTable::toString).collect(Collectors.toList());
    }

//    public SqlHolder parseSql(String sql) throws JSQLParserException {
//        List<Drop> dropStatement = new ArrayList<>();
//        List<CreateTable> createStatement = new ArrayList<>();
//        List<Insert> insertStatement = new ArrayList<>();
//        Statements statements = CCJSqlParserUtil.parseStatements(sql);
//
//        for (Statement statement : statements.getStatements()) {
//            if (statement instanceof Drop) {
//                dropStatement.add((Drop) statement);
//            } else if (statement instanceof CreateTable) {
//                createStatement.add((CreateTable) statement);
//            } else if (statement instanceof Insert) {
//                insertStatement.add((Insert) statement);
//            }
//        }
//        SqlHolder sqlHolder = new SqlHolder();
//        sqlHolder.setRawMysqlInsert(insertStatement.stream().map(Insert::toString).collect(Collectors.toList()));
//
//        dropStatement.stream().map(Drop::getName)
//                .forEach(JSqlConverter::setTableName);
//        sqlHolder.setDropList(dropStatement.stream().map(Drop::toString).collect(Collectors.toList()));
//
//        createStatement.forEach(createTable -> {
//            createTable.setTableOptionsStrings(Collections.emptyList());
//            JSqlConverter.setTableName(createTable.getTable());
//            createTable.getIndexes().forEach(index ->
//                    index.setColumnsNames(index.getColumnsNames().stream()
//                            .map(JSqlConverter::replaceBackQuote)
//                            .collect(Collectors.toList())));
//            createTable.getColumnDefinitions().forEach(columnDefinition -> {
//                columnDefinition.setColumnName(columnDefinition.getColumnName().replace("`", "\""));
//                ColDataType colDataType = columnDefinition.getColDataType();
//                String dataType = colDataType.getDataType().toUpperCase();
//
//                if (dataType.contains("INT")) {
//                    colDataType.setArgumentsStringList(null);
//                    if (dataType.startsWith("BIGINT")) {
//                        dataType = "BIGINT";
//                    } else if (dataType.startsWith("INT")) {
//                        dataType = "INTEGER";
//                    } else {
//                        dataType = "SMALLINT";
//                    }
//                } else if (dataType.contains("TEXT")) {
//                    colDataType.setArgumentsStringList(null);
//                    dataType = "TEXT";
//                }
//                List<String> spec = columnDefinition.getColumnSpecs() == null ? Collections.emptyList() :
//                        columnDefinition.getColumnSpecs().stream()
//                                .map(String::toUpperCase).filter(str -> !TO_REMOVE.contains(str))
//                                .collect(Collectors.toList());
//                boolean autoIncrement = spec.stream().anyMatch("AUTO_INCREMENT"::equals);
//                if (autoIncrement) {
//                    dataType = "INTEGER".equals(dataType) ? "SERIAL" : "BIGSERIAL";
//                }
//                colDataType.setDataType(dataType);
//
//                if (spec.stream().anyMatch(CLEAR_MAP.keySet()::contains)) {
//                    List<String> newSpec = new ArrayList<>(spec.size());
//                    for (int i = 0; i < spec.size(); i++) {
//                        String current = spec.get(i);
//                        Integer integer = CLEAR_MAP.get(current);
//                        if (null == integer) {
//                            newSpec.add(current);
//                        } else {
//                            i += integer - 1;
//                        }
//                    }
//                    spec = newSpec;
//                }
//                columnDefinition.setColumnSpecs(spec.isEmpty() ? null : spec);
//            });
//        });
//        sqlHolder.setCreateList(createStatement.stream().map(CreateTable::toString).collect(Collectors.toList()));
//        return sqlHolder;
//    }

//    /**
//     * @param sqlFilePath：表示mysql的sql脚本路径
//     * @param postgresqlModel：表示postgersql中的模式
//     */
//    public void generatePostgreSql(String sqlFilePath, String postgresqlModel) throws IOException, JSQLParserException {
//        List<Drop> dropStatement = new ArrayList<>();
//        List<CreateTable> createStatement = new ArrayList<>();
//        List<Insert> insertStatement = new ArrayList<>();
//        // MySQL DDL路径
//        String dDLs = new String(Files.readAllBytes(Paths.get(sqlFilePath)));
//        Statements statements = CCJSqlParserUtil.parseStatements(dDLs);
//
//        for (Statement s : statements.getStatements()) {
//            if (s instanceof Drop) {
//                dropStatement.add((Drop) s);
//            } else if (s instanceof CreateTable) {
//                createStatement.add((CreateTable) s);
//            } else if (s instanceof Insert) {
//                insertStatement.add((Insert) s);
//            }
//        }
//        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(Paths.get("/home/user/insert_Sql"))){
//            for (Insert insert : insertStatement) {
//                bufferedWriter.write(insert.toString());
//                bufferedWriter.newLine();
//            }
//        }
//
//        dropStatement.stream().map(Drop::getName)
//                .forEach(JSqlConverter::setTableName);
//
//        insertStatement.stream().map(Insert::getTable).forEach(JSqlConverter::setTableName);
//
//        insertStatement.stream().map(Insert::getColumns)
//                .flatMap(Collection::stream)
//                .forEach(column -> column.setColumnName(replaceBackQuote(column.getColumnName())));
//
//        insertStatement.stream()
//                .map(Insert::getItemsList)
//                .forEach(itemsList -> itemsList.accept(new ItemsListVisitorAdapter() {
//                    @Override
//                    public void visit(ExpressionList expressionList) {
//                        expressionList.accept(new ExpressionVisitorAdapter() {
//                            @Override
//                            public void visit(StringValue value) {
//                                if (value.getValue().contains("\\")) {
//                                    String replace = value.getValue()
//                                            .replace("'", "\\u0027")
//                                            .replace("\"", "\\u0022");
//                                    value.setValue(replace);
//                                }
//                            }
//                        });
//                    }
//                }));
//
//        createStatement.forEach(createTable -> {
//            createTable.setTableOptionsStrings(Collections.emptyList());
//            JSqlConverter.setTableName(createTable.getTable());
//            createTable.getIndexes().forEach(index ->
//                    index.setColumnsNames(index.getColumnsNames().stream()
//                            .map(JSqlConverter::replaceBackQuote)
//                            .collect(Collectors.toList())));
//            createTable.getColumnDefinitions().forEach(columnDefinition -> {
//                columnDefinition.setColumnName(columnDefinition.getColumnName().replace("`", "\""));
//                ColDataType colDataType = columnDefinition.getColDataType();
//                String dataType = colDataType.getDataType().toUpperCase();
//
//                if (dataType.contains("INT")) {
//                    colDataType.setArgumentsStringList(null);
//                    if (dataType.startsWith("BIGINT")) {
//                        dataType = "BIGINT";
//                    } else if (dataType.startsWith("INT")) {
//                        dataType = "INTEGER";
//                    } else {
//                        dataType = "SMALLINT";
//                    }
//                } else if (dataType.contains("TEXT")) {
//                    colDataType.setArgumentsStringList(null);
//                    dataType = "TEXT";
//                }
//                List<String> spec = columnDefinition.getColumnSpecs() == null ? Collections.emptyList() :
//                        columnDefinition.getColumnSpecs().stream()
//                                .map(String::toUpperCase).filter(str -> !TO_REMOVE.contains(str))
//                                .collect(Collectors.toList());
//                boolean autoIncrement = spec.stream().anyMatch("AUTO_INCREMENT"::equals);
//                if (autoIncrement) {
//                    dataType = "INTEGER".equals(dataType) ? "SERIAL" : "BIGSERIAL";
//                }
//                colDataType.setDataType(dataType);
//
//                if (spec.stream().anyMatch(CLEAR_MAP.keySet()::contains)) {
//                    List<String> newSpec = new ArrayList<>(spec.size());
//                    for (int i = 0; i < spec.size(); i++) {
//                        String current = spec.get(i);
//                        Integer integer = CLEAR_MAP.get(current);
//                        if (null == integer) {
//                            newSpec.add(current);
//                        } else {
//                            i += integer - 1;
//                        }
//                    }
//                    spec = newSpec;
//                }
//                columnDefinition.setColumnSpecs(spec.isEmpty() ? null : spec);
//            });
//        });
//        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("/home/user/drop_sql"))) {
//            for (Drop drop : dropStatement) {
//
//                writer.write(drop.toString());
//                writer.write(";");
//                writer.newLine();
//            }
//            for (CreateTable createTable : createStatement) {
//                writer.write(createTable.toString());
//                writer.write(";");
//                writer.newLine();
//            }
//            for (Insert insert : insertStatement) {
//                writer.write(insert.toString());
//                writer.write(";");
//                writer.newLine();
//            }
//        }
//    }

    private static void setTableName(Table table) {
        table.setName(Optional.ofNullable(table.getName()).map(JSqlConverter::replaceBackQuote).orElse(null));
        table.getDatabase().setDatabaseName(Optional.ofNullable(table.getDatabase().getDatabaseName()).map(JSqlConverter::replaceBackQuote).orElse(null));
    }

    private static String replaceBackQuote(String s) {
        return s.replace("`", "\"");
    }

}
