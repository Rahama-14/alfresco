/*
 * Created on 20-May-2005
 */
package org.alfresco.web.bean;

import java.io.File;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.transaction.UserTransaction;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.coci.CheckOutCheckInService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.context.UIContextService;
import org.alfresco.web.app.servlet.DownloadContentServlet;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.apache.log4j.Logger;

/**
 * @author Kevin Roast
 */
public class CheckinCheckoutBean
{
   // ------------------------------------------------------------------------------
   // Bean property getters and setters 
   
   /**
    * @return Returns the BrowseBean.
    */
   public BrowseBean getBrowseBean()
   {
      return this.browseBean;
   }
   
   /**
    * @param browseBean The BrowseBean to set.
    */
   public void setBrowseBean(BrowseBean browseBean)
   {
      this.browseBean = browseBean;
   }
   
   /**
    * @return Returns the NodeService.
    */
   public NodeService getNodeService()
   {
      return this.nodeService;
   }

   /**
    * @param nodeService The NodeService to set.
    */
   public void setNodeService(NodeService nodeService)
   {
      this.nodeService = nodeService;
   }
   
   /**
    * @return Returns the VersionOperationsService.
    */
   public CheckOutCheckInService getVersionOperationsService()
   {
      return this.versionOperationsService;
   }
   
   /**
    * @param versionOperationsService  The VersionOperationsService to set.
    */
   public void setVersionOperationsService(CheckOutCheckInService versionOperationsService)
   {
      this.versionOperationsService = versionOperationsService;
   }
   
   /**
    * @return Returns the ContentService.
    */
   public ContentService getContentService()
   {
      return this.contentService;
   }

   /**
    * @param contentService   The ContentService to set.
    */
   public void setContentService(ContentService contentService)
   {
      this.contentService = contentService;
   }
   
   /**
    * @return The document node being used for the current operation
    */
   public Node getDocument()
   {
      return this.document;
   }

   /**
    * @param document The document node to be used for the current operation
    */
   public void setDocument(Node document)
   {
      this.document = document;
   }
   
   /**
    * @return Returns the working copy Document.
    */
   public Node getWorkingDocument()
   {
      return this.workingDocument;
   }
   
   /**
    * @param workingDocument The working copy Document to set.
    */
   public void setWorkingDocument(Node workingDocument)
   {
      this.workingDocument = workingDocument;
   }
   
   /**
    * @param keepCheckedOut   The keepCheckedOut to set.
    */
   public void setKeepCheckedOut(boolean keepCheckedOut)
   {
      this.keepCheckedOut = keepCheckedOut;
   }
   
   /**
    * @return Returns the keepCheckedOut.
    */
   public boolean getKeepCheckedOut()
   {
      return this.keepCheckedOut;
   }
   
   /**
    * @return Returns the version history notes.
    */
   public String getVersionNotes()
   {
      return this.versionNotes;
   }

   /**
    * @param versionNotes  The version history notes to set.
    */
   public void setVersionNotes(String versionNotes)
   {
      this.versionNotes = versionNotes;
   }
   
   /**
    * @return Returns the selected Space Id.
    */
   public String getSelectedSpaceId()
   {
      return this.selectedSpaceId;
   }
   
   /**
    * @param selectedSpaceId  The selected Space Id to set.
    */
   public void setSelectedSpaceId(String selectedSpaceId)
   {
      this.selectedSpaceId = selectedSpaceId;
   }
   
   /**
    * @return Returns the copy location. Either the current or other space.
    */
   public String getCopyLocation()
   {
      if (this.fileName != null)
      {
         return this.COPYLOCATION_OTHER;
      }
      else
      {
         return this.copyLocation;
      }
   }
   
   /**
    * @param copyLocation The copy location. Either the current or other space.
    */
   public void setCopyLocation(String copyLocation)
   {
      this.copyLocation = copyLocation;
   }
   
   /**
    * @return Returns the name of the file
    */
   public String getFileName()
   {
      // try and retrieve the file and filename from the file upload bean
      // representing the file we previously uploaded.
      FacesContext ctx = FacesContext.getCurrentInstance();
      FileUploadBean fileBean = (FileUploadBean)ctx.getExternalContext().getSessionMap().
         get(FileUploadBean.FILE_UPLOAD_BEAN_NAME);
      if (fileBean != null)
      {
         this.file = fileBean.getFile();
         this.fileName = fileBean.getFileName();
      }
      
      return this.fileName;
   }

