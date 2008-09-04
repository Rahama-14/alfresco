/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
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
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.cmis.dictionary;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;

import org.alfresco.cmis.CMISCardinalityEnum;
import org.alfresco.cmis.CMISPropertyTypeEnum;
import org.alfresco.cmis.CMISUpdatabilityEnum;
import org.alfresco.repo.dictionary.IndexTokenisationMode;
import org.alfresco.repo.dictionary.constraint.ListOfValuesConstraint;
import org.alfresco.repo.dictionary.constraint.StringLengthConstraint;
import org.alfresco.repo.search.impl.lucene.analysis.DateTimeAnalyser;
import org.alfresco.repo.search.impl.lucene.analysis.DoubleAnalyser;
import org.alfresco.repo.search.impl.lucene.analysis.FloatAnalyser;
import org.alfresco.repo.search.impl.lucene.analysis.IntegerAnalyser;
import org.alfresco.repo.search.impl.lucene.analysis.LongAnalyser;
import org.alfresco.repo.search.impl.lucene.analysis.PathAnalyser;
import org.alfresco.repo.search.impl.lucene.analysis.VerbatimAnalyser;
import org.alfresco.service.cmr.dictionary.Constraint;
import org.alfresco.service.cmr.dictionary.ConstraintDefinition;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.namespace.QName;

/**
 * A CMIS property definition
 * 
 * @author andyh
 */
public class CMISPropertyDefinition implements Serializable
{
    /**
     * 
     */
    private static final long serialVersionUID = -8119257313852558466L;

    private String propertyName;

    private String propertyId;
    
    private String displayName;

    private String description;
    
    private boolean isInherited;

    private CMISPropertyTypeEnum propertyType;

    private CMISCardinalityEnum cardinality;

    private int maximumLength = -1;

    private String schemaURI = null;

    private String encoding = null;

    private Collection<CMISChoice> choices = new HashSet<CMISChoice>();

    private boolean isOpenChoice = false;

    private boolean required;

    private String defaultValue;

    private CMISUpdatabilityEnum updatability;

    private boolean queryable;

    private boolean orderable;

    public CMISPropertyDefinition(CMISDictionaryService cmisDictionary, QName propertyQName, QName typeQName)
    {
        CMISMapping cmisMapping = cmisDictionary.getCMISMapping();
        PropertyDefinition propDef = cmisDictionary.getDictionaryService().getProperty(propertyQName);
        if (propDef.getContainerClass().getName().equals(CMISMapping.RELATIONSHIP_QNAME))
        {
            // Properties of associations - all the same
            propertyName = cmisMapping.getCmisPropertyName(propertyQName);
            propertyId = cmisMapping.getCmisPropertyId(propertyQName);
            displayName = (propDef.getTitle() != null) ? propDef.getTitle() : propertyName;
            description = propDef.getDescription();
            isInherited = false;
            propertyType = cmisMapping.getPropertyType(propertyQName);
            cardinality = propDef.isMultiValued() ? CMISCardinalityEnum.MULTI_VALUED : CMISCardinalityEnum.SINGLE_VALUED;
            required = propDef.isMandatory();
            defaultValue = propDef.getDefaultValue();
            updatability = propDef.isProtected() ? CMISUpdatabilityEnum.READ_ONLY : CMISUpdatabilityEnum.READ_AND_WRITE;
            queryable = false;
            orderable = false;
        }
        else
        {

            propertyName = cmisMapping.getCmisPropertyName(propertyQName);
            propertyId = cmisMapping.getCmisPropertyId(propertyQName);
            displayName = (propDef.getTitle() != null) ? propDef.getTitle() : propertyName;
            description = propDef.getDescription();
            if(propDef.getContainerClass().isAspect())
            {
                isInherited = false;
            }
            else
            {
                isInherited = !propDef.getContainerClass().equals(typeQName);
            }
            propertyType = cmisMapping.getPropertyType(propertyQName);
            cardinality = propDef.isMultiValued() ? CMISCardinalityEnum.MULTI_VALUED : CMISCardinalityEnum.SINGLE_VALUED;
            for (ConstraintDefinition constraintDef : propDef.getConstraints())
            {
                Constraint constraint = constraintDef.getConstraint();
                if (constraint instanceof ListOfValuesConstraint)
                {
                    int position = 1;  // CMIS is 1 based (according to XSDs)
                    ListOfValuesConstraint lovc = (ListOfValuesConstraint) constraint;
                    for (String allowed : lovc.getAllowedValues())
                    {
                        CMISChoice choice = new CMISChoice(allowed, allowed, position++);
                        choices.add(choice);
                    }
                }
                if (constraint instanceof StringLengthConstraint)
                {
                    StringLengthConstraint slc = (StringLengthConstraint) constraint;
                    maximumLength = slc.getMaxLength();
                }
            }
            required = propDef.isMandatory();
            defaultValue = propDef.getDefaultValue();
            updatability = propDef.isProtected() ? CMISUpdatabilityEnum.READ_ONLY : CMISUpdatabilityEnum.READ_AND_WRITE;
            queryable = propDef.isIndexed();
            if (queryable)
            {
                IndexTokenisationMode indexTokenisationMode = IndexTokenisationMode.TRUE;
                if (propDef.getIndexTokenisationMode() != null)
                {
                    indexTokenisationMode = propDef.getIndexTokenisationMode();
                }
                switch (indexTokenisationMode)
                {
                case BOTH:
                case FALSE:
                    orderable = true;
                    break;
                case TRUE:
                default:
                    String analyserClassName = propDef.getDataType().getAnalyserClassName();
                    if (analyserClassName.equals(DateTimeAnalyser.class.getCanonicalName())
                            || analyserClassName.equals(DoubleAnalyser.class.getCanonicalName()) || analyserClassName.equals(FloatAnalyser.class.getCanonicalName())
                            || analyserClassName.equals(IntegerAnalyser.class.getCanonicalName()) || analyserClassName.equals(LongAnalyser.class.getCanonicalName())
                            || analyserClassName.equals(PathAnalyser.class.getCanonicalName()) || analyserClassName.equals(VerbatimAnalyser.class.getCanonicalName()))
                    {
                        orderable = true;
                    }
                    else
                    {
                        orderable = false;
                    }
                }
            }
            else
            {
                orderable = false;
            }
        }

    }

