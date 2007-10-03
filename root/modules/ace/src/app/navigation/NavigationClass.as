package app.navigation
{
	import mx.containers.Canvas;
	import mx.containers.ViewStack;
	import component.tabControl.TabControl;
	import component.tabControl.TabControlSelectionChangeEvent;

	public class NavigationClass extends Canvas
	{
		public var viewStack:ViewStack;
		public var tabControl:TabControl;
		
		protected override function createChildren():void
		{
			super.createChildren();
			
			// Add event handlers
			tabControl.addEventListener(TabControlSelectionChangeEvent.SELECTION_CHANGE, onSelectionChange);
		}	
		
		private function onSelectionChange(event:TabControlSelectionChangeEvent):void
		{
			// Change the view in step with the tab
			viewStack.selectedIndex = event.selectedItem.value as Number;
		}
		
	}
}