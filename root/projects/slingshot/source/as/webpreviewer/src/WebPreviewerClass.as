package
{
	import flash.external.ExternalInterface;
	import flash.system.System;
	
	import mx.controls.Alert;
	import mx.core.Application;
	import mx.events.FlexEvent;
	
	import org.alfresco.previewer.DocumentZoomDisplayEvent;
	import org.alfresco.previewer.Previewer;
	import org.alfresco.previewer.PreviewerEvent;
	import org.hasseg.externalMouseWheel.ExternalMouseWheelSupport;

	/**
	 * Wraps the Previewer component in a html/web environment, takes the supplied variables 
	 * and loads the content defined by the url and calls javascript callbacks if something goes wrong.
	 */ 
	public class WebPreviewerClass extends Application
	{
		/**
		 * UI CONTROLS IMPLEMENTED BY MXML 
		 * 
		 * Should really be protected or private but can't be since code behind is used.
		 */
				
		public var previewer:Previewer;
		
		/**
		 * A string representing the javascript callback method that should get called
		 * if an event happens that shall be communicated to environments outside the flash player
		 * such as an document zoom display load error.
		 */
		private var jsCallback:String;

		/**
		 * Constructor
		 */ 		
		public function WebPreviewerClass()
		{
			super();			
			this.addEventListener(FlexEvent.APPLICATION_COMPLETE, onApplicationComplete);
		}
		
		/**
		 * Called by the FLEX framework when the whole application is complete and created.
		 * Will set the variables supplied throught the embed tags and set them on the components.
		 */
		public function onApplicationComplete(event:FlexEvent):void
		{
			 
			// Add mouse wheel scroll support for browsers on mac.
			ExternalMouseWheelSupport.getInstance(stage);
			
			// If something goes wrong we want to get a chance of notifying the html/javascript environment.
			previewer.documentDisplay.addEventListener(DocumentZoomDisplayEvent.DOCUMENT_LOAD_ERROR, onDocumentDisplayError);
			previewer.documentDisplay.addEventListener(DocumentZoomDisplayEvent.DOCUMENT_CONTENT_TYPE_ERROR, onDocumentDisplayError);
			
			// Make sure we can notify the html/javascript environment when we enter and leaves full window mode.
			previewer.addEventListener(PreviewerEvent.FULL_WINDOW_BUTTON_CLICK, onFullWindowClick);
			previewer.addEventListener(PreviewerEvent.FULL_WINDOW_ESCAPE, onFullWindowEscape);
			
			// Get variables from the embed/object tag
			var url:String = Application.application.parameters.url;							
			var paging:String = Application.application.parameters.paging;
			var fileName:String = Application.application.parameters.fileName;
			var showFullScreenButton:String = Application.application.parameters.show_fullscreen_button;
			var showFullWindowButton:String = Application.application.parameters.show_fullwindow_button;			
			jsCallback = Application.application.parameters.jsCallback;
			
			// i18n labels
			var i18n:Object = new Object();
			i18n.actualSize = Application.application.parameters.i18n_actualSize;
			i18n.fitPage = Application.application.parameters.i18n_fitPage;
			i18n.fitWidth = Application.application.parameters.i18n_fitWidth;
			i18n.fitHeight = Application.application.parameters.i18n_fitHeight;
			i18n.fullscreen = Application.application.parameters.i18n_fullscreen;
			i18n.fullwindow = Application.application.parameters.i18n_fullwindow;
			i18n.fullwindowEscape = Application.application.parameters.i18n_fullwindow_escape;
			i18n.page = Application.application.parameters.i18n_page;
			i18n.pageOf = Application.application.parameters.i18n_pageOf;
						
			// Set variables on the preview component								
			previewer.paging = paging != null && paging.toLowerCase() == "true";
			previewer.fileName = fileName;
			previewer.showFullScreenButton = showFullScreenButton != null && showFullScreenButton.toLowerCase() == "true";
			previewer.showFullWindowButton = showFullWindowButton != null && showFullWindowButton.toLowerCase() == "true";
			previewer.i18nLabels = i18n;
			
			trace(System.totalMemory);
			
			// Start the loading the content in to the previewer				
			previewer.url = url; 			
		}
		
		
		/**
		 * Called if something goes wrong during the loading of the content specified by url.
		 * 
		 * @param event An event describing the error.
		 */
		private function onDocumentDisplayError(event:DocumentZoomDisplayEvent):void
		{
			var code:String = "error";
			code = event.type == DocumentZoomDisplayEvent.DOCUMENT_LOAD_ERROR ? "io" : code;
			code = event.type == DocumentZoomDisplayEvent.DOCUMENT_CONTENT_TYPE_ERROR ? "content" : code;
			dispatchJavascriptEvent({ error: { code: code } });			
		}
		
		/**
		 * Called if user presses full window button in previewer component.
		 * 
		 * @param event An object describing the button click event.
		 */
		private function onFullWindowClick(event: PreviewerEvent):void
		{			
			dispatchJavascriptEvent({ event: { type: "onFullWindowClick" } });
		}

		/**
		 * Called if user presses escape in full window mode in previewer component.
		 * 
		 * @param event An object describing the escape event.
		 */
		private function onFullWindowEscape(event: PreviewerEvent):void
		{			
			dispatchJavascriptEvent({ event: { type: "onFullWindowEscape" } });
		}
				
		/**
		 * Dispatches an event to the web/html environment.
		 * 
		 * @param event The event to dispatch.
		 */	
		private function dispatchJavascriptEvent(event:Object):void
		{
			if (ExternalInterface.available && jsCallback != null)
			{													
				ExternalInterface.call(jsCallback, event);
			}				
		}
		
	}
}