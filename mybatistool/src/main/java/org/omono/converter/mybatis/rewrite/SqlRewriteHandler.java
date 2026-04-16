package org.omono.converter.mybatis.rewrite;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.*;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlASTVisitorAdapter;
import org.omono.converter.mybatis.ConversionConfig;
import org.omono.converter.mybatis.ConversionContext;
import org.omono.converter.mybatis.SqlAnalysisResult;
import org.omono.converter.mybatis.clause.ClauseContext;
import org.omono.converter.mybatis.clause.ColumnNameConverter;
import org.omono.converter.mybatis.clause.handler.TableNameConverter;
import org.omono.converter.mybatis.clause.handler.WhereClauseHandler;
import org.omono.converter.mybatis.type.mapping.SqlTypeMapper;
import org.omono.converter.common.TypeCategory;
import org.omono.converter.schema.TableMapping;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

/**
 * Abstract base class for SQL statement rewrite handlers.
 * Provides common functionality for converting MySQL SQL to PostgreSQL.
 */
public abstract class SqlRewriteHandler {
    
    /**
     * Get the statement type this handler processes
     */
    public abstract ConversionContext.StatementType getStatementType();
    
    /**
     * Analyze the SQL statement to extract metadata
     * @param stmt the SQL statement
     * @param result the analysis result to populate
     */
    public abstract void analyze(SQLStatement stmt, SqlAnalysisResult result);
    
    /**
     * Analyze the SQL statement and return the result.
     * Convenience method that creates a new SqlAnalysisResult.
     * 
     * @param stmt the SQL statement
     * @return the analysis result
     */
    public SqlAnalysisResult analyze(SQLStatement stmt) {
        SqlAnalysisResult result = new SqlAnalysisResult();
        result.setStatementType(getStatementType());
        analyze(stmt, result);
        return result;
    }
    
    /**
     * Convert the SQL statement for the target database.
     * @param stmt the SQL statement to convert
     * @param analysis the analysis result
     * @param mapping the table mapping (may be null)
     * @param context the conversion context
     * @param config the conversion configuration
     */
    public abstract void convert(SQLStatement stmt, SqlAnalysisResult analysis, 
                                  TableMapping mapping, ConversionContext context,
                                  ConversionConfig config);
    
    /**
     * Check if this handler supports the given statement
     */
    public abstract boolean supports(SQLStatement stmt);
    
    /**
     * Extract table name from a table source
     */
    protected String extractTableName(SQLTableSource tableSource) {
        if (tableSource instanceof SQLExprTableSource) {
            SQLExprTableSource exprTable = (SQLExprTableSource) tableSource;
            return TableNameConverter.cleanIdentifier(exprTable.getTableName());
        }
        return null;
    }
    
    /**
     * Extract table name from a SQL expression
     */
    protected String extractTableName(SQLExpr expr) {
        if (expr instanceof SQLIdentifierExpr) {
            return TableNameConverter.cleanIdentifier(((SQLIdentifierExpr) expr).getName());
        }
        return null;
    }
    
    /**
     * Check if a name is a value placeholder
     */
    protected boolean isValuePlaceholder(String name) {
        return name.equals("?") || 
               name.startsWith(":") || 
               name.startsWith("#{") || 
               name.startsWith("${");
    }
    
    /**
     * Create a visitor that quotes identifiers for PostgreSQL.
     * Uses TableMapping directly for backward compatibility.
     * @deprecated Use {@link #createQuoteVisitor(ClauseContext)} instead for flexible name conversion.
     */
    protected MySqlASTVisitorAdapter createQuoteVisitor(TableMapping mapping) {
        return new MySqlASTVisitorAdapter() {
            @Override
            public boolean visit(SQLExprTableSource x) {
                String name = x.getTableName();
                if (name != null) {
                    String cleanName = TableNameConverter.cleanIdentifier(name);
                    x.setExpr("\"" + cleanName + "\"");
                }
                return true;
            }
            
            @Override
            public boolean visit(SQLIdentifierExpr x) {
                String name = x.getName();
                if (name != null && !isValuePlaceholder(name)) {
                    String cleanName = ColumnNameConverter.cleanIdentifier(name);
                    String convertedName = mapping != null ? mapping.getPostgresColumnName(cleanName) : cleanName;
                    x.setName("\"" + convertedName + "\"");
                }
                return true;
            }
            
            @Override
            public boolean visit(SQLPropertyExpr x) {
                // Handle qualified column references like "t.col" or "alias.col"
                // Convert to "t"."col" for PostgreSQL
                String colName = x.getName();
                if (colName != null) {
                    String cleanColName = ColumnNameConverter.cleanIdentifier(colName);
                    String convertedName = mapping != null ? mapping.getPostgresColumnName(cleanColName) : cleanColName;
                    x.setName("\"" + convertedName + "\"");
                }
                
                // Quote the owner (table alias/name) if it's an identifier
                SQLExpr owner = x.getOwner();
                if (owner instanceof SQLIdentifierExpr) {
                    String ownerName = ((SQLIdentifierExpr) owner).getName();
                    if (ownerName != null) {
                        ((SQLIdentifierExpr) owner).setName("\"" + TableNameConverter.cleanIdentifier(ownerName) + "\"");
                    }
                }
                return true;
            }
        };
    }
    
