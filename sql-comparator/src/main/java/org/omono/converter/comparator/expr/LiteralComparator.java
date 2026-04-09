package org.omono.converter.comparator.expr;

import com.alibaba.druid.sql.ast.expr.*;
import org.omono.converter.common.TypeCategory;
import org.omono.converter.comparator.context.CompareContext;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * Comparator for SQL literal expressions.
 * Handles type checking and value comparison for literals.
 */
public class LiteralComparator {
    
    private final CompareContext ctx;
    
    public LiteralComparator(CompareContext ctx) {
        this.ctx = ctx;
    }
    
    /**
     * Compare two literal expressions.
     */
    public void compare(SQLLiteralExpr a, SQLLiteralExpr b, String path) {
        TypeCategory catA = getTypeCategory(a);
        TypeCategory catB = getTypeCategory(b);
        
        Object valA = extractValue(a);  // 已标准化
        Object valB = extractValue(b);  // 已标准化
        
        if (catA == catB) {
            // 同类型直接比较
            if (!Objects.equals(valA, valB)) {
                throw new AssertionError(path + ": Value mismatch - " + valA + " vs " + valB);
            }
            return;
        }
        
        // 处理 DECIMAL vs DOUBLE/LONG 的比较
        if (isNumericCategory(catA) && isNumericCategory(catB)) {
            if (compareNumericValues(valA, valB)) {
                return;
            }
        }
        
        // 不同类型，查找 matcher
        BiPredicate<Object, Object> matcher = ctx.getTypeMatcher(catA, catB);
        if (matcher != null && matcher.test(valA, valB)) {
            return;
        }
        
        throw new AssertionError(path + ": Type/Value mismatch - " + catA + ":" + valA + " vs " + catB + ":" + valB);
    }
    
    private boolean isNumericCategory(TypeCategory cat) {
        return cat == TypeCategory.LONG || cat == TypeCategory.DOUBLE || cat == TypeCategory.DECIMAL;
    }
    
    private boolean compareNumericValues(Object valA, Object valB) {
        // 统一转换为 BigDecimal 进行比较
        BigDecimal bdA = toBigDecimal(valA);
        BigDecimal bdB = toBigDecimal(valB);
        if (bdA != null && bdB != null) {
            return bdA.compareTo(bdB) == 0;
        }
        return false;
    }
    
    private BigDecimal toBigDecimal(Object val) {
        if (val instanceof BigDecimal) {
            return (BigDecimal) val;
        }
        if (val instanceof Long) {
            return BigDecimal.valueOf((Long) val);
        }
        if (val instanceof Double) {
            return BigDecimal.valueOf((Double) val);
        }
        if (val instanceof Number) {
            return new BigDecimal(val.toString());
        }
        return null;
    }
    
    /**
     * Determine the type category of a literal expression.
     */
    public TypeCategory getTypeCategory(SQLLiteralExpr expr) {
        if (expr instanceof SQLBooleanExpr) return TypeCategory.BOOLEAN;
        if (expr instanceof SQLTextLiteralExpr) return TypeCategory.STRING;
        if (expr instanceof SQLNullExpr) return TypeCategory.OTHER;
        
        // DECIMAL literal
        if (expr instanceof SQLDecimalExpr) {
            return TypeCategory.DECIMAL;
        }
        
        // Numeric type detection
        if (expr instanceof SQLNumericLiteralExpr) {
            Number num = ((SQLNumericLiteralExpr) expr).getNumber();
            if (num instanceof Integer || num instanceof Long ||
                num instanceof Short || num instanceof Byte) {
                return TypeCategory.LONG;
            }
            if (num instanceof Double || num instanceof Float) {
                return TypeCategory.DOUBLE;
            }
            if (num instanceof BigDecimal) {
                return TypeCategory.DECIMAL;
            }
        }
        
        if (expr instanceof SQLDateExpr || expr instanceof SQLTimestampExpr) {
            return TypeCategory.DATE;
        }
        
        return TypeCategory.OTHER;
    }
    
    /**
     * Extract the value from a literal expression.
     * Values are normalized: Integer/Short/Byte → Long, Float → Double.
     */
    public Object extractValue(SQLLiteralExpr expr) {
        if (expr instanceof SQLCharExpr) {
            return ((SQLCharExpr) expr).getText();
        }
        if (expr instanceof SQLIntegerExpr) {
            // Normalize to Long
            Number num = ((SQLIntegerExpr) expr).getNumber();
            return num.longValue();
        }
        if (expr instanceof SQLNumberExpr) {
            Number num = ((SQLNumberExpr) expr).getNumber();
            // Normalize to Long or Double
            if (num instanceof Double || num instanceof Float) {
                return num.doubleValue();
            }
            if (num instanceof BigDecimal) {
                return ((BigDecimal) num).stripTrailingZeros();  // Normalize BigDecimal
            }
            return num.longValue();
        }
        if (expr instanceof SQLDecimalExpr) {
            // Handle DECIMAL literal (e.g., DECIMAL '100.00')
            Number num = ((SQLDecimalExpr) expr).getNumber();
            if (num instanceof BigDecimal) {
                return ((BigDecimal) num).stripTrailingZeros();
            }
            return num;
        }
        if (expr instanceof SQLBooleanExpr) {
            return ((SQLBooleanExpr) expr).getBooleanValue();
        }
        if (expr instanceof SQLNullExpr) {
            return null;
        }
        return expr.toString();
    }
}