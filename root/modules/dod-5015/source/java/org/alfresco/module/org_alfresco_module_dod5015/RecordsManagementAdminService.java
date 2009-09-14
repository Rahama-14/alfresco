/*
 * Copyright (C) 2005-2009 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have received a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.module.org_alfresco_module_dod5015;

import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.dictionary.AssociationDefinition;
import org.alfresco.service.cmr.dictionary.ConstraintDefinition;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

/**
 * Records management custom model service interface.
 * 
 * @author Neil McErlean, janv
 */
public interface RecordsManagementAdminService
{
    /**
     * This method returns the custom properties that have been defined for the specified
     * customisable RM element.
     * Note: the custom property definitions are retrieved from the dictionaryService
     * which is notified of any newly created definitions on transaction commit.
     * Therefore custom properties created in the current transaction will not appear
     * in the results.
     * 
     * @param customisedElement
     * @return
     */
    public Map<QName, PropertyDefinition> getCustomPropertyDefinitions(CustomisableRmElement customisedElement);
    
    /**
     * This method returns the custom properties that have been defined for all of
     * the specified customisable RM elements.
     * Note: the custom property definitions are retrieved from the dictionaryService
     * which is notified of any newly created definitions on transaction commit.
     * Therefore custom properties created in the current transaction will not appear
     * in the results.
     */
    public Map<QName, PropertyDefinition> getCustomPropertyDefinitions();
    
    /**
     * Add custom property definition
     * 
     * Note: no default value, single valued, optional, not system protected, no constraints
     * 
     * @param propId - If a value for propId is provided it will be used to identify property definitions
     *                 within URLs and in QNames. Therefore it must contain URL/QName-valid characters
     *                 only. It must also be unique.
     *                 If a null value is passed, an id will be generated.
     * @param aspectName - mandatory
     * @param label - mandatory
     * @param dataType - mandatory
     * @param title - optional
     * @param description - optional
     * 
     * @return the propId, whether supplied as a parameter or generated.
     */
    public QName addCustomPropertyDefinition(QName propId, String aspectName, String label, QName dataType, String title, String description);
    
    /**
     * Add custom property definition with one optional constraint reference
     * 
     * @param propId - If a value for propId is provided it will be used to identify property definitions
     *                 within URLs and in QNames. Therefore it must contain URL/QName-valid characters
     *                 only. It must also be unique.
     *                 If a null value is passed, an id will be generated.
     * @param aspectName - mandatory
     * @param label - mandatory
     * @param dataType - mandatory
     * @param title - optional
     * @param description - optional
     * @param defaultValue - optional
     * @param multiValued - TRUE if multi-valued property
     * @param mandatory - TRUE if mandatory property
     * @param isProtected - TRUE if protected property
     * @param lovConstraintQName - optional custom constraint
     * 
     * @return the propId, whether supplied as a parameter or generated.
     */
    public QName addCustomPropertyDefinition(QName propId, String aspectName, String label, QName dataType, String title, String description, String defaultValue, boolean multiValued, boolean mandatory, boolean isProtected, QName lovConstraintQName);
    
    /**
     * Update the custom property definition's label (title).
     * 
     * @param propQName the qname of the property definition
     * @param newLabel the new value for the label.
     * @return the propId.
     */
    public QName setCustomPropertyDefinitionLabel(QName propQName, String newLabel);

    /**
     * Sets a new list of values constraint on the custom property definition.
     * 
     * @param propQName the qname of the property definition
     * @param newLovConstraint the List-Of-Values constraintRef.
     * @return the propId.
     */
    public QName setCustomPropertyDefinitionConstraint(QName propQName, QName newLovConstraint);

    /**
     * Removes all list of values constraints from the custom property definition.
     * 
     * @param propQName the qname of the property definition
     * @return the propId.
     */
    public QName removeCustomPropertyDefinitionConstraints(QName propQName);

    /**
     * 
     * @param propQName
     * @deprecated currently throws UnsupportedOperationException
     */
    public void removeCustomPropertyDefinition(QName propQName);
    
    /**
     * This method returns the custom references that have been defined in the custom
     * model.
     * Note: the custom reference definitions are retrieved from the dictionaryService
     * which is notified of any newly created definitions on transaction commit.
     * Therefore custom references created in the current transaction will not appear
     * in the results.
     * 
     * @return The Map of custom references (both parent-child and standard).
     */
    public Map<QName, AssociationDefinition> getCustomReferenceDefinitions();
    
    /**
     * Fetches all associations <i>from</i> the given source.
     * 
     * @param node the node from which the associations start.
     * @return a List of associations.
     */
    public List<AssociationRef> getCustomReferencesFrom(NodeRef node);
    
    /**
     * Fetches all child associations of the given source. i.e. all associations where the
     * given node is the parent.
     * 
     * @param node
     * @return
     */
    public List<ChildAssociationRef> getCustomChildReferences(NodeRef node);

    /**
     * Returns a List of all associations <i>to</i> the given node.
     * 
     * @param node the node to which the associations point.
     * @return a List of associations.
     */
    public List<AssociationRef> getCustomReferencesTo(NodeRef node);
    
