package org.sakaiproject.coursewall.api;

import java.util.*;

import org.sakaiproject.coursewall.api.datamodel.Comment;
import org.sakaiproject.coursewall.api.datamodel.Post;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.EntityProducer;

public interface CoursewallManager extends EntityProducer {

    public static final String ENTITY_PREFIX = "coursewall";
    public static final String REFERENCE_ROOT = Entity.SEPARATOR + ENTITY_PREFIX;
    public static final String COURSEWALL_POST_CREATED = "coursewall.post.created";
    public static final String COURSEWALL_POST_DELETED = "coursewall.post.deleted";
    public static final String COURSEWALL_COMMENT_CREATED = "coursewall.comment.created";
    public static final String COURSEWALL_COMMENT_DELETED = "coursewall.comment.deleted";

    public static final String POST_CACHE = "org.sakaiproject.coursewall.sortedPostCache";
    public static final String AUTHOR_CACHE = "org.sakaiproject.coursewall.authorCache";
    public static final String ALL = "all";

    public Post getPost(String postId) throws Exception;

    // Used by Dashboard integration
    public Post getPostHeader(String postId) throws Exception;

    public List<Post> getPosts(String placementId) throws Exception;

    public List<Post> getPosts(QueryBean query) throws Exception;

    public boolean savePost(Post post);

    public boolean deletePost(String postId);

    public Comment getComment(String commentId) throws Exception;

    public boolean saveComment(Comment comment);

    public boolean deleteComment(String siteId, String commentId);

    public List<CoursewallAuthor> getAuthors(String siteId, String sortedBy);
}
