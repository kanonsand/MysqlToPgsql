package converter;

import com.alibaba.druid.DbType;
import com.alibaba.druid.pool.DruidDataSourceFactory;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import org.apache.commons.dbutils.QueryRunner;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Main converter class that orchestrates MySQL to PostgreSQL SQL migration
 * 
 * Supported statements:
 * - DROP TABLE
 * - CREATE TABLE
 * - INSERT
 * - UPDATE
 * - DELETE
 * 
 * Usage: java -jar xxx.jar url username password sqlfile
 * 
 * Note: If date type string insertion fails, try adding ?stringtype=unspecified to the JDBC URL
 */
public class Converter {

    private final List<StatementHandler<?>> handlers;
    private final QueryRunner queryRunner;

    public Converter(DataSource dataSource) {
        this.queryRunner = new QueryRunner(dataSource);
        this.handlers = new ArrayList<>();
        // Register handlers in execution order
        registerHandler(new DropTableHandler());
        registerHandler(new CreateTableHandler());
        registerHandler(new InsertHandler());
        registerHandler(new UpdateHandler());
        registerHandler(new DeleteHandler());
    }

    /**
     * Register a statement handler
     * @param handler the handler to register
     */
    public void registerHandler(StatementHandler<?> handler) {
        handlers.add(handler);
    }

    /**
     * Parse SQL file and return all statements
     * @param sqlFilePath path to the SQL file
     * @return list of parsed SQL statements
     */
    public List<SQLStatement> parseSqlFile(String sqlFilePath) throws IOException {
        byte[] allBytes = Files.readAllBytes(Paths.get(sqlFilePath));
        String sql = new String(allBytes);
        return SQLUtils.parseStatements(sql, DbType.mysql);
    }

    /**
     * Categorize statements by type for each handler
     * @param statements all parsed statements
     * @return map of handler to list of statements
     */
    @SuppressWarnings("unchecked")
    public void processStatements(List<SQLStatement> statements) {
        for (StatementHandler<?> handler : handlers) {
            List<SQLStatement> handlerStatements = new ArrayList<>();
            
            // Collect statements for this handler
            for (SQLStatement statement : statements) {
                if (handler.canHandle(statement)) {
                    handlerStatements.add(statement);
                }
            }

            if (handlerStatements.isEmpty()) {
                continue;
            }

            System.out.println("Processing " + handlerStatements.size() + " " + handler.getStatementTypeName() + " statement(s)...");

            // Process each statement
            for (SQLStatement statement : handlerStatements) {
                try {
                    executeHandler(handler, statement);
                } catch (SQLException e) {
                    System.err.println("Error executing " + handler.getStatementTypeName() + " statement: " + e.getMessage());
                    // Continue with next statement
                }
            }
        }
    }

    /**
     * Execute a handler for a specific statement
     */
    @SuppressWarnings("unchecked")
    private <T extends SQLStatement> void executeHandler(StatementHandler<T> handler, SQLStatement statement) throws SQLException {
        T typedStatement = (T) statement;
        handler.execute(queryRunner, typedStatement);
    }

    /**
     * Run the migration
     * @param sqlFilePath path to the SQL file
     */
    public void migrate(String sqlFilePath) throws IOException, SQLException {
        System.out.println("Parsing SQL file: " + sqlFilePath);
        List<SQLStatement> statements = parseSqlFile(sqlFilePath);
        System.out.println("Found " + statements.size() + " statement(s)");
        
        processStatements(statements);
        
        System.out.println("Migration completed!");
    }

    /**
     * Main entry point
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("Usage: java -jar xxx.jar url username password sqlfile");
            System.out.println("\nSupported statements: DROP TABLE, CREATE TABLE, INSERT, UPDATE, DELETE");
            System.out.println("\nNote: If date type string insertion fails, try adding ?stringtype=unspecified to the JDBC URL");
            return;
        }

        // Create data source from command line arguments
        Properties properties = new Properties();
        properties.put(DruidDataSourceFactory.PROP_URL, args[0]);
        properties.put(DruidDataSourceFactory.PROP_USERNAME, args[1]);
        properties.put(DruidDataSourceFactory.PROP_PASSWORD, args[2]);
        DataSource dataSource = DruidDataSourceFactory.createDataSource(properties);

        // Create converter and run migration
        Converter converter = new Converter(dataSource);
        converter.migrate(args[3]);
    }
}
