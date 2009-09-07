Alfresco.RM = Alfresco.RM || {};

/**
 * RM EmailMappings component
 * 
 * @namespace Alfresco
 * @class Alfresco.RM.References
 */
(function RM_EmailMappings()
{
   /**
    * YUI Library aliases
    */
   var Dom = YAHOO.util.Dom,
       Event = YAHOO.util.Event,
       Sel = YAHOO.util.Selector;


   /**
    * RM EmailMappings componentconstructor.
    * 
    * @param {String} htmlId The HTML id of the parent element
    * @return {Alfresco.dashlet.MyDocuments} The new component instance
    * @constructor
    */
   Alfresco.RM.EmailMappings = function RM_EmailMappings_constructor(htmlId)
   {
      Alfresco.RM.EmailMappings.superclass.constructor.call(this, "Alfresco.RM.EmailMappings", htmlId, []);
      this.currentValues = {};
      this.dataMap = new Alfresco.RM.EmailMappings_Data();
      YAHOO.Bubbling.on("EmailMappingsLoaded", this.onDataLoad, this);
      YAHOO.Bubbling.on("EmailMappingsSaved", this.onDataSave, this);      
      YAHOO.Bubbling.on("EmailMappingsChanged", this.onMappingChanged, this);

      return this;
   };
    
    YAHOO.extend(Alfresco.RM.EmailMappings, Alfresco.component.Base,
   {
      
      /**
       * Initialises event listening and custom events
       *  
       */
      initEvents : function RM_EmailMappings_initEvents()
      {
         Event.on(this.id,'click',this.onInteractionEvent,null,this);
         //register event
         this.registerEventHandler('click',[
            {
               rule : 'button#save-mappings-button',
               o : {
                     handler:this.onSaveMappings,
                     scope : this
               }
            },
            {
               rule : 'button#discard-mappings-button',
               o : {
                     handler:this.onDiscardChanges,
                     scope : this
               }
            },
            {
               rule : 'button.delete-mapping',
               o : {
                     handler:this.onDeleteMapping,
                     scope : this
               }
            },
            {
               rule : 'button#add-mapping-button',
               o : {
                     handler:this.onAddMapping,
                     scope : this
               }
            }                                  
         ]);
         return this;
      },
      /**
       * Handler for save button
       *  
       */      
      onSaveMappings: function RM_EmailMappings_onSaveMappings()
      {
         var me = this;
         Alfresco.util.PopupManager.displayPrompt(
         {
            title: this.msg('label.save-changes-title'),
            text: this.msg('label.save-changes-confirmation'),
            buttons: [
            {
               text: this.msg('label.yes'), // To early to localize at this time, do it when called instead
               handler: function()
               {
                  me.dataMap.save();
                  this.destroy();
               },
               isDefault: false
            },
            {
               text: this.msg('label.no'), // To early to localize at this time, do it when called instead
               handler: function()
               {
                  this.destroy();
               },
               isDefault: false
            }
            ]
         });         
      },
      /**
       * Handler for discard changes button.
       *  
       */
      onDiscardChanges: function RM_EmailMappings_onDiscardChanges()
      {
         var me = this;
         Alfresco.util.PopupManager.displayPrompt(
         {
            title: this.msg('label.discard-changes-title'),
            text: this.msg('label.discard-changes-confirmation'),
            buttons: [
            {
               text: this.msg('label.yes'), // To early to localize at this time, do it when called instead
               handler: function()
               {
                  me._discardChanges();
                  this.destroy();
               },
               isDefault: false
            },
            {
               text: this.msg('label.no'), // To early to localize at this time, do it when called instead
               handler: function()
               {
                  this.destroy();
               },
               isDefault: false
            }
            ]
         });
      },

      /**
       * Handler for delete mapping button. Removes from datamap and DOM
       *  
       */      
      onDeleteMapping: function RM_EmailMappings_onDeleteMapping(e)
      {
         
         var li = Dom.getAncestorByTagName(Event.getTarget(e), 'li');
         var fromTo = li.id.split('::');
         this.dataMap.remove({
            from:fromTo[0],
            to:fromTo[1]
         });
         li.parentNode.removeChild(li);
      },
      
      /**
       * Handler for add mapping button
       * 
       *  
       */      
      onAddMapping: function RM_EmailMappings_onAddMapping()
      {


         if (this.currentValues['emailProperty'] && this.currentValues['rmProperty'])
         {
            var oMap = {
               from:this.currentValues['emailProperty'].value,
               to:this.currentValues['rmProperty'].value
            };
            
            if (this.dataMap.isValidAddition(oMap))
            {
               this.dataMap.add(oMap);
               this.renderMapping(oMap);
            }
         }
      },
      
      /**
       * Discard changes by reloading page
       *  
       */      
      _discardChanges: function RM_EmailMappings_discardChanges()
      {
         window.location.reload();     
      },
      
      /**
       * Fired by YUI when parent element is available for scripting
       * @method onReady
       * 
       */
      onReady: function RM_EmailMappings_onReady()
      {
         this.initEvents();
         var me = this;
         var elements = Sel.query('button',this.id).concat(Sel.query('input[type="submit"]',this.id));
         // Create widget button while reassigning classname to src element (since YUI removes classes). 
         // We need the classname so we can identify what action to take when it is interacted with (event delegation).
         for (var i=0, len = elements.length; i<len; i++)
         {
            var el= elements[i];
            if (el.id.indexOf('-button')==-1 && el.className.indexOf('button-menu') == -1)
            {
              var id = el.id.replace(this.id+'-','');
              this.widgets[id] = new YAHOO.widget.Button(el.id);
              this.widgets[id]._button.className=el.className;
            }
         }
         
         this.widgets['list'] = Sel.query('#emailMappings-list ul')[0];

         //handles menu selection; sets label and stores selected values
         var onHandleMenuSelection = function(menuItem, menuName) {
            var menuItemText = menuItem.cfg.getConfig().text;
            var menuItemValue = menuItem.value;

            this.widgets[menuName + '-menu'].set('label',menuItemText);
            this.currentValues[menuName] = {value:menuItemValue,label:menuItemText};
            if (menuName == 'emailProperty')
            {
               this.updateMenu(menuItemValue);
            }
            this.widgets['add-mapping'].set('disabled',menuItem.cfg.getProperty('disabled'));

         };

         
         //create menus
         this.widgets['emailProperty-menu'] = new YAHOO.widget.Button("emailMappings-emailProperty-button", {
               type: "split", 
               menu: 'emailMappings-emailProperty-menu',
               lazyloadmenu:false
            }
         );
         
         this.widgets['emailProperty-menu'].getMenu().subscribe('click', function handleMenuSelection(e, args) {
            // me.onHandleRMMenuSelection(e,args);
            var menuName = 'emailProperty';

            return function (e, args) {
               onHandleMenuSelection.call(this, args[1], menuName);
            };
         }(), this, true);
         
            
         this.widgets['rmProperty-menu'] = new YAHOO.widget.Button("emailMappings-rmProperty-button",{
               type: "split", 
               menu: 'emailMappings-rmProperty-menu',
               lazyloadmenu:false               
            }
         );

         this.widgets['rmProperty-menu'].getMenu().subscribe('click', function handleMenuSelection(e, args) {
            var menuName = 'rmProperty';

            return function (e, args) {
               onHandleMenuSelection.call(this, args[1], menuName);
            };
         }(), this, true);

         //load data
         this.dataMap.load();
         //set up initial state
         onHandleMenuSelection.call(this,this.widgets['emailProperty-menu'].getMenu().getItem(0),'emailProperty');
         onHandleMenuSelection.call(this,this.widgets['rmProperty-menu'].getMenu().getItem(0),'rmProperty');    
         
      },
      
      /**
       * Deselects unavailable (already mapped) options depending on menu selection
       * 
       * @method updateMenu
       * 
       * @param {String} menuItemValue Value of selected menu item 
       */
      updateMenu : function RM_EmailMappings_updateMenu(menuItemValue)
      {
         var mappedValues = this.dataMap.getSelectionByKey(menuItemValue);

         var rmMenu = this.widgets['rmProperty-menu'].getMenu();
         var rmItems = rmMenu.getItems();
         for (var i=0,len = rmItems.length; i < len; i++)
         {
               rmItems[i].cfg.setProperty('disabled', false);     
         }
         if (mappedValues !== '')
         {
            for (var i=0,len = rmItems.length; i < len; i++)
            {
               var menuItem = rmItems[i];
               menuItem.cfg.setProperty('disabled', false);               
               if (mappedValues.indexOf(menuItem.value) != -1)
               {
                  menuItem.cfg.setProperty('disabled', true);
               }
            }
         }
      },
      
      /**
       * Handler for when data mapping loads.
       *  
       */
      onDataLoad : function RM_EmailMappings_onDataLoad(e, args)
      {
         args[1] = args[1].mappings;
         for (var i=0,len = args[1].mappings.length;i<len; i++)
         {
            this.renderMapping(args[1].mappings[i]);
         }
      },
      /**
       * Handler for when data mapping saves. Disables save and discard buttons
       *  
       */
      onDataSave : function RM_EmailMappings_onDataSave(e, args)
      {
         this.widgets['save-mappings'].set('disabled', true);
         this.widgets['discard-mappings'].set('disabled', true);                    
      },
      
      /**
       * Renders mapping to DOM.
       * 
       * @param {Object} oMap - Value object of 'from' and 'to' string variables
       *  
       */
      renderMapping : function RM_EmailMappings_renderMapping(oMap)
      {
         var newLi = document.createElement('li');
         newLi.id = oMap.from + '::' + oMap.to;
         newLi.innerHTML = YAHOO.lang.substitute("<p>{email} {to} {rm}</p>", {
            email : oMap.from,
            to : this.msg('label.to'),
            rm : oMap.to
         });

         this.widgets['list'].appendChild(newLi);
         var button = new YAHOO.widget.Button({
            id:oMap.from +'-'+ oMap.to + '-button',
            label:this.msg('label.delete'),
            container:newLi.id
         });
         button.on('click', this.onDeleteMapping, this ,true);
         button.appendTo(newLi);
      },
      
      /**
       * Handler when mapping is added or deleted in UI (but not yet persisted).
       * Enables/Disables saves and discard buttons if UI is changed from initial state
       *  
       */
      onMappingChanged : function(e, args)
      {
         var data = args[1];
         if ((data.markedForAddition==='') && (data.markedForDeletion===''))
         {
            this.widgets['save-mappings'].set('disabled', true);
            this.widgets['discard-mappings'].set('disabled', true);            
         }
         else
         {
            this.widgets['save-mappings'].set('disabled', false);
            this.widgets['discard-mappings'].set('disabled', false);                        
         }
      }
      
   });
})();


