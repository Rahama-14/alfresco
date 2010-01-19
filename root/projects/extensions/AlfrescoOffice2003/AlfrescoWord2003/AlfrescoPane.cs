using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.IO;
using System.Net;
using System.Runtime.InteropServices;
using System.Security.Permissions;
using System.Text;
using System.Windows.Forms;
using Microsoft.VisualStudio.Tools.Applications.Runtime;
using Word = Microsoft.Office.Interop.Word;
using Office = Microsoft.Office.Core;

namespace AlfrescoWord2003
{
   [PermissionSet(SecurityAction.Demand, Name = "FullTrust")]
   [System.Runtime.InteropServices.ComVisibleAttribute(true)]
   public partial class AlfrescoPane : Form
   {
      private const int INTERNET_OPTION_END_BROWSER_SESSION = 42;

      private Word.Application m_WordApplication;
      private ServerDetails m_ServerDetails;
      private string m_TemplateRoot = "";
      private bool m_ShowPaneOnActivate = false;
      private bool m_ManuallyHidden = false;
      private bool m_LastWebPageSuccessful = true;
      private bool m_DebugMode = false;
      private bool m_ClearSession = false;
      private bool m_SupportsDocx = false;

      // Win32 SDK functions
      [DllImport("user32.dll")]
      public static extern int SetFocus(int hWnd);

      [DllImport("wininet.dll")]
      public static extern int InternetSetOption(int hInternet, int lOption, string sBuffer, int lBufferLength);

