/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.repo.node.integrity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;

/**
 * @see org.alfresco.repo.node.integrity.IntegrityEvent
 * 
 * @author Derek Hulley
 */
public class IntegrityEventTest extends TestCase
{
    private static final String NAMESPACE = "http://test";
    
    private NodeRef nodeRef;
    private QName typeQName;
    private QName qname;
    private IntegrityEvent event;
    
    public void setUp() throws Exception
    {
        nodeRef = new NodeRef("workspace://protocol/ID123");
        typeQName = QName.createQName(NAMESPACE, "SomeTypeQName");
        qname = QName.createQName(NAMESPACE, "qname");
        
        event = new TestIntegrityEvent(null, null, nodeRef, typeQName, qname);
    }
    
    public void testSetFunctionality() throws Exception
    {
        Set<IntegrityEvent> set = new HashSet<IntegrityEvent>(5);
        boolean added = set.add(event);
        assertTrue(added);
        added = set.add(new TestIntegrityEvent(null, null, nodeRef, typeQName, qname));
        assertFalse(added);
    }
    
    private static class TestIntegrityEvent extends AbstractIntegrityEvent
    {
        public TestIntegrityEvent(
                NodeService nodeService,
                DictionaryService dictionaryService,
                NodeRef nodeRef,
                QName typeQName,
                QName qname)
        {
            super(nodeService, dictionaryService, nodeRef, typeQName, qname);
        }

        public void checkIntegrity(List<IntegrityRecord> eventResults)
        {
            throw new UnsupportedOperationException();
        }
    }
}
