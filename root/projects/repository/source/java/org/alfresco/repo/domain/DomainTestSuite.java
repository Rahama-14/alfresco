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
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.repo.domain;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.alfresco.repo.domain.audit.AuditDAOTest;
import org.alfresco.repo.domain.contentdata.ContentDataDAOTest;
import org.alfresco.repo.domain.encoding.EncodingDAOTest;
import org.alfresco.repo.domain.hibernate.HibernateSessionHelperTest;
import org.alfresco.repo.domain.locks.LockDAOTest;
import org.alfresco.repo.domain.mimetype.MimetypeDAOTest;

/**
 * Suite for domain-related tests.
 * 
 * @author Derek Hulley
 */
public class DomainTestSuite extends TestSuite
{
    public static Test suite() 
    {
        TestSuite suite = new TestSuite();
        
        suite.addTestSuite(ContentDataDAOTest.class);
        suite.addTestSuite(EncodingDAOTest.class);
        suite.addTestSuite(HibernateSessionHelperTest.class);
        suite.addTestSuite(LockDAOTest.class);
        suite.addTestSuite(MimetypeDAOTest.class);
        suite.addTestSuite(LocaleDAOTest.class);
        suite.addTestSuite(PropertyValueTest.class);
        suite.addTestSuite(QNameDAOTest.class);
        suite.addTestSuite(PropertyValueTest.class);
        suite.addTestSuite(AuditDAOTest.class);
                
        return suite;
    }
}
