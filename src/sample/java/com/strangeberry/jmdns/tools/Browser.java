// Licensed under Apache License version 2.0
// Original license LGPL

//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package com.strangeberry.jmdns.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.GridLayout;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Enumeration;

import javax.jmdns.JmmDNS;
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
 * @author Arthur van Hoff, Werner Randelshofer
 */
public class Browser extends JFrame implements ServiceListener, ServiceTypeListener, ListSelectionListener {
    /**
     *
     */
    private static final long serialVersionUID = 5750114542524415107L;
    JmmDNS                    jmmdns;
    // Vector headers;
    String                    type;
    DefaultListModel          types;
    JList                     typeList;
    DefaultListModel          services;
    JList                     serviceList;
    JTextArea                 info;

    /**
     * @param mmDNS
     * @throws IOException
     */
    Browser(JmmDNS mmDNS) throws IOException {
        super("JmDNS Browser");
        this.jmmdns = mmDNS;

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

        this.jmmdns.addServiceTypeListener(this);

        // register some well known types
        // String list[] = new String[] { "_http._tcp.local.", "_ftp._tcp.local.", "_tftp._tcp.local.", "_ssh._tcp.local.", "_smb._tcp.local.", "_printer._tcp.local.", "_airport._tcp.local.", "_afpovertcp._tcp.local.", "_ichat._tcp.local.",
        // "_eppc._tcp.local.", "_presence._tcp.local.", "_rfb._tcp.local.", "_daap._tcp.local.", "_touchcs._tcp.local." };
        String[] list = new String[] {};

        for (int i = 0; i < list.length; i++) {
            this.jmmdns.registerServiceType(list[i]);
        }

        this.setVisible(true);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.ServiceListener#serviceAdded(javax.jmdns.ServiceEvent)
     */
    @Override
    public void serviceAdded(ServiceEvent event) {
        final String name = event.getName();

        System.out.println("ADD: " + name);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                insertSorted(services, name);
            }
        });
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.ServiceListener#serviceRemoved(javax.jmdns.ServiceEvent)
     */
    @Override
    public void serviceRemoved(ServiceEvent event) {
        final String name = event.getName();

        System.out.println("REMOVE: " + name);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                services.removeElement(name);
            }
        });
    }

    @Override
    public void serviceResolved(ServiceEvent event) {
        final String name = event.getName();

        System.out.println("RESOLVED: " + name);
        if (name.equals(serviceList.getSelectedValue())) {
            ServiceInfo[] serviceInfos = this.jmmdns.getServiceInfos(type, name);
            this.dislayInfo(serviceInfos);
            // this.dislayInfo(new ServiceInfo[] { event.getInfo() });
        }
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.ServiceTypeListener#serviceTypeAdded(javax.jmdns.ServiceEvent)
     */
    @Override
    public void serviceTypeAdded(ServiceEvent event) {
        final String aType = event.getType();

        System.out.println("TYPE: " + aType);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                insertSorted(types, aType);
            }
        });
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.ServiceTypeListener#subTypeForServiceTypeAdded(javax.jmdns.ServiceEvent)
     */
    @Override
    public void subTypeForServiceTypeAdded(ServiceEvent event) {
        System.out.println("SUBTYPE: " + event.getType());
    }

    void insertSorted(DefaultListModel model, String value) {
        for (int i = 0, n = model.getSize(); i < n; i++) {
            int result = value.compareToIgnoreCase((String) model.elementAt(i));
            if (result == 0) {
                return;
            }
            if (result < 0) {
                model.insertElementAt(value, i);
                return;
            }
        }
        model.addElement(value);
    }

    /**
     * List selection changed.
     *
     * @param e
     */
    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            if (e.getSource() == typeList) {
                type = (String) typeList.getSelectedValue();
                System.out.println("VALUE CHANGED: type: " + type);
                this.jmmdns.removeServiceListener(type, this);
                services.setSize(0);
                info.setText("");
                if (type != null) {
                    this.jmmdns.addServiceListener(type, this);
                }
            } else if (e.getSource() == serviceList) {
                String name = (String) serviceList.getSelectedValue();
                System.out.println("VALUE CHANGED: type: " + type + " service: " + name);
                if (name == null) {
                    info.setText("");
                } else {
                    ServiceInfo[] serviceInfos = this.jmmdns.getServiceInfos(type, name);
                    // This is actually redundant. getServiceInfo will force the resolution of the service and call serviceResolved
                    this.dislayInfo(serviceInfos);
                }
            }
        }
    }

    private void dislayInfo(ServiceInfo[] serviceInfos) {
        if (serviceInfos.length == 0) {
            System.out.println("INFO: null");
            info.setText("service not found\n");
        } else {
            final StringBuilder sb = new StringBuilder(2048);
            System.out.println("INFO: " + serviceInfos.length);
            for (ServiceInfo service : serviceInfos) {
                System.out.println("INFO: " + service);
                sb.append(service.getName());
                sb.append('.');
                sb.append(service.getTypeWithSubtype());
                sb.append('\n');
                sb.append(service.getServer());
                sb.append(':');
                sb.append(service.getPort());
                sb.append('\n');
                for (InetAddress address : service.getInetAddresses()) {
                    sb.append(address);
                    sb.append(':');
                    sb.append(service.getPort());
                    sb.append('\n');
                }
                for (Enumeration<String> names = service.getPropertyNames(); names.hasMoreElements();) {
                    String prop = names.nextElement();
                    sb.append(prop);
                    sb.append('=');
                    sb.append(service.getPropertyString(prop));
                    sb.append('\n');
                }
                sb.append("------------------------\n");
            }
            this.info.setText(sb.toString());
        }
    }

    /**
     * Table data.
     */
    class ServiceTableModel extends AbstractTableModel {
        /**
         *
         */
        private static final long serialVersionUID = 5607994569609827570L;

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return "service";
                case 1:
                    return "address";
                case 2:
                    return "port";
                case 3:
                    return "text";
            }
            return null;
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public int getRowCount() {
            return services.size();
        }

        @Override
        public Object getValueAt(int row, int col) {
            return services.elementAt(row);
        }
    }

    @Override
    public String toString() {
        return "RVBROWSER";
    }

    /**
     * Main program.
     *
     * @param argv
     * @throws IOException
     */
    public static void main(String argv[]) throws IOException {
        new Browser(JmmDNS.Factory.getInstance());
    }
}