    /**
     * Create a visitor that quotes identifiers using ClauseContext converters.
     * This is the preferred method for flexible database name conversion.
     */
    protected MySqlASTVisitorAdapter createQuoteVisitor(ClauseContext ctx) {
        return new MySqlASTVisitorAdapter() {
            @Override
            public boolean visit(SQLExprTableSource x) {
                String name = x.getTableName();
                if (name != null) {
                    // Table name in FROM clause
                    String quotedName = ctx.convertAndQuoteTableName(name);
                    x.setExpr(quotedName);
                }
                return true;
            }
            
            @Override
            public boolean visit(SQLIdentifierExpr x) {
                String name = x.getName();
                if (name != null && !isValuePlaceholder(name)) {
                    // Simple column name without table alias
                    String quotedName = ctx.convertAndQuoteColumnName(name);
                    x.setName(quotedName);
                }
                return true;
            }
            
            @Override
            public boolean visit(SQLPropertyExpr x) {
                // Handle qualified column references like "t.col" or "alias.col"
                // Convert to "t"."col" for PostgreSQL
                String colName = x.getName();
                if (colName != null) {
                    // Extract table alias from owner
                    SQLExpr owner = x.getOwner();
                    String tableAlias = null;
                    if (owner instanceof SQLIdentifierExpr) {
                        tableAlias = ((SQLIdentifierExpr) owner).getName();
                    }
                    
                    // Qualified column name with table alias
                    String quotedName = ctx.convertAndQuoteColumnName(colName, tableAlias);
                    x.setName(quotedName);
                    
                    // Quote the table alias
                    if (tableAlias != null) {
                        String quotedTable = ctx.convertAndQuoteTableName(tableAlias);
                        ((SQLIdentifierExpr) owner).setName(quotedTable);
                    }
                }
                return true;
            }
        };
    }
    
    /**
     * Get the type category for a column from the table mapping.
     * 
     * @param mapping the table mapping (may be null)
     * @param columnName the column name (MySQL name)
     * @return the type category, or OTHER if not found
     */
    protected TypeCategory getColumnType(TableMapping mapping, String columnName) {
        if (mapping == null || columnName == null) {
            return TypeCategory.OTHER;
        }
        
        String pgColName = mapping.getPostgresColumnName(columnName);
        String pgColType = mapping.getPostgresColumnType(columnName);
        
        if (pgColType != null) {
            return SqlTypeMapper.getCategory(pgColType);
        }
        return TypeCategory.OTHER;
    }
    
    /**
     * Get the type category for a column using ClauseContext.
     * 
     * @param ctx the clause context
     * @param columnName the column name
     * @return the type category, or OTHER if not found
     */
    protected TypeCategory getColumnType(ClauseContext ctx, String columnName) {
        if (ctx == null || columnName == null) {
            return TypeCategory.OTHER;
        }
        
        String targetType = ctx.getColumnType(columnName);
        if (targetType != null) {
            return SqlTypeMapper.getCategory(targetType);
        }
        return TypeCategory.OTHER;
    }
    
    /**
     * Convert a literal expression to match the target type category.
     * 
     * @param expr the literal expression
     * @param targetCategory the target type category
     * @return the converted expression, or the original if no conversion needed
     */
    protected SQLExpr convertLiteral(SQLExpr expr, TypeCategory targetCategory) {
        if (expr == null || targetCategory == TypeCategory.OTHER) {
            return expr;
        }
        
        // Handle string literal ('abc', '123')
        if (expr instanceof SQLCharExpr) {
            String text = ((SQLCharExpr) expr).getText();
            return convertStringLiteral(text, targetCategory);
        }
        
        // Handle numeric literal (123, 123.45)
        if (expr instanceof SQLIntegerExpr || expr instanceof SQLNumberExpr) {
            return convertNumericLiteral(expr, targetCategory);
        }
        
        // Handle boolean literal
        if (expr instanceof SQLBooleanExpr) {
            return convertBooleanLiteral((SQLBooleanExpr) expr, targetCategory);
        }
        
        // Other expression types (function calls, column references, etc.) - no conversion
        return expr;
    }
    
