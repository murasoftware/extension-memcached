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
package org.lucee.extension.io.cache;

import java.io.IOException;

import lucee.commons.io.cache.Cache;
import lucee.commons.io.cache.CacheEntry;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.type.Struct;
import lucee.runtime.type.dt.TimeSpan;

public class CacheUtil {

	public static Struct getInfo(CacheEntry ce) {
		Struct info=CFMLEngineFactory.getInstance().getCreationUtil().createStruct();
		info.setEL("created", ce.created());
		info.setEL("last_hit", ce.lastHit());
		info.setEL("last_modified", ce.lastModified());

		info.setEL("hit_count", new Double(ce.hitCount()));
		info.setEL("size", new Double(ce.size()));
		
		
		info.setEL("idle_time_span", toTimespan(ce.idleTimeSpan()));		
		info.setEL("live_time_span", toTimespan(ce.liveTimeSpan()));
		
		
		return info;
	}


	public static Struct getInfo(Cache c) {
		Struct info=CFMLEngineFactory.getInstance().getCreationUtil().createStruct();
		try{
			long value = c.hitCount();
			if(value>=0)info.setEL("hit_count", new Double(value));
		}catch(Throwable t){} // we use Throwable instead of IOException to make it also work with Lucee 4.5
		try{
			long value = c.missCount();
			if(value>=0)info.setEL("miss_count", new Double(value));
		}catch(Throwable t){} // we use Throwable instead of IOException to make it also work with Lucee 4.5
		return info;
	}

	
	public static Object toTimespan(long timespan) {
		if(timespan==0)return "";
		TimeSpan ts = CFMLEngineFactory.getInstance().getCastUtil().toTimespan(new Double(timespan/(24D*60D*60D*1000)),null);
		if(ts==null)return "";
		return ts;
	}


	public static String toString(CacheEntry ce) {

		return "created:	"+ce.created()
		+"\nlast-hit:	"+ce.lastHit()
		+"\nlast-modified:	"+ce.lastModified()
		
		+"\nidle-time:	"+ce.idleTimeSpan()
		+"\nlive-time	:"+ce.liveTimeSpan()
		
		+"\nhit-count:	"+ce.hitCount()
		+"\nsize:		"+ce.size();
	}


	
	
	
	
	
}
