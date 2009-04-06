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
package org.alfresco.repo.content.transform.magick;

import java.util.Collections;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.content.transform.AbstractContentTransformerTest;
import org.alfresco.repo.content.transform.ContentTransformer;
import org.alfresco.repo.content.transform.ProxyContentTransformer;
import org.alfresco.service.cmr.repository.TransformationOptions;
import org.alfresco.util.exec.RuntimeExec;

/**
 * @see org.alfresco.repo.content.transform.magick.JMagickContentTransformer
 * 
 * @author Derek Hulley
 */
public class ImageMagickContentTransformerTest extends AbstractContentTransformerTest
{
    private ImageMagickContentTransformerWorker worker;
    private ContentTransformer transformer;
    
    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        
        RuntimeExec executer = new RuntimeExec();
        executer.setCommand(new String[] {"imconvert.exe", "${source}", "${options}", "${target}"});
        executer.setDefaultProperties(Collections.singletonMap("options", ""));
        
        this.worker = new ImageMagickContentTransformerWorker();
        worker.setMimetypeService(mimetypeService);
        worker.setExecuter(executer);
        worker.afterPropertiesSet();
        
        ProxyContentTransformer transformer = new ProxyContentTransformer();
        transformer.setMimetypeService(mimetypeService);
        this.transformer = transformer;
        
    }
    
    /**
     * @return Returns the same transformer regardless - it is allowed
     */
    protected ContentTransformer getTransformer(String sourceMimetype, String targetMimetype)
    {
        return transformer;
    }
    
    public void testReliability() throws Exception
    {
        if (!this.worker.isAvailable())
        {
            return;
        }
        boolean reliability = transformer.isTransformable(
                MimetypeMap.MIMETYPE_IMAGE_GIF, MimetypeMap.MIMETYPE_TEXT_PLAIN, new TransformationOptions());
        assertEquals("Mimetype should not be supported", false, reliability);
        reliability = transformer.isTransformable(
                MimetypeMap.MIMETYPE_IMAGE_GIF, MimetypeMap.MIMETYPE_IMAGE_JPEG, new TransformationOptions());
        assertEquals("Mimetype should be supported", true, reliability);
    }
}
