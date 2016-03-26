package org.sakaiproject.coursewall.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.sakaiproject.db.api.SqlReader;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.coursewall.api.datamodel.Comment;
import org.sakaiproject.coursewall.api.datamodel.Post;
import org.sakaiproject.coursewall.api.PersistenceManager;
import org.sakaiproject.coursewall.api.QueryBean;
import org.sakaiproject.coursewall.api.SakaiProxy;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PersistenceManagerImpl implements PersistenceManager {

    private final Logger logger = Logger.getLogger(PersistenceManagerImpl.class);

    private static final String POST_SELECT = "SELECT * FROM COURSEWALL_POST WHERE ID = ?";
    private static final String SITE_POSTS_SELECT = "SELECT * FROM COURSEWALL_POST WHERE SITE_ID = ? ORDER BY CREATED_DATE DESC";
    private static final String COMMENT_SELECT = "SELECT * FROM COURSEWALL_COMMENT WHERE ID = ?";
    private static final String COMMENTS_SELECT = "SELECT * FROM COURSEWALL_COMMENT WHERE POST_ID = ? ORDER BY CREATED_DATE ASC";
    private static final String COMMENT_INSERT = "INSERT INTO COURSEWALL_COMMENT VALUES(?,?,?,?,?,?)";
    private static final String COMMENT_UPDATE = "UPDATE COURSEWALL_COMMENT SET CONTENT = ?, MODIFIED_DATE = ? WHERE ID = ?";
    private static final String COMMENT_DELETE = "DELETE FROM COURSEWALL_COMMENT WHERE ID = ?";
    private static final String POST_UPDATE = "UPDATE COURSEWALL_POST SET CONTENT = ?, MODIFIED_DATE = ? WHERE ID = ?";
    private static final String POST_INSERT = "INSERT INTO COURSEWALL_POST VALUES (?,?,?,?,?,?)";
    private static final String POST_DELETE = "DELETE FROM COURSEWALL_POST WHERE ID = ?";
    private static final String COMMENTS_DELETE = "DELETE FROM COURSEWALL_COMMENT WHERE POST_ID = ?";

    private SakaiProxy sakaiProxy;
    private ServerConfigurationService serverConfigurationService;
    private SqlService sqlService;

    public void init() {

        if (serverConfigurationService.getBoolean("auto.ddl", true)) {
            sqlService.ddl(this.getClass().getClassLoader(), "coursewall_tables");
        }
    }

    public boolean postExists(String postId) {

        if (logger.isDebugEnabled()) {
            logger.debug("postExists(" + postId + ")");
        }

        List<Post> posts = sqlService.dbRead(POST_SELECT
                , new Object[] {postId}
                , new SqlReader<Post>() {
                    public Post readSqlResultRecord(ResultSet result) {
                        return new Post();
                    }
                });

        return posts.size() > 0;
    }

    public List<Post> getAllPost(String siteId) throws Exception {
        return getAllPost(siteId, false);
    }

    public List<Post> getAllPost(String siteId, boolean populate) throws Exception {

        if (logger.isDebugEnabled()) {
            logger.debug("getAllPost(" + siteId + ")");
        }

        return sqlService.dbRead(SITE_POSTS_SELECT
                , new Object[] {siteId}
                , new SqlReader<Post>() {
                    public Post readSqlResultRecord(ResultSet result) {
                        return loadPostFromResult(result, populate);
                    }
                });
    }

    public Comment getComment(String commentId) {

        List<Comment> comments = sqlService.dbRead(COMMENT_SELECT, new Object[] { commentId }, new SqlReader<Comment>() {
                public Comment readSqlResultRecord(ResultSet result) {
                    try {
                        return new Comment(result);
                    } catch (SQLException sqle) {
                        logger.error("Failed to get comment", sqle);
                        return null;
                    }
                }
            });

        Comment comment = comments.get(0);
        comment.setCreatorDisplayName(sakaiProxy.getDisplayNameForTheUser(comment.getCreatorId()));
        return comment;
    }

    public Comment saveComment(Comment comment) {

        if ("".equals(comment.getId())) {
            comment.setId(UUID.randomUUID().toString());
            sqlService.dbWrite(COMMENT_INSERT
                , new Object[] { comment.getId()
                                    , comment.getPostId()
                                    , comment.getContent()
                                    , comment.getCreatorId()
                                    , new Timestamp(comment.getCreatedDate())
                                    , new Timestamp(comment.getModifiedDate()) });

        } else {
            sqlService.dbWrite(COMMENT_UPDATE
                , new Object[] { comment.getContent()
                                    , new Timestamp(comment.getModifiedDate())
                                    , comment.getId() });
        }

        return getComment(comment.getId());
    }

    public boolean deleteComment(String commentId) {

        sqlService.dbWrite(COMMENT_DELETE, new Object[] { commentId });
        return true;
    }

    public Post savePost(Post post) {

        logger.debug("savePost()");

        if (postExists(post.getId())) {

            sqlService.dbWrite(POST_UPDATE
                , new Object[] { post.getContent()
                                    , new Timestamp(new Date().getTime())
                                    , post.getId() });
        } else {
            post.setId(UUID.randomUUID().toString());
            sqlService.dbWrite(POST_INSERT
                , new Object [] { post.getId()
                                    , post.getSiteId()
                                    , post.getContent()
                                    , post.getCreatorId()
                                    , new Timestamp(post.getCreatedDate())
                                    , new Timestamp(post.getModifiedDate())});
        }

        return getPost(post.getId(), false);
    }

    public boolean deletePost(Post post) {

        if (logger.isDebugEnabled()) {
            logger.debug("deletePost(" + post.getId() + ")");
        }

        Runnable transaction = new Runnable() {

            public void run() {

                sqlService.dbWrite(COMMENTS_DELETE, new Object [] { post.getId() });
                sqlService.dbWrite(POST_DELETE, new Object [] { post.getId() });
            }
        };

        return sqlService.transact(transaction, "COURSEWALL_POST_DELETION_TRANSACTION");
    }

    public Post getPost(String postId, boolean loadComments) {

        List<Post> posts = sqlService.dbRead(POST_SELECT, new Object[] { postId }, new SqlReader<Post>() {
                public Post readSqlResultRecord(ResultSet result) {
                    return loadPostFromResult(result, loadComments);
                }
            });

        return posts.get(0);
    }

    public List<Post> getPosts(QueryBean query) throws Exception {

        StringBuilder statement = new StringBuilder();

        statement.append("SELECT * FROM ").append("COURSEWALL_POST");

        List params = new ArrayList();

        if (query.hasConditions()) {
            statement.append(" WHERE ");

            // we know that there are conditions. Build the statement
            if (query.queryBySiteId()) {
                statement.append("SITE_ID = ? AND ");
                params.add(query.getSiteId());
            }

            if (query.queryByCreator()) {
                statement.append("CREATOR_ID = ? AND ");
                params.add(query.getCreator());
            }
        }

        // At this point, we know that there is an AND at the end of the
        // statement. Remove it. 4 is the length of AND with the last space.
        statement = new StringBuilder(statement.toString().substring(0, statement.length() - 4));

        statement.append("ORDER BY CREATED_DATE DESC");

        return sqlService.dbRead(statement.toString(), params.toArray(), new SqlReader<Post>() {
                public Post readSqlResultRecord(ResultSet result) {
                    return loadPostFromResult(result, true);
                }
            });
    }

    private Post loadPostFromResult(ResultSet result, boolean loadComments) {

        try {
            Post post = new Post(result);
            post.setCreatorDisplayName(
                    sakaiProxy.getDisplayNameForTheUser(post.getCreatorId()));
            if (loadComments) {
                List<Comment> comments = sqlService.dbRead(COMMENTS_SELECT
                        , new Object[] {post.getId()}
                        , new SqlReader<Comment>() {
                            public Comment readSqlResultRecord(ResultSet commentResult) {

                                try {
                                    Comment comment = new Comment(commentResult);
                                    comment.setCreatorDisplayName(
                                            sakaiProxy.getDisplayNameForTheUser(comment.getCreatorId()));
                                    String toolId = sakaiProxy.getCoursewallToolId(post.getSiteId());
                                    String url = sakaiProxy.getPortalUrl() + "/directtool/"
                                                            + toolId + "?state=post&postId=" + post.getId();
                                    comment.setUrl(url);
                                    return comment;
                                } catch (SQLException sqle) {
                                    logger.error("Failed to read comment from DB.", sqle);
                                    return null;
                                }
                            }
                        });
                post.setComments(comments);
            }
            return post;
        } catch (SQLException sqle) {
            logger.error("Failed to read post from DB.", sqle);
            return null;
        }
    }
}
