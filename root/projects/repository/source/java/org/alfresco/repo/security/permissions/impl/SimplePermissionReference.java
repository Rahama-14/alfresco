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
package org.alfresco.repo.security.permissions.impl;

import java.util.HashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.alfresco.service.namespace.QName;

/**
 * A simple permission reference.
 * 
 * @author andyh
 */
public final class SimplePermissionReference extends AbstractPermissionReference
{   
    /**
     * 
     */
    private static final long serialVersionUID = 637302438293417818L;

    private static ReadWriteLock lock = new ReentrantReadWriteLock();

    private static HashMap<QName, HashMap<String, SimplePermissionReference>> instances = new HashMap<QName, HashMap<String, SimplePermissionReference>>();

    public static SimplePermissionReference getPermissionReference(QName qName, String name)
    {
        lock.readLock().lock();
        try
        {
            HashMap<String, SimplePermissionReference> typed = instances.get(qName);
            if(typed != null)
            {
                SimplePermissionReference instance = typed.get(name);
                if(instance != null)
                {
                    return instance;
                }
            }
        }
        finally
        {
            lock.readLock().unlock();
        }
        
        lock.writeLock().lock();
        try
        {
            HashMap<String, SimplePermissionReference> typed = instances.get(qName);
            if(typed == null)
            {
                typed = new HashMap<String, SimplePermissionReference>();
                instances.put(qName, typed);
            }
            SimplePermissionReference instance = typed.get(name);
            if(instance == null)
            {
                instance = new SimplePermissionReference(qName, name);
                typed.put(name, instance);
            }
            return instance;
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }
    
    
    /*
     * The type
     */
    private QName qName;
    
    /*
     * The name of the permission
     */
    private String name;
    
    
    // TODO: make protected
    public SimplePermissionReference(QName qName, String name)
    {
        super();
        this.qName = qName;
        this.name = name;
    }

    public QName getQName()
    {
        return qName;
    }

    public String getName()
    {
        return name;
    }

}
