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

#include "alfresco\Alfresco.hpp"
#include "util\DataBuffer.h"
#include "util\Exception.h"
#include "util\Integer.h"
#include "util\Debug.h"

#include <WinNetWk.h>

using namespace Alfresco;
using namespace std;

// Define exceptions

EXCEPTION_CLASS(Alfresco, BadInterfaceException);

/**
 * Class constructor
 * 
 * @param path	UNC or mapped drive path to an Alfresco folder on CIFS server
 */
AlfrescoInterface::AlfrescoInterface(String& path) {

	// Clear the file handle

	m_handle  = INVALID_HANDLE_VALUE;

	// Default the protocol version

	m_protocolVersion = 1;

	// Set the working directory path

	setRootPath( path);
}

/**
 * Class destructor
 */
AlfrescoInterface::~AlfrescoInterface() {

	// Close the folder

	if ( m_handle != INVALID_HANDLE_VALUE)
		CloseHandle(m_handle);
}

/**
 * Check if the path is a folder on an Alfresco CIFS server
 *
 * @return bool
 */
bool AlfrescoInterface::isAlfrescoFolder( void) {

	// Check if the handle is valid, if not then the path is not valid

	if ( m_handle == INVALID_HANDLE_VALUE)
		return false;

	// Send a special I/O control to the Alfresco share to check that it is an Alfresco CIFS server

	DataBuffer reqbuf(16);
	DataBuffer respbuf(256);

	reqbuf.putFixedString(IOSignature, IOSignatureLen);

	bool alfFolder = false;

	try {

		// Check if the remote server is an Alfresco CIFS server

		sendIOControl( FSCTL_ALFRESCO_PROBE, reqbuf, respbuf);
		alfFolder = true;

		// Get the protocol version, if available

		respbuf.getInt();	// status
		if ( respbuf.getAvailableLength() >= 4)
			m_protocolVersion = respbuf.getInt();
	}
	catch ( Exception ex) {
	}

	// If the folder is not an Alfresco CIFS folder then close the folder

	if ( alfFolder == false) {
		CloseHandle(m_handle);
		m_handle = INVALID_HANDLE_VALUE;
	}

	// Return the folder status

	return alfFolder;
}

/**
 * Return Alfresco file information for the specified file/folder
 *
 * @param fileName const wchar_t*
 * @return PTR_AlfrescoFileInfo
 */
PTR_AlfrescoFileInfo AlfrescoInterface::getFileInformation( const wchar_t* fileName) {

	// Check if the folder handle is valid

	if ( m_handle == INVALID_HANDLE_VALUE)
		throw BadInterfaceException();

	// Build the file information I/O control request

	DataBuffer reqbuf( 256);
	DataBuffer respbuf( 512);

	reqbuf.putFixedString( IOSignature, IOSignatureLen);
	reqbuf.putString( fileName);

	sendIOControl( FSCTL_ALFRESCO_FILESTS, reqbuf, respbuf);

	// Unpack the request status

	PTR_AlfrescoFileInfo pFileInfo;

	unsigned int reqSts = respbuf.getInt();
	if ( reqSts == StsSuccess) {

		// Create the file information

		pFileInfo.reset( new AlfrescoFileInfo( fileName));

		// Unpack the file details

		pFileInfo->setType( respbuf.getInt());
		if ( pFileInfo->isType() == TypeFile) {

			// Unpack the working copy details

			if ( respbuf.getInt() == True) {
				String workOwner = respbuf.getString();
				String workFrom  = respbuf.getString();

				pFileInfo->setWorkingCopy( workOwner, workFrom);
			}

			// Unpack the lock details

			unsigned int lockType = respbuf.getInt();
			String lockOwner;

			if ( lockType != LockNone)
				lockOwner = respbuf.getString();

			pFileInfo->setLockType( lockType, lockOwner);

			// Unpack the content details

			if ( respbuf.getInt() == True) {
				LONG64 siz = respbuf.getLong();
				String mimeType = respbuf.getString();

				pFileInfo->setContent( siz, mimeType);
			}
		}
	}

	// Return the file information

	return pFileInfo;
}

/**
* Return Alfresco action information for the specified executable
*
* @param fileName const wchar_t*
* @return AlfrescoActionInfo
*/
AlfrescoActionInfo AlfrescoInterface::getActionInformation( const wchar_t* exeName) {

	// Check if the folder handle is valid

	if ( m_handle == INVALID_HANDLE_VALUE)
		throw BadInterfaceException();

	// Build the action information I/O control request

	DataBuffer reqbuf( 256);
	DataBuffer respbuf( 512);

	reqbuf.putFixedString( IOSignature, IOSignatureLen);
	reqbuf.putString( exeName);

	sendIOControl( FSCTL_ALFRESCO_GETACTIONINFO, reqbuf, respbuf);

	// Unpack the request status

	AlfrescoActionInfo actionInfo;

	unsigned int reqSts = respbuf.getInt();
	if ( reqSts == StsSuccess) {

		// Unpack the action name, attributes and pre-action flags

		String name = respbuf.getString();
		unsigned int attr = respbuf.getInt();
		unsigned int preActions = respbuf.getInt();
		String confirmMsg = respbuf.getString();

		// Create the action information

		actionInfo.setName(name);
		actionInfo.setAttributes(attr);
		actionInfo.setPreProcessActions(preActions);
		actionInfo.setPseudoName( exeName);
		actionInfo.setConfirmationMessage( confirmMsg);
	}

	// Return the action information

	return actionInfo;
}