   /**
    * @param fileName The name of the file
    */
   public void setFileName(String fileName)
   {
      this.fileName = fileName;
      
      // we also need to keep the file upload bean in sync
      FacesContext ctx = FacesContext.getCurrentInstance();
      FileUploadBean fileBean = (FileUploadBean)ctx.getExternalContext().getSessionMap().
         get(FileUploadBean.FILE_UPLOAD_BEAN_NAME);
      if (fileBean != null)
      {
         fileBean.setFileName(this.fileName);
      }
   }
   
   /**
    * @return Returns the document content used for in-line editing.
    */
   public String getDocumentContent()
   {
      return this.documentContent;
   }
   
   /**
    * @param documentContent     The document content for in-line editing.
    */
   public void setDocumentContent(String documentContent)
   {
      this.documentContent = documentContent;
   }
   
   /**
    * @return Returns output from the in-line editor page.
    */
   public String getEditorOutput()
   {
      return this.editorOutput;
   }

   /**
    * @param editorOutput  The output from the in-line editor page
    */
   public void setEditorOutput(String editorOutput)
   {
      this.editorOutput = editorOutput;
   }
   
   
   // ------------------------------------------------------------------------------
   // Navigation action event handlers
   
   /**
    * Action event called by all actions that need to setup a Content Document context on the 
    * CheckinCheckoutBean before an action page/wizard is called. The context will be a Node in
    * setDocument() which can be retrieved on action pages via getDocument().
    * 
    * @param event   ActionEvent
    */
   public void setupContentAction(ActionEvent event)
   {
      UIActionLink link = (UIActionLink)event.getComponent();
      Map<String, String> params = link.getParameterMap();
      String id = params.get("id");
      if (id != null && id.length() != 0)
      {
         setupContentDocument(id);
      }
      else
      {
         setDocument(null);
      }
      
      clearUpload();
   }
   
   /**
    * Setup a content document node context
    * 
    * @param id      GUID of the node to setup as the content document context
    * 
    * @return The Node
    */
   private Node setupContentDocument(String id)
   {
      if (logger.isDebugEnabled())
         logger.debug("Setup for action, setting current document to: " + id);

      Node node = null;
      
      try
      {
         // create the node ref, then our node representation
         NodeRef ref = new NodeRef(Repository.getStoreRef(), id);
         node = new Node(ref, this.nodeService);
         
         // create content URL to the content download servlet with ID and expected filename
         // the myfile part will be ignored by the servlet but gives the browser a hint
         String url = DownloadContentServlet.generateURL(ref, node.getName());
         node.getProperties().put("url", url);
         node.getProperties().put("workingCopy", node.hasAspect(ContentModel.ASPECT_WORKING_COPY));            
         
         // remember the document
         setDocument(node);
         
         // refresh the UI, calling this method now is fine as it basically makes sure certain
         // beans clear the state - so when we finish here other beans will have been reset
         UIContextService.getInstance(FacesContext.getCurrentInstance()).notifyBeans();
      }
      catch (InvalidNodeRefException refErr)
      {
         Utils.addErrorMessage( MessageFormat.format(Repository.ERROR_NODEREF, new Object[] {id}) );
      }
      
      return node;
   }
   
