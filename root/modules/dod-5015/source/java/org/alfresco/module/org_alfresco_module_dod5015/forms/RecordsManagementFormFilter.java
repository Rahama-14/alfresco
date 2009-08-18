package org.alfresco.module.org_alfresco_module_dod5015.forms;

import java.util.List;
import java.util.Map;

import org.alfresco.module.org_alfresco_module_dod5015.CustomisableRmElement;
import org.alfresco.module.org_alfresco_module_dod5015.DOD5015Model;
import org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementAdminService;
import org.alfresco.repo.forms.FieldGroup;
import org.alfresco.repo.forms.Form;
import org.alfresco.repo.forms.FormData;
import org.alfresco.repo.forms.processor.AbstractFilter;
import org.alfresco.repo.forms.processor.node.ContentModelFormProcessor;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Abstract base class for records management related form filter
 * implementations.
 *
 * @author Gavin Cornwell
 */
public abstract class RecordsManagementFormFilter extends AbstractFilter implements DOD5015Model
{
    /** Logger */
    private static Log logger = LogFactory.getLog(RecordsManagementFormFilter.class);
    
    public static final String CUSTOM_RM_FIELD_GROUP_ID = "rm-custom";
    protected static final FieldGroup CUSTOM_RM_FIELD_GROUP = new FieldGroup(
                CUSTOM_RM_FIELD_GROUP_ID, null, false, false, null);
    
    protected NamespaceService namespaceService;
    protected NodeService nodeService;
    protected RecordsManagementAdminService rmAdminService;
    
    /**
     * Sets the NamespaceService instance
     * 
     * @param namespaceService The NamespaceService instance
     */
    public void setNamespaceService(NamespaceService namespaceService)
    {
        this.namespaceService = namespaceService;
    }
    
    /**
     * Sets the node service 
     * 
     * @param nodeService The NodeService instance
     */
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }
    
    /**
     * Sets the RecordsManagementAdminService instance
     * 
     * @param rmAdminService The RecordsManagementAdminService instance
     */
    public void setRecordsManagementAdminService(RecordsManagementAdminService rmAdminService)
    {
        this.rmAdminService = rmAdminService;
    }

    /*
     * @see org.alfresco.repo.forms.processor.Filter#beforePersist(java.lang.Object, org.alfresco.repo.forms.FormData)
     */
    public void beforePersist(Object item, FormData data)
    {
        // ignored
    }

    /*
     * @see org.alfresco.repo.forms.processor.Filter#beforeGenerate(java.lang.Object, java.util.List, java.util.List, org.alfresco.repo.forms.Form, java.util.Map)
     */
    public void beforeGenerate(Object item, List<String> fields, List<String> forcedFields, 
                Form form, Map<String, Object> context)
    {
        // ignored
    }
    
    /*
     * @see org.alfresco.repo.forms.processor.Filter#afterPersist(java.lang.Object, org.alfresco.repo.forms.FormData, java.lang.Object)
     */
    public void afterPersist(Object item, FormData data, Object persistedObject)
    {
        // ignored
    }

    /**
     * Adds a property definition for each of the custom properties for the given RM type to the given
     * form.
     * 
     * @param rmTypeCustomAspect Enum representing the RM type to add custom properties for
     * @param form The form to add the properties to
     */
    protected void addCustomRMProperties(CustomisableRmElement rmTypeCustomAspect, Form form)
    {
        if (rmTypeCustomAspect != null)
        {
            Map<QName, PropertyDefinition> customProps = this.rmAdminService.getAvailableCustomProperties(
                        rmTypeCustomAspect);
        
            if (logger.isDebugEnabled())
                logger.debug("Found " + customProps.size() + " custom property for " + rmTypeCustomAspect);
            
            // setup field definition for each custom property
            for (PropertyDefinition property : customProps.values())
            {
                ContentModelFormProcessor.generatePropertyField(property, form, null, 
                            CUSTOM_RM_FIELD_GROUP, this.namespaceService);
            }
        }
    }
}
