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
package com.strangeberry.jmdns.tools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

/**
 * A sample JmDNS responder that reads a set of rendezvous service definitions from a file and registers them with rendezvous. It uses the same file format as Apple's responder. Each record consists of 4 lines: name, type, text, port. Empty lines and
 * lines starting with # between records are ignored.
 * 
 * @author Arthur van Hoff
 */
public class Responder {
    /**
     * Constructor.
     * 
     * @param jmdns
     * @param file
     * @throws IOException
     */
    public Responder(JmDNS jmdns, String file) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(file));
        try {
            while (true) {
                String ln = in.readLine();
                while ((ln != null) && (ln.startsWith("#") || ln.trim().length() == 0)) {
                    ln = in.readLine();
                }
                if (ln != null) {
                    String name = ln;
                    String type = in.readLine();
                    String text = in.readLine();
                    int port = Integer.parseInt(in.readLine());

                    // make sure the type is fully qualified and in the local. domain
                    if (type != null) {
                        if (!type.endsWith(".")) {
                            type += ".";
                        }
                        if (!type.endsWith(".local.")) {
                            type += "local.";
                        }

                        jmdns.registerService(ServiceInfo.create(type, name, port, text));
                    }
                }
            }
        } finally {
            in.close();
        }
    }

    /**
     * Create a responder.
     * 
     * @param argv
     * @throws IOException
     */
    public static void main(String argv[]) throws IOException {
        new Responder(JmDNS.create(), (argv.length > 0) ? argv[0] : "services.txt");
    }
}