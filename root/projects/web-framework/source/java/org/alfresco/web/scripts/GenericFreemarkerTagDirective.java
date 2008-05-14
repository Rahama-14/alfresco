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
 * http://www.alfresco.com/legal/licensing
 */
package org.alfresco.web.scripts;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.tagext.Tag;

import org.alfresco.util.ReflectionHelper;
import org.alfresco.web.site.RequestContext;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;

/**
 * A generic Freemarker Directive wrapper around the Java Tag classes
 * 
 * @author muzquiano
 */
public class GenericFreemarkerTagDirective extends FreemarkerTagSupportDirective
{
    public GenericFreemarkerTagDirective(RequestContext context, String tagName, String tagClassName)
    {
        this(context);
        setTagName(tagName);
        setTagClassName(tagClassName);
    }

    public GenericFreemarkerTagDirective(RequestContext context)
    {
        super(context);
    }
    
    protected String tagName;
    protected String tagClassName;
    
    public void setTagClassName(String tagClassName)
    {
        this.tagClassName = tagClassName;
    }
    
    public String getTagClassName()
    {
        return this.tagClassName;
    }
    
    public void setTagName(String tagName)
    {
        this.tagName = tagName;
    }
    
    public String getTagName()
    {
        return this.tagName;
    }
    
    public void execute(Environment env, Map params, TemplateModel[] loopVars,
            TemplateDirectiveBody body) throws TemplateException, IOException
    {
        // instantiate the tag class
        Tag tag = (Tag) ReflectionHelper.newObject(getTagClassName());
        
        if(tag != null)
        {
            // set directive parameters onto the tag
            Iterator it = params.keySet().iterator();
            while(it.hasNext())
            {
                String name = (String) it.next();
                TemplateModel value = (TemplateModel) params.get(name);

                // If the value is a scalar, we can do the set
                // However, we can only do Strings (at the moment)
                if(value instanceof TemplateScalarModel)
                {
                    // TODO: Totally improve how this is done
                    String method = "set" + name.substring(0,1).toUpperCase() + name.substring(1, name.length());
                    Class[] argTypes = new Class[] { String.class };
                    String v = ((TemplateScalarModel)value).getAsString();
                    String[] args = new String[] { v };
                    ReflectionHelper.invoke(tag, method, argTypes, args);
                }
                else
                {
                    throw new TemplateModelException("The '"+name+"' parameter to the '" + getTagName() + "' directive must be a string.");                    
                }
            }
        }
        
        // copy in body content (if there is any)
        String bodyContentString = null;
        if(body != null)
        {
            if(tag instanceof BodyTagSupport)
            {
                // dump out the Freemarker body content
                StringWriter bodyStringWriter = new StringWriter();
                body.render(bodyStringWriter);
                bodyContentString = bodyStringWriter.toString();
            }
        }

        /**
         * Execute the tag
         */
        try
        {
            String output = executeTag(tag, bodyContentString);
            env.getOut().write(output);
            env.getOut().flush();
        }
        catch(Exception ex)
        {
            throw new TemplateException("Unable to process tag and commit output", ex, env);
        }        
    }
}
