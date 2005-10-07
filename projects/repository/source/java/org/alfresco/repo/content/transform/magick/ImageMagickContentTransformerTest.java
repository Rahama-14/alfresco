/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the Mozilla Public License version 1.1 
 * with a permitted attribution clause. You may obtain a
 * copy of the License at
 *
 *   http://www.alfresco.org/legal/license.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.repo.content.transform.magick;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.content.transform.AbstractContentTransformerTest;
import org.alfresco.repo.content.transform.ContentTransformer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @see org.alfresco.repo.content.transform.magick.JMagickContentTransformer
 * 
 * @author Derek Hulley
 */
public class ImageMagickContentTransformerTest extends AbstractContentTransformerTest
{
    private static final Log logger = LogFactory.getLog(ImageMagickContentTransformerTest.class);

    private ImageMagickContentTransformer transformer;
    
    public void onSetUpInTransaction() throws Exception
    {
        transformer = new ImageMagickContentTransformer();
        transformer.setMimetypeMap(mimetypeMap);
        transformer.setConvertCommand("imconvert.exe ${source} ${options} ${target}");
        transformer.init();
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
        if (!transformer.isAvailable())
        {
            return;
        }
        double reliability = 0.0;
        reliability = transformer.getReliability(MimetypeMap.MIMETYPE_IMAGE_GIF, MimetypeMap.MIMETYPE_TEXT_PLAIN);
        assertEquals("Mimetype should not be supported", 0.0, reliability);
        reliability = transformer.getReliability(MimetypeMap.MIMETYPE_IMAGE_GIF, MimetypeMap.MIMETYPE_IMAGE_JPEG);
        assertEquals("Mimetype should be supported", 1.0, reliability);
    }
}
