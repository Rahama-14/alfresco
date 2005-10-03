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
package org.alfresco.example.webservice.sample;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.rpc.ServiceException;

import org.alfresco.example.webservice.authentication.AuthenticationResult;
import org.alfresco.example.webservice.authentication.AuthenticationServiceLocator;
import org.alfresco.example.webservice.authentication.AuthenticationServiceSoapBindingStub;
import org.alfresco.example.webservice.content.ContentServiceLocator;
import org.alfresco.example.webservice.content.ContentServiceSoapBindingStub;
import org.alfresco.example.webservice.content.ReadResult;
import org.alfresco.example.webservice.types.Content;
import org.alfresco.example.webservice.types.ContentFormat;
import org.alfresco.example.webservice.types.ParentReference;
import org.alfresco.example.webservice.types.Reference;
import org.apache.axis.EngineConfiguration;
import org.apache.axis.configuration.FileProvider;

/**
 * Web service sample 3
 * <p>
 * This web service sample shows how new content can be added to the repository and how content 
 * can be read and updated via the web service API.
 * 
 * @author Roy Wetherall
 */
public class WebServiceSample3
{
    /** Content strings used in the sample */
    private static final String INITIAL_CONTENT = "This is some new content that I am adding to the repository";
    private static final String UPDATED_CONTENT = "This is the updated content";

    /** The type of the association we are creating to the new content */
    private static final String ASSOC_CONTAINS = "{http://www.alfresco.org/model/content/1.0}contains";
    
    /**
     * Main function
     */
    public static void main(String[] args) throws Exception
    {
        AuthenticationServiceSoapBindingStub authenticationService = (AuthenticationServiceSoapBindingStub)new AuthenticationServiceLocator().getAuthenticationService();
        
        // Start the session
        AuthenticationResult result = authenticationService.startSession(WebServiceSample1.USERNAME, WebServiceSample1.PASSWORD);
        WebServiceSample1.currentTicket = result.getTicket();
        
        // Get the content service
        ContentServiceSoapBindingStub contentService = getContentWebService();        
        
        // Create new content in the respository
        Reference newContentReference = createNewContent(contentService, INITIAL_CONTENT);
        
        // Read the newly added content from the respository
        ReadResult readResult = contentService.read(newContentReference);
        
        // Get the content from the download servlet using the URL and display it
        System.out.println("The newly added content is:");
        System.out.println(getContentAsString(WebServiceSample1.currentTicket, readResult.getUrl()));
        
        // Update the content with something new
        contentService.write(newContentReference, UPDATED_CONTENT.getBytes());
        
        // Now output the updated content
        ReadResult readResult2 = contentService.read(newContentReference);
        System.out.println("The updated content is:");
        System.out.println(getContentAsString(WebServiceSample1.currentTicket, readResult2.getUrl()));
        
        // End the session
        authenticationService.endSession();
    }
    
    /**
     * Get the content web service
     * 
     * @return                      content web service 
     * @throws ServiceException
     */
    public static ContentServiceSoapBindingStub getContentWebService() throws ServiceException
    {
        // Create the content service, adding the WS security header information
        EngineConfiguration config = new FileProvider(new ByteArrayInputStream(WebServiceSample1.WS_SECURITY_INFO.getBytes()));
        ContentServiceLocator contentServiceLocator = new ContentServiceLocator(config);        
        ContentServiceSoapBindingStub contentService = (ContentServiceSoapBindingStub)contentServiceLocator.getContentService();
        return contentService;
    }
    
    /**
     * Helper method to create new content.
     *  
     * @param contentService    the content web service
     * @param content           the content itself
     * @return                  a reference to the created content node
     * @throws Exception        
     */
    public static Reference createNewContent(ContentServiceSoapBindingStub contentService, String content) 
        throws Exception
    {
        // First we'll use the previous sample to get hold of a reference to a space that we can create the content within
        Reference reference = WebServiceSample2.executeSearch();                
        
        // Create a parent reference, this contains information about the association we are createing to the new content and the
        // parent of the new content (the space retrived from the search)
        ParentReference parentReference = new ParentReference(ASSOC_CONTAINS, ASSOC_CONTAINS);
        parentReference.setStore(reference.getStore());
        parentReference.setUuid(reference.getUuid());
        
        // Define the content format for the content we are adding
        ContentFormat contentFormat = new ContentFormat("text/plain", "UTF-8");
        
        // Add the content to the repository
        Content newContent = contentService.create(parentReference, "myDocument.txt", contentFormat, content.getBytes());
        
        // Get a reference to the newly created content
        return newContent.getReference();
    }
    
    /**
     * This method gets the content from the download servlet for a given URL and returns it as a string.
     * 
     * @param ticket        the current ticket
     * @param strUrl        the content URL
     * @return              the content as a string
     * @throws Exception
     */
    public static String getContentAsString(String ticket, String strUrl) throws Exception
    {
        // Add the ticket to the url
        strUrl += "?ticket=" + ticket;
        
        // Connect to donwload servlet
        StringBuilder readContent = new StringBuilder();
        URL url = new URL(strUrl);
        URLConnection conn = url.openConnection();
        InputStream is = conn.getInputStream();
        int read = is.read();
        while (read != -1)
        {
           readContent.append((char)read);
           read = is.read();
        }
        
        // return content as a string
        return readContent.toString();
    }
}