      public Word.Application WordApplication
      {
         set
         {
            m_WordApplication = value;
            m_SupportsDocx = (Convert.ToDecimal(m_WordApplication.Version, System.Globalization.CultureInfo.InvariantCulture) > 11);
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
         bool bHaveDocument = (m_WordApplication.Documents.Count > 0);

         try
         {
            if (bHaveDocument)
            {
               this.showDocumentDetails();
               if (!m_ManuallyHidden)
               {
                  this.Show();
               }
            }
            else
            {
               m_ServerDetails.DocumentPath = "";
               // this.showHome(false);
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
               m_WordApplication.Activate();
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
         m_ServerDetails.DocumentPath = "";
         if (m_WordApplication.Documents.Count == 1)
         {
            // Closing last document, but might also be closing app
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
            m_WordApplication.Activate();
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
            string theURI = string.Format(@"{0}{1}myAlfresco?p=&e=doc", m_ServerDetails.WebClientURL, m_TemplateRoot);
            // We don't prompt the user if the document is closing
            string strAuthTicket = m_ServerDetails.getAuthenticationTicket(!isClosing);
            /**
             * Long ticket fix - the encoded ticket is 2426 characters long, therefore has to be sent
             * as an HTTP header to avoid the IE6 and IE7 2048 character limit on a GET URL
             */
            string strAuthHeader = "";
            if ((strAuthTicket != "") && (strAuthTicket != "ntlm"))
            {
               if ((Uri.EscapeDataString(strAuthTicket).Length + theURI.Length)  > 1024)
               {
                  strAuthHeader = "ticket: " + strAuthTicket;
               }
               else
               {
                  theURI += "&ticket=" + strAuthTicket;
               }
            }
            if ((strAuthTicket == "") && !isClosing)
            {
               PanelMode = PanelModes.Configuration;
               return;
            }

            if (m_ClearSession)
            {
               m_ClearSession = false;
               InternetSetOption(0, INTERNET_OPTION_END_BROWSER_SESSION, null, 0);
            }

            if (!isClosing || (strAuthTicket != ""))
            {
               webBrowser.ObjectForScripting = this;
               UriBuilder uriBuilder = new UriBuilder(theURI);
               webBrowser.Navigate(uriBuilder.Uri.AbsoluteUri, null, null, strAuthHeader);
               PanelMode = PanelModes.WebBrowser;
            }
         }
      }

      public void showDocumentDetails()
      {
         string relativePath = "";

         // Do we have a valid web server address?
         if (m_ServerDetails.WebClientURL == "")
         {
            // No - show the configuration UI
            PanelMode = PanelModes.Configuration;
         }
         else
         {
            m_ServerDetails.DocumentPath = m_WordApplication.ActiveDocument.FullName;
            relativePath = m_ServerDetails.DocumentPath;

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
            string theURI = string.Format(@"{0}{1}documentDetails?p={2}&e=doc", m_ServerDetails.WebClientURL, m_TemplateRoot, relativePath);
            string strAuthTicket = m_ServerDetails.getAuthenticationTicket(true);
            /**
             * Long ticket fix - the encoded ticket is 2426 characters long, therefore has to be sent
             * as an HTTP header to avoid the IE6 and IE7 2048 character limit on a GET URL
             */
            string strAuthHeader = "";
            if ((strAuthTicket != "") && (strAuthTicket != "ntlm"))
            {
               if ((Uri.EscapeDataString(strAuthTicket).Length + theURI.Length) > 1024)
               {
                  strAuthHeader = "ticket: " + strAuthTicket;
               }
               else
               {
                  theURI += "&ticket=" + strAuthTicket;
               }
            }
            if (strAuthTicket == "")
            {
               PanelMode = PanelModes.Configuration;
               return;
            }

            if (m_ClearSession)
            {
               m_ClearSession = false;
               InternetSetOption(0, INTERNET_OPTION_END_BROWSER_SESSION, null, 0);
            }
            
            webBrowser.ObjectForScripting = this;
            UriBuilder uriBuilder = new UriBuilder(theURI);
            webBrowser.Navigate(uriBuilder.Uri.AbsoluteUri, null, null, strAuthHeader);
            PanelMode = PanelModes.WebBrowser;
         }
      }

      public void openDocument(string documentPath)
      {
         object missingValue = Type.Missing;
         // WebDAV or CIFS?
         string strFullPath = m_ServerDetails.getFullPath(documentPath, "", false);
         object file = strFullPath;
         try
         {
            if (m_DebugMode)
            {
               MessageBox.Show("Document path=\n" + strFullPath, "Open Document", MessageBoxButtons.OK, MessageBoxIcon.Information);
            }
            Word.Document doc = m_WordApplication.Documents.Open(
               ref file, ref missingValue, ref missingValue, ref missingValue, ref missingValue, ref missingValue,
               ref missingValue, ref missingValue, ref missingValue, ref missingValue,
               ref missingValue, ref missingValue, ref missingValue, ref missingValue, ref missingValue, ref missingValue);
            
         }
         catch (Exception e)
         {
            MessageBox.Show(Properties.Resources.UnableToOpen + ": " + e.Message, Properties.Resources.MessageBoxTitle, MessageBoxButtons.OK, MessageBoxIcon.Error);
         }
      }

      public void compareDocument(string relativeURL)
      {
         object missingValue = Type.Missing;

         if (relativeURL.StartsWith("/"))
         {
            relativeURL = relativeURL.Substring(1);
         }

         if (m_DebugMode)
         {
            MessageBox.Show("Document path=\n" + m_ServerDetails.WebClientURL + relativeURL, "Compare Document", MessageBoxButtons.OK, MessageBoxIcon.Information);
         }
         m_WordApplication.ActiveDocument.Compare(
            m_ServerDetails.WebClientURL + relativeURL, ref missingValue, ref missingValue, ref missingValue, ref missingValue,
            ref missingValue, ref missingValue, ref missingValue);
      }

      public void insertDocument(string relativePath, string nodeRef)
      {
         object missingValue = Type.Missing;
         object trueValue = true;
         object falseValue = false;

         try
         {
            // Create a new document if no document currently open
            if (m_WordApplication.Selection == null)
            {
               m_WordApplication.Documents.Add(ref missingValue, ref missingValue, ref missingValue, ref missingValue);
            }

            object range = m_WordApplication.Selection.Range;

            // WebDAV or CIFS?
            string strFullPath = m_ServerDetails.getFullPath(relativePath, m_WordApplication.ActiveDocument.FullName, true);
            string strExtn = Path.GetExtension(relativePath).ToLower();
            string fileName = Path.GetFileName(relativePath);
            string nativeExtn = ".doc" + (m_SupportsDocx ? "x" : "");

            // If we're using WebDAV, then download file locally before inserting
            if (strFullPath.StartsWith("http"))
            {
               // Need to unescape the the original path to get the filename
               fileName = Path.GetFileName(Uri.UnescapeDataString(relativePath));
               string strTempFile = Path.GetTempPath() + fileName;
               if (File.Exists(strTempFile))
               {
                  try
                  {
                     File.Delete(strTempFile);
                  }
                  catch (Exception e)
                  {
                     strTempFile = Path.GetTempFileName();
                  }
               }
               WebClient fileReader = new WebClient();
               string url = m_ServerDetails.WebClientURL + "download/direct/" + nodeRef.Replace(":/", "") + "/" + Path.GetFileName(relativePath);
               fileReader.Headers.Add("Cookie: " + webBrowser.Document.Cookie);
               fileReader.DownloadFile(url, strTempFile);
               strFullPath = strTempFile;
            }

            if (".bmp .gif .jpg .jpeg .png".IndexOf(strExtn) != -1)
            {
               if (m_DebugMode)
               {
                  MessageBox.Show("Image path=\n" + strFullPath, "Insert Image", MessageBoxButtons.OK, MessageBoxIcon.Information);
               }
               m_WordApplication.ActiveDocument.InlineShapes.AddPicture(strFullPath, ref falseValue, ref trueValue, ref range);
            }
            else if (nativeExtn.IndexOf(strExtn) != -1)
            {
               if (m_DebugMode)
               {
                  MessageBox.Show("Document path=\n" + strFullPath, "Insert Document", MessageBoxButtons.OK, MessageBoxIcon.Information);
               }
               m_WordApplication.Selection.InsertFile(strFullPath, ref missingValue, ref trueValue, ref missingValue, ref missingValue);
            }
            else
            {
               object filename = strFullPath;
               object iconFilename = Type.Missing;
               object iconIndex = Type.Missing;
               object iconLabel = fileName;
               string defaultIcon = Util.DefaultIcon(strExtn);
               if (defaultIcon.Contains(","))
               {
                  string[] iconData = defaultIcon.Split(new char[] { ',' });
                  iconFilename = iconData[0];
                  iconIndex = iconData[1];
               }
               if (m_DebugMode)
               {
                  MessageBox.Show("Object path=\n" + strFullPath, "Insert OLE Object", MessageBoxButtons.OK, MessageBoxIcon.Information);
               }
               m_WordApplication.ActiveDocument.InlineShapes.AddOLEObject(ref missingValue, ref filename, ref falseValue, ref trueValue,
                  ref iconFilename, ref iconIndex, ref iconLabel, ref range);
            }
         }
         catch (Exception e)
         {
            MessageBox.Show(Properties.Resources.UnableToInsert + ": " + e.Message, Properties.Resources.MessageBoxTitle, MessageBoxButtons.OK, MessageBoxIcon.Error);
         }
      }

      public bool docHasExtension()
      {
         string name = m_WordApplication.ActiveDocument.Name;
         return (name.EndsWith(".doc") || name.EndsWith(".docx"));
      }

      public void saveToAlfresco(string documentPath)
      {
         saveToAlfrescoAs(documentPath, m_WordApplication.ActiveDocument.Name);
      }

      public void saveToAlfrescoAs(string relativeDirectory, string documentName)
      {
         object missingValue = Type.Missing;

         string currentDocPath = m_WordApplication.ActiveDocument.FullName;
         // Ensure last separator is present
         if (relativeDirectory == null)
         {
            relativeDirectory = "/";
         }
         else if (!relativeDirectory.EndsWith("/"))
         {
            relativeDirectory += "/";
         }

         // Have the correct file extension already?
         if (!documentName.EndsWith(".doc"))
         {
            documentName += ".doc";
         }
         // Add the Word filename
         relativeDirectory += documentName;

         // CIFS or WebDAV path?
         string savePath = m_ServerDetails.getFullPath(relativeDirectory, currentDocPath, false);

         // Box into object - Word requirement
         object file = savePath;
         try
         {
            if (m_DebugMode)
            {
               MessageBox.Show("Save path=\n" + savePath, "Save Document", MessageBoxButtons.OK, MessageBoxIcon.Information);
            }
            m_WordApplication.ActiveDocument.SaveAs(
               ref file, ref missingValue, ref missingValue, ref missingValue, ref missingValue, ref missingValue,
               ref missingValue, ref missingValue, ref missingValue, ref missingValue,
               ref missingValue, ref missingValue, ref missingValue, ref missingValue, ref missingValue, ref missingValue);

            this.OnDocumentChanged();
         }
         catch (Exception e)
         {
            MessageBox.Show(Properties.Resources.UnableToSave + ": " + e.Message, Properties.Resources.MessageBoxTitle, MessageBoxButtons.OK, MessageBoxIcon.Error);
         }
      }

      public void showSettingsPanel()
      {
         PanelMode = PanelModes.Configuration;
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
      private string m_WebDAVURL = "";

      private void LoadSettings()
      {
         m_ServerDetails.LoadFromRegistry();
         txtWebClientURL.Text = m_ServerDetails.WebClientURL;
         m_WebDAVURL = m_ServerDetails.WebDAVURL;
         txtCIFSServer.Text = m_ServerDetails.CIFSServer;
         chkUseCIFS.Checked = (m_ServerDetails.CIFSServer != "");
         chkUseCIFS_CheckedChanged(null, null);
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
         m_DebugMode = (Control.ModifierKeys == (Keys.Control | Keys.Shift));

         if (m_SettingsChanged)
         {
            m_ServerDetails.WebClientURL = txtWebClientURL.Text;
            m_ServerDetails.WebDAVURL = m_WebDAVURL;
            m_ServerDetails.CIFSServer = chkUseCIFS.Checked ? txtCIFSServer.Text : "";
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
            m_ClearSession = true;
         }

         this.OnDocumentChanged();
      }

      private void btnDetailsCancel_Click(object sender, EventArgs e)
      {
         LoadSettings();
      }

      private void txtWebClientURL_TextChanged(object sender, EventArgs e)
      {
         m_SettingsChanged = true;
         
         // Build WebDAV URL
         try
         {
            string strWebDAV = txtWebClientURL.Text;
            if (!strWebDAV.EndsWith("/"))
            {
               strWebDAV += "/";
            }
            m_WebDAVURL = strWebDAV + "webdav/";
         }
         catch
         {
         }

         // Build autocomplete string for the CIFS textbox
         try
         {
            Uri clientUri = new Uri(txtWebClientURL.Text);
            string strCIFS = "\\\\" + clientUri.Host + "a\\alfresco\\";
            txtCIFSServer.AutoCompleteCustomSource.Clear();
            txtCIFSServer.AutoCompleteCustomSource.Add(strCIFS);
         }
         catch
         {
         }
      }

      private void chkUseCIFS_CheckedChanged(object sender, EventArgs e)
      {
         m_SettingsChanged = true;
         txtCIFSServer.Enabled = chkUseCIFS.Checked;
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

            bool bLastPageOK = m_LastWebPageSuccessful;
            m_LastWebPageSuccessful = false;
            if (bLastPageOK)
            {
               showHome(true);
            }
         }
         else
         {
            if (!m_LastWebPageSuccessful)
            {
               m_LastWebPageSuccessful = true;
               this.OnDocumentChanged();
            }
         }
      }
   }
}