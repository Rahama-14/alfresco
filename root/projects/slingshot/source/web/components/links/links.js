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
 * Links component
 *
 *
 * @namespace Alfresco
 * @class Alfresco.Links
 */

(function()
{
   /**
    * YUI Library aliases
    */
   var Dom = YAHOO.util.Dom,
         Event = YAHOO.util.Event,
         Element = YAHOO.util.Element;
         
   var $html = Alfresco.util.encodeHTML;

   /**
    * Links constructor.
    *
    * @param {String} htmlId The HTML id of the parent element
    * @return {Alfresco.SiteFinder} The new SiteFinder instance
    * @constructor
    */
   Alfresco.Links = function(htmlId)
   {

      this.id = htmlId;
      this.name = "Alfresco.Links";
      this.currentFilter = {};

      this.widgets = {};

      /**
       * Object literal used to generate unique tag ids
       *
       * @property tagId
       * @type object
       */

      this.tagId =
      {
         id: 0,
         tags: {}
      };

      /**
       * The deleted link CSS style.
       */
      this.DELETEDCLASS = "delete-link";

      /**
       * The edited link CSS style.
       */
      this.EDITEDCLASS = "edit-link";

      /* Register this component */
      Alfresco.util.ComponentManager.register(this);

      /* Load YUI Components */
      Alfresco.util.YUILoaderHelper.require(["button", "container", "datasource", "datatable", "json", "resize"], this.onComponentsLoaded, this);

      YAHOO.Bubbling.on("filterChanged", this.onFilterChanged, this);
      YAHOO.Bubbling.on("linksListRefresh", this.onLinksListRefresh, this);

      this.newLinkBtn = null;
      this.changeListViewBtn = null;
      this.linksMenu = null;
   };

   Alfresco.Links.prototype =
   {
      /**
       * Tells whether an action is currently ongoing.
       *
       * @property busy
       * @type boolean
       * @see _setBusy/_releaseBusy
       */
      busy: false,

      /**
       * Object container for initialization options
       *
       * @property options
       * @type object
       */
      options:
      {
         /**
          * Current siteId.
          *
          * @property siteId
          * @type string
          */
         siteId: "",


         /**
          * Initially used filter name and id.
          */
         initialFilter: {},

         /**
          * Number of items displayed per page
          *
          * @property pageSize
          * @type int
          */
         pageSize: 10,

         /**
          * Flag indicating whether the list shows a detailed view or a simple one.
          *
          * @property simpleView
          * @type boolean
          */
         simpleView: false,

         /**
          * permission delete
          */
         permissionDelete: true,

         /**
          * permission update
          */
         permissionUpdate:true,

         /**
          * Length of preview content loaded for each topic
          */
         maxContentLength: 512,

         /**
          * Minimal length of filter panel
          */

         MIN_FILTER_PANEL_WIDTH : 150,

         /**
          * Maximal length of filter panel
          */

         MAX_FILTER_PANEL_WIDTH : 640 - ((YAHOO.env.ua.ie > 0) && (YAHOO.env.ua.ie < 7) ? 160 : 0),

         /**
          * The pagination flag.
          *
          * @property: usePagination
          * @type: boolean
          * @default: true
          */
         usePagination : true,

         /**
          * Minimal height of filter panel
          */
         MAX_FILTER_PANEL_HEIGHT : 200,

         /**
          * ContainerId representing root container
          *
          * @property containerId
          * @type string
          */
         containerId: ""
      },

      /**
       * Set multiple initialization options at once.
       *
       * @method setOptions
       * @param obj {object} Object literal specifying a set of options
       */
      setOptions: function Links_setOptions(obj)
      {
         this.options = YAHOO.lang.merge(this.options, obj);
         return this;
      },

      /**
       * Fired by YUILoaderHelper when required component script files have
       * been loaded into the browser.
       *
       * @method onComponentsLoaded
       */
      onComponentsLoaded: function Links_onComponentsLoaded()
      {
         Event.onContentReady(this.id, this.onReady, this, true);
      },

      /**
       * Fired by YUI when parent element is available for scripting.
       * Component initialisation, including instantiation of YUI widgets and event listener binding.
       *
       * @method onReady
       */
      onReady: function Links_onReady()
      {
         this.activate();
      },

      /** Object container for storing YUI widget instances.
       *
       * @property widgets
       * @type object
       */
      widgets: null,

      /**
       * init DataSource
       * @method createDataSource
       * @return {Alfresco.Links} returns 'this' for method chaining
       */
      createDataSource : function Links_createDataSource()
      {
         var uriResults = YAHOO.lang.substitute(Alfresco.constants.PROXY_URI + "api/links/site/{site}/{container}",
         {
            site: this.options.siteId,
            container: this.options.containerId
         });

         this.widgets.dataSource = new YAHOO.util.DataSource(uriResults);
         this.widgets.dataSource.responseType = YAHOO.util.DataSource.TYPE_JSON;
         this.widgets.dataSource.connXhrMode = 'queueRequests';
         this.widgets.dataSource.responseSchema =
         {
            resultsList: 'items',
            fields: ['name', 'title', 'description', 'url', 'tags', 'internal', 'createdOn', 'author', 'permissions'],
            metaFields:
            {
               paginationRecordOffset: 'startIndex',
               totalRecords: 'total',
               metadata: 'metadata'
            }
         };

         return this;
      },
      /**
       * Updates the toolbar using the passed permissions
       * @method updateToolbar
       * @param linkPermissions {object} Container permissions
       */
      updateToolbar: function Links_updateToolbar(linkPermissions)
      {  
         if (linkPermissions.create === "false")
         {
            this.newLinkBtn.set("disabled", true);
         }
      },
      /**
       * Set messages for this component.
       *
       * @method setMessages
       * @param obj {object} Object literal specifying a set of messages
       * @return {Alfresco.Links} returns 'this' for method chaining
       */
      setMessages: function Links_setMessages(obj)
      {
         Alfresco.util.addMessages(obj, this.name);
         return this;
      },

      /**
       * Initialise DataTable
       *
       * @method createDataTable
       */
      createDataTable : function Links_createDataTable()
      {
         var me = this;

         var generateUserProfileUrl = function DL_generateUserProfileUrl(userName)
         {
            return Alfresco.util.uriTemplate("userpage",
            {
               userid: userName,
               pageid: "profile"
            });
         };

         /**
          * Selector custom datacell formatter
          *
          * @method renderCellSelected
          * @param elCell {object}
          * @param oRecord {object}
          * @param oColumn {object}
          * @param oData {object|string}
          */
         var renderCellSelected = function Links_renderCellSelected(elCell, oRecord, oColumn, oData)
         {
            elCell.innerHTML = '<input class="checkbox-column" type="checkbox" />';
            elCell.firstChild.onclick = function()
            {
               var count = me.getSelectedLinks().length;
               me.linksMenu.set("disabled", count === 0);
            };
         };

         /**
          * Description custom datacell formatter
          *
          * @method renderCellDescription
          * @param elCell {object}
          * @param oRecord {object}
          * @param oColumn {object}
          * @param oData {object|string}
          */
         var renderCellDescription = function Links_renderCellDescription(elCell, oRecord, oColumn, oData)
         {
            var data = oRecord.getData();
            var name = data["title"],
               url = data["url"],
               description = data["description"],
               createdOn = data["createdOn"],
               author = data["author"],
               tags = data["tags"],
               internal = data["internal"];
            
            var linksViewUrl = me.generateLinksViewUrl(me.options.siteId, me.options.containerId, data.name);
            var tagsStr = "";
            if (tags.length > 0)
            {
               for (var i = 0; i < tags.length; i++)
               {
                  tagsStr += me._generateTagLink(tags[i]);
                  if (i != (tags.length - 1))
                  {
                     tagsStr += ', &nbsp;';
                  }
               }
            }
            else
            {
               tagsStr = me._msg("dialog.tags.none");
            }
            var innerHtml = '<h3 class="link-title"><a href="' + linksViewUrl + '">' + $html(name) + '</a></h3>';
            
            innerHtml += '<div class="detail"><span class="item"><em style="padding-right: 2px; float: left">' + me._msg("details.url") + ':</em> ' +
                         '<a style="float: left;"' +  (internal ? '' : ' target="_blank" class="external"') + ' href=' + (url.indexOf("://") === -1 || url[0] === '/' ? 'http://' : '') +
                         $html(url) + '>' + $html(url) + '</a></span></div>';

            if (!me.options.simpleView)
            {
               innerHtml += '<div class="detail"><span class="item"><em>' + me._msg("details.created.on") + ':</em> ' + Alfresco.util.formatDate(data["createdOn"]) + '</span>' +
                            '<span class="item"><em>' + me._msg("details.created.by") + ':</em> ' + Alfresco.util.people.generateUserLink(author) + '</span></div>';

               innerHtml += '<div class="detail"><span class="item"><em>' + me._msg("details.description") + ':</em> ' + $html(description) + '</span></div>';

               innerHtml += '<div class="detail"><span class="tag-item"><em>' + me._msg("details.tags") + ': </em>' + tagsStr + '</span></div>';
            }

            elCell.innerHTML = innerHtml;
         };

         /**
          * Actions custom datacell formatter
          *
          * @method renderCellActions
          * @param elCell {object}
          * @param oRecord {object}
          * @param oColumn {object}
          * @param oData {object|string}
          */
         var renderCellActions = function Links_renderCellActions(elCell, oRecord, oColumn, oData)
         {
            var prefix = oRecord.getData("title"),
               permissions = oRecord.getData("permissions");
               
            elCell.style.display = "none";
            elCell.innerHTML = "<div class='" + me.EDITEDCLASS + "'><a id='edit-" + prefix + "'><span>" + me._msg("links.edit") + "</a></span></div>" +
               "<div class='" + me.DELETEDCLASS + "'><a id='delete-" + prefix + "'><span>" + me._msg("links.delete") + "</a></span></div>";

            var elink = elCell.getElementsByTagName("a")[0];
            var ec = elCell.childNodes[0];
            var dlink = elCell.getElementsByTagName("a")[1];
            var dc = elCell.childNodes[1];

            // Edit permission?
            if (permissions["edit"])
            {
               ec.onclick = function Links_onEditLink()
               {
                  window.location = YAHOO.lang.substitute(Alfresco.constants.URL_CONTEXT + "page/site/{site}/links-linkedit?linkId={linkId}",
                  {
                     site: me.options.siteId,
                     linkId: oRecord.getData('name')
                  });
               };

               ec.onmouseover = function()
               {
                  Dom.addClass(this, me.EDITEDCLASS + "-over");
               };

               ec.onmouseout = function()
               {
                  Dom.removeClass(this, me.EDITEDCLASS + "-over");
               };
            }
            else
            {
               Dom.addClass(ec, 'hidden');
            }
            
            // Delete permission?
            if (permissions["delete"])
            {
               dc.onclick = function ()
               {
                  var mes = me._msg("dialog.confirm.message.delete", prefix);
                  var callback = function()
                  {
                     me.deleteLinks([oRecord]);
                  };
                  me.showConfirmDialog(mes, callback);
               };

               dc.onmouseover = function()
               {
                  Dom.addClass(this, me.DELETEDCLASS + "-over");
               };

               dc.onmouseout = function()
               {
                  Dom.removeClass(this, me.DELETEDCLASS + "-over");
               };

            }
            else
            {
               Dom.addClass(dc, 'hidden');
            }

            // Styling
            Dom.setStyle(elCell.parentNode, "border-left", "3px solid #fff");
            if (me.options.simpleView)
            {
               Dom.addClass(elCell.parentNode, 'simple-view');
            }
            else
            {
               Dom.removeClass(elCell.parentNode, 'simple-view');
            }
         };
         
         var columnDefinitions =
         [
            {
               key: 'selected', label: 'Selected', sortable: false, formatter: renderCellSelected
            },
            {
               key: 'title', label: 'Title', sortable: false, formatter: renderCellDescription
            },
            {
               key: 'description', label: 'Description', formatter: renderCellActions
            }
         ];

         YAHOO.widget.DataTable.CLASS_SELECTED = "links-selected-row";

         YAHOO.widget.DataTable.MSG_EMPTY = '<span class="datatable-msg-empty">' + this._msg("links.empty") + '</span>';

         this.widgets.paginator = new YAHOO.widget.Paginator(
         {
            containers: [this.id + "-paginator"],
            rowsPerPage: this.options.pageSize,
            initialPage: 1,
            template: this._msg("pagination.template"),
            pageReportTemplate: this._msg("pagination.template.page-report"),
            previousPageLinkLabel: this._msg("pagination.previousPageLinkLabel"),
            nextPageLinkLabel: this._msg("pagination.nextPageLinkLabel")
         });

         // called by the paginator on state changes
         var handlePagination = function Links_handlePagination (state, dt)
         {
            me.updateLinks(
            {
               page: state.page
            });
         };

         this.widgets.dataTable = new YAHOO.widget.DataTable(this.id + '-links', columnDefinitions, this.widgets.dataSource,
         {
            renderLoopSize: 32,
            initialLoad: false,
            paginationEventHandler: handlePagination,
            paginator: this.widgets.paginator
         });

         this.widgets.dataTable.doBeforeLoadData = function Links_doBeforeLoadData(sRequest, oResponse, oPayload)
         {
            if (oResponse.error)
            {
               try
               {
                  var response = YAHOO.lang.JSON.parse(oResponse.responseText);
                  YAHOO.widget.DataTable.MSG_ERROR = response.message;
               }
               catch(e)
               {
               }

            }
            else if (oResponse.results && !me.options.usePagination)
            {
               this.renderLoopSize = oResponse.results.length >> YAHOO.env.ua.gecko ? 3 : 5;
            }

            // Must return true to have the "Loading..." message replaced by the error message
            return true;
         };

         this.widgets.dataTable.subscribe("tableMsgShowEvent", function(oArgs)
         {
            // NOTE: Scope needs to be DataTable
            this._elMsgTbody.parentNode.style.width = "";
         });

         this.widgets.dataTable.set("selectionMode", "single");

         var onRowMouseover = function Links_onRowMouseover(e)
         {
            me.widgets.dataTable.selectRow(e.target);
            e.target.cells[2].childNodes[0].style.display = "";
            e.target.cells[2].childNodes[0].style.width = "100px";
            e.target.cells[2].style.borderLeft = "1px solid #C5E6E9";
         };

         var onRowMouseout = function Links_onRowMouseout(e)
         {
            me.widgets.dataTable.unselectRow(e.target);
            e.target.cells[2].childNodes[0].style.display = "none";
            e.target.cells[2].style.borderLeft = "1px solid #FFF";
         };

         this.widgets.dataTable.subscribe("rowMouseoverEvent", onRowMouseover);
         this.widgets.dataTable.subscribe("rowMouseoutEvent", onRowMouseout);

         var filterObj = YAHOO.lang.merge(
         {
            filterId: "all",
            filterOwner: "Alfresco.LinkFilter",
            filterData: null
         }, this.options.initialFilter);
         
         YAHOO.Bubbling.fire("filterChanged", filterObj);
      },

      /**
      * Generate a view url for a given site, link id.
      *
      * @param linkId the id/name of the link
      * @return an url to access the link
      */
      generateLinksViewUrl: function Links_generateLinksViewUrl(site, container, linkId)
      {
         var url = YAHOO.lang.substitute(Alfresco.constants.URL_CONTEXT + "page/site/{site}/links-view?linkId={linkId}",
         {
            site: site,
            container: container,
            linkId: linkId
         });
         return url;
      },

      /**
       * Links Filter changed event handler
       *
       * @method onFilterChanged
       * @param layer {object} Event fired (unused)
       * @param args {array} Event parameters (new filterId)
       */
      onFilterChanged: function Links_onFilterChanged(layer, args)
      {
         var obj = args[1];
         if ((obj !== null) && (obj.filterId !== null))
         {
            this.currentFilter =
            {
               filterId: obj.filterId,
               filterOwner: obj.filterOwner,
               filterData: obj.filterData
            };
            this.updateLinks({ page: 1 });
         }
      },

      /**
       * Links Refresh Required event handler
       *
       * @method onLinksListRefresh
       * @param layer {object} Event fired (unused)
       * @param args {array} Event parameters (unused)
       */
      onLinksListRefresh: function Links_onLinksListRefresh(layer, args)
      {
         this.updateLinks();
      },

      /**
       * Updates links list by calling data webscript with current site and filter information
       *
       * @method updateLinks
       */
      updateLinks:function Links_updateLinks(p_obj)
      {

         function successHandler(sRequest, oResponse, oPayload)
         {
            this.widgets.dataTable.onDataReturnInitializeTable.call(this.widgets.dataTable, sRequest, oResponse, oPayload);
            this.updateListTitle();
            this.linksMenu.set("disabled", this.getSelectedLinks().length === 0);
            var perm = oResponse.meta.metadata.linkPermissions;
            this.options.permissionDelete = perm["delete"];
            this.options.permissionUpdate = perm["edit"];
            this.updateToolbar(perm);
         }

         function failureHandler(sRequest, oResponse)
         {

            if (oResponse.status == 401)
            {
               // Our session has likely timed-out, so refresh to offer the login page
               window.location.reload(true);
            }
            else
            {
               try
               {
                  var response = YAHOO.lang.JSON.parse(oResponse.responseText);
                  YAHOO.widget.DataTable.MSG_ERROR = response.message;
                  this.widgets.dataTable.showTableMessage(response.message, YAHOO.widget.DataTable.CLASS_ERROR);
                  if (oResponse.status == 404)
                  {
                     YAHOO.Bubbling.fire("deactivateAllControls");
                  }
               }
               catch(e)
               {
               }
            }
         }
         this.widgets.dataSource.sendRequest(this._buildLinksParams(p_obj || {}),
         {
            success: successHandler,
            failure: failureHandler,
            scope: this
         });

      },

      /**
       * Update the list title.
       * @method updateListTitle
       */
      updateListTitle: function Links_updateListTitle()
      {
         var elem = Dom.get(this.id + '-listTitle');
         var title = this._msg("title.generic");

         var filterOwner = this.currentFilter.filterOwner;
         var filterId = this.currentFilter.filterId;
         var filterData = this.currentFilter.filterData;
         if (filterOwner == "Alfresco.LinkFilter")
         {
            switch (filterId)
            {
               case "all":
                  title = this._msg("title.all");
                  break;
               case "user":
                  title = this._msg("title.user");
                  break;
               case "recent":
                  title = this._msg("title.recent");
                  break;
            }

         }
         else if (filterOwner == "Alfresco.LinkTags")
         {
            title = this._msg("title.bytag", $html(filterData));
         }

         elem.innerHTML = title;
      },

      /**
       * Deactivate All Controls event handler
       *
       * @method onDeactivateAllControls
       * @param layer {object} Event fired
       * @param args {array} Event parameters (depends on event type)
       */
      onDeactivateAllControls: function Links_onDeactivateAllControls(layer, args)
      {
         for (var widget in this.widgets)
         {
            if (widget)
            {
               this.widgets[widget].set("disabled", true);
            }
         }
      },

      /**
       * activation of components
       * @method activate.
       */
      activate : function Links_activate()
      {
         this.attachButtons();
         Dom.setStyle(this.id + '-links-header', 'visibility', 'visible');
         Dom.setStyle(this.id + '-body', 'visibility', 'visible');

         this.createDataSource();
         this._attachResize();
         this.createDataTable();
      },

      /**
       * topic list resize event handler
       * @method onTopicListResize.
       * @param width {int}
       */
      onTopicListResize : function Links_onTopicListResize(width)
      {
         if (width)
         {
            Dom.setStyle(Dom.get("divLinkFilters"), "height", "auto");
            Dom.setStyle(Dom.get("divLinkList"), "margin-left", width + 3 + "px");
         }
      },

      /**
      * menu item event handler
      * @method onMenuItemClick.
      * @param sType, aArgs, p_obj
      */
      onMenuItemClick:function Links_onMenuItemClick(sType, aArgs, p_obj)
      {
         var me = this;
         switch (aArgs[1]._oAnchor.className.split(" ")[0])
         {
            case "delete-item":
               var callback = function()
               {
                  var arrLinks = me.getSelectedLinks();
                  me.deleteLinks(arrLinks);
               };
               this.showConfirmDialog(this._msg("dialog.confirm.message.delete.selected"), callback);
               break;
               
            case "deselect-item" :
               this.deselectAll();
               this.linksMenu.set("disabled", true);
               break;
         }

      },

      /**
      * deselect all links
      * @method deselectAll.
      * @param no params
      */
      deselectAll : function Links_deselectAll()
      {
         var rows = this.widgets.dataTable.getTbodyEl().rows;
         for (var i = 0; i < rows.length; i++)
         {
            rows[i].cells[0].getElementsByTagName('input')[0].checked = false;

         }
      },

      /**
       * init links buttons
       * @method attachButtons.
       */
      attachButtons : function Links_attachButtons()
      {
         var me = this;
         this.newLinkBtn = Alfresco.util.createYUIButton(this, "create-link-button", this.showCreateLinkDlg,
         {
            disabled: false,
            value: "create"
         });

         this.linksMenu = Alfresco.util.createYUIButton(this, "selected-i-dd", this.onMenuItemClick,
         {
            disabled : true,
            type: "menu",
            menu:"selectedItems-menu"
         });

         this.changeListViewBtn = Alfresco.util.createYUIButton(this, "viewMode-button", this.changeListView,
         {
         });

         this.linksSelectMenu = Alfresco.util.createYUIButton(this, "select-button", this.onSelectItemClick,
         {
            type: "menu",
            menu: "selecItems-menu"
         });

         this.widgets.rssFeed = Alfresco.util.createYUIButton(this, "rss-feed", null,
         {
            type: "link"
         });

         this.widgets.rssFeed.set("href", this._generateRSSFeedUrl());
      },

      /**
       * Handler on Menu Item Click
       * @param sType
       * @param aArgs
       * @param p_obj
       * @method onSelectItemClick
       */
      onSelectItemClick : function Links_onSelectItemClick(sType, aArgs, p_obj)
      {
         var elem = YAHOO.env.ua.ie ? aArgs[0].srcElement : aArgs[0].target;
         if (elem.tagName.toLocaleLowerCase() != "span")
         {
            elem = elem.getElementsByTagName("span")[0];
         }
         switch (elem.className.split(" ")[0])
         {
            case "links-action-deselect-all" :
               this.deselectAll();
               this.linksMenu.set("disabled", true);
               break;

            case "links-action-select-all" :
               this.selectAll();
               this.linksMenu.set("disabled", !this.getSelectedLinks().length);
               break;

            case "links-action-invert-selection" :
               this.invertAll();
               break;
         }
      },

      /**
       * Invert All Selection on the page
       * @method invertAll
       */
      invertAll : function Links_invertAll()
      {
         var isDisable = false;
         var rows = this.widgets.dataTable.getTbodyEl().rows;
         for (var i = 0; i < rows.length; i++)
         {
            var ipt = rows[i].cells[0].getElementsByTagName('input')[0];
            ipt.checked = !ipt.checked;
            isDisable = ipt.checked ? true : isDisable;
         }
         this.linksMenu.set("disabled", !isDisable);
      },

      /**
       * select All Tags on the page
       * @method selectAll
       */
      selectAll : function Links_selectAll()
      {
         var rows = this.widgets.dataTable.getTbodyEl().rows;
         for (var i = 0; i < rows.length; i++)
         {
            rows[i].cells[0].getElementsByTagName('input')[0].checked = true;
         }
      },

      /**
       * show 'Create Link' dialog
       * @method showCreateLinkDlg.
       */
      showCreateLinkDlg : function Links_showCreateLinkDlg()
      {
         var url = YAHOO.lang.substitute(Alfresco.constants.URL_CONTEXT + "page/site/{site}/links-linkedit",
         {
            site: this.options.siteId
         });
         window.location = url;
      },

      /**
       * change list view
       * @method changeListView.
       */
      changeListView : function Links_changeListView()
      {
         var records = this.widgets.dataTable.getRecordSet().getRecords();
         var rows = this.widgets.dataTable.getTbodyEl().rows;
         var colDefinitions = this.widgets.dataTable.getColumnSet().getDefinitions();

         this.options.simpleView = !this.options.simpleView;
         var j = 0;
         for (var i in records)
         {
            if (i)
            {
               colDefinitions[1].formatter.call(this, rows[j].cells[1].firstChild, records[i]);
               colDefinitions[2].formatter.call(this, rows[j].cells[2].firstChild, records[i]);
               j++;
            }
         }
         this.changeListViewBtn.set("label", this._msg(this.options.simpleView ? "header.detailedList" : "header.simpleList"));
      },

      /**
       * @method deleteLinks
       * @param arr {array}
       */
      deleteLinks: function Links_deleteLinks(arr)
      {
         var me = this;
         if (!this._setBusy(this._msg('message.wait')))
         {
            return;
         }

         // get the url to call
         var ids = [];
         for (var i in arr)
         {
            if (i)
            {
               ids.push(arr[i].getData().name);
            }
         }

         var url = YAHOO.lang.substitute(Alfresco.constants.PROXY_URI + "api/links/delete/site/{site}/{container}",
         {
            site: this.options.siteId,
            container: this.options.containerId
         });

         // ajax request success handler
         var onDeletedSuccess = function Links_deleteLinkConfirm_onDeletedSuccess(response)
         {
            // remove busy message
            this._releaseBusy();

            // reload the table data
            this.updateLinks();
            YAHOO.Bubbling.fire("tagRefresh");
         };

         // execute ajax request
         Alfresco.util.Ajax.request(
         {
            url: url,
            method: "POST",
            requestContentType : "application/json",
            successMessage: this._msg("message.delete.success"),
            successCallback:
            {
               fn: onDeletedSuccess,
               scope: this
            },
            failureMessage: this._msg("message.delete.failure"),
            failureCallback:
            {
               fn: function(response)
               {
                  response.config.failureMessage = YAHOO.lang.JSON.parse(response.serverResponse.responseText).message;
                  this._releaseBusy();
               },
               scope: this
            },
            dataObj :
            {
               items : ids
            }
         });

      },

      /**
       * Adds the link.
       *
       * @param rowData {object} the row's data.
       * @method createLink.
       */
      createLink: function Links_createLink(data)
      {
         this.updateLinks({ page: 1 });
      },

      /**
       * Updates the link.
       *
       * @param rowData {object} the row's data.
       * @param row {YAHOO.widget.Record}.
       * @method updateLink.
       */
      onUpdateLink: function Links_onUpdateLink(rowData, row)
      {
         this.updateLinks();

      },

      /**
       * Show delete confirm dialog.
       * @param row {YAHOO.widget.Record} the row which needs for delete.
       */
      showConfirmDialog: function Links_showConfirmDialog(mes, callback)
      {
         var me = this;

         var prompt = Alfresco.util.PopupManager.displayPrompt(
         {
            text: mes,
            buttons: [
            {
               text: this._msg("button.delete"),
               handler: function()
               {
                  callback();
                  this.destroy();
               }
            },
            {
               text: this._msg("button.cancel"),
               handler: function()
               {
                  this.destroy();
               },
               isDefault: true
            }]
         });
      },

      /**
       * Gets the array of selected links.
       *
       * @method getSelectedLinks
       */
      getSelectedLinks: function Links_getSelectedLinks()
      {
         var arr = [];
         var rows = this.widgets.dataTable.getTbodyEl().rows;
         for (var i = 0; i < rows.length; i++)
         {
            if (rows[i].cells[0].getElementsByTagName('input')[0].checked)
            {
               var data = this.widgets.dataTable.getRecord(i);
               if (data)
               {
                  arr.push(data);
               }
            }
         }

         return arr;
      },

      /**
       * PRIVATE FUNCTIONS
       */

      /**
       * activation of resize
       * @method _attachResize.
       */
      _attachResize : function Links__attachResize()
      {
         this.widgets.horizResize = new YAHOO.util.Resize("divLinkFilters",
         {
            handles: ["r"],
            minWidth: this.options.MIN_FILTER_PANEL_WIDTH,
            maxWidth: this.options.MAX_FILTER_PANEL_WIDTH
         });


         this.widgets.horizResize.on("resize",
               function(eventTarget)
               {
                  this.onTopicListResize(eventTarget.width);
               }, this, true);

         this.widgets.horizResize.resize(null, null, this.options.MIN_FILTER_PANEL_WIDTH, 0, 0, true);
      },

      /**
       * Generate ID alias for tag, suitable for DOM ID attribute
       *
       * @method _generateTagId
       * @param tagName {string} Tag name
       * @return {string} A unique DOM-safe ID for the tag
       */
      _generateTagId : function Links__generateTagId(tagName)
      {
         var id = 0;
         var tagId = this.tagId;
         if (tagName in tagId.tags) {
            id = tagId.tags[tagName];
         }
         else
         {
            tagId.id++;
            id = tagId.tags[tagName] = tagId.id;
         }
         return this.id + "-tagId-" + id;
      },

      /**Build URI parameter string for doclist JSON data webscript
       *
       * @method _buildDocListParams
       * @param p_obj.page {string} Page number
       * @param p_obj.pageSize {string} Number of items per page
       */
      _buildLinksParams: function Links_buildLinksParams(p_obj)
      {
         var params = {
            contentLength: this.options.maxContentLength,
            fromDate: null,
            toDate: null,
            tag: null,

            page: this.widgets.paginator.get("page") || "1",
            pageSize: this.widgets.paginator.get("rowsPerPage")
         };

         // Passed-in overrides
         if (typeof p_obj == "object")
         {
            params = YAHOO.lang.merge(params, p_obj);
         }

         // calculate the startIndex param
         params.startIndex = (params.page - 1) * params.pageSize;

         // check what url to call and with what parameters
         var filterOwner = this.currentFilter.filterOwner;
         var filterId = this.currentFilter.filterId;
         var filterData = this.currentFilter.filterData;

         // check whether we got a filter or not
         var url = "";
         if (filterOwner == "Alfresco.LinkFilter")
         {
            switch (filterId)
            {
               case "all":
                  url = "?filter=all";
                  break;
               case "user":
                  url = "?filter=user";
                  break;
               case "recent":
                  url = "?filter=recent";
                  break;
            }
         }
         else if (filterOwner == "Alfresco.LinkTags")
         {
            url = "?filter=tag";
            params.tag = filterData;
         }

         // build the url extension
         var urlExt = "";
         for (var paramName in params)
         {
            if (params[paramName] !== null)
            {
               urlExt += "&" + paramName + "=" + encodeURIComponent(params[paramName]);
            }
         }
         if (urlExt.length > 0)
         {
            urlExt = urlExt.substring(1);
         }
         return url + "&" + urlExt;

      },

      /**
       * Gets a custom message
       *
       * @method _msg
       * @param messageId {string} The messageId to retrieve
       * @return {string} The custom message
       * @private
       */
      _msg: function Links_msg(messageId)
      {
         return Alfresco.util.message.call(this, messageId, this.name, Array.prototype.slice.call(arguments).slice(1));
      },

      /**
       * Init the 'Create Link' dialog.
       *
       * @method _initCreateLinkDialog.
       */
      _initCreateLinkDialog: function Links_initCreateLinkDialog()
      {
         this.widgets.createLinkDlg = new Alfresco.LinksEditDialog(this.id + "-editdlg");
         this.widgets.createLinkDlg.setOptions(
         {
            siteId:this.options.siteId,
            containerId:this.options.containerId,
            templateUrl: Alfresco.constants.URL_SERVICECONTEXT + "components/links/modaldialogs/edit-dialog"
         });
         this.widgets.createLinkDlg.init();
      },

      /**
       * Removes the busy message and marks the component as non-busy
       */
      _releaseBusy: function Links_releaseBusy()
      {
         if (this.busy)
         {
            this.widgets.busyMessage.destroy();
            this.busy = false;
            return true;
         }
         else
         {
            return false;
         }
      },

      /**
       * Displays the provided busyMessage but only in case
       * the component isn't busy set.
       *
       * @return true if the busy state was set, false if the component is already busy
       */
      _setBusy: function Links__setBusy(busyMessage)
      {
         if (this.busy)
         {
            return false;
         }
         this.busy = true;
         this.widgets.busyMessage = Alfresco.util.PopupManager.displayMessage(
         {
            text: busyMessage,
            spanClass: "wait",
            displayTime: 0
         });
         return true;
      },

      /**
       * Generate the html markup for a tag link.
       *
       * @method _generateTagLink
       * @param tagName {string} the tag to create a link for
       * @return {string} the markup for a tag
       */
      _generateTagLink : function Links_generateTagLink(tagName)
      {
         var encodedTagName = $html(tagName);
         var html = '';
         html += '<span id="' + this._generateTagId(tagName) + '" class="nodeAttrValue">';
         html += '<a href="#" class="tag-link" title="' + encodedTagName + '">';
         html += '<span>' + encodedTagName + '</span>';
         html += '</a>';
         html += '</span>';
         return html;
      },

      /**
       * Generates the HTML mark-up for the RSS feed link
       *
       * @method _generateRSSFeedUrl
       * @private
       */
      _generateRSSFeedUrl: function Links__generateRSSFeedUrl()
      {
            var url = YAHOO.lang.substitute(Alfresco.constants.URL_CONTEXT + "service/components/links/rss?site={site}",
            {
               site: this.options.siteId
            });

       return url;
      }
   };
})();