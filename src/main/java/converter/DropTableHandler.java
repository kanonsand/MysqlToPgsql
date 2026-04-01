package converter;

import com.alibaba.druid.sql.ast.statement.SQLDropTableStatement;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlASTVisitorAdapter;

import java.util.Collections;
import java.util.List;

/**
 * Handler for DROP TABLE statements
 */
public class DropTableHandler extends BaseHandler<SQLDropTableStatement> {

    @Override
    protected Class<SQLDropTableStatement> getStatementClass() {
        return SQLDropTableStatement.class;
    }

    @Override
    public String getStatementTypeName() {
        return "drop";
    }

    @Override
    protected String doConvert(SQLDropTableStatement statement) {
        // Apply visitor to convert table names
        statement.accept(new MySqlASTVisitorAdapter() {
            @Override
            public void endVisit(SQLDropTableStatement x) {
                super.endVisit(x);
                x.getTableSources().forEach(sqlExprTableSource ->
                    sqlExprTableSource.setSimpleName(convertTableName(sqlExprTableSource.getTableName()))
                );
            }
        });
        return statement.toString();
    }

    @Override
    public List<String> convert(SQLDropTableStatement statement) {
        return Collections.singletonList(doConvert(statement));
    }
}
