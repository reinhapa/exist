/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2001-03 Wolfgang M. Meier
 *  wolfgang@exist-db.org
 *  http://exist.sourceforge.net
 *  
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *  
 *  $Id$
 */
package org.exist.storage.cache;

import org.apache.log4j.Logger;

/**
 * Base interface for all cache implementations that are used for
 * buffering btree and data pages.
 * 
 * @author Wolfgang <wolfgang@exist-db.org>
 */
public interface Cache {
	
	/**
	 * Add the item to the cache. If it is already in the cache,
	 * update the references.
	 * 
	 * @param item
	 */
	public void add(Cacheable item);
	
	/**
	 * Add the item to the cache. If it is already in the cache,
	 * update the references.
	 * 
	 * @param item
	 * @param initialRefCount the initial reference count for the item
	 */
	public void add(Cacheable item, int initialRefCount);
	
	/**
	 * Retrieve an item from the cache.
	 * 
	 * @param item
	 * @return the item in the cache or null if it does not exist.
	 */
	public Cacheable get(Cacheable item);
	
	/**
	 * Retrieve an item by its key.
	 * 
	 * @param key a unique key, usually the page number
	 * @return the item in the cache or null if it does not exist.
	 */
	public Cacheable get(long key);
	
	/**
	 * Remove an item from the cache.
	 * 
	 * @param item
	 */
	public void remove(Cacheable item);
	
	public boolean hasDirtyItems();
	
	/**
	 * Call release on all items, but without
	 * actually removing them from the cache.
	 * 
	 * This gives the items a chance to write all
	 * unwritten data to disk.
	 */
	public void flush();
	
	/**
	 * Get the size of this cache.
	 * 
	 * @return
	 */
	public int getBuffers();
	
	/**
	 * Get the number of buffers currently used.
	 * 
	 * @return
	 */
	public int getUsedBuffers();
	
	/**
	 * Get the number of times where an object has been successfully
	 * loaded from the cache.
	 * @return
	 */
	public int getHits();
	
	/**
	 * Get the number of times where an object could not be
	 * found in the cache.
	 * 
	 * @return
	 */
	public int getFails();
	
	public void setFileName(String fileName);
	
	public final static Logger LOG = Logger.getLogger(Cache.class);
}