/**
 * Run a desktop action
 *
 * @param action AlfrescoActionInfo&
 * @param params DesktopParams&
 * @return DesktopResponse
 */
DesktopResponse AlfrescoInterface::runAction(AlfrescoActionInfo& action, DesktopParams& params) {

	// Check if the folder handle is valid

	if ( m_handle == INVALID_HANDLE_VALUE)
		throw BadInterfaceException();

	// Build the run action I/O control request

	DataBuffer reqbuf( 1024);
	DataBuffer respbuf( 16 * 1024);

	reqbuf.putFixedString( IOSignature, IOSignatureLen);
	reqbuf.putString( action.getName());
	reqbuf.putInt((unsigned int)params.numberOfTargets());

	for ( unsigned int i = 0; i < params.numberOfTargets(); i++) {

		// Pack the current target details

		const DesktopTarget* pTarget = params.getTarget(i);

		reqbuf.putInt(pTarget->isType());
		reqbuf.putString(pTarget->getTarget());
	}

	// Send the run action request

	sendIOControl( FSCTL_ALFRESCO_RUNACTION, reqbuf, respbuf);

	// Unpack the run action response

	unsigned int actionSts = respbuf.getInt();
	String actionMsg = respbuf.getString();

	// Return the desktop response

	DesktopResponse response(actionSts, actionMsg);
	return response;
}

/**
 * Send an I/O control request to the Alfresco CIFS server, receive and validate the response
 *
 * @param ctlCode const unsigned int
 * @param reqbuf DataBuffer&
 * @param respbuf DataBuffer&
 */
void AlfrescoInterface::sendIOControl( const unsigned int ctlCode, DataBuffer& reqbuf, DataBuffer& respbuf) {

	// Send the I/O control request, receive the response

	DWORD retLen = 0;

	BOOL res = DeviceIoControl( m_handle, ctlCode, reqbuf.getBuffer(), reqbuf.getLength(),
		respbuf.getBuffer(), respbuf.getBufferLength(), &retLen, (LPOVERLAPPED) NULL);

	if ( res) {

		// Validate the reply signature

		if ( retLen >= IOSignatureLen) {
			respbuf.setLength(retLen);
			respbuf.setEndOfBuffer();

			String sig = respbuf.getString(IOSignatureLen, false);

			if ( sig.equals(IOSignature) == false)
				throw Exception( L"Invalid I/O control signature received");
		}
	}
	else
		throw Exception( L"Send I/O control error", Integer::toString( GetLastError()));
}

/**
 * Set the root path to be used as the working directory
 *
 * @param rootPath const wchar_t*
 * @return bool
 */
bool AlfrescoInterface::setRootPath( const wchar_t* rootPath) {

	// Close the existing folder, if valid

	if ( m_handle != INVALID_HANDLE_VALUE)
		CloseHandle(m_handle);

	// Clear the root path

	m_rootPath = "";

	// Check if the path is to a mapped drive

	String path    = rootPath;
	String alfPath = rootPath;

	if ( alfPath.length() >= 2 && alfPath.charAt(1) == ':') {

		// Try and convert the local path to a UNC path

		m_mappedDrive = alfPath.substring(0, 2);
		wchar_t remPath[MAX_PATH];
		DWORD remPathLen = MAX_PATH;

		DWORD sts = WNetGetConnection(( LPWSTR) m_mappedDrive.data(), remPath, &remPathLen);
		if ( sts != NO_ERROR)
			return false;

		// Build the UNC path to the folder

		alfPath = remPath;
		if ( alfPath.endsWith( PathSeperator) == false)
			alfPath.append( PathSeperator);

		m_rootPath = alfPath;

		// Build the full UNC path to the target

		if ( path.length() > 3)
			alfPath.append( path.substring( 3));
	}

	// Save the UNC path

	m_uncPath = alfPath;

	// Check if the UNC path is valid

	if ( m_uncPath.startsWith(UNCPathPrefix)) {

		// Strip any trailing separator from the path

		if ( m_uncPath.endsWith(PathSeperator))
			m_uncPath = m_uncPath.substring(0, m_uncPath.length() - 1);

		// Make sure the path is to a folder

		DWORD attr = GetFileAttributes(m_uncPath);

		if ( attr != INVALID_FILE_ATTRIBUTES && (attr & FILE_ATTRIBUTE_DIRECTORY)) {

			// Open the path

			m_handle = CreateFile(m_uncPath, FILE_READ_DATA, FILE_SHARE_READ | FILE_SHARE_WRITE, NULL, OPEN_EXISTING, FILE_FLAG_BACKUP_SEMANTICS, NULL);

			if ( m_handle == INVALID_HANDLE_VALUE) {
				
				// DEBUG

				if ( HAS_DEBUG)
					DBGOUT_TS << "%% Error opening folder " << m_uncPath << ", error " << GetLastError() << endl;

				// Error, failed to open folder on Alfresco CIFS share

				return false;
			}
		}

		// Set the root path

		if ( m_rootPath.length() == 0) {
			int pos = m_uncPath.indexOf( PathSeperator, 2);
			if ( pos != -1) {
				pos = m_uncPath.indexOf( PathSeperator, pos + 1);
				if ( pos == -1)
					m_rootPath = m_uncPath;
				else
					m_rootPath = m_uncPath.substring(0, pos);
			}
		}
	}

	// Return the folder status

	return isAlfrescoFolder();
}

