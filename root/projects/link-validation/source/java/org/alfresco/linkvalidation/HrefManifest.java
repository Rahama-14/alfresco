/*-----------------------------------------------------------------------------
*  Copyright 2007 Alfresco Inc.
*  
*  This program is free software; you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation; either version 2 of the License, or
*  (at your option) any later version.
*  
*  This program is distributed in the hope that it will be useful, but
*  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
*  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
*  
*  You should have received a copy of the GNU General Public License along
*  with this program; if not, write to the Free Software Foundation, Inc.,
*  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.  As a special
*  exception to the terms and conditions of version 2.0 of the GPL, you may
*  redistribute this Program in connection with Free/Libre and Open Source
*  Software ("FLOSS") applications as described in Alfresco's FLOSS exception.
*  You should have received a copy of the text describing the FLOSS exception,
*  and it is also available here:   http://www.alfresco.com/legal/licensing
*  
*  
*  Author  Jon Cox  <jcox@alfresco.com>
*  File    HrefManifest.java
*----------------------------------------------------------------------------*/

package  org.alfresco.linkvalidation;

import java.io.Serializable;
import java.util.List;

/**
*  Contains a (possibly filtered) list of the hrefs within a file.
*  Common uses of this class are to fetch the links in a web page
*  or just the broken ones (i.e.: response status 400-599).
*/
public class HrefManifest implements Serializable
{
    static final long serialVersionUID = 6532525229716576911L;

    protected String       file_;
    protected List<String> hrefs_;

    public  HrefManifest( String        file, 
                          List<String>  hrefs
                        )
    {
        file_   = file;
        hrefs_  = hrefs;
    }

    public String       getFileName()  { return file_; }
    public List<String> getHrefs()     { return hrefs_;}
}
