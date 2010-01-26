/**
 * Copyright (C) 2005-2010 Alfresco Software Limited.
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
 
/**
 * Repository DocumentList component.
 * 
 * @namespace Alfresco
 * @class Alfresco.RepositoryDocumentList
 * @superclass Alfresco.DocumentList
 */
(function()
{
   /**
    * YUI Library aliases
    */
   var Dom = YAHOO.util.Dom;

   /**
    * Alfresco Slingshot aliases
    */
   var $html = Alfresco.util.encodeHTML,
      $links = Alfresco.util.activateLinks,
      $combine = Alfresco.util.combinePaths;

   
   /**
    * RepositoryDocumentList constructor.
    * 
    * @param {String} htmlId The HTML id of the parent element
    * @return {Alfresco.RepositoryDocumentList} The new Records DocumentList instance
    * @constructor
    */
   Alfresco.RepositoryDocumentList = function(htmlId)
   {
      return Alfresco.RepositoryDocumentList.superclass.constructor.call(this, htmlId);
   };
   
   /**
    * Extend Alfresco.DocumentList
    */
   YAHOO.extend(Alfresco.RepositoryDocumentList, Alfresco.DocumentList);

   /**
    * Generate "changeFilter" event mark-up specifically for category changes
    *
    * @method generateCategoryMarkup
    * @param category {Array} category[0] is name, category[1] is qnamePath
    * @return {string} Mark-up for use in node attribute
    */
   Alfresco.DocumentList.generateCategoryMarkup = function RDL_generateCategoryMarkup(category)
   {
      return Alfresco.DocumentList.generateFilterMarkup(
      {
         filterId: "category",
         filterData: $combine(category[1], category[0])
      });
   };

   /**
    * Augment prototype with main class implementation, ensuring overwrite is enabled
    */
   YAHOO.lang.augmentObject(Alfresco.RepositoryDocumentList.prototype,
   {
      /**
       * DataTable Cell Renderers
       */

      /**
       * Returns description/detail custom datacell formatter
       *
       * @method fnRenderCellDescription
       * @override
       * @param scope {object} DataTable owner scope
       */
      fnRenderCellDescription: function RDL_fnRenderCellDescription()
      {
         var scope = this;
         
         /**
          * Description/detail custom datacell formatter
          *
          * @method fnRenderCellDescription
          * @param elCell {object}
          * @param oRecord {object}
          * @param oColumn {object}
          * @param oData {object|string}
          */
         return function RDL_renderCellDescription(elCell, oRecord, oColumn, oData)
         {
            var desc = "", docDetailsUrl, tags, tag, categories, category, i, j;
            var record = oRecord.getData(),
               type = record.type,
               isLink = record.isLink,
               locn = record.location,
               title = "",
               description = record.description || scope.msg("details.description.none");

            // Use title property if it's available
            if (record.title && record.title !== record.displayName)
            {
               title = '<span class="title">(' + $html(record.title) + ')</span>';
            }

            // Link handling
            if (isLink)
            {
               oRecord.setData("displayName", scope.msg("details.link-to", record.displayName));
            }

            if (type == "folder")
            {
               /**
                * Folders
                */
               desc += '<h3 class="filename">' + Alfresco.DocumentList.generateFavourite(scope, oRecord) + '<a href="#" class="filter-change" rel="' + Alfresco.DocumentList.generatePathMarkup(locn) + '">';
               desc += $html(record.displayName) + '</a>' + title + '</h3>';

               if (scope.options.simpleView)
               {
                  /**
                   * Simple View
                   */
                  desc += '<div class="detail"><span class="item-simple"><em>' + scope.msg("details.modified.on") + '</em> ' + Alfresco.util.formatDate(record.modifiedOn, "dd mmmm yyyy") + '</span>';
                  desc += '<span class="item-simple"><em>' + scope.msg("details.by") + '</em> <a href="' + Alfresco.DocumentList.generateUserProfileUrl(record.modifiedByUser) + '">' + $html(record.modifiedBy) + '</a></span></div>';
               }
               else
               {
                  /**
                   * Detailed View
                   */
                  desc += '<div class="detail"><span class="item"><em>' + scope.msg("details.modified.on") + '</em> ' + Alfresco.util.formatDate(record.modifiedOn) + '</span>';
                  desc += '<span class="item"><em>' + scope.msg("details.modified.by") + '</em> <a href="' + Alfresco.DocumentList.generateUserProfileUrl(record.modifiedByUser) + '">' + $html(record.modifiedBy) + '</a></span></div>';
                  desc += '<div class="detail"><span class="item"><em>' + scope.msg("details.description") + '</em> ' + $links($html(description)) + '</span></div>';

                  /* Categories */
                  categories = record.categories;
                  desc += '<div class="detail"><span class="item category-item"><em>' + scope.msg("details.categories") + '</em> ';
                  if (categories.length > 0)
                  {
                     for (i = 0, j = categories.length; i < j; i++)
                     {
                        category = categories[i];
                        desc += '<span class="category"><a href="#" class="filter-change" rel="' + Alfresco.DocumentList.generateCategoryMarkup(category) + '">' + $html(category[0]) + '</a></span>' + (j - i > 1 ? ", " : "");
                     }
                  }
                  else
                  {
                     desc += scope.msg("details.categories.none");
                  }
                  desc += '</span></div>';

                  /* Tags */
                  tags = record.tags;
                  desc += '<div class="detail"><span class="item tag-item"><em>' + scope.msg("details.tags") + '</em> ';
                  if (tags.length > 0)
                  {
                     for (i = 0, j = tags.length; i < j; i++)
                     {
                        tag = $html(tags[i]);
                        desc += '<span class="tag"><a href="#" class="tag-link" rel="' + tag + '" title="' + tags[i] + '">' + tag + '</a></span>' + (j - i > 1 ? ", " : "");
                     }
                  }
                  else
                  {
                     desc += scope.msg("details.tags.none");
                  }
                  desc += '</span></div>';
               }
            }
            else
            {
               /**
                * Documents and Links
                */
               docDetailsUrl = scope.getActionUrls(oRecord).documentDetailsUrl;

               // Locked / Working Copy handling
               if (record.lockedByUser === Alfresco.constants.USERNAME)
               {
                  desc += '<div class="info-banner">' + scope.msg("details.banner." + (record.actionSet === "lockOwner" ? "lock-owner" : "editing")) + '</div>';
               }
               else if (record.lockedByUser && record.lockedByUser !== "")
               {
                  desc += '<div class="info-banner">' + scope.msg("details.banner.locked", '<a href="' + Alfresco.DocumentList.generateUserProfileUrl(record.lockedByUser) + '">' + $html(record.lockedBy) + '</a>') + '</div>';
               }

               desc += '<h3 class="filename">' + Alfresco.DocumentList.generateFavourite(scope, oRecord) + '<span id="' + scope.id + '-preview-' + oRecord.getId() + '"><a href="' + docDetailsUrl + '">';
               desc += $html(record.displayName) + '</a></span>' + title + '</h3>';

               if (scope.options.simpleView)
               {
                  /**
                   * Simple View
                   */
                  desc += '<div class="detail"><span class="item-simple"><em>' + scope.msg("details.modified.on") + '</em> ' + Alfresco.util.formatDate(record.modifiedOn, "dd mmmm yyyy") + '</span>';
                  desc += '<span class="item-simple"><em>' + scope.msg("details.by") + '</em> <a href="' + Alfresco.DocumentList.generateUserProfileUrl(record.modifiedByUser) + '">' + $html(record.modifiedBy) + '</a></span></div>';
               }
               else
               {
                  /**
                   * Detailed View
                   */
                  if (record.custom.isWorkingCopy)
                  {
                     /**
                      * Working Copy
                      */
                     desc += '<div class="detail">';
                     desc += '<span class="item"><em>' + scope.msg("details.editing-started.on") + '</em> ' + Alfresco.util.formatDate(record.modifiedOn) + '</span>';
                     desc += '<span class="item"><em>' + scope.msg("details.editing-started.by") + '</em> <a href="' + Alfresco.DocumentList.generateUserProfileUrl(record.modifiedByUser) + '">' + $html(record.modifiedBy) + '</a></span>';
                     desc += '<span class="item"><em>' + scope.msg("details.size") + '</em> ' + Alfresco.util.formatFileSize(record.size) + '</span>';
                     desc += '</div><div class="detail">';
                     desc += '<span class="item"><em>' + scope.msg("details.description") + '</em> ' + $links($html(description)) + '</span>';
                     desc += '</div>';
                  }
                  else
                  {
                     /**
                      * Non-Working Copy
                      */
                     desc += '<div class="detail">';
                     desc += '<span class="item"><em>' + scope.msg("details.modified.on") + '</em> ' + Alfresco.util.formatDate(record.modifiedOn) + '</span>';
                     desc += '<span class="item"><em>' + scope.msg("details.modified.by") + '</em> <a href="' + Alfresco.DocumentList.generateUserProfileUrl(record.modifiedByUser) + '">' + $html(record.modifiedBy) + '</a></span>';
                     desc += '<span class="item"><em>' + scope.msg("details.version") + '</em> ' + record.version + '</span>';
                     desc += '<span class="item"><em>' + scope.msg("details.size") + '</em> ' + Alfresco.util.formatFileSize(record.size) + '</span>';
                     desc += '</div><div class="detail">';
                     desc += '<span class="item"><em>' + scope.msg("details.description") + '</em> ' + $links($html(description)) + '</span>';
                     desc += '</div>';

                     /* Categories */
                     categories = record.categories;
                     desc += '<div class="detail"><span class="item category-item"><em>' + scope.msg("details.categories") + '</em> ';
                     if (categories.length > 0)
                     {
                        for (i = 0, j = categories.length; i < j; i++)
                        {
                           category = categories[i];
                           desc += '<span class="category"><a href="#" class="filter-change" rel="' + Alfresco.DocumentList.generateCategoryMarkup(category) + '">' + $html(category[0]) + '</a></span>' + (j - i > 1 ? ", " : "");
                        }
                     }
                     else
                     {
                        desc += scope.msg("details.categories.none");
                     }
                     desc += '</span></div>';

                     /* Tags */
                     tags = record.tags;
                     desc += '<div class="detail"><span class="item tag-item"><em>' + scope.msg("details.tags") + '</em> ';
                     if (tags.length > 0)
                     {
                        for (i = 0, j = tags.length; i < j; i++)
                        {
                           tag = $html(tags[i]);
                           desc += '<span class="tag"><a href="#" class="tag-link" rel="' + tag + '" title="' + tags[i] + '">' + tag + '</a></span>' + (j - i > 1 ? ", " : "");
                        }
                     }
                     else
                     {
                        desc += scope.msg("details.tags.none");
                     }
                     desc += '</span></div>';
                  }
               }
            }
            elCell.innerHTML = desc;
         };
      },

      /**
       * Set multiple initialization options at once.
       *
       * @method setOptions
       * @override
       * @param obj {object} Object literal specifying a set of options
       * @return {Alfresco.RepositoryDocumentList} returns 'this' for method chaining
       */
      setOptions: function RDL_setOptions(obj)
      {
         return Alfresco.RepositoryDocumentList.superclass.setOptions.call(this, YAHOO.lang.merge(
         {
            workingMode: Alfresco.doclib.MODE_REPOSITORY
         }, obj));
      },


      /**
       * PRIVATE FUNCTIONS
       */

      /**
       * Build URI parameter string for doclist JSON data webscript
       *
       * @override
       * @method _buildDocListParams
       * @param p_obj.page {string} Page number
       * @param p_obj.pageSize {string} Number of items per page
       * @param p_obj.path {string} Path to query
       * @param p_obj.type {string} Filetype to filter: "all", "documents", "folders"
       * @param p_obj.site {string} Current site
       * @param p_obj.container {string} Current container
       * @param p_obj.filter {string} Current filter
       */
      _buildDocListParams: function RDL__buildDocListParams(p_obj)
      {
         // Essential defaults
         var obj = 
         {
            nodeRef: this.options.nodeRef,
            path: this.currentPath,
            type: this.options.showFolders ? "all" : "documents",
            filter: this.currentFilter
         };
         
         // Pagination in use?
         if (this.options.usePagination)
         {
            obj.page = this.widgets.paginator.getCurrentPage() || this.currentPage;
            obj.pageSize = this.widgets.paginator.getRowsPerPage();
         }

         // Passed-in overrides
         if (typeof p_obj == "object")
         {
            obj = YAHOO.lang.merge(obj, p_obj);
         }

         // Build the URI stem
         var params = YAHOO.lang.substitute("{type}/node/{nodeRef}" + (obj.filter.filterId == "path" || obj.filter.filterId == "category" ? "{path}" : ""),
         {
            type: encodeURIComponent(obj.type),
            nodeRef: obj.nodeRef.uri,
            path: $combine("/", Alfresco.util.encodeURIPath(obj.path))
         });

         // Filter parameters
         params += "?filter=" + encodeURIComponent(obj.filter.filterId);
         if (obj.filter.filterData && obj.filter.filterId !== "path")
         {
            params += "&filterData=" + encodeURIComponent(obj.filter.filterData);             
         }
         
         // Paging parameters
         if (this.options.usePagination)
         {
            params += "&size=" + obj.pageSize  + "&pos=" + obj.page;
         }
         
         // No-cache
         params += "&noCache=" + new Date().getTime();
         
         // Repository mode (don't resolve Site-based folders)
         params += "&libraryRoot=" + encodeURIComponent(this.options.nodeRef.toString());

         return params;
      }
   }, true);
})();
