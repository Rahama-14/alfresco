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
package org.alfresco.repo.web.util.paging;

/**
 * Paging.  A utility for maintaining paged indexes for a collection of N items.
 * 
 * There are two types of cursor:
 * 
 * a) Paged
 * 
 * This type of cursor is driven from a page number and page size.  Random access within
 * the collection is possible by jumping straight to a page.  A simple scroll through
 * the collection is supported by iterating through each next page.  
 * 
 * b) Windowed
 * 
 * This type of cursor is driven from a skip row count and maximum number of rows.  Random
 * access is not supported.  The collection of items is simply scrolled through from
 * start to end by iterating through each next set of rows.
 * 
 * In either case, a paging cursor provides a start row and end row which may be used
 * to extract the items for the page from the collection of N items.
 * 
 * A zero (or less) page size or row maximum means "unlimited". 
 * 
 * Zero or one based Page and Rows indexes are supported.  By default, Pages are 1 based and
 * Rows are 0 based.
 *
 * At any time, -1 is returned to represent "out of range" i.e. for next, previous, last page.
 * 
 * Pseudo-code for traversing through a collection of N items (10 at a time):
 * 
 * Paging paging = new Paging();
 * Cursor page = paging.createCursor(N, paging.createPage(1, 10));
 * while (page.isInRange())
 * {
 *    for (long i = page.getStartRow(); i <= page.getEndRow(); i++)
 *    {
 *       ...collection[i]...
 *    }
 *    page = paging.createCursor(N, paging.createPage(page.getNextPage(), page.getPageSize());
 * }
 * 
 * Cursor window = paging.createCursor(N, paging.createWindow(0, 10));
 * while (window.isInRange())
 * {
 *    for (long i = window.getStartRow(); i <= window.getEndRow(); i++)
 *    {
 *       ...collection[i]...
 *    }
 *    window = paging.createCursor(N, paging.createWindow(window.getNextPage(), window.getPageSize());   
 * }
 * 
 * @author davidc
 */
public class Paging
{
    public enum PageType
    {
        PAGE,
        WINDOW
    };
    
    boolean zeroBasedPage = false;
    boolean zeroBasedRow = true;

    /**
     * Sets zero based page index
     * 
     * Note: scoped to this paging cursor instance
     * 
     * @param zeroBasedPage  true => 0 based, false => 1 based
     */
    public void setZeroBasedPage(boolean zeroBasedPage)
    {
        this.zeroBasedPage = zeroBasedPage;
    }

    /**
     * Is zero based page index?
     * 
     * Note: scoped to this paging cursor instance
     *
     * @return true => 0 based, false => 1 based
     */
    public boolean isZeroBasedPage()
    {
        return zeroBasedPage;
    }
    
    /**
     * Sets zero based row index
     * 
     * Note: scoped to this paging cursor instance
     * 
     * @param zeroBasedRow  true => 0 based, false => 1 based
     */
    public void setZeroBasedRow(boolean zeroBasedRow)
    {
        this.zeroBasedRow = zeroBasedRow;
    }

    /**
     * Is zero based row index?
     * 
     * Note: scoped to this paging cursor instance
     *
     * @return true => 0 based, false => 1 based
     */
    public boolean isZeroBasedRow()
    {
        return zeroBasedRow;
    }
    
    /**
     * Create a Page
     * 
     * @param pageNumber  page number
     * @param pageSize  page size
     * @return  the page
     */
    public Page createPage(int pageNumber, int pageSize)
    {
        return new Page(PageType.PAGE, zeroBasedPage, pageNumber, pageSize);
    }
    
    /**
     * Create a Window
     * @param skipRows  number of rows to skip
     * @param maxRows  maximum number of rows in window
     * @return  the window
     */
    public Page createWindow(int skipRows, int maxRows)
    {
        return new Page(PageType.WINDOW, zeroBasedRow, skipRows, maxRows);
    }
    
    /**
     * Create a Cursor
     * 
     * @param totalRows  total number of rows in cursor (< 0 for don't know)
     * @param page  the page / window within cursor
     * @return  the cursor
     */
    public Cursor createCursor(int totalRows, Page page)
    {
        if (page.getType() == PageType.PAGE)
        {
            return new PagedCursor(zeroBasedRow, totalRows, page.zeroBasedIdx, page.startIdx, page.pageSize);
        }
        else if (page.getType() == PageType.WINDOW)
        {
            return new WindowedCursor(zeroBasedRow, totalRows, page.startIdx, page.pageSize);
        }
        return null;
    }
    
    /**
     * Create a Paged Result Set
     * 
     * @param results  the results for the page within the cursor
     * @param cursor  the cursor
     * @return  the paged result set
     */
    public PagedResults createPagedResults(Object[] results, Cursor cursor)
    {
        return new ArrayPagedResults(results, cursor);
    }
    
}
