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
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.repo.jgroups;

import org.jgroups.Channel;
import org.jgroups.Message;

import junit.framework.TestCase;

/**
 * @see AlfrescoJChannelFactoryTest
 * 
 * @author Derek Hulley
 * @since 2.1.3
 */
public class AlfrescoJChannelFactoryTest extends TestCase 
{
    private static byte[] bytes = new byte[65536];
    static
    {
        for (int i = 0; i < bytes.length; i++)
        {
            bytes[i] = 1;
        }
    }

    private AlfrescoJGroupsChannelFactory factory;
    private String appRegion;
    
    @Override
    protected void setUp() throws Exception
    {
        factory = new AlfrescoJGroupsChannelFactory();
        appRegion = getName();
    }
    
    /**
     * Check that the channel is behaving
     */
    private void stressChannel(Channel channel) throws Exception
    {
        System.out.println("Test: " + getName());
        System.out.println("    Channel: " + channel);
        System.out.println("    Cluster: " + channel.getClusterName());
        channel.send(null, null, Boolean.TRUE);
        channel.send(new Message(null, null, bytes));
    }
    
    public void testNoCluster() throws Exception
    {
        Channel channel = AlfrescoJGroupsChannelFactory.getChannel(appRegion);
        stressChannel(channel);
    }
    
    public void testBasicCluster() throws Exception
    {
        AlfrescoJGroupsChannelFactory.changeClusterNamePrefix("blah");
        Channel channel = AlfrescoJGroupsChannelFactory.getChannel(appRegion);
        stressChannel(channel);
    }
    
    public void testHotSwapCluster() throws Exception
    {
        AlfrescoJGroupsChannelFactory.changeClusterNamePrefix("ONE");
        Channel channel1 = AlfrescoJGroupsChannelFactory.getChannel(appRegion);
        stressChannel(channel1);
        AlfrescoJGroupsChannelFactory.changeClusterNamePrefix("TWO");
        Channel channel2 = AlfrescoJGroupsChannelFactory.getChannel(appRegion);
        stressChannel(channel1);
        assertTrue("Channel reference must be the same", channel1 == channel2);
    }
}
