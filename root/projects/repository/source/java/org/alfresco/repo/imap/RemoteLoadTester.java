package org.alfresco.repo.imap;

import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeMultipart;

import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sun.mail.util.BASE64DecoderStream;

public class RemoteLoadTester extends TestCase
{

    private Log logger = LogFactory.getLog(RemoteLoadTester.class);

    private static final String USER_NAME = "admin";
    private static final String USER_PASSWORD = "admin";
    private static final String TEST_FOLDER_NAME = "test_imap1000";

    @Override
    public void setUp() throws Exception
    {
    }

    public void tearDown() throws Exception
    {

    }


    public void testMailbox()
    {
        logger.info("Getting folder...");
        long t = System.currentTimeMillis();
        
        String host = "localhost";

        // Create empty properties
        Properties props = new Properties();
        props.setProperty("mail.imap.partialfetch", "false");

        // Get session
        Session session = Session.getDefaultInstance(props, null);

        Store store = null;
        Folder folder = null;
        try
        {
            // Get the store
            store = session.getStore("imap");
            store.connect(host, USER_NAME, USER_PASSWORD);

            // Get folder
            folder = store.getFolder(TEST_FOLDER_NAME);
            folder.open(Folder.READ_ONLY);

            // Get directory
            Message message[] = folder.getMessages();

            for (int i = 0, n = message.length; i < n; i++)
            {
                message[i].getAllHeaders();
                
                Address[] from = message[i].getFrom();
                System.out.print(i + ": ");
                if (from != null)
                {
                    System.out.print(message[i].getFrom()[0] + "\t");
                }
                System.out.println(message[i].getSubject());

                Object content = message[i].getContent();
                if (content instanceof MimeMultipart)
                {
                    for (int j = 0, m = ((MimeMultipart)content).getCount(); j < m; j++)
                    {
                        BodyPart part = ((MimeMultipart)content).getBodyPart(j);
                        Object partContent = part.getContent();

                        if (partContent instanceof String)
                        {
                            String body = (String)partContent;
                        }
                        else if (partContent instanceof FilterInputStream)
                        {
                            FilterInputStream fis = (FilterInputStream)partContent;
                            BufferedInputStream bis = new BufferedInputStream(fis);

                           /* while (bis.available() > 0) 
                            {
                               bis.read();
                            }*/
                            byte[] bytes = new byte[524288];
                            while (bis.read(bytes) != -1)
                            {
                            }
                            bis.close();
                            fis.close();
                        }
                    }
                }
            
                int nn = 0;
            
            }

            
            
            t = System.currentTimeMillis() - t;
            logger.info("Time: " + t + " ms (" + t/1000 + " s)");
            logger.info("Length: " + message.length);

        }
        catch (Exception e)
        {
            logger.error(e.getMessage(), e);
            fail(e.getMessage());
        }
        finally
        {
            // Close connection
            try
            {
                if (folder != null)
                {
                    folder.close(false);
                }
            }
            catch (MessagingException e)
            {
                logger.error(e.getMessage(), e);
                fail(e.getMessage());
            }
            try
            {
                if (store != null)
                {
                    store.close();
                }
            }
            catch (MessagingException e)
            {
                logger.error(e.getMessage(), e);
                fail(e.getMessage());
            }
        }

    }


}
