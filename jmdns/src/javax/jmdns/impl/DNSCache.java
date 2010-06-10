//Copyright 2003-2005 Arthur van Hoff Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL

package javax.jmdns.impl;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jmdns.impl.constants.DNSRecordClass;
import javax.jmdns.impl.constants.DNSRecordType;

/**
 * A table of DNS entries. This is a map table which can handle multiple entries with the same name.
 * <p/>
 * Storing multiple entries with the same name is implemented using a linked list. This is hidden from the user and can change in later implementation.
 * <p/>
 * Here's how to iterate over all entries:
 *
 * <pre>
 *       for (Iterator i=dnscache.allValues().iterator(); i.hasNext(); ) {
 *             DNSEntry entry = i.next();
 *             ...do something with entry...
 *       }
 * </pre>
 *
 * <p/>
 * And here's how to iterate over all entries having a given name:
 *
 * <pre>
 *       for (Iterator i=dnscache.getDNSEntryList(name).iterator(); i.hasNext(); ) {
 *             DNSEntry entry = i.next();
 *           ...do something with entry...
 *       }
 * </pre>
 *
 * @version %I%, %G%
 * @author Arthur van Hoff, Werner Randelshofer, Rick Blair, Pierre Frisch
 */
public class DNSCache extends AbstractMap<String, List<? extends DNSEntry>>
{

    // private static Logger logger = Logger.getLogger(DNSCache.class.getName());

    private transient Set<Map.Entry<String, List<? extends DNSEntry>>> _entrySet = null;

    /**
     *
     */
    public static final DNSCache EmptyCache = new _EmptyCache();

    static class _EmptyCache extends DNSCache
    {

