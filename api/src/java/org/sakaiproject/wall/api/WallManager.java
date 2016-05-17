package org.sakaiproject.wall.api;

import java.util.*;

import org.sakaiproject.wall.api.datamodel.Comment;
import org.sakaiproject.wall.api.datamodel.Post;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.EntityProducer;

/**
 * @author Adrian Fish (adrian.r.fish@gmail.com)
 */
public interface WallManager extends EntityProducer {

    public static final String ENTITY_PREFIX = "wall";
    public static final String REFERENCE_ROOT = Entity.SEPARATOR + ENTITY_PREFIX;
    public static final String WALL_POST_CREATED = "wall.post.created";
    public static final String WALL_POST_DELETED = "wall.post.deleted";
    public static final String WALL_COMMENT_CREATED = "wall.comment.created";
    public static final String WALL_COMMENT_DELETED = "wall.comment.deleted";

    public static final String POST_CACHE = "org.sakaiproject.wall.sortedPostCache";

    public List<Post> getPosts(QueryBean query) throws Exception;

    public Post savePost(Post post);

    public boolean deletePost(String postId);

    public Comment saveComment(String wallId, Comment comment);

    public boolean deleteComment(String wallId, String commentId);
}
