package org.sakaiproject.coursewall.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.sakaiproject.coursewall.api.CoursewallFunctions;
import org.sakaiproject.coursewall.api.CoursewallManager;
import org.sakaiproject.coursewall.api.CoursewallSecurityManager;
import org.sakaiproject.coursewall.api.PersistenceManager;
import org.sakaiproject.coursewall.api.QueryBean;
import org.sakaiproject.coursewall.api.SakaiProxy;
import org.sakaiproject.coursewall.api.XmlDefs;
import org.sakaiproject.coursewall.api.datamodel.Comment;
import org.sakaiproject.coursewall.api.datamodel.Post;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.HttpAccess;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.memory.api.Cache;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Adrian Fish (adrian.r.fish@gmail.com)
 */
@Setter @Slf4j
public class CoursewallManagerImpl implements CoursewallManager {

    private PersistenceManager persistenceManager;
    private CoursewallSecurityManager coursewallSecurityManager;
    private SakaiProxy sakaiProxy;

    public void init() {
        
        if (log.isDebugEnabled()) {
            log.debug("init()");
        }

        log.info("Registering Coursewall functions ...");

        sakaiProxy.registerFunction(CoursewallFunctions.COURSEWALL_POST_CREATE);
        sakaiProxy.registerFunction(CoursewallFunctions.COURSEWALL_POST_READ_ANY);
        sakaiProxy.registerFunction(CoursewallFunctions.COURSEWALL_POST_UPDATE_ANY);
        sakaiProxy.registerFunction(CoursewallFunctions.COURSEWALL_POST_UPDATE_OWN);
        sakaiProxy.registerFunction(CoursewallFunctions.COURSEWALL_POST_DELETE_ANY);
        sakaiProxy.registerFunction(CoursewallFunctions.COURSEWALL_POST_DELETE_OWN);
        sakaiProxy.registerFunction(CoursewallFunctions.COURSEWALL_COMMENT_CREATE);
        sakaiProxy.registerFunction(CoursewallFunctions.COURSEWALL_COMMENT_READ_ANY);
        sakaiProxy.registerFunction(CoursewallFunctions.COURSEWALL_COMMENT_READ_OWN);
        sakaiProxy.registerFunction(CoursewallFunctions.COURSEWALL_COMMENT_UPDATE_ANY);
        sakaiProxy.registerFunction(CoursewallFunctions.COURSEWALL_COMMENT_UPDATE_OWN);
        sakaiProxy.registerFunction(CoursewallFunctions.COURSEWALL_COMMENT_DELETE_ANY);
        sakaiProxy.registerFunction(CoursewallFunctions.COURSEWALL_COMMENT_DELETE_OWN);
        sakaiProxy.registerFunction(CoursewallFunctions.COURSEWALL_MODIFY_PERMISSIONS);

        log.info("Registered Coursewall functions ...");

        sakaiProxy.registerEntityProducer(this);
    }

    public Post getPost(String postId, boolean includeComments) {
        return persistenceManager.getPost(postId, includeComments);
    }

    public Post getPostHeader(String postId) throws Exception {

        if (log.isDebugEnabled()) {
            log.debug("getUnfilteredPost(" + postId + ")");
        }

        Post post = persistenceManager.getPost(postId, false);
        post.setContent("");
        return post;
    }

    private List<Post> getPosts(String siteId) throws Exception {
        return coursewallSecurityManager.filter(persistenceManager.getAllPost(siteId), siteId);
    }

