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
package org.alfresco.repo.search.impl.lucene.index;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.util.GUID;
import org.alfresco.util.TempFileProvider;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;

import junit.framework.TestCase;

public class IndexInfoTest extends TestCase
{

    public static final String[] WORD_LIST = { "aardvark", "banana", "cucumber", "daffodil", "emu", "frog", "gibbon",
            "humour", "injection", "jelly", "key", "lemur", "monkey", "number", "open", "plummet", "quest",
            "replication", "steam", "tunnel", "uncommon", "verbose", "where", "xylem", "yellow", "zebra", "alpha",
            "bravo", "charlie", "delta", "echo", "foxtrot", "golf", "hotel", "indigo", "juliet", "kilo", "lima",
            "mike", "november", "oscar", "papa", "quebec", "romeo", "sierra", "tango", "uniform", "victor", "whisky",
            "xray", "yankee", "zulu" };

    public static final String[] CREATE_LIST = { "aardvark", "banana", "cucumber", "daffodil", "emu", "frog", "gibbon",
            "humour", "injection", "jelly", "key", "lemur", "monkey", "number", "open", "plummet", "quest",
            "replication", "steam", "tunnel", "uncommon", "verbose", "where", "xylem", "yellow", "zebra", };

    public static final String[] UPDATE_LIST = { "alpha", "bravo", "charlie", "delta", "echo", "foxtrot", "golf",
            "hotel", "indigo", "juliet", "kilo", "lima", "mike", "november", "oscar", "papa", "quebec", "romeo",
            "sierra", "tango", "uniform", "victor", "whisky", "xray", "yankee", "zulu" };
    
    public static final String[] CREATE_LIST_2 = { "aardvark2", "banana2", "cucumber2", "daffodil2", "emu2", "frog2", "gibbon2",
        "humour2", "injection2", "jelly2", "key2", "lemur2", "monkey2", "number2", "open2", "plummet2", "quest2",
        "replication2", "steam2", "tunnel2", "uncommon2", "verbose2", "where2", "xylem2", "yellow2", "zebra2", };

public static final String[] UPDATE_LIST_2 = { "alpha2", "bravo2", "charlie2", "delta2", "echo2", "foxtrot2", "golf2",
        "hotel2", "indigo2", "juliet2", "kilo2", "lima2", "mike2", "november2", "oscar2", "papa2", "quebec2", "romeo2",
        "sierra2", "tango2", "uniform2", "victor2", "whisky2", "xray2", "yankee2", "zulu2" };

    public IndexInfoTest()
    {
        super();
    }

    public IndexInfoTest(String arg0)
    {
        super(arg0);
    }

