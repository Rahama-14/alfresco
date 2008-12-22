/*
 * Copyright (C) 2005-2009 Alfresco Software Limited.
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
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.module.vti.handler.alfresco.v2;

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.EnumSet;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import org.alfresco.model.ContentModel;
import org.alfresco.module.vti.handler.VtiHandlerException;
import org.alfresco.module.vti.handler.alfresco.AbstractAlfrescoMethodHandler;
import org.alfresco.module.vti.handler.alfresco.VtiDocumentHepler;
import org.alfresco.module.vti.handler.alfresco.VtiExceptionUtils;
import org.alfresco.module.vti.handler.alfresco.VtiUtils;
import org.alfresco.module.vti.metadata.dic.DocumentStatus;
import org.alfresco.module.vti.metadata.dic.GetOption;
import org.alfresco.module.vti.metadata.model.DocMetaInfo;
import org.alfresco.module.vti.metadata.model.DocsMetaInfo;
import org.alfresco.module.vti.metadata.model.Document;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Alfresco implementation of MethodHandler and AbstractAlfrescoMethodHandler
 * 
 * @author Mikcael Shavnev
 * @author Dmitry Lazurkin
 */
public class AlfrescoMethodHandler extends AbstractAlfrescoMethodHandler
{
    private final static Log logger = LogFactory.getLog(AlfrescoMethodHandler.class);

    /**
     * @see org.alfresco.module.vti.handler.MethodHandler#getListDocuments(java.lang.String, boolean, boolean, java.lang.String, java.lang.String, boolean, boolean, boolean,
     *      boolean, boolean, boolean, boolean, boolean, java.util.Map, boolean)
     */
    public DocsMetaInfo getListDocuments(String serviceName, boolean listHiddenDocs, boolean listExplorerDocs, String platform, String initialURL, boolean listRecurse,
            boolean listLinkInfo, boolean listFolders, boolean listFiles, boolean listIncludeParent, boolean listDerived, boolean listBorders, boolean validateWelcomeNames,
            Map<String, Object> folderList, boolean listChildWebs)

    {
        // serviceName ignored
        // listHiddenDocs ignored
        // listExplorerDocs ignored

        DocsMetaInfo result = new DocsMetaInfo();
        FileInfo folderFileInfo = getPathHelper().resolvePathFileInfo(serviceName + "/" + initialURL);
        AlfrescoMethodHandler.assertValidFileInfo(folderFileInfo);
        AlfrescoMethodHandler.assertFolder(folderFileInfo);
        FileInfo sourceFileInfo = folderFileInfo;

        if (listFolders)
        {
            for (FileInfo folder : getFileFolderService().listFolders(sourceFileInfo.getNodeRef()))
            {
                result.getFolderMetaInfoList().add(buildDocMetaInfo(folder, folderList));
            }
        }

        if (listFiles)
        {
            for (FileInfo file : getFileFolderService().listFiles(sourceFileInfo.getNodeRef()))
            {
                if (file.isLink() == false)
                {
                    if (getNodeService().hasAspect(file.getNodeRef(), ContentModel.ASPECT_WORKING_COPY) == false)
                    {
                        result.getFileMetaInfoList().add(buildDocMetaInfo(file, folderList));
                    }
                }
            }
        }

        if (listIncludeParent)
        {
            result.getFolderMetaInfoList().add(buildDocMetaInfo(sourceFileInfo, folderList));
        }

        return result;
    }

