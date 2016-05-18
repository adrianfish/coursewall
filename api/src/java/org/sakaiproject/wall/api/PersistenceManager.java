package org.sakaiproject.wall.api;

import java.util.List;

import org.sakaiproject.wall.api.datamodel.Comment;
import org.sakaiproject.wall.api.datamodel.Post;
import org.sakaiproject.wall.api.QueryBean;

/**
 * @author Adrian Fish (adrian.r.fish@gmail.com)
 */
public interface PersistenceManager {

    public boolean postExists(String postId);
    public List<Post> getAllPost(QueryBean queryBean) throws Exception;
    public List<Post> getAllPost(QueryBean queryBean, boolean populate) throws Exception;
    public Comment getComment(String commentId);
    public Comment saveComment(Comment comment);
    public boolean deleteComment(String commentId);
    public Post savePost(Post post);
    public boolean deletePost(Post post);
    public Post getPost(String postId, boolean loadComments);
}
