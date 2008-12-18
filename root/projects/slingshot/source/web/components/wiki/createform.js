/*
 *** Alfresco.WikiCreateForm
*/
(function()
{
   /**
    * Alfresco Slingshot aliases
    */
   var $html = Alfresco.util.encodeHTML;
    
	Alfresco.WikiCreateForm = function(containerId)
   {
	   this.name = "Alfresco.WikiCreateForm";
      this.id = containerId;

      // Initialise prototype properties
      this.widgets = {};

      /* Load YUI Components */
      Alfresco.util.YUILoaderHelper.require(["button", "container", "connection", "editor"], this.onComponentsLoaded, this);

      return this;
   };
   
	Alfresco.WikiCreateForm.prototype =
	{
      /**
       * Object container for storing YUI widget instances.
       * 
       * @property widgets
       * @type object
       */
      widgets: null,

	   /**
   	  * Sets the current site for this component.
   	  * 
   	  * @property siteId
   	  * @type string
   	  */
   	setSiteId: function(siteId)
   	{
   		this.siteId = siteId;
   		return this;
   	},
   	
       /**
        * Set messages for this component.
        *
        * @method setMessages
        * @param obj {object} Object literal specifying a set of messages
        * @return {Alfresco.WikiCreateForm} returns 'this' for method chaining
        */
       setMessages: function(obj)
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
       onComponentsLoaded: function()
       {
          YAHOO.util.Event.onContentReady(this.id, this.init, this, true);
       },
		
		/**
   	 * Fired by YUI when parent element is available for scripting.
   	 * Initialises components, including YUI widgets.
   	 *
   	 * @method init
   	 */
   	init: function()
   	{
         this.tagLibrary = new Alfresco.module.TagLibrary(this.id);
		   this.tagLibrary.setOptions(
		   {
		      siteId: this.siteId
		   });
         this.tagLibrary.initialize();

         this.widgets.pageEditor = Alfresco.util.createImageEditor(this.id + '-pagecontent',
         {
            height: "300px",
            width: "600px",
            dompath: false, // Turns on the bar at the bottom
            animate: false, // Animates the opening, closing and moving of Editor windows
            markup: "xhtml",
            siteId: this.siteId
         });
         this.widgets.pageEditor.render();

         this.widgets.saveButton = new YAHOO.widget.Button(this.id + "-save-button",
         {
            type: "submit"
         });

      	Alfresco.util.createYUIButton(this, "cancel-button", null,
      	{
      		type: "link"
      	});
      	
         // Create the form that does the validation/submit
         var form = new Alfresco.forms.Form(this.id + "-form");
         form.addValidation(this.id + "-pageTitle", Alfresco.forms.validation.mandatory, null, "blur");
         form.addValidation(this.id + "-pageTitle", Alfresco.forms.validation.nodeName, null, "keyup");
  	      form.setShowSubmitStateDynamically(true);
         form.setSubmitElements(this.widgets.saveButton);
         form.setAJAXSubmit(true,
         {
            successCallback:
            {
               fn: this.onPageCreated,
               scope: this
            },
            failureMessage: "Page create failed"
         });

         form.setSubmitAsJSON(true);
         form.setAjaxSubmitMethod(Alfresco.util.Ajax.PUT);
         form.doBeforeFormSubmit =
         {
            fn: function(form, obj)
            {
               // Disable save button to prevent double-submission
               this.widgets.saveButton.set("disabled", true);
               // Put the HTML back into the text area
               this.widgets.pageEditor.saveHTML();
               // Update the tags set in the form
               this.tagLibrary.updateForm(this.id + "-form", "tags");

               // Avoid submitting the input field used for entering tags
               var tagInputElem = YAHOO.util.Dom.get(this.id + "-tag-input-field");
               if (tagInputElem)
               {
                  tagInputElem.disabled = true;
               }
               
               var title = YAHOO.util.Dom.get(this.id + "-pageTitle").value;
               title = title.replace(/\s+/g, "_");
               // Set the "action" attribute of the form based on the page title
               form.action =  Alfresco.constants.PROXY_URI + "slingshot/wiki/page/" + this.siteId + "/" + title;
               
               // Display pop-up to indicate that the page is being saved
               var savingMessage = Alfresco.util.PopupManager.displayMessage(
               {
                  displayTime: 0,
                  text: '<span class="wait">' + $html(Alfresco.util.message("message.saving", this.name)) + '</span>',
                  noEscape: true
               });
            },
            scope: this
         }

         form.init();      	
	      
	   },

      /**
   	 * Event handler that gets called when the page is successfully created.
   	 * Redirects the user to the newly created page.
   	 *
   	 * @method onPageCreated
   	 * @param e {object} DomEvent
   	 */	   
	   onPageCreated: function(e)
	   {
	      var name = "Main_Page"; // safe default
	      
	      var obj = YAHOO.lang.JSON.parse(e.serverResponse.responseText);
	      if (obj)
	      {
	         name = obj.name;
	      }
	   
         // Redirect to the page that has just been created
         window.location =  Alfresco.constants.URL_CONTEXT + "page/site/" + this.siteId + "/wiki-page?title=" + name;
	   }
   };
      
})();      
