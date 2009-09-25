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
* YUI Library aliases
* Deliberately named differently to the ones various components and modules use, to avoid unexpected behaviour.
*/
var YUIDom = YAHOO.util.Dom,
   YUIEvent = YAHOO.util.Event,
   YUISelector = YAHOO.util.Selector;

/**
 * Alfresco root namespace.
 * 
 * @namespace Alfresco
 */
// Ensure Alfresco root object exists
if (typeof Alfresco == "undefined" || !Alfresco)
{
   var Alfresco = {};
}

/**
 * Alfresco top-level constants namespace.
 * 
 * @namespace Alfresco
 * @class Alfresco.constants
 */
Alfresco.constants = Alfresco.constants || {};

/**
 * Alfresco top-level component namespace.
 * 
 * @namespace Alfresco
 * @class Alfresco.component
 */
Alfresco.component = Alfresco.component || {};

/**
 * Alfresco top-level dashlet namespace.
 * 
 * @namespace Alfresco
 * @class Alfresco.dashlet
 */
Alfresco.dashlet = Alfresco.dashlet || {};

/**
 * Alfresco top-level module namespace.
 * 
 * @namespace Alfresco
 * @class Alfresco.module
 */
Alfresco.module = Alfresco.module || {};

/**
 * Alfresco top-level util namespace.
 * 
 * @namespace Alfresco
 * @class Alfresco.util
 */
Alfresco.util = Alfresco.util || {};

/**
 * Alfresco top-level logger namespace.
 * 
 * @namespace Alfresco
 * @class Alfresco.logger
 */
Alfresco.logger = Alfresco.logger || {};

/**
 * Alfresco top-level service namespace.
 *
 * @namespace Alfresco
 * @class Alfresco.service
 */
Alfresco.service = Alfresco.service || {};

/**
 * Alfresco top-level thirdparty namespace.
 * Used for importing third party javascript functions
 * 
 * @namespace Alfresco
 * @class Alfresco.thirdparty
 */
Alfresco.thirdparty = Alfresco.thirdparty || {};

/**
 * Alfresco top-level widget namespace.
 * 
 * @namespace Alfresco
 * @class Alfresco.widget
 */
Alfresco.widget = Alfresco.widget || {};

/**
 * Alfresco top-level admin namespace.
 * 
 * @namespace Alfresco
 * @class Alfresco.Admin
 */
Alfresco.admin = Alfresco.admin || {};

/**
 * Alfresco top-level doclib namespace.
 *
 * @namespace Alfresco
 * @class Alfresco.doclib
 */
Alfresco.doclib = Alfresco.doclib || {};

/**
 * Alfresco top-level messages namespace.
 * 
 * @namespace Alfresco
 * @class Alfresco.messages
 */
Alfresco.messages = Alfresco.messages ||
{
   global: null,
   scope: {}
};

/**
 * Appends an array onto an object
 * @method Alfresco.util.appendArrayToObject
 * @param obj {object} Object to be appended to
 * @param arr {array} Array to append/merge onto object
 * @return {object} The appended object
 * @static
 */
Alfresco.util.appendArrayToObject = function(obj, arr)
{
   if (arr)
   {
      for (var i = 0, ii = arr.length; i < ii; i++)
      {
         if (arr[i] !== undefined)
         {
            obj[arr[i]] = true;
         }
      }
   }
   return obj;
};

/**
 * Convert an array into an object
 * @method Alfresco.util.arrayToObject
 * @param arr {array} Array to convert to object
 * @return {object} Object conversion of array
 * @static
 */
Alfresco.util.arrayToObject = function(arr)
{
   var obj = {};
   if (arr)
   {
      for (var i = 0, ii = arr.length; i < ii; i++)
      {
         if (arr[i] !== undefined)
         {
            obj[arr[i]] = true;
         }
      }
   }
   return obj;
};

/**
 * Create empty JavaScript object literal from dotted notation string
 * <pre>e.g. Alfresco.util.dotNotationToObject("org.alfresco.site") returns {"org":{"alfresco":{"site":{}}}}</pre>
 *
 * @method Alfresco.util.dotNotationToObject
 * @param str {string} an dotted notation string
 * @param value {object|string|number} an optional object to set the "deepest" object to
 * @return {object} An empty object literal, build from the dotted notation
 * @static
 */
Alfresco.util.dotNotationToObject = function(str, value)
{
   var object = {}, obj = object;
   if (typeof str === "string")
   {
      var properties = str.split("."), property, i, ii;
      for (i = 0, ii = properties.length - 1; i < ii; i++)
      {
         property = properties[i];
         obj[property] = {};
         obj = obj[property];
      }
      obj[properties[i]] = value !== undefined ? value : null;
   }
   return object;
};

/**
 * Finds the index of an object in an array
 *
 * @method Alfresco.util.findObjectPropertyByName
 * @param obj {object} i.e. {org:{alfresco:{site:"share"}}}
 * @param str {striog} i.e. "org.alfresco.site"
 * @return {object} the value for the property specified by the string, in the example "share" would be returned
 * @static
 */
Alfresco.util.findValueByDotNotation = function(obj, property)
{
   if(property && obj)
   {
      var currObj = obj;
      var props = property.split(".");
      for (var i = 0; i < props.length; i++)
      {
         currObj = currObj[props[i]];
         if (typeof currObj == "undefined")
         {
            return null;
         }
      }
      return currObj;
   }
   return null;
};

/**
 * Check if an array contains an object
 * @method Alfresco.util.arrayContains
 * @param arr {array} Array to convert to object
 * @param el {object} The element to be searched for in the array
 * @return {boolean} True if arr contains el
 * @static
 */
Alfresco.util.arrayContains = function(arr, el)
{
   return Alfresco.util.arrayIndex(arr, el) !== -1;
};

/**
 * Removes element el from array arr
 *
 * @method Alfresco.util.arrayRemove
 * @param arr {array} Array to remove el from
 * @param el {object} The element to be removed
 * @return {boolean} The array now without the element
 * @static
 */
Alfresco.util.arrayRemove = function(arr, el)
{
   var i = Alfresco.util.arrayIndex(arr, el);
   while (i !== -1)
   {
      arr.splice(i, 1);
      i = Alfresco.util.arrayIndex(arr, el);
   }
   return arr;
};

/**
 * Finds the index of an object in an array
 *
 * @method Alfresco.util.arrayIndex
 * @param arr {array} Array to search in
 * @param el {object} The element to find the index for in the array
 * @return {integer} -1 if not found, other wise the index
 * @static
 */
Alfresco.util.arrayIndex = function(arr, el)
{
   if (arr)
   {
      for (var i = 0, ii = arr.length; i < ii; i++)
      {
          if (arr[i] == el)
          {
             return i;
          }
      }
   }
   return -1;
};

/**
 * Asserts param contains a proper value
 * @method Alfresco.util.assertNotEmpty
 * @param param {object} Parameter to assert valid
 * @param message {string} Error message to throw on assertion failure
 * @static
 * @throws {Error}
 */
Alfresco.util.assertNotEmpty = function(param, message)
{
   if (typeof param == "undefined" || !param || param === "")
   {
      throw new Error(message);
   }
};

/**
 * Append multiple parts of a path, ensuring duplicate path separators are removed
 *
 * @method Alfresco.util.combinePaths
 * @param path1 {string} First path
 * @param path2 {string} Second path
 * @param ...
 * @param pathN {string} Nth path
 * @return {string} A string containing the combined paths
 * @static
 */
Alfresco.util.combinePaths = function()
{
   var path = "", i, ii;
   for (i = 0, ii = arguments.length; i < ii; i++)
   {
      path += arguments[i] + "/";
   }
   return path.substring(0, path.length - 1).replace(/\/{2,}/g, "/");
};

/**
 * Constants for conversion between bytes, kilobytes, megabytes and gigabytes
 */
Alfresco.util.BYTES_KB = 1024;
Alfresco.util.BYTES_MB = 1048576;
Alfresco.util.BYTES_GB = 1073741824;

/**
 * Converts a file size in bytes to human readable form
 *
 * @method Alfresco.util.formatFileSize
 * @param fileSize {number} File size in bytes
 * @return {string} The file size in a readable form, i.e 1.2mb
 * @static
 * @throws {Error}
 */
Alfresco.util.formatFileSize = function(fileSize)
{
   if (typeof fileSize == "string")
   {
      fileSize = parseInt(fileSize, 10);
   }
   
   if (fileSize < Alfresco.util.BYTES_KB)
   {
      return fileSize + " " + Alfresco.util.message("size.bytes");
   }
   else if (fileSize < Alfresco.util.BYTES_MB)
   {
      fileSize = Math.round(fileSize / Alfresco.util.BYTES_KB);
      return fileSize + " " + Alfresco.util.message("size.kilobytes");
   }
   else if (fileSize < Alfresco.util.BYTES_GB)
   {
      fileSize = Math.round(fileSize / Alfresco.util.BYTES_MB);
      return fileSize + " " + Alfresco.util.message("size.megabytes");
   }

   fileSize = Math.round(fileSize / Alfresco.util.BYTES_GB);
   return fileSize + " " + Alfresco.util.message("size.gigabytes");
};

/**
 * Given a filename, returns either a filetype icon or generic icon file stem
 *
 * @method Alfresco.util.getFileIcon
 * @param p_fileName {string} File to find icon for
 * @param p_fileType {string} Optional: Filetype to offer further hinting
 * @param p_iconSize {int} Icon size: 32
 * @return {string} The icon name, e.g. doc-file-32.png
 * @static
 */
Alfresco.util.getFileIcon = function(p_fileName, p_fileType, p_iconSize)
{
   // Mapping from extn to icon name for cm:content
   var extns = 
   {
      "doc": "doc",
      "docx": "doc",
      "ppt": "ppt",
      "pptx": "ppt",
      "xls": "xls",
      "xlsx": "xls",
      "pdf": "pdf",
      "bmp": "img",
      "gif": "img",
      "jpg": "img",
      "jpeg": "img",
      "png": "img",
      "txt": "text"
   };

   var prefix = "generic",
      fileType = p_fileType !== undefined ? p_fileType : "cm:content",
      iconSize = p_iconSize !== undefined ? p_iconSize : 32;
   
   // If type = cm:content, then use extn look-up
   var type = Alfresco.util.getFileIcon.types[fileType];
   if (type === "file")
   {
      var extn = p_fileName.substring(p_fileName.lastIndexOf(".") + 1).toLowerCase();
      if (extn in extns)
      {
         prefix = extns[extn];
      }
   }
   else if (typeof type == "undefined")
   {
      type = "file";
   }
   return prefix + "-" + type + "-" + iconSize + ".png";
};
Alfresco.util.getFileIcon.types =
{
   "{http://www.alfresco.org/model/content/1.0}content": "file",
   "cm:content": "file",
   "{http://www.alfresco.org/model/content/1.0}thumbnail": "file",
   "cm:thumbnail": "file",
   "{http://www.alfresco.org/model/content/1.0}folder": "folder",
   "cm:folder": "folder",
   "{http://www.alfresco.org/model/content/1.0}category": "category",
   "cm:category": "category",
   "{http://www.alfresco.org/model/site/1.0}sites": "site",
   "st:sites": "site",
   "{http://www.alfresco.org/model/site/1.0}site": "site",
   "st:site": "site"
};

/**
 * Formats a Freemarker datetime into more UI-friendly format
 *
 * @method Alfresco.util.formatDate
 * @param date {string} Optional: Date as returned from data webscript. Today used if missing.
 * @param mask {string} Optional: Mask to use to override default.
 * @return {string} Date formatted for UI
 * @static
 */
Alfresco.util.formatDate = function(date)
{
   try
   {
      return Alfresco.thirdparty.dateFormat.apply(this, arguments);
   }
   catch(e)
   {
      return date;
   }
};

/**
 * Convert an ISO8601 date string into a JavaScript native Date object
 *
 * @method Alfresco.util.fromISO8601
 * @param date {string} ISO8601 formatted date string
 * @return {Date|null} JavaScript native Date object
 * @static
 */
Alfresco.util.fromISO8601 = function(date)
{
   try
   {
      return Alfresco.thirdparty.fromISO8601.apply(this, arguments);
   }
   catch(e)
   {
      return null;
   }
};

/**
 * Convert a JavaScript native Date object into an ISO8601 date string
 *
 * @method Alfresco.util.toISO8601
 * @param date {Date} JavaScript native Date object
 * @return {string} ISO8601 formatted date string
 * @static
 */
Alfresco.util.toISO8601 = function(date)
{
   try
   {
      return Alfresco.thirdparty.toISO8601.apply(this, arguments);
   }
   catch(e)
   {
      return "";
   }
};

/**
 * Convert an JSON date exploded into an object literal into a JavaScript native Date object.
 * NOTE: Passed-in date will have month as zero-based.
 *
 * @method Alfresco.util.fromExplodedJSONDate
 * @param date {object} object literal of the following example format (UTC):
 * <pre>
 *    date = 
 *    {
 *       year: 2009
 *       month: 4 // NOTE: zero-based
 *       date: 22
 *       hours: 14
 *       minutes: 27
 *       seconds: 42
 *       milliseconds: 390
 *    }
 * </pre>
 * @return {Date|null} JavaScript native Date object
 * @static
 */
Alfresco.util.fromExplodedJSONDate = function(date)
{
   try
   {
      var isoDate = YAHOO.lang.substitute("{year 4}-{month 2}-{date 2}T{hours 2}:{minutes 2}:{seconds 2}.{milliseconds 3}Z", date, function(p_key, p_value, p_meta)
      {
         if (p_key == "month")
         {
            ++p_value;
         }
			p_value = String(p_value);
			var length = parseInt(p_meta, 10) || 2;
			while (p_value.length < length)
			{
				p_value = "0" + p_value;
			}
			return p_value;
      });
      return Alfresco.thirdparty.fromISO8601.apply(this, [isoDate, Array.prototype.slice.call(arguments).slice(1)]);
   }
   catch(e)
   {
      return null;
   }
};

/**
 * Convert an object literal into a JavaScript native Date object into an JSON date exploded.
 * NOTE: Passed-in date will have month as zero-based.
 *
 * @method Alfresco.util.toExplodedJSONDate
 * @param date {Date} JavaScript Date object
 * @return {object}
 * <pre>
 *    date = 
 *    {
 *       year: 2009
 *       month: 4 // NOTE: zero-based
 *       date: 22
 *       hours: 14
 *       minutes: 27
 *       seconds: 42
 *       milliseconds: 390
 *    }
 * </pre>
 * @static
 */
Alfresco.util.toExplodedJSONDate = function(date)
{
   return (
   {
      zone: "UTC",
      year: date.getFullYear(),
      month: date.getMonth(),
      date: date.getDate(),
      hours: date.getHours(),
      minutes: date.getMinutes(),
      seconds: date.getSeconds(),
      milliseconds: date.getMilliseconds()
   });
};

/**
 * Pad a value with leading zeros to the specified length.
 * 
 * @method Alfresco.util.pad
 * @param value {string|number} non null value to pad
 * @param value {number} length to pad out with leading zeros
 * @return {string} padded value as a string
 * @static
 */
Alfresco.util.pad = function(value, length)
{
	value = String(value);
	length = parseInt(length, 10) || 2;
	while (value.length < length)
	{
		value = "0" + value;
	}
	return value;
};

/**
 * Inserts the given string into the supplied text element at the current cursor position.
 *
 * @method Alfresco.util.insertAtCursor
 * @param el {object} The Dom text element to insert into
 * @param txt {string} The string to insert at current cursor position
 * @static
 */
