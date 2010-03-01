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
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>. */
package org.alfresco.filesys.repo.desk;

import java.util.Date;

import org.alfresco.filesys.alfresco.DesktopAction;
import org.alfresco.filesys.alfresco.DesktopParams;
import org.alfresco.filesys.alfresco.DesktopResponse;

/**
 * Echo Desktop Action Class
 * 
 * <p>Simple desktop action that echoes back the received string.
 * 
 * @author gkspencer
 */
public class EchoDesktopAction extends DesktopAction {

	/**
	 * Class constructor
	 */
	public EchoDesktopAction()
	{
		super( 0, PreConfirmAction);
	}
	
	@Override
	public String getConfirmationString() {
		return "Run echo action";
	}

	@Override
	public DesktopResponse runAction(DesktopParams params) {

		// Return a text message
		
		return new DesktopResponse(StsSuccess, "Test message from echo action at " + new Date()); 
	}
}
