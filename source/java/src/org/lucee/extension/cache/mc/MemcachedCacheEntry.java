package org.lucee.extension.cache.mc;

import java.io.Serializable;
import java.util.Date;

import lucee.commons.io.cache.CacheEntry;
import lucee.runtime.type.Struct;

public class MemcachedCacheEntry implements CacheEntry, Serializable {

	private String key;
	private Object value;

	public MemcachedCacheEntry(String key, Object value) {
		this.key = key;
		this.value = value;
	}

	@Override
	public Date created() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Struct getCustomInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getKey() {
		return key;
	}

	@Override
	public Object getValue() {
		return value;
	}

	@Override
	public int hitCount() {
		return -1;
	}

	@Override
	public long idleTimeSpan() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Date lastHit() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Date lastModified() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long liveTimeSpan() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long size() {
		// TODO Auto-generated method stub
		return 0;
	}

}
