package javax.jmdns.impl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.jmdns.JmmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

// simple intermediate class between jmmdns and main class...
public class EKjmdns extends Thread implements Terminable {

    static Logger logger = LoggerFactory.getLogger(EKjmdns.class.getName());


    //service variables
    String serviceType = "_http._tcp.local.";
    String serviceName = "mohammad_desktop";
    String serviceText = "WIFI_WIFI_WIFI";
    int servicePort = 0;
    JmmDNS registry;
    ServiceInfo serviceInfo;
    SampleListener sampleListener;

    //constructor
    public EKjmdns(){}

    @Override
    public void terminate() {
        try {
            if (registry != null) {
                registry.unregisterAllServices();
                registry.close();
                registry.disableLTEsupport();
                System.out.println(" terminated jmdns stuff");
            }
        } catch (Exception ex) {
            System.out.println(" error in terminating jmdns stuff");
        }
    }

    @Override
    public void run(){
        try{
            //get new jmmdns instance
            registry = JmmDNS.Factory.getInstance();

            //sampleListener
            sampleListener = new SampleListener();

            //serviceinfo
            serviceInfo = ServiceInfo.create(serviceType, serviceName, servicePort, 1, 1, true,  encodeServiceText(serviceText));

            //register service
            registry.registerService(serviceInfo);

            //add service listener
            registry.addServiceListener(serviceType, sampleListener);

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //ServiceListener callback functions
    private static class SampleListener implements ServiceListener {

        public SampleListener(){}

        @Override
        public void serviceAdded(ServiceEvent event) {
            final String name = event.getInfo().getName();
            final String text = decodeServiceText(new String(event.getInfo().getTextBytes()));

            //System.out.println("_NSD_ Service Added:" + " Name: " + name + ", Text: " + text);
            logger.debug("_NSD_ Service Added:" + " Name: " + name + ", Text: " + text);
        }

        @Override
        public void serviceRemoved(ServiceEvent event) {
            final String name = event.getInfo().getName();
            final String text = decodeServiceText(new String(event.getInfo().getTextBytes()));

            //System.out.println("_NSD_ Service Removed:" + " Name: " + name + ", Text: " + text);
            logger.debug("_NSD_ Service Removed:" + " Name: " + name + ", Text: " + text);

        }

        @Override
        public void serviceResolved(ServiceEvent event) {
            final String name = event.getInfo().getName();
            final String text = decodeServiceText(new String(event.getInfo().getTextBytes()));

            //System.out.println("_NSD_ Service Resolved:" + " Name: " + name + ", Text: " + text);
            logger.debug("_NSD_ Service Resolved:" + " Name: " + name + ", Text: " + text);

        }

    }

    //only used for android
    public void enableNeighborDiscoveryOnLTE(){
        if (registry!=null){
            //enable lte support
            ServiceInfo serviceInfoLTE = ServiceInfo.create("LTE_LTE_LTE", serviceName, servicePort, 1, 1, true,  encodeServiceText("LTE_LTE_LTE"));
            registry.enableLTEsupport(serviceInfoLTE, sampleListener);
        }
    }

    //only used for android
    public void disableNeighborDiscoveryOnLTE(){
        if (registry!=null){
            registry.disableLTEsupport();
        }
    }

    //adds encoding with a service text before use.
    public static String encodeServiceText(String serviceText){
        //Add '!@# before and '$%^' after serviceText
        return "!@#" + serviceText + "$%^";
    }

    //decode a service text before
    public static String decodeServiceText(String serviceText){
        //check if the string has '!@# before and '$%^' after
        if(serviceText.contains("!@#") && serviceText.contains("$%^")){
            return serviceText.substring(serviceText.indexOf("!@#") + "!@#".length(), serviceText.indexOf("$%^"));
        }

        return "<empty>";
    }

}
