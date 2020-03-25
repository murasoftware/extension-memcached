package org.lucee.extension.cache.mc;

import java.io.Serializable;
import java.util.Date;

import lucee.commons.io.cache.CacheEntry;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.type.Struct;

public class MemcachedCacheEntry implements CacheEntry, Serializable {

	private static final long serialVersionUID = -1726639719761934902L;

	private String key;
	private Object value;
	private long created;
	private Long idleTime;

	public MemcachedCacheEntry(String key, Object value, Long idleTime, Long until) {
		this.key = key;
		this.value = value;
		this.created = System.currentTimeMillis();
	}

	@Override
	public Date created() {
		return CFMLEngineFactory.getInstance().getCreationUtil().createDate(created);
	}

	@Override
	public Struct getCustomInfo() {
		return CacheUtil.getInfo(this);
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
		return 0;
	}

	@Override
	public long idleTimeSpan() {
		if (idleTime == null)
			return Long.MIN_VALUE;
		return idleTime.longValue();
	}

	@Override
	public Date lastHit() {
		return null;
	}

	@Override
	public Date lastModified() {
		return null;
	}

	@Override
	public long liveTimeSpan() {
		return 0;
	}

	@Override
	public long size() {
		return 0;
	}

}
