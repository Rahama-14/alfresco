/**
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
 * http://www.alfresco.com/legal/licensing
 */
 
/**
 * Repository Document info component.
 * 
 * @namespace Alfresco
 * @class Alfresco.RepositoryDocumentInfo
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
   var $html = Alfresco.util.encodeHTML;
   
   /**
    * RepositoryDocumentInfo constructor.
    * 
    * @param {String} htmlId The HTML id of the parent element
    * @return {Alfresco.RepositoryDocumentInfo} The new RepositoryDocumentInfo instance
    * @constructor
    */
   Alfresco.RepositoryDocumentInfo = function(htmlId)
   {
      return Alfresco.RepositoryDocumentInfo.superclass.constructor.call(this, htmlId);
   };
   
   YAHOO.extend(Alfresco.RepositoryDocumentInfo, Alfresco.DocumentInfo,
   {
      /**
       * Event handler called when the "documentDetailsAvailable" event is received
       *
       * @override
       * @method: onDocumentDetailsAvailable
       */
      onDocumentDetailsAvailable: function RepositoryDocumentInfo_onDocumentDetailsAvailable(layer, args)
      {
         var docData = args[1].documentDetails;
         
         // render tags values
         var tags = docData.tags,
            tagsHtml = "",
            i, ii;
         
         if (tags.length === 0)
         {
            tagsHtml = Alfresco.util.message("label.none", this.name);
         }
         else
         {
            for (i = 0, ii = tags.length; i < ii; i++)
            {
               tagsHtml += '<div class="tag"><img src="' + Alfresco.constants.URL_CONTEXT + 'components/images/tag-16.png" />';
               tagsHtml += $html(tags[i]) + '</div>';
            }
         }
         
         Dom.get(this.id + "-tags").innerHTML = tagsHtml;
      }
   });
})();