    public void testCreateAndSearch() throws IOException
    {
        System.setProperty("disableLuceneLocks", "true");

        // no deletions - create only
        HashSet<NodeRef> deletions = new HashSet<NodeRef>();
        for (int i = 0; i < 0; i++)
        {
            deletions.add(new NodeRef(new StoreRef("woof", "bingle"), GUID.generate()));
        }

        File tempLocation = TempFileProvider.getTempDir();
        File testArea = new File(tempLocation, "IndexInfoTest");
        File testDir = new File(testArea, "" + System.currentTimeMillis());
        final IndexInfo ii = new IndexInfo(testDir);

        for (int i = 0; i < WORD_LIST.length; i++)
        {
            IndexReader reader = ii.getMainIndexReferenceCountingReadOnlyIndexReader();
            assertEquals(reader.numDocs(), i);
            reader.close();

            String guid = GUID.generate();
            ii.setStatus(guid, TransactionStatus.ACTIVE, null, null);
            IndexWriter writer = ii.getDeltaIndexWriter(guid, new StandardAnalyzer());

            Document doc = new Document();
            for (int k = 0; k < 15; k++)
            {
                doc.add(new Field("ID" + k, guid, false, true, false));
            }
            doc.add(new Field("TEXT", WORD_LIST[i], false, true, false));
            writer.addDocument(doc);

            ii.closeDeltaIndexWriter(guid);
            ii.setStatus(guid, TransactionStatus.PREPARING, null, null);
            ii.setPreparedState(guid, deletions, 1);
            ii.getDeletions(guid);
            ii.setStatus(guid, TransactionStatus.PREPARED, null, null);

            reader = ii.getMainIndexReferenceCountingReadOnlyIndexReader();
            assertEquals(reader.numDocs(), i);
            for (int j = 0; j < WORD_LIST.length; j++)
            {
                TermDocs tds = reader.termDocs(new Term("TEXT", WORD_LIST[j]));
                if (j < i)
                {
                    assertTrue(tds.next());
                    assertEquals(tds.doc(), j);
                }
                else
                {
                    assertFalse(tds.next());
                }
            }
            reader.close();

            reader = ii.getMainIndexReferenceCountingReadOnlyIndexReader(guid, deletions);
            assertEquals(reader.numDocs(), i + 1);
            for (int j = 0; j < WORD_LIST.length; j++)
            {
                TermDocs tds = reader.termDocs(new Term("TEXT", WORD_LIST[j]));
                if (j <= i)
                {
                    assertTrue(tds.next());
                    assertEquals(tds.doc(), j);
                }
                else
                {
                    assertFalse(tds.next());
                }
            }
            reader.close();

            ii.setStatus(guid, TransactionStatus.COMMITTING, null, null);
            ii.setStatus(guid, TransactionStatus.COMMITTED, null, null);

            reader = ii.getMainIndexReferenceCountingReadOnlyIndexReader();
            assertEquals(reader.numDocs(), i + 1);
            for (int j = 0; j < WORD_LIST.length; j++)
            {
                TermDocs tds = reader.termDocs(new Term("TEXT", WORD_LIST[j]));
                if (j <= i)
                {
                    assertTrue(tds.next());
                    assertEquals(tds.doc(), j);
                }
                else
                {
                    assertFalse(tds.next());
                }
            }
            reader.close();

        }

    }

