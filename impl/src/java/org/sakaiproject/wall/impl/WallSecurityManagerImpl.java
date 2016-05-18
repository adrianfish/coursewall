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

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.sakaiproject.assignment.api.AssignmentService;
import org.sakaiproject.authz.api.Role;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.wall.api.datamodel.Post;
import org.sakaiproject.wall.api.WallFunctions;
import org.sakaiproject.wall.api.WallSecurityManager;
import org.sakaiproject.wall.api.SakaiProxy;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.tool.api.ToolManager;

/**
 * @author Adrian Fish (adrian.r.fish@gmail.com)
 */
@Setter @Slf4j
public class WallSecurityManagerImpl implements WallSecurityManager {

    private SakaiProxy  sakaiProxy;
    private SecurityService securityService;
    private SiteService siteService;
    private ToolManager toolManager;

    public boolean canCurrentUserCommentOnPost(Post post) {

        log.debug("canCurrentUserCommentOnPost()");

        if (sakaiProxy.isAllowedFunction(WallFunctions.WALL_COMMENT_CREATE, post.getSiteId())) {
            return true;
        }

        // An author can always comment on their own posts
        if (post.getCreatorId().equals(sakaiProxy.getCurrentUserId())) {
            return true;
        }

        return false;
    }

    public boolean canCurrentUserDeletePost(Post post) throws SecurityException {

        if (sakaiProxy.isAllowedFunction(WallFunctions.WALL_POST_DELETE_ANY, post.getSiteId())) {
            return true;
        }

        String currentUser = sakaiProxy.getCurrentUserId();

        // If the current user is the author and has wall.post.delete.own
        if (currentUser != null && currentUser.equals(post.getCreatorId())
                && sakaiProxy.isAllowedFunction(WallFunctions.WALL_POST_DELETE_OWN, post.getSiteId())) {
            return true;
        }

        return false;
    }

    public boolean canCurrentUserEditPost(Post post) {

        // This acts as an override
        if (sakaiProxy.isAllowedFunction(WallFunctions.WALL_POST_UPDATE_ANY, post.getSiteId())) {
            return true;
        }

        String currentUser = sakaiProxy.getCurrentUserId();

        // If the current user is authenticated and the post author, yes.
        if (currentUser != null && currentUser.equals(post.getCreatorId()) && sakaiProxy.isAllowedFunction(WallFunctions.WALL_POST_UPDATE_OWN, post.getSiteId())) {
            return true;
        }

        return false;
    }

    /**
     * Tests whether the current user can read each Post and if not, filters
     * that post out of the resulting list
     */
    public List<Post> filter(List<Post> posts, String siteId, String embedder) {

        System.out.println("embedder: " + embedder);
        System.out.println("posts: " + posts.size());

        if (posts != null && posts.size() > 0) {
            if (embedder.equals("SITE")) {
                boolean readAny = securityService.unlock(WallFunctions.WALL_POST_READ_ANY, "/site/" + siteId);
                return (readAny) ? posts : new ArrayList<Post>();
            } else if (embedder.equals("ASSIGNMENT")) {
                boolean readAny = securityService.unlock(AssignmentService.SECURE_ADD_ASSIGNMENT_SUBMISSION, "/site/" + siteId);
                return (readAny) ? posts : new ArrayList<Post>();
            } else if (embedder.equals("SOCIAL")) {
                return posts;
            } else {
                return new ArrayList<Post>();
            } 
        } else {
            return posts;
        }
    }

    public boolean canCurrentUserReadPost(Post post) {

        Site site = sakaiProxy.getSiteOrNull(post.getSiteId());

        if (site != null) {
            return securityService.unlock(WallFunctions.WALL_POST_READ_ANY, "/site/" + post.getSiteId());
        } else {
            return false;
        }
    }

    public Site getSiteIfCurrentUserCanAccessTool(String siteId) {

        Site site;
        try {
            site = siteService.getSiteVisit(siteId);
        } catch (Exception e) {
            return null;
        }

        //check user can access the tool, it might be hidden
        ToolConfiguration toolConfig = site.getToolForCommonId("sakai.wall");
        if(!toolManager.isVisible(site, toolConfig)) {
            return null;
        }

        return site;
    }
}
