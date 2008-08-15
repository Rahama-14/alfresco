/**
 * Topic component.
 * Shows and allows to edit a topic.
 * 
 * @namespace Alfresco
 * @class Alfresco.DiscussionsTopic
 */
(function()
{
   /**
    * YUI Library aliases
    */
   var Dom = YAHOO.util.Dom,
       Event = YAHOO.util.Event,
       Element = YAHOO.util.Element;

   /**
    * Alfresco Slingshot aliases
    */
   var $html = Alfresco.util.encodeHTML;

   /**
   * Topic constructor.
   * 
   * @param {String} htmlId The HTML id of the parent element
   * @return {Alfresco.TopicView} The new Topic instance
   * @constructor
   */
   Alfresco.DiscussionsTopic = function(htmlId)
   {
      this.name = "Alfresco.DiscussionsTopic";
      this.id = htmlId;
      
      /* Register this component */
      Alfresco.util.ComponentManager.register(this);

      /* Load YUI Components */
      Alfresco.util.YUILoaderHelper.require(["datasource", "json", "connection", "event", "button", "menu", "editor"], this.onComponentsLoaded, this);
     
      /* Decoupled event listeners */
      YAHOO.Bubbling.on("tagSelected", this.onTagSelected, this);
           
      return this;
   }
   
   Alfresco.DiscussionsTopic.prototype =
   {
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
          * Current containerId.
          * 
          * @property containerId
          * @type string
          */
         containerId: "discussions",

         /**
          * Id of the topic to display.
          * 
          * @property topicId
          * @type string
          */
         topicId: ""

      },
     
      /**
       * Holds the data displayed in this component.
       */
      topicData: null,
      
      /**
       * Object container for storing YUI widget instances.
       * 
       * @property widgets
       * @type object
       */
      widgets: {},
      
      /**
       * Object container for storing module instances.
       * 
       * @property modules
       * @type object
       */
      modules: {},
      
      /**
       * Object literal used to generate unique tag ids
       * 
       * @property tagId
       * @type object
       */
      tagId:
      {
         id: 0,
         tags: {}
      },
      
      /**
       * Set multiple initialization options at once.
       *
       * @method setOptions
       * @param obj {object} Object literal specifying a set of options
       */
      setOptions: function DiscussionsTopic_setOptions(obj)
      {
         this.options = YAHOO.lang.merge(this.options, obj);
         return this;
      },
     
      /**
       * Set multiple initialization options at once.
       *
       * @method setOptions
       * @param obj {object} Object literal specifying a set of options
       */
      setMessages: function DiscussionsTopic_setMessages(obj)
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
      onComponentsLoaded: function DiscussionsTopic_onComponentsLoaded()
      {
         YAHOO.util.Event.onContentReady(this.id, this.onReady, this, true);
      },
   
  
      /**
       * Fired by YUI when parent element is available for scripting.
       * Component initialisation, including instantiation of YUI widgets and event listener binding.
       *
       * @method onReady
       */
      onReady: function DiscussionsTopic_onReady()
      {
         var me = this;
         
         // Hook action events.
         var fnActionHandlerDiv = function DiscussionsTopic_fnActionHandlerDiv(layer, args)
         {
            var owner = YAHOO.Bubbling.getOwnerByTagName(args[1].anchor, "div");
            if (owner !== null)
            {
               var action = "";
               action = owner.className;
               if (typeof me[action] == "function")
               {
                  me[action].call(me, me.topicData.name);
                  args[1].stop = true;
               }
            }
            return true;
         }
         YAHOO.Bubbling.addDefaultAction("topic-action-link-div", fnActionHandlerDiv);
         
         // register tag action handler, which will issue tagSelected bubble events.
         Alfresco.util.tags.registerTagActionHandler(this);
          
         // initialize the mouse over listener
         Alfresco.util.rollover.registerHandlerFunctions(this.id, this.onTopicElementMouseEntered, this.onTopicElementMouseExited);
          
         // load the topic data
         this._loadTopicData();
      },
      
      
      /**
       * Loads the topic data and updates the ui.
       */
      _loadTopicData: function DiscussionsTopic__loadTopicData()
      {
         // ajax request success handler
         var loadTopicDataSuccess = function DiscussionsTopic__loadTopicData_loadTopicDataSuccess(response)
         {
            // set the loaded data
            var data = response.json.item
            this.topicData = data;
            
            // render the ui
            this.renderUI();
            
            // inform the comment components about the loaded post
            this._fireTopicDataChangedEvent();
         };
         
         // construct url to call
         var url = YAHOO.lang.substitute(Alfresco.constants.PROXY_URI + "api/forum/post/site/{site}/{container}/{topicId}",
         {
            site : this.options.siteId,
            container: this.options.containerId,
            topicId: this.options.topicId
         });
         
         // execute ajax request
         Alfresco.util.Ajax.request(
         {
            url: url,
            successCallback:
            {
               fn: loadTopicDataSuccess,
               scope: this
            },
            failureMessage: this._msg("message.loadtopicdata.failure")
         });
      },

      /**
       * Renders the UI with the data available in the component.
       */
      renderUI: function DiscussionsTopic_renderUI()
      {   
         // get the container div
         var viewDiv = Dom.get(this.id + '-topic-view-div');
         
         // render the topic and insert the resulting html
         var html = this.renderTopic(this.topicData);
         viewDiv.innerHTML = html;
         
         // attach the rollover listeners
         Alfresco.util.rollover.registerListenersByClassName(this.id, 'topic', 'div');
      },
      
      /**
       * Renders the topic.
       * 
       * @param data {object} the data object containing the topic data
       * @return {string} html representing the data
       */
      renderTopic: function DiscussionsTopic_renderTopic(data)
      {
         var html = '';
          
         html += '<div id="' + this.id + '-topicview" class="node topic topicview">'
         
         // actions
         html += '<div class="nodeEdit">';
         html += '<div class="onAddReply"><a href="#" class="topic-action-link-div">' + this._msg("action.reply") + '</a></div>';   
         if (data.permissions.edit)
         {
            html += '<div class="onEditTopic"><a href="#" class="topic-action-link-div">' + this._msg("action.edit") + '</a></div>';
         }
         if (data.permissions['delete'])
         {
            html += '<div class="onDeleteTopic"><a href="#" class="topic-action-link-div">' + this._msg("action.delete") + '</a></div>';
         }
         html += '</div>';
  
         // avatar
         html += '<div class="authorPicture">' + Alfresco.util.people.generateUserAvatarImg(data.author) + '</div>';

         // content
         html += '<div class="nodeContent">';
         html += '<div class="nodeTitle"><a href="' + Alfresco.util.discussions.getTopicViewPage(this.options.siteId, this.options.containerId, data.name) + '">' + $html(data.title) + '</a> ';
         if (data.isUpdated)
         {
            html += '<span class="nodeStatus">(' + this._msg("post.updated") + ')</span>';
         }
         html += '</div>';
         
         html += '<div class="published">';
         html += '<span class="nodeAttrLabel">' + this._msg("post.createdOn") + ': </span>';
         html += '<span class="nodeAttrValue">' + Alfresco.util.formatDate(data.createdOn) + '</span>';
         html += '<span class="spacer"> | </span>';
         html += '<span class="nodeAttrLabel">' + this._msg("post.author") + ': </span>';
         html += '<span class="nodeAttrValue">' + Alfresco.util.people.generateUserLink(data.author) + '</span>';
         html += '<br />';
         if (data.lastReplyBy)
         {
            html += '<span class="nodeAttrLabel">' + this._msg("post.lastReplyBy") + ': </span>';
            html += '<span class="nodeAttrValue">' + Alfresco.util.people.generateUserLink(data.lastReplyBy) + '</span>';                  
            html += '<span class="spacer"> | </span>';
            html += '<span class="nodeAttrLabel">' + this._msg("post.lastReplyOn") + ': </span>';
            html += '<span class="nodeAttrValue">' + Alfresco.util.formatDate(data.lastReplyOn) + '</span>';
         }
         else
         {
            html += '<span class="nodeAttrLabel">' + this._msg("replies.label") + ': </span>';
            html += '<span class="nodeAttrValue">' + this._msg("replies.noReplies") + '</span>';                  
         }
         html += '</div>';
             
         html += '<div class="userLink">' + Alfresco.util.people.generateUserLink(data.author) + ' ' + this._msg("said") + ':</div>';
         html += '<div class="content yuieditor">' + data.content + '</div>';
         html += '</div>'
         // end view

         // begin footer
         html += '<div class="nodeFooter">';
         html += '<span class="nodeAttrLabel replyTo">' + this._msg("replies.label") + ': </span>';
         html += '<span class="nodeAttrValue">(' + data.totalReplyCount + ')</span>';
         html += '<span class="spacer"> | </span>';
             
         html += '<span class="nodeAttrLabel tag">' + this._msg("tags.label") +': </span>';
         if (data.tags.length > 0)
         {
            for (var x=0; x < data.tags.length; x++)
            {
               if (x > 0)
               {
                  html += ", ";
               }
               html += Alfresco.util.tags.generateTagLink(this, data.tags[x]);
            }
         }
         else
         {
            html += '<span class="nodeAttrValue">' + this._msg("tags.noTags") + '</span>';
         }
         html += '</div></div></div>';
          
         return html;
      },

      /**
       * Handler for add reply action link
       */
      onAddReply: function DiscussionsTopic_onAddReply(htmlId, ownerId, param)
      {
         YAHOO.Bubbling.fire('addReplyToPost',
         {
            postRef : this.topicData.nodeRef
         });
      },
     
      /**
       * Handler for edit topic action link
       */
      onEditTopic: function DiscussionsTopic_onEditTopic()
      {
         this._loadEditForm();
      },
     
      /**
       * Handler for delete topic action link
       */
      onDeleteTopic: function DiscussionsTopic_onDeleteTopic()
      {
         // ajax request success handler
         var onDeleted = function onDeleted(response)
         {
            var listUrl = YAHOO.lang.substitute(Alfresco.constants.URL_CONTEXT + "page/site/{site}/discussions-topiclist?container={container}",
            {
               site: this.options.siteId,
               container: this.options.containerId
            });
            window.location = listUrl;
         };
         
         // construct the url to call
         var url = YAHOO.lang.substitute(Alfresco.constants.PROXY_URI + "api/forum/post/site/{site}/{container}/{topicId}",
         {
            site : this.options.siteId,
            container: this.options.containerId,
            topicId: this.options.topicId
         });
         
         // perform the ajax request to delete the topic
         Alfresco.util.Ajax.request(
         {
            url: url,
            method: "DELETE",
            responseContentType : "application/json",
            successMessage: this._msg("message.delete.success"),
            successCallback:
            {
               fn: onDeleted,
               scope: this
            },
            failureMessage: this._msg("message.delete.failure")
         });
      },
      
      /**
       * Tag selected handler
       *
       * @method onTagSelected
       * @param tagId {string} Tag name.
       * @param target {HTMLElement} Target element clicked.
       */
      onTagSelected: function DiscussionsTopic_onTagSelected(layer, args)
      {
         var obj = args[1];
         if (obj && (obj.tagName !== null))
         {
            // construct the topic list url with initial active tag filter
            var url = YAHOO.lang.substitute(Alfresco.constants.URL_CONTEXT + "page/site/{site}/discussions-topiclist" +
                    "?container={container}&filterId={filterId}&filterOwner={filterOwner}&filterData={filterData}",
            {
               site: this.options.siteId,
               container: this.options.containerId,
               filterId: "tag",
               filterOwner: "Alfresco.TopicListTags",
               filterData: obj.tagName
            });

            window.location = url;
         }
      },

      /**
       * Loads the edit form.
       */
      _loadEditForm: function DiscussionsTopic__loadEditForm()
      {  
         // Load the UI template from the server
         Alfresco.util.Ajax.request(
         {
            url: Alfresco.constants.URL_SERVICECONTEXT + "modules/discussions/topic/edit-topic",
            dataObj:
            {
               htmlid: this.id + "-form"
            },
            successCallback:
            {
               fn: this.onEditFormLoaded,
               scope: this
            },
            failureMessage: this._msg("message.loadeditform.failure")
         });
      },

      /**
       * Event callback when dialog template has been loaded
       *
       * @method onFormLoaded
       * @param response {object} Server response from load template XHR request
       */
      onEditFormLoaded: function DiscussionsTopic_onEditFormLoaded(response)
      {
         // id to use for the form
         var formId = this.id + "-form";
          
         // use the already loaded data
         var data = this.topicData;
          
         // find the right divs to insert the html into
         var viewDiv = Dom.get(this.id + "-topic-view-div");
         var editDiv = Dom.get(this.id + "-topic-edit-div");
         
         // insert the html
         editDiv.innerHTML = response.serverResponse.responseText;
         
         // insert current values into the form
         var actionUrl = YAHOO.lang.substitute(Alfresco.constants.PROXY_URI + "api/forum/post/site/{site}/{container}/{topicId}",
         {
            site: this.options.siteId,
            container : this.options.containerId,
            topicId: this.options.topicId
         });
         Dom.get(formId + "-form").setAttribute("action", actionUrl);
         Dom.get(formId + "-site").setAttribute("value", this.options.siteId);
         Dom.get(formId + "-container").setAttribute("value", this.options.containerId);
         Dom.get(formId + "-title").setAttribute("value", data.title);
         // construct the browseUrl. {post.name} gets replaced on the server
         var browseUrl = YAHOO.lang.substitute(Alfresco.constants.URL_PAGECONTEXT + "site/{site}/discussions-topicview?container={container}&topicId={post.name}",
         {
            site: this.options.siteId,
            container: this.options.containerId
         });
         Dom.get(formId + "-browseTopicUrl").setAttribute("value", browseUrl);
         Dom.get(formId + "-content").value = data.content;
         
         // show the form and hide the view
         Dom.addClass(viewDiv, "hidden");
         Dom.removeClass(editDiv, "hidden");
             
         // and finally register the form handling
         this._registerEditTopicForm(data, formId);
      },

      /**
       * Registers the form logic
       */
      _registerEditTopicForm: function DiscussionsTopic__registerEditTopicForm(data, formId)
      {
         // register the tag listener
         this.modules.tagLibraryListener = new Alfresco.TagLibraryListener(formId + "-form", "tags");
         
         // add the tags that are already set on the post
         this.modules.tagLibrary = new Alfresco.TagLibrary(formId);
         this.modules.tagLibrary.setOptions({ siteId: this.options.siteId });
         this.modules.tagLibrary.onReady();
         if (data.tags.length > 0)
         {
            // find the tag library component
            //var taglibrary = Alfresco.util.ComponentManager.findFirst("Alfresco.TagLibrary");
            this.modules.tagLibrary.addTags(this.topicData.tags);
         }
         
         // register the okButton
         this.widgets.okButton = new YAHOO.widget.Button(formId + "-submit", {type: "submit"});
         
         // register the cancel button
         this.widgets.cancelButton = new YAHOO.widget.Button(formId + "-cancel", {type: "button"});
         this.widgets.cancelButton.subscribe("click", this.onEditFormCancelButtonClick, this, true);
         
         // instantiate the simple editor we use for the form
         this.widgets.editor = new YAHOO.widget.SimpleEditor(formId + '-content', {
             height: '180px',
             width: '700px',
             dompath: false, //Turns on the bar at the bottom
             animate: false, //Animates the opening, closing and moving of Editor windows
             toolbar:  Alfresco.util.editor.getTextOnlyToolbarConfig(this._msg)
         });
         // render the editor - we use the private function as we want this to happen
         // prior of displaying the form. Otherwise we get quite ugly ui behavior
         this.widgets.editor._render();
         
         // create the form that does the validation/submit
         var commentForm = new Alfresco.forms.Form(formId + "-form");
         commentForm.setShowSubmitStateDynamically(true, false);
         commentForm.setSubmitElements(this.widgets.okButton);
         commentForm.setAjaxSubmitMethod(Alfresco.util.Ajax.PUT);
         commentForm.setAJAXSubmit(true,
         {
            successMessage: this._msg("message.savetopic.success"),
            successCallback:
            {
               fn: this.onEditFormSubmitSuccess,
               scope: this
            },
            failureMessage: this._msg("message.savetopic.failure")
         });
         commentForm.setSubmitAsJSON(true);
         commentForm.doBeforeFormSubmit =
         {
            fn: function(form, obj)
            {
               //Put the HTML back into the text area
               this.widgets.editor.saveHTML();
               // update the tags set in the form
               this.modules.tagLibraryListener.updateForm();
            },
            scope: this
         }
         
         commentForm.init();
      },
      
      /**
       * Edit form submit success handler
       */
      onEditFormSubmitSuccess: function DiscussionsTopic_onCreateFormSubmitSuccess(response, object)
      {
         // the response contains the new data for the comment. Render the comment html
         // and insert it into the view element
         this.topicData = response.json.item;
         this.renderUI();
            
         // hide the form and show the ui
         this._hideEditView();
            
         // inform the replies object about the update
         this._fireTopicDataChangedEvent();
      },
      
      /**
       * Edit form cancel button click handler
       */
      onEditFormCancelButtonClick: function(type, args)
      {
          this._hideEditView();
      },
      
      /**
       * Hides the form and displays the view
       */
      _hideEditView: function()
      {
         var editDiv = Dom.get(this.id + "-topic-edit-div");
         var viewDiv = Dom.get(this.id + "-topic-view-div");
         Dom.addClass(editDiv, "hidden");
         Dom.removeClass(viewDiv, "hidden");
      },

      /**
       * Called when the mouse enters into the topic div
       */
      onTopicElementMouseEntered: function DiscussionsTopicList_onTopicElementMouseEntered(layer, args)
      {
         var elem = args[1].target;
         YAHOO.util.Dom.addClass(elem, 'overNode');
         var editBloc = YAHOO.util.Dom.getElementsByClassName( 'nodeEdit' , null , elem, null );
         YAHOO.util.Dom.addClass(editBloc, 'showEditBloc');
      },
     
      /**
       * Called whenever the mouse exits the topic div
       */
      onTopicElementMouseExited: function DiscussionsTopicList_onTopicElementMouseExited(layer, args)
      {
         var elem = args[1].target;
         YAHOO.util.Dom.removeClass(elem, 'overNode');
         var editBloc = YAHOO.util.Dom.getElementsByClassName( 'nodeEdit' , null , elem , null );
         YAHOO.util.Dom.removeClass(editBloc, 'showEditBloc');
      },

      /**
       * Fires a topic data changed bubble event
       */
      _fireTopicDataChangedEvent: function DiscussionsTopicList__fireTopicDataChangedEvent()
      {
         var eventData = {
            topicRef: this.topicData.nodeRef,
            topicTitle: this.topicData.title,
            topicId: this.topicData.name
         }
         YAHOO.Bubbling.fire("topicDataChanged", eventData);
      },

      /**
       * Gets a custom message
       *
       * @method _msg
       * @param messageId {string} The messageId to retrieve
       * @return {string} The custom message
       * @private
       */
      _msg: function DiscussionsTopic_msg(messageId)
      {
         return Alfresco.util.message.call(this, messageId, "Alfresco.DiscussionsTopic", Array.prototype.slice.call(arguments).slice(1));
      }
   };
})();
