package org.omono.converter.common;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.*;

import java.math.BigDecimal;

/**
 * Type category for SQL type classification.
 * Each category maps to a specific JDBC method for type conversion.
 */
public enum TypeCategory {
    LONG("setLong") {
        @Override
        public boolean matchesLiteral(SQLExpr expr) {
            return expr instanceof SQLIntegerExpr
                || expr instanceof SQLBigIntExpr
                || expr instanceof SQLSmallIntExpr
                || expr instanceof SQLTinyIntExpr;
        }
        
        @Override
        public SQLExpr convert(SQLExpr expr) {
            if (matchesLiteral(expr)) return expr;
            if (expr instanceof SQLNumericLiteralExpr) {
                return new SQLIntegerExpr(((SQLNumericLiteralExpr) expr).getNumber().longValue());
            }
            if (expr instanceof SQLTextLiteralExpr) {
                try {
                    return new SQLIntegerExpr(Long.parseLong(((SQLTextLiteralExpr) expr).getText().trim()));
                } catch (NumberFormatException e) { return null; }
            }
            if (expr instanceof SQLBooleanExpr) {
                return new SQLIntegerExpr(((SQLBooleanExpr) expr).getBooleanValue() ? 1L : 0L);
            }
            return null;
        }
    },
    
    DOUBLE("setDouble") {
        @Override
        public boolean matchesLiteral(SQLExpr expr) {
            return expr instanceof SQLNumberExpr 
                || expr instanceof SQLDoubleExpr 
                || expr instanceof SQLFloatExpr;
        }
        
        @Override
        public SQLExpr convert(SQLExpr expr) {
            if (matchesLiteral(expr)) return expr;
            if (expr instanceof SQLNumericLiteralExpr) {
                return new SQLNumberExpr(((SQLNumericLiteralExpr) expr).getNumber().doubleValue());
            }
            if (expr instanceof SQLTextLiteralExpr) {
                try {
                    return new SQLNumberExpr(Double.parseDouble(((SQLTextLiteralExpr) expr).getText().trim()));
                } catch (NumberFormatException e) { return null; }
            }
            if (expr instanceof SQLBooleanExpr) {
                return new SQLNumberExpr(((SQLBooleanExpr) expr).getBooleanValue() ? 1.0 : 0.0);
            }
            return null;
        }
    },
    
    DECIMAL("setBigDecimal") {
        @Override
        public boolean matchesLiteral(SQLExpr expr) {
            return expr instanceof SQLNumberExpr 
                || expr instanceof SQLDecimalExpr
                || expr instanceof SQLBigIntExpr;
        }
        
        @Override
        public SQLExpr convert(SQLExpr expr) {
            if (matchesLiteral(expr)) return expr;
            if (expr instanceof SQLNumericLiteralExpr) {
                Number num = ((SQLNumericLiteralExpr) expr).getNumber();
                BigDecimal bd = (num instanceof BigDecimal) ? (BigDecimal) num : new BigDecimal(num.toString());
                return new SQLNumberExpr(bd);
            }
            if (expr instanceof SQLTextLiteralExpr) {
                try {
                    return new SQLNumberExpr(new BigDecimal(((SQLTextLiteralExpr) expr).getText().trim()));
                } catch (NumberFormatException e) { return null; }
            }
            if (expr instanceof SQLBooleanExpr) {
                return new SQLNumberExpr(((SQLBooleanExpr) expr).getBooleanValue() ? BigDecimal.ONE : BigDecimal.ZERO);
            }
            return null;
        }
    },
    
    STRING("setString") {
        @Override
        public boolean matchesLiteral(SQLExpr expr) {
            return expr instanceof SQLTextLiteralExpr;
        }
        
        @Override
        public SQLExpr convert(SQLExpr expr) {
            if (matchesLiteral(expr)) return expr;
            if (expr instanceof SQLNumericLiteralExpr) {
                return new SQLCharExpr(((SQLNumericLiteralExpr) expr).getNumber().toString());
            }
            if (expr instanceof SQLBooleanExpr) {
                return new SQLCharExpr(String.valueOf(((SQLBooleanExpr) expr).getBooleanValue()));
            }
            return null;
        }
    },
    
    DATE("setTimestamp") {
        @Override
        public boolean matchesLiteral(SQLExpr expr) {
            return expr instanceof SQLDateExpr 
                || expr instanceof SQLTimestampExpr
                || expr instanceof SQLDateTimeExpr
                || expr instanceof SQLTimeExpr;
        }
        
        @Override
        public SQLExpr convert(SQLExpr expr) {
            if (matchesLiteral(expr)) return expr;
            if (expr instanceof SQLTextLiteralExpr) {
                return new SQLCharExpr(((SQLTextLiteralExpr) expr).getText());
            }
            return null;
        }
    },
    
    BOOLEAN("setBoolean") {
        @Override
        public boolean matchesLiteral(SQLExpr expr) {
            return expr instanceof SQLBooleanExpr;
        }
        
        @Override
        public SQLExpr convert(SQLExpr expr) {
            if (matchesLiteral(expr)) return expr;
            if (expr instanceof SQLNumericLiteralExpr) {
                return new SQLBooleanExpr(((SQLNumericLiteralExpr) expr).getNumber().longValue() != 0);
            }
            if (expr instanceof SQLTextLiteralExpr) {
                String text = ((SQLTextLiteralExpr) expr).getText().trim().toLowerCase();
                return new SQLBooleanExpr("true".equals(text) || "1".equals(text));
            }
            return null;
        }
    },
    
    BINARY("setBytes") {
        @Override
        public boolean matchesLiteral(SQLExpr expr) {
            return expr instanceof SQLHexExpr || expr instanceof SQLBinaryExpr;
        }
        
        @Override
        public SQLExpr convert(SQLExpr expr) {
            if (matchesLiteral(expr)) return expr;
            return null;
        }
    },
    
    OTHER("setObject") {
        @Override
        public boolean matchesLiteral(SQLExpr expr) {
            return false;
        }
        
        @Override
        public SQLExpr convert(SQLExpr expr) {
            return null;
        }
    };
    
    private final String targetJdbcMethod;
    
    TypeCategory(String targetJdbcMethod) {
        this.targetJdbcMethod = targetJdbcMethod;
    }
    
    /**
     * Get the target JDBC method for type conversion
     */
    public String getTargetJdbcMethod() {
        return targetJdbcMethod;
    }
    
    /**
     * Check if the expression is a literal that matches this type category.
     * @param expr SQL expression to check
     * @return true if the expression matches this type category
     */
    public abstract boolean matchesLiteral(SQLExpr expr);
    
    /**
     * Convert an expression to this type category.
     * @param expr SQL expression to convert
     * @return converted expression, or null if conversion is not possible
     */
    public abstract SQLExpr convert(SQLExpr expr);
}
