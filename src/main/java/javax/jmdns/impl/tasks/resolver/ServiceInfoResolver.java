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

package javax.jmdns.impl.tasks.resolver;

import java.io.IOException;

import javax.jmdns.impl.DNSEntry;
import javax.jmdns.impl.DNSOutgoing;
import javax.jmdns.impl.DNSQuestion;
import javax.jmdns.impl.DNSRecord;
import javax.jmdns.impl.JmDNSImpl;
import javax.jmdns.impl.ServiceInfoImpl;
import javax.jmdns.impl.constants.DNSRecordClass;
import javax.jmdns.impl.constants.DNSRecordType;

/**
 * The ServiceInfoResolver queries up to three times consecutively for a service info, and then removes itself from the timer.
 * <p/>
 * The ServiceInfoResolver will run only if JmDNS is in state ANNOUNCED. REMIND: Prevent having multiple service resolvers for the same info in the timer queue.
 */
public class ServiceInfoResolver extends DNSResolverTask {

    private final ServiceInfoImpl serviceInfo;

    public ServiceInfoResolver(JmDNSImpl jmDNSImpl, ServiceInfoImpl info) {
        super(jmDNSImpl);
        serviceInfo = info;
        serviceInfo.setDns(getDns());
        getDns().addListener(info, DNSQuestion.newQuestion(serviceInfo.getQualifiedName(), DNSRecordType.TYPE_ANY, DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE));
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.impl.tasks.DNSTask#getName()
     */
    @Override
    public String getName() {
        return "ServiceInfoResolver(" + (this.getDns() != null ? this.getDns().getName() : "") + ")";
    }

    /*
     * (non-Javadoc)
     * @see java.util.TimerTask#cancel()
     */
    @Override
    public boolean cancel() {
        // We should not forget to remove the listener
        boolean result = super.cancel();
        if (!serviceInfo.isPersistent()) {
            this.getDns().removeListener(serviceInfo);
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.impl.tasks.Resolver#addAnswers(javax.jmdns.impl.DNSOutgoing)
     */
    @Override
    protected DNSOutgoing addAnswers(DNSOutgoing out) throws IOException {
        DNSOutgoing newOut = out;
        if (!serviceInfo.hasData()) {
            long now = System.currentTimeMillis();
            newOut = this.addAnswer(newOut, (DNSRecord) this.getDns().getCache().getDNSEntry(serviceInfo.getQualifiedName(), DNSRecordType.TYPE_SRV, DNSRecordClass.CLASS_IN), now);
            newOut = this.addAnswer(newOut, (DNSRecord) this.getDns().getCache().getDNSEntry(serviceInfo.getQualifiedName(), DNSRecordType.TYPE_TXT, DNSRecordClass.CLASS_IN), now);
            if (!serviceInfo.getServer().isEmpty()) {
                for (DNSEntry addressEntry : this.getDns().getCache().getDNSEntryList(serviceInfo.getServer(), DNSRecordType.TYPE_A, DNSRecordClass.CLASS_IN)) {
                    newOut = this.addAnswer(newOut, (DNSRecord) addressEntry, now);
                }
                for (DNSEntry addressEntry : this.getDns().getCache().getDNSEntryList(serviceInfo.getServer(), DNSRecordType.TYPE_AAAA, DNSRecordClass.CLASS_IN)) {
                    newOut = this.addAnswer(newOut, (DNSRecord) addressEntry, now);
                }
            }
        }
        return newOut;
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.impl.tasks.Resolver#addQuestions(javax.jmdns.impl.DNSOutgoing)
     */
    @Override
    protected DNSOutgoing addQuestions(DNSOutgoing out) throws IOException {
        DNSOutgoing newOut = out;
        if (!serviceInfo.hasData()) {
            newOut = this.addQuestion(newOut, DNSQuestion.newQuestion(serviceInfo.getQualifiedName(), DNSRecordType.TYPE_SRV, DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE));
            newOut = this.addQuestion(newOut, DNSQuestion.newQuestion(serviceInfo.getQualifiedName(), DNSRecordType.TYPE_TXT, DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE));
            if (!serviceInfo.getServer().isEmpty()) {
                newOut = this.addQuestion(newOut, DNSQuestion.newQuestion(serviceInfo.getServer(), DNSRecordType.TYPE_A, DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE));
                newOut = this.addQuestion(newOut, DNSQuestion.newQuestion(serviceInfo.getServer(), DNSRecordType.TYPE_AAAA, DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE));
            }
        }
        return newOut;
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.impl.tasks.Resolver#description()
     */
    @Override
    protected String description() {
        return "querying service info: " + (serviceInfo != null ? serviceInfo.getQualifiedName() : "null");
    }

}