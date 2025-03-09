/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package javax.jmdns.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;
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

/**
 * User Interface for browsing JmDNS services.
 *
 * @author Arthur van Hoff, Werner Randelshofer
 */
public class Browser extends JFrame implements ServiceListener, ServiceTypeListener, ListSelectionListener {

    private static final long serialVersionUID = 5750114542524415107L;
    static JmmDNS jmmdns;
    static Browser instance;

    private final DefaultListModel<String> services;
    private final DefaultListModel<String> types;
    private final JList<String> serviceList;
    private final JList<String> typeList;
    private final JTextArea info;
    private String type;

    /**
     * Constructs a new MdnsBrowser instance.
     * Initializes the GUI components and sets up the browser window.
     */
    Browser() {
        super("JmDNS Browser");
        instance = this;

        Color bg = new Color(230, 230, 230);
        EmptyBorder border = new EmptyBorder(5, 5, 5, 5);
        Container content = getContentPane();
        content.setLayout(new GridLayout(1, 3));

        types = new DefaultListModel<>();
        typeList = new JList<>(types);
        typeList.setBorder(border);
        typeList.setBackground(bg);
        typeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        typeList.addListSelectionListener(this);

        JPanel typePanel = new JPanel();
        typePanel.setLayout(new BorderLayout());
        typePanel.add("North", new JLabel("Types"));
        typePanel.add("Center", new JScrollPane(typeList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
        content.add(typePanel);

        services = new DefaultListModel<>();
        serviceList = new JList<>(services);
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
        info.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12)); // Set fixed-width font

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BorderLayout());
        infoPanel.add("North", new JLabel("Details"));
        infoPanel.add("Center", new JScrollPane(info, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
        content.add(infoPanel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = screenSize.width / 2;
        int height = screenSize.height / 2;
        setSize(width, height);
        setLocationRelativeTo(null);

        Thread jmdnsLoaderThread = new Thread(new JmDnsLoader());
        jmdnsLoaderThread.start();

        this.setVisible(true);
    }

    /**
     * Main program entry point.
     *
     * @param argv Command line arguments (not used)
     */
    public static void main(String[] argv) {
        new Browser();
    }

    /**
     * Called when a service is added.
     *
     * @param event The ServiceEvent containing information about the added service
     */
    @Override
    public void serviceAdded(ServiceEvent event) {
        final String name = event.getName();
        log("Service added: " + name);
        SwingUtilities.invokeLater(() -> insertSorted(services, name));
    }

    /**
     * Called when a service is removed.
     *
     * @param event The ServiceEvent containing information about the removed service
     */
    @Override
    public void serviceRemoved(ServiceEvent event) {
        final String name = event.getName();
        log("Service removed: " + name);
        SwingUtilities.invokeLater(() -> services.removeElement(name));
    }

    /**
     * Called when a service is resolved.
     *
     * @param event The ServiceEvent containing information about the resolved service
     */
    @Override
    public void serviceResolved(ServiceEvent event) {
        final String name = event.getName();
        log("Service resolved: " + name);

        if (name.equals(serviceList.getSelectedValue())) {
            ServiceInfo[] serviceInfos = jmmdns.getServiceInfos(type, name);
            this.displayInfo(serviceInfos);
        }
    }

    /**
     * Called when a service type is added.
     *
     * @param event The ServiceEvent containing information about the added service type
     */
    @Override
    public void serviceTypeAdded(ServiceEvent event) {
        final String aType = event.getType();
        log("Service type added: " + aType);
        SwingUtilities.invokeLater(() -> insertSorted(types, aType));
    }

    /**
     * Called when a subtype for a service type is added.
     *
     * @param event The ServiceEvent containing information about the added subtype
     */
    @Override
    public void subTypeForServiceTypeAdded(ServiceEvent event) {
        log("Subtype for service type added: " + event.getType());
    }

    /**
     * Inserts a value into a DefaultListModel in a sorted manner.
     *
     * @param model The DefaultListModel to insert into
     * @param value The value to insert
     */
    void insertSorted(DefaultListModel<String> model, String value) {
        int index = Collections.binarySearch(
                Collections.list(model.elements()),
                value,
                String::compareToIgnoreCase
        );
        if (index < 0) {
            model.add(-index - 1, value);
        }
    }

    /**
     * Handles list selection changes.
     *
     * @param e The ListSelectionEvent
     */
    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            if (e.getSource() == typeList) {
                type = typeList.getSelectedValue();
                log("VALUE CHANGED: type: " + type);
                jmmdns.removeServiceListener(type, this);
                services.setSize(0);
                info.setText("");
                if (type != null) {
                    jmmdns.addServiceListener(type, this);
                }
            } else if (e.getSource() == serviceList) {
                String name = serviceList.getSelectedValue();
                log("VALUE CHANGED: type: " + type + " service: " + name);
                if (name == null) {
                    info.setText("");
                } else {
                    ServiceInfo[] serviceInfos = jmmdns.getServiceInfos(type, name);
                    this.displayInfo(serviceInfos);
                }
            }
        }
    }

    /**
     * Displays information about the selected service(s).
     *
     * @param serviceInfos Array of ServiceInfo objects to display
     */
    private void displayInfo(ServiceInfo[] serviceInfos) {
        if (serviceInfos.length == 0) {
            info.setText("service not found\n");
        } else {
            final StringBuilder sb = new StringBuilder(2048);
            for (ServiceInfo service : serviceInfos) {
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
                for (Enumeration<String> names = service.getPropertyNames(); names.hasMoreElements(); ) {
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
     * Returns a string representation of this MdnsBrowser.
     *
     * @return The string "JmDNS Browser"
     */
    @Override
    public String toString() {
        return "JmDNS Browser";
    }

    /**
     * Logs a message to the console.
     *
     * @param message The message to be logged
     */
    private void log(String message) {
        //System.out.println(message);
    }

    /**
     * Initializes the browser by adding a ServiceTypeListener.
     */
    private void initialiseBrowser() {
        try {
            jmmdns.addServiceTypeListener(instance);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Inner class to load JmDNS in a separate thread.
     */
    class JmDnsLoader implements Runnable {
        @Override
        public void run() {
            jmmdns = JmmDNS.Factory.getInstance();
            SwingUtilities.invokeLater(Browser.this::initialiseBrowser);
        }
    }
}