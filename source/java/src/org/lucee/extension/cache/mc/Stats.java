package org.lucee.extension.cache.mc;

import java.net.SocketAddress;
import java.util.Iterator;
import java.util.Map;

import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.util.Cast;

public class Stats {
	private Map<SocketAddress, Map<String, String>> raw;
	// // https://docs.oracle.com/cd/E17952_01/mysql-5.6-en/ha-memcached-stats.html

	private int curr_items = 0;
	private int total_items = 0;
	private long bytes = 0;
	private int curr_connections = 0;
	private int total_connections = 0;

	private long cmd_get = 0;
	private long cmd_set = 0;

	private long get_hits = 0;
	private long get_misses = 0;

	private long delete_hits = 0;
	private long delete_misses = 0;

	private long incr_hits = 0;
	private long incr_misses = 0;

	private long decr_hits = 0;
	private long decr_misses = 0;

	private long cas_hits = 0;
	private long cas_misses = 0;
	private long cas_badvalue = 0;
	private long evictions = 0;
	private long bytes_read = 0;
	private long bytes_written = 0;
	private long conn_yields = 0;
	private int limit_maxbytes = 0;
	private int threads = 0;

	public Stats(Map<SocketAddress, Map<String, String>> raw) {
		this.raw = raw;
		CFMLEngine eng = CFMLEngineFactory.getInstance();
		Cast cast = eng.getCastUtil();
		Iterator<Map<String, String>> it = raw.values().iterator();
		Map<String, String> map;

		while (it.hasNext()) {
			map = it.next();
			curr_items += cast.toIntValue(map.get("curr_items"), 0);
			total_items += cast.toIntValue(map.get("total_items"), 0);
			curr_connections += cast.toIntValue(map.get("curr_connections"), 0);
			total_connections += cast.toIntValue(map.get("total_connections"), 0);
			bytes += cast.toLongValue(map.get("bytes"), 0);
			cmd_get += cast.toLongValue(map.get("cmd_get"), 0);
			cmd_set += cast.toLongValue(map.get("cmd_set"), 0);
			get_hits += cast.toLongValue(map.get("get_hits"), 0);
			get_misses += cast.toLongValue(map.get("get_misses"), 0);
			delete_hits += cast.toLongValue(map.get("delete_hits"), 0);
			delete_misses += cast.toLongValue(map.get("delete_misses"), 0);
			incr_hits += cast.toLongValue(map.get("incr_hits"), 0);
			incr_misses += cast.toLongValue(map.get("incr_misses"), 0);
			decr_hits += cast.toLongValue(map.get("decr_hits"), 0);
			decr_misses += cast.toLongValue(map.get("decr_misses"), 0);
			cas_hits += cast.toLongValue(map.get("cas_hits"), 0);
			cas_misses += cast.toLongValue(map.get("cas_misses"), 0);
			cas_badvalue += cast.toLongValue(map.get("cas_misses"), 0);
			evictions += cast.toLongValue(map.get("evictions"), 0);
			bytes_read += cast.toLongValue(map.get("bytes_read"), 0);
			bytes_written += cast.toLongValue(map.get("bytes_written"), 0);
			conn_yields += cast.toLongValue(map.get("conn_yields"), 0);
			limit_maxbytes += cast.toIntValue(map.get("limit_maxbytes"), 0);
			threads += cast.toIntValue(map.get("threads"), 0);
		}
	}

	public Map<SocketAddress, Map<String, String>> getRaw() {
		return raw;
	}

	/**
	 * Current number of items stored by this instance.
	 */
	public int curr_items() {
		return curr_items;
	}

	/**
	 * Total number of items stored during the life of this instance.
	 */
	public int total_items() {
		return total_items;
	}

	/**
	 * Current number of bytes used by this server to store items.
	 */
	public long bytes() {
		return bytes;
	}

	/**
	 * Current number of open connections.
	 */
	public int curr_connections() {
		return curr_connections;
	}

	/**
	 * Total number of connections opened since the server started running.
	 */
	public int total_connections() {
		return total_connections;
	}

	/**
	 * Total number of retrieval requests (get operations).
	 */
	public long cmd_get() {
		return cmd_get;
	}

	/**
	 * Total number of storage requests (set operations).
	 */
	public long cmd_set() {
		return cmd_set;
	}

	/**
	 * Number of keys that have been requested and found present.
	 */
	public long get_hits() {
		return get_hits;
	}

	/**
	 * Number of items that have been requested and not found.
	 */
	public long get_misses() {
		return get_misses;
	}

	/**
	 * Number of keys that have been deleted and found present.
	 */
	public long delete_hits() {
		return delete_hits;
	}

	/**
	 * Number of items that have been delete and not found.
	 */
	public long delete_misses() {
		return delete_misses;
	}

	/**
	 * Number of keys that have been incremented and found present.
	 */
	public long incr_hits() {
		return incr_hits;
	}

	/**
	 * Number of items that have been incremented and not found.
	 */
	public long incr_misses() {
		return incr_misses;
	}

	/**
	 * Number of keys that have been decremented and found present.
	 */
	public long decr_hits() {
		return decr_hits;
	}

	/**
	 * Number of items that have been decremented and not found.
	 */
	public long decr_misses() {
		return decr_misses;
	}

	/**
	 * Number of keys that have been compared and swapped and found present.
	 */
	public long cas_hits() {
		return cas_hits;
	}

	/**
	 * Number of items that have been compared and swapped and not found.
	 */
	public long cas_misses() {
		return cas_misses;
	}

	/**
	 * Number of keys that have been compared and swapped, but the comparison
	 * (original) value did not match the supplied value.
	 */
	public long cas_badvalue() {
		return cas_badvalue;
	}

	/**
	 * Number of valid items removed from cache to free memory for new items.
	 */
	public long evictions() {
		return evictions;
	}

	/**
	 * Total number of bytes read by this server from network.
	 */
	public long bytes_read() {
		return bytes_read;
	}

	/**
	 * Total number of bytes sent by this server to network.
	 */
	public long bytes_written() {
		return bytes_written;
	}

	/**
	 * Number of yields for connections (related to the -R option).
	 */
	public long conn_yields() {
		return conn_yields;
	}

	/**
	 * Number of bytes this server is permitted to use for storage.
	 */
	public int limit_maxbytes() {
		return limit_maxbytes;
	}

	/**
	 * Number of worker threads requested.
	 */
	public int threads() {
		return threads;
	}

}
