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
package org.alfresco.util.exec;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.util.exec.RuntimeExec.ExecutionResult;

import junit.framework.TestCase;

/**
 * @see org.alfresco.util.exec.RuntimeExec
 * 
 * @author Derek Hulley
 */
public class RuntimeExecTest extends TestCase
{
    public void testStreams() throws Exception
    {
        RuntimeExec exec = new RuntimeExec();
        
        // This test will return different results on Windows and Linux!
        // note that some Unix variants will error without a path
        HashMap<String, String[]> commandMap = new HashMap<String, String[]>(5);
        commandMap.put("*", new String[] {"find", "/", "-maxdepth", "1", "-name", "var"});
        commandMap.put("Windows.*", new String[] {"find", "/?"});
        exec.setCommandsAndArguments(commandMap);
        // execute
        ExecutionResult ret = exec.execute();
        
        String out = ret.getStdOut();
        String err = ret.getStdErr();
        
        assertEquals("Didn't expect error code", 0, ret.getExitValue());
        assertEquals("Didn't expect any error output", 0, err.length());
        assertTrue("No output found", out.length() > 0);
    }

    public void testWildcard() throws Exception
    {
        RuntimeExec exec = new RuntimeExec();

        // set the command
        Map<String, String[]> commandMap = new HashMap<String, String[]>(3, 1.0f);
        commandMap.put(".*", (new String[]{"TEST"}));
        exec.setCommandsAndArguments(commandMap);
        
        String[] commandStr = exec.getCommand();
        assertTrue("Expected default match to work", Arrays.deepEquals(new String[] {"TEST"}, commandStr));
    }
    
    public void testWithProperties() throws Exception
    {
        RuntimeExec exec = new RuntimeExec();

        // set the command
        Map<String, String[]> commandMap = new HashMap<String, String[]>(3, 1.0f);
        commandMap.put("Windows.*", new String[]{"dir", "${path}"});
        commandMap.put("Linux", new String[] {"ls", "${path}"});
        commandMap.put("Mac OS X", new String[]{"ls", "${path}"});
        commandMap.put("*", new String[]{"wibble", "${path}"});
        exec.setCommandsAndArguments(commandMap);
        
        // set the default properties
        Map<String, String> defaultProperties = new HashMap<String, String>(1, 1.0f);
        defaultProperties.put("path", ".");
        exec.setDefaultProperties(defaultProperties);
        
        // check that the command lines generated are correct
        String defaultCommand[] = exec.getCommand();
        String dynamicCommand[] = exec.getCommand(Collections.singletonMap("path", "./"));
        // check
        String os = System.getProperty("os.name");
        String[] defaultCommandCheck = null;
        String[] dynamicCommandCheck = null;
        if (os.matches("Windows.*"))
        {
            defaultCommandCheck = new String[]{"dir", "."};
            dynamicCommandCheck = new String[]{"dir", "./"};
        }
        else if (os.equals("Linux") || os.equals("Mac OS X"))
        {
            defaultCommandCheck = new String[]{"ls", "."};
            dynamicCommandCheck = new String[]{"ls", "./"};
        }
        else
        {
            defaultCommandCheck = new String[]{"wibble", "."};
            dynamicCommandCheck = new String[]{"wibble", "./"};
        }
        assertTrue("Default command for OS " + os + " is incorrect", Arrays.deepEquals(defaultCommandCheck, defaultCommand));
        assertTrue("Dynamic command for OS " + os + " is incorrect", Arrays.deepEquals(dynamicCommandCheck, dynamicCommand));
    }
}
