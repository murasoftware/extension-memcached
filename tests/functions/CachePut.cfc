<!--- 
 *
 * Copyright (c) 2014, the Railo Company LLC. All rights reserved.
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
 ---><cfcomponent extends="org.lucee.cfml.test.LuceeTestCase">
	<cfset variables.cacheName="Test"&ListFirst(ListLast(getCurrentTemplatePath(),"\/"),".")>
	
	<cffunction name="testCachePutMemcachedCache" localMode="modern">
		<cfset createMemcachedCache()>
		<cfset testCachePut()>
		<cfset deleteCache()>
	</cffunction>

	
	<cffunction access="private" name="testCachePut" localMode="modern">

<!--- begin old test code --->
<cfset server.enableCache=true>

<cflock scope="server" timeout="10">
	<!--- <cfset cacheRemove(arrayToList(cacheGetAllIds()))> --->
	<cfset prefix=getTickCount()>

	<cfset cachePut(prefix&'abc','123',CreateTimeSpan(0,0,0,1))>
	<cfset cachePut(prefix&'def','123',CreateTimeSpan(0,0,0,2),CreateTimeSpan(0,0,0,1))>
	<cfset cachePut(prefix&'ghi','123',CreateTimeSpan(0,0,0,0),CreateTimeSpan(0,0,0,0))>
    
	<cfset sct={}>
    <cfset sct.a=cacheGet(prefix&'abc')>
    <cfset sct.b=cacheGet(prefix&'def')>
    <cfset sct.c=cacheGet(prefix&'ghi')>
    
    <cfset valueEquals(left="#structKeyExists(sct,'a')#", right="true")>
    <cfset valueEquals(left="#structKeyExists(sct,'b')#", right="true")>
    <cfset valueEquals(left="#structKeyExists(sct,'c')#", right="true")>
    <cfset sleep(1200)>
    <cfset sct.d=cacheGet(prefix&'abc')>
    <cfset sct.e=cacheGet(prefix&'def')>
    <cfset sct.f=cacheGet(prefix&'ghi')>
    <cfset valueEquals(left="#structKeyExists(sct,'d')#", right="false")>
    <cfset valueEquals(left="#structKeyExists(sct,'e')#", right="false")>
    <cfset valueEquals(left="#structKeyExists(sct,'f')#", right="true")>
    
<cfif server.ColdFusion.ProductName EQ "lucee">    
	<cfset cachePut(prefix&'def','123',CreateTimeSpan(0,0,0,2),CreateTimeSpan(0,0,0,1),cacheName)>
</cfif>
</cflock>

<!--- end old test code --->
	
		
		<!--- <cfset assertEquals("","")> --->
	</cffunction>
	
	<cffunction access="private" name="valueEquals">
		<cfargument name="left">
		<cfargument name="right">
		<cfset assertEquals(arguments.right,arguments.left)>
	</cffunction>
	
<cfscript>
	private function createMemcachedCache() {
		admin 
				action="updateCacheConnection"
				type="web"
				password="#request.webadminpassword#"
				
				default="object"
				name="#cacheName#" 
				class="#request.cache.memcached.class#" 
				storage="false"
				custom="#request.cache.memcached.custom#";
	}
				
	private function deleteCache(){
		admin 
			action="removeCacheConnection"
			type="web"
			password="#request.webadminpassword#"
			name="#cacheName#";
						
	}
</cfscript>	
</cfcomponent>