(function RM_EmailMappings_Data() {
   /**
    * A Value object for the mapping data for email mappings.
    * 
    * Since additions and deletions are not carried out individually (any mappings to be added
    * or removal are sent together in one request), we need to manage the data construct.
    * 
    * @constructor
    * @param {Object} { mappings: ['from':'fromAttribute', 'to': 'toAttribute']} 
    */
   Alfresco.RM.EmailMappings_Data = function RM_EmailMappings_Data_constructor()
   {
      //data is { mappings:[{from:'fromvalue',to:'tovalue'}]}
      //internal representation - data as loaded from server 
      this.data = null;
      this.markedForDeletion = '';
      this.markedForAddition = '';
      this.index = {};
      
      this.dataUri = Alfresco.constants.PROXY_URI + "api/rma/admin/emailmap";
      
      return this;
   };
   
   Alfresco.RM.EmailMappings_Data.prototype = {
      
      /**
       * Adds a mapping
       * 
       * @method add
       * 
       * @param {Object} Mapping object to add
       * @return {Boolean} True or False depending on success of addition 
       */
      add: function RM_EmailMappings_Data_add(obj)
      {
         //if valid, mark for addition and reindex internal representation
         if (this.isValidAddition(obj))
         {
            this.markedForAddition += obj.from + '::' + obj.to + ',';
            this.data.mappings.push(obj);
            //reindex this.data
            this._index();
            YAHOO.Bubbling.fire('EmailMappingsChanged', {markedForAddition:this.markedForAddition,markedForDeletion:this.markedForDeletion});
            return true;
         }
         return false;
      },
      
      remove: function RM_EmailMappings_Data_remove(obj)
      {
         //if valid, mark for deletion and reindex internal representation
         // if (this.isValidDeletion(obj))
         // {
            //removing a new mapping but one that hasn't been saved
            if (this.markedForAddition.indexOf(obj.from + '::' + obj.to) != -1)
            {
               this.markedForAddition = this.markedForAddition.replace(obj.from + '::' + obj.to + ',', '');
            }
            else
            {
               this.markedForDeletion+=obj.from + '::' + obj.to + ',';
            }

            var a = [];
            for (var i=0,len=this.data.mappings.length;i<len;i++)
            {
               var o = this.data.mappings[i];
               if ((o.from != obj.from) || (o.to != obj.to))
               {
                  a.push(o);
               }
            }
            this.data.mappings = a;
            //reindex this.data 
            this._index();
            YAHOO.Bubbling.fire('EmailMappingsChanged', {markedForAddition:this.markedForAddition,markedForDeletion:this.markedForDeletion});            
         // }
      },
      
      /**
       * Checks if obj is valid for addition or not
       * 
       * @method isValidAddition 
       * 
       * @param {Object} Must have 'from' and 'to' properties, both of which should be strings. Invalid if already mapped
       * return {Boolean} True or false 
       */
      isValidAddition: function RM_EmailMappings_Data_isValidAddition(obj)
      {
         if (obj.hasOwnProperty('from') && obj.hasOwnProperty('to'))
         {
            //check current 'from' mappings and if not in string, is valid.
            if (this.index[obj.from])
            {
               if (this.index[obj.from].indexOf(obj.to)!=-1)
               {
                  return false;
               }
               else
               {
                  return true;                  
               }
            } 
            else
            {
               //haven't got a mapping for this 'from' so is valid
               return true;               
            }
         }
         //invalid object, no 'from' or 'to' attributes
         return false;
      },
      
      /**
       * Checks if obj is valid for deletion or not
       * 
       * @method isValidDeletion
       * 
       * @param {Object} Must have 'from' and 'to' properties, both of which should be strings. Invalid if not already mapped
       * return {Boolean} True or false 
       */
      isValidDeletion: function RM_EmailMappings_Data_isValidDeletion(obj)
      {
         if (obj.hasOwnProperty('from') && obj.hasOwnProperty('to'))
         {
            //check current 'from' mappings and if not in string, is valid.
            if (this.index[obj.from])
            {
               if (this.index[obj.from].indexOf(obj.to)!=-1)
               {
                  
                  return true;
               }
               else
               {
                  return false;                  
               }
            } 
            else
            {
               //haven't got a mapping for this 'from' so isn't valid
               return false;               
            }
         }
         //invalid object, no 'from' or 'to' attributes
         return false;
      },

      /**
       * Loads mapping data via AJAX
       *  
       */
      load: function RM_EmailMappings_Data_load()
      {
         var me = this;
         Alfresco.util.Ajax.jsonRequest(
         {
            method: Alfresco.util.Ajax.GET,
            url: this.dataUri,
            successCallback:
            {
               fn: function(args)
               {
                  me.data = args.json.data;
                  this._index();
                  YAHOO.Bubbling.fire('EmailMappingsLoaded',{mappings:me.data});                  
               },
               scope: this
            },
            failureMessage: Alfresco.util.message("message.loadFailure", this.name)
         });
      },
      /**
       * Saves current data mapping via AJAX
       *  
       */
      save: function RM_EmailMappings_Data_save()
      {
         var dataObj = {};
         if (this.markedForAddition!=="")
         {
            dataObj.add = [];
            var additions = this.markedForAddition.split(',');
            if (additions[additions.length-1]==="")
            {
               additions.pop();
            }
            for (var i=0,len = additions.length;i<len;i++)
            {
               var map = additions[i].split('::');
               dataObj.add.push({from:map[0],to:map[1]});
            }
         }
         if (this.markedForDeletion!=="")
         {
            dataObj["delete"] = [];
            var deletions = this.markedForDeletion.split(',');
            if (deletions[deletions.length-1]==="")
            {
               deletions.pop();
            }
            for (var i=0,len = deletions.length;i<len;i++)
            {
               var map = deletions[i].split('::');
               dataObj["delete"].push({from:map[0],to:map[1]});
            }
         }         
         var me = this;
         Alfresco.util.Ajax.jsonRequest(
         {
           method: Alfresco.util.Ajax.POST,
           url: this.dataUri,
           dataObj: dataObj,
           successCallback:
           {
              fn: function datamap_save()
              {
                 me.markedForDeletion = "";
                 me.markedForAddition = "";
                 Alfresco.util.PopupManager.displayMessage({
                        text: Alfresco.util.message('message.saveSuccess', "Alfresco.RM.EmailMappings"),
                        spanClass: 'message',
                        modal: true,
                        noEscape: true,
                        displayTime: 1
                     });
                 YAHOO.Bubbling.fire("EmailMappingsSaved", {mappings:me.data});
              },
              scope: this
           },
           failureMessage: Alfresco.util.message("message.saveFailure", this.name)
         });
        
      },

      /**
       * Indexes mapping so validation can be carried out more smoothly
       * 
       * @method index 
       * @return {} A object like
       * @example 
       *   {
       *       from:{
       *          'fromAttribute': 'CSV string of "to" attributes'
       *       },
       *       to: { // required?
       *          'toAttribute': 'CSV string of "from" attributes'
       *       }
       *   } 
       */

      _index: function RM_EmailMappings_Data_index()
      {
         var indexedData = {};
         for (var i=0, len = this.data.mappings.length;i<len;i++)
         {
            var o = this.data.mappings[i];
            if (indexedData[o.from]===undefined)
            {
               indexedData[o.from] = '';
               
            }
            if (indexedData[o.from]!==undefined && indexedData[o.from].indexOf(o.to) == -1)
            {
               indexedData[o.from] += o.to + ',';
            }
         }
         this.index = indexedData;
      },
      
      /**
       * Retrieves values of index data based on specified key
       *  
       * @param {String} key - Value of the key used in the index - always the same as the 'from' value
       * in a mapping
       * 
       */
      getSelectionByKey: function RM_EmailMappings_Data_getSelectedValues(key)
      {
         if (this.index[key])
         {
            return this.index[key];
         }
         else return '';
      }

   };
}
)();
