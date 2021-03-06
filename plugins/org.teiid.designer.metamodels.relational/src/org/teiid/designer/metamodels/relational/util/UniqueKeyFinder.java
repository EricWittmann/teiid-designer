/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.teiid.designer.metamodels.relational.util;

import org.eclipse.emf.ecore.EObject;
import org.teiid.designer.metamodels.relational.BaseTable;
import org.teiid.designer.metamodels.relational.Catalog;
import org.teiid.designer.metamodels.relational.Schema;
import org.teiid.designer.metamodels.relational.UniqueKey;

/**
 * UniqueKeyFinder
 *
 * @since 8.0
 */
public class UniqueKeyFinder extends RelationalEntityFinder {

    /**
     * Construct an instance of UniqueKeyFinder.
     */
    public UniqueKeyFinder() {
        super();
    }

    /**
     * @see org.teiid.designer.core.util.ModelVisitor#visit(org.eclipse.emf.ecore.EObject)
     */
    @Override
	public boolean visit( final EObject object ) {
        if (object instanceof UniqueKey) {
            found((UniqueKey)object);
            return false;
        }

        // BaseTables contain unique keys ...
        if (object instanceof Catalog) {
            return true;
        }
        if (object instanceof Schema) {
            return true;
        }
        if (object instanceof BaseTable) {
            return true;
        }
        return false;
    }

}
