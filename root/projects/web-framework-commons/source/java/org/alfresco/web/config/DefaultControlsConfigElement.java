/*
 * Copyright (C) 2005-2008 Alfresco Software Limited.
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
package org.alfresco.web.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.config.ConfigElement;
import org.alfresco.config.ConfigException;
import org.alfresco.config.element.ConfigElementAdapter;

/**
 * Custom config element that represents &lt;default-controls&gt; values for the
 * client.
 * 
 * @author Neil McErlean.
 */
public class DefaultControlsConfigElement extends ConfigElementAdapter
{
    public static final String CONFIG_ELEMENT_ID = "default-controls";
    private static final long serialVersionUID = -6758804774427314050L;

    private final Map<String, Control> datatypeDefCtrlMappings = new LinkedHashMap<String, Control>();

    /**
     * This constructor creates an instance with the default name.
     */
    public DefaultControlsConfigElement()
    {
        super(CONFIG_ELEMENT_ID);
    }

    /**
     * This constructor creates an instance with the specified name.
     * 
     * @param name the name for the ConfigElement.
     */
    public DefaultControlsConfigElement(String name)
    {
        super(name);
    }

    /**
     * @see org.alfresco.config.ConfigElement#getChildren()
     */
    @Override
    public List<ConfigElement> getChildren()
    {
        throw new ConfigException(
                "Reading the default-controls config via the generic interfaces is not supported");
    }

    /**
     * @see org.alfresco.config.ConfigElement#combine(org.alfresco.config.ConfigElement)
     */
    @Override
    public ConfigElement combine(ConfigElement configElement)
    {
        // There is an assumption here that it is only like-with-like combinations
        // that are allowed. i.e. Only an instance of a DefaultControlsConfigElement
        // can be combined with this.
        DefaultControlsConfigElement otherDCCElement = (DefaultControlsConfigElement) configElement;

        DefaultControlsConfigElement result = new DefaultControlsConfigElement();

        for (String nextDataType : datatypeDefCtrlMappings.keySet())
        {
            String nextTemplate = getTemplateFor(nextDataType);
            Control nextDefaultControls = otherDCCElement.datatypeDefCtrlMappings
                    .get(nextDataType);
            List<ControlParam> nextControlParams = null;
            List<String> cssDeps = null;
            List<String> jsDeps = null;
            if (nextDefaultControls != null)
            {
                nextControlParams = nextDefaultControls.getControlParams();

                final String[] cssDepsArray = nextDefaultControls.getCssDependencies();
                final String[] jsDepsArray = nextDefaultControls.getJsDependencies();
                cssDeps = cssDepsArray == null ? null : Arrays.asList(cssDepsArray);
                jsDeps = jsDepsArray == null ? null : Arrays.asList(jsDepsArray);
            }
            
            result.addDataMapping(nextDataType, nextTemplate,
                            nextControlParams, cssDeps, jsDeps);
        }

        for (String nextDataType : otherDCCElement.datatypeDefCtrlMappings.keySet())
        {
            String nextTemplate = otherDCCElement.getTemplateFor(nextDataType);
            Control nextDefaultControls = otherDCCElement.datatypeDefCtrlMappings
                    .get(nextDataType);
            List<ControlParam> nextControlParams = null;
            List<String> cssDeps = null;
            List<String> jsDeps = null;
            if (nextDefaultControls != null)
            {
                nextControlParams = nextDefaultControls.getControlParams();
                
                final String[] cssDepsArray = nextDefaultControls.getCssDependencies();
                final String[] jsDepsArray = nextDefaultControls.getJsDependencies();
                cssDeps = cssDepsArray == null ? null : Arrays.asList(cssDepsArray);
                jsDeps = jsDepsArray == null ? null : Arrays.asList(jsDepsArray);
            }
            
            result.addDataMapping(nextDataType, nextTemplate,
                            nextControlParams, cssDeps, jsDeps);
        }

        return result;
    }

    /* package */void addDataMapping(String dataType, String template,
            List<ControlParam> parameters, List<String> cssDeps, List<String> jsDeps)
    {
        if (parameters == null)
        {
            parameters = Collections.emptyList();
        }
        Control newControl = new Control(dataType, template);
        for (ControlParam p : parameters)
        {
            newControl.addControlParam(p);
        }
        newControl.addCssDependencies(cssDeps);
        newControl.addJsDependencies(jsDeps);
        this.datatypeDefCtrlMappings.put(dataType, newControl);
    }
    
    public List<String> getItemNames()
    {
        Set<String> result = datatypeDefCtrlMappings.keySet();
        List<String> resultList = new ArrayList<String>(result);
    	return Collections.unmodifiableList(resultList);
    }
    
    public Map<String, Control> getItems()
    {
    	return Collections.unmodifiableMap(datatypeDefCtrlMappings);
    }

    /**
     * This method returns a String representing the path of the template associated
     * with the given dataType.
     * 
     * @param dataType the dataType for which a template is required.
     * @return the path of the associated template. <code>null</code> if the specified
     *         dataType is <code>null</code> or if there is no registered template.
     */
    public String getTemplateFor(String dataType)
    {
        Control ctrl = datatypeDefCtrlMappings.get(dataType);
        if (ctrl == null)
        {
            return null;
        }
        else
        {
            return ctrl.getTemplate();
        }
    }

    /**
     * This method returns an unmodifiable List of <code>ControlParam</code> objects
     * associated with the specified dataType.
     * 
     * @param dataType the dataType for which control-params are required.
     * @return an unmodifiable List of the associated <code>ControlParam</code> objects.
     * 
     * @see org.alfresco.web.config.ControlParam
     */
    public List<ControlParam> getControlParamsFor(String dataType)
    {
        return Collections.unmodifiableList(datatypeDefCtrlMappings.get(
                dataType).getControlParams());
    }
}
