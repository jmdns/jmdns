//Licensed under Apache License version 2.0
//Original license LGPL

package samples;

import java.io.*;

/**
 * TestShutdownHook.
 *
 * @author Werner Randelshofer
 * @version 1.0 May 24, 2004 Created.
 */
public class TestShutdownHook
{

    /**
     * Creates a new instance.
     *
     * @param args
     */
    public static void main(String[] args)
    {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run()
            {
                System.out.println("Shutdown Hook");
            }
        });
        try
        {
            int b;
            while ((b = System.in.read()) != -1)
            {
                System.out.print("\"" + (char) b);
            }
        }
        catch (IOException e)
        {
            /* Stub */
        }
    }

}