        /*
         * (non-Javadoc)
         *
         * @see java.util.AbstractMap#size()
         */
        @Override
        public int size()
        {
            return 0;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.AbstractMap#isEmpty()
         */
        @Override
        public boolean isEmpty()
        {
            return true;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.AbstractMap#containsKey(java.lang.Object)
         */
        @Override
        public boolean containsKey(Object key)
        {
            return false;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.AbstractMap#containsValue(java.lang.Object)
         */
        @Override
        public boolean containsValue(Object value)
        {
            return false;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.AbstractMap#get(java.lang.Object)
         */
        @Override
        public List<DNSEntry> get(Object key)
        {
            return null;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.AbstractMap#keySet()
         */
        @Override
        public Set<String> keySet()
        {
            return Collections.emptySet();
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.AbstractMap#values()
         */
        @Override
        public Collection<List<? extends DNSEntry>> values()
        {
            return Collections.emptySet();
        }

        /*
         * (non-Javadoc)
         *
         * @see com.webobjects.discoveryservices.DNSCache#entrySet()
         */
        @Override
        public Set<Map.Entry<String, List<? extends DNSEntry>>> entrySet()
        {
            return Collections.emptySet();
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.AbstractMap#equals(java.lang.Object)
         */
        @SuppressWarnings("unchecked")
        @Override
        public boolean equals(Object o)
        {
            return (o instanceof Map) && ((Map) o).size() == 0;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.AbstractMap#put(java.lang.Object, java.lang.Object)
         */
        @Override
        public List<? extends DNSEntry> put(String key, List<? extends DNSEntry> value)
        {
            return null;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.AbstractMap#hashCode()
         */
        @Override
        public int hashCode()
        {
            return 0;
        }

    }

    /**
     *
     */
    protected static class _CacheEntry extends Object implements Map.Entry<String, List<? extends DNSEntry>>
    {

        private List<? extends DNSEntry> _value;

        private String _key;

        /**
         * @param key
         * @param value
         */
        protected _CacheEntry(String key, List<? extends DNSEntry> value)
        {
            super();
            _key = (key != null ? key.trim().toLowerCase() : null);
            _value = value;
        }

        /**
         * @param entry
         */
        protected _CacheEntry(Map.Entry<String, List<? extends DNSEntry>> entry)
        {
            super();
            if (entry instanceof _CacheEntry)
            {
                _key = ((_CacheEntry) entry).getKey();
                _value = ((_CacheEntry) entry).getValue();
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.Map$Entry#getKey()
         */
        public String getKey()
        {
            return (_key != null ? _key : "");
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.Map$Entry#getValue()
         */
        public List<? extends DNSEntry> getValue()
        {
            return _value;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.Map$Entry#setValue(java.lang.Object)
         */
        public List<? extends DNSEntry> setValue(List<? extends DNSEntry> value)
        {
            List<? extends DNSEntry> oldValue = _value;
            _value = value;
            return oldValue;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @SuppressWarnings("unchecked")
        @Override
        public boolean equals(Object entry)
        {
            if (!(entry instanceof Map.Entry))
                return false;
            return this.getKey().equals(((Map.Entry) entry).getKey()) && this.getValue().equals(((Map.Entry) entry).getValue());
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode()
        {
            return ((_key == null) ? 0 : _key.hashCode()) ^ ((_value == null) ? 0 : _value.hashCode());
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#toString()
         */
        @Override
        public synchronized String toString()
        {
            StringBuffer aLog = new StringBuffer();
            aLog.append("\n\t\tname '" + _key + "'");
            if (_value != null)
            {
                for (DNSEntry entry : _value)
                {
                    aLog.append("\n\t\t\t" + entry.toString());
                }
            }
            else
            {
                aLog.append(" no entries");
            }
            return aLog.toString();
        }
    }

    /**
     *
     */
    public DNSCache()
    {
        this(1024);
    }

    /**
     * @param map
     */
    public DNSCache(DNSCache map)
    {
        this(map != null ? map.size() : 1024);
        if (map != null)
        {
            this.putAll(map);
        }
    }

    /**
     * Create a table with a given initial size.
     *
     * @param initialCapacity
     */
    public DNSCache(int initialCapacity)
    {
        super();
        _entrySet = new HashSet<Map.Entry<String, List<? extends DNSEntry>>>(initialCapacity);
    }

    // ====================================================================
    // Map

    /*
     * (non-Javadoc)
     *
     * @see java.util.AbstractMap#entrySet()
     */
    @Override
    public Set<Map.Entry<String, List<? extends DNSEntry>>> entrySet()
    {
        if (_entrySet == null)
            _entrySet = new HashSet<Map.Entry<String, List<? extends DNSEntry>>>();
        return _entrySet;
    }

    /**
     * @param key
     * @return map entry for the key
     */
    protected Map.Entry<String, List<? extends DNSEntry>> getEntry(String key)
    {
        String stringKey = (key != null ? key.trim().toLowerCase() : null);
        for (Map.Entry<String, List<? extends DNSEntry>> entry : this.entrySet())
        {
            if (stringKey != null)
            {
                if (stringKey.equals(entry.getKey()))
                    return entry;
            }
            else
            {
                if (entry.getKey() == null)
                    return entry;
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.AbstractMap#put(java.lang.Object, java.lang.Object)
     */
    @Override
    public List<? extends DNSEntry> put(String key, List<? extends DNSEntry> value)
    {
        synchronized (this)
        {
            List<? extends DNSEntry> oldValue = null;
            Map.Entry<String, List<? extends DNSEntry>> oldEntry = this.getEntry(key);
            if (oldEntry != null)
            {
                oldValue = oldEntry.setValue(value);
            }
            else
            {
                this.entrySet().add(new _CacheEntry(key, value));
            }
            return oldValue;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.AbstractMap#clone()
     */
    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        return new DNSCache(this);
    }

    // ====================================================================

    /**
     * Returns all entries in the cache
     *
     * @return all entries in the cache
     */
    public synchronized Collection<DNSEntry> allValues()
    {
        List<DNSEntry> allValues = new ArrayList<DNSEntry>();
        for (List<? extends DNSEntry> entry : this.values())
        {
            if (entry != null)
                allValues.addAll(entry);
        }
        return allValues;
    }

    /**
     * Iterate only over items with matching name. Returns an list of DNSEntry or null. To retrieve all entries, one must iterate over this linked list.
     *
     * @param name
     * @return list of DNSEntries
     */
    public synchronized Collection<? extends DNSEntry> getDNSEntryList(String name)
    {
        return this.get(name != null ? name.toLowerCase() : null);
    }

    /**
     * Get a matching DNS entry from the table (using isSameEntry). Returns the entry that was found.
     *
     * @param dnsEntry
     * @return DNSEntry
     */
    public synchronized DNSEntry getDNSEntry(DNSEntry dnsEntry)
    {
        DNSEntry result = null;
        if (dnsEntry != null)
        {
            Collection<? extends DNSEntry> entryList = this.getDNSEntryList(dnsEntry.getKey());
            if (entryList != null)
            {
                for (DNSEntry testDNSEntry : entryList)
                {
                    if (dnsEntry.isSameEntry(testDNSEntry))
                    {
                        result = testDNSEntry;
                        break;
                    }
                }
            }
        }
        return result;
    }

    /**
     * Get a matching DNS entry from the table.
     *
     * @param name
     * @param type
     * @param recordClass
     * @return DNSEntry
     */
    public synchronized DNSEntry getDNSEntry(String name, DNSRecordType type, DNSRecordClass recordClass)
    {
        DNSEntry result = null;
        Collection<? extends DNSEntry> entryList = this.getDNSEntryList(name);
        if (entryList != null)
        {
            for (DNSEntry testDNSEntry : entryList)
            {
                if (testDNSEntry.getRecordType().equals(type) && ((DNSRecordClass.CLASS_ANY == recordClass) || testDNSEntry.getRecordClass().equals(recordClass)))
                {
                    result = testDNSEntry;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Adds an entry to the table.
     *
     * @param dnsEntry
     * @return true if the entry was added
     */
    public synchronized boolean addDNSEntry(final DNSEntry dnsEntry)
    {
        boolean result = false;
        if (dnsEntry != null)
        {
            Map.Entry<String, List<? extends DNSEntry>> oldEntry = this.getEntry(dnsEntry.getKey());

            List<DNSEntry> aNewValue = null;
            if (oldEntry != null)
            {
                aNewValue = new ArrayList<DNSEntry>(oldEntry.getValue());
            }
            else
            {
                aNewValue = new ArrayList<DNSEntry>();
            }
            aNewValue.add(dnsEntry);

            if (oldEntry != null)
            {
                oldEntry.setValue(aNewValue);
            }
            else
            {
                this.entrySet().add(new _CacheEntry(dnsEntry.getKey(), aNewValue));
            }
            // This is probably not very informative
            result = true;
        }
        return result;
    }

    /**
     * Removes a specific entry from the table. Returns true if the entry was found.
     *
     * @param dnsEntry
     * @return true if the entry was removed
     */
    public synchronized boolean removeDNSEntry(DNSEntry dnsEntry)
    {
        boolean result = false;
        if (dnsEntry != null)
        {
            Collection<? extends DNSEntry> entry = this.getDNSEntryList(dnsEntry.getKey());
            if (entry != null)
            {
                result = entry.remove(dnsEntry);
                // If we just removed the last one we need to get rid of the entry
                if (entry.size() == 0)
                    this.remove(dnsEntry.getKey());
            }
        }
        return result;
    }

    /**
     * Replace an existing entry by a new one.<br/>
     * <b>Note:</b> the 2 entries must have the same key.
     *
     * @param newDNSEntry
     * @param existingDNSEntry
     * @return <code>true</code> if the entry has been replace, <code>false</code> otherwise.
     */
    public synchronized boolean replaceDNSEntry(DNSEntry newDNSEntry, DNSEntry existingDNSEntry)
    {
        boolean result = false;
        if ((newDNSEntry != null) && (existingDNSEntry != null) && (newDNSEntry.getKey().equals(existingDNSEntry.getKey())))
        {
            Map.Entry<String, List<? extends DNSEntry>> oldEntry = this.getEntry(newDNSEntry.getKey());

            List<DNSEntry> aNewValue = null;
            if (oldEntry != null)
            {
                aNewValue = new ArrayList<DNSEntry>(oldEntry.getValue());
            }
            else
            {
                aNewValue = new ArrayList<DNSEntry>();
            }
            aNewValue.remove(existingDNSEntry);
            aNewValue.add(newDNSEntry);

            if (oldEntry != null)
            {
                oldEntry.setValue(aNewValue);
            }
            else
            {
                this.entrySet().add(new _CacheEntry(newDNSEntry.getKey(), aNewValue));
            }
            // This is probably not very informative
            result = true;
        }
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.AbstractMap#toString()
     */
    @Override
    public synchronized String toString()
    {
        StringBuffer aLog = new StringBuffer();
        aLog.append("\t---- cache ----");
        for (Map.Entry<String, List<? extends DNSEntry>> entry : this.entrySet())
        {
            aLog.append("\n\t\t" + entry.toString());
        }
        return aLog.toString();
    }

}
