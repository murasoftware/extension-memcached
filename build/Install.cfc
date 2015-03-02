<!--- 
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
 ---><cfcomponent>



    <cfset variables.oldJars=["lucee-extension-memcache.jar","java_memcached-release.jar"]>
    <cfset variables.oldContext=["MemCached.cfc"]>


    
    <cffunction name="validate" returntype="void" output="no"
    	hint="called to validate values">
    	<cfargument name="error" type="struct">
        <cfargument name="path" type="string">
        <cfargument name="config" type="struct">
        <cfargument name="step" type="numeric">
        
    </cffunction>
    
    <cffunction name="install" returntype="string" output="no"
    	hint="called from Lucee to install application">
    	<cfargument name="error" type="struct">
        <cfargument name="path" type="string">
        <cfargument name="config" type="struct">

        <cfadmin 
            action="updateJar"
            type="#request.adminType#"
            password="#session["password"&request.adminType]#"
            jar="#path#jars/memcached-2-5-1.jar">


        <cfadmin 
            action="updateJar"
            type="#request.adminType#"
            password="#session["password"&request.adminType]#"
            jar="#path#jars/lucee-extension-memcached.jar">
            
        <cfadmin 
        	action="updateContext"
            type="#request.adminType#"
            password="#session["password"&request.adminType]#"
            source="#path#context/admin/cdriver/MemCached.cfc"
            destination="admin/cdriver/MemCached.cfc">

        <cfreturn 'Memcached Driver is now successfully installed, you must restart the Application Server/Servlet Engine before you can use it.'>
    </cffunction>
    	
     <cffunction name="update" returntype="string" output="no"
    	hint="called from Lucee to update a existing application">
    	<cfargument name="error" type="struct" default="#{}#">
        <cfargument name="path" type="string">
        <cfargument name="config" type="struct">
        
        <!--- remove old version jars --->
        <cfloop array="#variables.oldJars#" item="local.jarName">
            <cftry>
                <cfadmin 
                    action="removeJar"
                    type="#request.adminType#"
                    password="#session["password"&request.adminType]#"
                    jar="#local.jarName#">
                <cfcatch></cfcatch>
            </cftry>
        </cfloop>
        
        <!--- remove old contexts --->
        <cfloop array="#variables.oldContext#" item="local.contextName">
            <cftry>
                <cfadmin 
                    action="removeContext"
                    type="#request.adminType#"
                    password="#session["password"&request.adminType]#"
                    destination="admin/cdriver/#local.contextName#">
                <cfcatch></cfcatch>
            </cftry>
        </cfloop>


        <cfadmin 
            action="updateJar"
            type="#request.adminType#"
            password="#session["password"&request.adminType]#"
            jar="#path#jars/lucee-extension-memcached.jar">
            
        <cfadmin 
        	action="updateContext"
            type="#request.adminType#"
            password="#session["password"&request.adminType]#"
            source="#path#context/admin/cdriver/MemCached.cfc"
            destination="admin/cdriver/MemCached.cfc">

        <cfreturn 'Memcached Driver is now successfully updated.'>
        
    </cffunction>
    
    
    <cffunction name="uninstall" returntype="string" output="no"
    	hint="called from Lucee to uninstall application">
    	<cfargument name="path" type="string">
        <cfargument name="config" type="struct">
        
        <cfadmin 
            action="removeJar"
            type="#request.adminType#"
            password="#session["password"&request.adminType]#"
            jar="lucee-extension-memcached.jar">
        
        <cftry>
        	<cffile action="delete" file="#path#jars/memcached-2-5-1.jar">
        	<cfcatch></cfcatch>
        </cftry>
		<cfadmin 
        	action="removeContext"
            type="#request.adminType#"
            password="#session["password"&request.adminType]#"
            destination="admin/cdriver/MemCached.cfc">
        
        <cfset install(argumentsCollection=arguments)>

        <cfreturn 'Memcached Driver is now successfully removed'>
    </cffunction>
    
    
    <cffunction name="getLibFolder" access="private">
		<cfset var cl=getPageContext().getClass().getClassLoader()>
        <cfset path="lucee/loader/engine/CFMLEngine.class">
        <cfset var res = cl.getResource(path)>
        <cfset var strFile = createObject('java','java.net.URLDecoder').decode(res.getFile().trim(),"iso-8859-1")>
        <cfset var index=strFile.indexOf('!')>
        <cfif index!=-1><cfset strFile=strFile.substring(0,index)></cfif>
        <cfset strFile=GetDirectoryFromPath(strFile)>
		
		<cfif strFile.startsWith("file:")><cfset strFile=strFile.substring(5)></cfif>
        <cfif findNoCase("windows",server.os.name) and left(strFile,1) EQ "/">
			<cfset strFile=strFile.substring(1)>
		</cfif>
		
		<cfreturn strFile>
    </cffunction>
    
</cfcomponent>