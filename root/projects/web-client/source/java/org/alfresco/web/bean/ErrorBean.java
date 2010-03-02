/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.web.bean;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;

import javax.servlet.ServletException;

/**
 * Bean used by the error page, holds the last exception to be thrown by the system
 * 
 * @author gavinc
 */
public class ErrorBean implements Serializable
{
   private static final long serialVersionUID = -5101720299256547100L;

   public static final String ERROR_BEAN_NAME = "alfresco.ErrorBean";
   
   private String returnPage;
   private Throwable lastError;

   /**
    * @return Returns the page to go back to after the error has been displayed
    */
   public String getReturnPage()
   {
      return returnPage;
   }

   /**
    * @param returnPage The page to return to after showing the error
    */
   public void setReturnPage(String returnPage)
   {
      this.returnPage = returnPage;
   }

   /**
    * @return Returns the lastError.
    */
   public Throwable getLastError()
   {
      return lastError;
   }

   /**
    * @param error The lastError to set.
    */
   public void setLastError(Throwable error)
   {
      // servlet exceptions hide the actual error within the rootCause
      // variable, set the base error to that and throw away the 
      // ServletException wrapper
      if (error instanceof ServletException && 
            ((ServletException)error).getRootCause() != null)
      {
         this.lastError = ((ServletException)error).getRootCause();
      }
      else
      {
         this.lastError = error;
      }
   }
   
   /**
    * @return Returns the last error to occur in string form
    */
   public String getLastErrorMessage()
   {
      String message = "No error currently stored";
      
      if (this.lastError != null)
      {
         StringBuilder builder = new StringBuilder(this.lastError.toString());;
         Throwable cause = this.lastError.getCause();
         
         // build up stack trace of all causes
         while (cause != null)
         {
            builder.append("\ncaused by:\n");
            builder.append(cause.toString());
            
            if (cause instanceof ServletException && 
                  ((ServletException)cause).getRootCause() != null)
            {
               cause = ((ServletException)cause).getRootCause();
            }
            else
            {
               cause = cause.getCause();
            }  
         }
         
         message = builder.toString();
         
         // format the message for HTML display
         message = message.replaceAll("<", "&lt;");
         message = message.replaceAll(">", "&gt;");
         message = message.replaceAll("\n", "<br>");
      }
      
      return message;
   }
   
   /**
    * @return Returns the stack trace for the last error
    */
   public String getStackTrace()
   {
      String trace = "No stack trace available";
      
      if (this.lastError != null)
      {
         StringWriter stringWriter = new StringWriter();
         PrintWriter writer = new PrintWriter(stringWriter);
         this.lastError.printStackTrace(writer);
         
         // format the message for HTML display
         trace = stringWriter.toString();
         trace = trace.replaceAll("<", "&lt;");
         trace = trace.replaceAll(">", "&gt;");
         trace = trace.replaceAll("\n", "<br>");
      }
      
      return trace;
   }
}
