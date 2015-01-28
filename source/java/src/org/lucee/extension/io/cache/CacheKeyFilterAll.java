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

import lucee.commons.io.cache.CacheKeyFilter;
import lucee.loader.util.Util;

/**
 * accept everything
 */
public class CacheKeyFilterAll implements CacheKeyFilter {

	private static CacheKeyFilterAll instance=new CacheKeyFilterAll();

	/**
	 * @see railo.commons.io.cache.CacheKeyFilter#accept(java.lang.String)
	 */
	public boolean accept(String key) {
		return true;
	}

	@Override
	public String toPattern() {
		return "*";
	}

	public static CacheKeyFilterAll getInstance() {
		return instance;
	}
	
	public static boolean equalTo(CacheKeyFilter filter){
		if(filter==null) return true;
		if(filter instanceof CacheKeyFilterAll) return true;
		String pattern = filter.toPattern();
		if(Util.isEmpty(pattern,true)) return true;
		return pattern.equals("*");
	}

}
