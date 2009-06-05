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

package org.alfresco.web.bean.wcm.preview;

import org.alfresco.config.JNDIConstants;
import org.alfresco.web.bean.wcm.AVMUtil;


/**
 * A PreviewURIService that constructs a virtualisation server URI.
 *
 * @author Peter Monks (peter.monks@alfresco.com)
 * @version $Id$
 */
public class VirtualisationServerPreviewURIService
    implements PreviewURIService
{
    /**
     * @see org.alfresco.web.bean.wcm.preview.PreviewURIService#getPreviewURI(java.lang.String, java.lang.String)
     */
    public String getPreviewURI(final String storeId, final String pathToAsset)
    {
        if ((pathToAsset == null) || (pathToAsset.length() == 0))
        {
            return AVMUtil.buildStoreUrl(storeId);
        }
        
        // Sanity checking
        if (!pathToAsset.startsWith('/' + JNDIConstants.DIR_DEFAULT_WWW + '/' + JNDIConstants.DIR_DEFAULT_APPBASE))
        {
            throw new IllegalStateException("Invalid asset path in AVM node ref: " + storeId + ":" + pathToAsset);
        }

        return AVMUtil.buildAssetUrl(storeId, pathToAsset);
    }

}

