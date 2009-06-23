/*
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
 * FlashUpload component.
 *
 * Popups a YUI panel and displays a filelist and buttons to browse for files
 * and upload them. Files can be removed and uploads can be cancelled.
 * For single file uploads version input can be submitted.
 *
 * A multi file upload scenario could look like:
 *
 * var flashUpload = Alfresco.component.getFlashUploadInstance();
 * var multiUploadConfig =
 * {
 *    siteId: siteId,
 *    containerId: doclibContainerId,
 *    path: docLibUploadPath,
 *    filter: [],
 *    mode: flashUpload.MODE_MULTI_UPLOAD,
 * }
 * this.flashUpload.show(multiUploadConfig);
 *
 * @namespace Alfresco.module
 * @class Alfresco.component.FlashUpload
 */
(function()
{
   /**
    * YUI Library aliases
    */
   var Dom = YAHOO.util.Dom,
      Element = YAHOO.util.Element,
      KeyListener = YAHOO.util.KeyListener;

   /**
    * FlashUpload constructor.
    *
    * FlashUpload is considered a singleton so constructor should be treated as private,
    * please use Alfresco.component.getFlashUploadInstance() instead.
    *
    * @param {string} htmlId The HTML id of the parent element
    * @return {Alfresco.component.FlashUpload} The new FlashUpload instance
    * @constructor
    * @private
    */
   Alfresco.FlashUpload = function(containerId)
   {
      this.name = "Alfresco.FlashUpload";
      this.id = containerId;
      this.swf = Alfresco.constants.URL_CONTEXT + "yui/uploader/assets/uploader.swf?dt=" + (new Date()).getTime();
      this.hasRequiredFlashPlayer = Alfresco.util.hasRequiredFlashPlayer(9, 0, 45);
      
      /* Register this component */
      Alfresco.util.ComponentManager.register(this);

      if (this.hasRequiredFlashPlayer)
      {
         // Load YUI Components if flash player is installed
         Alfresco.util.YUILoaderHelper.require(["button", "container", "datatable", "datasource", "cookie", "uploader"], this.onComponentsLoaded, this);
      }

      return this;
   };

   Alfresco.FlashUpload.prototype =
   {
      /**
       * The flash move will dispatch the contentReady event twice,
       * make sure we only react on it twice.
       *
       * @property contentReady
       * @type boolean
       */
      contentReady: false,
      
      /**
       * The user is browsing and adding files to the file list
       *
       * @property STATE_BROWSING
       * @type int
       */
      STATE_BROWSING: 1,

      /**
       * File(s) is being uploaded to the server
       *
       * @property STATE_UPLOADING
       * @type int
       */
      STATE_UPLOADING: 2,

      /**
       * All files are processed and have either failed or been successfully
       * uploaded to the server.
       *
       * @property STATE_FINISHED
       * @type int
       */
      STATE_FINISHED: 3,
      
      /**
       * File failed to upload.
       *
       * @property STATE_FAILURE
       * @type int
       */
      STATE_FAILURE: 4,

      /**
       * File was successfully STATE_SUCCESS.
       *
       * @property STATE_SUCCESS
       * @type int
       */
      STATE_SUCCESS: 5,

       /**
       * The state of which the uploader currently is, where the flow is.
       * STATE_BROWSING > STATE_UPLOADING > STATE_FINISHED
       *
       * @property state
       * @type int
       */
      state: 1,

      /**
       * Stores references and state for each file that is in the file list.
       * The fileId parameter from the YAHOO.widget.Uploader is used as the key
       * and the value is an object that stores the state and references.
       *
       * @property fileStore
       * @type object Used as a hash table with fileId as key and an object
       *       literal as the value.
       *       The object literal is of the form:
       *       {
       *          contentType: {HTMLElement},        // select that holds the chosen contentType for the file.
       *          fileButton: {YAHOO.widget.Button}, // Will be disabled on success or STATE_FAILURE
       *          state: {int},                      // Keeps track if the individual file has been successfully uploaded or failed
       *                                             // (state flow: STATE_BROWSING > STATE_UPLOADING > STATE_SUCCESS or STATE_FAILURE)
       *          progress: {HTMLElement},           // span that is the "progress bar" which is moved during progress
       *          progressInfo: {HTMLElement},       // span that displays the filename and the state
       *          progressPercentage: {HTMLElement}, // span that displays the upload percentage for the individual file
       *          fileName: {string},                // filename
       *          nodeRef: {string}                  // nodeRef if the file has been uploaded successfully
       *       }
       */
      fileStore: {},

      /**
       * The number of successful uploads since upload was clicked.
       *
       * @property noOfSuccessfulUploads
       * @type int
       */
      noOfSuccessfulUploads: 0,

      /**
       * The number of failed uploads since upload was clicked.
       *
       * @property noOfFailedUploads
       * @type int
       */
      noOfFailedUploads: 0,

      /**
       * Remembers what files that how been added to the file list since
       * the show method was called.
       *
       * @property addedFiles
       * @type object
       */
      addedFiles: {},

      /**
       * Shows uploader in single upload mode.
       *
       * @property MODE_SINGLE_UPLOAD
       * @static
       * @type int
       */
      MODE_SINGLE_UPLOAD: 1,

      /**
       * Shows uploader in single update mode.
       *
       * @property MODE_SINGLE_UPDATE
       * @static
       * @type int
       */
      MODE_SINGLE_UPDATE: 2,

      /**
       * Shows uploader in multi upload mode.
       *
       * @property MODE_MULTI_UPLOAD
       * @static
       * @type int
       */
      MODE_MULTI_UPLOAD: 3,
      
      /**
       * The default config for the gui state for the uploader.
       * The user can override these properties in the show() method to use the
       * uploader for both single & multi uploads and single updates.
       *
       * @property defaultShowConfig
       * @type object
       */
      defaultShowConfig:
      {
         siteId: null,
         containerId: null,
         uploadDirectory: null,
         updateNodeRef: null,
         updateFilename: null,
         mode: this.MODE_SINGLE_UPLOAD,
         filter: [],
         onFileUploadComplete: null,
         overwrite: false,
         thumbnails: null,
         uploadURL: null,
         username: null
      },

      /**
       * The merged result of the defaultShowConfig and the config passed in
       * to the show method.
       *
       * @property defaultShowConfig
       * @type object
       */
      showConfig: {},

      /**
       * Contains the upload gui
       *
       * @property panel
       * @type YAHOO.widget.Panel
       */
      panel: null,

      /**
       * Object container for storing YUI widget instances.
       * 
       * @property widgets
       * @type object
       */
      widgets: {},

      /**
       * YUI class that controls the .swf to open the browser dialog window
       * and transfers the files.
       *
       * @property uploader
       * @type YAHOO.widget.Uploader
       */
      uploader: null,

      /**
       * A property that is set to true after the loaded swf movie has dispatched its swfReady/contentReady event
       *
       * @property uploader Ready
       * @type boolean
       */
      uploaderReady: false,

      /**
       * Used to display the user selceted files and keep track of what files
       * that are selected and should be STATE_FINISHED.
       *
       * @property uploader
       * @type YAHOO.widget.DataTable
       */
      dataTable: null,

      /**
       * HTMLElement of type span that displays the dialog title.
       *
       * @property titleText
       * @type HTMLElement
       */
      titleText: null,

      /**
       * HTMLElement of type span that displays help text for multi uploads.
       *
       * @property multiUploadTip
       * @type HTMLElement
       */
      multiUploadTip: null,

      /**
       * HTMLElement of type span that displays help text for single updates.
       *
       * @property singleUpdateTip
       * @type HTMLElement
       */
      singleUpdateTip: null,

      /**
       * HTMLElement of type span that displays the total upload status
       *
       * @property statusText
       * @type HTMLElement
       */
      statusText: null,

      /**
       * HTMLElement of type radio button for major or minor version 
       *
       * @property description
       * @type HTMLElement
       */
      minorVersion: null,

      /**
       * HTMLElement of type textarea for version comment
       *
       * @property description
       * @type HTMLElement
       */
      description: null,

      /**
       * HTMLElement of type div that displays the version input form.
       *
       * @property versionSection
       * @type HTMLElement
       */
      versionSection: null,

      /**
       * HTMLElements of type div that is used to to display a column in a
       * row in the file table list. It is loaded dynamically from the server
       * and then cloned for each row and column in the file list.
       * The fileItemTemplates has the following form:
       * {
       *    left:   HTMLElement to display the left column
       *    center: HTMLElement to display the center column
       *    right:  HTMLElement to display the right column
       * }
       *
       * @property fileItemTemplates
       * @type HTMLElement
       */
      fileItemTemplates: {},

      /**
       * Set messages for this module.
       *
       * @method setMessages
       * @param obj {object} Object literal specifying a set of messages
       * @return {Alfresco.component.FlashUpload} returns 'this' for method chaining
       */
      setMessages: function FU_setMessages(obj)
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
      onComponentsLoaded: function FU_onComponentsLoaded()
      {
         // Tell the YUI class where the swf is
         YAHOO.widget.Uploader.SWFURL = this.swf;

         Dom.removeClass(this.id + "-dialog", "hidden");

         // Create the panel
         this.panel = new YAHOO.widget.Panel(this.id + "-dialog",
         {
            modal: true,
            draggable: false,
            fixedcenter: true,
            visible: false,
            close: false
         });
         this.panel.render(document.body);

         // Save a reference to the file row template that is hidden inside the markup
         this.fileItemTemplates.left = Dom.get(this.id + "-left-div");
         this.fileItemTemplates.center = Dom.get(this.id + "-center-div");
         this.fileItemTemplates.right = Dom.get(this.id + "-right-div");

         // Create the YIU datatable object
         this._createEmptyDataTable();

         // Save a reference to the HTMLElement displaying texts so we can alter the texts later
         this.titleText = Dom.get(this.id + "-title-span");
         this.multiUploadTip = Dom.get(this.id + "-multiUploadTip-span");
         this.singleUpdateTip = Dom.get(this.id + "-singleUpdateTip-span");
         this.statusText = Dom.get(this.id + "-status-span");
         this.description = Dom.get(this.id + "-description-textarea");

         // Save reference to version radio so we can reset and get its value later
         this.minorVersion = Dom.get(this.id + "-minorVersion-radioButton");

         // Save a reference to browseButton so wa can change it later
         //this.widgets.browseButton = Alfresco.util.createYUIButton(this, "browse-button", this.onBrowseButtonClick);

         // Save a reference to the HTMLElement displaying version input so we can hide or show it
         this.versionSection = Dom.get(this.id + "-versionSection-div");

         // Create and save a reference to the uploadButton so we can alter it later
         this.widgets.uploadButton = Alfresco.util.createYUIButton(this, "upload-button", this.onUploadButtonClick);

         // Create and save a reference to the cancelOkButton so we can alter it later
         this.widgets.cancelOkButton = Alfresco.util.createYUIButton(this, "cancelOk-button", this.onCancelOkButtonClick);

         // Create and save a reference to the uploader so we can call it later
         this.uploader = new YAHOO.widget.Uploader(this.id + "-flashuploader-div", Alfresco.constants.URL_CONTEXT + "themes/" + Alfresco.constants.THEME + "/images/upload-button-sprite.png");
         this.uploader.subscribe("fileSelect", this.onFileSelect, this, true);
         this.uploader.subscribe("uploadComplete",this.onUploadComplete, this, true);
         this.uploader.subscribe("uploadProgress",this.onUploadProgress, this, true);
         this.uploader.subscribe("uploadStart",this.onUploadStart, this, true);
         this.uploader.subscribe("uploadCancel",this.onUploadCancel, this, true);
         this.uploader.subscribe("uploadCompleteData",this.onUploadCompleteData, this, true);
         this.uploader.subscribe("uploadError",this.onUploadError, this, true);
         this.uploader.subscribe("contentReady", this.onContentReady, this, true);

         // Register the ESC key to close the dialog
         this.widgets.escapeListener = new KeyListener(document,
         {
            keys: KeyListener.KEY.ESCAPE
         },
         {
            fn: this.onCancelOkButtonClick,
            scope: this,
            correctScope: true
         });
      },


      /**
       * Called when the "wrapping" SWFPlayer-flash movie is loaded
       *
       * @method onContentReady
       */
      onContentReady: function FP_onContentReady(event)
      {
         this.uploader.enable();
         this.uploader.setAllowMultipleFiles(this.showConfig.mode === this.MODE_MULTI_UPLOAD);
         this.uploader.setFileFilters(this.showConfig.filter);
      },

      /**
       * Show can be called multiple times and will display the uploader dialog
       * in different ways depending on the config parameter.      
       *
       * @method show
       * @param config {object} describes how the upload dialog should be displayed
       * The config object is in the form of:
       * {
       *    siteId: {string},        // site to upload file(s) to
       *    containerId: {string},   // container to upload file(s) to (i.e. a doclib id)
       *    uploadPath: {string},    // directory path inside the component to where the uploaded file(s) should be save
       *    updateNodeRef: {string}, // nodeRef to the document that should be updated
       *    updateFilename: {string},// The name of the file that should be updated, used to display the tip
       *    mode: {int},             // MODE_SINGLE_UPLOAD, MODE_MULTI_UPLOAD or MODE_SINGLE_UPDATE
       *    filter: {array},         // limits what kind of files the user can select in the OS file selector
       *    onFileUploadComplete: null, // Callback after upload
       *    overwrite: false         // If true and in mode MODE_XXX_UPLOAD it tells
       *                             // the backend to overwrite a versionable file with the existing name
       *                             // If false and in mode MODE_XXX_UPLOAD it tells
       *                             // the backend to append a number to the versionable filename to avoid
       *                             // an overwrite and a new version
       * }
       */
      show: function FU_show(config)
      {
         if (!this.hasRequiredFlashPlayer)
         {
            Alfresco.util.PopupManager.displayPrompt(
            {
               text: Alfresco.util.message("label.noFlash", this.name)
            });
         }

         // Merge the supplied config with default config and check mandatory properties
         this.showConfig = YAHOO.lang.merge(this.defaultShowConfig, config);
         if (this.showConfig.uploadDirectory === undefined && this.showConfig.updateNodeRef === undefined)
         {
             throw new Error("An updateNodeRef OR uploadDirectory must be provided");
         }
         if (this.showConfig.uploadDirectory !== null && this.showConfig.uploadDirectory.length === 0)
         {
            this.showConfig.uploadDirectory = "/";
         } 

         // Apply the config before it is shown
         this._resetGUI();

         // Apply the config before it is shown
         this._applyConfig();

         // Enable the Esc key listener
         this.widgets.escapeListener.enable();

         // Show the upload panel
         this.panel.show();

         // Need to resize FF in Ubuntu so the button appears
         var swfWrapper = this.id + "-flashuploader-div";
         if(navigator.userAgent && navigator.userAgent.indexOf("Ubuntu") != -1 &&
            YAHOO.env.ua.gecko > 1 && !Dom.hasClass(swfWrapper, "button-fix"))
         {
            Dom.addClass(swfWrapper, "button-fix");
         }

      },

      _resetGUI: function FU__resetGUI()
      {
         // Reset references and the gui before showing it
         this.state = this.STATE_BROWSING;
         this.noOfFailedUploads = 0;
         this.noOfSuccessfulUploads = 0;
         this.statusText["innerHTML"] = "&nbsp;";
         this.description.value = "";
         this.minorVersion.checked = true;
         this.widgets.uploadButton.set("label", Alfresco.util.message("button.upload", this.name));
         this.widgets.uploadButton.set("disabled", true);
         this.widgets.cancelOkButton.set("label", Alfresco.util.message("button.cancel", this.name));
         this.widgets.cancelOkButton.set("disabled", false);
      },

      /**
       * Fired by YUI:s DataTable when the added row has been rendered to the data table list.
       *
       * @method onPostRenderEvent       
       */
      onPostRenderEvent: function FU_onPostRenderEvent()
      {
         // Display the upload button since all files are rendered
         if (this.dataTable.getRecordSet().getLength() > 0)
         {
            this.widgets.uploadButton.set("disabled", false);
         }
         if(this.showConfig.mode === this.MODE_SINGLE_UPDATE)
         {
            if(this.dataTable.getRecordSet().getLength() == 0)
            {
               this.uploader.enable();
            }
            else
            {
               this.uploader.disable();
            }
         }
      },


      /**
       * Fired by YUI:s DataTable when a row has been added to the data table list.
       * Keeps track of added files.
       *
       * @method onRowDeleteEvent
       * @param event {object} a DataTable "rowDelete" event
       */
      onRowDeleteEvent: function FU_onRowDeleteEvent(event)
      {
      },

      /**
       * Fired by YIUs Uploader when the user has selected one or more files
       * from the OS:s file dialog window.
       * Adds file that hasn't been selected before to the gui and adjusts the gui.
       *
       * @method onFileSelect
       * @param event {object} an Uploader "fileSelect" event
       */
      onFileSelect: function FU_onFileSelect(event)
      {
         // Disable upload button until all files have been rendered and added
         this.widgets.uploadButton.set("disabled", true);

         // For each time the user select new files, all the previous selected
         // files also are included in the event.fileList. Make sure we only
         // add files to the table that haven's been added before.
         var newFiles = [];
         for (var i in event.fileList)
         {
            if(this.dataTable.get("renderLoopSize") == 0)
            {
               this.dataTable.set("renderLoopSize", 1);
            }
            var data = YAHOO.widget.DataTable._cloneObject(event.fileList[i]);
            if (!this.addedFiles[this._getUniqueFileToken(data)])
            {
               if (data.size == 0)           
               {
                  Alfresco.util.PopupManager.displayMessage(
                  {
                     text: Alfresco.util.message("message.zeroByteFileSelected", this.name, data.name)
                  });
               }
               else
               {
                  // Since the flash movie allows the user to select one file several
                  // times we need to keep track of the selected files by our selves
                  var uniqueFileToken = this._getUniqueFileToken(data);
                  this.addedFiles[uniqueFileToken] = uniqueFileToken;
                  newFiles.push(data);                  
               }
            }
         }
         // Add all files to table
         this.dataTable.addRows(newFiles, 0);
      },

      /**
       * Fired by YIU:s Uploader when transfer has been start for one of the files.
       * Adjusts the gui.
       *
       * @method onUploadStart
       * @param event {object} an Uploader "uploadStart" event
       */
      onUploadStart: function FU_onUploadStart(event)
      {
         // Get the reference to the files gui components
         var fileInfo = this.fileStore[event["id"]];

         // Hide the contentType drop down if it wasn't hidden already
         Dom.addClass(fileInfo.contentType, "hidden");

         // Show the progress percentage if it wasn't visible already
         fileInfo.progressPercentage["innerHTML"] = "0%";
         Dom.removeClass(fileInfo.progressPercentage, "hidden");

         // Make sure we know we are in upload state
         fileInfo.state = this.STATE_UPLOADING;
      },

      /**
       * Fired by YIU:s Uploader during the transfer for one of the files.
       * Adjusts the gui and its progress bars.
       *
       * @method onUploadComplete
       * @param event {object} an Uploader "uploadProgress" event
       */
      onUploadProgress: function FU_onUploadProgress(event)
      {
         var flashId = event["id"];
         var fileInfo = this.fileStore[flashId];

         // Set percentage
         var percentage = event["bytesLoaded"] / event["bytesTotal"];
         fileInfo.progressPercentage["innerHTML"] = Math.round(percentage * 100) + "%";

         // Set progress position
         var left = (-400 + (percentage * 400));
         Dom.setStyle(fileInfo.progress, "left", left + "px");
      },

      /**
       * Fired by YIU:s Uploader when transfer is complete for one of the files.
       *
       * @method onUploadComplete
       * @param event {object} an Uploader "uploadComplete" event
       */
      onUploadComplete: function FU_onUploadComplete(event)
      {
         /**
          * Actions taken on a completed upload is handled by the
          * onUploadCompleteData() method instead.
          */
      },

      /**
       * Fired by YIU:s Uploader when transfer is completed for a file.
       * A difference compared to the onUploadComplete() method is that
       * the response body is available in the event.
       * Adjusts the gui and calls for another file to upload if the upload
       * was succesful.
       *
       * @method onUploadCompleteData
       * @param event {object} an Uploader "uploadCompleteData" event
       */
      onUploadCompleteData: function FU_onUploadCompleteData(event)
      {
         // The individual file has been transfered completely
         // Now adjust the gui for the individual file row
         var fileInfo = this.fileStore[event["id"]];
         fileInfo.state = this.STATE_SUCCESS;
         fileInfo.fileButton.set("disabled", true);

         // Extract the nodeRef and (possibly changed) fileName from the JSON response
         var oldFileName = fileInfo.fileName;
         var json = Alfresco.util.parseJSON(event.data);
         if (json)
         {
            fileInfo.nodeRef = json.nodeRef;
            fileInfo.fileName = json.fileName;
         }

         // Add the label "Successful" after the filename, updating the fileName from the response
         fileInfo.progressInfo["innerHTML"] = fileInfo.progressInfo["innerHTML"].replace(oldFileName, fileInfo.fileName) + " " + Alfresco.util.message("label.success", this.name);

         // Change the style of the progress bar
         fileInfo.progress.setAttribute("class", "fileupload-progressFinished-span");

         // Move the progress bar to "full" progress
         Dom.setStyle(fileInfo.progress, "left", 0 + "px");
         fileInfo.progressPercentage["innerHTML"] = "100%";
         this.noOfSuccessfulUploads++;

         // Adjust the rest of the gui
         this._updateStatus();
         this._uploadFromQueue(1);
         this._adjustGuiIfFinished();
      },

      /**
       * Fired by YIU:s Uploader when transfer has been cancelled for one of the files.
       * Doesn't do anything.
       *
       * @method onUploadCancel
       * @param event {object} an Uploader "uploadCancel" event
       */
      onUploadCancel: function FU_onUploadCancel(event)
      {
         // The gui has already been adjusted in the function that caused the cancel
      },

      /**
       * Fired by YIU:s Uploader when transfer failed for one of the files.
       * Adjusts the gui and calls for another file to upload.
       *
       * @method onUploadError
       * @param event {object} an Uploader "uploadError" event
       */
      onUploadError: function FU_onUploadError(event)
      {
         var fileInfo = this.fileStore[event["id"]];

         // This sometimes gets called twice, make sure we only adjust the gui once
         if (fileInfo.state !== this.STATE_FAILURE)
         {
            fileInfo.state = this.STATE_FAILURE;

            // Add the label "Failure" to the filename
            fileInfo.progressInfo["innerHTML"] = fileInfo.progressInfo["innerHTML"] +
                                                 " " + Alfresco.util.message("label.failure", this.name);

            // Change the style of the progress bar
            fileInfo.progress.setAttribute("class", "fileupload-progressFailure-span");

            // Set the progress bar to "full" progress
            Dom.setStyle(fileInfo.progress, "left", 0 + "px");

            // Disable the remove button
            fileInfo.fileButton.set("disabled", true);

            // Adjust the rest of the gui
            this.noOfFailedUploads++;
            this._updateStatus();
            this._uploadFromQueue(1);
            this._adjustGuiIfFinished();
         }
      },

      /**
       * Called by an anonymous function which that redirects the call to here
       * when the user clicks the file remove button.
       * Removes the file and cancels it if it was being uploaded
       *
       * @method _onFileButtonClickHandler
       * @param flashId {string} an id matching the flash movies fileId
       * @param recordId {int} an id matching a record in the data tables data source
       */
      _onFileButtonClickHandler: function FU__onFileButtonClickHandler(flashId, recordId)
      {
         /**
          * The file button has been clicked to remove a file.
          * Remove the file from the datatable and all references to it.
          */
         var r = this.dataTable.getRecordSet().getRecord(recordId);
         this.addedFiles[this._getUniqueFileToken(r.getData())] = null;
         this.fileStore[flashId] = null;
         this.dataTable.deleteRow(r);
         if (this.state === this.STATE_BROWSING)
         {
            // Remove the file from the flash movies memory
            this.uploader.removeFile(flashId);
            if (this.dataTable.getRecordSet().getLength() === 0)
            {
               // If it was the last file, disable the gui since no files exist.
               this.widgets.uploadButton.set("disabled", true);
               this.uploader.enable();
            }
         }
         else if (this.state === this.STATE_UPLOADING)
         {
            // Cancel the ongoing upload for the file in the flash movie
            this.uploader.cancel(flashId);

            // Continue to upload documents from the queue
            this._uploadFromQueue(1);

            // Update the rest of the gui
            this._updateStatus();
            this._adjustGuiIfFinished();
         }
      },

      /**
       * Fired when the user clicks the cancel/ok button.
       * The action taken depends on what state the uploader is in.
       * In STATE_BROWSING  - Closes the panel.
       * In STATE_UPLOADING - Cancels current uploads,
       *                      informs the user about how many that were uploaded,
       *                      tells the documentlist to update itself
       *                      and closes the panel.
       * In STATE_FINISHED  - Tells the documentlist to update itself
       *                      and closes the panel.
       *
       * @method onBrowseButtonClick
       * @param event {object} a Button "click" event
       */
      onCancelOkButtonClick: function FU_onCancelOkButtonClick()
      {
         var message;
         if (this.state === this.STATE_BROWSING)
         {     
            // Do nothing (but close the panel, which happens below)
         }
         else if (this.state === this.STATE_UPLOADING)
         {
            this._cancelAllUploads();

            // Inform the user if any files were uploaded before the rest was cancelled
            var noOfUploadedFiles = 0;
            for (var i in this.fileStore)
            {
               if (this.fileStore[i] && this.fileStore[i].state === this.STATE_SUCCESS)
               {
                  noOfUploadedFiles++;
               }
            }
            if (noOfUploadedFiles > 0)
            {
               message = Alfresco.util.message("message.cancelStatus", this.name);
               message = YAHOO.lang.substitute(message,
               {
                  "0": noOfUploadedFiles
               });
            }

            // Tell the document list to refresh itself if present
            YAHOO.Bubbling.fire("metadataRefresh",
            {
               currentPath: this.showConfig.path
            });
         }
         else if (this.state === this.STATE_FINISHED)
         {
            // Tell the document list to refresh itself if present and to
            // highlight the uploaded file (if multi upload was used display the first file)
            var fileName = null;
            for (var i in this.fileStore)
            {
               var f = this.fileStore[i];
               if (f && f.state === this.STATE_SUCCESS)
               {
                  fileName = f.fileName;
                  break;
               }
            }
            if (fileName)
            {
               YAHOO.Bubbling.fire("metadataRefresh",
               {
                  currentPath: this.showConfig.path,
                  highlightFile: fileName
               });
            }
            else
            {
               YAHOO.Bubbling.fire("metadataRefresh",
               {
                  currentPath: this.showConfig.path
               });
            }
         }

         // Hide the panel
         this.panel.hide();
                  
         // Disable the Esc key listener
         this.widgets.escapeListener.disable();

         // Remove all files and references for this upload "session"
         this._clear();

         // Inform the user if any files were uploaded before the rest was cancelled
         if (message)
         {
            Alfresco.util.PopupManager.displayPrompt(
            {
               text: message
            });
         }
      },

      /**
       * Fired when the user clicks the upload button.
       * Starts the uploading and adjusts the gui.
       *
       * @method onBrowseButtonClick
       * @param event {object} a Button "click" event
       */
      onUploadButtonClick: function FU_onUploadButtonClick()
      {
         if (this.state === this.STATE_BROWSING)
         {
            // Change the stat to uploading state and adjust the gui
            var length = this.dataTable.getRecordSet().getLength();
            if (length > 0)
            {
               this.state = this.STATE_UPLOADING;
               this.widgets.uploadButton.set("disabled", true);
               this.uploader.disable();
               this._updateStatus();
            }
            // And start uploading from the queue
            this._uploadFromQueue(2);
         }
      },

      /**
       * Adjust the gui according to the config passed into the show method.
       *
       * @method _applyConfig
       * @private
       */
      _applyConfig: function FU__applyConfig()
      {
         // Set the panel title
         var title;
         if (this.showConfig.mode === this.MODE_SINGLE_UPLOAD)
         {
            title = Alfresco.util.message("header.singleUpload", this.name);
         }
         else if (this.showConfig.mode === this.MODE_MULTI_UPLOAD)
         {
            title = Alfresco.util.message("header.multiUpload", this.name);
         }
         else if (this.showConfig.mode === this.MODE_SINGLE_UPDATE)
         {
            title = Alfresco.util.message("header.singleUpdate", this.name);
         }
         this.titleText["innerHTML"] = title;

         if (this.showConfig.mode === this.MODE_SINGLE_UPDATE)
         {

            var tip = Alfresco.util.message("label.singleUpdateTip", this.name);
            tip = YAHOO.lang.substitute(tip,
            {
               "0": this.showConfig.updateFilename
            });
            this.singleUpdateTip["innerHTML"] = tip;

            // Display the version input form
            Dom.removeClass(this.versionSection, "hidden");
         }
         else
         {
            // Hide the version input form
            Dom.addClass(this.versionSection, "hidden");
         }

         if (this.showConfig.mode === this.MODE_MULTI_UPLOAD)
         {
            // Show the upload status label, only interesting for multiple files
            Dom.removeClass(this.statusText, "hidden");

            // Show the help label for how to select multiple files
            Dom.removeClass(this.multiUploadTip, "hidden");

            // Hide the help label for other modes
            Dom.addClass(this.singleUpdateTip, "hidden");

            // Make the file list long
            this.dataTable.set("height", "204px", true);
         }
         else
         {
            // Hide the upload status label, only interesting for multiple files
            Dom.addClass(this.statusText, "hidden");

            // Hide the help label for how to select multiple files
            Dom.addClass(this.multiUploadTip, "hidden");

            // Show the help label for single updates
            if (this.showConfig.mode === this.MODE_SINGLE_UPDATE)
            {
               // Show the help label for single updates
               Dom.removeClass(this.singleUpdateTip, "hidden");
            }
            else
            {
               // Hide the help label for single updates
               Dom.addClass(this.singleUpdateTip, "hidden");
            }

            // Make the file list short
            this.dataTable.set("height", "40px");
         }

         // Check if flash player existed or if the no flash message is displayed
         var uploaderDiv = Dom.get(this.id + "-flashuploader-div");
         var p = Dom.getFirstChild(uploaderDiv);
         if (p && p.tagName.toLowerCase() == "p")
         {
            // Flash isn't installed, make sure the no flash error message is displayed
            Dom.setStyle(uploaderDiv, "height", "30px");
            Dom.setStyle(uploaderDiv, "height", "200px");
         }
         else
         {
            this._applyUploaderConfig(
            {
               multiSelect: this.showConfig.mode === this.MODE_MULTI_UPLOAD,
               filter: this.showConfig.filter
            }, 0);

         }
      },

      _applyUploaderConfig: function (obj, attempt)
      {
         try
         {
            this.uploader.enable();
            this.uploader.setAllowMultipleFiles(obj.multiSelect);
            this.uploader.setFileFilters(obj.filter);
         }
         catch(e)
         {
            if(attempt == 7)
            {
               Alfresco.util.PopupManager.displayMessage(
               {
                  text: Alfresco.util.message("message.flashConfigError", this.name)
               });
            }
            else
            {
               YAHOO.lang.later(100, this, this._applyUploaderConfig, [obj, ++attempt]);
            }
         }
      },

      /**
       * Helper function to create the data table and its cell formatter.
       *
       * @method _createEmptyDataTable
       * @private
       */
      _createEmptyDataTable: function FU__createEmptyDataTable()
      {
         /**
          * Save a reference of 'this' so that the formatter below can use it
          * later (since the formatter method gets called with another scope
          * than 'this').
          */
         var myThis = this;

         /**
          * Responsible for rendering the left row in the data table
          *
          * @param el HTMLElement the td element
          * @param oRecord Holds the file data object
          */
         var formatLeftCell = function(el, oRecord, oColumn, oData)
         {
            myThis._formatCellElements(el, oRecord, myThis.fileItemTemplates.left);
         };

         /**
          * Responsible for rendering the center row in the data table
          *
          * @param el HTMLElement the td element
          * @param oRecord Holds the file data object
          */
         var formatCenterCell = function(el, oRecord, oColumn, oData)
         {
            myThis._formatCellElements(el, oRecord, myThis.fileItemTemplates.center);
         };

         /**
          * Responsible for rendering the right row in the data table
          *
          * @param el HTMLElement the td element
          * @param oRecord Holds the file data object
          */
         var formatRightCell = function(el, oRecord, oColumn, oData)
         {
            myThis._formatCellElements(el, oRecord, myThis.fileItemTemplates.right);
         };

         /**
          * Takes a left, center or right column template and looks for expected
          * html components and vcreates yui objects or saves references to
          * them so they can be updated during the upload progress.
          *
          * @param el HTMLElement the td element
          * @param oRecord Holds the file data object
          * @param template the template to display in the column
          */
         this._formatCellElements = function(el, oRecord, template)
         {

            // Set the state for this file(/row) if it hasn't been set
            var flashId = oRecord.getData()["id"];
            if (!this.fileStore[flashId])
            {
               this.fileStore[flashId] =
               {
                  state: this.STATE_BROWSING,
                  fileName: oRecord.getData("name"),
                  nodeRef: null
               };
            }

            // create an instance from the template and give it a uniqueue id.
            var cell = new Element(el);
            var templateInstance = template.cloneNode(true);
            templateInstance.setAttribute("id", templateInstance.getAttribute("id") + flashId);

            // Save references to elements that will be updated during upload.
            var progress = Dom.getElementsByClassName("fileupload-progressSuccess-span", "span", templateInstance);
            if (progress.length == 1)
            {
               this.fileStore[flashId].progress = progress[0];
            }
            var progressInfo = Dom.getElementsByClassName("fileupload-progressInfo-span", "span", templateInstance);
            if (progressInfo.length == 1)
            {
               // Display the file size in human readable format after the filename.
               var readableSize = new Number(oRecord.getData()["size"]);
               readableSize = Alfresco.util.formatFileSize(readableSize);
               var fileInfoStr = oRecord.getData()["name"] + " (" + readableSize + ")";

               // Display the file name and size.
               progressInfo = progressInfo[0];
               this.fileStore[flashId].progressInfo = progressInfo;
               this.fileStore[flashId].progressInfo["innerHTML"] = fileInfoStr;
            }


            // * Save a reference to the contentType dropdown so we can find each
            // * files contentType before upload.            
            var contentType = Dom.getElementsByClassName("fileupload-contentType-select", "select", templateInstance);
            if (contentType.length == 1)
            {
               this.fileStore[flashId].contentType = contentType[0];
            }

            // Save references to elements that will be updated during upload.
            var progressPercentage = Dom.getElementsByClassName("fileupload-percentage-span", "span", templateInstance);
            if (progressPercentage.length == 1)
            {
               this.fileStore[flashId].progressPercentage = progressPercentage[0];
            }

            // Create a yui button for the fileButton.
            var fButton = Dom.getElementsByClassName("fileupload-file-button", "button", templateInstance);
            if (fButton.length == 1)
            {
               var fileButton = new YAHOO.widget.Button(fButton[0],
               {
                  type: "button"
               });
               fileButton.subscribe("click", function()
               {
                  this._onFileButtonClickHandler(flashId, oRecord.getId());
               }, this, true);
               this.fileStore[flashId].fileButton = fileButton;
            }

            // Insert the templateInstance to the column.
            cell.appendChild (templateInstance);
         };


         // Definition of the data table column
         var myColumnDefs = [
            {key: "id",      className:"col-left", resizable: false, formatter: formatLeftCell},
            {key: "name",    className:"col-center", resizable: false, formatter: formatCenterCell},
            {key: "created", className:"col-right", resizable: false, formatter: formatRightCell}
         ];

         // The data tables underlying data source.
         var myDataSource = new YAHOO.util.DataSource([]);
         myDataSource.responseType = YAHOO.util.DataSource.TYPE_JSARRAY;
         myDataSource.responseSchema =
         {
            fields: ["id","name","created","modified","type", "size", "progress"]
         };

         /**
          * Create the data table.
          * Set the properties even if they will get changed in applyConfig
          * afterwards, if not set here they will not be changed later.
          */
         YAHOO.widget.DataTable._bStylesheetFallback = !!YAHOO.env.ua.ie;
         var dataTableDiv = Dom.get(this.id + "-filelist-table");
         this.dataTable = new YAHOO.widget.DataTable(dataTableDiv, myColumnDefs, myDataSource,
         {
            scrollable: true,            
            height: "100px", // must be set to something so it can be changed afterwards, when the showconfig options decides if its a sinlge or multi upload
            width: "620px",
            renderLoopSize: 0, // value > 0 results in an error in IE & Safari from YIU2.6.0
            MSG_EMPTY: Alfresco.util.message("label.noFiles", this.name)
         });
         this.dataTable.subscribe("postRenderEvent", this.onPostRenderEvent, this, true);
         this.dataTable.subscribe("rowDeleteEvent", this.onRowDeleteEvent, this, true);
      },

      /**
       * Helper function to create a unique file token from the file data object
       *
       * @method _getUniqueFileToken
       * @param data {object} a file data object describing a file
       * @private
       */
      _getUniqueFileToken: function FU__getUniqueFileToken(data)
      {
         return data.name + ":" + data.size + ":" + data.cDate + ":" + data.mDate
      },

      /**
       * Update the status label with the latest information about the upload progress
       *
       * @method _updateStatus
       * @private
       */
      _updateStatus: function FU__updateStatus()
      {
         // Update the status label with the latest information about the upload progress
         var status = Alfresco.util.message("label.uploadStatus", this.name);
         status = YAHOO.lang.substitute(status,
         {
            "0" : this.noOfSuccessfulUploads,
            "1" : this.dataTable.getRecordSet().getLength(),
            "2" : this.noOfFailedUploads
         });
         this.statusText["innerHTML"] = status; 
      },

      /**
       * Checks if all files are finished (successfully uploaded or failed)
       * and if so adjusts the gui.
       *
       * @method _adjustGuiIfFinished
       * @private
       */
      _adjustGuiIfFinished: function FU__adjustGuiIfFinished()
      {
         var objComplete =
         {
            successful: [],
            failed: []
         };
         var file = null;
         
         // Go into finished state if all files are finished: successful or failures
         for (var i in this.fileStore)
         {
            file = this.fileStore[i];
            if (file)
            {
               if (file.state == this.STATE_SUCCESS)
               {
                  // Push successful file
                  objComplete.successful.push(
                  {
                     fileName: file.fileName,
                     nodeRef: file.nodeRef
                  });
               }
               else if (file.state == this.STATE_FAILURE)
               {
                  // Push failed file
                  objComplete.failed.push(
                  {
                     fileName: file.fileName
                  });
               }
               else
               {
                  return;
               }
            }
         }
         this.state = this.STATE_FINISHED;
         this.widgets.cancelOkButton.set("label", Alfresco.util.message("button.ok", this.name));
         this.widgets.uploadButton.set("disabled", true);
         
         var callback = this.showConfig.onFileUploadComplete;
         if (callback && typeof callback.fn == "function")
         {
            // Call the onFileUploadComplete callback in the correct scope
            callback.fn.call((typeof callback.scope == "object" ? callback.scope : this), objComplete, callback.obj);
         }
      },

      /**
       * Starts to upload as many files as specified by noOfUploadsToStart
       * as long as there are files left to upload.
       *
       * @method _uploadFromQueue
       * @param noOfUploadsToStart
       * @private
       */
      _uploadFromQueue: function FU__uploadFromQueue(noOfUploadsToStart)
      {
         // generate upload POST url
         var url;
         if (this.showConfig.uploadURL == null)
         {
            url = Alfresco.constants.PROXY_URI + "api/upload";
         }
         else
         {
            url = Alfresco.constants.PROXY_URI + this.showConfig.uploadURL;
         }
         
         // Flash does not correctly bind to the session cookies during POST
         // so we manually patch the jsessionid directly onto the URL instead
         url += ";jsessionid=" + YAHOO.util.Cookie.get("JSESSIONID");
         
         // Find files to upload
         var startedUploads = 0;
         var length = this.dataTable.getRecordSet().getLength();
         for (var i = 0; i < length && startedUploads < noOfUploadsToStart; i++)
         {
            var record = this.dataTable.getRecordSet().getRecord(i);
            var flashId = record.getData("id");
            var fileInfo = this.fileStore[flashId];
            if (fileInfo.state === this.STATE_BROWSING)
            {
               // Upload has NOT been started for this file, start it now
               fileInfo.state = this.STATE_UPLOADING;
               
               var attributes =
               {
                  siteId: this.showConfig.siteId,
                  containerId: this.showConfig.containerId,
                  username: this.showConfig.username
               };
               if (this.showConfig.mode === this.MODE_SINGLE_UPDATE)
               {         
                  attributes.updateNodeRef = this.showConfig.updateNodeRef;
                  attributes.majorVersion = !this.minorVersion.checked;
                  attributes.description = this.description.value;
               }
               else
               {
                  attributes.uploadDirectory = this.showConfig.uploadDirectory;
                  var contentType = fileInfo.contentType.options[fileInfo.contentType.selectedIndex].value;
                  attributes.contentType = contentType;
                  attributes.overwrite = this.showConfig.overwrite;
                  if (this.showConfig.thumbnails)
                  {
                     attributes.thumbnails = this.showConfig.thumbnails;
                  }
               }
               this.uploader.upload(flashId, url, "POST", attributes, "filedata");
               startedUploads++;
            }
         }
      },

      /**
       * Cancels all uploads inside the flash movie.
       *
       * @method _cancelAllUploads
       * @private
       */
      _cancelAllUploads: function FU__cancelAllUploads()
      {
         // Cancel all uploads inside the flash movie
         var length = this.dataTable.getRecordSet().getLength();
         for (var i = 0; i < length; i++)
         {
            var record = this.dataTable.getRecordSet().getRecord(i);
            var flashId = record.getData("id");
            this.uploader.cancel(flashId);
         }
      },

      /**
       * Remove all references to files inside the data table, flash movie
       * and the this class references.
        *
       * @method _clear
       * @private
       */
      _clear: function FU__clear()
      {
         /**
          * Remove all references to files inside the data table, flash movie
          * and this class's references.
          */
         var length = this.dataTable.getRecordSet().getLength();
         this.addedFiles = {};
         this.fileStore = {};
         this.dataTable.deleteRows(0, length);
         this.uploader.clearFileList();
      }

   };

})();

