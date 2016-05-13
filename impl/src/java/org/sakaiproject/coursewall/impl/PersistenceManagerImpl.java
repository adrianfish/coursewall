package org.sakaiproject.coursewall.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.sakaiproject.db.api.SqlReader;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.coursewall.api.datamodel.Comment;
import org.sakaiproject.coursewall.api.datamodel.Post;
import org.sakaiproject.coursewall.api.PersistenceManager;
import org.sakaiproject.coursewall.api.QueryBean;
import org.sakaiproject.coursewall.api.SakaiProxy;

/**
 * @author Adrian Fish (adrian.r.fish@gmail.com)
 */
@Getter @Setter @Slf4j
public class PersistenceManagerImpl implements PersistenceManager {

    private static final String POST_SELECT
        = "SELECT cp.*,cw.ID as WALL_ID,cw.SITE_ID,cw.EMBEDDER FROM COURSEWALL_POST as cp, COURSEWALL_WALL as cw, COURSEWALL_WALL_POST as cwp "
            + "WHERE cp.ID = ? AND cp.ID = cwp.POST_ID and cwp.WALL_ID = cw.ID";
    private static final String WALL_SELECT = "SELECT * FROM COURSEWALL_WALL WHERE ID = ?";
    private static final String WALL_POSTS_SELECT
        = "SELECT cw.ID as WALL_ID,cw.SITE_ID,cw.EMBEDDER,cp.* FROM COURSEWALL_WALL as cw,COURSEWALL_WALL_POST as cwp,COURSEWALL_POST as cp "
            + "WHERE cw.ID = ? AND cwp.WALL_ID = cw.ID AND cp.ID = cwp.POST_ID ORDER BY CREATED_DATE DESC";
    private static final String WALL_POST_INSERT = "INSERT INTO COURSEWALl_WALL_POST VALUES(?,?)";
    private static final String WALL_INSERT = "INSERT INTO COURSEWALl_WALL VALUES(?,?,?)";
    private static final String COMMENT_SELECT = "SELECT * FROM COURSEWALL_COMMENT WHERE ID = ?";
    private static final String COMMENTS_SELECT = "SELECT * FROM COURSEWALL_COMMENT WHERE POST_ID = ? ORDER BY CREATED_DATE ASC";
    private static final String COMMENT_INSERT = "INSERT INTO COURSEWALL_COMMENT VALUES(?,?,?,?,?,?)";
    private static final String COMMENT_UPDATE = "UPDATE COURSEWALL_COMMENT SET CONTENT = ?, MODIFIED_DATE = ? WHERE ID = ?";
    private static final String COMMENT_DELETE = "DELETE FROM COURSEWALL_COMMENT WHERE ID = ?";
    private static final String POST_UPDATE = "UPDATE COURSEWALL_POST SET CONTENT = ?, MODIFIED_DATE = ? WHERE ID = ?";
    private static final String POST_INSERT = "INSERT INTO COURSEWALL_POST VALUES (?,?,?,?,?)";
    private static final String POST_DELETE = "DELETE FROM COURSEWALL_POST WHERE ID = ?";
    private static final String WALL_POST_DELETE = "DELETE FROM COURSEWALL_WALL_POST WHERE POST_ID = ?";
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

        if (log.isDebugEnabled()) {
            log.debug("postExists(" + postId + ")");
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

    public List<Post> getAllPost(String wallId) throws Exception {
        return getAllPost(wallId, false);
    }

    public List<Post> getAllPost(String wallId, boolean populate) throws Exception {

        if (log.isDebugEnabled()) {
            log.debug("getAllPost(" + wallId + ")");
        }

        return sqlService.dbRead(WALL_POSTS_SELECT
                , new Object[] {wallId}
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
                        log.error("Failed to get comment", sqle);
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

        log.debug("savePost()");

        if (postExists(post.getId())) {

            sqlService.dbWrite(POST_UPDATE
                , new Object[] { post.getContent()
                                    , new Timestamp(new Date().getTime())
                                    , post.getId() });
        } else {
            Runnable transaction = new Runnable() {

                public void run() {

                    // Test if the wall exists.
                    List<String> wallIds = sqlService.dbRead(WALL_SELECT
                        , new Object[] {post.getWallId()}
                        , new SqlReader<String>() {
                            public String readSqlResultRecord(ResultSet result) {
                                try {
                                    return result.getString("ID");
                                } catch (SQLException sqle) {
                                    return null;
                                }
                            }
                        });

                    if (wallIds.size() == 0) {
                        // Wall doesn't exist yet. Create it.
                        sqlService.dbWrite(WALL_INSERT
                            , new Object [] { post.getWallId(), post.getSiteId(), post.getEmbedder() });
                    }

                    post.setId(UUID.randomUUID().toString());
                    sqlService.dbWrite(POST_INSERT
                        , new Object [] { post.getId()
                                            , post.getContent()
                                            , post.getCreatorId()
                                            , new Timestamp(post.getCreatedDate())
                                            , new Timestamp(post.getModifiedDate())});
                    sqlService.dbWrite(WALL_POST_INSERT
                        , new Object [] { post.getWallId(), post.getId() });
                }
            };
            sqlService.transact(transaction, "COURSEWALL_POST_CREATION_TRANSACTION");
        }

        return getPost(post.getId(), false);
    }

    public boolean deletePost(Post post) {

        if (log.isDebugEnabled()) {
            log.debug("deletePost(" + post.getId() + ")");
        }

        Runnable transaction = new Runnable() {

            public void run() {

                Object[] params = new Object [] { post.getId() };
                sqlService.dbWrite(COMMENTS_DELETE, params);
                sqlService.dbWrite(WALL_POST_DELETE, params);
                sqlService.dbWrite(POST_DELETE, params);
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
                                    log.error("Failed to read comment from DB.", sqle);
                                    return null;
                                }
                            }
                        });
                post.setComments(comments);
            }
            return post;
        } catch (SQLException sqle) {
            log.error("Failed to read post from DB.", sqle);
            return null;
        }
    }
}