    /**
     * Convert a string literal to the target type.
     */
    private SQLExpr convertStringLiteral(String text, TypeCategory targetCategory) {
        if (text == null || text.isEmpty()) {
            return targetCategory == TypeCategory.STRING ? 
                new SQLCharExpr(text) : new SQLNullExpr();
        }
        
        switch (targetCategory) {
            case LONG:
                try {
                    long value = Long.parseLong(text);
                    return new SQLIntegerExpr(BigInteger.valueOf(value));
                } catch (NumberFormatException e) {
                    return new SQLCharExpr(text); // Keep as string if parse fails
                }
                
            case DOUBLE:
                try {
                    double value = Double.parseDouble(text);
                    return new SQLDoubleExpr(value);
                } catch (NumberFormatException e) {
                    return new SQLCharExpr(text);
                }
                
            case DECIMAL:
                try {
                    BigDecimal value = new BigDecimal(text);
                    return new SQLDecimalExpr(value);
                } catch (NumberFormatException e) {
                    return new SQLCharExpr(text);
                }
                
            case BOOLEAN:
                String lower = text.toLowerCase();
                if ("true".equals(lower) || "1".equals(lower)) {
                    return new SQLBooleanExpr(true);
                }
                if ("false".equals(lower) || "0".equals(lower)) {
                    return new SQLBooleanExpr(false);
                }
                return new SQLCharExpr(text);
                
            case DATE:
                // Keep date strings as-is, let PostgreSQL handle them
                return new SQLCharExpr(text);
                
            case STRING:
            default:
                return new SQLCharExpr(text);
        }
    }
    
    /**
     * Convert a numeric literal to the target type.
     */
    private SQLExpr convertNumericLiteral(SQLExpr expr, TypeCategory targetCategory) {
        Number number;
        
        if (expr instanceof SQLIntegerExpr) {
            number = ((SQLIntegerExpr) expr).getNumber();
        } else if (expr instanceof SQLNumberExpr) {
            number = ((SQLNumberExpr) expr).getNumber();
        } else {
            return expr;
        }
        
        switch (targetCategory) {
            case STRING:
                return new SQLCharExpr(number.toString());
                
            case LONG:
                return new SQLIntegerExpr(BigInteger.valueOf(number.longValue()));
                
            case DOUBLE:
                return new SQLDoubleExpr(number.doubleValue());
                
            case DECIMAL:
                return new SQLDecimalExpr(new BigDecimal(number.toString()));
                
            case BOOLEAN:
                return new SQLBooleanExpr(number.longValue() != 0);
                
            default:
                return expr;
        }
    }
    
    /**
     * Convert a boolean literal to the target type.
     */
    private SQLExpr convertBooleanLiteral(SQLBooleanExpr expr, TypeCategory targetCategory) {
        boolean value = expr.getValue();
        
        switch (targetCategory) {
            case STRING:
                return new SQLCharExpr(String.valueOf(value));
                
            case LONG:
                return new SQLIntegerExpr(BigInteger.valueOf(value ? 1 : 0));
                
            case DOUBLE:
                return new SQLDoubleExpr(value ? 1.0 : 0.0);
                
            case DECIMAL:
                return new SQLDecimalExpr(new BigDecimal(value ? 1 : 0));
                
            default:
                return expr;
        }
    }
    
    /**
     * Extract parameter types from a WHERE clause expression.
     * 
     * @param where the WHERE clause expression
     * @param ctx the clause context
     * @return list of type categories in parameter order
     */
    protected List<TypeCategory> extractWhereParameterTypes(SQLExpr where, ClauseContext ctx) {
        WhereClauseHandler handler = new WhereClauseHandler();
        return handler.extractParameterTypes(where, ctx);
    }
    
    /**
     * Process a WHERE clause expression.
     * 
     * @param where the WHERE clause expression
     * @param ctx the clause context
     */
    protected void processWhereClause(SQLExpr where, ClauseContext ctx) {
        new WhereClauseHandler().process(where, ctx);
    }
}