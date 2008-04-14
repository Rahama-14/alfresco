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
package org.alfresco.web.scripts;

import java.util.List;

import org.alfresco.web.scripts.Description.FormatStyle;

import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;

/**
 * Custom FreeMarker Template language method.
 * <p>
 * Render script url independent of script hosting environment e.g. render inside / outside
 * portal.
 * <p>
 * Usage: scripturl(String url)
 * 
 * @author davidc
 */
public final class ScriptUrlMethod implements TemplateMethodModelEx
{
    WebScriptRequest req;
    WebScriptResponse res;
    
    /**
     * Construct
     * 
     * @param basePath  base path used to construct absolute url
     */
    public ScriptUrlMethod(WebScriptRequest req, WebScriptResponse res)
    {
        this.req = req;
        this.res = res;
    }
    
    
    /**
     * @see freemarker.template.TemplateMethodModel#exec(java.util.List)
     */
    public Object exec(List args) throws TemplateModelException
    {
        String result = "";
        
        if (args.size() != 0)
        {
            Object arg0 = args.get(0);
            boolean prefixServiceUrl = true;
            if (args.size() == 2 && args.get(1) instanceof TemplateBooleanModel)
            {
               prefixServiceUrl = ((TemplateBooleanModel)args.get(1)).getAsBoolean();
            }
            
            if (arg0 instanceof TemplateScalarModel)
            {
                String arg = ((TemplateScalarModel)arg0).getAsString();
                
                StringBuffer buf = new StringBuffer(128);
                buf.append(prefixServiceUrl ? req.getServicePath() : "");
                buf.append(arg);
                if (arg.length() != 0)
                {
                   if (arg.indexOf('?') == -1)
                   {
                      buf.append('?');
                   }
                   else
                   {
                      buf.append('&');
                   }
                }
                else
                {
                   buf.append('?');
                }
                buf.append("guest=" + (req.isGuest() ? "true" : ""));
                if (req.getFormatStyle() == FormatStyle.argument)
                {
                    buf.append("&format=");
                    buf.append(req.getFormat());
                }
                
                result = res.encodeScriptUrl(buf.toString());
            }
        }
        
        return result;
    }
}