    /**
     * Get the property name
     * 
     * @return
     */
    public String getPropertyName()
    {
        return propertyName;
    }

    /**
     * Get the property id
     * 
     * @return
     */
    public String getPropertyId()
    {
        return propertyId;
    }

    /**
     * Get the display name
     * 
     * @return
     */
    public String getDisplayName()
    {
        return displayName;
    }

    /**
     * Get the description
     * 
     * @return
     */
    public String getDescription()
    {
        return description;
    }
    
    /**
     * Is the property definition inherited?
     * 
     * @return
     */
    public boolean isInherited()
    {
        return isInherited;
    }

    /**
     * Get the property type
     * 
     * @return
     */
    public CMISPropertyTypeEnum getPropertyType()
    {
        return propertyType;
    }

    /**
     * Get the cardinality
     * 
     * @return
     */
    public CMISCardinalityEnum getCardinality()
    {
        return cardinality;
    }

    /**
     * For variable length properties, get the maximum length allowed. Unsupported.
     * 
     * @return
     */
    public int getMaximumLength()
    {
        return maximumLength;
    }

    /**
     * For properties of type CMISPropertyType.XML the schema to which the property must conform. Unsupported
     * 
     * @return - the schema URI
     */
    public String getSchemaURI()
    {
        return schemaURI;
    }

    /**
     * For properties of type CMISPropertyType.XML the encoding used for the property value
     * 
     * @return the encoding
     */
    public String getEncoding()
    {
        return encoding;
    }

    /**
     * Get the choices available as values for this property TODO: not implemented yet
     * 
     * @return
     */
    public Collection<CMISChoice> getChoices()
    {
        return choices;
    }

    /**
     * Is this a choice where a user can enter other values (ie a list with common options)
     * 
     * @return
     */
    public boolean isOpenChoice()
    {
        return isOpenChoice;
    }

    /**
     * Is this property required?
     * 
     * @return
     */
    public boolean isRequired()
    {
        return required;
    }

    /**
     * get the default value as a String
     * 
     * @return
     */
    public String getDefaultValue()
    {
        return defaultValue;
    }

    /**
     * Is this property updatable?
     * 
     * @return
     */
    public CMISUpdatabilityEnum getUpdatability()
    {
        return updatability;
    }

    /**
     * Is this property queryable?
     * 
     * @return
     */
    public boolean isQueryable()
    {
        return queryable;
    }

    /**
     * Is this property orderable in queries?
     * 
     * @return
     */
    public boolean isOrderable()
    {
        return orderable;
    }

    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("CMISPropertyDefinition[");
        builder.append("PropertyName=").append(getPropertyName()).append(", ");
        builder.append("PropertyId=").append(getPropertyId()).append(", ");
        builder.append("DisplayName=").append(getDisplayName()).append(", ");
        builder.append("Description=").append(getDescription()).append(", ");
        builder.append("IsInherited=").append(isInherited()).append(", ");
        builder.append("PropertyType=").append(getPropertyType()).append(", ");
        builder.append("Cardinality=").append(getCardinality()).append(", ");
        builder.append("MaximumLength=").append(getMaximumLength()).append(", ");
        builder.append("SchemaURI=").append(getSchemaURI()).append(", ");
        builder.append("Encoding=").append(getEncoding()).append(", ");
        builder.append("Choices=").append(getChoices()).append(", ");
        builder.append("IsOpenChoice=").append(isOpenChoice()).append(", ");
        builder.append("Required=").append(isRequired()).append(", ");
        builder.append("Default=").append(getDefaultValue()).append(", ");
        builder.append("Updatable=").append(getUpdatability()).append(", ");
        builder.append("Queryable=").append(isQueryable()).append(", ");
        builder.append("Orderable=").append(isOrderable());
        builder.append("]");
        return builder.toString();
    }

}