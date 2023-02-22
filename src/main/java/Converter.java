import com.alibaba.druid.DbType;
import com.alibaba.druid.pool.DruidDataSourceFactory;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLName;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLValuableExpr;
import com.alibaba.druid.sql.ast.expr.SQLValuesExpr;
import com.alibaba.druid.sql.ast.statement.SQLCharacterDataType;
import com.alibaba.druid.sql.ast.statement.SQLColumnDefinition;
import com.alibaba.druid.sql.ast.statement.SQLDropTableStatement;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlCreateTableStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlInsertStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlTableIndex;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlASTVisitorAdapter;
import com.alibaba.druid.sql.parser.SQLParserUtils;
import com.alibaba.druid.sql.parser.SQLStatementParser;
import com.alibaba.druid.sql.visitor.SQLEvalVisitor;
import net.sf.jsqlparser.JSQLParserException;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class Converter {
    public static void main(String[] args) throws Exception {
        /**
         * 如果遇到date类型字符串无法插入，可以尝试在url中添加如下参数
         *?stringtype=unspecified
          */
        if (args.length < 4) {
            System.out.println("java -jar xxx.jar url username password sqlfile");
            return;
        }
        Properties properties = new Properties();
        properties.put(DruidDataSourceFactory.PROP_URL, args[0]);
        properties.put(DruidDataSourceFactory.PROP_USERNAME, args[1]);
        properties.put(DruidDataSourceFactory.PROP_PASSWORD, args[2]);
//        properties.put(DruidDataSourceFactory.PROP_URL, "jdbc:postgresql://10.134.4.210:54321/serverrest");
//        properties.put(DruidDataSourceFactory.PROP_USERNAME, "gpadmin");
//        properties.put(DruidDataSourceFactory.PROP_PASSWORD, "greenplum@dptech");
        DataSource dataSource = DruidDataSourceFactory.createDataSource(properties);


        QueryRunner queryRunner = new QueryRunner(dataSource);
        byte[] allBytes = Files.readAllBytes(Paths.get(args[3]));
//        byte[] allBytes= Files.readAllBytes(Paths.get("/home/user/lib_update.sql"));
        String sql = new String(allBytes);
        List<SQLStatement> sqlStatements = SQLUtils.parseStatements(sql, DbType.mysql);
        List<SQLDropTableStatement> dropList = new ArrayList<>();
        List<MySqlCreateTableStatement> createTableList = new ArrayList<>();
        List<MySqlInsertStatement> insertList = new ArrayList<>();
        for (SQLStatement sqlStatement : sqlStatements) {
            if (sqlStatement instanceof SQLDropTableStatement) {
                dropList.add((SQLDropTableStatement) sqlStatement);
            } else if (sqlStatement instanceof MySqlCreateTableStatement) {
                createTableList.add((MySqlCreateTableStatement) sqlStatement);
            } else if (sqlStatement instanceof MySqlInsertStatement) {
                insertList.add((MySqlInsertStatement) sqlStatement);
            }
        }

        dropList.forEach(drop->drop.accept(new MySqlASTVisitorAdapter(){
            @Override
            public void endVisit(SQLDropTableStatement x) {
                super.endVisit(x);
                x.getTableSources().forEach(sqlExprTableSource -> sqlExprTableSource.setSimpleName(sqlExprTableSource.getTableName().replace("`", "\"")));
            }
        }));
        for (SQLDropTableStatement sqlDropTableStatement : dropList) {
            String dropStatement = sqlDropTableStatement.toString();
            System.out.println("drop statement: " + dropStatement);
            queryRunner.execute(dropStatement);
        }
        createTableList.forEach(create->create.accept(new MySqlASTVisitorAdapter(){
            @Override
            public void endVisit(MySqlCreateTableStatement x) {
                super.endVisit(x);
                x.getTableOptions().clear();
                x.getTableElementList().removeIf(sqlTableElement -> sqlTableElement instanceof MySqlTableIndex);
                x.getMysqlKeys().forEach(key->key.setIndexType(null));

                x.getTableElementList().stream().filter(SQLColumnDefinition.class::isInstance)
                        .map(SQLColumnDefinition.class::cast)
                        .map(SQLColumnDefinition::getDataType)
                        .filter(SQLCharacterDataType.class::isInstance)
                        .map(SQLCharacterDataType.class::cast).forEach(sqlCharacterDataType -> {
                            sqlCharacterDataType.setCharSetName(null);
                            sqlCharacterDataType.setCollate(null);
                        });
            }
        }));
        List<String> createCollect = createTableList.stream().map(MySqlCreateTableStatement::toString).flatMap(e -> {
            try {
                return JSqlConverter.convertCreate(e).stream();
            } catch (JSQLParserException ex) {
                throw new RuntimeException(ex);
            }
        }).collect(Collectors.toList());
        for (String create : createCollect) {
            System.out.println("create statement: " + create);
            queryRunner.execute(create);
        }
        int insertCount = 0;
        for (MySqlInsertStatement mysqlInsert : insertList) {
            List<SQLExpr> columns = mysqlInsert.getColumns();
            Integer valueNum = mysqlInsert.getValuesList().stream().findFirst().map(valuesClause -> valuesClause.getValues().size()).orElse(0);
            int realValueNum = columns.size();
            if (columns.size() != valueNum) {
                if (columns.isEmpty()) {
                    realValueNum = valueNum;
                } else {
                    System.out.println("insert sql param and filed not match");
                    continue;
                }
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("INSERT INTO ");
            SQLName tableName = mysqlInsert.getTableName();
            String simpleTableName = tableName.getSimpleName();
            stringBuilder.append(simpleTableName.replace("`", "\""));
            if (!columns.isEmpty()) {
                stringBuilder.append(" (")
                        .append(columns.stream().map(SQLIdentifierExpr.class::cast).map(SQLIdentifierExpr::getName).map(e -> e.replace("`", "\"")).collect(Collectors.joining(",")))
                        .append(") ");

            }
            stringBuilder.append(" VALUES (");
            for (int i = 0; i < realValueNum - 1; i++) {
                stringBuilder.append("?,");
            }
            stringBuilder.append("?)");
            String insertStatement = stringBuilder.toString();

            List<SQLInsertStatement.ValuesClause> valuesList = mysqlInsert.getValuesList();
            List<Object[]> collect = valuesList.stream().map(valuesClause -> valuesClause.getValues().stream().map(SQLValuableExpr.class::cast).map(SQLValuableExpr::getValue).toArray()).collect(Collectors.toList());
            collect.forEach(objects -> {
                for (int i = 0; i < objects.length; i++) {
                    if (objects[i] == SQLEvalVisitor.EVAL_VALUE_NULL) {
                        objects[i] = null;
                    }
                }
            });
            List<List<Object[]>> partition = ListUtils.partition(collect, 500);
            List<Object[][]> params = partition.stream().map(list -> list.toArray(new Object[0][])).collect(Collectors.toList());

            for (Object[][] param : params) {
                queryRunner.insertBatch(insertStatement, new ScalarHandler<>(), param);
                insertCount += param.length;
                System.out.println("insert progress: " + insertCount);
            }

        }
    }


}
