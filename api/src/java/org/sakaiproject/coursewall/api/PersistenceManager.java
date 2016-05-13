package org.sakaiproject.coursewall.api;

import java.util.List;

import org.sakaiproject.coursewall.api.datamodel.Comment;
import org.sakaiproject.coursewall.api.datamodel.Post;
import org.sakaiproject.coursewall.api.QueryBean;

/**
 * @author Adrian Fish (adrian.r.fish@gmail.com)
 */
public interface PersistenceManager {

    public boolean postExists(String postId);
    public List<Post> getAllPost(String siteId) throws Exception;
    public List<Post> getAllPost(String siteId, boolean populate) throws Exception;
    public Comment getComment(String commentId);
    public Comment saveComment(Comment comment);
    public boolean deleteComment(String commentId);
    public Post savePost(Post post);
    public boolean deletePost(Post post);
    public Post getPost(String postId, boolean loadComments);
}