   /**
    * Action called upon completion of the Check Out file page
    */
   public String checkoutFile()
   {
      String outcome = null;
      
      UserTransaction tx = null;
      
      Node node = getDocument();
      if (node != null)
      {
         try
         {
            tx = Repository.getUserTransaction(FacesContext.getCurrentInstance());
            tx.begin();
            
            if (logger.isDebugEnabled())
               logger.debug("Trying to checkout content node Id: " + node.getId());
            
            // checkout the node content to create a working copy
            if (logger.isDebugEnabled())
            {
               logger.debug("Checkout copy location: " + getCopyLocation());
               logger.debug("Selected Space Id: " + this.selectedSpaceId);
            }
            NodeRef workingCopyRef;
            if (getCopyLocation().equals(COPYLOCATION_OTHER) && this.selectedSpaceId != null)
            {
               // checkout to a arbituary parent Space 
               NodeRef destRef = new NodeRef(Repository.getStoreRef(), 
                     this.selectedSpaceId);
               
               ChildAssociationRef childAssocRef = this.nodeService.getPrimaryParent(destRef);
               workingCopyRef = this.versionOperationsService.checkout(node.getNodeRef(),
                     destRef, ContentModel.ASSOC_CONTAINS, childAssocRef.getQName());
            }
            else
            {
               workingCopyRef = this.versionOperationsService.checkout(node.getNodeRef());
            }
            
            // set the working copy Node instance
            Node workingCopy = new Node(workingCopyRef, this.nodeService);
            setWorkingDocument(workingCopy);
            
            // create content URL to the content download servlet with ID and expected filename
            // the myfile part will be ignored by the servlet but gives the browser a hint
            String url = DownloadContentServlet.generateURL(workingCopyRef, workingCopy.getName());
            
            workingCopy.getProperties().put("url", url);
            
            // commit the transaction
            tx.commit();
            
            // show the page that display the checkout link
            outcome = "checkoutFileLink";
         }
         catch (Throwable err)
         {
            // rollback the transaction
            try { if (tx != null) {tx.rollback();} } catch (Exception tex) {}
            Utils.addErrorMessage("Unable to checkout Content Node due to system error: " + err.getMessage());
         }
      }
      else
      {
         logger.warn("WARNING: checkoutFile called without a current Document!");
      }
      
      return outcome;
   }
   
   /**
    * Action called upon completion of the Check Out file Link download page
    */
   public String checkoutFileOK()
   {
      String outcome = null;
      
      Node node = getWorkingDocument();
      if (node != null)
      {
         // clean up and clear action context
         clearUpload();
         setDocument(null);
         setWorkingDocument(null);
         
         outcome = "browse";
      }
      else
      {
         logger.warn("WARNING: checkoutFileOK called without a current WorkingDocument!");
      }
      
      return outcome;
   }
   
   /**
    * Action called upon completion of the Edit File download page
    */
   public String editFileOK()
   {
      String outcome = null;
      
      Node node = getDocument();
      if (node != null)
      {
         // clean up and clear action context
         clearUpload();
         setDocument(null);
         setWorkingDocument(null);
         
         outcome = "browse";
      }
      else
      {
         logger.warn("WARNING: editFileOK called without a current Document!");
      }
      
      return outcome;
   }
   
   /**
    * Action handler called to calculate which editing screen to display based on the mimetype
    * of a document. If appropriate, the in-line editing screen will be shown.
    */
   public void editFile(ActionEvent event)
   {
      UIActionLink link = (UIActionLink)event.getComponent();
      Map<String, String> params = link.getParameterMap();
      String id = params.get("id");
      if (id != null && id.length() != 0)
      {
         Node node = setupContentDocument(id);
         // TODO: detect the inline editing aspect here instead
         String mimetype = (String)node.getProperties().get("mimetype");
         if ("text/html".equals(mimetype))
         {
            // found a document that can be edited in-line
            // retrieve the content so it's available to the editing screen
            ContentReader reader = getContentService().getReader(node.getNodeRef());
            setDocumentContent( reader.getContentString() );
            setEditorOutput(null);
            
            // navigate to appropriate screen
            FacesContext fc = FacesContext.getCurrentInstance();
            fc.getApplication().getNavigationHandler().handleNavigation(fc, null, "editInline");
         }
         else
         {
            // normal downloadable document
            FacesContext fc = FacesContext.getCurrentInstance();
            fc.getApplication().getNavigationHandler().handleNavigation(fc, null, "editFile");
         }
      }
   }
   
