// Copyright (C) 2002  Strangeberry Inc.
// @(#)DNSCache.java, 1.11, 11/29/2002
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
// 
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
// 
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

package com.strangeberry.jmdns;

import java.util.*;

/**
 * A table of DNS entries. This is a closed hash table which
 * can handle multiple entries with the same name. It provides
 * iterators to efficiently iterate over all the entries with
 * a given name.
 *
 * @author	Arthur van Hoff
 * @version 	1.11, 11/29/2002
 */
class DNSCache
{
    final static float LOAD_FACTOR = 0.75f;

    DNSEntry entries[];
    int count;
    float loadFactor;

    /**
     * Create a table with a given initial size.
     */
    DNSCache(int size)
    {
	size = Math.max(2, size * 2);
	for (; (size % 2 == 0) || (size % 3 == 0) || (size % 5 == 0) ; size++);
	this.entries = new DNSEntry[size];
    }

    /**
     * Add an entry to the table. The table will be grown
     * if it is more than 75% full.
     */
    void add(DNSEntry entry)
    {
	// rehash if necessary
	if (count >= (entries.length * LOAD_FACTOR)) {
	    entries = rehash(entries, entries.length * 2);
	}
	add(entries, entry);
	count++;
    }

    /**
     * Add an entry to a table.
     */
    private void add(DNSEntry entries[], DNSEntry entry)
    {
	int i = Math.abs(entry.key.hashCode()) % entries.length;
	while (entries[i] != null) {
	    i = ((i == 0) ? entries.length : i) - 1;
	}
	entries[i] = entry;
    }

    /**
     * Rehash a table.
     */
    private DNSEntry[] rehash(DNSEntry entries[], int size)
    {
	for (; (size % 2 == 0) || (size % 3 == 0) || (size % 5 == 0) ; size++);
	
	DNSEntry newentries[] = new DNSEntry[size];
	for (int i = 0, n = entries.length ; i < n ; i++) {
	    if (entries[i] != null) {
		add(newentries, entries[i]);
	    }
	}
	return newentries;
    }

    /**
     * Remove a specific entry from the table. Returns true if the
     * entry was found.
     */
    boolean remove(DNSEntry entry)
    {
	int i = Math.abs(entry.key.hashCode()) % entries.length;
	while (true) {
	    DNSEntry e = entries[i];
	    if (e == null) {
		return false;
	    }
	    if (e == entry) {
		remove(i);
		return true;
	    }
	    if (--i < 0) {
		i = entries.length - 1;
	    }
	}
    }

    /**
     * Remove entry at a given index. Reshuffle as needed.
     */
    private void remove(int i)
    {
	int empty = i;
	entries[empty] = null;
	count--;
	while (true) {
	    if (--i < 0) {
		i = entries.length - 1;
	    }
	    DNSEntry e = entries[i];
	    if (e == null) {
		return;
	    }
	    int j = Math.abs(e.key.hashCode()) % entries.length;
	    if ((i < empty) ? ((j < i) || (j >= empty)) : ((j < i) && (j >= empty))) {
		entries[empty] = e;
		empty = i;
		entries[empty] = null;
	    }
	}
    }

    /**
     * Get a matching DNS entry from the table (using equals).
     * Returns the entry that was found.
     */
    DNSEntry get(DNSEntry entry)
    {
	int i = Math.abs(entry.key.hashCode()) % entries.length;
	while (true) {
	    DNSEntry e = entries[i];
	    if (e == null) {
		return null;
	    }
	    if (e.equals(entry)) {
		return e;
	    }
	    if (--i < 0) {
		i = entries.length - 1;
	    }
	}
    }

    /**
     * Get a matching DNS entry from the table.
     */
    DNSEntry get(String name, int type, int clazz)
    {
	String key = name.toLowerCase();
	int i = Math.abs(key.hashCode()) % entries.length;
	while (true) {
	    DNSEntry e = entries[i];
	    if (e == null) {
		return null;
	    }
	    if (key.equals(e.key) && (type == e.type) && (clazz == e.clazz)) {
		return e;
	    }
	    if (--i < 0) {
		i = entries.length - 1;
	    }
	}
    }

    /**
     * Iterate over all entries.
     */
    Iterator all()
    {
	return new IterateAll();
    }

    /**
     * Iterate only over items with matching name.
     */
    Iterator find(String name)
    {
	return new IterateKey(name.toLowerCase());
    }

    /**
     * Iterate over all entries.
     */
    private class IterateAll implements Iterator
    {
	int index;

	IterateAll()
	{
	    index = entries.length - 1;
	}
	
	public boolean hasNext()
	{
	    for (; index >= 0 ; index--) {
		if (entries[index] != null) {
		    return true;
		}
	    }
	    return false;
	}

	public Object next()
	{
	    return entries[index--];
	}

	public void remove()
	{
	    DNSCache.this.remove(++index);
	}
    }

    /**
     * Iterate over some entries.
     */
    private class IterateKey implements Iterator
    {
	String key;
	int index;

	IterateKey(String key)
	{
	    this.key = key;
	    this.index = Math.abs(key.hashCode()) % entries.length;
	}
	
	public boolean hasNext()
	{
	    while (true) {
		if (index < 0) {
		    index = entries.length - 1;
		}
		DNSEntry e = entries[index];
		if (e == null) {
		    return false;
		}
		if (e.key.equals(key)) {
		    return true;
		}
		index--;
	    }
	}

	public Object next()
	{
	    return entries[index--];
	}

	public void remove()
	{
	    DNSCache.this.remove(++index);
	}
    }
    

    /**
     * List all entries for debugging.
     */
    void print()
    {
	for (Iterator i = all() ; i.hasNext() ;) {
	    System.out.println(i.next());
	}
    }
}
