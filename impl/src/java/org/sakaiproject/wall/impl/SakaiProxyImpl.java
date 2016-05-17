/*************************************************************************************
 * Copyright 2006, 2008 Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 *
 *       http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.

 *************************************************************************************/
package org.sakaiproject.wall.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.sakaiproject.authz.api.AuthzGroup;
import org.sakaiproject.authz.api.AuthzGroupService;
import org.sakaiproject.authz.api.AuthzPermissionException;
import org.sakaiproject.authz.api.FunctionManager;
import org.sakaiproject.authz.api.GroupNotDefinedException;
import org.sakaiproject.authz.api.Role;
import org.sakaiproject.authz.api.SecurityAdvisor;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.wall.api.WallFunctions;
import org.sakaiproject.wall.api.WallManager;
import org.sakaiproject.wall.api.SakaiProxy;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.entity.api.EntityManager;
import org.sakaiproject.entity.api.EntityProducer;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.memory.api.Cache;
import org.sakaiproject.memory.api.MemoryService;
import org.sakaiproject.memory.api.SimpleConfiguration;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.Tool;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.tool.api.ToolSession;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.util.FormattedText;

/**
 * @author Adrian Fish (adrian.r.fish@gmail.com)
 */
@Setter @Slf4j
public class SakaiProxyImpl implements SakaiProxy {

    private AuthzGroupService authzGroupService;
    private EntityManager entityManager;
    private EventTrackingService eventTrackingService;
    private FunctionManager functionManager;
    private MemoryService memoryService;
    private SecurityService securityService;
    private SessionManager sessionManager;
    private ServerConfigurationService serverConfigurationService;
    private SiteService siteService;
    private ToolManager toolManager;
    private UserDirectoryService userDirectoryService;

    public void init() {
    }


    public Session getCurrentSession() {
        return sessionManager.getCurrentSession();
    }

    public String getCurrentSiteId() {
        return toolManager.getCurrentPlacement().getContext(); // equivalent to
    }

    public Site getSiteOrNull(String siteId) {

        Site site = null;

        try {
            site = siteService.getSite(siteId);
        } catch (IdUnusedException idue) {
            log.warn("No site with id '" + siteId + "'");
        }

        return site;
    }

    /**
     * {@inheritDoc}
     */
    public String getCurrentSiteLocale() {

        String siteId = toolManager.getCurrentPlacement().getContext();

        Site currentSite = getSiteOrNull(siteId);

        if (currentSite != null) {
            String locale = currentSite.getProperties().getProperty("locale_string");
            if (locale != null) {
                return locale;
            }
        }

        return null;
    }

    public Tool getCurrentTool() {
        return toolManager.getCurrentTool();
    }

    public String getCurrentToolId() {
        return toolManager.getCurrentPlacement().getId();
    }

    public String getCurrentUserId() {

        Session session = sessionManager.getCurrentSession();
        String userId = session.getUserId();
        return userId;
    }

    public ToolSession getCurrentToolSession() {
        return sessionManager.getCurrentToolSession();
    }

    public void setCurrentToolSession(ToolSession toolSession) {
        sessionManager.setCurrentToolSession(toolSession);
    }

    public String getDisplayNameForTheUser(String userId) {

        try {
            User sakaiUser = userDirectoryService.getUser(userId);
            return FormattedText.escapeHtmlFormattedText(sakaiUser.getDisplayName());
        } catch (Exception e) {
            return userId; // this can happen if the user does not longer exist
            // in the system
        }
    }

    public boolean isCurrentUserAdmin() {
        return securityService.isSuperUser();
    }

    public String getPortalUrl() {

        // don't use serverConfigurationService.getPortalUrl() as it can return
        // 'sakai-entitybroker-direct' instead of 'portal'
        String serverUrl = serverConfigurationService.getServerUrl();
        return serverUrl + serverConfigurationService.getString("portalPath");
    }

    public void registerEntityProducer(EntityProducer entityProducer) {
        entityManager.registerEntityProducer(entityProducer, "/wall");
    }

    public void registerFunction(String function) {

        List functions = functionManager.getRegisteredFunctions("wall.");

        if (!functions.contains(function)) {
            functionManager.registerFunction(function);
        }
    }

