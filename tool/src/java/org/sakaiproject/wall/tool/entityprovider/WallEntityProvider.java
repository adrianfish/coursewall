package org.sakaiproject.wall.tool.entityprovider;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import org.sakaiproject.authz.api.PermissionsHelper;
import org.sakaiproject.wall.api.datamodel.Comment;
import org.sakaiproject.wall.api.datamodel.Post;
import org.sakaiproject.wall.api.datamodel.PostsData;
import org.sakaiproject.wall.api.WallManager;
import org.sakaiproject.wall.api.WallSecurityManager;
import org.sakaiproject.wall.api.QueryBean;
import org.sakaiproject.wall.api.SakaiProxy;
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

/**
 * @author Adrian Fish (adrian.r.fish@gmail.com)
 */
@Slf4j
public class WallEntityProvider extends AbstractEntityProvider implements RequestAware, AutoRegisterEntityProvider, Outputable, Describeable, ActionsExecutable, ReferenceParseable {
    
    public final static String ENTITY_PREFIX = "wall";

    private final static String USER_AGENT
        = "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2";

    @Setter
    private WallManager wallManager;
    
    @Setter
    private WallSecurityManager wallSecurityManager;

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
       return new EntityReference("sakai:wall");
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
        
        String wallId = view.getPathSegment(2);
        String siteId = (String) params.get("siteId");
        String embedder = (String) params.get("embedder");

        if (StringUtils.isBlank(wallId) || StringUtils.isBlank(siteId) || StringUtils.isBlank(embedder)) {
            throw new EntityException(
                "Bad request: To get the posts in a wall you need a url like '/direct/wall/posts/WALLID.json?siteId=siteId&embedder=SITE'"
                                            , "", HttpServletResponse.SC_BAD_REQUEST);
        }
        
        /*
        if (wallSecurityManager.getSiteIfCurrentUserCanAccessTool(siteId) == null) {
            throw new EntityException("Access denied.", "", HttpServletResponse.SC_UNAUTHORIZED);
        }
        */

        List<Post> posts = new ArrayList<Post>();

        boolean isUserSite = sakaiProxy.isUserSite(siteId);

        QueryBean query = new QueryBean();
        query.wallId = wallId;
        query.siteId = siteId;
        query.embedder = embedder;
        query.isUserSite = isUserSite;

