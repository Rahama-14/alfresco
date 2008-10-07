/*
 * Copyright (C) 2005-2008 Alfresco Software Limited.
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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.sf.acegisecurity.Authentication;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.avm.AVMException;
import org.alfresco.service.cmr.avm.AVMNodeDescriptor;
import org.alfresco.service.cmr.avm.AVMService;
import org.alfresco.service.cmr.avm.AVMStoreDescriptor;
import org.alfresco.service.cmr.repository.ContentIOException;


/**
 * This is another tester designed to emulate more typical use patterns.
 * @author britt
 */
class AVMCrawler implements Runnable
{
    /**
     * The AVMService to use.
     */
    private AVMService fService;
    
    private Authentication authentication;

    /**
     * The Operation count.
     */
    private int fOpCount;

    /**
     * Whether we are done.
     */
    private boolean fDone;

    /**
     * Whether an error has occurred.
     */
    private boolean fError;
    private String fErrorStackTrace = null;

    /**
     * Random number generator.
     */
    private Random fRandom;

    /**
     * Make up a new one.
     * @param service The AVMService.
     */
    public AVMCrawler(AVMService service, Authentication authentication)
    {
        fService = service;
        this.authentication = authentication;
        fOpCount = 0;
        fDone = false;
        fError = false;
        fRandom = new Random();
    }

    /**
     * Tell this thread it is done.
     */
    public void setDone()
    {
        fDone = true;
    }

    /**
     * Is this thread in an error state.
     */
    public boolean getError()
    {
        return fError;
    }
    
    /**
     * Get error stack trace
     */
    public String getErrorStackTrace()
    {
        return fErrorStackTrace;
    }
    
    /**
     * Implementation of run.
     */
    public void run()
    {
        try
        {
            AuthenticationUtil.setCurrentAuthentication(authentication);
            while (!fDone)
            {
                doCrawl();
            }
        }
        catch (Throwable t)
        {
            t.printStackTrace();
            
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            
            fError = true;
            fErrorStackTrace = sw.toString();
        }
        finally
        {
            AuthenticationUtil.clearCurrentSecurityContext();
        }
    }

    /**
     * Do one crawl.
     */
    public void doCrawl()
    {
        try
        {
            List<AVMStoreDescriptor> reps = fService.getStores();
            fOpCount++;
            
            if (reps.size() == 0)
            {
                System.out.println("No AVM stores");
                return;
            }
            
            AVMStoreDescriptor repDesc = reps.get(fRandom.nextInt(reps.size()));
            Map<String, AVMNodeDescriptor> rootListing = fService.getDirectoryListing(-1, repDesc.getName() + ":/");
            fOpCount++;
            // Get all the directories in the root.
            List<AVMNodeDescriptor> dirs = new ArrayList<AVMNodeDescriptor>();
            for (AVMNodeDescriptor desc : rootListing.values())
            {
                if (desc.isDirectory())
                {
                    dirs.add(desc);
                }
            }
            
            if (dirs.size() == 0)
            {
                System.out.println("No dirs in root: "+repDesc.getName() + ":/");
            }
            else
            {
                AVMNodeDescriptor dir = dirs.get(fRandom.nextInt(dirs.size()));
                int depth = 1;
                while (dir != null)
                {
                    Map<String, AVMNodeDescriptor> listing = fService.getDirectoryListing(-1, dir.getPath());
                    fOpCount++;
                    List<AVMNodeDescriptor> files = new ArrayList<AVMNodeDescriptor>();
                    dirs = new ArrayList<AVMNodeDescriptor>();
                    for (AVMNodeDescriptor desc : listing.values())
                    {
                        if (desc.isDirectory())
                        {
                            dirs.add(desc);
                        }
                        else
                        {
                            files.add(desc);
                        }
                    }
                    // Read some files if there are any.
                    if (files.size() > 0)
                    {
                        for (int i = 0; i < 6; i++)
                        {
                            BufferedReader
                                reader = new BufferedReader
                                (new InputStreamReader
                                 (fService.getFileInputStream(-1, files.get(fRandom.nextInt(files.size())).getPath())));
                            fOpCount++;
                            String line = reader.readLine();
                            System.out.println(line);
                            reader.close();
                        }
                        // Modify some files.
                        for (int i = 0; i < 2; i++)
                        {
                            String path = files.get(fRandom.nextInt(files.size())).getPath();
                            System.out.println("Modifying: " + path);
                            PrintStream out = new PrintStream(fService.getFileOutputStream(path));
                            out.println("I am " + path);
                            out.close();
                            fOpCount++;
                        }
                    }
                    if (fRandom.nextInt(depth) < depth - 1)
                    {
                        // Create some files.
                        for (int i = 0; i < 1; i++)
                        {
                            String name = randomName();
                            if (listing.containsKey(name))
                            {
                                break;
                            }
                            System.out.println("Creating File: " + name);
                            fService.createFile(dir.getPath(), name, 
                                new ByteArrayInputStream(("I am " + name).getBytes()));
                            fOpCount++;
                        }
                    }
                    // 1 in 100 times create a directory.
                    if (fRandom.nextInt(100) == 0)
                    {
                        String name = randomName();
                        if (listing.containsKey(name))
                        {
                            break;
                        }
                        System.out.println("Creating Directory: " + name);
                        fService.createDirectory(dir.getPath(), name);
                        fOpCount++;
                    }
                    if (listing.size() > 0)
                    {
                        // 1 in 100 times remove something
                        if (fRandom.nextInt(100) == 0)
                        {
                            List<String> names = new ArrayList<String>(listing.keySet());
                            String name = names.get(fRandom.nextInt(names.size()));
                            System.out.println("Removing: " + name);
                            fService.removeNode(dir.getPath(), 
                                                name);
                            fOpCount++;
                        }
                    }
                    if (dirs.size() > 0)
                    {
                        dir = dirs.get(fRandom.nextInt(dirs.size()));
                    }
                    else
                    {
                        dir = null;
                    }
                    depth++;
                }
            }
            
            if (fRandom.nextInt(16) == 0)
            {
                System.out.println("Snapshotting: " + repDesc.getName());
                fService.createSnapshot(repDesc.getName(), null, null);
                fOpCount++;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            if (e instanceof AVMException)
            {
                return;
            }
            if (e instanceof ContentIOException)
            {
                return;
            }
            throw new AVMException("Failure", e);
        }
    }
    
    /**
     * Get a random two character string.
     * @return A random name.
     */
    private String randomName()
    {
        char [] chars = new char[2];
        chars[0] = (char)('a' + fRandom.nextInt(12));
        chars[1] = (char)('a' + fRandom.nextInt(12));
        return new String(chars);
    }
    
    /**
     * Get the operation count.
     */
    public int getOpCount()
    {
        return fOpCount;
    }
}

                
