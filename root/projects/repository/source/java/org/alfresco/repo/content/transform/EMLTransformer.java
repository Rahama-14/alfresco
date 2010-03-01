/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.repo.content.transform;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.TransformationOptions;

public class EMLTransformer extends AbstractContentTransformer2
{
    public boolean isTransformable(String sourceMimetype, String targetMimetype, TransformationOptions options)
    {
        if (!MimetypeMap.MIMETYPE_RFC822.equals(sourceMimetype) || !MimetypeMap.MIMETYPE_TEXT_PLAIN.equals(targetMimetype))
        {
            // only support RFC822 -> TEXT
            return false;
        }
        else
        {
            return true;
        }
    }

    @Override
    protected void transformInternal(ContentReader reader, ContentWriter writer, TransformationOptions options) throws Exception
    {
        InputStream is = null;
        try
        {
            is = reader.getContentInputStream();

            MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()), is);

            final StringBuilder sb = new StringBuilder();
            Object content = mimeMessage.getContent();
            if (content instanceof Multipart)
            {
                Multipart multipart = (Multipart) content;
                Part part = multipart.getBodyPart(0);

                if (part.getContent() instanceof Multipart)
                {
                    multipart = (Multipart) part.getContent();
                    for (int i = 0, n = multipart.getCount(); i < n; i++)
                    {
                        part = multipart.getBodyPart(i);
                        if (part.getContentType().contains("text"))
                        {
                            sb.append(part.getContent().toString()).append("\n");

                        }

                    }

                }
                else if (part.getContentType().contains("text"))
                {
                    sb.append(part.getContent().toString());

                }

            }
            else
            {
                sb.append(content.toString());
            }

            writer.putContent(sb.toString());
        }
        finally
        {
            if (is != null)
            {
                try
                {
                    is.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

}
