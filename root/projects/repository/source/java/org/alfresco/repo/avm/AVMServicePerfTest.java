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
 * http://www.alfresco.com/legal/licensing" */

package org.alfresco.repo.avm;

import java.io.PrintStream;

/**
 * Performance test(s).
 * @author britt
 */
public class AVMServicePerfTest extends AVMServiceTestBase
{
    public void testSetup() throws Exception
    {
        super.testSetup();
    }
    
    public void testAdd100x10a() throws Throwable
    {
        add(100, 10);
    }
    
    public void testAdd100x10b() throws Throwable
    {
        add(100, 10);
    }
    
    public void testAdd100x10c() throws Throwable
    {
        add(100, 10);
    }
    
    public void testAdd100x10d() throws Throwable
    {
        add(100, 10);
    }
    
    public void testAdd500x2e() throws Throwable
    {
        add(500, 2);
    }
    
    public void testAdd500x4g() throws Throwable
    {
        add(500, 4);
    }
    
    /**
     * Test adding 100 files to each of 10 directories.
     */
    private void add(int fileCnt, int dirCnt) throws Throwable
    {
        try
        {
            if ((dirCnt < 1) || (dirCnt > 10))
            {
                throw new Exception("Invalid ("+dirCnt+") - currently supports between 1 and 10 directories");
            }
            
            String [] dirs = { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j" };
            for (int j = 0; j < dirCnt; j++)
            {
                String dir = dirs[j];
                fService.createDirectory("main:/", dir);
                String ndir = "main:/" + dir;
                fService.createDirectory(ndir, dir);
                ndir = ndir + "/" + dir;
                for (int i = 0; i < fileCnt; i++)
                {
                    PrintStream out = new PrintStream(fService.createFile(ndir, "file" + i));
                    out.println("I am " + ndir + "/file" + i);
                    
                    //System.out.println(ndir + "/file" + i);
                    
                    out.close();
                }
                fService.createSnapshot("main", null, null);
            }
            
            // System.out.println(recursiveList("main", -1));
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            throw e;
        }
    }
}