/**
 * Class constructor
 *
 * @param fileName const wchar_t*
 */
AlfrescoFileInfo::AlfrescoFileInfo(const wchar_t* fileName) {
	m_name = fileName;

	m_workingCopy = false;
	m_lockType = LockNone;

	m_hasContent = false;
	m_contentLen = 0L;
}

/**
 * Set the working copy owner and copied from document path
 *
 * @param owner const wchar_t*
 * @param copiedFrom const wchar_t*
 */
void AlfrescoFileInfo::setWorkingCopy( const wchar_t* owner, const wchar_t* copiedFrom) {
	m_workingCopy = false;
	m_workOwner   = L"";
	m_copiedFrom  = L"";

	if ( owner != NULL) {
		m_workingCopy = true;
		m_workOwner = owner;
		if ( copiedFrom != NULL)
			m_copiedFrom = copiedFrom;
	}
}

/**
 * Set the lock type and owner
 *
 * @param typ unsigned int
 * @param owner const wchar_t*
 */
void AlfrescoFileInfo::setLockType( unsigned int typ, const wchar_t* owner) {
	m_lockType  = typ;
	m_lockOwner = owner;
}

/**
* Set the lock type and owner
*
* @param siz LONG64
* @param mimeType const wchar_t*
*/
void AlfrescoFileInfo::setContent( LONG64 siz, const wchar_t* mimeType) {
	m_hasContent  = true;
	m_contentLen  = siz;
	m_contentMimeType = mimeType;
}

/**
 * Equality operator
 *
 * @return bool
 */
bool AlfrescoFileInfo::operator==( const AlfrescoFileInfo& finfo) {
	if ( getName().equals( finfo.getName()))
		return true;
	return false;
}

/**
 * Less than operator
 *
 * @return bool
 */
bool AlfrescoFileInfo::operator<( const AlfrescoFileInfo& finfo) {
	if ( finfo.getName().compareTo( getName()) < 0)
		return true;
	return false;
}

/**
 * Default constructor
 */
AlfrescoActionInfo::AlfrescoActionInfo(void) {
	m_attributes = 0;
	m_clientPreActions = 0;
}

/**
 * Class constructor
 *
 * @param name const String&
 * @param attr const unsigned int
 * @param preActions const unsigned int
 */
AlfrescoActionInfo::AlfrescoActionInfo( const String& name, const unsigned int attr, const unsigned int preActions) {
	m_name = name;
	m_attributes = attr;
	m_clientPreActions = preActions;
}

/**
 * Return the action information as a string
 *
 * @return const String
 */
const String AlfrescoActionInfo::toString(void) const {
	String str = L"[";

	str.append(getName());
	str.append(L":");
	str.append(getPseudoName());
	str.append(L":Attr=0x");
	str.append(Integer::toHexString(getAttributes()));
	str.append(L":preActions=0x");
	str.append(Integer::toHexString(getPreProcessActions()));

	if ( hasConfirmationMessage()) {
		str.append(L":Conf=");
		str.append(getConfirmationMessage());
	}
	str.append(L"]");

	return str;
}

/**
 * Assignment operator
 *
 * @param actionInfo const AlfrescoActionInfo&
 * @return AlfrescoActionInfo&
 */
AlfrescoActionInfo& AlfrescoActionInfo::operator=( const AlfrescoActionInfo& actionInfo) {
	setName(actionInfo.getName());
	setPseudoName(actionInfo.getPseudoName());

	setAttributes(actionInfo.getAttributes());
	setPreProcessActions(actionInfo.getPreProcessActions());

	setConfirmationMessage(actionInfo.getConfirmationMessage());

	return *this;
}
