package util.webscript
{
	import mx.rpc.http.HTTPService;
	import flash.events.Event;
	import mx.rpc.AsyncToken;
	import mx.controls.Alert;
	import mx.rpc.events.ResultEvent;
	import mx.rpc.events.FaultEvent;
	import util.authentication.AuthenticationService;
	import mx.rpc.events.InvokeEvent;
	import mx.rpc.Fault;
	import util.error.ErrorService;
	import util.authentication.AuthenticationError;
	
	/**
	 * Web script service.
	 * 
	 * This provides an encapsulated way for clients to call web scripts.
	 * 
	 * @author Roy Wetherall
	 */
	public class WebScriptService extends HTTPService
	{
		/** HTTP method constants */
		public static const GET:String = "GET";
		public static const POST:String = "POST";
		public static const PUT:String = "PUT";
		public static const DELETE:String = "DELETE";
		
		/** Web script URL */
		private var _url:String;
		
		/** HTTP method */
		private var _method:String;
		
		/** Indicates whether GET methods should be tunneled or not */
		private var _alwaysTunnelGetMethod:Boolean = true;
		
		/** Indicates whether the ticket is required to make the web service request */
		private var _ticketRequired:Boolean = true;
		
		/**
		 * Constructor
		 */
		public function WebScriptService(url:String, method:String, onSuccess:Function=null, onFailure:Function=null, ticketRequired:Boolean=true, alwaysTunnelGetMethod:Boolean = true)
		{
			// Inherited constructor
			super();
			
			// Store the url and method in case we need them later
			this._url = url;
			this._method = method;
			this._alwaysTunnelGetMethod = alwaysTunnelGetMethod;
			this._ticketRequired = ticketRequired;
			
			// Set the method
			if (this._method == GET && this._alwaysTunnelGetMethod == false)
			{
				this.method = GET;	
			}
			else
			{
				this.method = POST;
			}			
			
			// Set the url
			this.url = url;		
			
			// Ensure that a success code is always returned			
			var headerList:Array = new Array();
            headerList["alf-force-success-response"] = "true";
            this.headers = headerList;
            
            // Register the event listeners            
			addEventListener(ResultEvent.RESULT, onResultEvent);
			addEventListener(FaultEvent.FAULT, onFaultEvent);
			
			// Register the passed event handlers
			if (onSuccess != null)
			{
				addEventListener(SuccessEvent.SUCCESS, onSuccess);
			}
			if (onFailure != null)
			{
				addEventListener(FailureEvent.FAILURE, onFailure);
			}	
		}
		
		/** 
		 * Getter for the alwaysTunnelGetMethod property
		 */
		public function get alwaysTunnelGetMethod():Boolean
		{
			return this._alwaysTunnelGetMethod;
		}
		 
		 /**
		  * Setter for the alwaysTunnelGetMethod property
		  */
		public function set alwaysTunnelGetMethod(value:Boolean):void
		{
			this._alwaysTunnelGetMethod = value;
		} 
		
		/**
		 * Getter for the ticketRequired property
		 */
		public function get ticketRequired():Boolean
		{
			return this._ticketRequired;
		}
		
		/**
		 * Setter for the ticketRequired property
		 */
		public function set ticketRequired(value:Boolean):void
		{
			this._ticketRequired = value;
		}
		
		/**
		 * Execute the web script with the provided parameter values.
		 */
		public function execute(parameters:Object=null):AsyncToken
		{
			var result:AsyncToken = null;
			
			try
			{
				// Create a parameter list if it doesn't already exist
				if (parameters == null)
				{
					parameters = new Object();
				}
				
				if (this._ticketRequired == true)
				{
					// Add the current ticket to the parameters
					var ticket:String = AuthenticationService.instance.ticket;
					if (ticket == null)
					{
						throw new AuthenticationError("Unable to execute web script because required ticket is not available from the AuthenticationService.");	
					}
					else
					{
						parameters.alf_ticket = ticket;
					}
				}
				
				// Tunnel methods as required
				if (this.method == POST && this._method != POST)
				{
					parameters.alf_method = this._method;
				}			
				
				// Send the request to the HTTPService
				result = send(parameters);
			}
			catch (error:Error)
			{
				// Raise the error
				ErrorService.instance.raiseError(ErrorService.APPLICATION_ERROR, error);
			}
			
			return result;
		}
		
		/**
		 * On result event handler
		 */
		public function onResultEvent(event:ResultEvent):void
		{
			if (event.result.response != null && 
			    event.result.response.status != null && 
			    event.result.response.status.code != null && 
			    event.result.response.status.code != "200")
			{
				// Get the details of the problem from the respose
				var code:String = event.result.response.status.code;
				var message:String = event.result.response.message;
				var details:String = event.result.response.callstack;
				
				// Raise failure event
				var fault:Fault = new Fault(code, message, details);
				raiseFailureEvent(fault);
			}
			else
			{
				// Re-raise the onSuccess event
				var newEvent:SuccessEvent = new SuccessEvent(SuccessEvent.SUCCESS, event.bubbles, event.cancelable, event.result, event.token, event.message);			
				dispatchEvent(newEvent);
			}				
		}
		
		/**
		 * On fault event handler
		 */
		public function onFaultEvent(event:FaultEvent):void
		{
			// Raise the onFailure event
			raiseFailureEvent(event.fault);
		}
		
		/**
		 * Helper method to raise a failure event.  If no failure event is registered then an application error is raised.
		 */
		private function raiseFailureEvent(fault:Fault):void
		{
			if (hasEventListener(FailureEvent.FAILURE) == true)
			{
				// Raise the onFailure event
				var newEvent:FailureEvent = new FailureEvent(FailureEvent.FAILURE, false, true, fault);
				dispatchEvent(newEvent);			
			}
			else
			{
				// Just raise error if no failure event is registered
				var error:WebScriptError = new WebScriptError("Execution of web script failed:" + fault.faultString);
				ErrorService.instance.raiseError(ErrorService.APPLICATION_ERROR, error);
			}
		}
	}
}