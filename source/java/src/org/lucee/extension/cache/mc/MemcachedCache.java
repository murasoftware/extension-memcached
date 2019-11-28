package org.lucee.extension.cache.mc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lucee.commons.io.cache.CacheEntry;
import lucee.commons.io.cache.CachePro;
import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.config.Config;
import lucee.runtime.exp.PageException;
import lucee.runtime.type.Struct;
import lucee.runtime.util.Cast;
import lucee.runtime.util.ListUtil;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.internal.OperationFuture;

public class MemcachedCache extends CacheSupport {

	private static final int DEFAULT_PORT = 11211;
	private static final int DAY = 60 * 60 * 24;
	MemcachedClient _client = null;
	boolean caseSensitive = false;
	private String cacheName;
	private Struct arguments;
	private ClassLoader cl;
	private TranscoderImpl transcoder;
	private InetSocketAddress[] addresses;
	private int defaultExpires;

	// stats items
	@Override
	public void init(Config config, String cacheName, Struct arguments) throws IOException {
		this.cacheName = cacheName;
		this.arguments = arguments;
		transcoder = new TranscoderImpl(config.getClassLoader());
	}

	private MemcachedClient getCacheEL() {
		try {
			return getCache();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private MemcachedClient getCache() throws IOException {
		if (_client == null) {
			CFMLEngine engine = CFMLEngineFactory.getInstance();
			Cast cast = engine.getCastUtil();

			// host(s)/port(s)
			InetSocketAddress[] addresses = null;
			try {
				String strServers = cast.toString(arguments.get("servers"), null);
				// backward comaptibility
				if (strServers == null) {
					String host = cast.toString(arguments.get("host"));
					int port = cast.toIntValue(arguments.get("port", null), DEFAULT_PORT);
					addresses = new InetSocketAddress[] { new InetSocketAddress(host, port) };
				} else {
					strServers = strServers.trim();
					String[] servers = strServers.split("\\s+");
					addresses = new InetSocketAddress[servers.length];
					int index;
					String host;
					for (int i = 0; i < servers.length; i++) {
						host = servers[i].trim();
						index = host.lastIndexOf(':');
						if (index == -1) {
							addresses[i] = new InetSocketAddress(host, DEFAULT_PORT);
						} else {
							addresses[i] = new InetSocketAddress(host.substring(0, index),
									cast.toIntValue(host.substring(index + 1)));
						}
					}
				}
			} catch (PageException pe) {
				engine.getExceptionUtil().toIOException(pe);
			}
			this.addresses = addresses;

			// settings
			defaultExpires = cast.toIntValue(arguments.get("default_expires", null), 600);
			if (defaultExpires <= 0)
				defaultExpires = 600;
			_client = new MemcachedClient(addresses);
		}
		return _client;
	}

	// client.removeObserver(obs)

	@Override
	public CachePro decouple() {
		// TODO
		return null;
	}

	@Override
	public boolean contains(String key) throws IOException {
		return getCache().get(keyTranslate(key), transcoder) != null; // make faster transcoder that does nothing
	}

	@Override
	public CacheEntry getCacheEntry(String key, CacheEntry defaultValue) {
		Object val = getCacheEL().get(keyTranslate(key), transcoder);
		if (val == null)
			return defaultValue;
		return toCacheEntry(key, val);
	}

	public Stats getStats() {
		Map<SocketAddress, Map<String, String>> raw = getCacheEL().getStats();
		Stats s = new Stats(raw);

		return s;
	}

	public int getCurrentItems() {
		Map<SocketAddress, Map<String, String>> raw = getCacheEL().getStats();
		Cast cast = CFMLEngineFactory.getInstance().getCastUtil();
		Iterator<Map<String, String>> it = raw.values().iterator();
		int currItems = 0;
		while (it.hasNext()) {
			currItems += cast.toIntValue(it.next().get("curr_items"), 0);
		}
		return currItems;
	}

	@Override
	public Struct getCustomInfo() {
		Struct info = CFMLEngineFactory.getInstance().getCreationUtil().createStruct();
		Stats stats = getStats();

		info.setEL("hit_count", Double.valueOf(hitCount(stats)));
		info.setEL("miss_count", Double.valueOf(missCount(stats)));
		info.setEL("bytes", Double.valueOf(stats.bytes()));
		info.setEL("bytes_read", Double.valueOf(stats.bytes_read()));
		info.setEL("bytes_written", Double.valueOf(stats.bytes_written()));
		info.setEL("curr_connections", Double.valueOf(stats.curr_connections()));
		info.setEL("total_connections", Double.valueOf(stats.total_connections()));
		info.setEL("conn_yields", Double.valueOf(stats.conn_yields()));
		info.setEL("curr_items", Double.valueOf(stats.curr_items()));
		info.setEL("total_items", Double.valueOf(stats.total_items()));
		info.setEL("evictions", Double.valueOf(stats.evictions()));
		info.setEL("limit_maxbytes", Double.valueOf(stats.limit_maxbytes()));
		info.setEL("threads", Double.valueOf(stats.threads()));

		info.setEL("hit_count", Double.valueOf(hitCount(stats)));
		info.setEL("miss_count", Double.valueOf(missCount(stats)));
		info.setEL("hit_count_get", Double.valueOf(stats.get_hits()));
		info.setEL("miss_count_get", Double.valueOf(stats.get_misses()));
		info.setEL("hit_count_delete", Double.valueOf(stats.delete_hits()));
		info.setEL("miss_count_delete", Double.valueOf(stats.delete_misses()));
		info.setEL("hit_count_cas", Double.valueOf(stats.cas_hits()));
		info.setEL("miss_count_cas", Double.valueOf(stats.cas_misses()));
		info.setEL("hit_count_decr", Double.valueOf(stats.decr_hits()));
		info.setEL("miss_count_decr", Double.valueOf(stats.decr_misses()));
		info.setEL("hit_count_incr", Double.valueOf(stats.incr_hits()));
		info.setEL("miss_count_incr", Double.valueOf(stats.incr_misses()));

		// testing
		/*
		 * Collection<String> slabs = getSlabs(); info.setEL("slabs", slabs);
		 * Iterator<String> it = slabs.iterator(); String str; Struct data =
		 * CFMLEngineFactory.getInstance().getCreationUtil().createStruct();
		 * info.setEL("data", data); while (it.hasNext()) { str = it.next();
		 * System.err.println("------------------->"); System.err.println("->" + str);
		 * data.setEL(str, getCacheEL().getStats("cachedump " + str + " 0")); //
		 * data.appendEL(getCacheEL().getStats("lru_crawler metadump " + str));
		 * 
		 * // lru_crawler metadump all
		 * 
		 * } for (int i = 1; i < 100; i++) { data.setEL(i + "",
		 * getCacheEL().getStats("cachedump " + i + " 0")); }
		 */

		// data.appendEL(getCacheEL().getStats("lru_crawler metadump all"));

		return info;
	}

	private Collection<String> getSlabs() {
		Set<String> set = new HashSet<>();
		CFMLEngine eng = CFMLEngineFactory.getInstance();
		ListUtil lu = eng.getListUtil();

		Iterator<Map<String, String>> it = getCacheEL().getStats("items").values().iterator();
		Map<String, String> map;
		Iterator<String> itt;
		String[] arr;
		while (it.hasNext()) {
			map = it.next();
			itt = map.keySet().iterator();
			while (itt.hasNext()) {
				arr = lu.toStringArray(itt.next(), ":");
				set.add(arr[1]);
			}
		}
		return set;
	}

	@Override
	public long hitCount() {
		return hitCount(getStats());
	}

	private long hitCount(Stats stats) {
		return stats.get_hits() + stats.delete_hits() + stats.delete_hits() + stats.incr_hits() + stats.decr_hits()
				+ stats.cas_hits();
	}

	@Override
	public long missCount() {
		return missCount(getStats());
	}

	private long missCount(Stats stats) {
		return stats.get_misses() + stats.delete_misses() + stats.delete_misses() + stats.incr_misses()
				+ stats.decr_misses() + stats.cas_misses();
	}

	@Override
	public List<String> keys() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void put(String key, Object val, Long idleTime, Long until) throws IOException {
		// expires
		int exp;
		if (until != null && until.longValue() > 0) {
			exp = (int) (until.longValue() / 1000);
			if (exp < 1)
				exp = 1;
		} else if (idleTime != null && idleTime.longValue() > 0) {
			exp = (int) (idleTime.longValue() / 1000);
			if (exp < 1)
				exp = 1;
		} else {
			exp = defaultExpires;
		}
		getCache().set(keyTranslate(key), exp > DAY ? ((int) (System.currentTimeMillis() / 1000L)) + exp : exp,
				new MemcachedCacheEntry(key, val), transcoder);
	}

	//
	// 1574429409

	private String keyTranslate(String key) {
		return caseSensitive || key == null ? key : key.toLowerCase();
	}

	@Override
	public boolean remove(String key) throws IOException {
		OperationFuture<Boolean> res = getCache().delete(keyTranslate(key));
		try {
			return res.get();
		} catch (Exception e) {
			throw CFMLEngineFactory.getInstance().getExceptionUtil().toIOException(e);
		}
	}

	@Override
	public CacheEntry getQuiet(String key, CacheEntry defaultValue) {
		// TODO
		return getCacheEntry(key, defaultValue);
	}

	@Override
	public int clear() throws IOException {
		int count = getCurrentItems();
		try {
			// Invalidate all items immediately
			Boolean res = getCache().flush().get();
			if (Boolean.TRUE.equals(res)) {
				return count;
			}
		} catch (Exception e) {
			throw CFMLEngineFactory.getInstance().getExceptionUtil().toIOException(e);
		}
		return 0;
	}

	private CacheEntry toCacheEntry(String key, Object obj) {
		if (obj instanceof CacheEntry)
			return (CacheEntry) obj;
		// cache entry from different classloader?
		if (CFMLEngineFactory.getInstance().getClassUtil().isInstaneOf(obj.getClass(), CacheEntry.class)) {
			log("different classloader");
			// read data out by redflection
		}
		log("object is not a cache entry");
		return new MemcachedCacheEntry(key, obj);
	}

	private void log(String msg) {
		System.err.println(msg);
	}

}