    public List<Post> getPosts(QueryBean query) throws Exception {

        Cache cache = sakaiProxy.getOrCreateCache(POST_CACHE);
        if (query.queryBySiteId()) {
            String siteId = query.getSiteId();

            if (!cache.containsKey(siteId)) {
                if (log.isDebugEnabled()) {
                    log.debug("Cache miss on site id: " + siteId);
                }
                cache.put(siteId, new HashMap<String, List<Post>>());
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Cache hit on site id: " + siteId);
                }
            }

            Map<String, List<Post>> siteMap = (Map<String, List<Post>>) cache.get(siteId);

            String key = ALL;

            if (query.queryByCreator()) {
                key = query.getCreator();
            }

            if (log.isDebugEnabled()) {
                log.debug("KEY: " + key);
            }

            if (!siteMap.containsKey(key)) {
                if (log.isDebugEnabled()) {
                    log.debug("Cache miss on '" + key + "'. It will be added.");
                }
                siteMap.put(key, persistenceManager.getPosts(query));
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Cache hit on '" + key + "'");
                }
            }
            return coursewallSecurityManager.filter((List<Post>) siteMap.get(key), siteId);
        } else {
            return coursewallSecurityManager.filter(persistenceManager.getPosts(query), null);
        }
    }

    public Post savePost(Post post) {

        try {
            Post newOrUpdatedPost = persistenceManager.savePost(post);
            if (newOrUpdatedPost != null) {
                removeSiteFromCaches(newOrUpdatedPost.getSiteId());
                return newOrUpdatedPost;
            } else {
                log.error("Failed to save post");
            }
        } catch (Exception e) {
            log.error("Caught exception whilst saving post", e);
        }

        return null;
    }

    public boolean deletePost(String postId) {

        try {
            Post post = persistenceManager.getPost(postId, false);
            if (coursewallSecurityManager.canCurrentUserDeletePost(post)) {
                if (persistenceManager.deletePost(post)) {
                    // Invalidate all caches for this site
                    removeSiteFromCaches(post.getSiteId());
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public Comment saveComment(String siteId, Comment comment) {

        try {
            Comment savedComment = persistenceManager.saveComment(comment);
            if (savedComment != null) {
                removeSiteFromCaches(siteId);
                return savedComment;
            }
        } catch (Exception e) {
            log.error("Caught exception whilst saving comment", e);
        }

        return null;
    }

    public boolean deleteComment(String siteId, String commentId) {

        try {
            if (persistenceManager.deleteComment(commentId)) {
                removeSiteFromCaches(siteId);
                return true;
            }
        } catch (Exception e) {
            log.error("Caught exception whilst deleting comment.", e);
        }

        return false;
    }

    private String serviceName() {
        return CoursewallManager.class.getName();
    }

    public String archive(String siteId, Document doc, Stack stack, String archivePath, List attachments) {

        if (log.isDebugEnabled()) {
            log.debug("archive(siteId:" + siteId + ",archivePath:" + archivePath + ")");
        }

        StringBuilder results = new StringBuilder();

        results.append(getLabel() + ": Started.\n");

        int postCount = 0;

        try {
            // start with an element with our very own (service) name
            Element element = doc.createElement(serviceName());
            element.setAttribute("version", "11.x");
            ((Element) stack.peek()).appendChild(element);
            stack.push(element);

            Element coursewall = doc.createElement("coursewall");
            List<Post> posts = getPosts(siteId);
            if (posts != null && posts.size() > 0) {
                for (Post post : posts) {
                    Element postElement = post.toXml(doc, stack);
                    coursewall.appendChild(postElement);
                    postCount++;
                }
            }

            ((Element) stack.peek()).appendChild(coursewall);
            stack.push(coursewall);

            stack.pop();

            results.append(getLabel() + ": Finished. " + postCount + " post(s) archived.\n");
        } catch (Exception any) {
            results.append(getLabel() + ": exception caught. Message: " + any.getMessage());
            log.warn(getLabel() + " exception caught. Message: " + any.getMessage());
        }

        stack.pop();

        return results.toString();
    }

    /**
     * From EntityProducer
     */
    public String merge(String siteId, Element root, String archivePath, String fromSiteId, Map attachmentNames, Map userIdTrans, Set userListAllowImport) {

        if (log.isDebugEnabled()) {
            log.debug("merge(siteId:" + siteId + ",root tagName:" + root.getTagName() + ",archivePath:" + archivePath + ",fromSiteId:" + fromSiteId);
        }

        StringBuilder results = new StringBuilder();

        int postCount = 0;

        NodeList postNodes = root.getElementsByTagName(XmlDefs.POST);
        final int numberPosts = postNodes.getLength();

        for (int i = 0; i < numberPosts; i++) {
            Node child = postNodes.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                log.error("Post nodes should be elements. Skipping ...");
                continue;
            }

            Element postElement = (Element) child;

            Post post = new Post();
            post.fromXml(postElement);
            post.setSiteId(siteId);

            savePost(post);

            for (Comment comment : post.getComments()) {
                comment.setPostId(post.getId());
                saveComment(siteId, comment);
            }

            postCount++;
        }

        results.append("Stored " + postCount + " posts.");

        return results.toString();
    }

    /**
     * From EntityProducer
     */
    public Entity getEntity(Reference ref) {

        if (log.isDebugEnabled()) {
            log.debug("getEntity(Ref ID:" + ref.getId() + ")");
        }

        Entity rv = null;

        try {
            String reference = ref.getReference();

            String[] parts = reference.split(Entity.SEPARATOR);

            if (parts.length == 5) {
                String postId = parts[4];
                rv = persistenceManager.getPost(postId, true);
            }
        } catch (Exception e) {
            log.warn("getEntity(): " + e);
        }

        return rv;
    }

    /**
     * From EntityProducer
     */
    public Collection getEntityAuthzGroups(Reference ref, String userId) {

        if (log.isDebugEnabled()) {
            log.debug("getEntityAuthzGroups(Ref ID:" + ref.getId() + "," + userId + ")");
        }

        List ids = new ArrayList();
        ids.add("/site/" + ref.getContext());
        return ids;
    }

    public String getEntityDescription(Reference arg0) {
        return null;
    }

    public ResourceProperties getEntityResourceProperties(Reference ref) {

        try {
            String reference = ref.getReference();

            int lastIndex = reference.lastIndexOf(Entity.SEPARATOR);
            String postId = reference.substring(lastIndex, reference.length() - lastIndex);
            Entity entity = persistenceManager.getPost(postId, false);
            return entity.getProperties();
        } catch (Exception e) {
            log.warn("getEntity(): " + e);
            return null;
        }
    }

    /**
     * From EntityProducer
     */
    public String getEntityUrl(Reference ref) {
        return getEntity(ref).getUrl();
    }

    /**
     * From EntityProducer
     */
    public HttpAccess getHttpAccess() {
        return null;
    }

    /**
     * From EntityProducer
     */
    public String getLabel() {
        return "coursewall";
    }

    /**
     * From EntityProducer
     */
    public boolean parseEntityReference(String referenceString, Reference reference) {

        String[] parts = referenceString.split(Entity.SEPARATOR);

        if (parts.length < 2 || !parts[1].equals("coursewall")) // Leading slash adds
                                                          // an empty element
            return false;

        if (parts.length == 2) {
            reference.set("sakai:coursewall", "", "", null, "");
            return true;
        }

        String siteId = parts[2];
        String subType = parts[3];
        String entityId = parts[4];

        if ("posts".equals(subType)) {
            reference.set("coursewall", "posts", entityId, null, siteId);
            return true;
        }

        return false;
    }

    public boolean willArchiveMerge() {
        return true;
    }

    public String getEntityPrefix() {
        return CoursewallManager.ENTITY_PREFIX;
    }

    public boolean entityExists(String id) {

        String postId = id.substring(id.lastIndexOf(Entity.SEPARATOR));

        try {
            if (persistenceManager.postExists(postId))
                return true;
        } catch (Exception e) {
            log.error("entityExists threw an exception", e);
        }

        return false;
    }

    private void removeSiteFromCaches(String siteId) {

        Cache postCache = sakaiProxy.getOrCreateCache(POST_CACHE);
        postCache.remove(siteId);
    }
}