   /**
    * Action handler called to set the content of a node from an inline editing page.
    */
   public String editInlineOK()
   {
      String outcome = null;
      
      UserTransaction tx = null;
      
      Node node = getDocument();
      if (node != null)
      {
         try
         {
            tx = Repository.getUserTransaction(FacesContext.getCurrentInstance());
            tx.begin();
            
            if (logger.isDebugEnabled())
               logger.debug("Trying to update content node Id: " + node.getId());
            
            // get an updating writer that we can use to modify the content on the current node
            ContentWriter writer = this.contentService.getUpdatingWriter(node.getNodeRef());
            writer.putContent(this.editorOutput);
            
            // commit the transaction
            tx.commit();
            
            // clean up and clear action context
            clearUpload();
            setDocument(null);
            setDocumentContent(null);
            setEditorOutput(null);
            
            outcome = "browse";
         }
         catch (Throwable err)
         {
            // rollback the transaction
            try { if (tx != null) {tx.rollback();} } catch (Exception tex) {}
            Utils.addErrorMessage("Unable to update Content Node due to system error: " + err.getMessage());
         }
      }
      else
      {
         logger.warn("WARNING: editInlineOK called without a current Document!");
      }
      
      return outcome;
   }
   
   /**
    * Action to undo the checkout of a document just checked out from the checkout screen.
    */
   public String undoCheckout()
   {
      String outcome = null;
      
      Node node = getWorkingDocument();
      if (node != null)
      {
         try
         {
            // try to cancel checkout of the working copy
            this.versionOperationsService.cancelCheckout(node.getNodeRef());
            
            clearUpload();
            
            outcome = "browse";
         }
         catch (Throwable err)
         {
            Utils.addErrorMessage("Unable to cancel checkout of Content Node due to system error: " + err.getMessage(), err);
         }
      }
      else
      {
         logger.warn("WARNING: undoCheckout called without a current WorkingDocument!");
      }
      
      return outcome;
   }
   
   /**
    * Action to undo the checkout of a locked document. This document may either by the original copy
    * or the working copy node. Therefore calculate which it is, if the working copy is found then
    * we simply cancel checkout on that document. If the original copy is found then we need to find
    * the appropriate working copy and perform the action on that node.
    */
   public String undoCheckoutFile()
   {
      String outcome = null;
      
      Node node = getDocument();
      if (node != null)
      {
         try
         {
            if (node.hasAspect(ContentModel.ASPECT_WORKING_COPY))
            {
               this.versionOperationsService.cancelCheckout(node.getNodeRef());
            }
            else if (node.hasAspect(ContentModel.ASPECT_LOCKABLE))
            {
               // TODO: find the working copy for this document and cancel the checkout on it
               //       is this possible? as currently only the workingcopy aspect has the copyReference
               //       attribute - this means we cannot find out where the copy is to cancel it!
               //       can we construct an XPath node lookup?
               throw new RuntimeException("NOT IMPLEMENTED");
            }
            else
            {
               throw new IllegalStateException("Node supplied for undo checkout has neither Working Copy or Locked aspect!");
            }
            
            clearUpload();
            
            outcome = "browse";
         }
         catch (Throwable err)
         {
            Utils.addErrorMessage("Unable to cancel checkout of Content Node due to system error: " + err.getMessage(), err);
         }
      }
      else
      {
         logger.warn("WARNING: undoCheckout called without a current WorkingDocument!");
      }
      
      return outcome;
   }
   
   /**
    * Action called upon completion of the Check In file page
    */
   public String checkinFileOK()
   {
      String outcome = null;
      
      UserTransaction tx = null;
      
      // NOTE: for checkin the document node _is_ the working document!
      Node node = getDocument();
      if (node != null && (getCopyLocation().equals(COPYLOCATION_CURRENT) || this.getFileName() != null))
      {
         try
         {
            tx = Repository.getUserTransaction(FacesContext.getCurrentInstance());
            tx.begin();
            
            if (logger.isDebugEnabled())
               logger.debug("Trying to checkin content node Id: " + node.getId());
            
            // we can either checkin the content from the current working copy node
            // which would have been previously updated by the user
            String contentUrl;
            if (getCopyLocation().equals(COPYLOCATION_CURRENT))
            {
               contentUrl = (String)node.getProperties().get("contentUrl");
            }
            // or specify a specific file as the content instead
            else
            {
               // add the content to a repo temp writer location
               // we can then retrieve the URL to the content to to be set on the node during checkin
               ContentWriter writer = this.contentService.getWriter(node.getNodeRef());
               writer.putContent(this.file);
               contentUrl = writer.getContentUrl();
            }
            
            if (contentUrl == null || contentUrl.length() == 0)
            {
               throw new IllegalStateException("Content URL is empty for specified working copy content node!");
            }
            
            // add version history text to props
            Map<String, Serializable> props = new HashMap<String, Serializable>(1, 1.0f);
            props.put(Version.PROP_DESCRIPTION, this.versionNotes);
            NodeRef originalDoc = this.versionOperationsService.checkin(
                  node.getNodeRef(),
                  props,
                  contentUrl, 
                  this.keepCheckedOut);
            
            // commit the transaction
            tx.commit();
            
            // clear action context
            setDocument(null);
            clearUpload();
            
            outcome = "browse";
         }
         catch (Throwable err)
         {
            // rollback the transaction
            try { if (tx != null) {tx.rollback();} } catch (Exception tex) {}
            Utils.addErrorMessage("Unable to check-in Content Node due to system error: " + err.getMessage(), err);
         }
      }
      else
      {
         logger.warn("WARNING: checkinFileOK called without a current Document!");
      }
      
      return outcome;
   }
   
