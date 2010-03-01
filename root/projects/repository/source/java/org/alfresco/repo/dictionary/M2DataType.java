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
package org.alfresco.repo.dictionary;


/**
 * Property Type Definition
 * 
 * @author David Caruana
 *
 */
public class M2DataType
{
    private String name = null;
    private String title = null;
    private String description = null;
    private String analyserClassName = null;
    private String javaClassName = null;
    
    
    /*package*/ M2DataType()
    {
        super();
    }
    

    public String getName()
    {
        return name;
    }
    
    
    public void setName(String name)
    {
        this.name = name;
    }

    
    public String getTitle()
    {
        return title;
    }
    
    
    public void setTitle(String title)
    {
        this.title = title;
    }
    
    
    public String getDescription()
    {
        return description;
    }
    
    
    public void setDescription(String description)
    {
        this.description = description;
    }
    
    
    public String getAnalyserClassName()
    {
        return analyserClassName;
    }
    
    
    public void setAnalyserClassName(String analyserClassName)
    {
        this.analyserClassName = analyserClassName;;
    }

    
    public String getJavaClassName()
    {
        return javaClassName;
    }
    
    
    public void setJavaClassName(String javaClassName)
    {
        this.javaClassName = javaClassName;;
    }
    
}
