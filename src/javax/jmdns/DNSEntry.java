// %Z%%M%, %I%, %G%
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

package javax.jmdns;

/**
 * DNS entry with a name, type, and class. This is the base
 * class for questions and records.
 *
 * @author	Arthur van Hoff
 * @version 	%I%, %G%
 */
class DNSEntry {
    String key;
    String name;
    int type;
    int clazz;
    boolean unique;
    
    /**
     * Create an entry.
     */
    DNSEntry(String name, int type, int clazz)
    {
	this.key = name.toLowerCase();
	this.name = name;
	this.type = type;
	this.clazz = clazz & DNSConstants.CLASS_MASK;
	this.unique = (clazz & DNSConstants.CLASS_UNIQUE) != 0;
    }

    /**
     * Check if two entries have exactly the same name, type, and class.
     */
    public boolean equals(Object obj)
    {
	if (obj instanceof DNSEntry) {
	    DNSEntry other = (DNSEntry)obj;
	    return name.equals(other.name) && type == other.type && clazz == other.clazz;
	}
	return false;
    }

    public String getName() {
        return name;
    }
    
    /**
     * Overriden, to return a value which is consistent with the value returned
     * by equals(Object).
     */
    public int hashCode()
    {
	return name.hashCode() + type + clazz;
    }
    /**
     * Get a string given a clazz.
     */
    static String getClazz(int clazz)
    {
	switch (clazz & DNSConstants.CLASS_MASK) {
    	  case DNSConstants.CLASS_IN:	return "in";
    	  case DNSConstants.CLASS_CS:	return "cs";
    	  case DNSConstants.CLASS_CH:	return "ch";
    	  case DNSConstants.CLASS_HS:	return "hs";
    	  case DNSConstants.CLASS_NONE:	return "none";
    	  case DNSConstants.CLASS_ANY:	return "any";
	  default:		return "?";
	}
    }

    /**
     * Get a string given a type.
     */
    static String getType(int type)
    {
	switch (type) {
    	  case DNSConstants.TYPE_A: 		return "a";
    	  case DNSConstants.TYPE_NS:		return "ns";
    	  case DNSConstants.TYPE_MD:		return "md";
    	  case DNSConstants.TYPE_MF:		return "mf";
    	  case DNSConstants.TYPE_CNAME:	return "cname";
    	  case DNSConstants.TYPE_SOA:	return "soa";
    	  case DNSConstants.TYPE_MB:		return "mb";
    	  case DNSConstants.TYPE_MG:		return "mg";
    	  case DNSConstants.TYPE_MR:		return "mr";
    	  case DNSConstants.TYPE_NULL:	return "null";
    	  case DNSConstants.TYPE_WKS:	return "wks";
    	  case DNSConstants.TYPE_PTR:	return "ptr";
    	  case DNSConstants.TYPE_HINFO:	return "hinfo";
    	  case DNSConstants.TYPE_MINFO:	return "minfo";
    	  case DNSConstants.TYPE_MX:		return "mx";
    	  case DNSConstants.TYPE_TXT:	return "txt";
    	  case DNSConstants.TYPE_SRV:	return "srv";
    	  case DNSConstants.TYPE_ANY:	return "any";
	      default:		return "?";
	}
    }

    public String toString(String hdr, String other)
    {
	return hdr + "[" + getType(type) + "," + getClazz(clazz) + (unique ? "-unique," : ",") + name + ((other != null) ? "," + other + "]" : "]");
    }
}