   /**
    * Action called upon completion of the Update File page
    */
   public String updateFileOK()
   {
      String outcome = null;
      
      UserTransaction tx = null;
      
      // NOTE: for update the document node _is_ the working document!
      Node node = getDocument();
      if (node != null && this.getFileName() != null)
      {
         try
         {
            tx = Repository.getUserTransaction(FacesContext.getCurrentInstance());
            tx.begin();
            
            if (logger.isDebugEnabled())
               logger.debug("Trying to update content node Id: " + node.getId());
            
            // get an updating writer that we can use to modify the content on the current node
            ContentWriter writer = this.contentService.getUpdatingWriter(node.getNodeRef());
            writer.putContent(this.file);
            
            // commit the transaction
            tx.commit();
            
            // clear action context
            setDocument(null);
            clearUpload();
            
            outcome = "browse";
         }
         catch (Throwable err)
         {
            // rollback the transaction
            try { if (tx != null) {tx.rollback();} } catch (Exception tex) {}
            Utils.addErrorMessage("Unable to update Content Node due to system error: " + err.getMessage(), err);
         }
      }
      else
      {
         logger.warn("WARNING: updateFileOK called without a current Document!");
      }
      
      return outcome;
   }
   
   /**
    * Deals with the cancel button being pressed on the check in file page
    */
   public String cancel()
   {
      // reset the state
      clearUpload();
      
      return "browse";
   }
   
   /**
    * Clear form state and upload file bean
    */
   private void clearUpload()
   {
      // delete the temporary file we uploaded earlier
      if (this.file != null)
      {
         this.file.delete();
      }
      
      this.file = null;
      this.fileName = null;
      this.keepCheckedOut = false;
      this.copyLocation = COPYLOCATION_CURRENT;
      this.versionNotes = "";
      this.selectedSpaceId = null;
      
      // remove the file upload bean from the session
      FacesContext ctx = FacesContext.getCurrentInstance();
      ctx.getExternalContext().getSessionMap().remove(FileUploadBean.FILE_UPLOAD_BEAN_NAME);
   }
   
   
   // ------------------------------------------------------------------------------
   // Private data
   
   private static Logger logger = Logger.getLogger(CheckinCheckoutBean.class);
   
   //private static final String WORKING_COPY = " (working copy)";
   
   /** constants for copy location selection */
   private static final String COPYLOCATION_CURRENT = "current";
   private static final String COPYLOCATION_OTHER   = "other";
   
   /** The current document */
   private Node document;
   
   /** The working copy of the document we are checking out */
   private Node workingDocument;
   
   /** Content of the document used for in-line editing */
   private String documentContent;
   
   /** Content of the document returned from in-line editing */
   private String editorOutput;
   
   /** transient form and upload properties */
   private File file;
   private String fileName;
   private boolean keepCheckedOut = false;
   private String copyLocation = COPYLOCATION_CURRENT;
   private String versionNotes = "";
   private String selectedSpaceId = null;
   
   /** The BrowseBean to be used by the bean */
   private BrowseBean browseBean;
   
   /** The NodeService to be used by the bean */
   private NodeService nodeService;
   
   /** The VersionOperationsService to be used by the bean */
   private CheckOutCheckInService versionOperationsService;
   
   /** The ContentService to be used by the bean */
   private ContentService contentService;
}
