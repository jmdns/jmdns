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
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.jmdns.*;

/**
 * User Interface for browsing JmDNS services.
 *
 * @author	Arthur van Hoff
 * @version 	%I%, %G%
 */
public class Browser extends JFrame implements ServiceListener, ListSelectionListener
{
    JmDNS jmdns;
    Vector headers;
    DefaultListModel services;
    String type;
    JList typeList;
    JList serviceList;
    JTextArea info;
    
    Browser(JmDNS jmdns)
    {
	this(jmdns, new String[] {
	    "_http._tcp.local.",
	    "_ftp._tcp.local.",
	    "_tftp._tcp.local.",
	    "_ssh._tcp.local.",
	    "_smb._tcp.local.",
	    "_printer._tcp.local.",
	    "_airport._tcp.local.",
	    "_afpovertcp._tcp.local.",
	    "_ichat._tcp.local.",
	    "_eppc._tcp.local.",
            "_presence._tcp.local."
	});
    }
    Browser(JmDNS jmdns, String types[])
    { 
        super("JmDNS Browser");
	this.jmdns = jmdns;

	Color bg = new Color(230, 230, 230);
        EmptyBorder border = new EmptyBorder(5, 5, 5, 5);
	Container content = getContentPane();
        content.setLayout(new GridLayout(1, 3));
	Arrays.sort(types);
	type = types[0];
	
	typeList = new JList(types);
        typeList.setBorder(border);
	typeList.setBackground(bg);
	typeList.setSelectedIndex(0);
	typeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	typeList.addListSelectionListener(this);

	JPanel typePanel = new JPanel();
	typePanel.setLayout(new BorderLayout());
	typePanel.add("North", new JLabel("Types"));
	typePanel.add("Center", new JScrollPane(typeList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
	content.add(typePanel);

	services = new DefaultListModel();
	serviceList = new JList(services);
	serviceList.setBorder(border);
	serviceList.setBackground(bg);
	serviceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	serviceList.addListSelectionListener(this);

	JPanel servicePanel = new JPanel();
	servicePanel.setLayout(new BorderLayout());
	servicePanel.add("North", new JLabel("Services"));
	servicePanel.add("Center", new JScrollPane(serviceList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
	content.add(servicePanel);

	info = new JTextArea();
	info.setBorder(border);
	info.setBackground(bg);
	info.setEditable(false);
	info.setLineWrap(true);

	JPanel infoPanel = new JPanel();
	infoPanel.setLayout(new BorderLayout());
	infoPanel.add("North", new JLabel("Details"));
	infoPanel.add("Center", new JScrollPane(info, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
	content.add(infoPanel);
	
	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocation(100, 100);
        setSize(600, 400);
        show();

	jmdns.addServiceListener(type, this);
    }

    /**
     * Add a service.
     */
    public void addService(JmDNS jmdns, String type, String name)
    {
	if (name.endsWith("." + type)) {
	    name = name.substring(0, name.length() - (type.length() + 1));
	}
	System.out.println("ADD: " + name);
	services.addElement(name);
    }

    /**
     * Remove a service.
     */
    public void removeService(JmDNS jmdns, String type, String name)
    {
	if (name.endsWith("." + type)) {
	    name = name.substring(0, name.length() - (type.length() + 1));
	}
	//System.out.println("REMOVE: " + name);
	services.removeElement(name);
    }

    /**
     * List selection changed.
     */
    public void valueChanged(ListSelectionEvent e)
    {
	if (!e.getValueIsAdjusting()) {
	    if (e.getSource() == typeList) {
		type = (String)typeList.getSelectedValue();
		jmdns.removeServiceListener(this);
		services.setSize(0);
		info.setText("");
		jmdns.addServiceListener(type, this);
	    } else if (e.getSource() == serviceList) {
		String name = (String)serviceList.getSelectedValue();
		if (name == null) {
		    info.setText("");
		} else {
		    if (!name.endsWith(".")) {
			name = name + "." + type;
		    }
		    ServiceInfo service = jmdns.getServiceInfo(type, name);
		    if (service == null) {
			info.setText("service not found");
		    } else {
			StringBuffer buf = new StringBuffer();
			buf.append(name);
			buf.append('\n');
			buf.append(service.getAddress());
			buf.append(':');
			buf.append(service.getPort());
			buf.append('\n');
			String txt = service.getTextString();
			if (txt != null) {
			    buf.append('\n');
			    buf.append(txt);
			    buf.append('\n');
			}
		    
			info.setText(buf.toString());
		    }
		}
	    }
	}
    }

    /**
     * Table data.
     */
    class ServiceTableModel extends AbstractTableModel
    {
	public String getColumnName(int column)
	{
	    switch (column) {
	      case 0: return "service";
	      case 1: return "address";
	      case 2: return "port";
	      case 3: return "text";
	    }
	    return null;
	}
	public int getColumnCount()
	{
	    return 1;
	}
        public int getRowCount()
        {
	    return services.size();
        }
        public Object getValueAt(int row, int col)
        {
	    return services.elementAt(row);
        }
    }

    public String toString()
    {
	return "RVBROWSER";
    }

    /**
     * Main program.
     */
    public static void main(String argv[]) throws IOException
    {
	new Browser(new JmDNS());
    }
}
