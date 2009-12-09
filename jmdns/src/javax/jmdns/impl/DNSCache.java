//Copyright 2003-2005 Arthur van Hoff Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL

package javax.jmdns.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * A table of DNS entries. This is a hash table which can handle multiple entries with the same name.
 * <p/>
 * Storing multiple entries with the same name is implemented using a linked list of <code>CacheNode</code>'s.
 * <p/>
 * The current implementation of the API of DNSCache does expose the cache nodes to clients. Clients must explicitly
 * deal with the nodes when iterating over entries in the cache. Here's how to iterate over all entries in the cache:
 *
 * <pre>
 * for (Iterator i=dnscache.iterator(); i.hasNext(); ) {
 *    for (DNSCache.CacheNode n = (DNSCache.CacheNode) i.next(); n != null; n.next()) {
 *       DNSEntry entry = n.getValue();
 *       ...do something with entry...
 *    }
 * }
 * </pre>
 *
 * <p/>
 * And here's how to iterate over all entries having a given name:
 *
 * <pre>
 * for (DNSCache.CacheNode n = (DNSCache.CacheNode) dnscache.find(name); n != null; n.next()) {
 *     DNSEntry entry = n.getValue();
 *     ...do something with entry...
 * }
 * </pre>
 *
 * @version %I%, %G%
 * @author Arthur van Hoff, Werner Randelshofer, Rick Blair
 */
public class DNSCache
{
    // private static Logger logger = Logger.getLogger(DNSCache.class.getName());

    // Implementation note:
    // We might completely hide the existence of CacheNode's in a future version
    // of DNSCache. But this will require to implement two (inner) classes for
    // the iterators that will be returned by method <code>iterator()</code> and
    // method <code>find(name)</code>.
    // Since DNSCache is not a public class, it does not seem worth the effort
    // to clean its API up that much.

    // [PJYF Oct 15 2004] This should implements Collections that would be a much cleaner implementation

    /**
     * The hashtable used internally to store the entries of the cache. Keys are instances of String. The String
     * contains an unqualified service name. Values are linked lists of CacheNode instances.
     */
    private final HashMap hashtable;

    /**
     * Cache nodes are used to implement storage of multiple DNSEntry's of the same name in the cache.
     */
    public static class CacheNode
    {
        private final DNSEntry _value;
        private CacheNode _next;

        public CacheNode(DNSEntry value)
        {
            this._value = value;
        }

        public CacheNode next()
        {
            return _next;
        }

        public DNSEntry getValue()
        {
            return _value;
        }
    }

    /**
     * Create a table with a given initial size.
     *
     * @param size
     *            Initial cache size
     */
    public DNSCache(final int size)
    {
        hashtable = new HashMap(size);
    }

    /**
     * Clears the cache.
     */
    public synchronized void clear()
    {
        hashtable.clear();
    }

    /**
     * Adds an entry to the table.
     *
     * @param entry
     *            entry to add
     */
    public synchronized void add(final DNSEntry entry)
    {
        // logger.log("DNSCache.add("+entry.getName()+")");
        final CacheNode newValue = new CacheNode(entry);
        final CacheNode node = (CacheNode) hashtable.get(entry.getName());
        if (node == null)
        {
            hashtable.put(entry.getName(), newValue);
        }
        else
        {
            newValue._next = node._next;
            node._next = newValue;
        }
    }

    /**
     * Remove a specific entry from the table. Returns true if the entry was found.
     *
     * @param entry
     *            entry to remove
     * @return <code>true</code> if the entry was removed, <code>false</code> otherwise.
     */
    public synchronized boolean remove(DNSEntry entry)
    {
        CacheNode node = (CacheNode) hashtable.get(entry.getName());
        if (node != null)
        {
            if (node._value == entry)
            {
                if (node._next == null)
                {
                    hashtable.remove(entry.getName());
                }
                else
                {
                    hashtable.put(entry.getName(), node._next);
                }
                return true;
            }

            CacheNode previous = node;
            node = node._next;
            while (node != null)
            {
                if (node._value == entry)
                {
                    previous._next = node._next;
                    return true;
                }
                previous = node;
                node = node._next;
            }
        }
        return false;
    }

    /**
     * Get a matching DNS entry from the table (using equals). Returns the entry that was found.
     *
     * @param entry
     *            entry to match
     * @return cached DNS entry.
     */
    public synchronized DNSEntry get(DNSEntry entry)
    {
        for (CacheNode node = find(entry.getName()); node != null; node = node._next)
        {
            if (node._value.equals(entry))
            {
                return node._value;
            }
        }
        return null;
    }

    /**
     * Get a matching DNS entry from the table.
     *
     * @param name
     *            service name
     * @param type
     *            service type
     * @param clazz
     *            service class
     * @return cached DNS entry.
     */
    public synchronized DNSEntry get(String name, int type, int clazz)
    {
        for (CacheNode node = find(name); node != null; node = node._next)
        {
            if (node._value._type == type && node._value._clazz == clazz)
            {
                return node._value;
            }
        }
        return null;
    }

    /**
     * Iterates over all cache nodes. The iterator returns instances of DNSCache.CacheNode. Each instance returned is
     * the first node of a linked list. To retrieve all entries, one must iterate over this linked list. See code
     * snippets in the header of the class.
     *
     * @return node iterator
     */
    public synchronized Iterator iterator()
    {
        return new ArrayList(hashtable.values()).iterator();
    }

    /**
     * Iterate only over items with matching name. Returns an instance of DNSCache.CacheNode or null. If an instance is
     * returned, it is the first node of a linked list. To retrieve all entries, one must iterate over this linked list.
     *
     * @param name
     *            Entry to find
     * @return cached node
     */
    public synchronized CacheNode find(String name)
    {
        return (CacheNode) hashtable.get(name);
    }

    /**
     * List all entries for debugging.
     */
    public synchronized void print()
    {
        for (final Iterator i = iterator(); i.hasNext();)
        {
            for (CacheNode n = (CacheNode) i.next(); n != null; n = n._next)
            {
                System.out.println(n._value);
            }
        }
    }

    @Override
    public synchronized String toString()
    {
        final StringBuffer aLog = new StringBuffer();
        aLog.append("\t---- cache ----");
        for (final Iterator i = iterator(); i.hasNext();)
        {
            for (CacheNode n = (CacheNode) i.next(); n != null; n = n._next)
            {
                aLog.append("\n\t\t" + n._value);
            }
        }
        return aLog.toString();
    }

}
