//Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL

package javax.jmdns.impl;

import java.util.Set;

import javax.jmdns.ServiceInfo;
import javax.jmdns.impl.constants.DNSConstants;
import javax.jmdns.impl.constants.DNSRecordClass;
import javax.jmdns.impl.constants.DNSRecordType;
import javax.jmdns.impl.constants.DNSState;

/**
 * A DNS question.
 *
 * @version %I%, %G%
 * @author Arthur van Hoff, Pierre Frisch
 */
public class DNSQuestion extends DNSEntry
{
    // private static Logger logger = Logger.getLogger(DNSQuestion.class.getName());

    /**
     * Address question.
     */
    private static class DNS4Address extends DNSQuestion
    {
        DNS4Address(String name, DNSRecordType type, DNSRecordClass recordClass, boolean unique)
        {
            super(name, type, recordClass, unique);
        }

        @Override
        public void addAnswers(JmDNSImpl jmDNSImpl, Set<DNSRecord> answers)
        {
            DNSRecord answer = jmDNSImpl.getLocalHost().getDNS4AddressRecord();
            if (answer != null)
            {
                answers.add(answer);
            }
        }

    }

    /**
     * Address question.
     */
    private static class DNS6Address extends DNSQuestion
    {
        DNS6Address(String name, DNSRecordType type, DNSRecordClass recordClass, boolean unique)
        {
            super(name, type, recordClass, unique);
        }

        @Override
        public void addAnswers(JmDNSImpl jmDNSImpl, Set<DNSRecord> answers)
        {
            DNSRecord answer = jmDNSImpl.getLocalHost().getDNS6AddressRecord();
            if (answer != null)
            {
                answers.add(answer);
            }
        }

    }

    /**
     * Host Information question.
     */
    private static class HostInformation extends DNSQuestion
    {
        HostInformation(String name, DNSRecordType type, DNSRecordClass recordClass, boolean unique)
        {
            super(name, type, recordClass, unique);
        }
    }

    /**
     * Pointer question.
     */
    private static class Pointer extends DNSQuestion
    {
        Pointer(String name, DNSRecordType type, DNSRecordClass recordClass, boolean unique)
        {
            super(name, type, recordClass, unique);
        }

        @Override
        public void addAnswers(JmDNSImpl jmDNSImpl, Set<DNSRecord> answers)
        {
            // find matching services
            for (ServiceInfo serviceInfo : jmDNSImpl.getServices().values())
            {
                this.addAnswersForServiceInfo(jmDNSImpl, answers, (ServiceInfoImpl) serviceInfo);
            }
            if (this.getName().equalsIgnoreCase("_services._mdns._udp.local."))
            {
                for (String serviceType : jmDNSImpl.getServiceTypes().values())
                {
                    answers.add(new DNSRecord.Pointer("_services._mdns._udp.local.", DNSRecordType.TYPE_PTR, DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE, DNSConstants.DNS_TTL, serviceType));
                }
            }
        }

    }

    /**
     * Service question.
     */
    private static class Service extends DNSQuestion
    {
        Service(String name, DNSRecordType type, DNSRecordClass recordClass, boolean unique)
        {
            super(name, type, recordClass, unique);
        }

        @Override
        public void addAnswers(JmDNSImpl jmDNSImpl, Set<DNSRecord> answers)
        {
            // I am not sure of why there is a special case here [PJYF Oct 15 2004]
            if (jmDNSImpl.getLocalHost().getName().equalsIgnoreCase(this.getName()))
            {
                // type = DNSConstants.TYPE_A;
                DNSRecord answer = jmDNSImpl.getLocalHost().getDNS4AddressRecord();
                if (answer != null)
                {
                    answers.add(answer);
                }
                answer = jmDNSImpl.getLocalHost().getDNS6AddressRecord();
                if (answer != null)
                {
                    answers.add(answer);
                }
                return;
            }
            // Service type request
            if (jmDNSImpl.getServiceTypes().containsKey(this.getName().toLowerCase()))
            {
                DNSQuestion question = new Pointer(this.getName(), DNSRecordType.TYPE_PTR, this.getRecordClass(), this.isUnique());
                question.addAnswers(jmDNSImpl, answers);
                return;
            }

            this.addAnswersForServiceInfo(jmDNSImpl, answers, (ServiceInfoImpl) jmDNSImpl.getServices().get(this.getName().toLowerCase()));
        }

    }

    /**
     * Text question.
     */
    private static class Text extends DNSQuestion
    {
        Text(String name, DNSRecordType type, DNSRecordClass recordClass, boolean unique)
        {
            super(name, type, recordClass, unique);
        }

        @Override
        public void addAnswers(JmDNSImpl jmDNSImpl, Set<DNSRecord> answers)
        {
            this.addAnswersForServiceInfo(jmDNSImpl, answers, (ServiceInfoImpl) jmDNSImpl.getServices().get(this.getName().toLowerCase()));
        }

    }

