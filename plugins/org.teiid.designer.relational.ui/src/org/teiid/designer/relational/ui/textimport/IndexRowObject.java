/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.teiid.designer.relational.ui.textimport;

import java.util.ArrayList;
import java.util.List;

import org.teiid.designer.tools.textimport.ui.wizards.AbstractRowObject;

/**
 * @since 8.0
 */
public class IndexRowObject extends AbstractRowObject {

    /*
    OBJECT,    NAME,            Type,   Uniqueness, Tablespace,         Column
    INDEX,     PA_PROJECTS_U1,  NORMAL, UNIQUE ,    APPS_TS_TX_IDX ,    PROJECT_ID 
    */

    private String type;
    private boolean unique = false;
    private String tablespace;
    private List columnNames = new ArrayList();
    private static final String UNIQUE = "unique"; //$NON-NLS-1$

    private String currentRowString;
    private boolean valid;

    /**
     * @param row
     * @since 4.2
     */
    public IndexRowObject( String row ) {
        super(row);
        setObjectType(RelationalRowFactory.INDEX);
    }

    /**
     * @See org.teiid.designer.ui.tools.relationaltableimporter.wizards.AbstractRowObject#parseRow()
     * @since 4.2
     */
    /**
     * @see org.teiid.designer.tools.textimport.ui.wizards.AbstractRowObject#parseRow()
     * @since 4.2
     */
    @Override
    public void parseRow() {
        String rowString = getDataString();
        valid = false;
        // Extract the index name
        if (rowString != null && rowString.length() > 8) {
            try {
                String segment = null;
                // NAME
                int index = rowString.indexOf(COMMA); // Name should not have double quotes
                segment = rowString.substring(0, index).trim();
                if (segment != null && segment.length() > 0) setName(segment);

                if (rowString.length() > index) index++;

                String restOfRow = rowString.substring(index);

                // TYPE
                // Get the rest of the row....
                restOfRow = rowString.substring(index);
                int nextCommaIndex = restOfRow.indexOf(COMMA);
                segment = restOfRow.substring(0, nextCommaIndex).trim();
                if (segment != null && segment.length() > 0) type = segment;

                // UNIQUENESS
                // Get the rest of the row....
                restOfRow = restOfRow.substring(nextCommaIndex + 1);
                nextCommaIndex = restOfRow.indexOf(COMMA);
                segment = restOfRow.substring(0, nextCommaIndex).trim();
                if (segment != null && segment.length() > 0) {
                    if (segment.equalsIgnoreCase(UNIQUE)) {
                        unique = true;
                    } else {
                        unique = false;
                    }
                }

                // TABLESPACE
                // Get the rest of the row....
                restOfRow = restOfRow.substring(nextCommaIndex + 1);
                nextCommaIndex = restOfRow.indexOf(COMMA);
                segment = restOfRow.substring(0, nextCommaIndex).trim();
                if (segment != null && segment.length() > 0) {
                    tablespace = segment;
                }

                // COLUMNIDs
                restOfRow = restOfRow.substring(nextCommaIndex + 1);
                parseColumnList(restOfRow);

                valid = true;
            } catch (Exception ex) {
                // Probably a string index OOB exception, but basically we don't want to impede the import process with
                // one bad row, so we just say this row is invalid.
                valid = false;
            }
        }
    }

    private void parseColumnList( String restOfRow ) {

        int nextCommaIndex = restOfRow.indexOf(COMMA);
        // If only one, create and return.
        if (nextCommaIndex == -1) {
            if (restOfRow.length() > 0) {
                columnNames.add(restOfRow.trim());
            }
        } else {
            // we need to keep parsing
            currentRowString = restOfRow;
            String nextSegment = getNextSegment(currentRowString);

            while (nextSegment != null) {
                columnNames.add(nextSegment);
                nextSegment = getNextSegment(currentRowString);
                if (nextSegment == null) columnNames.add(currentRowString);
            }
        }
    }

    private String getNextSegment( String restOfRow ) {
        String segment = null;
        int nextCommaIndex = restOfRow.indexOf(COMMA);
        if (nextCommaIndex != -1) {
            segment = restOfRow.substring(0, nextCommaIndex).trim();
            currentRowString = restOfRow.substring(nextCommaIndex + 1).trim();
        }

        return segment;
    }

    public List getColumnNames() {
        return this.columnNames;
    }

    public String getTablespace() {
        return this.tablespace;
    }

    public String getType() {
        return this.type;
    }

    public boolean isUnique() {
        return this.unique;
    }

    @Override
    public boolean isValid() {
        return valid;
    }
}
