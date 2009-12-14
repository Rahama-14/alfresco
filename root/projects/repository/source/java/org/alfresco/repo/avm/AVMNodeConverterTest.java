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
 * http://www.alfresco.com/legal/licensing" */

package org.alfresco.repo.avm;

import org.alfresco.service.cmr.repository.NodeRef;
import org.springframework.extensions.surf.util.Pair;

import junit.framework.TestCase;

/**
 * Tester of the converter from NodeRef, StoreRef space to AVM space.
 * @author britt
 */
public class AVMNodeConverterTest extends TestCase
{
    /**
     * Test Going betwwen a NodeRef and a version, path pair.
     */
    public void testTranslate()
    {
        String avmPath = "main:/";
        int version = 2;
        NodeRef nodeRef = AVMNodeConverter.ToNodeRef(version, avmPath);
        System.out.println(nodeRef);
        Pair<Integer, String> backOut = AVMNodeConverter.ToAVMVersionPath(nodeRef);
        assertEquals(2, backOut.getFirst().intValue());
        assertEquals(avmPath, backOut.getSecond());
        avmPath = "main:/fista/mista/wisticuff";
        version = -1;
        nodeRef = AVMNodeConverter.ToNodeRef(version, avmPath);
        System.out.println(nodeRef);
        backOut = AVMNodeConverter.ToAVMVersionPath(nodeRef);
        assertEquals(-1, backOut.getFirst().intValue());
        assertEquals(avmPath, backOut.getSecond());
    }
}