    /**
     * AllRecords question.
     */
    private static class AllRecords extends DNSQuestion
    {
        AllRecords(String name, DNSRecordType type, DNSRecordClass recordClass, boolean unique)
        {
            super(name, type, recordClass, unique);
        }

        @Override
        public boolean isSameType(DNSEntry entry)
        {
            // We match all non null entry
            return (entry != null);
        }

        @Override
        public void addAnswers(JmDNSImpl jmDNSImpl, Set<DNSRecord> answers)
        {
            // I am not sure of why there is a special case here [PJYF Oct 15 2004]
            if (jmDNSImpl.getLocalHost().getName().equalsIgnoreCase(this.getName()))
            {
                // type = DNSConstants.TYPE_A;
                DNSRecord answer = jmDNSImpl.getLocalHost().getDNS4AddressRecord();
                if (answer != null)
                {
                    answers.add(answer);
                }
                answer = jmDNSImpl.getLocalHost().getDNS6AddressRecord();
                if (answer != null)
                {
                    answers.add(answer);
                }
                return;
            }
            // Service type request
            if (jmDNSImpl.getServiceTypes().containsKey(this.getName().toLowerCase()))
            {
                DNSQuestion question = new Pointer(this.getName(), DNSRecordType.TYPE_PTR, this.getRecordClass(), this.isUnique());
                question.addAnswers(jmDNSImpl, answers);
                return;
            }

            this.addAnswersForServiceInfo(jmDNSImpl, answers, (ServiceInfoImpl) jmDNSImpl.getServices().get(this.getName().toLowerCase()));
        }

    }

    DNSQuestion(String name, DNSRecordType type, DNSRecordClass recordClass, boolean unique)
    {
        super(name, type, recordClass, unique);
    }

    /**
     * Create a question.
     *
     * @param name
     * @param type
     * @param recordClass
     * @param unique
     * @return new question
     */
    public static DNSQuestion newQuestion(String name, DNSRecordType type, DNSRecordClass recordClass, boolean unique)
    {
        switch (type)
        {
            case TYPE_A:
                return new DNS4Address(name, type, recordClass, unique);
            case TYPE_A6:
                return new DNS6Address(name, type, recordClass, unique);
            case TYPE_AAAA:
                return new DNS6Address(name, type, recordClass, unique);
            case TYPE_ANY:
                return new AllRecords(name, type, recordClass, unique);
            case TYPE_HINFO:
                return new HostInformation(name, type, recordClass, unique);
            case TYPE_PTR:
                return new Pointer(name, type, recordClass, unique);
            case TYPE_SRV:
                return new Service(name, type, recordClass, unique);
            case TYPE_TXT:
                return new Text(name, type, recordClass, unique);
            default:
                return new DNSQuestion(name, type, recordClass, unique);
        }
    }

    /**
     * Check if this question is answered by a given DNS record.
     */
    boolean answeredBy(DNSEntry rec)
    {
        return this.isSameRecordClass(rec) && this.isSameType(rec) && this.getName().equals(rec.getName());
    }

    /**
     * Adds answers to the list for our question.
     *
     * @param jmDNSImpl
     * @param answers
     *            List of previous answer to append.
     */
    public void addAnswers(JmDNSImpl jmDNSImpl, Set<DNSRecord> answers)
    {
        // By default we do nothing
    }

    protected void addAnswersForServiceInfo(JmDNSImpl jmDNSImpl, Set<DNSRecord> answers, ServiceInfoImpl info)
    {
        if ((info != null) && (info.getState() == DNSState.ANNOUNCED))
        {
            if (this.getName().equalsIgnoreCase(info.getType()))
            {
                DNSRecord answer = jmDNSImpl.getLocalHost().getDNS4AddressRecord();
                if (answer != null)
                {
                    answers.add(answer);
                }
                answer = jmDNSImpl.getLocalHost().getDNS6AddressRecord();
                if (answer != null)
                {
                    answers.add(answer);
                }
                answers.add(new DNSRecord.Pointer(info.getType(), DNSRecordType.TYPE_PTR, DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE, DNSConstants.DNS_TTL, info.getQualifiedName()));
                answers.add(new DNSRecord.Service(info.getQualifiedName(), DNSRecordType.TYPE_SRV, DNSRecordClass.CLASS_IN, DNSRecordClass.UNIQUE, DNSConstants.DNS_TTL, info.getPriority(), info.getWeight(), info.getPort(), jmDNSImpl.getLocalHost()
                        .getName()));
                answers.add(new DNSRecord.Text(info.getQualifiedName(), DNSRecordType.TYPE_TXT, DNSRecordClass.CLASS_IN, DNSRecordClass.UNIQUE, DNSConstants.DNS_TTL, info.getText()));
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.impl.DNSEntry#isExpired(long)
     */
    @Override
    boolean isExpired(long now)
    {
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.impl.DNSEntry#toString(java.lang.StringBuilder)
     */
    @Override
    public void toString(StringBuilder aLog)
    {
        // do nothing
    }

}
