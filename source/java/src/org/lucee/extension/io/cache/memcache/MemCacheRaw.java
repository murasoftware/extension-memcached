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

import java.io.IOException;
import java.util.List;

import lucee.commons.io.cache.Cache;
import lucee.commons.io.cache.CacheEntry;
import lucee.commons.io.cache.CacheEntryFilter;
import lucee.commons.io.cache.CacheKeyFilter;
import lucee.commons.io.cache.exp.CacheException;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.config.Config;
import lucee.runtime.exp.PageException;
import lucee.runtime.type.Struct;
import lucee.runtime.util.Cast;

import org.lucee.extension.io.cache.CacheKeyFilterAll;
import org.lucee.extension.io.cache.CacheUtil;

import com.whalin.MemCached.MemCachedClient;
import com.whalin.MemCached.SockIOPool;

public class MemCacheRaw implements Cache {

	
	
	MemCachedClient _cache = null;
	private int hits;
	private int misses;
	//private Config config;
	private Struct arguments;
	private String cacheName;
	private SockIOPool pool;
	
	
	public static void init(Config config,String[] cacheNames,Struct[] arguments) throws IOException {
	}


	/**
	 * @throws IOException 
	 * @see railo.commons.io.cache.Cache#init(java.lang.String, railo.runtime.type.Struct)
	 * @deprecated use instead init(Config config,String cacheName,Struct arguments)
	 */
	public void init(String cacheName,Struct arguments) throws IOException {
		throw new IOException("do not use this init method, use instead init(Config config,String cacheName,Struct arguments)");
		// no longer used
	}
	public void init(Config config,String cacheName,Struct arguments) throws IOException {
		//this.config=config;
		this.cacheName=cacheName;
		this.arguments=arguments;
		
		try {
			getCache();
		} catch (PageException e) {
			throw new IOException(e);
		}
	}
	
	private MemCachedClient getCacheEL() {
		try {
			return getCache();
		} catch (PageException e) {
			throw new RuntimeException(e);
		}
	}
	
	private MemCachedClient getCache() throws PageException {
		if(_cache==null) {
			createPoolIfNecessary();
			_cache = new MemCachedClient(cacheName);
			//_cache.setClassLoader(config.getClassLoader());
		}
		return _cache;
	}
	
	private void createPoolIfNecessary() throws PageException {
		if(pool==null) {
		
			pool = SockIOPool.getInstance(cacheName);
		
			Cast cast = CFMLEngineFactory.getInstance().getCastUtil();
		
			String[] servers;
			String strServers = cast.toString(arguments.get("servers"),null);
			// backward comaptibility
			if(strServers==null){
				String host = cast.toString(arguments.get("host"));
				int port=cast.toIntValue(arguments.get("port",null),11211);
				servers = new String[] { host+":"+port};
			}
			else{
				strServers=strServers.trim();
				servers=strServers.split("\\s+");
				for(int i=0;i<servers.length;i++){
					servers[i]=servers[i].trim();
				}
			}
			
			
			
			// settings
			int initConn = cast.toIntValue(arguments.get("initial_connections",1),1);
			if(initConn>0)pool.setInitConn(initConn);
			
			int minConn = cast.toIntValue(arguments.get("min_spare_connections",1),1);
			if(minConn>0)pool.setMinConn(minConn);
			
			int maxConn=cast.toIntValue(arguments.get("max_spare_connections",32),32);
			if(maxConn>0)pool.setMaxConn(maxConn);
			
			int maxIdle=cast.toIntValue(arguments.get("max_idle_time",5),5);
			if(maxIdle>0)pool.setMaxIdle(maxIdle*1000);
			
			int maxBusy=cast.toIntValue(arguments.get("max_busy_time",30),30);
			if(maxBusy>0)pool.setMaxBusyTime(maxBusy*1000L);
			
			int maintSleep=cast.toIntValue(arguments.get("maint_thread_sleep",5),5);
			if(maintSleep>0)pool.setMaintSleep(maintSleep*1000L);
			
			int socketTO=cast.toIntValue(arguments.get("socket_timeout",3),3);
			if(socketTO>0)pool.setSocketTO(socketTO*1000);
			
			int socketConnTO=cast.toIntValue(arguments.get("socket_connect_to",3),3);
			if(socketConnTO>0)pool.setSocketConnectTO(socketConnTO*1000);
			
			pool.setFailover(cast.toBooleanValue(arguments.get("failover",false),false));
			pool.setFailback(cast.toBooleanValue(arguments.get("failback",false),false));
			pool.setNagle(cast.toBooleanValue(arguments.get("nagle_alg",false),false));
			pool.setAliveCheck(cast.toBooleanValue(arguments.get("alive_check",false),false));
			
			int bufferSize=cast.toIntValue(arguments.get("buffer_size",false),0);
			if(bufferSize>0)pool.setBufferSize(bufferSize*1024*1024);
			
        
			//pool.setHashingAlg(SockIOPool.NEW_COMPAT_HASH);
        	
			
	        pool.setServers(servers);
	        pool.initialize();
		}
		else if(!pool.isInitialized()) pool.initialize();
	}


	/**
	 * @see railo.commons.io.cache.Cache#contains(java.lang.String)
	 */
	public boolean contains(String key) {
		return getCacheEL().keyExists(key);
	}


