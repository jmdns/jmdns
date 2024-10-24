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
import java.util.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jmdns.impl.DNSOutgoing;
import javax.jmdns.impl.JmDNSImpl;
import javax.jmdns.impl.constants.DNSConstants;
import javax.jmdns.impl.tasks.DNSTask;

/**
 * This is the root class for all resolver tasks.
 *
 * @author Pierre Frisch
 */
public abstract class DNSResolverTask extends DNSTask {
    private final Logger logger = LoggerFactory.getLogger(DNSResolverTask.class);

    /**
     * Counts the number of queries being sent.
     */
    protected int count = 0;

    /**
     * @param jmDNSImpl the JmDNS instance which belongs to this resolver task
     */
    protected DNSResolverTask(JmDNSImpl jmDNSImpl) {
        super(jmDNSImpl);
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return super.toString() + " count: " + count;
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.impl.tasks.DNSTask#start(java.util.Timer)
     */
    @Override
    public void start(Timer timer) {
        if (!this.getDns().isCanceling() && !this.getDns().isCanceled()) {
            timer.schedule(this, DNSConstants.QUERY_WAIT_INTERVAL, DNSConstants.QUERY_WAIT_INTERVAL);
        }
    }

    /*
     * (non-Javadoc)
     * @see java.util.TimerTask#run()
     */
    @Override
    public void run() {
        try {
            if (this.getDns().isCanceling() || this.getDns().isCanceled()) {
                this.cancel();
            } else {
                if (count++ < 3) {
                    logger.debug("{}.run() JmDNS {}", this.getName(), this.description());

                    DNSOutgoing out = new DNSOutgoing(DNSConstants.FLAGS_QR_QUERY);
                    out = this.addQuestions(out);
                    if (this.getDns().isAnnounced()) {
                        out = this.addAnswers(out);
                    }
                    if (!out.isEmpty()) {
                        this.getDns().send(out);
                    }
                } else {
                    // After three queries, we can quit.
                    this.cancel();
                }
            }
        } catch (Throwable e) {
            logger.warn("{}.run() exception ", this.getName(), e);
            this.getDns().recover();
        }
    }

    /**
     * Overridden by subclasses to add questions to the message.<br/>
     * <b>Note:</b> Because of message size limitation the returned message may be different from the message parameter.
     *
     * @param out outgoing message
     * @return the outgoing message.
     * @throws IOException
     */
    protected abstract DNSOutgoing addQuestions(DNSOutgoing out) throws IOException;

    /**
     * Overridden by subclasses to add questions to the message.<br/>
     * <b>Note:</b> Because of message size limitation the returned message may be different from the message parameter.
     *
     * @param out outgoing message
     * @return the outgoing message.
     * @throws IOException
     */
    protected abstract DNSOutgoing addAnswers(DNSOutgoing out) throws IOException;

    /**
     * Returns a description of the resolver for debugging
     *
     * @return resolver description
     */
    protected abstract String description();

}