    public void testCreateDeleteAndSearch() throws IOException
    {
        assertEquals(CREATE_LIST.length, UPDATE_LIST.length);

        StoreRef storeRef = new StoreRef("woof", "bingle");

        System.setProperty("disableLuceneLocks", "true");

        // no deletions - create only
        ArrayList<NodeRef> nodeRefs = new ArrayList<NodeRef>();

        File tempLocation = TempFileProvider.getTempDir();
        File testArea = new File(tempLocation, "IndexInfoTest");
        File testDir = new File(testArea, "" + System.currentTimeMillis());
        final IndexInfo ii = new IndexInfo(testDir);

        for (int i = 0; i < CREATE_LIST.length; i++)
        {
            IndexReader reader = ii.getMainIndexReferenceCountingReadOnlyIndexReader();
            assertEquals(reader.numDocs(), i);
            reader.close();

            String guid = GUID.generate();
            ii.setStatus(guid, TransactionStatus.ACTIVE, null, null);
            IndexWriter writer = ii.getDeltaIndexWriter(guid, new StandardAnalyzer());

            Document doc = new Document();
            for (int k = 0; k < 15; k++)
            {
                doc.add(new Field("ID" + k, guid, false, true, false));
            }
            doc.add(new Field("TEXT", CREATE_LIST[i], false, true, false));
            NodeRef nodeRef = new NodeRef(storeRef, GUID.generate());
            nodeRefs.add(nodeRef);
            doc.add(new Field("ID", nodeRef.toString(), false, true, false));
            writer.addDocument(doc);

            ii.closeDeltaIndexWriter(guid);
            ii.setStatus(guid, TransactionStatus.PREPARING, null, null);
            ii.setPreparedState(guid, new HashSet<NodeRef>(), 1);
            ii.getDeletions(guid);
            ii.setStatus(guid, TransactionStatus.PREPARED, null, null);

            reader = ii.getMainIndexReferenceCountingReadOnlyIndexReader();
            assertEquals(reader.numDocs(), i);
            for (int j = 0; j < CREATE_LIST.length; j++)
            {
                TermDocs tds = reader.termDocs(new Term("TEXT", CREATE_LIST[j]));
                if (j < i)
                {
                    assertTrue(tds.next());
                    assertEquals(tds.doc(), j);
                }
                else
                {
                    assertFalse(tds.next());
                }
            }
            reader.close();

            reader = ii.getMainIndexReferenceCountingReadOnlyIndexReader(guid, new HashSet<NodeRef>());
            assertEquals(reader.numDocs(), i + 1);
            for (int j = 0; j < CREATE_LIST.length; j++)
            {
                TermDocs tds = reader.termDocs(new Term("TEXT", CREATE_LIST[j]));
                if (j <= i)
                {
                    assertTrue(tds.next());
                    assertEquals(tds.doc(), j);
                }
                else
                {
                    assertFalse(tds.next());
                }
            }
            reader.close();

            ii.setStatus(guid, TransactionStatus.COMMITTING, null, null);
            ii.setStatus(guid, TransactionStatus.COMMITTED, null, null);

            reader = ii.getMainIndexReferenceCountingReadOnlyIndexReader();
            assertEquals(reader.numDocs(), i + 1);
            for (int j = 0; j < CREATE_LIST.length; j++)
            {
                TermDocs tds = reader.termDocs(new Term("TEXT", CREATE_LIST[j]));
                if (j <= i)
                {
                    assertTrue(tds.next());
                    assertEquals(tds.doc(), j);
                }
                else
                {
                    assertFalse(tds.next());
                }
            }
            reader.close();

        }

        for (int i = 0; i < CREATE_LIST.length; i++)
        {
            HashSet<NodeRef> deletions = new HashSet<NodeRef>();
            deletions.add(nodeRefs.get(i));

            IndexReader reader = ii.getMainIndexReferenceCountingReadOnlyIndexReader();
            assertEquals(reader.numDocs(), CREATE_LIST.length - i);
            reader.close();

            String guid = GUID.generate();
            ii.setStatus(guid, TransactionStatus.ACTIVE, null, null);
            ii.closeDeltaIndexWriter(guid);
            ii.setStatus(guid, TransactionStatus.PREPARING, null, null);
            ii.setPreparedState(guid, deletions, 1);
            ii.getDeletions(guid);
            ii.setStatus(guid, TransactionStatus.PREPARED, null, null);

            reader = ii.getMainIndexReferenceCountingReadOnlyIndexReader();
            assertEquals(reader.numDocs(), CREATE_LIST.length - i);
            int lastDoc = -1;
            for (int j = 0; j < CREATE_LIST.length; j++)
            {
                TermDocs tds = reader.termDocs(new Term("TEXT", CREATE_LIST[j]));
                if (j >= i)
                {
                    assertTrue(tds.next());
                    assertTrue(tds.doc() > lastDoc);
                    lastDoc = tds.doc();
                }
                else
                {
                    assertFalse(tds.next());
                }
            }
            reader.close();

            reader = ii.getMainIndexReferenceCountingReadOnlyIndexReader(guid, deletions);
            assertEquals(reader.numDocs(), UPDATE_LIST.length - i - 1);
            lastDoc = -1;
            for (int j = 0; j < CREATE_LIST.length; j++)
            {
                TermDocs tds = reader.termDocs(new Term("TEXT", CREATE_LIST[j]));
                if (j > i)
                {
                    assertTrue(tds.next());
                    assertTrue(tds.doc() > lastDoc);
                    lastDoc = tds.doc();
                }
                else
                {
                    assertFalse(tds.next());
                }
            }

            reader.close();

            ii.setStatus(guid, TransactionStatus.COMMITTING, null, null);
            ii.setStatus(guid, TransactionStatus.COMMITTED, null, null);

            reader = ii.getMainIndexReferenceCountingReadOnlyIndexReader();
            assertEquals(reader.numDocs(), UPDATE_LIST.length - i - 1);
            lastDoc = -1;
            for (int j = 0; j < CREATE_LIST.length; j++)
            {
                TermDocs tds = reader.termDocs(new Term("TEXT", CREATE_LIST[j]));
                if (j > i)
                {
                    assertTrue(tds.next());
                    assertTrue(tds.doc() > lastDoc);
                    lastDoc = tds.doc();
                }
                else
                {
                    assertFalse(tds.next());
                }
            }

            reader.close();

            IndexReader reader1 = ii.getMainIndexReferenceCountingReadOnlyIndexReader();
            IndexReader reader2 = ii.getMainIndexReferenceCountingReadOnlyIndexReader();
            IndexReader reader3 = ii.getMainIndexReferenceCountingReadOnlyIndexReader();
            reader3.close();
            reader2.close();
            reader1.close();

        }

    }

