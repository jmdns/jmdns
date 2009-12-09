//Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL

package javax.jmdns.impl;


/**
 * A DNS question.
 *
 * @version %I%, %G%
 * @author Arthur van Hoff
 */
public final class DNSQuestion extends DNSEntry
{
    // private static Logger logger = Logger.getLogger(DNSQuestion.class.getName());

    /**
     * Create a question.
     *
     * @param name
     * @param type
     * @param clazz
     */
    public DNSQuestion(String name, int type, int clazz)
    {
        super(name, type, clazz);
    }

    /**
     * Check if this question is answered by a given DNS record.
     */
    boolean answeredBy(DNSRecord rec)
    {
        return (_clazz == rec._clazz) && ((_type == rec._type) || (_type == DNSConstants.TYPE_ANY))
                && _name.equals(rec._name);
    }

    /**
     * For debugging only.
     */
    @Override
    public String toString()
    {
        return toString("question", null);
    }
}
