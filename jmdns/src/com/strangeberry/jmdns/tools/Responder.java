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

package com.strangeberry.jmdns.tools;

import java.io.*;
import javax.jmdns.*;

/**
 * A sample JmDNS responder that reads a set of rendezvous service
 * definitions from a file and registers them with rendezvous. It uses
 * the same file format as Apple's responder.  Each record consists of
 * 4 lines: name, type, text, port. Empty lines and lines starting with #
 * between records are ignored.
 *
 * @author	Arthur van Hoff
 * @version 	%I%, %G%
 */
public class Responder
{
    /**
     * Constructor.
     */
    public Responder(JmDNS jmdns, String file) throws IOException
    {
	BufferedReader in = new BufferedReader(new FileReader(file));
	try {
	    while (true) {
		String ln = in.readLine();
		while ((ln != null) && (ln.startsWith("#") || ln.trim().length() == 0)) {
		    ln = in.readLine();
		}
		if (ln == null) {
		    break;
		}
		String name = ln;
		String type = in.readLine();
		String text = in.readLine();
		int port = Integer.parseInt(in.readLine());

		// make sure the type is fully qualified and in the local. domain
		if (!type.endsWith(".")) {
		    type += ".";
		}
		if  (!type.endsWith(".local.")) {
		    type += "local.";
		}

		jmdns.registerService(
		    new ServiceInfo(type, name, port, text));
	    }
	} finally {
	    in.close();
	}
    }
    
    /**
     * Create a responder.
     */
    public static void main(String argv[]) throws IOException
    {
	new Responder(new JmDNS(), (argv.length > 0) ? argv[0] : "services.txt");
    }
}


