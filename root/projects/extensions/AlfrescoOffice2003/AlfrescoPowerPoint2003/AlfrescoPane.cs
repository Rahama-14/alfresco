using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.IO;
using System.Runtime.InteropServices;
using System.Security.Permissions;
using System.Text;
using System.Windows.Forms;
using Microsoft.VisualStudio.Tools.Applications.Runtime;
using PowerPoint = Microsoft.Office.Interop.PowerPoint;
using Office = Microsoft.Office.Core;

namespace AlfrescoPowerPoint2003
{
   [PermissionSet(SecurityAction.Demand, Name = "FullTrust")]
   [System.Runtime.InteropServices.ComVisibleAttribute(true)]
   public partial class AlfrescoPane : Form
   {
      private PowerPoint.Application m_PowerPointApplication;
      private ServerDetails m_ServerDetails;
      private string m_TemplateRoot = "";
      private bool m_ShowPaneOnActivate = false;
      private bool m_ManuallyHidden = false;
      private bool m_SuppressCloseEvent = false;

      // Win32 SDK functions
      [DllImport("user32.dll")]
      public static extern int SetFocus(int hWnd);

      public PowerPoint.Application PowerPointApplication
      {
         set
         {
            m_PowerPointApplication = value;
         }
      }

      public string DefaultTemplate
      {
         set
         {
            m_TemplateRoot = value;
         }
      }

      public ServerDetails CurrentServer
      {
         get
         {
            return m_ServerDetails;
         }
      }

      public AlfrescoPane()
      {
         InitializeComponent();

         m_ServerDetails = new ServerDetails();
         LoadSettings();
      }

      ~AlfrescoPane()
      {
         m_ServerDetails.saveWindowPosition(this);
      }

      public void OnDocumentChanged()
      {
         try
         {
            if ((m_PowerPointApplication.ActivePresentation != null) && (m_ServerDetails.getAuthenticationTicket(false) != ""))
            {
               m_ServerDetails.DocumentPath = m_PowerPointApplication.ActivePresentation.FullName;
               this.showDocumentDetails(m_ServerDetails.DocumentPath);
            }
            else
            {
               m_ServerDetails.DocumentPath = "";
               this.showHome(false);
            }
            if (!m_ManuallyHidden)
            {
               this.Show();
            }
         }
         catch
         {
         }
      }

      delegate void OnWindowActivateCallback();

      public void OnWindowActivate()
      {
         if (m_ShowPaneOnActivate && !m_ManuallyHidden)
         {
            if (this.InvokeRequired)
            {
               OnWindowActivateCallback callback = new OnWindowActivateCallback(OnWindowActivate);
               this.Invoke(callback);
            }
            else
            {
               this.Show();
               m_PowerPointApplication.Activate();
            }
         }
      }

      delegate void OnWindowDeactivateCallback();

      public void OnWindowDeactivate()
      {
         if (this.InvokeRequired)
         {
            OnWindowDeactivateCallback callback = new OnWindowDeactivateCallback(OnWindowDeactivate);
            this.Invoke(callback);
         }
         else
         {
            m_ShowPaneOnActivate = true;
            this.Hide();
         }
      }

      public void OnDocumentBeforeClose()
      {
         if (m_SuppressCloseEvent)
         {
            m_SuppressCloseEvent = false;
         }
         else
         {
            m_ServerDetails.DocumentPath = "";
            this.showHome(true);
         }
      }

      public void OnToggleVisible()
      {
         m_ManuallyHidden = !m_ManuallyHidden;

         if (m_ManuallyHidden)
         {
            this.Hide();
         }
         else
         {
            this.Show();
            m_PowerPointApplication.Activate();
         }
      }

      public void showHome(bool isClosing)
      {
         // Do we have a valid web server address?
         if (m_ServerDetails.WebClientURL == "")
         {
            // No - show the configuration UI
            PanelMode = PanelModes.Configuration;
         }
         else
         {
            // Yes - navigate to the home template
            string theURI = string.Format(@"{0}{1}myAlfresco?p=&e=ppt", m_ServerDetails.WebClientURL, m_TemplateRoot);
            // We don't prompt the user if the document is closing
            string strAuthTicket = m_ServerDetails.getAuthenticationTicket(!isClosing);
            if (strAuthTicket != "")
            {
               theURI += "&ticket=" + strAuthTicket;
            }
            if (!isClosing || (strAuthTicket != ""))
            {
               webBrowser.ObjectForScripting = this;
               UriBuilder uriBuilder = new UriBuilder(theURI);
               webBrowser.Navigate(uriBuilder.Uri.AbsoluteUri);
               PanelMode = PanelModes.WebBrowser;
            }
         }
      }

