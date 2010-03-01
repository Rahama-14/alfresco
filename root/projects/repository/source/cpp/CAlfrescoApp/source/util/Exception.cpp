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

#include "util\Exception.h"

using namespace Alfresco;
using namespace std;

//	Define standard Java-like exceptions

EXCEPTION_CLASS(Alfresco, IOException);
EXCEPTION_CLASS(Alfresco, NullPointerException);
EXCEPTION_CLASS(Alfresco, ArrayIndexOutOfBoundsException);
EXCEPTION_CLASS(Alfresco, NumberFormatException);

/**
* Class constructor
* 
* @param moduleName const char*
* @param lineNum unsigned int
* @param msg const wchar_t*
* @param msg2 const wchar_t*
* @param msg3 const wchar_t*
* @param msg4 const wchar_t*
* @param msg5 const wchar_t*
*/
Exception::Exception( const char* moduleName, unsigned int lineNum, const wchar_t* msg, const wchar_t* msg2,
										  const wchar_t* msg3, const wchar_t* msg4, const wchar_t* msg5) {

	//	Prefix the message string with the module name and line number

	m_msg = moduleName;
	if ( lineNum != 0) {
		m_msg += " (";
		m_msg += lineNum;
		m_msg += ")";
	}
	m_msg += ": ";

	//	Add the messages parts

	if ( msg)
		m_msg += msg;

	if ( msg2) {
		m_msg += " ";
		m_msg += msg2;
	}

	if ( msg3) {
		m_msg += " ";
		m_msg += msg3;
	}

	if ( msg4) {
		m_msg += " ";
		m_msg += msg4;
	}

	if ( msg5) {
		m_msg += " ";
		m_msg += msg5;
	}
}

/**
 * Class constructor
 * 
 * @param msg const wchar_t*
 * @param msg2 const wchar_t*
 * @param msg3 const wchar_t*
 * @param msg4 const wchar_t*
 * @param msg5 const wchar_t*
 */
Exception::Exception( const wchar_t* msg, const wchar_t* msg2, const wchar_t* msg3, const wchar_t* msg4, const wchar_t* msg5) {
	if ( msg)
		m_msg = msg;

	if ( msg2) {
		m_msg += " ";
		m_msg += msg2;
	}

	if ( msg3) {
		m_msg += " ";
		m_msg += msg3;
	}

	if ( msg4) {
		m_msg += " ";
		m_msg += msg4;
	}

	if ( msg5) {
		m_msg += " ";
		m_msg += msg5;
	}
}

/**
 * Copy constructor
 * 
 * @param ex const Exception&
 */
Exception::Exception( const Exception& ex) {
	m_msg = ex.getMessage();
}

/**
 * Class destructor
 * 
 */
Exception::~Exception() {
}


