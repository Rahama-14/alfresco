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
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.repo.forms.processor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Abstract base class for all FormProcessor implementations provides
 * a regex pattern match to test for processor applicability
 *
 * @author Gavin Cornwell
 */
public abstract class AbstractFormProcessor implements FormProcessor
{
    private static final Log logger = LogFactory.getLog(AbstractFormProcessor.class);
    
    protected FormProcessorRegistry processorRegistry;
    protected String matchPattern;
    protected boolean active = true;
    protected Pattern patternMatcher;

    /**
     * Sets the form process registry
     * 
     * @param processorRegistry The FormProcessorRegistry instance
     */
    public void setProcessorRegistry(FormProcessorRegistry processorRegistry)
    {
        this.processorRegistry = processorRegistry;
    }
    
    /**
     * Sets the match pattern
     * 
     * @param pattern The regex pattern to use to determine if this processor is applicable
     */
    public void setMatchPattern(String pattern)
    {
        this.matchPattern = pattern;
    }

    /**
     * Sets whether this processor is active
     * 
     * @param active true if the processor should be active
     */
    public void setActive(boolean active)
    {
        this.active = active;
    }

    /**
     * Registers this processor with the processor registry
     */
    public void register()
    {
        if (this.processorRegistry == null)
        {
            if (logger.isWarnEnabled())
                logger.warn("Property 'processorRegistry' has not been set.  Ignoring auto-registration of processor: " + this);
            
            return;
        }
        
        if (this.matchPattern == null)
        {
            if (logger.isWarnEnabled())
                logger.warn("Property 'matchPattern' has not been set.  Ignoring auto-registration of processor: " + this);
            
            return;
        }
        else
        {
            // setup pattern matcher
            this.patternMatcher = Pattern.compile(this.matchPattern);
        }

        // register this instance
        this.processorRegistry.addProcessor(this);
    }
    
    /*
     * @see org.alfresco.repo.forms.processor.FormProcessor#isActive()
     */
    public boolean isActive()
    {
        return this.active;
    }

    /*
     * @see org.alfresco.repo.forms.processor.FormProcessor#isApplicable(java.lang.String)
     */
    public boolean isApplicable(String item)
    {
        // this form processor matches if the match pattern provided matches
        // the item provided
        
        Matcher matcher = patternMatcher.matcher(item);
        boolean matches = matcher.matches();
        
        if (logger.isDebugEnabled())
            logger.debug("Checking processor " + this + " for applicability for item '" + item + "', result = " + matches);
        
        return matches;
    }
    
    /*
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        StringBuilder buffer = new StringBuilder(super.toString());
        buffer.append(" (");
        buffer.append("active=").append(this.active);
        buffer.append(", matchPattern=").append(this.matchPattern);
        buffer.append(")");
        return buffer.toString();
    }
}
