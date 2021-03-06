/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.teiid.designer.schema.tools.processing;

import org.teiid.designer.schema.tools.model.schema.SchemaModel;
/**
 *Determines the relationships between SchemaObjects based upon an instance
 *of the RelationshipRules Interface and performs the folding of the children
 *into parents as needed.
 *
 * @since 8.0
 */
public interface RelationshipProcessor {

	/**
	 * Performs the folding operation on the provided SchemaModel.
	 * @param model - the SchemModel to perform the folding operation on.
	 */
	public abstract void calculateRelationshipTypes(SchemaModel model);

	/**
	 * Adds a relationship between SchemaObjects to the set of relationships
	 * maintained by the processor.
	 * @param key - the name of the child table in the relationship.
	 * @param value - the value of the Relationship
	 */
	public abstract void addRelationship(String key, Integer value);
	
	/**
	 * Returns the value of the Child Limit value for this RelationshipProcessor 
	 * @return Returns the value of the Child Limit value
	 */
	public int C_threshold();
	
	/**
	 * Returns the value of the Parent Limit value for this RelationshipProcessor 
	 * @return Returns the value of the Parent Limit value
	 */
	public int P_threshold();
	
	/**
	 * Returns the value of the Field Limit value for this RelationshipProcessor 
	 * @return Returns the value of the Field Limit value
	 */
	public int F_threshold();
	
	/**
	 * Accessor to provide the RelationshipRules that the processor will use
	 * to perform the forlding operations.
	 * @param rules - the RelationshipRules
	 */
	public abstract void setRelationshipRules(RelationshipRules rules);
}