      public void showDocumentDetails(string relativePath)
      {
         // Do we have a valid web server address?
         if (m_ServerDetails.WebClientURL == "")
         {
            // No - show the configuration UI
            PanelMode = PanelModes.Configuration;
         }
         else
         {
            if (relativePath.Length > 0)
            {
               if (!relativePath.StartsWith("/"))
               {
                  relativePath = "/" + relativePath;
               }
               // Strip off any additional parameters
               int paramPos = relativePath.IndexOf("?");
               if (paramPos != -1)
               {
                  relativePath = relativePath.Substring(0, paramPos);
               }
            }
            string theURI = string.Format(@"{0}{1}documentDetails?p={2}&e=ppt", m_ServerDetails.WebClientURL, m_TemplateRoot, relativePath);
            string strAuthTicket = m_ServerDetails.getAuthenticationTicket(true);
            if (strAuthTicket != "")
            {
               theURI += "&ticket=" + strAuthTicket;
            }
            webBrowser.ObjectForScripting = this;
            UriBuilder uriBuilder = new UriBuilder(theURI);
            webBrowser.Navigate(uriBuilder.Uri.AbsoluteUri);
            PanelMode = PanelModes.WebBrowser;
         }
      }

      public void openDocument(string documentPath)
      {
         object missingValue = Type.Missing;
         // WebDAV or CIFS?
         string strFullPath = m_ServerDetails.getFullPath(documentPath, "");
         try
         {
            PowerPoint.Presentation pres = m_PowerPointApplication.Presentations.Open(
               strFullPath, Microsoft.Office.Core.MsoTriState.msoFalse, Microsoft.Office.Core.MsoTriState.msoFalse,
               Microsoft.Office.Core.MsoTriState.msoTrue);
         }
         catch (Exception e)
         {
            MessageBox.Show("Unable to open the presentation from Alfresco: " + e.Message, "Alfresco Problem", MessageBoxButtons.OK, MessageBoxIcon.Error);
         }
      }

      public void compareDocument(string relativeURL)
      {
      }