    /**
     * @see org.alfresco.module.vti.handler.MethodHandler#decomposeURL(java.lang.String, java.lang.String)
     */
    public String[] decomposeURL(String url, String alfrescoContext)
    {
        if (!url.startsWith(alfrescoContext))
            throw new VtiHandlerException(VtiHandlerException.BAD_URL);

        if (url.equalsIgnoreCase(alfrescoContext))
        {
            return new String[] { alfrescoContext, "" };
        }

        String webUrl = "";
        String fileUrl = "";

        String[] splitPath = url.replaceAll(alfrescoContext, "").substring(1).split("/");

        StringBuilder tempWebUrl = new StringBuilder();

        for (int i = splitPath.length; i > 0; i--)
        {
            tempWebUrl.delete(0, tempWebUrl.length());

            for (int j = 0; j < i; j++)
            {
                if (j < i - 1)
                {
                    tempWebUrl.append(splitPath[j] + "/");
                }
                else
                {
                    tempWebUrl.append(splitPath[j]);
                }
            }

            FileInfo fileInfo = getPathHelper().resolvePathFileInfo(tempWebUrl.toString());

            if (fileInfo != null)
            {
                if (getNodeService().getType(fileInfo.getNodeRef()).equals(ContentModel.TYPE_FOLDER))
                {
                    webUrl = alfrescoContext + "/" + tempWebUrl;
                    if (url.replaceAll(webUrl, "").startsWith("/"))
                    {
                        fileUrl = url.replaceAll(webUrl, "").substring(1);
                    }
                    else
                    {
                        fileUrl = url.replaceAll(webUrl, "");
                    }
                    return new String[] { webUrl, fileUrl };
                }
            }
        }
        if (webUrl.equals(""))
        {
            throw new VtiHandlerException(VtiHandlerException.BAD_URL);
        }
        return new String[] { webUrl, fileUrl };
    }

    /**
     * @see org.alfresco.module.vti.handler.MethodHandler#getDocument(java.lang.String, java.lang.String, boolean, java.lang.String, java.util.EnumSet, int)
     */
    public Document getDocument(String serviceName, String documentName, boolean force, String docVersion, EnumSet<GetOption> getOptionSet, int timeout)
    {
        FileInfo fileFileInfo = getPathHelper().resolvePathFileInfo(serviceName + "/" + documentName);
        AlfrescoMethodHandler.assertValidFileInfo(fileFileInfo);
        AlfrescoMethodHandler.assertFile(fileFileInfo);
        FileInfo documentFileInfo = fileFileInfo;

        if (getOptionSet.contains(GetOption.none))
        {
            if (docVersion.length() > 0)
            {
                try
                {
                    VersionHistory versionHistory = getVersionService().getVersionHistory(documentFileInfo.getNodeRef());
                    Version version = versionHistory.getVersion(VtiUtils.toAlfrescoVersionLabel(docVersion));
                    NodeRef versionNodeRef = version.getFrozenStateNodeRef();

                    documentFileInfo = getFileFolderService().getFileInfo(versionNodeRef);
                }
                catch (AccessDeniedException e)
                {
                    throw e;
                }
                catch (RuntimeException e)
                {
                    if (logger.isWarnEnabled())
                    {
                        logger.warn("Version '" + docVersion + "' retrieving exception", e);
                    }

                    // suppress all exceptions and returns the most recent version of the document
                }
            }
        }
        else if (getOptionSet.contains(GetOption.chkoutExclusive) || getOptionSet.contains(GetOption.chkoutNonExclusive))
        {
            try
            {
                // ignore version string parameter
                documentFileInfo = checkout(documentFileInfo, timeout);
            }
            catch (AccessDeniedException e)
            {
                // open document in read-only mode without cheking out (in case if user open content of other user)
                Document document = new Document();
                document.setPath(documentName);
                ContentReader contentReader = getFileFolderService().getReader(documentFileInfo.getNodeRef());
                document.setInputStream(contentReader.getContentInputStream());
                return document;
            }
        }

        Document document = new Document();
        document.setPath(documentName);
        setDocMetaInfo(documentFileInfo, document);
        ContentReader contentReader = getFileFolderService().getReader(documentFileInfo.getNodeRef());
        if (contentReader != null)
        {
            document.setInputStream(contentReader.getContentInputStream());
        }
        else
        {
            // commons-io 1.1 haven't ClosedInputStream
            document.setInputStream(new ByteArrayInputStream(new byte[0]));
        }

        return document;
    }