Alfresco.util.insertAtCursor = function(el, txt)
{
   if (document.selection)
   {
      el.focus();
      var sel = document.selection.createRange();
      sel.text = txt;
   }
   else if (el.selectionStart || el.selectionStart == '0')
   {
      var startPos = el.selectionStart;
      var endPos = el.selectionEnd;
      el.value = el.value.substring(0, startPos) + txt + el.value.substring(endPos, el.value.length);
   }
   else
   {
      el.value += txt;
   }
   el.focus();
};

/**
 * Decodes an HTML-encoded string
 * Replaces &lt; &gt; and &amp; entities with their character equivalents
 *
 * @method Alfresco.util.decodeHTML
 * @param html {string} The string containing HTML entities
 * @return {string} Decoded string
 * @static
 */
Alfresco.util.decodeHTML = function(html)
{
   if (html === null)
   {
      return "";
   }
   return html.split("&lt;").join("<").split("&gt;").join(">").split("&amp;").join("&");
};

/**
 * Encodes a potentially unsafe string with HTML entities
 * Replaces <pre><, >, &</pre> characters with their entity equivalents.
 * Based on the equivalent encodeHTML and unencodeHTML functions in Prototype.
 *
 * @method Alfresco.util.encodeHTML
 * @param text {string} The string to be encoded
 * @return {string} Safe HTML string
 * @static
 */
Alfresco.util.encodeHTML = function(text)
{
   if (text === null || typeof text == "undefined")
   {
      return "";
   }
   
   if (YAHOO.env.ua.ie > 0)
   {
      text = "" + text;
      return text.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/\n/g, "<br />&nbsp;&nbsp;&nbsp;");
   }
   var me = arguments.callee;
   me.text.data = text;
   return me.div.innerHTML.replace(/\n/g, "<br />&nbsp;&nbsp;&nbsp;");
};
Alfresco.util.encodeHTML.div = document.createElement("div");
Alfresco.util.encodeHTML.text = document.createTextNode("");
Alfresco.util.encodeHTML.div.appendChild(Alfresco.util.encodeHTML.text);

/**
 * Scans a text string for links and injects HTML mark-up to activate them.
 * NOTE: If used in conjunction with encodeHTML, this function must be called last.
 *
 * @method Alfresco.util.activateLinks
 * @param text {string} The string potentially containing links
 * @return {string} String with links marked-up to make them active
 * @static
 */
