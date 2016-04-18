package org.sakaiproject.coursewall.tool.entityprovider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import lombok.Setter;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import org.sakaiproject.authz.api.PermissionsHelper;
import org.sakaiproject.coursewall.api.datamodel.Comment;
import org.sakaiproject.coursewall.api.datamodel.Post;
import org.sakaiproject.coursewall.api.datamodel.PostsData;
import org.sakaiproject.coursewall.api.CoursewallManager;
import org.sakaiproject.coursewall.api.CoursewallSecurityManager;
import org.sakaiproject.coursewall.api.QueryBean;
import org.sakaiproject.coursewall.api.SakaiProxy;
import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.entitybroker.EntityView;
import org.sakaiproject.entitybroker.entityprovider.annotations.EntityCustomAction;
import org.sakaiproject.entitybroker.entityprovider.capabilities.ActionsExecutable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.AutoRegisterEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Describeable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Outputable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.ReferenceParseable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.RequestAware;
import org.sakaiproject.entitybroker.entityprovider.extension.ActionReturn;
import org.sakaiproject.entitybroker.entityprovider.extension.Formats;
import org.sakaiproject.entitybroker.entityprovider.extension.RequestGetter;
import org.sakaiproject.entitybroker.exception.EntityException;
import org.sakaiproject.entitybroker.util.AbstractEntityProvider;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.tool.api.Session;

public class CoursewallEntityProvider extends AbstractEntityProvider implements RequestAware, AutoRegisterEntityProvider, Outputable, Describeable, ActionsExecutable, ReferenceParseable {
    
    public final static String ENTITY_PREFIX = "coursewall";

    private final Logger LOG = Logger.getLogger(getClass());

    @Setter
    private CoursewallManager coursewallManager;
    
    @Setter
    private CoursewallSecurityManager coursewallSecurityManager;

    @Setter
    private SakaiProxy sakaiProxy;

    @Setter
    private RequestGetter requestGetter;

    public Object getSampleEntity() {
        return new Post();
    }

    public String getEntityPrefix() {
        return ENTITY_PREFIX;
    }

    public EntityReference getParsedExemplar() {
       return new EntityReference("sakai:coursewall");
    }

    public String[] getHandledOutputFormats() {
        return new String[] { Formats.JSON,Formats.XML };
    }

