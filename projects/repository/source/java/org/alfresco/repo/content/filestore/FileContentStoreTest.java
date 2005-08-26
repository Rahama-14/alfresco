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
package org.alfresco.repo.content.filestore;

import java.io.File;

import org.alfresco.repo.content.AbstractContentReadWriteTest;
import org.alfresco.repo.content.ContentStore;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.util.TempFileProvider;

/**
 * Tests read and write functionality for the store.
 * 
 * @see org.alfresco.repo.content.filestore.FileContentStore
 * 
 * @author Derek Hulley
 */
public class FileContentStoreTest extends AbstractContentReadWriteTest
{
    private ContentStore store;
    private ContentReader reader;
    private ContentWriter writer;
    
    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        
        // create a store that uses a subdirectory of the temp directory
        File tempDir = TempFileProvider.getTempDir();
        store = new FileContentStore(
                tempDir.getAbsolutePath() +
                File.separatorChar +
                getName());
        
        writer = store.getWriter(null);
        String contentUrl = writer.getContentUrl();
        reader = store.getReader(contentUrl);
    }

    public void testSetUp() throws Exception
    {
        super.testSetUp();
        assertNotNull(reader);
        assertNotNull(writer);
    }
    
    @Override
    protected ContentStore getStore()
    {
        return store;
    }

    @Override
    protected ContentReader getReader()
    {
        return reader;
    }

    @Override
    protected ContentWriter getWriter()
    {
        return writer;
    }
}
