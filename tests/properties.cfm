<cfscript>

request.cache.memcached.class="org.lucee.extension.io.cache.memcache.MemCacheRaw";
request.cache.memcached.custom={
		'initial_connections':'1',
		'socket_timeout':30,
		'alive_check':true,
		'buffer_size':1,
		'max_spare_connections':'32',
		'socket_connect_to':3,
		'min_spare_connections':1,
		'failback':true,
		'maint_thread_sleep':5,
		'max_idle_time':600,
		'nagle_alg':true,
		'max_busy_time':30,
		'failover':true,
		'servers':'localhost:11211'};


</cfscript>