    public boolean isAllowedFunction(String function, String siteId) {

        try {
            Site site = siteService.getSite(siteId);
            Role role = site.getUserRole(getCurrentUserId());
            return isAllowedFunction(function, role);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isAllowedFunction(String function, Role role) {

        try {
            if (isCurrentUserAdmin()) {
                return true;
            }

            if (role == null) {
                return false;
            }

            return role.isAllowed(function);
        } catch (Exception e) {
            log.error("Caught exception while performing function test", e);
        }

        return false;
    }

    private void enableSecurityAdvisor(SecurityAdvisor securityAdvisor) {
        securityService.pushAdvisor(securityAdvisor);
    }

    private void disableSecurityAdvisor(SecurityAdvisor securityAdvisor) {
        securityService.popAdvisor(securityAdvisor);
    }

    public void postEvent(String event, String reference, String siteId) {
        eventTrackingService.post(eventTrackingService.newEvent(event, reference, true));
    }

    public Set<String> getSiteUsers(String siteId) {

        try {
            Site site = siteService.getSite(siteId);
            return site.getUsers();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public String getWallToolId(String siteId) {

        try {
            Site site = siteService.getSite(siteId);
            ToolConfiguration tc = site.getToolForCommonId("sakai.wall");
            return tc.getId();
        } catch (Exception e) {
            return "";
        }
    }

    public Set<String> getSitePermissionsForCurrentUser(String siteId, String embedder) {

        String userId = getCurrentUserId();

        if (userId == null) {
            throw new SecurityException("This action (userPerms) is not accessible to anon and there is no current user.");
        }

        Set<String> filteredFunctions = new TreeSet<String>();

        if (securityService.isSuperUser(userId)) {
            // Special case for the super admin
            filteredFunctions.addAll(functionManager.getRegisteredFunctions("wall"));
        } else {
            Site site = null;
            AuthzGroup siteHelperRealm = null;

            try {
                site = siteService.getSite(siteId);
                siteHelperRealm = authzGroupService.getAuthzGroup("!site.helper");
            } catch (Exception e) {
                // This should probably be logged but not rethrown.
            }

            Role siteRole = site.getUserRole(userId);

            Set<String> functions = siteRole.getAllowedFunctions();

            if (siteHelperRealm != null) {
                Role siteHelperRole = siteHelperRealm.getRole(siteRole.getId());
                if (siteHelperRole != null) {
                    // Merge in all the functions from the same role in !site.helper
                    functions.addAll(siteHelperRole.getAllowedFunctions());
                }
            }

            for (String function : functions) {
                if (function.startsWith("wall"))
                    filteredFunctions.add(function);
            }

            if (functions.contains("site.upd")) {
                filteredFunctions.add(WallFunctions.WALL_MODIFY_PERMISSIONS);
            }
        }

        return filteredFunctions;
    }

    public Map<String, Set<String>> getSitePermissions(String siteId) {

        Map<String, Set<String>> perms = new HashMap<String, Set<String>>();

        String userId = getCurrentUserId();

        if (userId == null) {
            throw new SecurityException("This action (perms) is not accessible to anon and there is no current user.");
        }

        try {
            Site site = siteService.getSite(siteId);

            Set<Role> roles = site.getRoles();
            for (Role role : roles) {
                Set<String> functions = role.getAllowedFunctions();
                Set<String> filteredFunctions = new TreeSet<String>();
                for (String function : functions) {
                    if (function.startsWith("wall"))
                        filteredFunctions.add(function);
                }

                perms.put(role.getId(), filteredFunctions);
            }
        } catch (Exception e) {
            log.error("Failed to get current site permissions.", e);
        }

        return perms;
    }

    public boolean setPermissionsForSite(String siteId, Map<String, Object> params) {

        String userId = getCurrentUserId();

        if (userId == null)
            throw new SecurityException("This action (setPerms) is not accessible to anon and there is no current user.");

        Site site = null;

        try {
            site = siteService.getSite(siteId);
        } catch (IdUnusedException ide) {
            log.warn(userId + " attempted to update WALL permissions for unknown site " + siteId);
            return false;
        }

        boolean admin = securityService.isSuperUser(userId);

        try {

            AuthzGroup authzGroup = authzGroupService.getAuthzGroup(site.getReference());

            // admin can update permissions. check for anyone else
            if (!securityService.isSuperUser()) {

                Role siteRole = authzGroup.getUserRole(userId);
                AuthzGroup siteHelperAuthzGroup = authzGroupService.getAuthzGroup("!site.helper");
                Role siteHelperRole = siteHelperAuthzGroup.getRole(siteRole.getId());

                if (!siteRole.isAllowed(WallFunctions.WALL_MODIFY_PERMISSIONS) && !siteRole.isAllowed("site.upd")) {
                    if (siteHelperRole == null || !siteHelperRole.isAllowed(WallFunctions.WALL_MODIFY_PERMISSIONS)) {
                        log.warn(userId + " attempted to update WALL permissions for site " + site.getTitle());
                        return false;
                    }
                }
            }

            boolean changed = false;

            for (String name : params.keySet()) {
                if (!name.contains(":")) {
                    continue;
                }

                String value = (String) params.get(name);

                String roleId = name.substring(0, name.indexOf(":"));

                Role role = authzGroup.getRole(roleId);
                if (role == null) {
                    throw new IllegalArgumentException("Invalid role id '" + roleId + "' provided in POST parameters.");
                }
                String function = name.substring(name.indexOf(":") + 1);

                if ("true".equals(value)) {
                    role.allowFunction(function);
                } else {
                    role.disallowFunction(function);
                }

                changed = true;
            }

            if (changed) {
                try {
                    authzGroupService.save(authzGroup);
                } catch (AuthzPermissionException ape) {
                    throw new SecurityException("The permissions for this site (" + siteId + ") cannot be updated by the current user.");
                }
            }

            return true;
        } catch (GroupNotDefinedException gnde) {
            log.error("No realm defined for site (" + siteId + ").");
        }

        return false;
    }

    public Cache getCache(String cache) {

        try {
            return memoryService.getCache(cache);
        } catch (Exception e) {
            log.error("Exception whilst retrieving '" + cache + "' cache. Returning null ...", e);
            return null;
        }
    }

    public boolean isUserSite(String siteId) {
        return siteService.isUserSite(siteId);
    }
}
