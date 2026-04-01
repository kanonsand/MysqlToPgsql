package converter;

import com.alibaba.druid.sql.ast.statement.SQLCharacterDataType;
import com.alibaba.druid.sql.ast.statement.SQLColumnDefinition;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlCreateTableStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlTableIndex;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlASTVisitorAdapter;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handler for CREATE TABLE statements
 */
public class CreateTableHandler extends BaseHandler<MySqlCreateTableStatement> {

    @Override
    protected Class<MySqlCreateTableStatement> getStatementClass() {
        return MySqlCreateTableStatement.class;
    }

    @Override
    public String getStatementTypeName() {
        return "create";
    }

    @Override
    protected String doConvert(MySqlCreateTableStatement statement) {
        // This method is not used directly, as we override convert() to use JSqlConverter
        return null;
    }

    @Override
    public List<String> convert(MySqlCreateTableStatement statement) {
        // Apply visitor to clean up MySQL-specific syntax
        statement.accept(new MySqlASTVisitorAdapter() {
            @Override
            public void endVisit(MySqlCreateTableStatement x) {
                super.endVisit(x);
                // Clear table options (ENGINE, CHARSET, etc.)
                x.getTableOptions().clear();
                // Remove table indexes
                x.getTableElementList().removeIf(sqlTableElement -> sqlTableElement instanceof MySqlTableIndex);
                // Clear index types
                x.getMysqlKeys().forEach(key -> key.setIndexType(null));
                // Clear charset and collate from column definitions
                x.getTableElementList().stream()
                    .filter(SQLColumnDefinition.class::isInstance)
                    .map(SQLColumnDefinition.class::cast)
                    .map(SQLColumnDefinition::getDataType)
                    .filter(SQLCharacterDataType.class::isInstance)
                    .map(SQLCharacterDataType.class::cast)
                    .forEach(sqlCharacterDataType -> {
                        sqlCharacterDataType.setCharSetName(null);
                        sqlCharacterDataType.setCollate(null);
                    });
            }
        });

        // Use JSqlConverter for additional type conversion
        try {
            return JSqlConverter.convertCreate(statement.toString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert CREATE TABLE statement", e);
        }
    }

    @Override
    protected int doExecute(org.apache.commons.dbutils.QueryRunner queryRunner,
                           MySqlCreateTableStatement statement, String sql) throws SQLException {
        // For CREATE TABLE, we execute each converted SQL
        return queryRunner.execute(sql);
    }

    @Override
    public int execute(org.apache.commons.dbutils.QueryRunner queryRunner,
                      MySqlCreateTableStatement statement) throws SQLException {
        List<String> convertedSqls = convert(statement);
        int totalAffected = 0;
        for (String sql : convertedSqls) {
            logStatement(sql);
            totalAffected += queryRunner.execute(sql);
        }
        return totalAffected;
    }
}
