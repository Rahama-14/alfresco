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

#ifndef _JavaLong_H
#define _JavaLong_H

//	Includes

#include <windows.h>

#include "util\String.h"
#include "util\Exception.h"
#include "util\JavaTypes.h"

//	Classes defined in this header file

namespace Alfresco {
	class Long;
}

/**
 * Java-like Long Class
 * 
 * Provides static methods to convert long/64 bit values to strings.
 */
class Alfresco::Long {
public:
	//	Convert a long/64 bit integer to a hexadecimal string

	static String toHexString( const LONG64 lval);

	// Convert a long/64 bit integer to a decimal string

	static String toString( const LONG64 lval);

	//	Make a long/64bit value from the low/high 32bit values

	static LONG64 makeLong( unsigned int lowPart, unsigned int highPart);
	static LONG64 makeLong( FILETIME fTime);

	//	Get the low/high 32bit values from a 64bit value

	static bool hasHighPart( LONG64 lval) { return ( lval > 0xFFFFFFFF) ? true : false; }

	static unsigned int getLowPart( LONG64 lval) { return (unsigned int) lval & 0xFFFFFFFF; }
	static unsigned int getHighPart( LONG64 lval) { return (unsigned int) ((lval >> 32) & 0xFFFFFFFF); }

	//	Parse a string to generate a long/64 bit integer value

	static LONG64 parseLong( const String& str, unsigned int radix = 10);

	//	Copy a long/64bit value to a FILETIME structure

	static void copyTo( LONG64 lval, FILETIME& ftime);

private:
	//	Hide constructors, static only class

	Long( void);
	Long(Long& ival);
};

#endif