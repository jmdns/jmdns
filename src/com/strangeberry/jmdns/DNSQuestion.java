// Copyright (C) 2002  Strangeberry Inc.
// @(#)DNSQuestion.java, 1.13, 11/29/2002
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
// 
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
// 
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

package com.strangeberry.jmdns;

import java.io.*;
import java.util.*;

/**
 * A DNS question.
 *
 * @author	Arthur van Hoff
 * @version 	1.13, 11/29/2002
 */
final class DNSQuestion extends DNSEntry
{
    /**
     * Create a question.
     */
    DNSQuestion(String name, int type, int clazz)
    {
	super(name, type, clazz);
    }

    /**
     * Check if this question is answered by a given DNS record.
     */
    boolean answeredBy(DNSRecord rec)
    {
	return (clazz == rec.clazz) && ((type == rec.type) || (type == TYPE_ANY)) &&
	    name.equals(rec.name);
    }

    /**
     * For debugging only.
     */
    public String toString()
    {
	return toString("question", null);
    }
}