    /**
     * Fetches all child associations where the given node is the child.
     * 
     * @param node
     * @return
     */
    public List<ChildAssociationRef> getCustomParentReferences(NodeRef node);
    
    /**
     * This method adds the specified custom reference instance between the specified nodes.
     * 
     * @param fromNode
     * @param toNode
     * @param assocId the server-side qname e.g. {http://www.alfresco.org/model/rmcustom/1.0}abcd-12-efgh-4567
     */
    public void addCustomReference(NodeRef fromNode, NodeRef toNode, QName assocId);

    /**
     * This method removes the specified custom reference instance from the specified node.
     * 
     * @param fromNode
     * @param toNode
     * @param assocId the server-side qname e.g. {http://www.alfresco.org/model/rmcustom/1.0}abcd-12-efgh-4567
     */
    public void removeCustomReference(NodeRef fromNode, NodeRef toNode, QName assocId);
    
    /**
     * This method creates a new custom association, using the given label as the title.
     * 
     * @param label the title of the association definition
     * @return the QName of the newly-created association.
     */
    public QName addCustomAssocDefinition(String label);
    
    /**
     * This method creates a new custom child association, combining the given source and
     * target and using the combined String  as the title.
     * 
     * @param source
     * @param target
     * @return the QName of the newly-created association.
     */
    public QName addCustomChildAssocDefinition(String source, String target);

    /**
     * This method updates the source and target values for the specified child association.
     * The source and target will be combined into a single string and stored in the title property.
     * Source and target are String metadata for RM parent/child custom references.
     * 
     * @param refQName qname of the child association.
     * @param newSource the new value for the source field.
     * @param newTarget the new value for the target field.
     * @see #getCompoundIdFor(String, String)
     * @see #splitSourceTargetId(String)
     */
    public QName updateCustomChildAssocDefinition(QName refQName, String newSource, String newTarget);

    /**
     * This method updates the label value for the specified association.
     * The label will be stored in the title property.
     * Label is String metadata for bidirectional custom references.
     * 
     * @param refQName qname of the child association.
     * @param newLabel the new value for the label field.
     */
    public QName updateCustomAssocDefinition(QName refQName, String newLabel);

    /**
     * This method returns ConstraintDefinition objects defined in the rmc model
     * (note: not property references or in-line defs)
     * The custom constraint definitions are retrieved from the dictionaryService
     * which is notified of any newly created definitions on transaction commit.
     * Therefore custom constraints created in the current transaction will not appear
     * in the results.
     */
    public List<ConstraintDefinition> getCustomConstraintDefinitions();
    
    /**
     * Get a Constraint Definition object
     */
    public ConstraintDefinition getCustomConstraintDefinition(QName constraintName);

    /**
     * This method adds a Constraint definition to the custom model.
     * The implementation of this method would have to go into the M2Model and insert
     * the relevant M2Objects for this new constraint.
     * 
     * param type not included as it would always be RMListOfValuesConstraint for RM.
     * 
     * @param constraintName the name e.g. rmc:foo
     * @param title the human-readable title e.g. My foo list
     * @param caseSensitive
     * @param allowedValues the allowed values list
     */
    public void addCustomConstraintDefinition(QName constraintName, String title, boolean caseSensitive, List<String> allowedValues);
    
    /**
     * This method would remove a constraint definition from the custom model.
     * The implementation of this method would have to go into the M2Model and
     * remove the specified M2Constraint object from the model.
     * It would be subject to the same limitations as other non-incremental changes.
     * 
     * @param constraintName the name e.g. rmc:foo.
     * 
     * @deprecated currently throws UnsupportedOperationException
     */
    public void removeCustomConstraintDefinition(QName constraintName);
    
    /**
     * This method would change the list of values supported in a custom constraint
     * 
     * @param name the name e.g. rmc:foo of the custom constraint.
     * @param newValues
     */
    public void changeCustomConstraintValues(QName constraintName, List<String> newValues);
    
    /**
     * 
     * @param constraintName
     * @param title
     */
    public void changeCustomConstraintTitle(QName constraintName, String title);
    
    /**
     * This method iterates over the custom properties, references looking for one whose id
     * exactly matches that specified.
     * 
     * @param localName the localName part of the qname of the property or reference definition.
     * @return the QName of the property, association definition which matches, or null.
     */
    public QName getQNameForClientId(String localName);

    /**
     * Given a compound id for source and target strings (as used with parent/child
     * custom references), this method splits the String and returns an array containing
     * the source and target IDs separately.
     * 
     * @param sourceTargetId the compound ID.
     * @return a String array, where result[0] == sourceId and result[1] == targetId.
     */
    public String[] splitSourceTargetId(String sourceTargetId);

    /**
     * This method retrieves a compound ID (client-side) for the specified
     * sourceId and targetId.
     * 
     * @param sourceId
     * @param targetId
     * @return
     */
    public String getCompoundIdFor(String sourceId, String targetId);
}
