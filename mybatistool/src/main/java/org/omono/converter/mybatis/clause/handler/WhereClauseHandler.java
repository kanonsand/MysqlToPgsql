package org.omono.converter.mybatis.clause.handler;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLObject;
import com.alibaba.druid.sql.ast.expr.*;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlASTVisitorAdapter;
import org.omono.converter.common.TypeCategory;
import org.omono.converter.mybatis.clause.ClauseContext;
import org.omono.converter.mybatis.clause.ColumnNameConverter;
import org.omono.converter.mybatis.type.mapping.SqlTypeMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Handler for WHERE clause.
 * Processes:
 * 1. Converts literals to match column types
 * 2. Quotes identifiers for PostgreSQL
 * 3. Extracts parameter types for type conversion
 */
public class WhereClauseHandler implements ClauseHandler {
    
    @Override
    public void process(Object clause, ClauseContext ctx) {
        if (clause == null || !(clause instanceof SQLExpr)) {
            return;
        }
        
        SQLExpr where = (SQLExpr) clause;
        where.accept(new WhereClauseVisitor(ctx));
    }
    
    /**
     * Extract parameter types from WHERE clause.
     * Returns types in the order placeholders appear in SQL.
     */
    public List<TypeCategory> extractParameterTypes(SQLExpr where, ClauseContext ctx) {
        List<TypeCategory> categories = new ArrayList<>();
        if (where == null) {
            return categories;
        }
        
        where.accept(new MySqlASTVisitorAdapter() {
            @Override
            public boolean visit(SQLVariantRefExpr x) {
                TypeCategory tc = determineTypeFromParent(x, ctx);
                categories.add(tc);
                return true;
            }
        });
        
        return categories;
    }
    
    /**
     * Determine parameter type by traversing parent nodes.
     */
    private TypeCategory determineTypeFromParent(SQLVariantRefExpr placeholder, ClauseContext ctx) {
        SQLObject parent = placeholder.getParent();
        
        while (parent != null) {
            TypeCategory tc = null;
            
            if (parent instanceof SQLBinaryOpExpr) {
                tc = extractFromBinaryOp((SQLBinaryOpExpr) parent, placeholder, ctx);
            } else if (parent instanceof SQLInListExpr) {
                tc = extractFromInList((SQLInListExpr) parent, ctx);
            } else if (parent instanceof SQLBetweenExpr) {
                tc = extractFromBetween((SQLBetweenExpr) parent, ctx);
            }
            
            if (tc != null) {
                return tc;
            }
            
            parent = parent.getParent();
        }
        
        return TypeCategory.OTHER;
    }
    
    private TypeCategory extractFromBinaryOp(SQLBinaryOpExpr binary, SQLVariantRefExpr placeholder, ClauseContext ctx) {
        SQLExpr left = binary.getLeft();
        SQLExpr right = binary.getRight();
        
        if (right == placeholder) {
            String colName = extractColumnName(left);
            if (colName != null) {
                return getColumnType(colName, ctx);
            }
        }
        if (left == placeholder) {
            String colName = extractColumnName(right);
            if (colName != null) {
                return getColumnType(colName, ctx);
            }
        }
        return null;
    }
    
    private TypeCategory extractFromInList(SQLInListExpr inList, ClauseContext ctx) {
        String colName = extractColumnName(inList.getExpr());
        if (colName != null) {
            return getColumnType(colName, ctx);
        }
        return null;
    }
    
    private TypeCategory extractFromBetween(SQLBetweenExpr between, ClauseContext ctx) {
        String colName = extractColumnName(between.getTestExpr());
        if (colName != null) {
            return getColumnType(colName, ctx);
        }
        return null;
    }
    
    /**
     * Visitor for WHERE clause - handles both literal conversion and identifier quoting.
     */
    private static class WhereClauseVisitor extends MySqlASTVisitorAdapter {
        private final ClauseContext ctx;
        
        WhereClauseVisitor(ClauseContext ctx) {
            this.ctx = ctx;
        }
        