    public void testCreateUpdateAndSearch() throws IOException
    {
        assertEquals(CREATE_LIST.length, UPDATE_LIST.length);

        StoreRef storeRef = new StoreRef("woof", "bingle");

        System.setProperty("disableLuceneLocks", "true");

        // no deletions - create only
        ArrayList<NodeRef> nodeRefs = new ArrayList<NodeRef>();

        File tempLocation = TempFileProvider.getTempDir();
        File testArea = new File(tempLocation, "IndexInfoTest");
        File testDir = new File(testArea, "" + System.currentTimeMillis());
        final IndexInfo ii = new IndexInfo(testDir);

        for (int i = 0; i < CREATE_LIST.length; i++)
        {
            IndexReader reader = ii.getMainIndexReferenceCountingReadOnlyIndexReader();
            assertEquals(reader.numDocs(), i);
            reader.close();

            String guid = GUID.generate();
            ii.setStatus(guid, TransactionStatus.ACTIVE, null, null);
            IndexWriter writer = ii.getDeltaIndexWriter(guid, new StandardAnalyzer());

            Document doc = new Document();
            for (int k = 0; k < 15; k++)
            {
                doc.add(new Field("ID" + k, guid, false, true, false));
            }
            doc.add(new Field("TEXT", CREATE_LIST[i], false, true, false));
            NodeRef nodeRef = new NodeRef(storeRef, GUID.generate());
            nodeRefs.add(nodeRef);
            doc.add(new Field("ID", nodeRef.toString(), false, true, false));
            writer.addDocument(doc);

            ii.closeDeltaIndexWriter(guid);
            ii.setStatus(guid, TransactionStatus.PREPARING, null, null);
            ii.setPreparedState(guid, new HashSet<NodeRef>(), 1);
            ii.getDeletions(guid);
            ii.setStatus(guid, TransactionStatus.PREPARED, null, null);

            reader = ii.getMainIndexReferenceCountingReadOnlyIndexReader();
            assertEquals(reader.numDocs(), i);
            for (int j = 0; j < CREATE_LIST.length; j++)
            {
                TermDocs tds = reader.termDocs(new Term("TEXT", CREATE_LIST[j]));
                if (j < i)
                {
                    assertTrue(tds.next());
                    assertEquals(tds.doc(), j);
                }
                else
                {
                    assertFalse(tds.next());
                }
            }
            reader.close();

            reader = ii.getMainIndexReferenceCountingReadOnlyIndexReader(guid, new HashSet<NodeRef>());
            assertEquals(reader.numDocs(), i + 1);
            for (int j = 0; j < CREATE_LIST.length; j++)
            {
                TermDocs tds = reader.termDocs(new Term("TEXT", CREATE_LIST[j]));
                if (j <= i)
                {
                    assertTrue(tds.next());
                    assertEquals(tds.doc(), j);
                }
                else
                {
                    assertFalse(tds.next());
                }
            }
            reader.close();

            ii.setStatus(guid, TransactionStatus.COMMITTING, null, null);
            ii.setStatus(guid, TransactionStatus.COMMITTED, null, null);

            reader = ii.getMainIndexReferenceCountingReadOnlyIndexReader();
            assertEquals(reader.numDocs(), i + 1);
            for (int j = 0; j < CREATE_LIST.length; j++)
            {
                TermDocs tds = reader.termDocs(new Term("TEXT", CREATE_LIST[j]));
                if (j <= i)
                {
                    assertTrue(tds.next());
                    assertEquals(tds.doc(), j);
                }
                else
                {
                    assertFalse(tds.next());
                }
            }
            reader.close();

        }

        for (int i = 0; i < UPDATE_LIST.length; i++)
        {
            HashSet<NodeRef> deletions = new HashSet<NodeRef>();
            deletions.add(nodeRefs.get(i));

            IndexReader reader = ii.getMainIndexReferenceCountingReadOnlyIndexReader();
            assertEquals(reader.numDocs(), UPDATE_LIST.length);
            reader.close();

            String guid = GUID.generate();
            ii.setStatus(guid, TransactionStatus.ACTIVE, null, null);
            IndexWriter writer = ii.getDeltaIndexWriter(guid, new StandardAnalyzer());

            Document doc = new Document();
            for (int k = 0; k < 15; k++)
            {
                doc.add(new Field("ID" + k, guid, false, true, false));
            }
            doc.add(new Field("TEXT", UPDATE_LIST[i], false, true, false));
            writer.addDocument(doc);

            ii.closeDeltaIndexWriter(guid);
            ii.setStatus(guid, TransactionStatus.PREPARING, null, null);
            ii.setPreparedState(guid, deletions, 1);
            ii.getDeletions(guid);
            ii.setStatus(guid, TransactionStatus.PREPARED, null, null);

            reader = ii.getMainIndexReferenceCountingReadOnlyIndexReader();
            assertEquals(reader.numDocs(), UPDATE_LIST.length);
            int lastDoc = -1;
            for (int j = 0; j < CREATE_LIST.length; j++)
            {
                TermDocs tds = reader.termDocs(new Term("TEXT", CREATE_LIST[j]));
                if (j >= i)
                {
                    assertTrue(tds.next());
                    assertTrue(tds.doc() > lastDoc);
                    lastDoc = tds.doc();
                }
                else
                {
                    assertFalse(tds.next());
                }
            }
            for (int j = 0; j < UPDATE_LIST.length; j++)
            {
                TermDocs tds = reader.termDocs(new Term("TEXT", UPDATE_LIST[j]));
                if (j < i)
                {
                    assertTrue(tds.next());
                    assertTrue(tds.doc() > lastDoc);
                    lastDoc = tds.doc();
                }
                else
                {
                    assertFalse(tds.next());
                }
            }
            reader.close();

            reader = ii.getMainIndexReferenceCountingReadOnlyIndexReader(guid, deletions);
            assertEquals(reader.numDocs(), UPDATE_LIST.length);
            lastDoc = -1;
            for (int j = 0; j < CREATE_LIST.length; j++)
            {
                TermDocs tds = reader.termDocs(new Term("TEXT", CREATE_LIST[j]));
                if (j > i)
                {
                    assertTrue(tds.next());
                    assertTrue(tds.doc() > lastDoc);
                    lastDoc = tds.doc();
                }
                else
                {
                    assertFalse(tds.next());
                }
            }
            for (int j = 0; j < UPDATE_LIST.length; j++)
            {
                TermDocs tds = reader.termDocs(new Term("TEXT", UPDATE_LIST[j]));
                if (j <= i)
                {
                    assertTrue(tds.next());
                    assertTrue(tds.doc() > lastDoc);
                    lastDoc = tds.doc();
                }
                else
                {
                    assertFalse(tds.next());
                }
            }

            reader.close();

            ii.setStatus(guid, TransactionStatus.COMMITTING, null, null);
            ii.setStatus(guid, TransactionStatus.COMMITTED, null, null);

            reader = ii.getMainIndexReferenceCountingReadOnlyIndexReader();
            assertEquals(reader.numDocs(), UPDATE_LIST.length);
            lastDoc = -1;
            for (int j = 0; j < CREATE_LIST.length; j++)
            {
                TermDocs tds = reader.termDocs(new Term("TEXT", CREATE_LIST[j]));
                if (j > i)
                {
                    assertTrue(tds.next());
                    assertTrue(tds.doc() > lastDoc);
                    lastDoc = tds.doc();
                }
                else
                {
                    assertFalse(tds.next());
                }
            }
            for (int j = 0; j < UPDATE_LIST.length; j++)
            {
                TermDocs tds = reader.termDocs(new Term("TEXT", UPDATE_LIST[j]));
                if (j <= i)
                {
                    assertTrue(tds.next());
                    assertTrue(tds.doc() > lastDoc);
                    lastDoc = tds.doc();
                }
                else
                {
                    assertFalse(tds.next());
                }
            }
            reader.close();

        }

    }