        try {
            posts = wallManager.getPosts(query);

            PostsData data = new PostsData();
            data.postsTotal = posts.size();

            int page = Integer.parseInt((String) params.get("page"));

            if (page == -1) {
                data.status = "END";
                data.posts = posts;
            } else {

                int pageSize = 20;
                int start  = page * pageSize;

                if (start >= data.postsTotal) {
                    data.status = "END";
                } else {
                    int end = start + pageSize;

                    if (log.isDebugEnabled()) {
                        log.debug("end: " + end);
                    }

                    if (end >= data.postsTotal) {
                        end = data.postsTotal;
                        data.status = "END";
                    }

                    data.posts = posts.subList(start, end);
                }
            }

            return new ActionReturn(data);
        } catch (Exception e) {
            log.error("Caught exception whilst getting posts.", e);
            throw new EntityException("Failed to retrieve posts for site " + siteId, "", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @EntityCustomAction(action = "savePost", viewKey = EntityView.VIEW_NEW)
    public ActionReturn handleSavePost(Map<String, Object> params) {

        log.debug("handleSavePost");

        String userId = developerHelperService.getCurrentUserId();

        if (userId == null) {
            throw new EntityException("You need to be logged in to store a post"
                                                , "", HttpServletResponse.SC_FORBIDDEN);
        }

        String content = (String) params.get("content");
        String siteId = (String) params.get("siteId");
        String wallId = (String) params.get("wallId");
        String embedder = (String) params.get("embedder");

        if (StringUtils.isBlank(content) || StringUtils.isBlank(siteId)
                || StringUtils.isBlank(wallId) || StringUtils.isBlank(embedder)) {
            throw new EntityException("You must supply a siteId, wallId, embedder and some content"
                                                , "", HttpServletResponse.SC_BAD_REQUEST);
        }

        //content = escape(content);

        String id = (String) params.get("id");

        boolean isNew = "".equals(id);

        Post post = new Post();
        post.setId(id);
        post.setCreatorId(userId);
        post.setSiteId(siteId);
        post.setWallId(wallId);
        post.setEmbedder(embedder);
        post.setContent(content);

        Post createdOrUpdatedPost = wallManager.savePost(post);
        if (createdOrUpdatedPost != null) {
            if (isNew) {
                sakaiProxy.postEvent(WallManager.WALL_POST_CREATED,
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

        log.debug("handleDeletePost");

        if (developerHelperService.getCurrentUserId() == null) {
            throw new EntityException("You need to be logged in to delete comments"
                                                , "", HttpServletResponse.SC_FORBIDDEN);
        }

        String postId = (String) params.get("postId");
        if (StringUtils.isBlank(postId)) {
            throw new EntityException("You must supply a postId"
                                                , "", HttpServletResponse.SC_BAD_REQUEST);
        }

        if (wallManager.deletePost(postId)) {
            return new ActionReturn("SUCCESS");
        } else {
            return new ActionReturn("FAIL");
        }
    }

    @EntityCustomAction(action = "deleteComment", viewKey = EntityView.VIEW_LIST)
    public ActionReturn handleDeleteComment(Map<String, Object> params) {

        log.debug("handleDeleteComment");

        if (developerHelperService.getCurrentUserId() == null) {
            throw new EntityException("You need to be logged in to delete comments"
                                                , "", HttpServletResponse.SC_FORBIDDEN);
        }

        String wallId = (String) params.get("wallId");
        String commentId = (String) params.get("commentId");

        if (StringUtils.isBlank(wallId) || StringUtils.isBlank(commentId)) {
            throw new EntityException("You must supply a wallId and a commentId"
                                                , "", HttpServletResponse.SC_BAD_REQUEST);
        }

        if (wallManager.deleteComment(wallId, commentId)) {
            return new ActionReturn("SUCCESS");
        } else {
            return new ActionReturn("FAIL");
        }
    }

    @EntityCustomAction(action = "saveComment", viewKey = EntityView.VIEW_NEW)
    public ActionReturn handleSaveComment(Map<String, Object> params) {

        log.debug("handleSaveComment");

        String userId = developerHelperService.getCurrentUserId();

        String postId = (String) params.get("postId");
        String content = (String) params.get("content");
        String wallId = (String) params.get("wallId");
        String siteId = (String) params.get("siteId");

        if (StringUtils.isBlank(content) || StringUtils.isBlank(wallId) || StringUtils.isBlank(postId) || StringUtils.isBlank(siteId)) {
            throw new EntityException("You must supply a wallId, siteId, postId and some content"
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

        Comment savedComment = wallManager.saveComment(wallId, comment);
        if (savedComment != null) {
            if (isNew) {
                String reference = WallManager.REFERENCE_ROOT + "/" + wallId + "/posts/" + postId + "/comments/" + comment.getId();
                sakaiProxy.postEvent(WallManager.WALL_COMMENT_CREATED, reference, siteId);
            }
            return new ActionReturn(savedComment);
        } else {
            return new ActionReturn("FAIL");
        }
    }

    @EntityCustomAction(action = "userPerms", viewKey = EntityView.VIEW_LIST)
    public Set<String> handleUserPermsGet(EntityView view, Map<String, Object> params) {

        String userId = developerHelperService.getCurrentUserId();
        
        if (userId == null) {
            throw new EntityException("You must be logged in to retrieve perms", "", HttpServletResponse.SC_UNAUTHORIZED);
        }

        String siteId = (String) params.get("siteId");
        String embedder = (String) params.get("embedder");

        if (StringUtils.isBlank(siteId) || StringUtils.isBlank(embedder)) {
            throw new EntityException("No siteId or embedder supplied", "", HttpServletResponse.SC_BAD_REQUEST);
        }

        return sakaiProxy.getSitePermissionsForCurrentUser(siteId, embedder);
    }

    @EntityCustomAction(action = "perms", viewKey = EntityView.VIEW_LIST)
    public Map<String, Set<String>> handlePermsGet(EntityView view, Map<String, Object> params) {

        String userId = developerHelperService.getCurrentUserId();
        
        if (userId == null) {
            throw new EntityException("You must be logged in to retrieve perms", "", HttpServletResponse.SC_UNAUTHORIZED);
        }

        String siteId = (String) params.get("siteId");

        if (StringUtils.isBlank(siteId)) {
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

    @EntityCustomAction(action = "getUrlMarkup", viewKey = EntityView.VIEW_LIST)
    public ActionReturn getUrlMarkup(OutputStream outputStream, EntityView view, Map<String, Object> params) {

        String userId = developerHelperService.getCurrentUserId();
        
        if (userId == null) {
            throw new EntityException("You must be logged in to get html", "", HttpServletResponse.SC_UNAUTHORIZED);
        }

        String urlString = (String) params.get("url");

        if (StringUtils.isBlank(urlString)) {
            throw new EntityException("No url supplied", "", HttpServletResponse.SC_BAD_REQUEST);
        }

        try {
            URL url = new URL(urlString);
            URLConnection conn = url.openConnection();
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.connect();
            String contentEncoding = conn.getContentEncoding();
            String contentType = conn.getContentType();

            if (contentType != null 
                    && (contentType.startsWith("text/html")
                            || contentType.startsWith("application/xhtml+xml")
                            || contentType.startsWith("application/xml"))) {
                String mimeType = contentType.split(";")[0].trim();
                if (log.isDebugEnabled()) {
                    log.debug("mimeType: " + mimeType);
                    log.debug("encoding: " + contentEncoding);
                }

                BufferedReader reader
                    = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                BufferedWriter writer
                    = new BufferedWriter(new OutputStreamWriter(outputStream));

                String line = null;
                while ((line = reader.readLine()) != null) {
                    writer.write(line);
                }

                return new ActionReturn(contentEncoding, mimeType, outputStream);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Invalid content type " + contentType + ". Throwing bad request ...");
                }
                throw new EntityException("Url content type not supported", "", HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (MalformedURLException mue) {
            throw new EntityException("Invalid url supplied", "", HttpServletResponse.SC_BAD_REQUEST);
        } catch (IOException ioe) {
            throw new EntityException("Failed to download url contents", "", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private String escape(String unescaped) {
        return StringEscapeUtils.escapeJava(unescaped);
    }
}
