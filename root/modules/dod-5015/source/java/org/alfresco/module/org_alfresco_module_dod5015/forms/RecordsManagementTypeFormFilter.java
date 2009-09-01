
package org.alfresco.module.org_alfresco_module_dod5015.forms;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.module.org_alfresco_module_dod5015.CustomisableRmElement;
import org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementModel;
import org.alfresco.repo.forms.Form;
import org.alfresco.repo.forms.FormData;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Implementation of a form processor Filter.
 * <p>
 * The filter implements the <code>afterGenerate</code> method to ensure a
 * default unique identifier is provided for the <code>rma:identifier</code>
 * property.
 * </p>
 * <p>
 * The filter also ensures that any custom properties defined for the records
 * management type are provided as part of the Form.
 * </p>
 * 
 * @author Gavin Cornwell
 */
public class RecordsManagementTypeFormFilter extends RecordsManagementFormFilter<TypeDefinition>
{
    /** Logger */
    private static Log logger = LogFactory.getLog(RecordsManagementTypeFormFilter.class);

    /*
     * @see
     * org.alfresco.repo.forms.processor.Filter#afterGenerate(java.lang.Object,
     * java.util.List, java.util.List, org.alfresco.repo.forms.Form,
     * java.util.Map)
     */
    public void afterGenerate(TypeDefinition type, List<String> fields, List<String> forcedFields, Form form,
                Map<String, Object> context)
    {
        QName typeName = type.getName();

        /*
         * if (TYPE_RECORD_SERIES.equals(typeName) ||
         * TYPE_RECORD_CATEGORY.equals(typeName) ||
         * TYPE_RECORD_FOLDER.equals(typeName)) { if (logger.isDebugEnabled())
         * logger.debug("Generating unique identifier for " +
         * typeName.toPrefixString(this.namespaceService)); // find the field
         * definition for the rma:identifier property List<FieldDefinition>
         * fieldDefs = form.getFieldDefinitions(); String identifierPropName =
         * PROP_IDENTIFIER.toPrefixString(this.namespaceService); for
         * (FieldDefinition fieldDef : fieldDefs) { if
         * (fieldDef.getName().equals(identifierPropName)) {
         * fieldDef.setDefaultValue(GUID.generate()); break; } }
         */

        // add any custom properties for the type being created (we don't need
        // to deal with
        // the record type in here as records are typically uploaded and then
        // their metadata
        // edited after the fact)
        if (TYPE_RECORD_SERIES.equals(typeName))
        {
            addCustomRMProperties(CustomisableRmElement.RECORD_SERIES, form);
        }
        else if (TYPE_RECORD_CATEGORY.equals(typeName))
        {
            addCustomRMProperties(CustomisableRmElement.RECORD_CATEGORY, form);
        }
        else if (TYPE_RECORD_FOLDER.equals(typeName))
        {
            addCustomRMProperties(CustomisableRmElement.RECORD_FOLDER, form);
        }
        // }
    }

    /*
     * @see org.alfresco.repo.forms.processor.Filter#afterPersist(java.lang.Object, org.alfresco.repo.forms.FormData, java.lang.Object)
     */
    public void afterPersist(TypeDefinition item, FormData data, NodeRef nodeRef)
    {

        // Once an RM container type has been persisted generate a default
        // identifer for it.
        if (this.nodeService.hasAspect(nodeRef, ASPECT_FILE_PLAN_COMPONENT))
        {
            if (logger.isDebugEnabled())
                logger.debug("Generating unique identifier for "
                            + this.nodeService.getType(nodeRef).toPrefixString(this.namespaceService));

            this.nodeService.setProperty(nodeRef, RecordsManagementModel.PROP_IDENTIFIER, generateIdentifier(nodeRef));
        }
    }

    /**
     * Generates a unique identifier for the given node (based on the dbid).
     * 
     * @param nodeRef The NodeRef to generate a unique id for
     * @return The identifier
     */
    protected String generateIdentifier(NodeRef nodeRef)
    {
        Calendar fileCalendar = Calendar.getInstance();
        String year = Integer.toString(fileCalendar.get(Calendar.YEAR));
        Long dbId = (Long) this.nodeService.getProperty(nodeRef, ContentModel.PROP_NODE_DBID);
        String identifier = year + "-" + padString(dbId.toString(), 10);

        if (logger.isDebugEnabled()) logger.debug("Generated '" + identifier + "' for unique identifier");

        return identifier;
    }

    /**
     * Function to pad a string with zero '0' characters to the required length
     * 
     * @param s String to pad with leading zero '0' characters
     * @param len Length to pad to
     * @return padded string or the original if already at >=len characters
     */
    protected String padString(String s, int len)
    {
        String result = s;

        for (int i = 0; i < (len - s.length()); i++)
        {
            result = "0" + result;
        }

        return result;
    }
}
