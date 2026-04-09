package org.omono.converter.mybatis.clause.handler;

import org.omono.converter.mybatis.clause.ClauseContext;

/**
 * Base interface for SQL clause handlers.
 * Each handler processes a specific clause (SELECT, FROM, WHERE, etc.)
 */
public interface ClauseHandler {
    
    /**
     * Process the clause for PostgreSQL conversion.
     * 
     * @param clause the clause AST node
     * @param context the processing context containing all shared state
     */
    void process(Object clause, ClauseContext context);
}
