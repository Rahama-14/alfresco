/*
 * Copyright (C) 2005-2006 Alfresco, Inc.
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

#include "alfresco\Alfresco.hpp"
#include "util\DataBuffer.h"
#include "util\Exception.h"
#include "util\Integer.h"

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

	// Check if the path is to a mapped drive

	String alfPath = path;

	if ( alfPath.length() >= 3 && alfPath.substring(1,3).equals( L":\\")) {

		// Try and convert the local path to a UNC path

		m_mappedDrive = alfPath.substring(0, 2);
		wchar_t remPath[MAX_PATH];
		DWORD remPathLen = MAX_PATH;

		DWORD sts = WNetGetConnection(( LPWSTR) m_mappedDrive.data(), remPath, &remPathLen);
		if ( sts != NO_ERROR)
			return;

		// Build the UNC path to the folder

		alfPath = remPath;
		if ( alfPath.endsWith( PathSeperator) == false)
			alfPath.append( PathSeperator);

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

			m_handle = CreateFile(m_uncPath, FILE_WRITE_DATA, FILE_SHARE_READ | FILE_SHARE_WRITE, NULL, OPEN_EXISTING, 0, NULL);
		}

		// Set the root path

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
		sendIOControl( FSCTL_ALFRESCO_PROBE, reqbuf, respbuf);
		alfFolder = true;
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
 * Check in a working copy file
 *
 * @param fileName const wchar_t*
 * @param keepCheckedOut bool
 */
void AlfrescoInterface::checkIn( const wchar_t* fileName, bool keepCheckedOut) {

	// Check if the folder handle is valid

	if ( m_handle == INVALID_HANDLE_VALUE)
		throw BadInterfaceException();

	// Build the file information I/O control request

	DataBuffer reqbuf( 256);
	DataBuffer respbuf( 128);

	reqbuf.putFixedString( IOSignature, IOSignatureLen);
	reqbuf.putString( fileName);
	reqbuf.putInt( keepCheckedOut ? True : False);

	sendIOControl( FSCTL_ALFRESCO_CHECKIN, reqbuf, respbuf);

	// Get the status code

	unsigned int stsCode = respbuf.getInt();
	if ( stsCode == StsSuccess)
		return;
	else {

		// Get the error message, if available

		String errMsg;

		if ( respbuf.getAvailableLength() > 0)
			errMsg = respbuf.getString();
		else {
			errMsg = "Error code ";
			errMsg.append( Integer::toString( stsCode));
		}

		// Throw an exception

		throw Exception( errMsg);
	}
}

/**
 * Check out a file and return the working copy file name
 *
 * @param fileName const wchar_t*
 * @param workingCopy String&
 */
void AlfrescoInterface::checkOut( const wchar_t* fileName, String& workingCopy) {

	// Check if the folder handle is valid

	if ( m_handle == INVALID_HANDLE_VALUE)
		throw BadInterfaceException();

	// Build the file information I/O control request

	DataBuffer reqbuf( 256);
	DataBuffer respbuf( 256);

	reqbuf.putFixedString( IOSignature, IOSignatureLen);
	reqbuf.putString( fileName);

	sendIOControl( FSCTL_ALFRESCO_CHECKOUT, reqbuf, respbuf);

	// Get the status code

	unsigned int stsCode = respbuf.getInt();
	if ( stsCode == StsSuccess) {

		// Get the working copy file name

		workingCopy = respbuf.getString();
	}
	else {

		// Get the error message, if available

		String errMsg;

		if ( respbuf.getAvailableLength() > 0)
			errMsg = respbuf.getString();
		else {
			errMsg = "Error code ";
			errMsg.append( Integer::toString( stsCode));
		}

		// Throw an exception

		throw Exception( errMsg);
	}
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
