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
 * http://www.alfresco.com/legal/licensing
 */

package org.alfresco.repo.deploy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.alfresco.repo.avm.AVMServiceTestBase;
import org.alfresco.repo.avm.util.BulkLoader;
import org.alfresco.service.cmr.avm.deploy.DeploymentCallback;
import org.alfresco.service.cmr.avm.deploy.DeploymentEvent;
import org.alfresco.service.cmr.avm.deploy.DeploymentReport;
import org.alfresco.service.cmr.avm.deploy.DeploymentReportCallback;
import org.alfresco.service.cmr.avm.deploy.DeploymentService;
import org.alfresco.util.Deleter;
import org.alfresco.util.NameMatcher;
import org.springframework.context.support.FileSystemXmlApplicationContext;

/**
 * Test of to filesystem deployment.
 * @author britt
 */
public class FSDeploymentTest extends AVMServiceTestBase
{
    public void testBasic()
        throws Exception
    {
        File log = new File("deplog");
        log.mkdir();
        File metadata = new File("depmetadata");
        metadata.mkdir();
        File data = new File("depdata");
        data.mkdir();
        File target = new File("target");
        target.mkdir();
        try
        {
            @SuppressWarnings("unused")
            FileSystemXmlApplicationContext receiverContext =
                new FileSystemXmlApplicationContext("../deployment/config/application-context.xml");
            DeploymentService service = (DeploymentService)fContext.getBean("DeploymentService");
            NameMatcher matcher = (NameMatcher)fContext.getBean("globalPathExcluder");
            setupBasicTree();
            fService.createFile("main:/a/b", "fudge.bak").close();
            DeploymentReport report = new DeploymentReport();
            List<DeploymentCallback> callbacks = new ArrayList<DeploymentCallback>();
            callbacks.add(new DeploymentReportCallback(report));
            
            service.deployDifferenceFS(-1, "main:/", "localhost", "default", 44100, "Giles", "Watcher", "sampleTarget", matcher, false, false, false, callbacks);
            int count = 0;
            for (DeploymentEvent event : report)
            {
                System.out.println(event);
                count++;
            }
            assertEquals(10, count);
            
            report = new DeploymentReport();
            callbacks = new ArrayList<DeploymentCallback>();
            callbacks.add(new DeploymentReportCallback(report));

            callbacks.add(new DeploymentReportCallback(report));

            service.deployDifferenceFS(-1, "main:/", "localhost", "default", 44100, "Giles", "Watcher", "sampleTarget", matcher, false, false, false, callbacks);
            count = 0;
            for (DeploymentEvent event : report)
            {
                System.out.println(event);
                count++;
            }
            assertEquals(2, count);
            fService.createFile("main:/d", "jonathan").close();
            fService.removeNode("main:/a/b");
            
            report = new DeploymentReport();
            callbacks = new ArrayList<DeploymentCallback>();
            callbacks.add(new DeploymentReportCallback(report));

            
            service.deployDifferenceFS(-1, "main:/", "localhost", "default", 44100, "Giles", "Watcher", "sampleTarget", matcher, false, false, false, callbacks);
            count = 0;
            for (DeploymentEvent event : report)
            {
                System.out.println(event);
                count++;
            }
            assertEquals(4, count);
            fService.removeNode("main:/d/e");
            fService.createFile("main:/d", "e").close();
            
            report = new DeploymentReport();
            callbacks = new ArrayList<DeploymentCallback>();
            callbacks.add(new DeploymentReportCallback(report));

            service.deployDifferenceFS(-1, "main:/", "localhost", "default", 44100, "Giles", "Watcher", "sampleTarget", matcher, false, false, false, callbacks);
            count = 0;
            for (DeploymentEvent event : report)
            {
                System.out.println(event);
                count++;
            }
            assertEquals(3, count);
            fService.removeNode("main:/d/e");
            fService.createDirectory("main:/d", "e");
            fService.createFile("main:/d/e", "Warren.txt").close();
            fService.createFile("main:/d/e", "It's a silly name.txt").close();
            report = new DeploymentReport();
            callbacks = new ArrayList<DeploymentCallback>();
            callbacks.add(new DeploymentReportCallback(report));

            service.deployDifferenceFS(-1, "main:/", "localhost", "default", 44100, "Giles", "Watcher", "sampleTarget", matcher, false, false, false, callbacks);
            count = 0;
            for (DeploymentEvent event : report)
            {
                System.out.println(event);
                count++;
            }
            assertEquals(5, count);
            BulkLoader loader = new BulkLoader();
            loader.setAvmService(fService);
            loader.recursiveLoad("source/java/org/alfresco/repo/avm", "main:/");
            report = new DeploymentReport();
            callbacks = new ArrayList<DeploymentCallback>();
            callbacks.add(new DeploymentReportCallback(report));

            service.deployDifferenceFS(-1, "main:/", "localhost", "default", 44100, "Giles", "Watcher", "sampleTarget", matcher, false, false, false, callbacks);
            fService.removeNode("main:/avm/hibernate");
            fService.getFileOutputStream("main:/avm/AVMServiceTest.java").close();
            report = new DeploymentReport();
            callbacks = new ArrayList<DeploymentCallback>();
            callbacks.add(new DeploymentReportCallback(report));

            service.deployDifferenceFS(-1, "main:/", "localhost", "default", 44100,  "Giles", "Watcher", "sampleTarget", matcher, false, false, false, callbacks);
            count = 0;
            for (DeploymentEvent event : report)
            {
                System.out.println(event);
                count++;
            }
            assertEquals(4, count);
        }
        finally
        {
            Deleter.Delete(log);
            Deleter.Delete(data);
            Deleter.Delete(metadata);
            Deleter.Delete(target);
            File dot = new File(".");
            String[] listing = dot.list();
            for (String name : listing)
            {
                if (name.startsWith("dep-record-"))
                {
                    File file = new File(name);
                    file.delete();
                }
            }
        }
    }
}
