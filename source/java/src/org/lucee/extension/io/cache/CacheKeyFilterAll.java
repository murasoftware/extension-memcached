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
