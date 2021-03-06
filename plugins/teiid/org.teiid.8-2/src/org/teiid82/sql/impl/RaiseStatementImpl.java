/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.teiid82.sql.impl;

import org.teiid.designer.query.sql.ILanguageVisitor;
import org.teiid.designer.query.sql.proc.IRaiseStatement;
import org.teiid.query.sql.proc.RaiseStatement;

/**
 *
 */
public class RaiseStatementImpl extends StatementImpl implements IRaiseStatement {

    /**
     * @param raiseStatement
     */
    public RaiseStatementImpl(RaiseStatement raiseStatement) {
        super(raiseStatement);
    }

    @Override
    public RaiseStatement getDelegate() {
        return (RaiseStatement) delegate;
    }

    @Override
    public void acceptVisitor(ILanguageVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public RaiseStatementImpl clone() {
        return new RaiseStatementImpl(getDelegate().clone());
    }
}