      public void insertDocument(string relativePath)
      {
         object missingValue = Type.Missing;
         object trueValue = true;
         object falseValue = false;

         try
         {
            // Create a new document if no document currently open
            if (m_PowerPointApplication.ActivePresentation == null)
            {
               m_PowerPointApplication.Presentations.Add(Microsoft.Office.Core.MsoTriState.msoTrue);
            }

            // WebDAV or CIFS?
            string strFullPath = m_ServerDetails.getFullPath(relativePath, m_PowerPointApplication.ActivePresentation.FullName);
            string strExtn = Path.GetExtension(relativePath).ToLower();

            // Store the active pane to restore it later
            PowerPoint.Pane activePane = m_PowerPointApplication.ActiveWindow.ActivePane;
            // Loop through all the panes available looking for the ppViewSlide type
            foreach (PowerPoint.Pane pane in m_PowerPointApplication.ActiveWindow.Panes)
            {
               if (pane.ViewType == PowerPoint.PpViewType.ppViewSlide)
               {
                  pane.Activate();
                  break;
               }
            }

            // Now we can get the current slide index
            PowerPoint.Slide currentSlide = (PowerPoint.Slide)m_PowerPointApplication.ActiveWindow.View.Slide;
            int currentSlideIndex = currentSlide.SlideIndex;
            PowerPoint.Shapes shapes = currentSlide.Shapes;

            // Is another PowerPoint presentation being inserted?
            if (".ppt".IndexOf(strExtn) != -1)
            {
               // Load the presentation in a hidden state
               PowerPoint.Presentation insertPres = m_PowerPointApplication.Presentations.Open(
                  strFullPath, Microsoft.Office.Core.MsoTriState.msoTrue, Microsoft.Office.Core.MsoTriState.msoFalse,
                  Microsoft.Office.Core.MsoTriState.msoFalse);

               if (insertPres != null)
               {
                  // Loop through copy-pasting the slides to be inserted
                  int insertIndex = currentSlideIndex + 1;
                  PowerPoint.Slides slides = m_PowerPointApplication.ActivePresentation.Slides;
                  foreach (PowerPoint.Slide insertSlide in insertPres.Slides)
                  {
                     insertSlide.Copy();
                     slides.Paste(insertIndex++);
                  }
                  // Close the hidden presentation, flagging that we should ignore the close event
                  m_SuppressCloseEvent = true;
                  insertPres.Close();
               }
            }
            else
            {
               // Get default coords for inserting an object
               object top = m_PowerPointApplication.ActiveWindow.Top;
               object left = m_PowerPointApplication.ActiveWindow.Left;

               // Do we have a selection?
               if (m_PowerPointApplication.ActiveWindow.Selection != null)
               {
                  if (m_PowerPointApplication.ActiveWindow.Selection.Type == PowerPoint.PpSelectionType.ppSelectionShapes)
                  {
                     // We can refine the insert location
                     top = m_PowerPointApplication.ActiveWindow.Selection.ShapeRange.Top;
                     left = m_PowerPointApplication.ActiveWindow.Selection.ShapeRange.Left;
                  }
               }

               if (".bmp .gif .jpg .jpeg .png".IndexOf(strExtn) != -1)
               {
                  try
                  {
                     // Inserting a bitmap picture
                     PowerPoint.Shape picture = shapes.AddPicture(strFullPath, Microsoft.Office.Core.MsoTriState.msoFalse, Microsoft.Office.Core.MsoTriState.msoTrue,
                        1, 2, 3, 4);
                     picture.Top = Convert.ToSingle(top);
                     picture.Left = Convert.ToSingle(left);
                     picture.ScaleWidth(1, Microsoft.Office.Core.MsoTriState.msoTrue, Microsoft.Office.Core.MsoScaleFrom.msoScaleFromTopLeft);
                     picture.ScaleHeight(1, Microsoft.Office.Core.MsoTriState.msoTrue, Microsoft.Office.Core.MsoScaleFrom.msoScaleFromTopLeft);
                  }
                  catch (Exception e)
                  {
                     MessageBox.Show(e.Message);
                  }
               }
               else
               {
                  // Inserting a different file type - do it as an OLE object
                  string iconFilename = String.Empty;
                  int iconIndex = 0;
                  string iconLabel = Path.GetFileName(strFullPath);
                  string defaultIcon = Util.DefaultIcon(Path.GetExtension(strFullPath));
                  if (defaultIcon.Contains(","))
                  {
                     string[] iconData = defaultIcon.Split(new char[] { ',' });
                     iconFilename = iconData[0];
                     iconIndex = Convert.ToInt32(iconData[1]);
                  }
                  object filename = strFullPath;
                  float size = 48;
                  shapes.AddOLEObject((float)left, (float)top, size, size, String.Empty, strFullPath, Microsoft.Office.Core.MsoTriState.msoTrue,
                     iconFilename, iconIndex, iconLabel, Microsoft.Office.Core.MsoTriState.msoFalse);
               }
            }

            // Restore the previously-active pane
            if (activePane != null)
            {
               activePane.Activate();
            }
         }
         catch (Exception e)
         {
            MessageBox.Show("Unable to insert content: " + e.Message, "Alfresco Problem", MessageBoxButtons.OK, MessageBoxIcon.Error);
         }
      }

      public bool docHasExtension()
      {
         return (m_PowerPointApplication.ActivePresentation.Name.EndsWith(".ppt"));
      }

      public void saveToAlfresco(string documentPath)
      {
         saveToAlfrescoAs(documentPath, m_PowerPointApplication.ActivePresentation.Name);
      }

      public void saveToAlfrescoAs(string relativeDirectory, string documentName)
      {
         object missingValue = Type.Missing;

         string currentDocPath = m_PowerPointApplication.ActivePresentation.FullName;
         // Ensure last separator is present
         if (!relativeDirectory.EndsWith("/"))
         {
            relativeDirectory += "/";
         }

         // Have the correct file extension already?
         if (!documentName.EndsWith(".ppt"))
         {
            documentName += ".ppt";
         }
         // Add the Word filename
         relativeDirectory += documentName;

         // CIFS or WebDAV path?
         string savePath = m_ServerDetails.getFullPath(relativeDirectory, currentDocPath);

         try
         {
            m_PowerPointApplication.ActivePresentation.SaveAs(
               savePath, Microsoft.Office.Interop.PowerPoint.PpSaveAsFileType.ppSaveAsDefault, Microsoft.Office.Core.MsoTriState.msoTriStateMixed);

            this.OnDocumentChanged();
         }
         catch (Exception e)
         {
            MessageBox.Show("Unable to save the presentation to Alfresco: " + e.Message, "Alfresco Problem", MessageBoxButtons.OK, MessageBoxIcon.Error);
         }
      }

