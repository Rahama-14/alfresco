/*-----------------------------------------------------------------------------
*  Copyright 2007-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
*
*
*  Author  Jon Cox  <jcox@alfresco.com>
*  File    HrefDifference.java
*----------------------------------------------------------------------------*/

package org.alfresco.linkvalidation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HrefDifference
{
    /**
    *  The href_status_map_ is a map of URLs the tuple of their
    *  return status & list of dependencies.
    */
    protected HrefStatusMap href_status_map_;  // status of links/maybe dep info

    /**
    *   The href_manifest_ contains a List<HrefManifestEntry> objects.
    *   Each HrefManifestEntry contains a file name,
    *   and possibly a list of hrefs within that file.
    */
    protected HrefManifest  href_manifest_;

    // Lazily computed values
    protected HrefManifest broken_in_newmod_;    // errors in new files
    protected HrefManifest broken_by_deletion_;  // err via new deletions

    // Only computed when merging diffs
    protected List<String> repaired_by_delmod_;  // fix by removing links
    protected List<String> repaired_by_new_;     // file satisfied broken dep

    // Temp values used in lazy computation
    protected HashMap<String, List<String>>  broken_manifest_map_;
    protected HashMap<String,String>         deleted_file_md5_;


    String href_attr_;          // href attribute lookup prefix

    int    src_version_;
    String src_store_;

    int    dst_base_version_;
    int    dst_latest_version_;
    String dst_store_;

    String src_webapp_url_base_;
    String dst_webapp_url_base_;

    HrefDifference(String href_attr,
                   int    src_version,
                   String src_store,
                   int    dst_base_version,
                   String dst_store,
                   int    dst_latest_version,
                   String src_webapp_url_base,
                   String dst_webapp_url_base)
    {
        href_attr_               = href_attr;

        src_version_             = src_version;
        src_store_               = src_store;

        dst_base_version_        = dst_base_version;
        dst_store_               = dst_store;

        dst_latest_version_      = dst_latest_version;

        src_webapp_url_base_     = src_webapp_url_base;
        dst_webapp_url_base_     = dst_webapp_url_base;

        href_manifest_           = new HrefManifest();
        href_status_map_         = new HrefStatusMap();

        broken_manifest_map_     = new HashMap<String, List<String>>();
        deleted_file_md5_        = new HashMap<String,String>();
    }


    public HrefManifest getHrefManifest()   { return href_manifest_;   }
    public HrefStatusMap getHrefStatusMap() { return href_status_map_; }
    public int getSrcVersion()              { return src_version_;}
    public int getDstBaseVersion()          { return dst_base_version_;}
    public int getDstLatestVersion()        { return dst_latest_version_;}

    String getHrefAttr()                    { return href_attr_;}
    String getSrcStore()                    { return src_store_;}
    String getDstStore()                    { return dst_store_;}
    String getSrcWebappUrlBase()            { return src_webapp_url_base_; }
    String getDstWebappUrlBase()            { return dst_webapp_url_base_; }


    Map<String,String> getDeletedFileMd5() { return deleted_file_md5_; }
    Map<String, List<String>> getBrokenManifestMap()
    {
        return broken_manifest_map_;
    }
}

