/*
 * @(#)TestShutdownHook.java  1.0  May 24, 2004
 *
 * Copyright (c) 2004 Werner Randelshofer
 * Staldenmattweg 2, Immensee, CH-6405, Switzerland.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Werner Randelshofer. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Werner Randelshofer.
 */

package samples;

import java.io.*;
/**
 * TestShutdownHook.
 *
 * @author  Werner Randelshofer
 * @version 1.0  May 24, 2004  Created.
 */
public class TestShutdownHook {
    
    /** Creates a new instance. */
    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(
        new Thread() {
            public void run() {
                System.out.println("Shutdown Hook");
            }
        }
        );
        try {
       int b;
       while ((b=System.in.read()) != -1) {
           System.out.print("\""+(char) b);
       }
        } catch (IOException e) {
        }
    }
    
}
