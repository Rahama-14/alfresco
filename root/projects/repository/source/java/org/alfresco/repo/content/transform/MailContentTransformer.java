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
package org.alfresco.repo.content.transform;

import java.io.IOException;
import java.io.InputStream;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.TransformationOptions;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.poifs.eventfilesystem.POIFSReader;
import org.apache.poi.poifs.eventfilesystem.POIFSReaderEvent;
import org.apache.poi.poifs.eventfilesystem.POIFSReaderListener;
import org.apache.poi.poifs.filesystem.DocumentInputStream;

/**
 * Outlook email msg format to-text transformer.
 * 
 * @author Kevin Roast
 */
public class MailContentTransformer extends AbstractContentTransformer2
{
    private static final Log logger = LogFactory.getLog(MailContentTransformer.class);
    
    private static final String STREAM_PREFIX = "__substg1.0_";
    private static final int STREAM_PREFIX_LENGTH = STREAM_PREFIX.length();
    
    /**
     * Only support MSG to text
     */
    public boolean isTransformable(String sourceMimetype, String targetMimetype, TransformationOptions options)
    {
        if (!MimetypeMap.MIMETYPE_OUTLOOK_MSG.equals(sourceMimetype) ||
            !MimetypeMap.MIMETYPE_TEXT_PLAIN.equals(targetMimetype))
        {
            // only support MSG -> TEXT
            return false;
        }
        else
        {
            return true;
        }
    }

    /**
     * @see org.alfresco.repo.content.transform.AbstractContentTransformer#transformInternal(org.alfresco.service.cmr.repository.ContentReader, org.alfresco.service.cmr.repository.ContentWriter, java.util.Map)
     */
    @Override
    protected void transformInternal(final ContentReader reader, ContentWriter writer, TransformationOptions options)
        throws Exception
    {
        final StringBuilder sb = new StringBuilder();
        POIFSReaderListener readerListener = new POIFSReaderListener()
        {
            public void processPOIFSReaderEvent(final POIFSReaderEvent event)
            {
                try
                {
                    if (event.getName().startsWith(STREAM_PREFIX))
                    {
                        StreamHandler handler = new StreamHandler(event.getName(), event.getStream());
                        String result = handler.process();
                        if (result != null)
                        {
                            sb.append(result);
                        }
                    }
                }
                catch (Exception ex)
                {
                    throw new ContentIOException("Property set stream: " + event.getPath() + event.getName(), ex);
                }
            }
        };
        
        InputStream is = null;
        try
        {
            is = reader.getContentInputStream();
            POIFSReader poiFSReader = new POIFSReader();
            poiFSReader.registerListener(readerListener);
            
            try
            {
                poiFSReader.read(is);
            }
            catch (IOException err)
            {
                // probably not an Outlook format MSG - ignore for now
                if (logger.isWarnEnabled())
                    logger.warn("Unable to extract text from message: " + err.getMessage());
            }
            finally
            {
                // Append the text to the writer
                writer.putContent(sb.toString());
            }
        }
        finally
        {
            if (is != null)
            {
                try { is.close(); } catch (IOException e) {}
            }
        }
    }
    
    private static final String ENCODING_TEXT = "001E";
    private static final String ENCODING_BINARY = "0102";
    private static final String ENCODING_UNICODE = "001F";
    
    private static final String SUBSTG_MESSAGEBODY = "1000";
    
    /**
     * Class to handle stream types. Can process and extract specific streams.
     */
    private class StreamHandler
    {
        StreamHandler(String name, DocumentInputStream stream)
        {
            this.type = name.substring(STREAM_PREFIX_LENGTH, STREAM_PREFIX_LENGTH + 4);
            this.encoding = name.substring(STREAM_PREFIX_LENGTH + 4, STREAM_PREFIX_LENGTH + 8);
            this.stream = stream;
        }
        
        String process()
            throws IOException
        {
            String result = null;
            
            if (SUBSTG_MESSAGEBODY.equals(this.type))
            {
                result = extractText(this.encoding);
            }
            
            return result;
        }
        
        /**
         * Extract the text from the stream based on the encoding
         * 
         * @return String
         * 
         * @throws IOException
         */
        private String extractText(String encoding)
            throws IOException
        {
            byte[] data = new byte[this.stream.available()];
            this.stream.read(data);
            
            if (encoding.equals(ENCODING_TEXT) || encoding.equals(ENCODING_BINARY))
            {
                return new String(data);
            }
            else if (encoding.equals(ENCODING_UNICODE))
            {
                // convert double-byte encoding to single byte for String conversion
                byte[] b = new byte[data.length >> 1];
                for (int i=0; i<b.length; i++)
                {
                    b[i] = data[i << 1];
                }
                return new String(b);
            }
            else
            {
                return new String(data);
            }
        }
        
        private String type;
        private String encoding;
        private DocumentInputStream stream;
    }
}
