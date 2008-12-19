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

package org.alfresco.previewer
{
	import flash.display.StageDisplayState;
	import flash.events.FullScreenEvent;
	import flash.events.KeyboardEvent;
	import flash.events.MouseEvent;
	import flash.ui.Keyboard;
	
	import mx.collections.ArrayCollection;
	import mx.containers.ApplicationControlBar;
	import mx.containers.VBox;
	import mx.controls.Button;
	import mx.controls.Label;
	import mx.controls.Menu;
	import mx.controls.TextInput;
	import mx.controls.sliderClasses.Slider;
	import mx.events.FlexEvent;
	import mx.events.ListEvent;
	import mx.events.SliderEvent;

	/**
	 * Component that uses the DocumentZoomDisplay component and adds ui controls for
	 * zooming, paging and fullscreen.
	 * 
	 * This component does not extend Application since it now can be reused as a separate component
	 * both on a wepage and in an air desktop application.
	 */
	[Bindable]
	public class PreviewerClass extends VBox
	{
						
		/**
		 * UI CONTROLS IMPLEMENTED BY MXML 
		 * 
		 * Should really be protected or private but can't be since code behind is used.
		 */
				
		public var headerApplicationControlBar:ApplicationControlBar;
		
		public var fileNameLabel:Label;
		public var zoomInButton:Button;
		public var zoomOutButton:Button;
		public var zoomSlider:Slider;

		public var zoomPercentageTextInput:TextInput;
		public var snapPointsButton:Button;
		
		public var previousButton:Button;
		public var pageTextInput:TextInput;
		public var noOfPagesLabel:Label;
		public var nextButton:Button;

		public var fullScreenButton:Button;
		
		public var documentDisplay:DocumentZoomDisplay;
		
		
		/** 
		 * The menu displaying the snapPoints (created in actionscript).
		 */		
		private var snapPointsMenu:Menu;
		
		/**
		 * The snapPoints displayed in the snapPointsMenu.
		 */
		private var snapPoints:ArrayCollection = new ArrayCollection();		
		
		/**
		 * The amount the scale/zoom shall change when the xoomInButton or zoomOutButton is clicked.
		 */
		private var zoomButtonStepAmount:Number = 0.1;
		
		/**
		 * Constructor
		 */
		public function PreviewerClass()
		{
			super();
			
			// Wait until the component is created so we can adjust the child components			
			addEventListener(FlexEvent.CREATION_COMPLETE, onCreationComplete);					
		}
		
		/**
		 * The url to the content to display.
		 * 
		 * @param url The url to the conent to load and display.
		 */
		public function set url(url:String):void
		{
			// Disable gui controls until content is loaded
			currentState = "disabled";
			
			// Load the content
			documentDisplay.url = url;
		}
		
		/**
		 * The url to the content to display.
		 * 
		 * @return The url to the conent to load and display.
		 */		
		public function get url():String
		{
			return documentDisplay.url;	
		}

		/**
		 * True if paging shall be enabled for AVM2/Actionscript3 movie clips.
		 * Default is false.
		 * 
		 * @param paging True if paging shall be enabled for AVM2/Actionscript3 movie clips.
		 */
		public function set paging(paging:Boolean):void
		{
			documentDisplay.paging = paging;
		}
		
		/**
		 * True if paging shall be enabled for AVM2/Actionscript3 movie clips.
		 * 
		 * @return True if paging shall be enabled for AVM2/Actionscript3 movie clips.
		 */
		public function get paging():Boolean
		{
			return documentDisplay.paging;
		}
		
				
		/**
		 * The fileName to the content to display.
		 * 
		 * @param fileName The url for the content to load and display.
		 */
		public function set fileName(fileName:String):void
		{
			fileNameLabel.text = fileName;
		}
		
		/**
		 * The fileName to the content to display.
		 * 
		 * @return The fileName for the content to load and display.
		 */		
		public function get fileName():String
		{
			return fileNameLabel.text;	
		}

			
		/**
		 * Called when the FLEX framework think this component has been created.
		 * Will adjust all the child components.
		 * 
		 * @param event Describing that this component is created.
		 */	
		private function onCreationComplete(event:FlexEvent):void
		{																		
            // Listen for new snappoints from the document zoom display
            documentDisplay.addEventListener(DocumentZoomDisplayEvent.DOCUMENT_SNAP_POINTS_CHANGE, onDocumentSnapPointsChange);
            
            // Make sure we update our gui depending on what page that currently is displayed
            documentDisplay.addEventListener(DocumentZoomDisplayEvent.DOCUMENT_PAGE_SCOPE_CHANGE, onDocumentPageScopeChange);

            // Make sure we update our slider if the document zoom display decides to change the scale
            documentDisplay.addEventListener(DocumentZoomDisplayEvent.DOCUMENT_SCALE_CHANGE, onDocumentScaleChange);
            
            // Listen for error events so we can disable our controls
			documentDisplay.addEventListener(DocumentZoomDisplayEvent.DOCUMENT_LOAD_ERROR, onDocumentDisplayError);
			documentDisplay.addEventListener(DocumentZoomDisplayEvent.DOCUMENT_CONTENT_TYPE_ERROR, onDocumentDisplayError);

            
            // Setup zoom slider control           
            zoomSlider.liveDragging = true;
            zoomSlider.minimum = 0.17; // Going below 0.17 makes the first frame invert??!!
            zoomSlider.maximum = 2.50;
            zoomSlider.value = 1;            
            zoomSlider.snapInterval = 0.001;
            zoomSlider.addEventListener(SliderEvent.CHANGE, onZoomChange);
            zoomSlider.dataTipFormatFunction = formatZoomTip;
            
            // Setup listeners for zoom buttons
            zoomInButton.addEventListener(MouseEvent.CLICK, onZoomInClick);
            zoomOutButton.addEventListener(MouseEvent.CLICK, onZoomOutClick);
            
            // The hidden button menu for snapPoints 
            zoomPercentageTextInput.addEventListener(KeyboardEvent.KEY_DOWN, onZoomPercentageTextInputKeyDown);                        
			snapPointsMenu = Menu.createMenu(this, snapPoints, false);
			snapPointsMenu.setStyle("openDuration", 0); 
            snapPointsMenu.addEventListener(ListEvent.CHANGE, onSnapPointsMenuChange);            
            snapPointsButton.addEventListener(MouseEvent.CLICK, onSnapPointsButtonClick);

			// Setup listeners for paging controls			
            nextButton.addEventListener(MouseEvent.CLICK, onNextClick);            
            previousButton.addEventListener(MouseEvent.CLICK, onPreviousClick);            
            pageTextInput.addEventListener(KeyboardEvent.KEY_DOWN, onCurrentPageKeyDown);

            // Setup listeners for fullscreen controls            
            fullScreenButton.addEventListener(MouseEvent.CLICK, onFullscreenClick);            
        }
			
		/**
		 * Called when the document zoom display has calcuated snapPoints to use for scaling the content.
		 * 
		 * @param event Desribes the scale snapPoints that shall be used for the content in the display area
		 */
		public function onDocumentSnapPointsChange(event:DocumentZoomDisplayEvent):void
		{	
			// Remove older snapPoints, menu will update since snapPoints is dataProivder to snapPointsMenu								            
            snapPoints.removeAll();            
            snapPoints.addItem({label: "Actual Size", data: 1});
            snapPoints.addItem({label: "Fit Page", data: event.fitToScreen});
            snapPoints.addItem({label: "Fit Width", data: event.fitToWidth});
            snapPoints.addItem({label: "Fit Height", data: event.fitToHeight});
            
            // Do show /hide the menu gets a size so its location later can be calculated
            snapPointsMenu.show(-400, - 400);
            snapPointsMenu.hide();            
                        			            
                                    	
        	// Set the zoom of the content to the most appropriate
			zoomSlider.value = event.fitByContentType;	
			changeZoom();
		}
				
		/**
		 * Called when a new page is displayed in the top of the document zoom display's display are.
		 * 
		 * @param event Describes the current page scope in in the cosument zoom display.
		 */ 
		public function onDocumentPageScopeChange(event:DocumentZoomDisplayEvent):void
		{	
			// Update the input and label controls		
			pageTextInput.text = event.page + "";
			noOfPagesLabel.text = event.noOfPages + "";
			if(stage.displayState != StageDisplayState.FULL_SCREEN)
			{
				if (event.noOfPages > 1)
				{
					currentState = "";
				}
				else
				{
					currentState = "singlePaged";
				}
			}
		}
		
		/**
		 * Called when a new page is displayed in the top of the document zoom display's display are.
		 * 
		 * @param event Describes the current page scope in in the cosument zoom display.
		 */ 
		public function onDocumentScaleChange(event:DocumentZoomDisplayEvent):void
		{
			// Set slider to the new zoom value, changed by the document display itself 
			zoomSlider.value = event.documentScale;	
			
			// Update zoom gi controls
			updateZoomControls();
		}
		
		
		/**
		 * Called if something goes wrong during the loading of the content specified by url.
		 * 
		 * @param event An event describing the error.
		 */
		private function onDocumentDisplayError(event:DocumentZoomDisplayEvent):void
		{
			currentState = "disabled";				
		}
		
		/**
		 * Called when the user clicks the previous page button.
		 * Will make document zoom display display the previous page.
		 * 
		 * @param event Describes the user mouse event
		 */
		public function onPreviousClick(event:MouseEvent):void
		{						
			changePage(new Number(pageTextInput.text) - 1);
		}

		/**
		 * Called when the user clicks the next page button.
		 * Will make document zoom display display the next page.
		 * 
		 * @param event Describes the user mouse event
		 */
		public function onNextClick(event:MouseEvent):void
		{
			changePage(new Number(pageTextInput.text) + 1);
		}

		/**
		 * Called when the user clicks a key on the keyboard and the currentPageTextInput has focus.
		 * Will make document zoom display display the page written in the text input control.
		 * 
		 * @param event Describes the user keyboard event
		 */
		public function onCurrentPageKeyDown(event:KeyboardEvent):void
		{            	 
			if (event.keyCode == Keyboard.LEFT || 
				event.keyCode == Keyboard.UP ||
				event.keyCode == Keyboard.RIGHT || 
				event.keyCode == Keyboard.DOWN)
			{
				/**
				 *  Make sure navigationg inside the textfield doesn't bubble up 
				 *  and cause a page change caused by arrow keys 
				 * that was used to navigate inside the text input. 
				 */
            	event.stopImmediatePropagation();
            }
            else if (event.keyCode == Keyboard.ENTER)
            {
            	// Change the page when enter is pressed.
            	var p:Number = new Number(pageTextInput.text);
            	var noOfPages:int = new int(noOfPagesLabel.text);           	
            	if (!p || p < 1 || p > noOfPages)
            	{
            		// An invalid page number was entered, try and find an appropiate page number instead
            		p = p ? p : 0;
            		p = Math.max(p, 1);
            		p = Math.min(p, noOfPages);
            		pageTextInput.text = p + "";            	
            	}
            	// call the method that changes the page
        		changePage(p);
            }
        }
		
		/**
		 * Called by other methods to change the page in the document zoom display.
		 * 
		 * @param page The page number to display.
		 */
		private function changePage(page:int):void
		{
			documentDisplay.page = page;
		}
		
		/**
		 * Called when the user has clicked the zoom out button (-).
		 * Will make the content smaller.
		 * 
		 * @param event Describes the user mouse click.
		 */
		public function onZoomOutClick(event:MouseEvent):void
		{			
			zoomSlider.value = zoomSlider.value - zoomButtonStepAmount < zoomSlider.minimum ? zoomSlider.minimum : zoomSlider.value - zoomButtonStepAmount;
			changeZoom();
		}

		/**
		 * Called when the user has clicked the zoom out button (-).
		 * Will make the content smaller.
		 * 
		 * @param event Describes the key event .
		 */
		public function onZoomPercentageTextInputKeyDown(event:KeyboardEvent):void
		{		
			if (event.keyCode == Keyboard.LEFT || 
				event.keyCode == Keyboard.UP ||
				event.keyCode == Keyboard.RIGHT || 
				event.keyCode == Keyboard.DOWN)
			{
				/**
				 *  Make sure navigationg inside the textfield doesn't bubble up 
				 *  and cause a page change caused by arrow keys 
				 * that was used to navigate inside the text input. 
				 */
            	event.stopImmediatePropagation();
            }
            else if(event.keyCode == Keyboard.ENTER)
			{
				var scale:Number;	
				var str:String = zoomPercentageTextInput.text.split("%")[0];
	        	var percentage:Number = new Number(str);        	           
	        	if (percentage)
	        	{
	        		scale = percentage / 100; 
	        		scale = Math.max(scale, zoomSlider.minimum);
	        		scale = Math.min(scale, zoomSlider.maximum);
	        	}
	        	else
	        	{
	        		scale = snapPoints.getItemAt(1).data;
	        	}
	
				zoomSlider.value = scale;
				changeZoom();
			}
		}

		/**
		 * Called when the user has selected a snapPOint from the snapPointMenu.
		 * Will change the zoom/scale of the content inside the 
		 * document zoom display to the scale specified by the snapPoint.		 
		 * 
		 * @param event Describes the list event.
		 */
		public function onSnapPointsMenuChange(event:ListEvent):void
		{
			zoomSlider.value = snapPointsMenu.selectedItem.data;
			changeZoom();					
		}
		
		/**
		 * Called when the user has clicked the zoom in button (+).
		 * Will make the content bigger.
		 * 
		 * @param event Describes the user mouse click.
		 */
		public function onZoomInClick(event:MouseEvent):void
		{						
			zoomSlider.value = zoomSlider.value + zoomButtonStepAmount > zoomSlider.maximum ? zoomSlider.maximum : zoomSlider.value + zoomButtonStepAmount;
			changeZoom();
		}

		/**
		 * Called when the user has clicked the snapPointsButton.
		 * Will display the snapPointsMenu.
		 * 
		 * @param event Describes the user mouse click.
		 */ 
		public function onSnapPointsButtonClick(event:MouseEvent):void
		{			
			snapPointsMenu.show(snapPointsButton.x + snapPointsButton.width - snapPointsMenu.width, 2 + snapPointsButton.y + snapPointsButton.height);
		}
		
		/**
		 * Called when the user has clicked or dragged the zoomSlider to a new value.
		 * Will scale/zoom the content.
		 * 
		 * @param event Describes the slider event.
		 */
		private function onZoomChange(event:SliderEvent):void
		{			
			changeZoom();						
		}
		
		/**
		 * Called by other methods to actually invoke the zoom and disable/enable the affected parts of the gui.
		 */
		private function changeZoom():void
		{	
			// Update other gui controls
			updateZoomControls();
			
			// Change the zoom on the document in the document display			
			documentDisplay.zoom = zoomSlider.value;
		}
		
		private function updateZoomControls():void
		{
			// Update the percent text input with the new zoom value.		
			zoomPercentageTextInput.text = zoomSlider.value ? Math.round(zoomSlider.value * 100)  + "%" : "";
			
			// Make sure zoom buttons only are enabled if its possible to zoom in/out further 
			zoomInButton.enabled = zoomSlider.value < zoomSlider.maximum;
			zoomOutButton.enabled = zoomSlider.value > zoomSlider.minimum;
			
			// Make doc display interactive
			documentDisplay.interactiveDocument = !zoomOutButton.enabled;				
		}
		
		/**
		 * Called when the user drags the zoomSlider.
		 * Will format the tooltip displayed during the drag to display the scale in percentage.
		 * 
		 * @param value The value of the zoomSlider when its dragged.
		 */
		private function formatZoomTip(value:Number):String { 
        	var percentage:int = Math.round(value * 100); 
        	return percentage + "%"; 
      	}
      	
      	/**
      	 * Called when the user clicks the fullScreenButton.
      	 * Will change the display to cover all of the users monitor.
      	 */
      	private function onFullscreenClick(event:MouseEvent):void
		{	
			// Make sure we get a change to take action when toggling between normal and full screen	
			stage.addEventListener(FullScreenEvent.FULL_SCREEN, onFullScreenDisplayStates);
			
			// Display as full screen									
			stage.displayState = StageDisplayState.FULL_SCREEN;
		}
		
		private function onFullScreenDisplayStates(event:FullScreenEvent):void
		{
			if (event.fullScreen)
			{
				/**
				 * Change the gui so filename is visible and text inputs are disabled
				 * since they can't be used in full screen due to adobe security policies.
				 */
				 if(currentState == "singlePaged")
				 {
		 			currentState = "singlePagedFullScreen";					 	
				 }
				 else
				 {
				 	currentState = "fullScreen";	
				 }				
			}
			else
			{
				// Change the gui so filename is hidden and text inputs are enabled again.
				if (currentState == "singlePagedFullScreen")
				{
		 			currentState = "singlePaged";					 	
				}
				else
				{
				 	currentState = "";	
				}				
			}
			
	
		}

	}
}