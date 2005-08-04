/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 * You may obtain a copy of the License at
 *
 *     http://www.gnu.org/licenses/lgpl.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.repo.action;

import java.io.Serializable;

import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.action.ParameterType;

/**
 * Parameter definition implementation class.
 * 
 * @author Roy Wetherall
 */
public class ParameterDefinitionImpl implements ParameterDefinition, Serializable
{
    /** 
     * Serial version UID
     */
    private static final long serialVersionUID = 3976741384558751799L;

    /**
     * The name of the parameter
     */
    private String name;
    
    /**
     * The type of the parameter
     */
    private ParameterType type;
    
    /**
     * The display label
     */
    private String displayLabel;
	
	/**
	 * Indicates whether it is mandatory for the parameter to be set
	 */
	private boolean isMandatory = false;

    /**
     * Constructor
     * 
     * @param name          the name of the parameter
     * @param type          the type of the parameter
     * @param displayLabel  the display label
     */
    public ParameterDefinitionImpl(
            String name, 
            ParameterType type,
            boolean isMandatory,
            String displayLabel)
    {
        this.name = name;
        this.type = type;
        this.displayLabel = displayLabel;
		this.isMandatory = isMandatory;
    }
    
    /**
     * @see org.alfresco.service.cmr.action.ParameterDefinition#getName()
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * @see org.alfresco.service.cmr.action.ParameterDefinition#getType()
     */
    public ParameterType getType()
    {
        return this.type;
    }
	
	/**
	 * @see org.alfresco.service.cmr.action.ParameterDefinition#isMandatory()
	 */
	public boolean isMandatory() 
	{
		return this.isMandatory;
	}

    /**
     * @see org.alfresco.service.cmr.action.ParameterDefinition#getDisplayLabel()
     */
    public String getDisplayLabel()
    {
        return this.displayLabel;
    }
}