    /**
     * @see org.alfresco.module.vti.handler.MethodHandler#uncheckOutDocument(java.lang.String, java.lang.String, boolean, java.util.Date, boolean, boolean)
     */
    public DocMetaInfo uncheckOutDocument(String serviceName, String documentName, boolean force, Date timeCheckedOut, boolean rlsshortterm, boolean validateWelcomeNames)
    {
        // force ignored
        // timeCheckedOut ignored

        FileInfo fileFileInfo = getPathHelper().resolvePathFileInfo(serviceName + "/" + documentName);
        AlfrescoMethodHandler.assertValidFileInfo(fileFileInfo);
        AlfrescoMethodHandler.assertFile(fileFileInfo);
        FileInfo documentFileInfo = fileFileInfo;

        DocumentStatus documentStatus = getDocumentHelper().getDocumentStatus(documentFileInfo.getNodeRef());

        // if document isn't checked out then throw exception
        if (VtiDocumentHepler.isCheckedout(documentStatus) == false)
        {
            DocMetaInfo docMetaInfo = new DocMetaInfo(false);
            docMetaInfo.setPath(documentName);
            return docMetaInfo;
        }

        // if document is checked out, but user isn't owner, then throw exception
        if (VtiDocumentHepler.isCheckoutOwner(documentStatus) == false)
        {
            throw new VtiHandlerException(VtiHandlerException.DOC_CHECKED_OUT);
        }

        UserTransaction tx = getTransactionService().getUserTransaction(false);
        try
        {
            tx.begin();

            if (rlsshortterm)
            {
                // try to release short-term checkout
                // if user have long-term checkout then skip releasing short-term checkout
                if (documentStatus.equals(DocumentStatus.LONG_CHECKOUT_OWNER) == false)
                {
                    getLockService().unlock(documentFileInfo.getNodeRef());
                }
            }
            else
            {
                // try to cancel long-term checkout
                NodeRef workingCopyNodeRef = getCheckOutCheckInService().getWorkingCopy(documentFileInfo.getNodeRef());
                getCheckOutCheckInService().cancelCheckout(workingCopyNodeRef);
            }

            tx.commit();
        }
        catch (Throwable e)
        {
            try
            {
                tx.rollback();
            }
            catch (Exception tex)
            {
            }

            if ((e instanceof VtiHandlerException) == false)
            {
                throw new VtiHandlerException(VtiHandlerException.DOC_NOT_CHECKED_OUT);
            }

            throw VtiExceptionUtils.createRuntimeException(e);
        }

        // refresh file info for current document
        documentFileInfo = getFileFolderService().getFileInfo(documentFileInfo.getNodeRef());

        DocMetaInfo docMetaInfo = new DocMetaInfo(false);
        docMetaInfo.setPath(documentName);
        setDocMetaInfo(documentFileInfo, docMetaInfo);

        return docMetaInfo;
    }

    /**
     * @see org.alfresco.module.vti.handler.MethodHandler#existResource(java.lang.String, javax.servlet.http.HttpServletResponse)
     */
    public boolean existResource(String uri, HttpServletResponse response)
    {
        return getPathHelper().resolvePathFileInfo(uri) != null;
    }

    /**
     * Build DocMetaInfo for getListDocuments method
     * 
     * @param fileInfo file info
     * @param folderList dates list
     * @return builded DocMetaInfo
     */
    private DocMetaInfo buildDocMetaInfo(FileInfo fileInfo, Map<String, Object> folderList)
    {
        boolean isModified = false;

        String path = getPathHelper().toUrlPath(fileInfo);

        Date cacheDate = (Date) folderList.get(path);
        Date srcDate = fileInfo.getModifiedDate();

        if (cacheDate == null || srcDate.after(cacheDate))
        {
            isModified = true;
        }

        DocMetaInfo docMetaInfo = new DocMetaInfo(fileInfo.isFolder());

        if (isModified)
        {
            setDocMetaInfo(fileInfo, docMetaInfo);
        }

        docMetaInfo.setPath(path);

        return docMetaInfo;
    }
}
