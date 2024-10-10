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
// Copyright 2003-2005 Arthur van Hoff Rick Blair

package javax.jmdns.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.jmdns.impl.constants.DNSRecordClass;
import javax.jmdns.impl.constants.DNSRecordType;
import javax.jmdns.impl.util.SimpleLockManager;
import javax.jmdns.impl.util.SimpleLockManager.Locked;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * @author Arthur van Hoff, Werner Randelshofer, Rick Blair, Pierre Frisch
 */
public class DNSCache extends ConcurrentHashMap<String, List<DNSEntry>> {

    private static final Logger       logger              = LoggerFactory.getLogger(DNSCache.class);

    private static final long   serialVersionUID    = 3024739453186759259L;
    
    private final transient SimpleLockManager _lm = new SimpleLockManager();

    /**
     *
     */
    public DNSCache() {
        this(1024);
    }

    /**
     * @param map
     */
    public DNSCache(DNSCache map) {
        this(map != null ? map.size() : 1024);
        if (map != null) {
            this.putAll(map);
        }
    }

    /**
     * Create a table with a given initial size.
     *
     * @param initialCapacity
     */
    public DNSCache(int initialCapacity) {
        super(initialCapacity);
    }

    // ====================================================================
    // Map

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        return new DNSCache(this);
    }

    // ====================================================================

    /**
     * Returns all entries in the cache
     *
     * @return all entries in the cache
     */
    public Collection<DNSEntry> allValues() {
        List<DNSEntry> allValues = new ArrayList<>();
        Set<Entry<String, List<DNSEntry>>> entries = this.entrySet();
        for (Entry<String, List<DNSEntry>> entry : entries) {
            if (entry == null)
                continue;
            String key = entry.getKey();
            try (Locked lock = _lm.lock(key)) {
                List<DNSEntry> entryList = entry.getValue();
                if (entryList != null)
                    allValues.addAll(entryList);
            }
        }
        return allValues;
    }

    /**
     * Iterate only over items with matching name. Returns a list of DNSEntry or null. To retrieve all entries, one must iterate over this linked list.
     *
     * @param name
     * @return list of DNSEntries
     */
    public Collection<? extends DNSEntry> getDNSEntryList(String name) {
        if (name == null)
            return Collections.emptyList();
        String key = name.toLowerCase();
        try (Locked lock = _lm.lock(key)) {
            List<DNSEntry> entryList = getEntryListOrEmpty(key);
            if (entryList.isEmpty())
                return Collections.emptyList();
            return new ArrayList<>(entryList);
        }
    }

    /**
     * Get a matching DNS entry from the table (using isSameEntry). Returns the entry that was found.
     *
     * @param dnsEntry
     * @return DNSEntry
     */
    public DNSEntry getDNSEntry(DNSEntry dnsEntry) {
        if (dnsEntry == null)
            return  null;
        String key = dnsEntry.getKey();
        try (Locked lock = _lm.lock(key)) {
            List<DNSEntry> entryList = getEntryListOrEmpty(key);
            for (DNSEntry testDNSEntry : entryList) {
                if (testDNSEntry.isSameEntry(dnsEntry))
                    return testDNSEntry;
            }
        }
        return null;
    }

    /**
     * Get a matching DNS entry from the table.
     *
     * @param name
     * @param type
     * @param recordClass
     * @return DNSEntry
     */
    public DNSEntry getDNSEntry(String name, DNSRecordType type, DNSRecordClass recordClass) {
        if (name == null)
            return null;
        
        String key = name.toLowerCase();
        try (Locked lock = _lm.lock(key)) {
            List<DNSEntry> entryList = getEntryListOrEmpty(key);
            for (DNSEntry testDNSEntry : entryList) {
                if (testDNSEntry.matchRecordType(type) && testDNSEntry.matchRecordClass(recordClass))
                    return testDNSEntry;
            }
        }
        return null;
    }
    
    /**
     * Get all matching DNS entries from the table.
     *
     * @param name
     * @param type
     * @param recordClass
     * @return list of entries
     */
    public Collection<? extends DNSEntry> getDNSEntryList(String name, DNSRecordType type, DNSRecordClass recordClass) {
        if (name == null)
            return Collections.emptyList();
        
        String key = name.toLowerCase();
        ArrayList<DNSEntry> result;
        try (Locked lock = _lm.lock(key)) {
            List<DNSEntry> entryList = getEntryListOrEmpty(key);
            result = new ArrayList<>(entryList);
        }
        // remove records that do not match
        result.removeIf(testDNSEntry -> !testDNSEntry.matchRecordType(type) || (!testDNSEntry.matchRecordClass(recordClass)));
        return result;
    }

    /**
     * Adds an entry to the table.
     *
     * @param dnsEntry
     * @return true if the entry was added
     */
    public boolean addDNSEntry(DNSEntry dnsEntry) {
        if (dnsEntry == null)
            return false;
        String key = dnsEntry.getKey();
        try (Locked lock = _lm.lock(key)) {
            List<DNSEntry> entryList = this.get(key);
            if (entryList == null) {
                entryList = new ArrayList<>(3);
            }
            entryList.add(dnsEntry);
            // re-add to the map to establish happens-before and aid visibility
            this.put(key, entryList);
        }
        return true;
    }

    /**
     * Removes a specific entry from the table. Returns true if the entry was found.
     *
     * @param dnsEntry
     * @return true if the entry was removed
     */
    public boolean removeDNSEntry(DNSEntry dnsEntry) {
        if (dnsEntry == null)
            return false;
        String key = dnsEntry.getKey();
        try (Locked lock = _lm.lock(key)) {
            List<DNSEntry> entryList = this.get(key);
            if (entryList == null)
                return false;
            boolean result = entryList.remove(dnsEntry);
            /* Remove from DNS cache when no records remain with this key */
            if (entryList.isEmpty()) {
                this.remove(key);
            } else {
                // re-add to the map to establish happens-before and aid visibility
                this.put(key, entryList);
            }
            return result;
        }
    }

    /**
     * Replace an existing entry by a new one.<br/>
     * <b>Note:</b> the 2 entries must have the same key.
     *
     * @param newDNSEntry
     * @param existingDNSEntry
     * @return <code>true</code> if the entry has been replaced, <code>false</code> otherwise.
     */
    public boolean replaceDNSEntry(DNSEntry newDNSEntry, DNSEntry existingDNSEntry) {
        if (newDNSEntry == null || existingDNSEntry == null || !newDNSEntry.getKey().equals(existingDNSEntry.getKey()))
            return false;
        String key = newDNSEntry.getKey();
        try (Locked lock = _lm.lock(key)) {
            List<DNSEntry> entryList = this.get(key);
            if (entryList == null) {
                entryList = new ArrayList<>(3);
            } else {
                entryList.remove(existingDNSEntry);
            }
            entryList.add(newDNSEntry);
            // re-add to the map to establish happens-before and aid visibility
            this.put(key, entryList);
            return true;
        }
    }
    
    private List<DNSEntry> getEntryListOrEmpty(String key) {
        List<DNSEntry> entryList = this.get(key);
        if (entryList == null)
            return Collections.emptyList();
        return entryList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(2000);
        sb.append("\n\t---- cache ----");
        for (Map.Entry<String, List<DNSEntry>> entry : this.entrySet()) {
            sb.append("\n\n\t\tname '").append(entry.getKey()).append('\'');
            List<DNSEntry> entryList = entry.getValue();
            if (entryList == null)
                continue;
            String key = entry.getKey();
            try (Locked lock = _lm.lock(key)) {
                if (entryList.isEmpty()) {
                    sb.append(" : no entries");
                } else {
                    for (DNSEntry dnsEntry : entryList) {
                        sb.append("\n\t\t\t").append(dnsEntry.toString());
                    }
                }
            }
        }
        return sb.toString();
    }

    /**
     * Prints the content of the cache to the {@link #logger}.
     */
    public void logCachedContent() {
        if (!logger.isTraceEnabled()) {
            return;
        }

        logger.trace("Cached DNSEntries: {}", this);
    }

}