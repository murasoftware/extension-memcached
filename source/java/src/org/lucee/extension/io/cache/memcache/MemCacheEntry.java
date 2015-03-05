/**
 *
 * Copyright (c) 2015, Lucee Association Switzerland. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either 
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public 
 * License along with this library.  If not, see <http://www.gnu.org/licenses/>.
 * 
 **/
package org.lucee.extension.io.cache.memcache;

import java.io.Serializable;
import java.util.Date;

import lucee.commons.io.cache.CacheEntry;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.type.Struct;

import org.lucee.extension.io.cache.CacheUtil;

public class MemCacheEntry implements CacheEntry,Serializable {

	private static final long serialVersionUID = -9099108476585093711L;

	private String key;
	private Object value;
	private long created;
	private Long idleTime;
	//private Long until;

	public MemCacheEntry(String key, Object value, Long idleTime, Long until) {
		this.key=key;
		this.value=value;
		this.idleTime=idleTime;
		//this.until=until;
		this.created=System.currentTimeMillis();
	}

	/**
	 * @see railo.commons.io.cache.CacheEntry#created()
	 */
	public Date created() {
		return CFMLEngineFactory.getInstance().getCreationUtil().createDate(created);
	}

	/**
	 * @see railo.commons.io.cache.CacheEntry#getKey()
	 */
	public String getKey() {
		return key;
	}

	/**
	 * @see railo.commons.io.cache.CacheEntry#getValue()
	 */
	public Object getValue() {
		return value;
	}

	public int hitCount() {
		return 0;
	}

	public long idleTimeSpan() {
		if(idleTime==null)return Long.MIN_VALUE;
		return idleTime.longValue();
	}

	public Date lastHit() {
		return null;
	}

	public Date lastModified() {
		return null;
	}

	public long liveTimeSpan() {
		return 0;
	}

	public long size() {
		return 0;
	}

	public Struct getCustomInfo() {
		return CacheUtil.getInfo(this);
	}

}