    @EntityCustomAction(action = "posts", viewKey = EntityView.VIEW_LIST)
    public ActionReturn getPosts(EntityView view, Map<String, Object> params) {
        
        String userId = developerHelperService.getCurrentUserId();
        
        if(userId == null) {
            throw new EntityException("You must be logged in to retrieve a post", "", HttpServletResponse.SC_UNAUTHORIZED);
        }
        
        String siteId = view.getPathSegment(2);

        if(siteId == null) {
            throw new EntityException(
                "Bad request: To get the posts in a site you need a url like '/direct/coursewall/posts/SITEID.json'"
                                            , "", HttpServletResponse.SC_BAD_REQUEST);
        }
        
        if (coursewallSecurityManager.getSiteIfCurrentUserCanAccessTool(siteId) == null) {
            throw new EntityException("Access denied.", "", HttpServletResponse.SC_UNAUTHORIZED);
        }
        
        List<Post> posts = new ArrayList<Post>();

        QueryBean query = new QueryBean();
        query.setSiteId(siteId);

        try {
            int page = Integer.parseInt((String) params.get("page"));
            System.out.println("PAGE:" + page);
            query.setPage(page);
            posts = coursewallManager.getPosts(query);
            int pageSize = 20;
            int start  = page * pageSize;
            int postsTotal = posts.size();

            PostsData data = new PostsData();

            if (start >= postsTotal) {
                data.status = "END";
            } else {
                int end = start + pageSize;

                if (LOG.isDebugEnabled()) {
                    LOG.debug("end: " + end);
                }

                data.postsTotal = postsTotal;

                if (end >= postsTotal) {
                    end = postsTotal;
                    data.status = "END";
                }

                data.posts = posts.subList(start, end);
            }

            return new ActionReturn(data);
        } catch (Exception e) {
            LOG.error("Caught exception whilst getting posts.", e);
            throw new EntityException("Failed to retrieve posts for site " + siteId, "", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @EntityCustomAction(action = "storePost", viewKey = EntityView.VIEW_NEW)
    public ActionReturn handleStorePost(Map<String, Object> params) {

        LOG.debug("handleStorePost");

        String userId = developerHelperService.getCurrentUserId();

        if (userId == null) {
            throw new EntityException("You need to be logged in to store a post"
                                                , "", HttpServletResponse.SC_FORBIDDEN);
        }

        String content = (String) params.get("content");
        String siteId = (String) params.get("siteId");

        if (StringUtils.isBlank(content) || StringUtils.isBlank(siteId)) {
            throw new EntityException("You must supply a siteId and some content"
                                                , "", HttpServletResponse.SC_BAD_REQUEST);
        }

        content = escape(content);

        String id = (String) params.get("id");

        boolean isNew = "".equals(id);

        Post post = new Post();
        post.setId(id);
        post.setCreatorId(userId);
        post.setSiteId(siteId);
        post.setContent(content);

        Post createdOrUpdatedPost = coursewallManager.savePost(post);
        if (createdOrUpdatedPost != null) {
            if (isNew) {
                sakaiProxy.postEvent(CoursewallManager.COURSEWALL_POST_CREATED,
                                        createdOrUpdatedPost.getReference(),
                                        createdOrUpdatedPost.getSiteId());
            }
            return new ActionReturn(createdOrUpdatedPost);
        } else {
            return new ActionReturn("FAIL");
        }
    }

    @EntityCustomAction(action = "deletePost", viewKey = EntityView.VIEW_LIST)
    public ActionReturn handleDeletePost(Map<String, Object> params) {

        LOG.debug("handleDeletePost");

        if (developerHelperService.getCurrentUserId() == null) {
            throw new EntityException("You need to be logged in to delete comments"
                                                , "", HttpServletResponse.SC_FORBIDDEN);
        }

        String postId = (String) params.get("postId");
        if (StringUtils.isBlank(postId)) {
            throw new EntityException("You must supply a postId"
                                                , "", HttpServletResponse.SC_BAD_REQUEST);
        }

        if (coursewallManager.deletePost(postId)) {
            return new ActionReturn("SUCCESS");
        } else {
            return new ActionReturn("FAIL");
        }
    }

    @EntityCustomAction(action = "deleteComment", viewKey = EntityView.VIEW_LIST)
    public ActionReturn handleDeleteComment(Map<String, Object> params) {

        LOG.debug("handleDeleteComment");

        if (developerHelperService.getCurrentUserId() == null) {
            throw new EntityException("You need to be logged in to delete comments"
                                                , "", HttpServletResponse.SC_FORBIDDEN);
        }

        String siteId = (String) params.get("siteId");
        String commentId = (String) params.get("commentId");

        if (StringUtils.isBlank(siteId) || StringUtils.isBlank(commentId)) {
            throw new EntityException("You must supply a siteId and a commentId"
                                                , "", HttpServletResponse.SC_BAD_REQUEST);
        }

        if (coursewallManager.deleteComment(siteId, commentId)) {
            return new ActionReturn("SUCCESS");
        } else {
            return new ActionReturn("FAIL");
        }
    }

    @EntityCustomAction(action = "storeComment", viewKey = EntityView.VIEW_NEW)
    public ActionReturn handleStoreComment(Map<String, Object> params) {

        LOG.debug("handleStoreComment");

        String userId = developerHelperService.getCurrentUserId();

        String postId = (String) params.get("postId");
        String content = (String) params.get("content");
        String siteId = (String) params.get("siteId");

        if (StringUtils.isBlank(content) || StringUtils.isBlank(siteId) || StringUtils.isBlank(postId)) {
            throw new EntityException("You must supply a siteId, postId and some content"
                                                , "", HttpServletResponse.SC_BAD_REQUEST);
        }

        String id = (String) params.get("id");

        Comment comment = new Comment();
        comment.setId(id);
        comment.setPostId(postId);
        comment.setCreatorId(userId);
        comment.setContent(content);

        content = escape(content);

        boolean isNew = "".equals(comment.getId());

        Comment savedComment = coursewallManager.saveComment(siteId, comment);
        if (savedComment != null) {
            if (isNew) {
                String reference = CoursewallManager.REFERENCE_ROOT + "/" + siteId + "/posts/" + postId + "/comments/" + comment.getId();
                sakaiProxy.postEvent(CoursewallManager.COURSEWALL_COMMENT_CREATED, reference, siteId);
            }
            return new ActionReturn(savedComment);
        } else {
            return new ActionReturn("FAIL");
        }
    }

    @EntityCustomAction(action = "user", viewKey = EntityView.VIEW_LIST)
    public List<Post> handleUser(EntityView view, Map<String, Object> params) {
        
        String callingUserId = developerHelperService.getCurrentUserId();
        
        if (callingUserId == null) {
            throw new EntityException("You must be logged in to retrieve a post","",HttpServletResponse.SC_UNAUTHORIZED);
        }
        
        String requestedUserId = view.getPathSegment(2);
        
        List<Post> posts = new ArrayList<Post>();

        QueryBean query = new QueryBean();
        query.setCreator(requestedUserId);

        try {
            posts = coursewallManager.getPosts(query);
        } catch (Exception e) {
            LOG.error("Caught exception whilst getting posts.", e);
            throw new EntityException("Failed to retrieve posts for user " + requestedUserId,"",HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        List<Post> filteredPosts = coursewallSecurityManager.filter(posts, null);
        
        for (Post filteredPost : filteredPosts) {
            filteredPost.minimise();
        }
        
        return filteredPosts;
    }

    @EntityCustomAction(action = "userPerms", viewKey = EntityView.VIEW_LIST)
    public Set<String> handleUserPermsGet(EntityView view, Map<String, Object> params) {

        String userId = developerHelperService.getCurrentUserId();
        
        if (userId == null) {
            throw new EntityException("You must be logged in to retrieve perms","",HttpServletResponse.SC_UNAUTHORIZED);
        }

        String siteId = (String) params.get("siteId");

        if (siteId == null || siteId.length() <= 0) {
            throw new EntityException("No siteId supplied","",HttpServletResponse.SC_BAD_REQUEST);
        }

        return sakaiProxy.getSitePermissionsForCurrentUser(siteId);
    }

    @EntityCustomAction(action = "perms", viewKey = EntityView.VIEW_LIST)
    public Map<String, Set<String>> handlePermsGet(EntityView view, Map<String, Object> params) {

        String userId = developerHelperService.getCurrentUserId();
        
        if (userId == null) {
            throw new EntityException("You must be logged in to retrieve perms", "", HttpServletResponse.SC_UNAUTHORIZED);
        }

        String siteId = (String) params.get("siteId");

        if (siteId == null || siteId.length() <= 0) {
            throw new EntityException("No siteId supplied", "", HttpServletResponse.SC_BAD_REQUEST);
        }

        return sakaiProxy.getSitePermissions(siteId);
    }

    @EntityCustomAction(action = "savePermissions", viewKey = EntityView.VIEW_NEW)
    public String handleSavePermissions(EntityView view, Map<String, Object> params) {

        String userId = developerHelperService.getCurrentUserId();
        
        if (userId == null) {
            throw new EntityException("You must be logged in to save permissions", "", HttpServletResponse.SC_UNAUTHORIZED);
        }

        String siteId = (String) params.get("siteId");

        if (sakaiProxy.setPermissionsForSite(siteId, params)) {
            return "success";
        } else {
            throw new EntityException("Failed to set perms", "", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private String escape(String unescaped) {
        return StringEscapeUtils.escapeJavaScript(unescaped);
    }
}