Alfresco.util.activateLinks = function(text)
{
   var re = new RegExp(/((http|ftp|https):\/\/[\w\-_]+(\.[\w\-_]+)+([\w\-\.,@?\^=%&:\/~\+#]*[\w\-\@?\^=%&\/~\+#])?)/g);
   text = text.replace(re, "<a href=\"$1\" target=\"_blank\">$1</a>");
   return text;
};

/**
 * Convert a plaintext Tweet into HTML with detected links parsed and "activated"
 *
 * @method Alfresco.util.tweetToHTML
 * @param text {string} The plaintext Tweet
 * @return {string} HTML string
 */
Alfresco.util.tweetToHTML = function(text)
{
   // URLs
   text = Alfresco.util.activateLinks(text);
   
   // User links
   var re = new RegExp(/(^|[^\w])@([\w]{1,})/g);
   text = text.replace(re, "$1<a href=\"http://twitter.com/$2\">@$2</a>");

   // Hash tags
   re = new RegExp(/#+([\w]{1,})/g);
   text = text.replace(re, "<a href=\"http://search.twitter.com/search?q=%23$1\">#$1</a>");

   return text;
};

/**
 * Tests a select element's options against "value" and
 * if there is a match that option is set to the selected index.
 *
 * @method Alfresco.util.setSelectedIndex
 * @param value {HTMLSelectElement} The select element to change the selectedIndex for
 * @param selectEl {string} The value to match agains the select elements option values
 * @return {string} The label/name of the seleceted option OR null if no option was found
 * @static
 */
Alfresco.util.setSelectedIndex = function(selectEl, value)
{
   for (var i = 0, l = selectEl.options.length; i < l; i++)
   {
      if (selectEl.options[i].value == value)
      {
         selectEl.selectedIndex = i;
         return selectEl.options[i].text;
      }
   }
   return null;
};

/**
 * Removes all potentially non safe tags from s (tags that are not listed in safeTags).
 * Normally the freemarker ?html encoding shall be used on the content from the repository but
 * components like blog, forum/discussion, wiki and comments requires some basic html tags to handle formatting.
 * Matches the functionality in org.alfresco.web.ui.common.StringUtils.java
 *
 * @method Alfresco.util.stripUnsafeHTMLTags
 * @param text {string} The string to remove potentially dangerous tags from
 * @return {string} Safe HTML string
 * @static
 */
Alfresco.util.stripUnsafeHTMLTags = function(s)
{
   var me = arguments.callee;
   s = "" + s;
   s = s.replace("onclick", "$");
   s = s.replace("onmouseover", "$");
   s = s.replace("onmouseout", "$");
   s = s.replace("onmousemove", "$");
   s = s.replace("onfocus", "$");
   s = s.replace("onblur", "$");
   var buf = [];
   var length = s.length;
   for (var i = 0; i < length; i++)
   {
      if (s.charAt(i) == '<')
      {
         // found a tag?
         var endMatchIndex = -1;
         var endTagIndex = -1;
         if (i < length - 2)
         {
            for (var x = (i + 1); x < length; x++)
            {
               if (s.charAt(x) == ' ' && endMatchIndex == -1)
               {
                  // keep track of the match point for comparing tags in the safeTags set
                  endMatchIndex = x;
               }
               else if (s.charAt(x) == '>')
               {
                  endTagIndex = x;
                  break;
               }
               else if (s.charAt(x) == '<')
               {
                  // found another angle bracket - not a tag def so we can safely output to here
                  break;
               }
            }
         }
         if (endTagIndex != -1)
         {
            // found end of the tag to match
            var tag = s.substring(i + 1, endTagIndex);
            var matchTag = tag;
            if (endMatchIndex != -1)
            {
               matchTag = s.substring(i + 1, endMatchIndex);
            }
            if (matchTag.charAt(0) == '/')
            {
               // Remove the '/' since it was an endtag
               matchTag = matchTag.substring(1);
            }
            if (me.safeTags[matchTag.toLowerCase()])
            {
               // safe tag - append to buffer
               buf.push('<');
               buf.push(tag);
               buf.push('>');
            }
            // inc counter to skip past whole tag
            i = endTagIndex;
            continue;
         }
      }
      buf.push(s.charAt(i));
   }
   return buf.join("");
};

Alfresco.util.stripUnsafeHTMLTags.safeTags =
{
   "strong": true,
   "sup": true,
   "sub": true,
   "em": true,
   "p": true,
   "b": true,
   "i": true,
   "br": true,
   "ul": true,
   "ol": true,
   "li": true,
   "h1": true,
   "h2": true,
   "h3": true,
   "h4": true,
   "h5": true,
   "h6": true,
   "div": true,
   "span": true,
   "a": true,
   "img": true,
   "font": true,
   "table": true,
   "thead": true,
   "tbody": true,
   "tr": true,
   "th": true,
   "td": true,
   "hr": true
};

/**
 * Returns a unique DOM ID for dynamically-created content. Optionally applies the new ID to an element.
 *
 * @method Alfresco.util.generateDomId
 * @param p_el {HTMLElement} Applies new ID to element
 * @param p_prefix {string} Optional prefix instead of "alf-id" default
 * @return {string} Dom Id guaranteed to be unique on the current page
 */
Alfresco.util.generateDomId = function(p_el, p_prefix)
{
   var domId, prefix = p_prefix || "alf-id";
   do
   {
      domId = prefix + Alfresco.util.generateDomId._nId++;
   } while (YUIDom.get(domId) !== null);

   if (p_el)
   {
      if (YAHOO.env.ua.ie > 0 && YAHOO.env.ua.ie < 8)
      {
         // MSIE 6 & 7-safe method
         p_el.attributes["id"].value = domId;
      }
      else
      {
         p_el.setAttribute("id", domId);
      }
   }
   
   return domId;
};
Alfresco.util.generateDomId._nId = 0;

/**
 * Converts "rel" attributes on <a> tags to "target" attributes.
 * "target" isn't supported in XHTML, so we use "rel" as a placeholder and replace at runtime.
 *
 * @method relToTarget
 * @param rootNode {HTMLElement|String} An id or HTMLElement to start the query from
*/
Alfresco.util.relToTarget = function(p_rootNode)
{
   var elements = YUISelector.query("a[rel]", p_rootNode);
   for (var i = 0, ii = elements.length; i < ii; i++)
   {
      elements[i].setAttribute("target", elements[i].getAttribute("rel"));
   }
};

/**
 * Wrapper to create a YUI Button with common attributes.
 * All supplied object parameters are passed to the button constructor
 * e.g. Alfresco.util.createYUIButton(this, "OK", this.onOK, {type: "submit"});
 *
 * @method Alfresco.util.createYUIButton
 * @param p_scope {object} Component containing button; must have "id" parameter
 * @param p_name {string} Dom element ID of markup that button is created from {p_scope.id}-{name}
 * @param p_onclick {function} If supplied, registered with the button's click event
 * @param p_obj {object} Optional extra object parameters to pass to button constructor
 * @param p_oElement {string|HTMLElement} Optional and accepts a string to use as an ID for getting a DOM reference or an actual DOM reference
 * @return {YAHOO.widget.Button} New Button instance
 * @static
 */
Alfresco.util.createYUIButton = function(p_scope, p_name, p_onclick, p_obj, p_oElement)
{
   // Default button parameters
   var obj =
   {
      type: "button",
      disabled: false
   };
   
   // Any extra parameters?
   if (typeof p_obj == "object")
   {
      obj = YAHOO.lang.merge(obj, p_obj);
   }
   
   // Fix-up the menu element ID
   if ((obj.type == "menu") && (typeof obj.menu == "string"))
   {
      obj.menu = p_scope.id + "-" + obj.menu;
   }
   
   // Create the button
   var oElement = p_oElement ? p_oElement : p_scope.id + "-" + p_name,
      button = new YAHOO.widget.Button(oElement, obj);

   if (typeof button == "object")
   {
      // Register the click listener if one was supplied
      if (typeof p_onclick == "function")
      {
         // Special case for a menu
         if (obj.type == "menu")
         {
            button.getMenu().subscribe("click", p_onclick, p_scope, true);
         }
         else
         {
            button.on("click", p_onclick, button, p_scope);
         }
      }
      
      // Special case if htmlName was passed-in as an option
      if (typeof obj.htmlName != "undefined")
      {
         button.get("element").getElementsByTagName("button")[0].name = obj.htmlName;
      }
   }
   return button;
};

/**
 * Wrapper to disable a YUI Button, including link buttons.
 * Link buttons aren't disabled by YUI; see http://developer.yahoo.com/yui/button/#apiref
 *
 * @method Alfresco.util.disableYUIButton
 * @param p_button {YAHOO.widget.Button} Button instance
 * @static
 */
Alfresco.util.disableYUIButton = function(p_button)
{
   if (p_button.set && p_button.get)
   {
      p_button.set("disabled", true);
      if (p_button.get("type") == "link")
      {
         /**
          * Note the non-optimal use of a "private" variable, which is why it's tested before use.
          */
         p_button.set("href", "");
         if (p_button._button && p_button._button.setAttribute)
         {
            p_button._button.setAttribute("onclick", "return false;");
         }
         p_button.addStateCSSClasses("disabled");
         p_button.removeStateCSSClasses("hover");
         p_button.removeStateCSSClasses("active");
         p_button.removeStateCSSClasses("focus");
      }
   }
};

/**
 * Creates a "disclosure twister" UI control from existing mark-up.
 *
 * @method Alfresco.util.createTwister
 * @param p_controller {Element|string} Element (or DOM ID) of controller node
 * <pre>The code will search for the next sibling which will be used as the hideable panel, unless overridden below</pre>
 * @param p_filterName {string} Filter's name under which to save it's collapsed state via preferences
 * @param p_config {object} Optional additional configuration to override the defaults
 * <pre>
 *    panel {Element|string} Use this panel as the hideable element instead of the controller's first sibling
 *    collapsed {boolean} Whether the twister should be drawn collapsed upon creation
 * </pre>
 * @return {boolean} true = success
 */
Alfresco.util.createTwister = function(p_controller, p_filterName, p_config)
{
   var defaultConfig =
   {
      panel: null,
      collapsed: null,
      CLASS_BASE: "alfresco-twister",
      CLASS_OPEN: "alfresco-twister-open",
      CLASS_CLOSED: "alfresco-twister-closed"
   };
   
   var elController, elPanel,
      config = YAHOO.lang.merge(defaultConfig, p_config || {});
   
   // Controller element
   elController = YUIDom.get(p_controller);
   if (elController === null)
   {
      return false;
   }
   
   // Panel element - next sibling or specified in configuration
   if (config.panel && YUIDom.get(config.panel))
   {
      elPanel = YUIDom.get(config.panel);
   }
   else
   {
      // Find the first sibling node
      elPanel = elController.nextSibling;
      while (elPanel.nodeType !== 1 && elPanel !== null)
      {
         elPanel = elPanel.nextSibling;
      }
   }
   if (elPanel === null)
   {
      return false;
   }

   // If "collapsed" isn't specified in config, then use the value stored in preferences
   if (config.collapsed === null)
   {
      var collapsedPrefs = Alfresco.util.arrayToObject(Alfresco.util.createTwister.collapsed.split(","));
      config.collapsed = !!collapsedPrefs[p_filterName];
   }

   // Initial State
   YUIDom.addClass(elController, config.CLASS_BASE);
   YUIDom.addClass(elController, config.collapsed ? config.CLASS_CLOSED : config.CLASS_OPEN);
   YUIDom.setStyle(elPanel, "display", config.collapsed ? "none" : "block");
   
   YUIEvent.addListener(elController, "click", function(p_event, p_obj)
   {
      // Update UI to new state
      var collapse = YUIDom.hasClass(p_obj.controller, config.CLASS_OPEN);
      if (collapse)
      {
         YUIDom.replaceClass(p_obj.controller, config.CLASS_OPEN, config.CLASS_CLOSED);
      }
      else
      {
         YUIDom.replaceClass(p_obj.controller, config.CLASS_CLOSED, config.CLASS_OPEN);
      }
      YUIDom.setStyle(p_obj.panel, "display", collapse ? "none" : "block");

      // Save to preferences
      var fnPref = collapse ? "add" : "remove",
         preferences = new Alfresco.service.Preferences();
      
      preferences[fnPref].call(preferences, Alfresco.service.Preferences.COLLAPSED_TWISTERS, p_obj.filterName);
   },
   {
      controller: elController,
      panel: elPanel,
      filterName: p_filterName
   });
};
Alfresco.util.createTwister.collapsed = "";

/**
 * Wrapper to create a YUI Panel with common attributes, as follows:
 * <pre>
 *   modal: true,
 *   constraintoviewport: true,
 *   draggable: true,
 *   fixedcenter: true,
 *   close: true,
 *   visible: false
 * </pre>
 * All supplied object parameters are passed to the panel constructor
 * e.g. Alfresco.util.createYUIPanel("myId", { draggable: false });
 *
 * @method Alfresco.util.createYUIPanel
 * @param p_el {string|HTMLElement} The element ID representing the Panel or the element representing the Panel
 * @param p_params {object} Optional extra/overridden object parameters to pass to Panel constructor
 * @param p_custom {object} Optional parameters to customise Panel creation:
 * <pre>
 *    render {boolean} By default the new Panel will be rendered to document.body. Set to false to prevent this.
 *    type {object} Use to override YAHOO.widget.Panel default type, e.g. YAHOO.widget.Dialog
 * </pre>
 * @return {YAHOO.widget.Panel|flags.type} New Panel instance
 * @static
 */
Alfresco.util.createYUIPanel = function(p_el, p_params, p_custom)
{
   // Default constructor parameters
   var panel,
      params =
      {
         modal: true,
         constraintoviewport: true,
         draggable: true,
         fixedcenter: "contained",
         close: true,
         visible: false
      },
      custom =
      {
         render: true,
         type: YAHOO.widget.Panel
      };
   
   // Any extra/overridden constructor parameters?
   if (typeof p_params == "object")
   {
      params = YAHOO.lang.merge(params, p_params);
   }
   // Any customisation?
   if (typeof p_custom == "object")
   {
      custom = YAHOO.lang.merge(custom, p_custom);
   }

   // Create and return the panel
   panel = new (custom.type)(p_el, params);

   if (custom.render)
   {
      panel.render(document.body);
   }
   
   return panel;
};

/**
 * Find an event target's class name, ignoring YUI classes.
 *
 * @method Alfresco.util.findEventClass
 * @param p_eventTarget {object} Event target from Event class
 * @param p_tagName {string} Optional tag if 'span' needs to be overridden
 * @return {string|null} Class name or null
 * @static
 */
Alfresco.util.findEventClass = function(p_eventTarget, p_tagName)
{
   var src = p_eventTarget.element;
   var tagName = (p_tagName || "span").toLowerCase();

   // Walk down until specified tag found and not a yui class
   while ((src !== null) && ((src.tagName.toLowerCase() != tagName) || (src.className.indexOf("yui") === 0)))
   {
      src = src.firstChild;
   }

   // Found the target element?
   if (src === null)
   {
      return null;
   }

   return src.className;
};

/**
 * Determines whether a Bubbling event should be ignored or not
 *
 * @method Alfresco.util.hasEventInterest
 * @param p_instance {object} Instance checking for event interest
 * @param p_args {object} Bubbling event args
 * @return {boolean} false to ignore event
 */
Alfresco.util.hasEventInterest = function(p_eventGroup, p_args)
{
   var obj = p_args[1],
      sourceGroup = "source",
      targetGroup = "target",
      hasInterest = false;

   if (obj)
   {
      // Was this a defaultAction event?
      if (obj.action === "navigate")
      {
         obj.eventGroup = obj.anchor.rel;
      }
      
      if (obj.eventGroup)
      {
         sourceGroup = (typeof obj.eventGroup == "string") ? obj.eventGroup : obj.eventGroup.eventGroup;
         targetGroup = (typeof p_eventGroup == "string") ? p_eventGroup : p_eventGroup.eventGroup;

         hasInterest = (sourceGroup == targetGroup);
      }
   }
   return hasInterest;
};

/**
 * Check if flash is installed.
 * Returns true if a flash player of the required version is installed
 *
 * @method Alfresco.util.isFlashInstalled
 * @param reqMajorVer {int}
 * @param reqMinorVer {int}
 * @param reqRevision {int}
 * @return {boolean} Returns true if a flash player of the required version is installed
 * @static
 */
Alfresco.util.hasRequiredFlashPlayer = function(reqMajorVer, reqMinorVer, reqRevision)
{
   if (typeof DetectFlashVer == "function")
   {
      return DetectFlashVer(reqMajorVer, reqMinorVer, reqRevision);
   }
   return false;
};

/**
 * Add a component's messages to the central message store.
 *
 * @method Alfresco.util.addMessages
 * @param p_obj {object} Object literal containing messages in the correct locale
 * @param p_messageScope {string} Message scope to add these to, e.g. componentId
 * @return {boolean} true if messages added
 * @throws {Error}
 * @static
 */
Alfresco.util.addMessages = function(p_obj, p_messageScope)
{
   if (p_messageScope === undefined)
   {
      throw new Error("messageScope must be defined");
   }
   else if (p_messageScope == "global")
   {
      throw new Error("messageScope cannot be 'global'");
   }
   else
   {
      Alfresco.messages.scope[p_messageScope] = YAHOO.lang.merge(Alfresco.messages.scope[p_messageScope] || {}, p_obj);
      return true;
   }
   // for completeness...
   return false;
};

/**
 * Copy one namespace's messages to another's.
 *
 * @method Alfresco.util.copyMessages
 * @param p_source {string} Source namespace
 * @param p_destination {string} Destination namespace. Will be overwritten with source's messages
 * @throws {Error}
 * @static
 */
Alfresco.util.copyMessages = function(p_source, p_destination)
{
   if (p_source === undefined)
   {
      throw new Error("Source must be defined");
   }
   else if (Alfresco.messages.scope[p_source] === undefined)
   {
      throw new Error("Source namespace doesn't exist");
   }
   else if (p_destination === undefined)
   {
      throw new Error("Destination must be defined");
   }
   else if (p_destination == "global")
   {
      throw new Error("Destination cannot be 'global'");
   }
   else
   {
      Alfresco.messages.scope[p_destination] = YAHOO.lang.merge({}, Alfresco.messages.scope[p_source]);
   }
};

/**
 * Resolve a messageId into a message.
 * If a messageScope is supplied, that container will be searched first
 * followed by the "global" message scope.
 *
 * @method Alfresco.util.message
 * @param p_messageId {string} Message id to resolve
 * @param p_messageScope {string} Message scope, e.g. componentId
 * @param multiple-values {string} Values to replace tokens with
 * @return {string} The localized message string or the messageId if not found
 * @throws {Error}
 * @static
 */
Alfresco.util.message = function(p_messageId, p_messageScope)
{
   var msg = p_messageId;
   
   if (typeof p_messageId != "string")
   {
      throw new Error("Missing or invalid argument: messageId");
   }
   
   var globalMsg = Alfresco.messages.global[p_messageId];
   if (typeof globalMsg == "string")
   {
      msg = globalMsg;
   }

   if ((typeof p_messageScope == "string") && (typeof Alfresco.messages.scope[p_messageScope] == "object"))
   {
      var scopeMsg = Alfresco.messages.scope[p_messageScope][p_messageId];
      if (typeof scopeMsg == "string")
      {
         msg = scopeMsg;
      }
   }
   
   // Search/replace tokens
   var tokens;
   if ((arguments.length == 3) && (typeof arguments[2] == "object"))
   {
      tokens = arguments[2];
   }
   else
   {
      tokens = Array.prototype.slice.call(arguments).slice(2);
   }
   msg = YAHOO.lang.substitute(msg, tokens);
   
   return msg;
};

/**
 * Fixes the hidden caret problem in Firefox 2.x.
 * Assumes <input> or <textarea> elements are wrapped in a <div class="yui-u"></div>
 *
 * @method Alfresco.util.caretFix
 * @param p_formElement {element|string} Form element to fix input boxes within
 * @static
 */
Alfresco.util.caretFix = function(p_formElement)
{
   if (YAHOO.env.ua.gecko === 1.8)
   {
      if (typeof p_formElement == "string")
      {
         p_formElement = YUIDom.get(p_formElement);
      }
      var nodes = YUISelector.query(".yui-u", p_formElement);
      for (var x = 0; x < nodes.length; x++)
      {
         var elem = nodes[x];
         YUIDom.addClass(elem, "caret-fix");
      }
   }
};

/**
 * Remove the fixes for the hidden caret problem in Firefox 2.x.
 * Should be called before hiding a form for re-use.
 *
 * @method Alfresco.util.undoCaretFix
 * @param p_formElement {element|string} Form element to undo fixes within
 * @static
 */
Alfresco.util.undoCaretFix = function(p_formElement)
{
   if (YAHOO.env.ua.gecko === 1.8)
   {
      if (typeof p_formElement == "string")
      {
         p_formElement = YUIDom.get(p_formElement);
      }
      var nodes = YUISelector.query(".caret-fix", p_formElement);
      for (var x = 0; x < nodes.length; x++)
      {
         var elem = nodes[x];
         YUIDom.removeClass(elem, "caret-fix");
      }
   }
};

/**
 * Parses a string to a json object and returns it.
 * If str contains invalid json code that is displayed using displayPrompt().
 *
 * @method Alfresco.util.parseJSON
 * @param jsonStr {string} The JSON string to be parsed
 * @param displayError {boolean} Set true to display a message informing about bad JSON syntax
 * @return {object} The object representing the JSON string
 * @static
 */
Alfresco.util.parseJSON = function(jsonStr, displayError)
{
   try
   {
      return YAHOO.lang.JSON.parse(jsonStr);
   }
   catch (error)
   {
      if (displayError)
      {
         Alfresco.util.PopupManager.displayPrompt(
         {
            title: "Failure",
            text: "Can't parse response as json: '" + jsonStr + "'"
         });
      }
   }
   return null;
};

/**
 * Returns a populated URI template, given a TemplateId and an object literal
 * containing the tokens to be substituted
 *
 * @method Alfresco.util.uriTemplate
 * @param templateId {string} URI TemplateId from web-framework configuration
 * @param obj {object} The object literal containing the token values to substitute
 * @param absolute {boolean} Whether the URL should include the protocol and host
 * @return {string|null} The populated URI or null if templateId not found
 * @static
 */
Alfresco.util.uriTemplate = function(templateId, obj, absolute)
{
   // Check we know about the templateId
   if (!templateId in Alfresco.constants.URI_TEMPLATES)
   {
      return null;
   }
   
   var uri = "",
      template = Alfresco.constants.URI_TEMPLATES[templateId];
   
   // Page context end with trailing "/", so remove any leading one from the URI template
   if (template.charAt(0) == "/")
   {
      template = template.substring(1);
   }
   // Absolute URI needs current protocol and host
   if (absolute)
   {
      uri = location.protocol + "//" + location.host;
   }
   uri += Alfresco.constants.URL_PAGECONTEXT + YAHOO.lang.substitute(template, obj);
   
   return uri;
};

/**
 * Returns a URL to the content represented by the passed-in nodeRef
 *
 * @method Alfresco.util.contentURL
 * @param nodeRef {string} Standard Alfresco nodeRef
 * @param name {string} Filename to download
 * @param attach {boolean} If true, browser should prompt the user to "Open or Save?", rather than display inline
 * @return {string} The URL to the content
 * @static
 */
Alfresco.util.contentURL = function(nodeRef, name, attach)
{
   return Alfresco.constants.PROXY_URI + "api/node/content/" + nodeRef.replace(":/", "") + "/" + name + (attach ? "?a=true" : "");
};

/**
 * Returns the value of the specified query string parameter.
 *
 * @method getQueryStringParameter
 * @param {string} paramName Name of the parameter we want to look up.
 * @param {string} queryString Optional URL to look at. If not specified,
 *     this method uses the URL in the address bar.
 * @return {string} The value of the specified parameter, or null.
 * @static
 */
Alfresco.util.getQueryStringParameter = function(paramName, url)
{
    var params = this.getQueryStringParameters(url);
    
    if (paramName in params)
    {
       return params[paramName];
    }

    return null;
};

/**
 * Returns the query string parameters as an object literal.
 * Parameters appearing more than once are returned an an array.
 * This method has been extracted from the YUI Browser History Manager.
 * It can be used here without the overhead of the History JavaScript include.
 *
 * @method getQueryStringParameters
 * @param queryString {string} Optional URL to look at. If not specified,
 *     this method uses the URL in the address bar.
 * @return {object} Object literal containing QueryString parameters as name/value pairs
 * @static
 */
Alfresco.util.getQueryStringParameters = function(url)
{
   var i, len, idx, queryString, params, tokens, name, value, objParams;

   url = url || top.location.href;

   idx = url.indexOf("?");
   queryString = idx >= 0 ? url.substr(idx + 1) : url;

   // Remove the hash if any
   idx = queryString.lastIndexOf("#");
   queryString = idx >= 0 ? queryString.substr(0, idx) : queryString;

   params = queryString.split("&");

   objParams = {};

   for (i = 0, len = params.length; i < len; i++)
   {
      tokens = params[i].split("=");
      if (tokens.length >= 2)
      {
         name = tokens[0];
         value = window.unescape(tokens[1]);
         switch (typeof objParams[name])
         {
            case "undefined":
               objParams[name] = value;
               break;

            case "string":
               objParams[name] = [objParams[name]].concat(value);
               break;

            case "object":
               objParams[name] = objParams[name].concat(value);
               break;
         }
      }
   }

   return objParams;
};

/**
 * Turns an object literal into a valid queryString.
 * Format of the object is as returned from the getQueryStringParameters() function.
 *
 * @method toQueryString
 * @param params {object} Object literal containing QueryString parameters as name/value pairs
 * @return {string} QueryString-formatted string
 * @static
 */
Alfresco.util.toQueryString = function(p_params)
{
   var qs = "?", name, value, val;
   for (name in p_params)
   {
      if (p_params.hasOwnProperty(name))
      {
         value = p_params[name];
         if (typeof value == "object")
         {
            for (val in value)
            {
               if (value.hasOwnProperty(val))
               {
                  qs += name + "=" + window.escape(value[val]) + "&";
               }
            }
         }
         else if (typeof value == "string")
         {
            qs += name + "=" + window.escape(value) + "&";
         }
      }
   }
   
   // Return the string after removing the last character
   return qs.substring(0, qs.length - 1);
};

/**
 * Retrieves a JavaScript session variable.
 * Variables are scoped to the current "location.host"
 *
 * @method getVar
 * @param name {string} Variable name
 * @param default {object} Default value to return if not set
 * @return {object|null} Variable value or default if provided (null otherwise)
 * @static
 */
Alfresco.util.getVar = function(p_name, p_default)
{
   var returnValue = typeof p_default != "undefined" ? p_default : null;
   
   try
   {
      if (window.name !== "" && YAHOO.lang.JSON.isValid(window.name))
      {
         var allVars = YAHOO.lang.JSON.parse(window.name),
            scopedVars = allVars[location.host],
            value = null;

         if (typeof scopedVars == "object")
         {
            value = scopedVars[p_name];
            if (typeof value !== "undefined" && value !== null)
            {
               returnValue = value;
            }
         }
      }
   }
   catch (e)
   {
      Alfresco.logger.error("Alfresco.util.getVar()", p_name, p_default, e);
   }
   
   return returnValue; 
};

/**
 * Sets a JavaScript session variable.
 * The variables are stored in window.name, so live for as long as the browser window does.
 * Variables are scoped to the current "location.host"
 *
 * @method setVar
 * @param name {string} Variable name
 * @param value {object} Value to set
 * @return {boolean} True for successful set
 * @static
 */
Alfresco.util.setVar = function(p_name, p_value)
{
   var success = true;
   
   try
   {
      var allVars = {};
      
      if (window.name !== "" && YAHOO.lang.JSON.isValid(window.name))
      {
         allVars = YAHOO.lang.JSON.parse(window.name);
      }
      
      if (typeof allVars[location.host] == "undefined")
      {
         allVars[location.host] = {};
      }
      allVars[location.host][p_name] = p_value;

      window.name = YAHOO.lang.JSON.stringify(allVars);
   }
   catch (e)
   {
      Alfresco.logger.error("Alfresco.util.setVar()", p_name, p_value, e);
      success = false;
   }
   return success;
};


/**
 * Takes a string and splits it up to valid tags by using whitespace as separators.
 * Note! If invalid characters are found they are treated as separators.
 * I.e the string "hello*world alfresco" would result in  tags: "hello", "world" and "alfresco".
 *
 * @method getTags
 * @param str {string} a string containing tags
 * @return {array} of valid tags
 * @static
 */
Alfresco.util.getTags = function(str)
{
   var tag = null,
      tags = [],
      regexp = /([^\s\"\*\\\>\<\?\/\:\|]+)/gi;

   while ((tag = regexp.exec(str)))
   {
      if (tags[tag[1]] === undefined)
      {
         tags[tag[1]] = true;
         tags.push(tag[1]);
      }
   }
   return tags;
};

/**
 * The YUI Bubbling Library augments callback objects with its own built-in fields.
 * This function strips those out, so the remainder of the object is "clean"
 *
 * @method cleanBubblingObject
 * @param callbackObj {object} Object literal as passed to the event handler
 * @return {object} Object stripped of Bubbling Library fields
 * @static
 */
Alfresco.util.cleanBubblingObject = function(callbackObj)
{
   // See Bubbling Library, fire() function. These fields are correct for v2.1.
   var augmented =
   {
	   action: true,
	   flagged: true,
	   decrepitate: true,
	   stop: true
   },
      cleanObj = {};
   
   for (var index in callbackObj)
   {
      if (callbackObj.hasOwnProperty(index) && augmented[index] !== true)
      {
         cleanObj[index] = callbackObj[index];
      }
   }
   return cleanObj;
};

/**
 * Wrapper for helping components specify their YUI components.
 * @class Alfresco.util.YUILoaderHelper
 */
Alfresco.util.YUILoaderHelper = function()
{
   /**
    * The YUILoader single instance which will load all the dependencies
    * @property yuiLoader
    * @type YAHOO.util.YUILoader
    */
   var yuiLoader = null;

   /**
    * Array to store callbacks from all component registrants
    * @property callbacks
    * @type Array
    */
   var callbacks = [];

   /**
    * Flag to indicate whether the initial YUILoader has completed
    * @property initialLoaderComplete
    * @type boolean
    */
   var initialLoaderComplete = false;
   
   return (
   {
      /**
       * Main entrypoint for components wishing to load a YUI component
       * @method require
       * @param p_aComponents {Array} List of required YUI components. See YUILoader documentation for valid names
       * @param p_oCallback {function} Callback function invoked when all required YUI components have been loaded
       * @param p_oScope {object} Scope for callback function
       */
      require: function YLH_require(p_aComponents, p_oCallback, p_oScope)
      {
         if (yuiLoader === null)
         {
            yuiLoader = new YAHOO.util.YUILoader(
            {
               base: Alfresco.constants.URL_CONTEXT + "yui/",
               filter: Alfresco.constants.DEBUG ? "DEBUG" : "",
               loadOptional: false,
               skin: {},
               onSuccess: Alfresco.util.YUILoaderHelper.onLoaderComplete,
               onFailure: function(event)
               {
                  alert("load failed:" + event);
               },
               scope: this
            });
            // Add Alfresco YUI components to YUI loader

            // SWFPlayer
            yuiLoader.addModule(
            {
               name: "swfplayer",
               type: "js",
               path: "swfplayer/swfplayer.js", //can use a path instead, extending base path
               varName: "SWFPlayer",
               requires: ['uploader'] // The FlashAdapter class is located in uploader.js
            });

            // ColumnBrowser - js
            yuiLoader.addModule(
            {
               name: "columnbrowser",
               type: "js",
               path: "columnbrowser/columnbrowser.js", //can use a path instead, extending base path
               varName: "ColumnBrowser",
               requires: ['json', 'carousel'],
               skinnable: true
            });

         }
         
         if (p_aComponents.length > 0)
         {
            /* Have all the YUI components the caller requires been registered? */
            var isRegistered = true;
            for (var i = 0; i < p_aComponents.length; i++)
            {
               if (YAHOO.env.getVersion(p_aComponents[i]) === null)
               {
                  isRegistered = false;
                  break;
               }
            }
            if (isRegistered && (p_oCallback !== null))
            {
               p_oCallback.call(typeof p_oScope != "undefined" ? p_oScope : window);
            }
            else
            {
               /* Add to the list of components to be loaded */
               yuiLoader.require(p_aComponents);

               /* Store the callback function and scope for later */
               callbacks.push(
               {
                  required: Alfresco.util.arrayToObject(p_aComponents),
                  fn: p_oCallback,
                  scope: (typeof p_oScope != "undefined" ? p_oScope : window)
               });
            }
         }
         else if (p_oCallback !== null)
         {
            p_oCallback.call(typeof p_oScope != "undefined" ? p_oScope : window);
         }
      },
      
      /**
       * Called by template once all component dependencies have been registered. Should be just before the </body> closing tag.
       * @method loadComponents
       */
      loadComponents: function YLH_loadComponents()
      {
         if (yuiLoader !== null)
         {
            yuiLoader.insert(null, "js");
         }
      },

      /**
       * Callback from YUILoader once all required YUI componentshave been loaded by the browser.
       * @method onLoaderComplete
       */
      onLoaderComplete: function YLH_onLoaderComplete()
      {
         for (var i = 0; i < callbacks.length; i++)
         {
            if (callbacks[i].fn)
            {
               callbacks[i].fn.call(callbacks[i].scope);
            }
         }
         callbacks = [];
         initialLoaderComplete = true;
      }
   });
}();


/**
 * Keeps track of Alfresco components on a page. Components should register() upon creation to be compliant.
 * @class Alfresco.util.ComponentManager
 */
Alfresco.util.ComponentManager = function()
{
   /**
    * Array of registered components.
    * 
    * @property components
    * @type Array
    */
   var components = [];
   
   return (
   {
      /**
       * Main entrypoint for components wishing to register themselves with the ComponentManager
       * @method register
       * @param p_aComponent {object} Component instance to be registered
       */
      register: function CM_register(p_oComponent)
      {
         components.push(p_oComponent);
         components[p_oComponent.id] = p_oComponent;
      },

      /**
       * Unregister a component from the ComponentManager
       * @method unregister
       * @param p_aComponent {object} Component instance to be unregistered
       */
      unregister: function CM_unregister(p_oComponent)
      {
         for (var i = 0; i < components.length; i++) // Do not optimize
         {
            if (components[i] == p_oComponent)
            {
               components.splice(i, 1);
               delete components[p_oComponent.id];
               break;
            }
         }
      },

      /**
       * Re-register a component with the ComponentManager
       * Component ID cannot be updated via this function, use separate unregister(), register() calls instead.
       * @method reregister
       * @param p_aComponent {object} Component instance to be unregistered, then registered again
       */
      reregister: function CM_reregister(p_oComponent)
      {
         this.unregister(p_oComponent);
         this.register(p_oComponent);
      },

      /**
       * Allows components to find other registered components by name, id or both
       * e.g. find({name: "Alfresco.DocumentLibrary"})
       * @method find
       * @param p_oParams {object} List of paramters to search by
       * @return {Array} Array of components found in the search
       */
      find: function CM_find(p_oParams)
      {
         var found = [];
         var bMatch, component;
         
         for (var i = 0, j = components.length; i < j; i++)
         {
            component = components[i];
            bMatch = true;
            for (var key in p_oParams)
            {
               if (p_oParams[key] != component[key])
               {
                  bMatch = false;
               }
            }
            if (bMatch)
            {
               found.push(component);
            }
         }
         return found;
      },

      /**
       * Allows components to find first registered components by name only
       * e.g. findFirst("Alfresco.DocumentLibrary")
       * @method findFirst
       * @param p_sName {string} Name of registered component to search on
       * @return {object|null} Component found in the search
       */
      findFirst: function CM_findFirst(p_sName)
      {
         var found = Alfresco.util.ComponentManager.find(
         {
            name: p_sName
         });
         
         return (typeof found[0] == "object" ? found[0] : null);
      },

      /**
       * Get component by Id
       * e.g. get("global_x002e_header-sites-menu")
       * @method get
       * @param p_sId {string} Id of registered component to return
       * @return {object|null} Component with given Id
       */
      get: function CM_get(p_sId)
      {
         return (components[p_sId] || null);
      }
   });
}();

/**
 * Provides a common interface for displaying popups in various forms
 *
 * @class Alfresco.util.PopupManager
 */
Alfresco.util.PopupManager = function()
{
   /**
    * Alfresco Slingshot aliases
    */
   var $html = Alfresco.util.encodeHTML;
   
   return (
   {

      /**
       * The html zIndex startvalue that will be incremented for each popup
       * that is displayed to make sure the popup is visible to the user.
       *
       * @property zIndex
       * @type int
       */
      zIndex: 15,

      /**
       * The default config for the displaying messages, can be overriden
       * when calling displayMessage()
       *
       * @property defaultDisplayMessageConfig
       * @type object
       */
      defaultDisplayMessageConfig:
      {
         title: null,
         text: null,
         spanClass: "message",
         displayTime: 2.5,
         effect: YAHOO.widget.ContainerEffect.FADE,
         effectDuration: 0.5,
         visible: false,
         noEscape: false
      },

      /**
       * Intended usage: To quickly assure the user that the expected happened.
       *
       * Displays a message as a popup on the screen.
       * In default mode it fades, is visible for half a second and then fades out.
       *
       * @method displayMessage
       * @param config {object}
       * The config object is in the form of:
       * {
       *    text: {string},         // The message text to display, mandatory
       *    spanClass: {string},    // The class of the span wrapping the text
       *    effect: {YAHOO.widget.ContainerEffect}, // the effect to use when shpwing and hiding the message,
       *                                            // default is YAHOO.widget.ContainerEffect.FADE
       *    effectDuration: {int},  // time in seconds that the effect should be played, default is 0.5
       *    displayTime: {int},     // time in seconds that the message will be displayed, default 2.5
       *    modal: {true}           // if the message should modal (the background overlayed with a gray transparent layer), default is false
       * }
       */
      displayMessage: function(config)
      {
         // Merge the users config with the default config and check mandatory properties
         var c = YAHOO.lang.merge(this.defaultDisplayMessageConfig, config);
         if (c.text === undefined)
         {
            throw new Error("Property text in userConfig must be set");
         }
         // Construct the YUI Dialog that will display the message
         var message = new YAHOO.widget.Dialog("message",
         {
            modal: false,
            visible: c.visible,
            close: false,
            draggable: false,
            effect:
            {
               effect: c.effect,
               duration: c.effectDuration
            },
            zIndex: this.zIndex++
         });

         // Set the message that should be displayed
         var bd =  "<span class='" + c.spanClass + "'>" + (c.noEscape ? c.text : $html(c.text)) + "</span>";
         message.setBody(bd);

         /**
          * Add it to the dom, center it, schedule the fade out of the message
          * and show it.
          */
         message.render(document.body);
         message.center();
         // Need to schedule a fade-out?
         if (c.displayTime > 0)
         {
            message.subscribe("show", this._delayPopupHide,
            {
               popup: message,
               displayTime: (c.displayTime * 1000)
            }, true);
         }
         message.show();
         
         return message;
      },

      /**
       * Gets called after the message has been displayed as long as it was
       * configured.
       * Hides the message from the user.
       *
       * @method _delayPopupHide
       */
      _delayPopupHide: function()
      {         
         YAHOO.lang.later(this.displayTime, this, function()
         {
            this.popup.destroy();
         });
      },

      /**
       * The default config for displaying "prompt" messages, can be overriden
       * when calling displayPrompt()
       *
       * @property defaultDisplayPromptConfig
       * @type object
       */
      defaultDisplayPromptConfig:
      {
         title: null,
         text: null,
         icon: null,
         close: false,
         constraintoviewport: true,
         draggable: true,
         effect: null,
         effectDuration: 0.5,
         modal: true,
         visible: false,
         noEscape: false,
         buttons: [
         {
            text: null, // Too early to localize at this time, do it when called instead
            handler: function()
            {
               this.destroy();
            },
            isDefault: true
         }]
      },

      /**
       * Intended usage: To inform the user that something unexpected happened
       * OR that ask the user if if an action should be performed.
       *
       * Displays a message as a popup on the screen with a button to make sure
       * the user responds to the prompt.
       *
       * In default mode it shows with an OK button that needs clicking to get closed.
       *
       * @method displayPrompt
       * @param config {object}
       * The config object is in the form of:
       * {
       *    title: {string},       // the title of the dialog, default is null
       *    text: {string},        // the text to display for the user, mandatory
       *    icon: null,            // the icon to display next to the text, default is null
       *    effect: {YAHOO.widget.ContainerEffect}, // the effect to use when showing and hiding the prompt, default is null
       *    effectDuration: {int}, // the time in seconds that the effect should run, default is 0.5
       *    modal: {boolean},      // if a grey transparent overlay should be displayed in the background
       *    close: {boolean},      // if a close icon should be displayed in the right upper corner, default is false
       *    buttons: []            // an array of button configs as described by YUI:s SimpleDialog, default is a single OK button
       *    noEscape: {boolean}    // indicates the the message has already been escaped (e.g. to display HTML-based messages)
       * }
       */
      displayPrompt: function(config)
      {
         if (this.defaultDisplayPromptConfig.buttons[0].text === null)
         {
            /**
             * This default value could not be set at instantion time since the
             * localized messages weren't present at that time
             */
            this.defaultDisplayPromptConfig.buttons[0].text = Alfresco.util.message("button.ok", this.name);
         }
         // Merge users config and the default config and check manadatory properties
         var c = YAHOO.lang.merge(this.defaultDisplayPromptConfig, config);
         if (c.text === undefined)
         {
            throw new Error("Property text in userConfig must be set");
         }

         // Create the SimpleDialog that will display the text
         var prompt = new YAHOO.widget.SimpleDialog("prompt",
         {
            close: c.close,
            constraintoviewport: c.constraintoviewport,
            draggable: c.draggable,
            effect: c.effect,
            modal: c.modal,
            visible: c.visible,
            zIndex: this.zIndex++
         });

         // Show the title if it exists
         if (c.title)
         {
            prompt.setHeader($html(c.title));
         }

         // Show the prompt text
         prompt.setBody(c.noEscape ? c.text : $html(c.text));

         // Show the icon if it exists
         if (c.icon)
         {
            prompt.cfg.setProperty("icon", c.icon);
         }

         // Add the buttons to the dialog
         if (c.buttons)
         {
            prompt.cfg.queueProperty("buttons", c.buttons);
         }

         // Add the dialog to the dom, center it and show it.
         prompt.render(document.body);
         prompt.center();
         prompt.show();
      },
      
      /**
       * The default config for the getting user input, can be overriden
       * when calling getUserInput()
       *
       * @property defaultGetUserInputConfig
       * @type object
       */
      defaultGetUserInputConfig:
      {
         title: null,
         text: null,
         value: "",
         icon: null,
         close: true,
         constraintoviewport: true,
         draggable: true,
         effect: null,
         effectDuration: 0.5,
         modal: true,
         visible: false,
         initialShow: true,
         noEscape: true,
         html: null,
         callback: null,
         buttons: [
         {
            text: null, // OK button. Too early to localize at this time, do it when called instead
            handler: null,
            isDefault: true
         },
         {
            text: null, // Cancel button. Too early to localize at this time, do it when called instead
            handler: function()
            {
               this.destroy();
            }
         }]
      },

      /**
       * Intended usage: To ask the user for a simple text input, similar to JavaScript's prompt() function.
       *
       * @method getUserInput
       * @param config {object}
       * The config object is in the form of:
       * {
       *    title: {string},       // the title of the dialog, default is null
       *    text: {string},        // optional label next to input box
       *    value: {string},       // optional default value to populate textbox with
       *    callback: {object}     // Object literal specifying function callback to receive user input. Only called if default button config used.
       *                           // fn: function, obj: optional pass-thru object, scope: callback scope
       *    icon: null,            // the icon to display next to the text, default is null
       *    effect: {YAHOO.widget.ContainerEffect}, // the effect to use when showing and hiding the prompt, default is null
       *    effectDuration: {int}, // the time in seconds that the effect should run, default is 0.5
       *    modal: {boolean},      // if a grey transparent overlay should be displayed in the background
       *    initialShow {boolean}  // whether to call show() automatically on the panel
       *    close: {boolean},      // if a close icon should be displayed in the right upper corner, default is true
       *    buttons: []            // an array of button configs as described by YUI:s SimpleDialog, default is a single OK button
       *    okButtonText: {string} // Allows just the label of the OK button to be overridden
       *    noEscape: {boolean}    // indicates the the text property has already been escaped (e.g. to display HTML-based messages)
       *    html: {string},        // optional override for function-generated HTML <input> field. Note however that you must supply your own
       *                           //    button handlers in this case in order to get the user's input from the Dom.
       * }
       * @return {YAHOO.widget.SimpleDialog} The dialog widget
       */
      getUserInput: function(config)
      {
         if (this.defaultGetUserInputConfig.buttons[0].text === null)
         {
            /**
             * This default value could not be set at instantion time since the
             * localized messages weren't present at that time
             */
            this.defaultGetUserInputConfig.buttons[0].text = Alfresco.util.message("button.ok", this.name);
         }
         if (this.defaultGetUserInputConfig.buttons[1].text === null)
         {
            this.defaultGetUserInputConfig.buttons[1].text = Alfresco.util.message("button.cancel", this.name);
         }
         
         // Merge users config and the default config and check manadatory properties
         var c = YAHOO.lang.merge(this.defaultGetUserInputConfig, config);

         // Create the SimpleDialog that will display the text
         var prompt = new YAHOO.widget.SimpleDialog("userInput",
         {
            close: c.close,
            constraintoviewport: c.constraintoviewport,
            draggable: c.draggable,
            effect: c.effect,
            modal: c.modal,
            visible: c.visible,
            zIndex: this.zIndex++
         });

         // Show the title if it exists
         if (c.title)
         {
            prompt.setHeader($html(c.title));
         }

         // Generate the HTML mark-up if not overridden
         var html = c.html,
            id = Alfresco.util.generateDomId();
         if (html === null)
         {
            html = "";
            if (c.text)
            {
               html += '<label for="' + id + '">' + (c.noEscape ? c.text : $html(c.text)) + '</label>';
            }
            html += '<textarea id="' + id + '" tabindex="1">' + c.value + '</textarea>';
         }
         prompt.setBody(html);

         // Show the icon if it exists
         if (c.icon)
         {
            prompt.cfg.setProperty("icon", c.icon);
         }

         // Add the buttons to the dialog
         if (c.buttons)
         {
            if (c.okButtonText)
            {
               // Override OK button label
               c.buttons[0].text = c.okButtonText;
            }
            
            // Default handler if no custom button passed-in
            if (typeof config.buttons == "undefined" || typeof config.buttons[0] == "undefined")
            {
               // OK button click handler
               c.buttons[0].handler = 
               {
                  fn: function(event, obj)
                  {
                     // Grab the input, destroy the pop-up, then callback with the value
                     var value = null;
                     if (YUIDom.get(obj.id))
                     {
                        value = YUIDom.get(obj.id).value;
                     }
                     this.destroy();
                     if (obj.callback.fn)
                     {
                        obj.callback.fn.call(obj.callback.scope || window, value, obj.callback.obj);
                     }
                  },
                  obj:
                  {
                     id: id,
                     callback: c.callback
                  }
               };
            }
            prompt.cfg.queueProperty("buttons", c.buttons);
         }

         // Add the dialog to the dom, center it and show it (unless flagged not to).
         prompt.render(document.body);
         prompt.center();
         if (c.initialShow)
         {
            prompt.show();
         }
         
         // If a default value was given, set the selectionStart and selectionEnd properties
         if (c.value !== "")
         {
            YUIDom.get(id).selectionStart = 0;
            YUIDom.get(id).selectionEnd = c.value.length;
         }

         // Register the ESC key to close the panel
         var escapeListener = new YAHOO.util.KeyListener(document,
         {
            keys: YAHOO.util.KeyListener.KEY.ESCAPE
         },
         {
            fn: function(id, keyEvent)
            {
               this.destroy();
            },
            scope: prompt,
            correctScope: true
         });
         escapeListener.enable();         
         
         if (YUIDom.get(id))
         {
            YUIDom.get(id).focus();
         }
         
         return prompt;
      }
   });
}();


/**
 * Keeps track of multiple filters on a page. Filters should register() upon creation to be compliant.
 * @class Alfresco.util.FilterManager
 */
Alfresco.util.FilterManager = function()
{
   /**
    * Array of registered filters.
    * 
    * @property filters
    * @type Array
    */
   var filters = [];
   
   return (
   {
      /**
       * Main entrypoint for filters wishing to register themselves with the FilterManager
       * @method register
       * @param p_filterOwner {string} Name of the owner registering this filter. Used when owning exclusive filters.
       * @param p_filterIds {string|Array} Single or multiple filterIds this filter owns
       */
      register: function FM_register(p_filterOwner, p_filterIds)
      {
         var i, ii, filterId;
         
         if (typeof p_filterIds == "string")
         {
            p_filterIds = [p_filterIds];
         }
         
         for (i = 0, ii = p_filterIds.length; i < ii; i++)
         {
            filterId = p_filterIds[i];
            filters.push(
            {
               filterOwner: p_filterOwner,
               filterId: filterId
            });
            filters[filterId] = p_filterOwner;
         }
      },

      /**
       * Get filterOwner by filterId
       *
       * @method getOwner
       * @param p_filterId {string} FilterId
       * @return {string|null} filterOwner that has registered for the given filterId
       */
      getOwner: function FM_getOwner(p_filterId)
      {
         return (filters[p_filterId] || null);
      }
   });
}();


/**
 * Helper class for submitting data to serverthat wraps a
 * YAHOO.util.Connect.asyncRequest call.
 *
 * The request methid provides default behaviour for displaying messages on
 * success and error events and simplifies json handling with encoding and decoding.
 *
 * @class Alfresco.util.Ajax
 */
Alfresco.util.Ajax = function()
{
   return {

      /**
       * Constant for contentType of type json
       *
       * @property JSON
       * @type string
       */
      JSON: "application/json",

      /**
       * Constant for method of type GET
       *
       * @property GET
       * @type string
       */
      GET: "GET",

      /**
       * Constant for method of type POST
       *
       * @property POST
       * @type string
       */
      POST: "POST",

      /**
       * Constant for method of type PUT
       *
       * @property PUT
       * @type string
       */
      PUT: "PUT",

      /**
       * Constant for method of type DELETE
       *
       * @property DELETE
       * @type string
       */
      DELETE: "DELETE",

      /**
       * The default request config used by method request()
       *
       * @property defaultRequestConfig
       * @type object
       */
      defaultRequestConfig:
      {
         method: "GET",        // GET, POST and hopefully PUT or DELETE if ot works...
         url: null,            // Must be set by user
         dataObj: null,        // Will be encoded to parameters (key1=value1&key2=value2)
                               // or a json string if contentType is set to JSON
         dataStr: null,        // Will be used in the request body, could be a already created parameter or json string
                               // Will be overriden by the encoding result from dataObj if dataObj is provided
         dataForm: null,       // A form object or id that contains the data to be sent with request
         requestContentType: null,    // Set to JSON if json should be used
         responseContentType: null,    // Set to JSON if json should be used
         successCallback: null,// Object literal representing callback upon successful operation
         successMessage: null, // Will be displayed by Alfresco.util.PopupManager.displayMessage if no success handler is provided
         failureCallback: null,// Object literal representing callback upon failed operation
         failureMessage: null,  // Will be displayed by Alfresco.util.displayPrompt if no failure handler is provided
         execScripts: false,    // Whether embedded <script> tags will be executed within the successful response
         noReloadOnAuthFailure: false, // Default to reloading the page on HTTP 401 response, which will redirect through the login page
         object: null           // An object that can be passed to be used by the success or failure handlers
      },

      /**
       * Wraps a YAHOO.util.Connect.asyncRequest call and provides some default
       * behaviour for displaying error or success messages, uri encoding and
       * json encoding and decoding.
       *
       * JSON
       *
       * If requestContentType is JSON, config.dataObj (if available) is encoded
       * to a json string and set in the request body.
       *
       * If a json string already has been created by the application it should
       * be passed in as the config.dataStr which will be put in the rewuest body.
       *
       * If responseContentType is JSON the server response is decoded to a
       * json object and set in the "json" attribute in the response object
       * which is passed to the succes or failure callback.
       *
       * PARAMETERS
       *
       * If requestContentType is null, config.dataObj (if available) is encoded
       * to a normal parameter string which is added to the url if method is
       * GET or DELETE and to the request body if method is POST or PUT.
       *
       * FORMS
       * A form can also be passed it and submitted just as desccribed in the
       * YUI documentation.
       *
       * SUCCESS
       *
       * If the request is successful successCallback.fn is called.
       * If successCallback.fn isn't provided successMessage is displayed.
       * If successMessage isn't provided nothing happens.
       *
       * FAILURE
       *
       * If the request fails failureCallback.fn is called.
       * If failureCallback.fn isn't displayed failureMessage is displayed.
       * If failureMessage isn't provided the "best error message as possible"
       * from the server response is displayed.
       *
       * CALLBACKS
       *
       * The success or failure handlers can expect a response object of the
       * following form (they will be called in the scope defined by config.scope)
       *
       * {
       *   config: {object},         // The config object passed in to the request,
       *   serverResponse: {object}, // The response provided by YUI
       *   json: {object}            // The serverResponse parsed and ready as an object
       * }
       *
       * @method request
       * @param config {object} Description of the request that should be made
       * The config object has the following form:
       * {
       *    method: {string}               // GET, POST, PUT or DELETE, default is GET
       *    url: {string},                 // the url to send the request to, mandatory
       *    dataObj: {object},             // Will be encoded to parameters (key1=value1&key2=value2) or a json string if requestContentType is set to JSON
       *    dataStr: {string},             // the request body, will be overriden by the encoding result from dataObj if dataObj is provided
       *    dataForm: {HTMLElement},       // A form object or id that contains the data to be sent with request
       *    requestContentType: {string},  // Set to JSON if json should be used
       *    responseContentType: {string}, // Set to JSON if json should be used
       *    successCallback: {object},     // Callback for successful request, should have the following form: {fn: successHandler, scope: scopeForSuccessHandler}
       *    successMessage: {string},      // Will be displayed using Alfresco.util.PopupManager.displayMessage if successCallback isn't provided
       *    failureCallback: {object},     // Callback for failed request, should have the following form: {fn: failureHandler, scope: scopeForFailureHandler}
       *    failureMessage: {string},      // Will be displayed by Alfresco.util.displayPrompt if no failureCallback isn't provided
       *    execScripts: {boolean},        // Whether embedded <script> tags will be executed within the successful response
       *    noReloadOnAuthFailure: {boolean}, // Set to TRUE to prevent an automatic page refresh on HTTP 401 response
       *    object: {object}               // An object that can be passed to be used by the success or failure handlers
       * }
       */
      request: function(config)
      {
         // Merge the user config with the default config and check for mandatory parameters
         var c = YAHOO.lang.merge(this.defaultRequestConfig, config);
         Alfresco.util.assertNotEmpty(c.url, "Parameter 'url' can NOT be null");
         Alfresco.util.assertNotEmpty(c.method, "Parameter 'method' can NOT be null");

         // If a contentType is provided set it in the header
         if (c.requestContentType)
         {
            YAHOO.util.Connect.setDefaultPostHeader(false);
            YAHOO.util.Connect.initHeader("Content-Type", c.requestContentType);
         }

         if (c.requestContentType === this.JSON)
         {
            if (c.method.toUpperCase() === this.GET)
            {
               if (c.dataObj)
               {
                  throw new Error("Parameter 'method' can not be 'GET' when trying to submit data in dataObj with contentType '" + c.requestContentType + "'");
               }
            }
            else
            {
               // If json is used encode the dataObj parameter and put it in the body
               c.dataStr = YAHOO.lang.JSON.stringify(c.dataObj || {});
            }
         }
         else
         {
            if (c.dataObj)
            {
               // Normal URL parameters
               if (c.method.toUpperCase() === this.GET)
               {
                  // Encode the dataObj and put it in the url
                  c.url += (c.url.indexOf("?") == -1 ? "?" : "&") + this.jsonToParamString(c.dataObj, false);
               }
               else
               {
                  // Enccode the dataObj and put it in the body
                  c.dataStr = this.jsonToParamString(c.dataObj, true);
               }
            }
         }
         
         if (c.dataForm !== null)
         {
            // Set the form on the connection manager
            YAHOO.util.Connect.setForm(c.dataForm);
         }

         /**
          * The private "inner" callback that will handle json and displaying
          * of messages and prompts
          */
         var callback = 
         {
            success: this._successHandler,
            failure: this._failureHandler,
            scope: this,
            argument:
            {
               config: config
            }
         };

         // Encode url to make sure it is transfered correctly
         c.url = encodeURI(c.url);
         
         // Do we need to tunnel the HTTP method if the client can't support it (Adobe AIR)
         if (YAHOO.env.ua.air !== 0)
         {
            // Check for unsupported HTTP methods
            if (c.method.toUpperCase() == "PUT" || c.method.toUpperCase() == "DELETE")
            {
               // Check we're not tunnelling already
               var alfMethod = Alfresco.util.getQueryStringParameter("alf_method", c.url);
               if (alfMethod === null)
               {
                  c.url += (c.url.indexOf("?") == -1 ? "?" : "&") + "alf_method=" + c.method;
                  c.method = this.POST;
               }
            }
         }

         // Make the request
         YAHOO.util.Connect.asyncRequest (c.method, c.url, callback, c.dataStr);
      },

      /**
       * Helper function for pure json requests, where both the request and
       * response are using json. Will result in a call to request() with
       * requestContentType and responseContentType set to JSON.
       *
       * @method request
       * @param config {object} Description of the request that should be made
       */
      jsonRequest: function(config)
      {
         config.requestContentType = this.JSON;
         config.responseContentType = this.JSON;
         this.request(config);
      },

      /**
       * Helper function for pure json requests, where both the request and
       * response are using json. Will result in a call to request() with
       * requestContentType and responseContentType set to JSON and method set to GET.
       *
       * @method request
       * @param config {object} Description of the request that should be made
       */
      jsonGet: function(config)
      {
         config.method = this.GET;
         this.jsonRequest(config);
      },

      /**
       * Helper function for pure json requests, where both the request and
       * response are using json. Will result in a call to request() with
       * requestContentType and responseContentType set to JSON and method set to POST.
       *
       * @method request
       * @param config {object} Description of the request that should be made
       */
      jsonPost: function(config)
      {
         config.method = this.POST;
         this.jsonRequest(config);
      },

      /**
       * Helper function for pure json requests, where both the request and
       * response are using json. Will result in a call to request() with
       * requestContentType and responseContentType set to JSON and method set to PUT.
       *
       * @method request
       * @param config {object} Description of the request that should be made
       */
      jsonPut: function(config)
      {
         config.method = this.PUT;
         this.jsonRequest(config);
      },

      /**
       * Helper function for pure json requests, where both the request and
       * response are using json. Will result in a call to request() with
       * requestContentType and responseContentType set to JSON and method set to DELETE.
       *
       * @method request
       * @param config {object} Description of the request that should be made
       */
      jsonDelete: function(config)
      {
         config.method = this.DELETE;         
         this.jsonRequest(config);
      },

      /**
       * Takes an object and creates a decoded URL parameter string of it.
       * Note! Does not contain a '?' character in the beginning.
       *
       * @method request
       * @param obj
       * @param encode	indicates whether the parameter values should be encoded or not
       * @private
       */
      jsonToParamString: function(obj, encode)
      {
         var params = "", first = true, attr;
         
         for (attr in obj)
         {
            if (obj.hasOwnProperty(attr))
            {
               if (first)
               {
                  first = false;
               }
               else
               {
                  params += "&";
               }

               // Make sure no user input destroys the url 
               if (encode)
               {
                  params += encodeURIComponent(attr) + "=" + encodeURIComponent(obj[attr]);
               }
               else
               {
                  params += attr + "=" + obj[attr];
               }
            }
         }
         return params;
      },

      /**
       * Handles successful request triggered by the request() method.
       * If execScripts was requested, retrieve and execute the script(s).
       * Otherwise, fall through to the _successHandlerPostExec function immediately.
       *
       * @method request
       * @param serverResponse
       * @private
       */
      _successHandler: function(serverResponse)
      {
         // Get the config that was used in the request() method
         var config = serverResponse.argument.config;
         
         // Need to execute embedded "<script>" tags?
         if (config.execScripts)
         {
            var scripts = [];
            var script = null;
            var regexp = /<script[^>]*>([\s\S]*?)<\/script>/gi;
            while ((script = regexp.exec(serverResponse.responseText)))
            {
               scripts.push(script[1]);
            }
            scripts = scripts.join("\n");
            
            // Remove the script from the responseText so it doesn't get executed twice
            serverResponse.responseText = serverResponse.responseText.replace(regexp, "");

            // Use setTimeout to execute the script. Note scope will always be "window"
            window.setTimeout(scripts, 0);

            // Delay-call the PostExec function to continue response processing after the setTimeout above
            YAHOO.lang.later(0, this, this._successHandlerPostExec, serverResponse);
         }
         else
         {
            this._successHandlerPostExec(serverResponse);
         }
      },
      
      /**
       * Follow-up handler after successful request triggered by the request() method.
       * If execScripts was requested, this function continues after the scripts have been run.
       * If the responseContentType was set to json the response is decoded
       * for easy access to the success callback.
       * If no success callback is provided the successMessage is displayed
       * using Alfresco.util.PopupManager.displayMessage().
       * If no successMessage is provided nothing happens.
       *
       * @method request
       * @param serverResponse
       * @private
       */
      _successHandlerPostExec: function(serverResponse)
      {       
         // Get the config that was used in the request() method
         var config = serverResponse.argument.config;
         var callback = config.successCallback;
         if (callback && typeof callback.fn == "function")
         {
            var contentType = serverResponse.getResponseHeader["Content-Type"] || config.responseContentType;
            // User provided a custom successHandler
            var json = null;

            if (/^\s*application\/json/.test(contentType))
            {
               // Decode the response since it should be json
               json = Alfresco.util.parseJSON(serverResponse.responseText);
            }

            // Call the success callback in the correct scope
            callback.fn.call((typeof callback.scope == "object" ? callback.scope : this),
            {
               config: config,
               json: json,
               serverResponse: serverResponse
            }, callback.obj);
         }
         if (config.successMessage)
         {
            /**
             * User provided successMessage.
             */
            Alfresco.util.PopupManager.displayMessage(
            {
               text: config.successMessage
            });
         }
      },

      /**
       * Handles failed request triggered by the request() method.
       * If the responseContentType was set to json the response is decoded
       * for easy access to the failure callback.
       * If no failure callback is provided the failureMessage is displayed
       * using Alfresco.util.PopupManager.displayPrompt().
       * If no failureMessage is provided "the best available server response"
       * is displayed using Alfresco.util.PopupManager.displayPrompt().
       *
       * @method request
       * @param serverResponse
       * @private
       */                                                
      _failureHandler: function(serverResponse)
      {
         // Get the config that was used in the request() method
         var config = serverResponse.argument.config;

         // Our session has likely timed-out, so refresh to offer the login page
         if (serverResponse.status == 401 && !config.noReloadOnAuthFailure)
         {
            window.location.reload(true);
            return;
         }

         // Invoke the callback
         var callback = config.failureCallback, json = null;
         
         if ((callback && typeof callback.fn == "function") || (config.failureMessage))
         {
            if (callback && typeof callback.fn == "function")
            {
               // If the caller has defined an error message display that instead of displaying message about bad json syntax
               var displayBadJsonResult = true;
               if (config.failureMessage || config.failureCallback)
               {
                  displayBadJsonResult = false;
               }

               // User provided a custom failureHandler
               if (config.responseContentType === "application/json")
               {
                  json = Alfresco.util.parseJSON(serverResponse.responseText, displayBadJsonResult);
               }
               callback.fn.call((typeof callback.scope == "object" ? callback.scope : this),
               {
                  config: config,
                  json: json,
                  serverResponse: serverResponse
               }, callback.obj);
            }
            if (config.failureMessage)
            {
               /**
               * User did not provide a custom failureHandler, instead display
               * the failureMessage if it exists
               */
               Alfresco.util.PopupManager.displayPrompt(
               {
                  title: Alfresco.util.message("message.failure", this.name),
                  text: config.failureMessage
               });
            }
         }
         else
         {
            /**
             * User did not provide any failure info at all, display as good
             * info as possible from the server response.
             */
            if (config.responseContentType == "application/json")
            {
               json = Alfresco.util.parseJSON(serverResponse.responseText);
               Alfresco.util.PopupManager.displayPrompt(
               {
                  title: json.status.name,
                  text: json.message
               });
            }
            else if (serverResponse.statusText)
            {
               Alfresco.util.PopupManager.displayPrompt(
               {
                  title: Alfresco.util.message("message.failure", this.name),
                  text: serverResponse.statusText
               });
            }
            else
            {
               Alfresco.util.PopupManager.displayPrompt(
               {
                  title: Alfresco.util.message("message.failure", this.name),
                  text: "Error sending data to server."
               });
            }
         }
      }

   };
}();

/**
 * Helper class for setting the user mouse cursor and making sure its used the
 * same way in the whole application.
 *
 * Use setCursor with the predefined state constants to set the cursor.
 * Each constant has a css selector in base.css where it can be styled
 * differently if needed.
 *
 * @class Alfresco.util.Cursor
 */
Alfresco.util.Cursor = function()
{
   return (
   {
      /**
       * Show cursor in state to indicate that the current element is draggable.
       * Styled through css selector ".draggable" in base.css
       *
       * @property DRAGGABLE
       * @type string
       */
      DRAGGABLE: "draggable",

      /**
       * Show cursor in state to indicate that the current element is dragged.
       * Styled through css selector ".drag" in base.css
       *
       * @property DRAG
       * @type string
       */
      DRAG: "drag",

      /**
       * Show cursor in state to indicate that the element dragged over IS a valid drop point.
       * Styled through css selector ".dropValid" in base.css
       *
       * @property DROP_VALID
       * @type string
       */
      DROP_VALID: "dropValid",

      /**
       * Show cursor in state to indicate that the element dragged over is NOT a valid drop point.
       * Styled through css selector ".dropInvalid" in base.css
       *
       * @property DROP_INVALID
       * @type string
       */
      DROP_INVALID: "dropInvalid",

      /**
       * @method setCursorState
       * @param el {HTMLElement} Object that is dragged and who's style affects the cursor
       * @param cursor {string} Predifined constant from Alfresco.util.CURSOR_XXX
       */
      setCursorState: function(el, cursorState)
      {
         var allStates = [this.DRAGGABLE, this.DRAG, this.DROP_VALID, this.DROP_INVALID];
         for (var i = 0; i < allStates.length; i++)
         {
            var cs = allStates[i];
            if (cs === cursorState)
            {
               YUIDom.addClass(el, cursorState);
            }
            else
            {
               YUIDom.removeClass(el, cs);
            }
         }
      }
   });
}();

/**
 * Transition methods that handles browser limitations.
 *
 * @class Alfresco.util.Anim
 */
Alfresco.util.Anim = function()
{
   return (
   {
      /**
       * The default attributes for a fadeIn or fadeOut call.
       *
       * @property fadeAttributes
       * @type {object} An object literal of the following form:
       * {
       *    adjustDisplay: true, // Will handle style attribute "display" in
       *                         // the appropriate way depending on if its
       *                         // fadeIn or fadeOut, default is true.
       *    callback: null,      // A function that will get called after the fade
       *    scope: this,         // The scope the callback function will get called in
       */
      fadeAttributes:
      {
         adjustDisplay: true,
         callback: null,
         scope: this
      },

      /**
       * Displays an object with opacity 0, increases the opacity during
       * 0.5 seconds for browsers supporting opcaity.
       *
       * (IE does not support opacity)
       *
       * @method fadeIn
       * @param el {HTMLElement} element to fade in
       * @param attributes
       */
      fadeIn: function A_fadeIn(el, attributes)
      {
         return this._fade(el, true, attributes);
      },

      /**
       * Displays an object with opacity 1, decreases the opacity during
       * 0.5 seconds for browsers supporting opacity and finally hides it.
       *
       * (IE does not support opacity)
       *
       * @method fadeOut
       * @param el {HTMLElement} element to fade out
       * @param attributes
       */
      fadeOut: function A_fadeOut(el, attributes)
      {
         return this._fade(el, false, attributes);
      },

      /**
       * @method _fade
       * @param el {HTMLElement} element to fade in
       * @param fadeIn {boolean} true if fadeIn false if fadeOut
       * @param attributes
       */
      _fade: function A__fade(el, fadeIn, attributes)
      {
         el = YUIDom.get(el);
         // No manadatory elements in attributes, avoid null checks below though
         attributes = YAHOO.lang.merge(this.fadeAttributes, attributes ? attributes : {});
         var adjustDisplay = attributes.adjustDisplay;

         // todo test against functionality instead of browser
         var supportsOpacity = YAHOO.env.ua.ie === 0;

         // Prepare el before fade
         if (supportsOpacity)
         {
            YUIDom.setStyle(el, "opacity", fadeIn ? 0 : 1);
         }

         // Show the element, transparent if opacity supported,
         // otherwise its visible and the "fade in" is finished
         if (supportsOpacity)
         {
            YUIDom.setStyle(el, "visibility", "visible");
         }
         else
         {
            YUIDom.setStyle(el, "visibility", fadeIn ? "visible" : "hidden");
         }

         // Make sure element is displayed
         if (adjustDisplay && YUIDom.getStyle(el, "display") === "none")
         {
            YUIDom.setStyle(el, "display", "block");
         }

         // Put variables in scope so they can be used in the callback below
         var fn = attributes.callback;
         var scope = attributes.scope;
         var myEl = el;
         if (supportsOpacity)
         {
            // Do the fade (from value/opacity has already been set above)
            var fade = new YAHOO.util.Anim(el,
            {
               opacity:
               {
                  to: fadeIn ? (YAHOO.env.ua.webkit > 0 ? 0.99 : 1) : 0
               }
            }, 0.5);
            fade.onComplete.subscribe(function(e)
            {
               if (!fadeIn && adjustDisplay)
               {
                  // Hide element from Dom if its a fadeOut
                  YUIDom.setStyle(myEl, "display", "none");
               }
               if (fn)
               {
                  // Call custom callback
                  fn.call(scope ? scope : this);
               }
            });
            fade.animate();
         }
         else
         {
            if (!fadeIn && adjustDisplay)
            {
               // Hide element from Dom if its a fadeOut
               YUIDom.setStyle(myEl, "display", "none");
            }
            if (fn)
            {
               // Call custom callback
               fn.call(scope ? scope : this);
            }
         }
      },
      
      /**
       * Default attributes for a pulse call.
       *
       * @property pulseAttributes
       * @type {object} An object literal containing:
       *    callback: {object} Function definition for callback on complete. {fn, scope, obj}
       *    inColor: {string} Optional colour for the pulse (default is #ffff80)
       *    outColor: {string} Optional colour to fade back to (default is original element backgroundColor)
       *    inDuration: {int} Optional time for "in" animation (default 0.2s)
       *    outDuration: {int} Optional time for "out" animation (default 1.2s)
       *    clearOnComplete: {boolean} Set to clear the backgroundColor style on pulse complete (default true)
       */
      pulseAttributes:
      {
         callback: null,
         inColor: "#ffff80",
         inDuration: 0.2,
         outDuration: 1.2,
         clearOnComplete: true
      },
      
      /**
       * Pulses the background colo(u)r of an HTMLELement
       *
       * @method pulse
       * @param el {HTMLElement|string} element to fade out
       * @param attributes {object} Object literal containing optional custom values
       */
      pulse: function A_pulse(p_el, p_attributes)
      {
         // Shortcut return if animation library not loaded
         if (!YAHOO.util.ColorAnim)
         {
            return;
         }
         
         var el = YUIDom.get(p_el);
         if (el)
         {
            // Set outColor to existing backgroundColor
            var attr = YAHOO.lang.merge(this.pulseAttributes,
            {
               outColor: YUIDom.getStyle(el, "backgroundColor")
            });
            if (typeof p_attributes == "object")
            {
               attr = YAHOO.lang.merge(attr, p_attributes);
            }

            // The "in" animation class
            var animIn = new YAHOO.util.ColorAnim(el,
            {
               backgroundColor:
               {
                  to: attr.inColor
               }
            }, attr.inDuration);

            // The "out" animation class
            var animOut = new YAHOO.util.ColorAnim(el,
            {
               backgroundColor:
               {
                  to: attr.outColor
               }
            }, attr.outDuration);
            
            // onComplete functions
            animIn.onComplete.subscribe(function A_aI_onComplete()
            {
               animOut.animate();
            });
            
            animOut.onComplete.subscribe(function A_aO_onComplete()
            {
               if (attr.clearOnComplete)
               {
                  YUIDom.setStyle(el, "backgroundColor", "");
               }
               if (attr.callback && (typeof attr.callback.fn == "function"))
               {
                  attr.callback.fn.call(attr.callback.scope || this, attr.callback.obj);
               }
            });
            
            // Kick off the pulse animation
            animIn.animate();
         }
      }
   });
}();

/**
 * Helper class for managing nodeRefs
 *
 * @class Alfresco.util.NodeRef
 */
(function()
{
   Alfresco.util.NodeRef = function(nodeRef)
   {
      this.nodeRef = nodeRef;
      
      try
      {
         var a = nodeRef.replace(":/", "").split("/");
         this.storeType = a[0];
         this.storeId = a[1];
         this.id = a[2];
      }
      catch (e)
      {
         return null;
      }
   
      return (
      {
         nodeRef: this.nodeRef,
         storeType: this.storeType,
         storeId: this.storeId,
         id: this.id
      });
   };
})();

/**
 * Logging makes use of the log4javascript framework.
 *
 * Original code:
 *    Author: Tim Down <tim@log4javascript.org>
 *    Version: 1.4.1
 *    Edition: log4javascript
 *    Build date: 24 March 2009
 *    Website: http://log4javascript.org
 */
if (typeof log4javascript != "undefined")
{
   /**
    * Initial state on page load:
    *    Always show if AUTOLOGGING flag is true
    *    Show if AUTOLOGGING flag is false, but have enabled logger in this session
    */
   Alfresco.logger = log4javascript.getDefaultLogger();
   if (Alfresco.constants.AUTOLOGGING || Alfresco.util.getQueryStringParameter("log") == "on")
   {
      Alfresco.logger.info("Alfresco Share LOGGING enabled.");
   }
   else
   {
      if (Alfresco.util.getVar("logging", false))
      {
         Alfresco.logger.info("Alfresco Share LOGGING re-enabled.");
      }
      else
      {
         log4javascript.setEnabled(false);
      }
   }

   // Hook log window unload event
   Alfresco.logger.getEffectiveAppenders()[0].addEventListener("unload", function()
   {
      log4javascript.setEnabled(false);
      Alfresco.util.setVar("logging", false);
   });

   // Hook key sequence to enable log window
   if (window.addEventListener)
   {
      var sequence = [],
         logSequence = [17, 17, 16, 16], // Ctrl, Ctrl, Shift, Shift
         logSequenceLen = logSequence.length,
         logSequenceStr = logSequence.toString();
      
      document.addEventListener("keydown", function(e)
      {
         sequence.push(e.keyCode);
         while (sequence.length > logSequenceLen)
         {
            sequence.shift();
         }
         if (sequence.toString().indexOf(logSequenceStr) >= 0)
         {
            sequence = [];
            if (log4javascript.isEnabled())
            {
               log4javascript.setEnabled(false);
               Alfresco.logger.getEffectiveAppenders()[0].hide();
               Alfresco.util.setVar("logging", false);
            }
            else
            {
               log4javascript.setEnabled(true);
               Alfresco.logger.getEffectiveAppenders()[0].show();
               Alfresco.util.setVar("logging", true);
            }
         }
      }, true);
   }
}
else
{
   Alfresco.logger =
   {
      trace: function() {},
      debug: function() {},
      info: function() {},
      warn: function() {},
      error: function() {},
      fatal: function() {},
      isDebugEnabled: function()
      {
         return false;
      }
   };
}


/**
 * Format a date object to a user-specified mask
 * Modified to retrieve i18n strings from Alfresco.messages
 *
 * Original code:
 *    Date Format 1.1
 *    (c) 2007 Steven Levithan <stevenlevithan.com>
 *    MIT license
 *    With code by Scott Trenda (Z and o flags, and enhanced brevity)
 *
 * http://blog.stevenlevithan.com/archives/date-time-format
 *
 * @method Alfresco.thirdparty.dateFormat
 * @return {string}
 * @static
 */
Alfresco.thirdparty.dateFormat = function()
{
   /*** dateFormat
   	Accepts a date, a mask, or a date and a mask.
   	Returns a formatted version of the given date.
   	The date defaults to the current date/time.
   	The mask defaults ``"ddd mmm d yyyy HH:MM:ss"``.
   */
   var dateFormat = function()
   {
   	var   token        = /d{1,4}|m{1,4}|yy(?:yy)?|([HhMsTt])\1?|[LloZ]|"[^"]*"|'[^']*'/g,
      		timezone     = /\b(?:[PMCEA][SDP]T|(?:Pacific|Mountain|Central|Eastern|Atlantic) (?:Standard|Daylight|Prevailing) Time|(?:GMT|UTC)(?:[-+]\d{4})?)\b/g,
      		timezoneClip = /[^-+\dA-Z]/g,
      		pad = function (value, length)
      		{
      			value = String(value);
      			length = parseInt(length, 10) || 2;
      			while (value.length < length)
      			{
      				value = "0" + value;
      			}
      			return value;
      		};

   	// Regexes and supporting functions are cached through closure
   	return function (date, mask)
   	{
   		// Treat the first argument as a mask if it doesn't contain any numbers
   		if (arguments.length == 1 &&
   			   (typeof date == "string" || date instanceof String) &&
   			   !/\d/.test(date))
   		{
   			mask = date;
   			date = undefined;
   		}

   		date = date ? new Date(date) : new Date();
   		if (isNaN(date))
   		{
   			throw "invalid date";
   		}

   		mask = String(this.masks[mask] || mask || this.masks["default"]);

   		var d = date.getDate(),
   			 D = date.getDay(),
   			 m = date.getMonth(),
   			 y = date.getFullYear(),
   			 H = date.getHours(),
   			 M = date.getMinutes(),
   			 s = date.getSeconds(),
   			 L = date.getMilliseconds(),
   			 o = date.getTimezoneOffset(),
   			 flags =
   			 {
   				 d:    d,
   				 dd:   pad(d),
   				 ddd:  this.i18n.dayNames[D],
   				 dddd: this.i18n.dayNames[D + 7],
   				 m:    m + 1,
   				 mm:   pad(m + 1),
   				 mmm:  this.i18n.monthNames[m],
   				 mmmm: this.i18n.monthNames[m + 12],
   				 yy:   String(y).slice(2),
   				 yyyy: y,
   				 h:    H % 12 || 12,
   				 hh:   pad(H % 12 || 12),
   				 H:    H,
   				 HH:   pad(H),
   				 M:    M,
   				 MM:   pad(M),
   				 s:    s,
   				 ss:   pad(s),
   				 l:    pad(L, 3),
   				 L:    pad(L > 99 ? Math.round(L / 10) : L),
   				 t:    H < 12 ? this.TIME_AM.charAt(0) : this.TIME_PM.charAt(0),
   				 tt:   H < 12 ? this.TIME_AM : this.TIME_PM,
   				 T:    H < 12 ? this.TIME_AM.charAt(0).toUpperCase() : this.TIME_PM.charAt(0).toUpperCase(),
   				 TT:   H < 12 ? this.TIME_AM.toUpperCase() : this.TIME_PM.toUpperCase(),
   				 Z:    (String(date).match(timezone) || [""]).pop().replace(timezoneClip, ""),
   				 o:    (o > 0 ? "-" : "+") + pad(Math.floor(Math.abs(o) / 60) * 100 + Math.abs(o) % 60, 4)
   			 };

   		return mask.replace(token, function ($0)
   		{
   			return ($0 in flags) ? flags[$0] : $0.slice(1, $0.length - 1);
   		});
   	};
   }();

   /**
    * Alfresco wrapper: delegate to wrapped code
    */
   return dateFormat.apply(arguments.callee, arguments);
};
Alfresco.thirdparty.dateFormat.DAY_NAMES = (Alfresco.util.message("days.medium") + "," + Alfresco.util.message("days.long")).split(",");
Alfresco.thirdparty.dateFormat.MONTH_NAMES = (Alfresco.util.message("months.short") + "," + Alfresco.util.message("months.long")).split(",");
Alfresco.thirdparty.dateFormat.TIME_AM = Alfresco.util.message("date-format.am");
Alfresco.thirdparty.dateFormat.TIME_PM = Alfresco.util.message("date-format.pm");
Alfresco.thirdparty.dateFormat.masks =
{
	"default":       Alfresco.util.message("date-format.default"),
	defaultDateOnly: Alfresco.util.message("date-format.defaultDateOnly"),
	shortDate:       Alfresco.util.message("date-format.shortDate"),
	mediumDate:      Alfresco.util.message("date-format.mediumDate"),
	longDate:        Alfresco.util.message("date-format.longDate"),
	fullDate:        Alfresco.util.message("date-format.fullDate"),
	shortTime:       Alfresco.util.message("date-format.shortTime"),
	mediumTime:      Alfresco.util.message("date-format.mediumTime"),
	longTime:        Alfresco.util.message("date-format.longTime"),
	isoDate:         "yyyy-mm-dd",
	isoTime:         "HH:MM:ss",
	isoDateTime:     "yyyy-mm-dd'T'HH:MM:ss",
	isoFullDateTime: "yyyy-mm-dd'T'HH:MM:ss.lo"
};
Alfresco.thirdparty.dateFormat.i18n =
{
	dayNames: Alfresco.thirdparty.dateFormat.DAY_NAMES,
	monthNames: Alfresco.thirdparty.dateFormat.MONTH_NAMES
};


/**
 * Converts an ISO8601-formatted date into a JavaScript native Date object
 *
 * Original code:
 *    dojo.date.stamp.fromISOString
 *    Copyright (c) 2005-2008, The Dojo Foundation
 *    All rights reserved.
 *    BSD license (http://trac.dojotoolkit.org/browser/dojo/trunk/LICENSE)
 *
 * @method Alfresco.thirdparty.fromISO8601
 * @param formattedString {string} ISO8601-formatted date string
 * @return {Date|null}
 * @static
 */
Alfresco.thirdparty.fromISO8601 = function()
{
   var fromISOString = function()
   {
      //	summary:
      //		Returns a Date object given a string formatted according to a subset of the ISO-8601 standard.
      //
      //	description:
      //		Accepts a string formatted according to a profile of ISO8601 as defined by
      //		[RFC3339](http://www.ietf.org/rfc/rfc3339.txt), except that partial input is allowed.
      //		Can also process dates as specified [by the W3C](http://www.w3.org/TR/NOTE-datetime)
      //		The following combinations are valid:
      //
      //			* dates only
      //			|	* yyyy
      //			|	* yyyy-MM
      //			|	* yyyy-MM-dd
      // 			* times only, with an optional time zone appended
      //			|	* THH:mm
      //			|	* THH:mm:ss
      //			|	* THH:mm:ss.SSS
      // 			* and "datetimes" which could be any combination of the above
      //
      //		timezones may be specified as Z (for UTC) or +/- followed by a time expression HH:mm
      //		Assumes the local time zone if not specified.  Does not validate.  Improperly formatted
      //		input may return null.  Arguments which are out of bounds will be handled
      //		by the Date constructor (e.g. January 32nd typically gets resolved to February 1st)
      //		Only years between 100 and 9999 are supported.
      //
      //	formattedString:
      //		A string such as 2005-06-30T08:05:00-07:00 or 2005-06-30 or T08:05:00

      var isoRegExp = /^(?:(\d{4})(?:-(\d{2})(?:-(\d{2}))?)?)?(?:T(\d{2}):(\d{2})(?::(\d{2})(.\d+)?)?((?:[+-](\d{2}):(\d{2}))|Z)?)?$/;

      return function(formattedString)
      {
      	var match = isoRegExp.exec(formattedString);
      	var result = null;

      	if (match)
      	{
      		match.shift();
      		if (match[1]){match[1]--;} // Javascript Date months are 0-based
      		if (match[6]){match[6] *= 1000;} // Javascript Date expects fractional seconds as milliseconds

      		result = new Date(match[0]||1970, match[1]||0, match[2]||1, match[3]||0, match[4]||0, match[5]||0, match[6]||0);

      		var offset = 0;
      		var zoneSign = match[7] && match[7].charAt(0);
      		if (zoneSign != 'Z')
      		{
      			offset = ((match[8] || 0) * 60) + (Number(match[9]) || 0);
      			if (zoneSign != '-')
      			{
      			   offset *= -1;
      			}
      		}
      		if (zoneSign)
      		{
      			offset -= result.getTimezoneOffset();
      		}
      		if (offset)
      		{
      			result.setTime(result.getTime() + offset * 60000);
      		}
      	}

      	return result; // Date or null
      };
   }();
   
   return fromISOString.apply(arguments.callee, arguments);
};

/**
 * Converts a JavaScript native Date object into a ISO8601-formatted string
 *
 * Original code:
 *    dojo.date.stamp.toISOString
 *    Copyright (c) 2005-2008, The Dojo Foundation
 *    All rights reserved.
 *    BSD license (http://trac.dojotoolkit.org/browser/dojo/trunk/LICENSE)
 *
 * @method Alfresco.thirdparty.toISO8601
 * @param dateObject {Date} JavaScript Date object
 * @param options {object} Optional conversion options
 *    zulu = true|false
 *    selector = "time|date"
 *    milliseconds = true|false
 * @return {string}
 * @static
 */
Alfresco.thirdparty.toISO8601 = function()
{
   var toISOString = function()
   {
      //	summary:
      //		Format a Date object as a string according a subset of the ISO-8601 standard
      //
      //	description:
      //		When options.selector is omitted, output follows [RFC3339](http://www.ietf.org/rfc/rfc3339.txt)
      //		The local time zone is included as an offset from GMT, except when selector=='time' (time without a date)
      //		Does not check bounds.  Only years between 100 and 9999 are supported.
      //
      //	dateObject:
      //		A Date object
   	var _ = function(n){ return (n < 10) ? "0" + n : n; };

      return function(dateObject, options)
      {
      	options = options || {};
      	var formattedDate = [];
      	var getter = options.zulu ? "getUTC" : "get";
      	var date = "";
      	if (options.selector != "time")
      	{
      		var year = dateObject[getter+"FullYear"]();
      		date = ["0000".substr((year+"").length)+year, _(dateObject[getter+"Month"]()+1), _(dateObject[getter+"Date"]())].join('-');
      	}
      	formattedDate.push(date);
      	if (options.selector != "date")
      	{
      		var time = [_(dateObject[getter+"Hours"]()), _(dateObject[getter+"Minutes"]()), _(dateObject[getter+"Seconds"]())].join(':');
      		var millis = dateObject[getter+"Milliseconds"]();
      		if (options.milliseconds === undefined || options.milliseconds) 
      		{
      			time += "."+ (millis < 100 ? "0" : "") + _(millis);
      		}
      		if (options.zulu)
      		{
      			time += "Z";
      		}
      		else if (options.selector != "time")
      		{
      			var timezoneOffset = dateObject.getTimezoneOffset();
      			var absOffset = Math.abs(timezoneOffset);
      			time += (timezoneOffset > 0 ? "-" : "+") + 
      				_(Math.floor(absOffset/60)) + ":" + _(absOffset%60);
      		}
      		formattedDate.push(time);
      	}
      	return formattedDate.join('T'); // String
      };
   }();

   return toISOString.apply(arguments.callee, arguments);
}; 


/**
 * Alfresco BaseService.
 *
 * @namespace Alfresco.service
 * @class Alfresco.service.BaseService
 */
/**
 * BaseService constructor.
 *
 * @return {Alfresco.service.BaseService} The new Alfresco.service.BaseService instance
 * @constructor
 */
Alfresco.service.BaseService = function BaseService_constructor()
{
   return this;
};

Alfresco.service.BaseService.prototype =
{

   /**
    * Generic helper method for invoking a Alfresco.util.Ajax.request() from a responseConfig object
    *
    * @method _jsonCall
    * @param method {string} The method for the XMLHttpRequest
    * @param url {string} The url for the XMLHttpRequest
    * @param dataObj {object} An object that will be transformed to a json string and put in the request body
    * @param responseConfig.successCallback {object} A success callback object
    * @param responseConfig.successMessage {string} A success message
    * @param responseConfig.failureCallback {object} A failure callback object
    * @param responseConfig.failureMessage {string} A failure message
    * @private
    */
   _jsonCall: function BaseService__jsonCall(method, url, dataObj, responseConfig)
   {      
      responseConfig = responseConfig || {};
      Alfresco.util.Ajax.jsonRequest(
      {
         method: method,
         url: url,
         dataObj: dataObj,
         successCallback: responseConfig.successCallback,
         successMessage: responseConfig.successMessage,
         failureCallback: responseConfig.failureCallback,
         failureMessage: responseConfig.failureMessage
      });
   }
};

/**
 * Alfresco Preferences.
 *
 * @namespace Alfresco.service
 * @class Alfresco.service.Preferences
 */
(function()
{
   /**
    * Preferences constructor.
    *
    * @return {Alfresco.service.Preferences} The new Alfresco.service.Preferences instance
    * @constructor
    */
   Alfresco.service.Preferences = function Preferences_constructor()
   {
      Alfresco.service.Preferences.superclass.constructor.call(this);
      return this;
   };

   YAHOO.extend(Alfresco.service.Preferences, Alfresco.service.BaseService,
   {
      /**
       * Gets a user specific property
       *
       * @method url
       * @return {string} The base url to the preference webscripts
       * @private
       */
      _url: function Preferences_url()
      {
         return Alfresco.constants.PROXY_URI + "api/people/" + Alfresco.constants.USERNAME + "/preferences";
      },

      /**
       * Requests a user specific property
       *
       * @method request
       * @param name {string} The name of the property to get, or null or no param for all
       * @param responseConfig {object} A config object with only success and failure callbacks and messages
       */
      request: function Preferences_request(name, responseConfig)
      {
         this._jsonCall(Alfresco.util.Ajax.GET, this._url() + (name ? "?pf=" + name : ""), null, responseConfig);
      },

      /**
       * Sets a user specific property
       *
       * @method set
       * @param name {string} The name of the property to set
       * @param value {object} The value of the property to set
       * @param responseConfig {object} A config object with only success and failure callbacks and messages
       */
      set: function Preferences_set(name, value, responseConfig)
      {
         var preference = Alfresco.util.dotNotationToObject(name, value);
         this._jsonCall(Alfresco.util.Ajax.POST, this._url(), preference, responseConfig);
      },

      /**
       * Adds a value to a user specific property that is treated as a multi value.
       * Since arrays aren't supported in the webscript we store multiple values using a comma separated string.
       *
       * @method add
       * @param name {string} The name of the property to set
       * @param value {object} The value of the property to set
       * @param responseConfig {object} A config object with only success and failure callbacks and messages
       */
      add: function Preferences_add(name, value, responseConfig)
      {
         var n = name, v = value;
         var rc = responseConfig ? responseConfig : {};
         var originalSuccessCallback = rc.successCallback;
         rc.successCallback =
         {
            fn: function (response, obj)
            {
               // Make sure the original succes callback is used
               rc.successCallback = originalSuccessCallback;

               // Get the value for the preference name
               var preferences = Alfresco.util.dotNotationToObject(n, null);
               preferences = YAHOO.lang.merge(preferences, response.json);
               var values = Alfresco.util.findValueByDotNotation(preferences, n);

               // Parse string to array, add the value and convert to string again
               if (typeof values == "string" || values === null)
               {
                  var arrValues = values ? values.split(",") : [];
                  arrValues.push(v);

                  // Save preference with the new value
                  this.set(name, arrValues.join(","), rc);
               }
            },
            scope: this
         };
         this.request(name, rc);
      },

      /**
       * Removes a value from a user specific property that is treated as a multi value.
       * Since arrays aren't supported in the webscript we store multiple values using a comma separated string.
       *
       * @method add
       * @param name {string} The name of the property to set
       * @param value {object} The value of the property to set
       * @param responseConfig {object} A config object with only success and failure callbacks and messages
       */
      remove: function Preferences_remove(name, value, responseConfig)
      {
         var n = name, v = value;
         var rc = responseConfig ? responseConfig : {};
         var originalSuccessCallback = rc.successCallback;
         rc.successCallback =
         {
            fn: function (response, obj)
            {
               // Make sure the original succes callback is used
               rc.successCallback = originalSuccessCallback;

               // Get the value for the preference name
               var preferences = Alfresco.util.dotNotationToObject(n, null);
               preferences = YAHOO.lang.merge(preferences, response.json);
               var values = Alfresco.util.findValueByDotNotation(preferences, n);

               // Parse string to array, remove the value and convert to string again
               if (typeof values == "string")
               {
                  var arrValues = values ? values.split(",") : [];
                  arrValues = Alfresco.util.arrayRemove(arrValues, v);               

                  // Save preference without value
                  this.set(name, arrValues.join(","), rc);
               }
            },
            scope: this
         };
         this.request(name, rc);
      }
   });
   
   /**
    * Preference keys
    */
    Alfresco.service.Preferences.FAVOURITE_DOCUMENTS = "org.alfresco.share.documents.favourites";
    Alfresco.service.Preferences.FAVOURITE_SITES = "org.alfresco.share.sites.favourites";
    Alfresco.service.Preferences.IMAP_FAVOURITE_SITES = "org.alfresco.share.sites.imapFavourites";
    Alfresco.service.Preferences.COLLAPSED_TWISTERS = "org.alfresco.share.twisters.collapsed";

})();

/**
 * Manager object for managing adapters for HTML editors.
 *  
 */
Alfresco.util.RichEditorManager = (function()
{
   var editors = [];
   return (
   {
      /**
      * Store a reference to a specified editor
      *
      * @method addEditor
      * @param editorName {string} Name of html editor to use, including namespace eg YAHOO.widget.SimpleEditor
      * @param editor {object} reference to editor
      */
      addEditor: function (editorName, editor)
      {
         editors[editorName] = editor;
      },

      /**
      * Retrieve a previously added editor
      *
      * @method getEditor
      * @param editorName {string}  name of editor to retrieve
      * @return {object} Returns a reference to specified editor.
      */
      getEditor: function (editorName)
      {
         if (editors[editorName])
         {
            return editors[editorName];
         }
         return null;
      }
   });
})();

/**
 * @module RichEditor
 * Factory object for creating instances of adapters around
 * specified editor implementations. Eg tinyMCE/YUI Editor. Also augments
 * created editor with YAHOO.util.EventProvider.
 * Editor can be initialized instantly by passing in 'id' and 'config' parameters or later 
 * on by calling init() method.
 * 
 * Fires editorInitialized event when html editor is initialized.
 * 
 * @param editorName {String} Name of editor to use. Must be same as one registered with
 * Alfresco.util.RichEditManager.
 * @param id {String} Optional Id of textarea to turn into rich text editor
 * @param config {String} Optional config object literal to use to configure editor.
 */
Alfresco.util.RichEditor = function(editorName,id,config) 
{
   var editor = Alfresco.util.RichEditorManager.getEditor(editorName);
   if (editor)
   {
      var ed = new editor();
      
      YAHOO.lang.augmentObject(ed,
      {
         unsubscribe: function() 
         {
         },

         subscribe : function(event, fn, scope) 
         {
            var edtr = ed.getEditor();
            //yui custom events
            if (edtr.subscribe)
            {
               edtr.subscribe(event, fn, scope, true);
            }
            else if (edtr[event])
            {
               edtr[event].add(function() 
               {
                  fn.apply(scope,arguments);
               });
            }
            YAHOO.Bubbling.on(event, fn, scope);
         },

         on: function(event, fn, scope) 
         {
            YAHOO.Bubbling.on(event, fn, scope);
         }
      });

      if (id && config)
      {
         // Check we can support the requested language
         if (config.language)
         {
            var langs = Alfresco.util.message("tinymce_languages").split(","),
               lang = "en";
            for (var i = 0, j = langs.length; i < j; i++)
            {
               if (langs[i] == config.language)
               {
                  lang = config.language;
                  break;
               }
            }
            config.language = lang;
         }
         
         ed.init(id,config);
      }
      return ed;
   }
   return null;
};


/**
 * The Alfresco.component.Base class provides core component functions
 * and is intended to be extended by other UI components, rather than
 * instantiated on it's own.
 */
(function()
{
   /**
    * Alfresco.component.Base constructor.
    * 
    * @param name {String} The name of the component
    * @param id {String} he DOM ID of the parent element
    * @param components {Array} Optional: Array of required YAHOO
    * @return {object} The new instance
    * @constructor
    */
   Alfresco.component.Base = function(name, id, components)
   {
      // Mandatory properties
      this.name = (typeof name == "undefined" || name === null) ? "Alfresco.component.Base" : name;
      this.id = (typeof id == "undefined" || id === null) ? Alfresco.util.generateDomId() : id;

      // Initialise default prototype properties
      this.widgets = {};
      this.modules = {};
      this.services = {};
      
      // Register this component
      Alfresco.util.ComponentManager.register(this);

      // Load YUI Components if req'd
      if (YAHOO.lang.isArray(components))
      {
         Alfresco.util.YUILoaderHelper.require(components, this.onComponentsLoaded, this);
      }
      else
      {
         this.onComponentsLoaded();
      }

      return this;
   };
   
   Alfresco.component.Base.prototype =
   {
      /**
       * Object container for initialization options
       *
       * @property options
       * @type object
       * @default {}
       */
      options: {},

      /**
       * Object container for storing YUI widget instances.
       * 
       * @property widgets
       * @type object
       * @default null
       */
      widgets: null,

      /**
       * Object container for storing module instances.
       * 
       * @property modules
       * @type object
       * @default null
       */
      modules: null,

      /**
       * Object container for storing service instances.
       * 
       * @property services
       * @type object
       */
      services: null,

      /**
       * Set multiple initialization options at once.
       *
       * @method setOptions
       * @param obj {object} Object literal specifying a set of options
       * @return {object} returns 'this' for method chaining
       */
      setOptions: function Base_setOptions(obj)
      {
         this.options = YAHOO.lang.merge(this.options, obj);
         return this;
      },

      /**
       * Set messages for this component.
       *
       * @method setMessages
       * @param obj {object} Object literal specifying a set of messages
       * @return {object} returns 'this' for method chaining
       */
      setMessages: function Base_setMessages(obj)
      {
         Alfresco.util.addMessages(obj, this.name);
         return this;
      },

      /**
       * Fired by YUILoaderHelper when required component script files have
       * been loaded into the browser.
       *
       * @method onComponentsLoaded
       */
      onComponentsLoaded: function Base_onComponentsLoaded()
      {
         if (this.onReady && this.onReady.call)
         {
            YUIEvent.onContentReady(this.id, this.onReady, this, true);
         }
      },

      /**
       * Gets a custom message
       *
       * @method msg
       * @param messageId {string} The messageId to retrieve
       * @return {string} The custom message
       */
      msg: function Base_msg(messageId)
      {
         return Alfresco.util.message.call(this, messageId, this.name, Array.prototype.slice.call(arguments).slice(1));
      }
   };
})();

/**
 * The Alfresco.component.BaseFilter class provides core functions for a "twister-style" filter.
 * It can be extended by other UI filters, or simply instantiated on it's own.
 */
(function()
{
   /**
    * Alfresco.component.BaseFilter constructor.
    * 
    * @param name {String} The name of the component
    * @param id {String} he DOM ID of the parent element
    * @param components {Array} Optional: Array of required YAHOO
    * @return {object} The new instance
    * @constructor
    */
   Alfresco.component.BaseFilter = function(name, id, components)
   {
      Alfresco.component.BaseFilter.superclass.constructor.apply(this, arguments);
      
      this.filterName = this.name.substring(this.name.lastIndexOf(".") + 1);
      this.controlsDeactivated = false;
      this.uniqueEventKey = Alfresco.util.generateDomId(null, "filter");

      // Decoupled event listeners
      YAHOO.Bubbling.on("filterChanged", this.onFilterChanged, this);
      YAHOO.Bubbling.on("deactivateAllControls", this.onDeactivateAllControls, this);
      
      return this;
   };

   YAHOO.extend(Alfresco.component.BaseFilter, Alfresco.component.Base,
   {
      /**
       * Filter name, automatically generated from component name.
       * 
       * @property filterName
       * @type {string}
       */
      filterName: null,

      /**
       * Selected filter.
       * 
       * @property selectedFilter
       * @type {element}
       */
      selectedFilter: null,

      /**
       * Flag to indicate whether all controls are deactivated or not.
       * 
       * @property controlsDeactivated
       * @type {boolean}
       */
      controlsDeactivated: null,

      /**
       * Unique event key used to hook DOM clicks on filters
       * 
       * @property uniqueEventKey
       * @type {string}
       */
      uniqueEventKey: null,

      /**
       * Set the filterId(s) this filter will be owning.
       *
       * @method setFilterIds
       * @param p_aFilterIds {array} Array of filterIds this filter will be owning
       */
      setFilterIds: function BaseFilter_setFilterIds(p_aFilterIds)
      {
         // Register the filter
         Alfresco.util.FilterManager.register(this.name, p_aFilterIds);
      },

      /**
       * Fired by YUI when parent element is available for scripting.
       * Component initialisation, including instantiation of YUI widgets and event listener binding.
       *
       * @method onReady
       */
      onReady: function BaseFilter_onReady()
      {
         var me = this,
            headers = YUISelector.query("h2", this.id);
         
         if (YAHOO.lang.isArray(headers))
         {
            // Create twister from the first H2 tag found by the query
            Alfresco.util.createTwister(headers[0], this.filterName);
         }
         
         // Add the unique event key into the filter link nodes
         var filterLinks = YUISelector.query("li a", this.id);
         for (var i = 0, ii = filterLinks.length; i < ii; i++)
         {
            YUIDom.addClass(filterLinks[i], this.uniqueEventKey);
         }
         
         // ...and attach a global listener to the unique event key
         YAHOO.Bubbling.addDefaultAction(this.uniqueEventKey, function BaseFilter_filterAction(layer, args)
         {
            var anchor = args[1].anchor,
               owner = YAHOO.Bubbling.getOwnerByTagName(anchor, "span");
            
            if ((owner !== null) && !me.controlsDeactivated)
            {
               var href = anchor.getAttribute("href", 2);
               // Check the filter isn't a link (yes wiki, we're all looking at you)
               // Note: IE6 (and IE7 for DHTML operations) just doesn't get it, even with the second parameter on getAttribute()
               if (YAHOO.env.ua.ie > 0 && YAHOO.env.ua.ie < 8)
               {
                  var d = href.length - 1;
                  if (d < 0 || href.lastIndexOf("#") != d)
                  {
                     return false;
                  }
               }
               else if (anchor.getAttribute("href", 2).length > 1)
               {
                  return false;
               }
               
               var filterId = owner.className,
                  filterData = anchor.rel;

               YAHOO.Bubbling.fire("filterChanged",
               {
                  filterOwner: me.name,
                  filterId: filterId,
                  filterData: filterData
               });

               // If a function has been provided which corresponds to the filter Id, then call it
               if (typeof me[filterId] == "function")
               {
                  me[filterId].call(me);
               }

               args[1].stop = true;
            }

            return true;
         });
      },

      /**
       * BUBBLING LIBRARY EVENT HANDLERS FOR PAGE EVENTS
       * Disconnected event handlers for inter-component event notification
       */

      /**
       * Fired when the currently active filter has changed
       *
       * @method onFilterChanged
       * @param layer {string} the event source
       * @param args {object} arguments object
       */
      onFilterChanged: function BaseFilter_onFilterChanged(layer, args)
      {
         var obj = args[1];
         if ((obj !== null) && (obj.filterId !== null))
         {
            obj.filterOwner = obj.filterOwner || Alfresco.util.FilterManager.getOwner(obj.filterId);
            
            if (obj.filterOwner == this.name)
            {
               // Remove the old highlight, as it might no longer be correct
               if (this.selectedFilter !== null)
               {
                  YUIDom.removeClass(this.selectedFilter, "selected");
               }

               // Need to find the selectedFilter element, from the current filterId
               var candidates = YUISelector.query("." + obj.filterId, this.id);
               if (candidates.length == 1)
               {
                  // This component now owns the active filter
                  this.selectedFilter = candidates[0].parentNode;
                  YUIDom.addClass(this.selectedFilter, "selected");
               }
               else if (candidates.length > 1)
               {
                  if (obj.filterData.indexOf("]") !== -1)
                  {
                     // Special case handling, as YUI Selector doesn't work with "]" within an attribute
                     // See http://yuilibrary.com/projects/yui2/ticket/1978321
                     for (var i = 0, ii = candidates.length; i < ii; i++)
                     {
                        if (candidates[i].firstChild.rel == obj.filterData)
                        {
                           // This component now owns the active filter
                           this.selectedFilter = candidates[i].parentNode;
                           YUIDom.addClass(this.selectedFilter, "selected");
                        }
                     }
                  }
                  else
                  {
                     candidates = YUISelector.query("a[rel='" + obj.filterData.replace("'", "\'") + "']", this.id);
                     if (candidates.length == 1)
                     {
                        // This component now owns the active filter
                        this.selectedFilter = candidates[0].parentNode.parentNode;
                        YUIDom.addClass(this.selectedFilter, "selected");
                     }
                  }
               }
            }
            else
            {
               // Currently filtering by something other than this component
               if (this.selectedFilter !== null)
               {
                  YUIDom.removeClass(this.selectedFilter, "selected");
               }
            }
         }
      },

      /**
       * Deactivate All Controls event handler
       *
       * @method onDeactivateAllControls
       * @param layer {object} Event fired
       * @param args {array} Event parameters (depends on event type)
       */
      onDeactivateAllControls: function BaseFilter_onDeactivateAllControls(layer, args)
      {
         this.controlsDeactivated = true;
         var filters = YUISelector.query("a." + this.uniqueEventKey, this.id);
         for (var i = 0, ii = filters.length; i < ii; i++)
         {
            YUIDom.addClass(filters[i], "disabled");
         }
      }
   });
})();
