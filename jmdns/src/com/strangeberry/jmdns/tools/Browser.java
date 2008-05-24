//Licensed under Apache License version 2.0
//Original license LGPL

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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.GridLayout;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import javax.jmdns.ServiceTypeListener;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

/**
 * User Interface for browsing JmDNS services.
 *
 * @author	Arthur van Hoff, Werner Randelshofer
 * @version 	%I%, %G%
 */
public class Browser extends JFrame implements ServiceListener, ServiceTypeListener, ListSelectionListener {
    JmDNS jmdns;
    Vector headers;
    String type;
    DefaultListModel types;
    JList typeList;
    DefaultListModel services;
    JList serviceList;
    JTextArea info;
    
    Browser(JmDNS jmdns) throws IOException {
        super("JmDNS Browser");
        this.jmdns = jmdns;
        
        Color bg = new Color(230, 230, 230);
        EmptyBorder border = new EmptyBorder(5, 5, 5, 5);
        Container content = getContentPane();
        content.setLayout(new GridLayout(1, 3));
        
        types = new DefaultListModel();
        typeList = new JList(types);
        typeList.setBorder(border);
        typeList.setBackground(bg);
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
        
        jmdns.addServiceTypeListener(this);
        
        // register some well known types
        String list[] = new String[] {
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
        };
        
        for (int i = 0 ; i < list.length ; i++) {
            jmdns.registerServiceType(list[i]);
        }
        
        show();
    }
    
    /**
     * Add a service.
     */
    public void serviceAdded(ServiceEvent event) {
        final String name = event.getName();
        
        System.out.println("ADD: " + name);
        SwingUtilities.invokeLater(new Runnable() {
        public void run() { insertSorted(services, name); }
        });
    }
    
    /**
     * Remove a service.
     */
    public void serviceRemoved(ServiceEvent event) {
        final String name = event.getName();

        System.out.println("REMOVE: " + name);
        SwingUtilities.invokeLater(new Runnable() {
        public void run() { services.removeElement(name); }
        });
    }
    
    /**
     * A new service type was <discovered.
     */
    public void serviceTypeAdded(ServiceEvent event) {
        final String type = event.getType();

        System.out.println("TYPE: " + type);
        SwingUtilities.invokeLater(new Runnable() {
        public void run() { insertSorted(types, type); }
        });
    }
    
    
    void insertSorted(DefaultListModel model, String value) {
        for (int i = 0, n = model.getSize() ; i < n ; i++) {
            if (value.compareToIgnoreCase((String)model.elementAt(i)) < 0) {
                model.insertElementAt(value, i);
                return;
            }
        }
        model.addElement(value);
    }
    
    /**
     * Resolve a service.
     */
    public void serviceResolved(ServiceEvent event) {
        String name = event.getName();
        String type = event.getType();
        ServiceInfo info = event.getInfo();

        if (name.equals(serviceList.getSelectedValue())) {
            if (info == null) {
                this.info.setText("service not found");
            } else {
                
                StringBuffer buf = new StringBuffer();
                buf.append(name);
                buf.append('.');
                buf.append(type);
                buf.append('\n');
                buf.append(info.getServer());
                buf.append(':');
                buf.append(info.getPort());
                buf.append('\n');
                buf.append(info.getAddress());
                buf.append(':');
                buf.append(info.getPort());
                buf.append('\n');
                for (Enumeration names = info.getPropertyNames() ; names.hasMoreElements() ; ) {
                    String prop = (String)names.nextElement();
                    buf.append(prop);
                    buf.append('=');
                    buf.append(info.getPropertyString(prop));
                    buf.append('\n');
                }
                
                this.info.setText(buf.toString());
            }
        }
    }
    
    /**
     * List selection changed.
     */
    public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            if (e.getSource() == typeList) {
                type = (String)typeList.getSelectedValue();
                jmdns.removeServiceListener(type, this);
                services.setSize(0);
                info.setText("");
                if (type != null) {
                jmdns.addServiceListener(type, this);
                }
            } else if (e.getSource() == serviceList) {
                String name = (String)serviceList.getSelectedValue();
                if (name == null) {
                    info.setText("");
                } else {
                    System.out.println(this+" valueChanged() type:"+type+" name:"+name);
                    System.out.flush();
                    ServiceInfo service = jmdns.getServiceInfo(type, name);
                    if (service == null) {
                        info.setText("service not found");
                    } else {
                        jmdns.requestServiceInfo(type, name);
                    }
                }
            }
        }
    }
    
    /**
     * Table data.
     */
    class ServiceTableModel extends AbstractTableModel {
        public String getColumnName(int column) {
            switch (column) {
                case 0: return "service";
                case 1: return "address";
                case 2: return "port";
                case 3: return "text";
            }
            return null;
        }
        public int getColumnCount() {
            return 1;
        }
        public int getRowCount() {
            return services.size();
        }
        public Object getValueAt(int row, int col) {
            return services.elementAt(row);
        }
    }
    
    public String toString() {
        return "RVBROWSER";
    }
    
    /**
     * Main program.
     */
    public static void main(String argv[]) throws IOException {
        new Browser(JmDNS.create());
    }
}
