package javax.jmdns.impl;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//simple main class from which jmdns/jmmdns can be run in a linux desktop machine.
public class MyJMMDNS {

	static Logger logger = Logger.getLogger(MyJMMDNS.class);
	static EKjmdns jmdns;

	//main function
	public static void main(String argv[]) throws Exception {

		PropertyConfigurator.configure("log4j.properties");

		//init jmmdns
		try {
			jmdns = new EKjmdns();
			ExecutorService executorService = Executors.newFixedThreadPool(1);
			executorService.execute(jmdns);

			//add shutdownhook
			List<Terminable> terminableTasks = new ArrayList<Terminable>();
			terminableTasks.add(jmdns);
			Runtime.getRuntime().addShutdownHook(new ShutDownHook(terminableTasks));

			System.out.println("End of main function!");

		}catch (Exception e){
			e.printStackTrace();
		}
	}

}