    public void testMultiThreadedCreateAndSearch()
    {
        
        System.setProperty("disableLuceneLocks", "true");

        File tempLocation = TempFileProvider.getTempDir();
        File testArea = new File(tempLocation, "IndexInfoTest");
        File testDir = new File(testArea, "" + System.currentTimeMillis());
        final IndexInfo ii = new IndexInfo(testDir);
        
        Thread thread1 = new Thread(new Test(ii, CREATE_LIST, UPDATE_LIST));
        Thread thread2 = new Thread(new Test(ii, CREATE_LIST_2, UPDATE_LIST_2));
        thread1.start();
        thread2.start();
        try
        {
            thread1.join();
            thread2.join();
        }
        catch (InterruptedException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static class Test implements Runnable
    {
        String[] create;
        String[] update;
        IndexInfo ii;
        
        Test(IndexInfo ii, String[] create, String[] update)
        {
            this.ii = ii;
            this.create = create;
            this.update = update;
        }
        
        public void run()
        {
            try
            {
                assertEquals(create.length, update.length);
                
                StoreRef storeRef = new StoreRef("woof", "bingle");

                // no deletions - create only
                ArrayList<NodeRef> nodeRefs = new ArrayList<NodeRef>();

                for (int i = 0; i < create.length; i++)
                {
                    IndexReader reader = ii.getMainIndexReferenceCountingReadOnlyIndexReader();
                    reader.close();

                    String guid = GUID.generate();
                    ii.setStatus(guid, TransactionStatus.ACTIVE, null, null);
                    IndexWriter writer = ii.getDeltaIndexWriter(guid, new StandardAnalyzer());

                    Document doc = new Document();
                    for (int k = 0; k < 15; k++)
                    {
                        doc.add(new Field("ID" + k, guid, false, true, false));
                    }
                    doc.add(new Field("TEXT", create[i], false, true, false));
                    NodeRef nodeRef = new NodeRef(storeRef, GUID.generate());
                    nodeRefs.add(nodeRef);
                    doc.add(new Field("ID", nodeRef.toString(), false, true, false));
                    writer.addDocument(doc);

                    ii.closeDeltaIndexWriter(guid);
                    ii.setStatus(guid, TransactionStatus.PREPARING, null, null);
                    ii.setPreparedState(guid, new HashSet<NodeRef>(), 1);
                    ii.getDeletions(guid);
                    ii.setStatus(guid, TransactionStatus.PREPARED, null, null);

                    reader = ii.getMainIndexReferenceCountingReadOnlyIndexReader();
                    
                    int lastDoc = -1;
                    
                    for (int j = 0; j < create.length; j++)
                    {
                        TermDocs tds = reader.termDocs(new Term("TEXT", create[j]));
                        if (j < i)
                        {
                            assertTrue(tds.next());
                            assertTrue(tds.doc() > lastDoc);
                            lastDoc = tds.doc();
                        }
                        else
                        {
                            assertFalse(tds.next());
                        }
                    }
                    reader.close();

                    reader = ii.getMainIndexReferenceCountingReadOnlyIndexReader(guid, new HashSet<NodeRef>());
                    lastDoc = -1;
                    for (int j = 0; j < create.length; j++)
                    {
                        TermDocs tds = reader.termDocs(new Term("TEXT", create[j]));
                        if (j <= i)
                        {
                            assertTrue(tds.next());
                            assertTrue(tds.doc() > lastDoc);
                            lastDoc = tds.doc();
                        }
                        else
                        {
                            assertFalse(tds.next());
                        }
                    }
                    reader.close();

                    ii.setStatus(guid, TransactionStatus.COMMITTING, null, null);
                    ii.setStatus(guid, TransactionStatus.COMMITTED, null, null);

                    reader = ii.getMainIndexReferenceCountingReadOnlyIndexReader();
                    lastDoc = -1;
                    for (int j = 0; j < create.length; j++)
                    {
                        TermDocs tds = reader.termDocs(new Term("TEXT", create[j]));
                        if (j <= i)
                        {
                            assertTrue(tds.next());
                            assertTrue(tds.doc() > lastDoc);
                            lastDoc = tds.doc();
                        }
                        else
                        {
                            assertFalse(tds.next());
                        }
                    }
                    reader.close();

                }

                for (int i = 0; i < update.length; i++)
                {
                    HashSet<NodeRef> deletions = new HashSet<NodeRef>();
                    deletions.add(nodeRefs.get(i));

                    IndexReader reader = ii.getMainIndexReferenceCountingReadOnlyIndexReader();
                  
                    reader.close();

                    String guid = GUID.generate();
                    ii.setStatus(guid, TransactionStatus.ACTIVE, null, null);
                    IndexWriter writer = ii.getDeltaIndexWriter(guid, new StandardAnalyzer());

                    Document doc = new Document();
                    for (int k = 0; k < 15; k++)
                    {
                        doc.add(new Field("ID" + k, guid, false, true, false));
                    }
                    doc.add(new Field("TEXT", update[i], false, true, false));
                    writer.addDocument(doc);

                    ii.closeDeltaIndexWriter(guid);
                    ii.setStatus(guid, TransactionStatus.PREPARING, null, null);
                    ii.setPreparedState(guid, deletions, 1);
                    ii.getDeletions(guid);
                    ii.setStatus(guid, TransactionStatus.PREPARED, null, null);

                    reader = ii.getMainIndexReferenceCountingReadOnlyIndexReader();
                   
                    int lastDoc = -1;
                    for (int j = 0; j < create.length; j++)
                    {
                        TermDocs tds = reader.termDocs(new Term("TEXT", create[j]));
                        if (j >= i)
                        {
                            assertTrue(tds.next());
                            assertTrue(tds.doc() > lastDoc);
                            lastDoc = tds.doc();
                        }
                        else
                        {
                            assertFalse(tds.next());
                        }
                    }
                    for (int j = 0; j < update.length; j++)
                    {
                        TermDocs tds = reader.termDocs(new Term("TEXT", update[j]));
                        if (j < i)
                        {
                            assertTrue(tds.next());
                            assertTrue(tds.doc() > lastDoc);
                            lastDoc = tds.doc();
                        }
                        else
                        {
                            assertFalse(tds.next());
                        }
                    }
                    reader.close();

                    reader = ii.getMainIndexReferenceCountingReadOnlyIndexReader(guid, deletions);
                    
                    lastDoc = -1;
                    for (int j = 0; j < create.length; j++)
                    {
                        TermDocs tds = reader.termDocs(new Term("TEXT", create[j]));
                        if (j > i)
                        {
                            assertTrue(tds.next());
                            assertTrue(tds.doc() > lastDoc);
                            lastDoc = tds.doc();
                        }
                        else
                        {
                            assertFalse(tds.next());
                        }
                    }
                    for (int j = 0; j < update.length; j++)
                    {
                        TermDocs tds = reader.termDocs(new Term("TEXT", update[j]));
                        if (j <= i)
                        {
                            assertTrue(tds.next());
                            assertTrue(tds.doc() > lastDoc);
                            lastDoc = tds.doc();
                        }
                        else
                        {
                            assertFalse(tds.next());
                        }
                    }

                    reader.close();

                    ii.setStatus(guid, TransactionStatus.COMMITTING, null, null);
                    ii.setStatus(guid, TransactionStatus.COMMITTED, null, null);

                    reader = ii.getMainIndexReferenceCountingReadOnlyIndexReader();
                   
                    lastDoc = -1;
                    for (int j = 0; j < create.length; j++)
                    {
                        TermDocs tds = reader.termDocs(new Term("TEXT", create[j]));
                        if (j > i)
                        {
                            assertTrue(tds.next());
                            assertTrue(tds.doc() > lastDoc);
                            lastDoc = tds.doc();
                        }
                        else
                        {
                            assertFalse(tds.next());
                        }
                    }
                    for (int j = 0; j < update.length; j++)
                    {
                        TermDocs tds = reader.termDocs(new Term("TEXT", update[j]));
                        if (j <= i)
                        {
                            assertTrue(tds.next());
                            assertTrue(tds.doc() > lastDoc);
                            lastDoc = tds.doc();
                        }
                        else
                        {
                            assertFalse(tds.next());
                        }
                    }
                    reader.close();

                }

            }
            catch (IOException e)
            {
                System.exit(-1);
            }
        }

    }
}