        @Override
        public boolean visit(SQLBinaryOpExpr x) {
            SQLExpr left = x.getLeft();
            SQLExpr right = x.getRight();
            
            // Convert literals based on column type
            String colName = extractColumnName(left);
            if (colName != null && isLiteral(right) && ctx.isConvertLiterals()) {
                TypeCategory tc = getColumnType(colName, ctx);
                SQLExpr converted = convertLiteral(right, tc);
                if (converted != right) {
                    x.setRight(converted);
                }
                return true;
            }
            
            colName = extractColumnName(right);
            if (colName != null && isLiteral(left) && ctx.isConvertLiterals()) {
                TypeCategory tc = getColumnType(colName, ctx);
                SQLExpr converted = convertLiteral(left, tc);
                if (converted != left) {
                    x.setLeft(converted);
                }
            }
            return true;
        }
        
        @Override
        public boolean visit(SQLInListExpr x) {
            if (!ctx.isConvertLiterals()) {
                return true;
            }
            
            String colName = extractColumnName(x.getExpr());
            if (colName != null) {
                TypeCategory tc = getColumnType(colName, ctx);
                
                List<SQLExpr> targetList = x.getTargetList();
                for (int i = 0; i < targetList.size(); i++) {
                    SQLExpr value = targetList.get(i);
                    if (isLiteral(value)) {
                        SQLExpr converted = convertLiteral(value, tc);
                        if (converted != value) {
                            targetList.set(i, converted);
                        }
                    }
                }
            }
            return true;
        }
        
        @Override
        public boolean visit(SQLBetweenExpr x) {
            if (!ctx.isConvertLiterals()) {
                return true;
            }
            
            String colName = extractColumnName(x.getTestExpr());
            if (colName != null) {
                TypeCategory tc = getColumnType(colName, ctx);
                
                SQLExpr begin = x.getBeginExpr();
                SQLExpr end = x.getEndExpr();
                
                if (isLiteral(begin)) {
                    SQLExpr converted = convertLiteral(begin, tc);
                    if (converted != begin) {
                        x.setBeginExpr(converted);
                    }
                }
                if (isLiteral(end)) {
                    SQLExpr converted = convertLiteral(end, tc);
                    if (converted != end) {
                        x.setEndExpr(converted);
                    }
                }
            }
            return true;
        }
        
        @Override
        public boolean visit(SQLIdentifierExpr x) {
            if (!ctx.isQuoteIdentifiers()) {
                return true;
            }
            
            String name = x.getName();
            if (name != null && !isPlaceholder(name)) {
                // Simple column name without table alias
                String quotedName = ctx.convertAndQuoteColumnName(name);
                x.setName(quotedName);
            }
            return true;
        }
        
        @Override
        public boolean visit(SQLPropertyExpr x) {
            if (!ctx.isQuoteIdentifiers()) {
                return true;
            }
            
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
                if (owner instanceof SQLIdentifierExpr) {
                    String quotedTable = ctx.convertAndQuoteTableName(tableAlias);
                    ((SQLIdentifierExpr) owner).setName(quotedTable);
                }
            }
            return true;
        }
    }
    
    // Static utility methods
    
    static String extractColumnName(SQLExpr expr) {
        if (expr instanceof SQLIdentifierExpr) {
            return ColumnNameConverter.cleanIdentifier(((SQLIdentifierExpr) expr).getName());
        }
        if (expr instanceof SQLPropertyExpr) {
            return ColumnNameConverter.cleanIdentifier(((SQLPropertyExpr) expr).getName());
        }
        return null;
    }
    
    static boolean isPlaceholder(String name) {
        return name.equals("?") || name.startsWith(":") || 
               name.startsWith("#{") || name.startsWith("${");
    }
    
    static boolean isLiteral(SQLExpr expr) {
        return expr instanceof SQLTextLiteralExpr
            || expr instanceof SQLNumericLiteralExpr
            || expr instanceof SQLBooleanExpr
            || expr instanceof SQLNullExpr;
    }
    
    static TypeCategory getColumnType(String colName, ClauseContext ctx) {
        String targetType = ctx.getColumnType(colName);
        if (targetType == null) {
            return TypeCategory.OTHER;
        }
        return SqlTypeMapper.getCategory(targetType);
    }
    
    static SQLExpr convertLiteral(SQLExpr expr, TypeCategory targetCategory) {
        SQLExpr converted = targetCategory.convert(expr);
        return converted != null ? converted : expr;
    }
}