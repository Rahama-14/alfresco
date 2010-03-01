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
package org.alfresco.repo.content.transform.magick;

/**
 * Image resize options
 * 
 * @author Roy Wetherall
 */
public class ImageResizeOptions
{
    /** The width */
    private int width = -1;
    
    /** The height */
    private int height = -1;
    
    /** Indicates whether the aspect ratio of the image should be maintained */
    private boolean maintainAspectRatio = true;
    
    /** Indicates whether this is a percentage resize */
    private boolean percentResize = false;
    
    /** Indicates whether the resized image is a thumbnail */
    private boolean resizeToThumbnail = false;
    
    /**
     * Defatult constructor
     */
    public ImageResizeOptions()
    {
    }
    
    public void setWidth(int width)
    {
        this.width = width;
    }
    
    public int getWidth()
    {
        return width;
    }
    
    public void setHeight(int height)
    {
        this.height = height;
    }
    
    public int getHeight()
    {
        return height;
    }
    
    public void setMaintainAspectRatio(boolean maintainAspectRatio)
    {
        this.maintainAspectRatio = maintainAspectRatio;
    }
    
    public boolean isMaintainAspectRatio()
    {
        return maintainAspectRatio;
    }
    
    public void setPercentResize(boolean percentResize)
    {
        this.percentResize = percentResize;
    }
    
    public boolean isPercentResize()
    {
        return percentResize;
    }
    
    public void setResizeToThumbnail(boolean resizeToThumbnail)
    {
        this.resizeToThumbnail = resizeToThumbnail;
    }
    
    public boolean isResizeToThumbnail()
    {
        return resizeToThumbnail;
    }    
}
