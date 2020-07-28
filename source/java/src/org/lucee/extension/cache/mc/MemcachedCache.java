package org.lucee.extension.cache.mc;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import lucee.commons.io.cache.CacheEntry;
import lucee.commons.io.cache.CachePro;
import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.loader.util.Util;
import lucee.runtime.config.Config;
import lucee.runtime.exp.PageException;
import lucee.runtime.type.Struct;
import lucee.runtime.util.Cast;
import lucee.runtime.util.ListUtil;
import net.spy.memcached.ConnectionFactory;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.ConnectionFactoryBuilder.Protocol;
import net.spy.memcached.DefaultConnectionFactory;
import net.spy.memcached.FailureMode;
import net.spy.memcached.MemcachedClient;

public class MemcachedCache extends CacheSupport {

	private static final int DEFAULT_PORT = 11211;
	private static final int DAY = 60 * 60 * 24;
	MemcachedClient _client = null;
	boolean caseSensitive = false;
	private String cacheName;
	private Struct arguments;
	private ClassLoader cl;
	private List<InetSocketAddress> addresses;
	private int defaultExpires;
	private ConnectionFactory connFactory;

	public static void init(Config config, String[] cacheNames, Struct[] arguments) throws IOException {
	}

	// stats items
	@Override
	public void init(Config config, String cacheName, Struct arguments) throws IOException {
		this.cacheName = cacheName;
		this.arguments = arguments;
		TranscoderImpl _transcoder = new TranscoderImpl(config.getClassLoader());

		{
			CFMLEngine engine = CFMLEngineFactory.getInstance();
			Cast cast = engine.getCastUtil();

			// host(s)/port(s)
			List<InetSocketAddress> addresses = new ArrayList<>();
			try {
				String strServers = cast.toString(arguments.get("servers"), null);
				// backward comaptibility
				if (strServers == null) {
					String host = cast.toString(arguments.get("host"));
					int port = cast.toIntValue(arguments.get("port", null), DEFAULT_PORT);
					addresses.add(new InetSocketAddress(host, port));
				} else {
					strServers = strServers.trim();
					String[] servers = strServers.split("\\s+");
					int index;
					String host;
					for (int i = 0; i < servers.length; i++) {
						host = servers[i].trim();
						index = host.lastIndexOf(':');
						if (index == -1) {
							addresses.add(new InetSocketAddress(host, DEFAULT_PORT));
						} else {
							addresses.add(new InetSocketAddress(host.substring(0, index),
									cast.toIntValue(host.substring(index + 1))));
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

			// failure mode
			FailureMode failureMode = toFailureMode(cast.toString(arguments.get("failure_mode", null), null),
					FailureMode.Redistribute);
			// protocol
			Protocol protocol = toProtocol(cast.toString(arguments.get("protocol", null), null), Protocol.BINARY);

			connFactory = new ConnectionFactoryBuilder().setDaemon(true).setTranscoder(_transcoder)
					.setFailureMode(failureMode).setProtocol(protocol).build();
		}

	}

	private Protocol toProtocol(String str, Protocol defaultValue) {
		if (Util.isEmpty(str, true))
			return defaultValue;
		str = str.trim().toLowerCase();
		if ("binary".equals(str))
			return Protocol.BINARY;
		if ("text".equals(str))
			return Protocol.TEXT;
		return defaultValue;
	}

	private static FailureMode toFailureMode(String str, FailureMode defaultValue) {
		if (Util.isEmpty(str, true))
			return defaultValue;
		str = str.trim().toLowerCase();
		if ("cancel".equals(str))
			return FailureMode.Cancel;
		if ("redistribute".equals(str))
			return FailureMode.Redistribute;
		if ("retry".equals(str))
			return FailureMode.Retry;
		return defaultValue;
	}

	private MemcachedClient getCache(boolean forceNewConnection) throws IOException {
		if (forceNewConnection && _client != null) {
			shutdownEL(_client);
			_client = null;
		}
		if (_client == null) {
			_client = new MemcachedClient(connFactory, addresses);
		}
		return _client;
	}

	private void shutdownEL(MemcachedClient c) {
		try {
			c.shutdown();
		} catch (Exception e) {
		}
	}

	@Override
	public CachePro decouple() {
		// is already decoupled by default
		return this;
	}

	@Override
	public boolean contains(String key) throws IOException {
		return getCache(false).get(keyTranslate(key)) != null; // make faster transcoder that does nothing
	}

	@Override
	public CacheEntry getCacheEntry(String key, CacheEntry defaultValue) {
		Object val = null;
		try {
			val = getCache(false).get(keyTranslate(key));
		} catch (Exception e) {
		}

		if (val == null)
			return defaultValue;
		return toCacheEntry(key, val);
	}

	public Stats getStats(Stats defaultValue) {
		MemcachedClient mcc;
		try {
			mcc = getCache(false);
		} catch (Exception e) {
			return defaultValue;
		}
		Map<SocketAddress, Map<String, String>> raw = mcc.getStats();
		Stats s = new Stats(raw);

		return s;
	}

	public int getCurrentItems(int defaultValue) {
		MemcachedClient mcc;
		try {
			mcc = getCache(false);
		} catch (Exception e) {
			return defaultValue;
		}
		Map<SocketAddress, Map<String, String>> raw = mcc.getStats();
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
		Stats stats = getStats(null);
		if (stats != null) {
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
		}
		return info;
	}

	private Collection<String> getSlabs(Collection<String> defaultValue) {
		MemcachedClient mcc;
		try {
			mcc = getCache(false);
		} catch (Exception e) {
			return defaultValue;
		}

		Set<String> set = new HashSet<>();
		CFMLEngine eng = CFMLEngineFactory.getInstance();
		ListUtil lu = eng.getListUtil();

		Iterator<Map<String, String>> it = mcc.getStats("items").values().iterator();
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
		return hitCount(getStats(null));
	}

	private long hitCount(Stats stats) {
		if (stats == null)
			return 0;
		return stats.get_hits() + stats.delete_hits() + stats.delete_hits() + stats.incr_hits() + stats.decr_hits()
				+ stats.cas_hits();
	}

	@Override
	public long missCount() {
		return missCount(getStats(null));
	}

	private long missCount(Stats stats) {
		if (stats == null)
			return 0;
		return stats.get_misses() + stats.delete_misses() + stats.delete_misses() + stats.incr_misses()
				+ stats.decr_misses() + stats.cas_misses();
	}

	@Override
	public List<String> keys() throws IOException {
		// TODO
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
		try {
			int _exp = exp > DAY ? ((int) (System.currentTimeMillis() / 1000L)) + exp : exp;
			MemcachedCacheEntry mce = new MemcachedCacheEntry(key, val, null, null);
			try {
				getCache(true).set(keyTranslate(key), _exp, mce).get(DefaultConnectionFactory.DEFAULT_OPERATION_TIMEOUT,
						TimeUnit.MILLISECONDS);
			} catch (TimeoutException e) {
				getCache(true).set(keyTranslate(key), _exp, mce).get(DefaultConnectionFactory.DEFAULT_OPERATION_TIMEOUT,
						TimeUnit.MILLISECONDS);
			}
			System.out.println("all fine!!");
		} catch (Exception e) {
			throw CFMLEngineFactory.getInstance().getExceptionUtil().toIOException(e);
		}
	}

	//
	// 1574429409

	private String keyTranslate(String key) {
		return caseSensitive || key == null ? key : key.toLowerCase();
	}

	@Override
	public boolean remove(String key) throws IOException {
		try {
			try {
				return getCache(false).delete(keyTranslate(key)).get(DefaultConnectionFactory.DEFAULT_OPERATION_TIMEOUT,
						TimeUnit.MILLISECONDS);
			} catch (TimeoutException te) {
				return getCache(true).delete(keyTranslate(key)).get(DefaultConnectionFactory.DEFAULT_OPERATION_TIMEOUT,
						TimeUnit.MILLISECONDS);
			}

		} catch (Exception e) {
			throw CFMLEngineFactory.getInstance().getExceptionUtil().toIOException(e);
		}
	}

	public boolean remove(String[] keys) throws IOException {
		boolean rtn = true;
		for (String key : keys) {
			if (!remove(key))
				rtn = false;
		}
		return rtn;
	}

	@Override
	public CacheEntry getQuiet(String key, CacheEntry defaultValue) {
		return getCacheEntry(key, defaultValue);
	}

	@Override
	public int clear() throws IOException {
		int count = getCurrentItems(0);
		try {
			// Invalidate all items immediately
			Boolean res;
			try {
				res = getCache(false).flush().get(DefaultConnectionFactory.DEFAULT_OPERATION_TIMEOUT,
						TimeUnit.MILLISECONDS);
			} catch (TimeoutException te) {
				res = getCache(true).flush().get(DefaultConnectionFactory.DEFAULT_OPERATION_TIMEOUT,
						TimeUnit.MILLISECONDS);
			}

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
			try {
				Method m = obj.getClass().getMethod("getValue", new Class[0]);
				obj = m.invoke(obj, new Object[0]);
			} catch (Exception e) {
			}
		}
		return new MemcachedCacheEntry(key, obj, null, null);
	}

}