	/**
	 * @see railo.commons.io.cache.Cache#getCustomInfo()
	 */
	public Struct getCustomInfo() {
		Struct info = CacheUtil.getInfo(this);
		return info;
	}

	/**
	 * @see railo.commons.io.cache.Cache#getCacheEntry(java.lang.String)
	 */
	public CacheEntry getCacheEntry(String key) throws CacheException {
		Object value = getCacheEL().get(key);
		if(value==null){
			misses++;
			throw new CacheException("there is no entry with key ["+key+"] in cache");
		}
		hits++;
		return new MemCacheEntry(key,value,null,null);
	}

	/**
	 * @see railo.commons.io.cache.Cache#getCacheEntry(java.lang.String, railo.commons.io.cache.CacheEntry)
	 */
	public CacheEntry getCacheEntry(String key, CacheEntry defaultValue) {
		Object value = getCacheEL().get(key);
		if(value==null){
			misses++;
			return defaultValue;
		}
		hits++;
		return new MemCacheEntry(key,value,null,null);
	}


	/**
	 * @see railo.commons.io.cache.Cache#getValue(java.lang.String)
	 */
	public Object getValue(String key) throws CacheException {
		Object value = getCacheEL().get(key);
		if(value==null){
			misses++;
			throw new CacheException("there is no entry with key ["+key+"] in cache");
		}
		hits++;
		return value;
	}

	/**
	 * @see railo.commons.io.cache.Cache#getValue(java.lang.String, java.lang.Object)
	 */
	public Object getValue(String key, Object defaultValue) {
		Object value = getCacheEL().get(key);
		if(value==null){
			misses++;
			return defaultValue;
		}
		hits++;
		return value;
	}

	/**
	 * @see railo.commons.io.cache.Cache#hitCount()
	 */
	public long hitCount() {
		return hits;
	}

	/**
	 * @see railo.commons.io.cache.Cache#missCount()
	 */
	public long missCount() {
		return misses;
	}

	/**
	 * @see railo.commons.io.cache.Cache#put(java.lang.String, java.lang.Object, java.lang.Long, java.lang.Long)
	 */
	public void put(String key, Object value, Long idleTime, Long until) {
		long time;

		// expire
		if(until!=null) time=until.longValue();
		else if(idleTime!=null) time=idleTime.longValue();
		else time=0;
		
		if(time<=0){
			getCacheEL().set(key,value);
		}
		else{
			getCacheEL().set(key,value, 
					CFMLEngineFactory.getInstance().getCreationUtil().createDate(
							time+System.currentTimeMillis()));
		}
		
		
	}

	/**
	 * @see railo.commons.io.cache.Cache#remove(java.lang.String)
	 */
	public boolean remove(String key) {
		return getCacheEL().delete(key);
	}
	

	// not supported

	/**
	 * @see railo.commons.io.cache.Cache#remove(railo.commons.io.cache.CacheKeyFilter)
	 */
	public int remove(CacheKeyFilter filter) {
		if(CacheKeyFilterAll.equalTo(filter)){
			getCacheEL().flushAll();
			return -1;
		}
		throw notSupported("remove:key filter");
	}
	
	public void clear() {
		getCacheEL().flushAll();
	}
	

	/**
	 * @see railo.commons.io.cache.Cache#remove(railo.commons.io.cache.CacheEntryFilter)
	 */
	public int remove(CacheEntryFilter filter) {
		throw notSupported("remove:entry filter");
	}

	/**
	 * @see railo.commons.io.cache.Cache#values()
	 */
	public List<Object> values() {
		throw notSupported("values");
	}

	/**
	 * @see railo.commons.io.cache.Cache#values(railo.commons.io.cache.CacheKeyFilter)
	 */
	public List<Object> values(CacheKeyFilter filter) {
		throw notSupported("entries:key filter");
	}

	/**
	 * @see railo.commons.io.cache.Cache#values(railo.commons.io.cache.CacheEntryFilter)
	 */
	public List<Object> values(CacheEntryFilter filter) {
		throw notSupported("values:entry filter");
	}
	
	/**
	 * @see railo.commons.io.cache.Cache#entries()
	 */
	public List<CacheEntry> entries() {
		throw notSupported("entries");
	}

	/**
	 * @see railo.commons.io.cache.Cache#entries(railo.commons.io.cache.CacheKeyFilter)
	 */
	public List<CacheEntry> entries(CacheKeyFilter filter) {
		throw notSupported("entries:key filter");
	}

	/**
	 * @see railo.commons.io.cache.Cache#entries(railo.commons.io.cache.CacheEntryFilter)
	 */
	public List<CacheEntry> entries(CacheEntryFilter filter) {
		throw notSupported("entries:entry filter");
	}

	/**
	 * @see railo.commons.io.cache.Cache#keys()
	 */
	public List<String> keys() {
		throw notSupported("keys");
	}

	/**
	 * @see railo.commons.io.cache.Cache#keys(railo.commons.io.cache.CacheKeyFilter)
	 */
	public List<String> keys(CacheKeyFilter filter) {
		throw notSupported("keys:key filter");
	}

	/**
	 * @see railo.commons.io.cache.Cache#keys(railo.commons.io.cache.CacheEntryFilter)
	 */
	public List keys(CacheEntryFilter filter) {
		throw notSupported("keys: entry filter");
	}
	
	

	private RuntimeException notSupported(String feature) {
		return new RuntimeException("this feature ["+feature+"] is not supported by memcached");
	}

}
