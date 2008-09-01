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
using Excel = Microsoft.Office.Interop.Excel;
using Office = Microsoft.Office.Core;

namespace AlfrescoExcel2003
{
   [PermissionSet(SecurityAction.Demand, Name = "FullTrust")]
   [System.Runtime.InteropServices.ComVisibleAttribute(true)]
   public partial class AlfrescoPane : Form
   {
      private const int INTERNET_OPTION_END_BROWSER_SESSION = 42;

      private Excel.Application m_ExcelApplication;
      private ServerDetails m_ServerDetails;
      private string m_TemplateRoot = "";
      private bool m_ShowPaneOnActivate = false;
      private bool m_ManuallyHidden = false;
      private bool m_LastWebPageSuccessful = true;
      private bool m_ClearSession = false;

      // Win32 SDK functions
      [DllImport("user32.dll")]
      public static extern bool SetForegroundWindow(int hWnd);

      [DllImport("wininet.dll")]
      public static extern int InternetSetOption(int hInternet, int lOption, string sBuffer, int lBufferLength);

      public Excel.Application ExcelApplication
      {
         set
         {
            m_ExcelApplication = value;
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
         bool bHaveDocument = (m_ExcelApplication.Workbooks.Count > 0);

         try
         {
            if (bHaveDocument)
            {
               this.showDocumentDetails();
            }
            else
            {
               m_ServerDetails.DocumentPath = "";
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
               SetForegroundWindow(m_ExcelApplication.Hwnd);
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
         if (m_ExcelApplication.Workbooks.Count == 1)
         {
            // Closing last spreadsheet, but might also be closing app
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
            SetForegroundWindow(m_ExcelApplication.Hwnd);
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
            string theURI = string.Format(@"{0}{1}myAlfresco?p=&e=xls", m_ServerDetails.WebClientURL, m_TemplateRoot);
            // We don't prompt the user if the document is closing
            string strAuthTicket = m_ServerDetails.getAuthenticationTicket(!isClosing);
            if (strAuthTicket != "")
            {
               theURI += "&ticket=" + Uri.EscapeDataString(strAuthTicket);
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
               webBrowser.Navigate(uriBuilder.Uri.AbsoluteUri);
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
            m_ServerDetails.DocumentPath = m_ExcelApplication.ActiveWorkbook.FullName;
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
            string theURI = string.Format(@"{0}{1}documentDetails?p={2}&e=xls", m_ServerDetails.WebClientURL, m_TemplateRoot, relativePath);
            string strAuthTicket = m_ServerDetails.getAuthenticationTicket(true);
            if (strAuthTicket != "")
            {
               theURI += "&ticket=" + Uri.EscapeDataString(strAuthTicket);
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
            webBrowser.Navigate(uriBuilder.Uri.AbsoluteUri);
            PanelMode = PanelModes.WebBrowser;
         }
      }

      public void openDocument(string documentPath)
      {
         object missingValue = Type.Missing;
         object trueValue = true;
         object falseValue = false;

         // WebDAV or CIFS?
         string strFullPath = m_ServerDetails.getFullPath(documentPath, "");
         try
         {
            Excel.Workbook book = m_ExcelApplication.Workbooks.Open(
               strFullPath, missingValue, missingValue, missingValue, missingValue, missingValue,
               missingValue, missingValue, missingValue, trueValue, trueValue, missingValue,
               missingValue, missingValue, missingValue);
         }
         catch (Exception e)
         {
            MessageBox.Show(Properties.Resources.UnableToOpen + ": " + e.Message, Properties.Resources.MessageBoxTitle, MessageBoxButtons.OK, MessageBoxIcon.Error);
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

         // Create a new document if no document currently open
         if (m_ExcelApplication.ActiveWorkbook == null)
         {
            m_ExcelApplication.Workbooks.Add(missingValue);
         }

         // WebDAV or CIFS?
         string strFullPath = m_ServerDetails.getFullPath(relativePath, m_ExcelApplication.ActiveWorkbook.FullName);
         string strExtn = Path.GetExtension(relativePath).ToLower();

         Excel.Worksheet worksheet = (Excel.Worksheet)m_ExcelApplication.ActiveSheet;
         Excel.Shapes shapes = worksheet.Shapes;
         Excel.Range range = (Excel.Range)m_ExcelApplication.Selection;
         object top = range.Top;
         object left = range.Left;

         if (".bmp .gif .jpg .jpeg .png".IndexOf(strExtn) != -1)
         {
            try
            {
               Excel.Shape picture = shapes.AddPicture(strFullPath, Microsoft.Office.Core.MsoTriState.msoFalse, Microsoft.Office.Core.MsoTriState.msoTrue,
                  1, 2, 3, 4);
               picture.Top = Convert.ToSingle(range.Top);
               picture.Left = Convert.ToSingle(range.Left);
               picture.ScaleWidth(1, Microsoft.Office.Core.MsoTriState.msoTrue, missingValue);
               picture.ScaleHeight(1, Microsoft.Office.Core.MsoTriState.msoTrue, missingValue);
            }
            catch (Exception e)
            {
               MessageBox.Show(e.Message, Properties.Resources.MessageBoxTitle);
            }
         }
         else
         {
            object iconFilename = Type.Missing;
            object iconIndex = Type.Missing;
            object iconLabel = Path.GetFileName(strFullPath);
            string defaultIcon = Util.DefaultIcon(Path.GetExtension(strFullPath));
            if (defaultIcon.Contains(","))
            {
               string[] iconData = defaultIcon.Split(new char[] { ',' });
               iconFilename = iconData[0];
               iconIndex = iconData[1];
            }
            object filename = strFullPath;
            object size = 32;
            Excel.Shape shape = shapes.AddOLEObject(missingValue, filename, falseValue, trueValue,
               iconFilename, iconIndex, iconLabel, left, top, size, size);
         }
      }

      public bool docHasExtension()
      {
         return (m_ExcelApplication.ActiveWorkbook.Name.EndsWith(".xls"));
      }

      public void saveToAlfresco(string documentPath)
      {
         saveToAlfrescoAs(documentPath, m_ExcelApplication.ActiveWorkbook.Name);
      }

      public void saveToAlfrescoAs(string relativeDirectory, string documentName)
      {
         object missingValue = Type.Missing;

         string currentDocPath = m_ExcelApplication.ActiveWorkbook.FullName;
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
         if (!documentName.EndsWith(".xls"))
         {
            documentName += ".xls";
         }
         // Add the Word filename
         relativeDirectory += documentName;

         // CIFS or WebDAV path?
         string savePath = m_ServerDetails.getFullPath(relativeDirectory, currentDocPath);

         // Box into object - Word requirement
         object file = savePath;
         try
         {
            m_ExcelApplication.ActiveWorkbook.SaveAs(
               file, missingValue, missingValue, missingValue, missingValue, missingValue,
               Microsoft.Office.Interop.Excel.XlSaveAsAccessMode.xlNoChange, missingValue, missingValue, missingValue,
               missingValue, missingValue);

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
         if (m_SettingsChanged)
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