      private enum PanelModes
      {
         WebBrowser,
         Configuration
      }

      private PanelModes PanelMode
      {
         set
         {
            pnlWebBrowser.Visible = (value == PanelModes.WebBrowser);
            pnlConfiguration.Visible = (value == PanelModes.Configuration);
         }
      }

      #region Settings Management
      /// <summary>
      /// Settings Management
      /// </summary>
      private bool m_SettingsChanged = false;

      private void LoadSettings()
      {
         m_ServerDetails.LoadFromRegistry();
         txtWebClientURL.Text = m_ServerDetails.WebClientURL;
         txtWebDAVURL.Text = m_ServerDetails.WebDAVURL;
         txtCIFSServer.Text = m_ServerDetails.CIFSServer;
         if (m_ServerDetails.Username != "")
         {
            txtUsername.Text = m_ServerDetails.Username;
            txtPassword.Text = m_ServerDetails.Password;
            chkRememberAuth.Checked = true;
         }
         else
         {
            txtUsername.Text = "";
            txtPassword.Text = "";
            chkRememberAuth.Checked = false;
         }
         m_SettingsChanged = false;
      }

      private void AlfrescoPane_Load(object sender, EventArgs e)
      {
         m_ServerDetails.loadWindowPosition(this);
      }

      private void AlfrescoPane_FormClosing(object sender, FormClosingEventArgs e)
      {
         m_ServerDetails.saveWindowPosition(this);

         // Override the close box
         if (e.CloseReason == CloseReason.UserClosing)
         {
            e.Cancel = true;
            m_ManuallyHidden = true;
            this.Hide();
         }
      }

      private void btnDetailsOK_Click(object sender, EventArgs e)
      {
         m_ServerDetails.WebClientURL = txtWebClientURL.Text;
         m_ServerDetails.WebDAVURL = txtWebDAVURL.Text;
         m_ServerDetails.CIFSServer = txtCIFSServer.Text;
         if (chkRememberAuth.Checked)
         {
            m_ServerDetails.Username = txtUsername.Text;
            m_ServerDetails.Password = txtPassword.Text;
         }
         else
         {
            m_ServerDetails.Username = "";
            m_ServerDetails.Password = "";
         }

         m_ServerDetails.SaveToRegistry();

         this.OnDocumentChanged();
      }

      private void btnDetailsCancel_Click(object sender, EventArgs e)
      {
         LoadSettings();
      }

      private void txtWebClientURL_TextChanged(object sender, EventArgs e)
      {
         m_SettingsChanged = true;
         
         // Build autocomplete string for the WebDAV textbox
         try
         {
            string strWebDAV = txtWebClientURL.Text;
            if (!strWebDAV.EndsWith("/"))
            {
               strWebDAV += "/";
            }
            strWebDAV += "webdav/";
            txtWebDAVURL.AutoCompleteCustomSource.Clear();
            txtWebDAVURL.AutoCompleteCustomSource.Add(strWebDAV);
         }
         catch
         {
         }

         // Build autocomplete string for the CIFS textbox
         try
         {
            Uri clientUri = new Uri(txtWebClientURL.Text);
            string strCIFS = "\\\\" + clientUri.Host + "_a\\alfresco\\";
            txtCIFSServer.AutoCompleteCustomSource.Clear();
            txtCIFSServer.AutoCompleteCustomSource.Add(strCIFS);
         }
         catch
         {
         }
      }

      private void txtWebDAVURL_TextChanged(object sender, EventArgs e)
      {
         m_SettingsChanged = true;
      }

      private void txtCIFSServer_TextChanged(object sender, EventArgs e)
      {
         m_SettingsChanged = true;
      }

      private void txtUsername_TextChanged(object sender, EventArgs e)
      {
         m_SettingsChanged = true;
      }

      private void txtPassword_TextChanged(object sender, EventArgs e)
      {
         m_SettingsChanged = true;
      }

      private void lnkBackToBrowser_LinkClicked(object sender, LinkLabelLinkClickedEventArgs e)
      {
         PanelMode = PanelModes.WebBrowser;
      }
      private void lnkShowConfiguration_LinkClicked(object sender, LinkLabelLinkClickedEventArgs e)
      {
         PanelMode = PanelModes.Configuration;
      }
      #endregion

      private void webBrowser_Navigated(object sender, WebBrowserNavigatedEventArgs e)
      {
         if (webBrowser.Url.ToString().EndsWith("login.jsp"))
         {
            m_ServerDetails.clearAuthenticationTicket();
            showHome(false);
         }
      }

   }
}