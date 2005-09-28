/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the Mozilla Public License version 1.1 
 * with a permitted attribution clause. You may obtain a
 * copy of the License at
 *
 *   http://www.alfresco.org/legal/license.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.web.bean.wizard;

import java.util.ResourceBundle;

import javax.faces.context.FacesContext;

import org.alfresco.web.app.Application;
import org.alfresco.web.bean.repository.Repository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Handler class used by the Create In-line Content Wizard 
 * 
 * @author Kevin Roast
 */
public class CreateContentWizard extends BaseContentWizard
{
   private static Log logger = LogFactory.getLog(CreateContentWizard.class);

   // TODO: retrieve these from the config service
   private static final String WIZARD_TITLE_ID = "create_content_title";
   private static final String WIZARD_DESC_ID = "create_content_desc";
   private static final String STEP1_TITLE_ID = "create_content_step1_title";
   private static final String STEP1_DESCRIPTION_ID = "create_content_step1_desc";
   private static final String STEP2_TITLE_ID = "create_content_step2_title";
   private static final String STEP2_DESCRIPTION_ID = "create_content_step2_desc";
   
   // create content wizard specific properties
   private String content;
   
   
   /**
    * Deals with the finish button being pressed
    * 
    * @return outcome
    */
   public String finish()
   {
      return saveContent(null, this.content);
   }
   
   /**
    * @see org.alfresco.web.bean.wizard.AbstractWizardBean#getWizardDescription()
    */
   public String getWizardDescription()
   {
      return Application.getMessage(FacesContext.getCurrentInstance(), WIZARD_DESC_ID);
   }

   /**
    * @see org.alfresco.web.bean.wizard.AbstractWizardBean#getWizardTitle()
    */
   public String getWizardTitle()
   {
      return Application.getMessage(FacesContext.getCurrentInstance(), WIZARD_TITLE_ID);
   }
   
   /**
    * @see org.alfresco.web.bean.wizard.AbstractWizardBean#getStepDescription()
    */
   public String getStepDescription()
   {
      String stepDesc = null;
      
      switch (this.currentStep)
      {
         case 1:
         {
            stepDesc = Application.getMessage(FacesContext.getCurrentInstance(), STEP1_DESCRIPTION_ID);
            break;
         }
         case 2:
         {
            stepDesc = Application.getMessage(FacesContext.getCurrentInstance(), STEP2_DESCRIPTION_ID);
            break;
         }
         case 3:
         {
            stepDesc = Application.getMessage(FacesContext.getCurrentInstance(), SUMMARY_DESCRIPTION_ID);
            break;
         }
         default:
         {
            stepDesc = "";
         }
      }
      
      return stepDesc;
   }

   /**
    * @see org.alfresco.web.bean.wizard.AbstractWizardBean#getStepTitle()
    */
   public String getStepTitle()
   {
      String stepTitle = null;
      
      switch (this.currentStep)
      {
         case 1:
         {
            stepTitle = Application.getMessage(FacesContext.getCurrentInstance(), STEP1_TITLE_ID);
            break;
         }
         case 2:
         {
            stepTitle = Application.getMessage(FacesContext.getCurrentInstance(), STEP2_TITLE_ID);
            break;
         }
         case 3:
         {
            stepTitle = Application.getMessage(FacesContext.getCurrentInstance(), SUMMARY_TITLE_ID);
            break;
         }
         default:
         {
            stepTitle = "";
         }
      }
      
      return stepTitle;
   }
   
   /**
    * @return Returns the content from the edited form.
    */
   public String getContent()
   {
      return this.content;
   }
   
   /**
    * @param content The content to edit (should be clear initially)
    */
   public void setContent(String content)
   {
      this.content = content;
   }
   
   /**
    * Initialises the wizard
    */
   public void init()
   {
      super.init();
      
      this.content = null;
      this.fileName = "newfile.html";
      this.contentType = Repository.getMimeTypeForFileName(
               FacesContext.getCurrentInstance(), this.fileName);
      this.title = this.fileName;
      
      // created content is inline editable by default
      this.inlineEdit = true;
   }
   
   /**
    * @return Returns the summary data for the wizard.
    */
   public String getSummary()
   {
      ResourceBundle bundle = Application.getBundle(FacesContext.getCurrentInstance());
      
      // TODO: show first few lines of content here?
      return buildSummary(
            new String[] {bundle.getString("file_name"), bundle.getString("type"), 
                          bundle.getString("content_type"), bundle.getString("title"), 
                          bundle.getString("description"), bundle.getString("author")},
            new String[] {this.fileName, getSummaryObjectType(), getSummaryContentType(), 
                          this.title, this.description, this.author});
   }
   
   /**
    * @see org.alfresco.web.bean.wizard.AbstractWizardBean#determineOutcomeForStep(int)
    */
   protected String determineOutcomeForStep(int step)
   {
      String outcome = null;
      
      switch(step)
      {
         case 1:
         {
            outcome = "create";
            break;
         }
         case 2:
         {
            outcome = "properties";
            break;
         }
         case 3:
         {
            outcome = "summary";
            break;
         }
         default:
         {
            outcome = CANCEL_OUTCOME;
         }
      }
      
      return outcome;
   }
}
