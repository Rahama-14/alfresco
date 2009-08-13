/**
 * Copyright (C) 2005-2009 Alfresco Software Limited.
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
 * TagLibrary
 * 
 * Module that manages the selection of tags in a form
 *
 * @namespace Alfresco
 * @class Alfresco.module.TagLibrary
 */
(function()
{
   /**
    * YUI Library aliases
    */
   var Dom = YAHOO.util.Dom,
      Event = YAHOO.util.Event;

   Alfresco.module.TagLibrary = function(htmlId)
   {
      Alfresco.module.TagLibrary.superclass.constructor.call(this, "Alfresco.module.TagLibrary", htmlId + "-tagLibrary", ["button"]);
      
      /**
       * TODO: Remove this hack.
       * The TagLibrary was trying to register itself with the htmlId. This isn't a good idea, as there will likely
       * already be a component registered with that id.
       * Unfortunately the TagLibrary code (and therefore it's historical users) rely on this id
       */
      this.id = htmlId;

      this.tagId =
      {
         id: 0,
         tags: {}
      };
      this.currentTags = [];
      
      return this;
   };

   YAHOO.extend(Alfresco.module.TagLibrary, Alfresco.component.Base,
   {
       /**
        * Object container for initialization options
        */
       options:
       {
          /**
           * Current siteId.
           * 
           * @property siteId
           * @type string
           * @default ""
           */
          siteId: "",
          
          /**
           * Maximum number of tags popular tags displayed
           * @property topN
           * @type integer
           * @default 10
           */
          topN: 10
       },

      /**
       * Object literal used to generate unique tag ids
       * 
       * @property tagId
       * @type object
       */
      tagId: null,
      
      /**
       * Currently selected tags.
       * 
       * @type: array of strings
       * @default empty array
       */
      currentTags: null,

      /**
       * Sets the current list of tags.
       * Use this method if the tags html and inputs have been generated on the server.
       * If you create the taglibrary in javascript, use setTags to also update the UI.
       *
       * @method setCurrentTags
       * @param tags {Array} String array of tags
       */
      setCurrentTags: function TagLibrary_setCurrentTags(tags)
      {
         this.currentTags = tags;
         return this;
      },
      
      /**
       * Registers the tag library logic with the dom tree
       *
       * @method initialize
       * @param formsRuntime {object} Instance of Alfresco.forms.Form
       */
      initialize: function TagLibrary_initialize(formsRuntime)
      {
         var me = this;

         var fnActionHandlerDiv = function TagLibrary_fnActionHandlerDiv(layer, args)
         {
            var owner = YAHOO.Bubbling.getOwnerByTagName(args[1].anchor, "li");
            if (owner !== null)
            {
               var action = "";
               action = owner.getAttribute("class");
               if (typeof me[action] == "function")
               {
                  var tagName = me.findTagName(me, owner.id);
                  me[action].call(me, tagName);
                  args[1].stop = true;
               }
            }
      		 
            return true;
         };
         YAHOO.Bubbling.addDefaultAction("taglibrary-action", fnActionHandlerDiv);

         // load link for popular tags
         Event.addListener(this.id + "-load-popular-tags-link", "click", this.onPopularTagsLinkClicked, this, true);
         
         // register the "enter" event on the tag text field to add the tag (otherwise the form gets submitted)
         var enterKeyListener = new YAHOO.util.KeyListener(this.id + "-tag-input-field", 
         {
            keys: YAHOO.util.KeyListener.KEY.ENTER
         }, 
         {
            fn: function TagLibrary_enterKeyListener(eventName, event, obj)
            {
               me.onAddTagButtonClick();
               Event.stopEvent(event[1]);
               return false;
            },
            scope: this,
            correctScope: true
         }, "keypress");
         enterKeyListener.enable();
         
         // button to add tag to list
         var addTagButton = Alfresco.util.createYUIButton(this, "add-tag-button", this.onAddTagButtonClick,
         {
            type: "button",
            disabled: (typeof formsRuntime != "undefined"),
            htmlName: "-"
         });

         // Add validators
         if (formsRuntime)
         {
            var tagFormsRuntime = new Alfresco.forms.Form(formsRuntime.formId);
            tagFormsRuntime.setShowSubmitStateDynamically(true, false);
            tagFormsRuntime.setSubmitElements(addTagButton);
            tagFormsRuntime.setAJAXSubmit(true);
            tagFormsRuntime.doBeforeAjaxRequest =
            {
               fn: function TagLibrary_fnValidation(form, obj)
               {
                  return false;
               },
               obj: null,
               scope: this
            };
            tagFormsRuntime.addValidation(this.id + "-tag-input-field", Alfresco.forms.validation.nodeName);
            tagFormsRuntime.addValidation(this.id + "-tag-input-field", Alfresco.forms.validation.mandatory);
            tagFormsRuntime.addValidation(this.id + "-tag-input-field", Alfresco.forms.validation.length,
            {
               max: 256,
               crop: true
            }, "keyup");
         }
      },
      
      /**
       * Generate ID alias for tag, suitable for DOM ID attribute
       *
       * @method generateTagId
       * @param scope {object} instance that contains a tagId object (which stores the generated tag id mappings)
       * @param tagName {string} Tag name
       * @return {string} A unique DOM-safe ID for the tag
       */
      generateTagId: function TagLibrary_generateTagId(scope, tagName, action)
      {
         var id = 0,
            tagId = scope.tagId;
         
         if (tagName in tagId.tags)
         {
            id = tagId.tags[tagName];
         }
         else
         {
           tagId.id++;
           id = tagId.tags[tagName] = tagId.id;
         }
         return scope.id + "-" + action + "-" + id;
      },
      
      /**
       * Returns the tagName given a id generated by generateTagId.
       *
       * @method findTagName
       * @param scope {object} instance that contains a tagId object (which stores the generated tag id mappings)
       * @param tagId {string} Tag ID
       */
      findTagName: function TagLibrary_findTagName(scope, tagId)
      {
         var actionAndId = tagId.substring(scope.id.length + 1),
            tagIdValue = actionAndId.substring(actionAndId.indexOf('-') + 1);
         
         for (var tag in scope.tagId.tags)
         {
            if (scope.tagId.tags[tag] == tagIdValue)
            {
               return tag;
            }
         }
         return null;
      },
      
      /**
       * Adds an array of tags to the current tags.
       * For each tag the html is generated, this function can therefore
       * be used to set the tags when using the taglibrary as a client-side
       * only component (no tags generated on the server)
       *
       * @method setTags
       * @param tags {array} Array containing the tags (by name)
       */
      setTags: function TagLibrary_setTags(tags)
      {
         // first make sure that there are no previous tags available
         Dom.get(this.id + '-current-tags').innerHTML = '';
         this.currentTags = [];
         
         // add each tag to the list, also generating the html
         for (var x = 0, xx = tags.length; x < xx; x++)
         {
            this._addTagImpl(tags[x]);
         }
      },

      /**
       * Get all tags currently selected
       *
       * @method getTags
       */
      getTags: function TagLibrary_getTags()
      {
         return this.currentTags;
      },
      
      /**
       * Updates a form with the currently selected tags.
       *
       * @method updateForm
       * @param formId {string} the id of the form to update
       * @param tagsFieldName {string} the name of the field to use to store the tags in
       */
      updateForm: function TagLibrary_updateForm(formId, tagsFieldName)
      {
         // construct the complete name to use for the field
         var fullFieldName = tagsFieldName + '[]';
         
         // clean out the currently available tag inputs
         var formElem = Dom.get(formId);
         
         // find all input fields, delete the inputs that match the field name
         var inputs = formElem.getElementsByTagName("input"),
            x, xx;
         
         // IMPORTANT: Do NOT optimize loop - loop bounds are modified inside
         for (x = 0; x < inputs.length; x++)
         {
            if (inputs[x].name == fullFieldName)
            {
                // remove the field
                inputs[x].parentNode.removeChild(inputs[x]);
                x--;
            }
         }
         
         var tagName, elem;
         if (this.currentTags.length > 0)
         {
            // generate inputs for the selected tags
            for (x = 0, xx = this.currentTags.length; x < xx; x++)
            {
               tagName = this.currentTags[x];
               elem = document.createElement('input');
               elem.setAttribute('name', fullFieldName);
               elem.setAttribute('value', tagName);
               elem.setAttribute('type', 'hidden');
               formElem.appendChild(elem);
            }
         }
         else
         {
            elem = document.createElement('input');
            elem.setAttribute('name', tagsFieldName);
            elem.setAttribute('value', '');
            elem.setAttribute('type', 'hidden');
            formElem.appendChild(elem);
         }
      },

      /**
       * Triggered by a click on one of the selected tags
       *
       * @method onRemoveTag
       * @param tagName {string} Tag clicked
       */
      onRemoveTag: function TagLibrary_onRemoveTag(tagName)
      {
          this._removeTagImpl(tagName);
      },
      
      /**
       * Triggered by a click onto one of the popular tags.
       *
       * @method onAddTag
       * @param tagName {string} Tag clicked
       */
      onAddTag: function TagLibrary_onAddTag(tagName)
      {
         this._addTagImpl(tagName);
      },

      /**
       * Loads the popular tags
       *
       * @method onPopularTagsLinkClicked
       */
      onPopularTagsLinkClicked: function TagLibrary_onPopularTagsLinkClicked(e, obj)
      {
         // load the popular tags through an ajax call
         var url = YAHOO.lang.substitute(Alfresco.constants.PROXY_URI + "api/tagscopes/site/{site}/tags?d={d}&tn={tn}",
         {
            site: this.options.siteId,
            d: new Date().getTime(),
            tn: this.options.topN
         });
         Alfresco.util.Ajax.request(
         {
            url: url,
            method: "GET",
            responseContentType : "application/json",
            successCallback:
            {
               fn: this._onPopularTagsLoaded,
               scope: this
            },
            failureMessage: this.msg("taglibrary.msg.failedLoadTags")
         });
         Event.stopEvent(e);
      },
      
      /**
       * Popular tags loaded handler
       *
       * @method _onPopularTagsLoaded
       * @param response {object} Server response
       * @private
       */
      _onPopularTagsLoaded: function TagLibrary__onPopularTagsLoaded(response)
      {
         this._displayPopularTags(response.json.tags);
      },
      
      /**
       * Update the UI with the popular tags loaded via AJAX.
       *
       * @method _displayPopularTags
       * @param tags {array} Array of tags
       * @private
       */
      _displayPopularTags: function TagLibrary__showPopularTags(tags)
      {
         // remove the popular tags load link
         Dom.setStyle(this.id + "-load-popular-tags-link", "display", "none");

         // add all tags to the ui
         var popularTagsElem = Dom.get(this.id + "-popular-tags"),
            elem, elemId;
         
         for (var x = 0, xx = tags.length; x < xx; x++)
         {
            elem = document.createElement('li');
            elemId = this.generateTagId(this, tags[x].name, 'onAddTag');
            elem.setAttribute('id', elemId);
            elem.setAttribute('class', 'onAddTag');
            elem.innerHTML = '<a href="#" class="taglibrary-action"><span>' + tags[x].name + '</span><span class="add">&nbsp;</span></a>';
            popularTagsElem.appendChild(elem);
         }
      },

      /**
       * Adds the content of the text field as a new tag.
       *
       * @method onAddTagButtonClick
       */
      onAddTagButtonClick: function TagLibrary_onAddTagButtonClick(type, args)
      {
         // get the text of the input field
         var inputField = Dom.get(this.id + "-tag-input-field"),
            text = inputField.value,
            tags = Alfresco.util.getTags(text);
         
         for (var x = 0, xx = tags.length; x < xx; x++)
         {
            this._addTagImpl(tags[x]);
         }
         
         // finally clear the text field
         inputField.value = "";
      },
       
      /**
       * Fires a tags changed event.
       *
       * @method _fireTagsChangedEvent
       */
      _fireTagsChangedEvent: function TagLibrary__fireTagsChangedEvent()
      {
         // send out a message informing about the new set of tags
         YAHOO.Bubbling.fire('onTagLibraryTagsChanged',
         {
            tags : this.currentTags
         });
      },

      /**
       * Add a tag to the current set of selected tags
       *
       * @method _addTagImpl
       * @param tagName {string} Name of tag
       * @private
       */
      _addTagImpl: function TagLibrary__addTagImpl(tagName)
      {
         // sanity checks
         if (tagName === null || tagName.length < 1)
         {
             return;
         }
         
         // check whether the tag has already been added
         for (var x = 0, xx = this.currentTags.length; x < xx; x++)
         {
            if (tagName == this.currentTags[x])
            {
               return;
            }
         }
         
         // add the tag to the internal data structure
         this.currentTags.push(tagName);
         
         // add the tag to the UI
         var currentTagsElem = Dom.get(this.id + "-current-tags"),
            elem = document.createElement('li'),
            elemId = this.generateTagId(this, tagName, 'onRemoveTag');
         
         elem.setAttribute('id', elemId);
         elem.setAttribute('class', 'onRemoveTag');
         elem.innerHTML = '<a href="#" class="taglibrary-action"><span>' + tagName + '</span><span class="remove">&nbsp;</span></a>';
         currentTagsElem.appendChild(elem);

         // inform interested parties about change
         this._fireTagsChangedEvent();
      },

      /**
       * Remove a tag from the current set of selected tags
       *
       * @method _removeTagImpl
       * @param tagName {string} Name of tag
       * @private
       */
      _removeTagImpl: function TagLibrary__removeTagImpl(tagName)
      {
         // sanity checks
         if (tagName === null || tagName.length < 1)
         {
             return;
         }
         
         // IMPORTANT: Do NOT optimize loop - loop bounds are modified inside
         for (var x = 0; x < this.currentTags.length; x++)
         {
            if (tagName == this.currentTags[x])
            {
               this.currentTags.splice(x, 1);
               x--;
            }
         }

         // remove the ui element
         var elemId = this.generateTagId(this, tagName, 'onRemoveTag'),
            tagElemToRemove = Dom.get(elemId);
         
         tagElemToRemove.parentNode.removeChild(tagElemToRemove);
         
         // inform interested parties about change
         this._fireTagsChangedEvent();
      }
   });     
})();