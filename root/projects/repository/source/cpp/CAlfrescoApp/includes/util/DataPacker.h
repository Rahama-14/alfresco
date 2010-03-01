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

#ifndef _DataPacker_H
#define _DataPacker_H

//	Includes

#include "util\String.h"
#include "util\Types.h"
#include "util\JavaTypes.h"

//	Classes defined in this header file

namespace Alfresco {
	class DataPacker;
}

/**
 * DataPacker Class
 *
 * The DataPacker class provides methods for packing and unpacking of various data types from a buffer.
 */
class Alfresco::DataPacker {
private:
	//	Hide constructors

	DataPacker( void) {};
	DataPacker(const DataPacker& dp) {};

public:
	//	Unpack data types from a buffer

	static int getShort(CBUFPTR buf, BUFPOS pos);
	static int getInt(CBUFPTR buf, BUFPOS pos);
	static LONG64 getLong(CBUFPTR buf, BUFPOS pos);

	static int getIntelShort(CBUFPTR buf, BUFPOS pos);
	static int getIntelInt(CBUFPTR buf, BUFPOS pos);
	static LONG64 getIntelLong(CBUFPTR buf, BUFPOS pos);

	static String getString(CBUFPTR buf, BUFPOS pos, const unsigned int maxLen, const bool isUni = false);
	static String getUnicodeString(CBUFPTR buf, BUFPOS pos, const unsigned int maxLen);

	//	Pack data types into a buffer

	static void putShort(const int val, BUFPTR buf, BUFPOS pos);
	static void putInt(const int val, BUFPTR buf, BUFPOS pos);
	static void putLong(const LONG64 val, BUFPTR buf, BUFPOS pos);

	static void putIntelShort(const int val, BUFPTR buf, BUFPOS pos);
	static void putIntelInt(const int val, BUFPTR buf, BUFPOS pos);
	static void putIntelLong(const LONG64 val, BUFPTR buf, BUFPOS pos);

	static unsigned int putString(const String& str, BUFPTR buf, BUFPOS pos, bool nullTerm = true, bool isUni = false);
	static unsigned int putString(const char* str, BUFLEN len, BUFPTR buf, BUFPOS pos, bool nullTerm = true);
	static unsigned int putString(const wchar_t* str, BUFLEN len, BUFPTR buf, BUFPOS pos, bool nullTerm = true);

	static void putZeros(BUFPTR buf, BUFPOS pos, const unsigned int count);

	//	Calculate buffer positions

	static unsigned int getStringLength(const String& str, const bool isUni = false, const bool nulTerm = false);
	static unsigned int getBufferPosition(BUFPOS pos, const String& str, const bool isUni = false, const bool nulTerm = false);

	//	Align a buffer offset

	static inline BUFPOS longwordAlign( BUFPOS pos) { return ( pos + 3) & 0xFFFFFFFC; }
	static inline BUFPOS wordAlign( BUFPOS pos) { return ( pos + 1) & 0xFFFFFFFE; }
};

#endif
