coursewall.utils = {

    POST_WITHOUT_COMMENTS_STYLE: 'coursewall-post-without-comments',
    POST_WITH_COMMENTS_STYLE: 'coursewall-post-with-comments',

    fromHtml: function (html) {
        return html.replace(/<br>/g, '\n');
    },
    toHtml: function (text) {
        return text.replace(/\r\n|\n|\r/g, '<br>');
    },
    addHandlersToComment: function (commentId) {

        $('#coursewall-comment-edit-link-' + commentId).click(coursewall.utils.editCommentHandler);
        $('#coursewall-comment-delete-link-' + commentId).click(coursewall.utils.deleteCommentHandler);
    },
    editPostHandler: function (e) {

        var postId = this.dataset.postId;
        var contentDiv = $('#coursewall-post-content-' + postId);
        coursewall.utils.oldContent = contentDiv.html();
        var replacedContent = coursewall.utils.fromHtml(coursewall.utils.oldContent);
        var editorTemplate = Handlebars.templates['inplace_post_editor'];
        var editorMarkup = editorTemplate({postId: postId, content: replacedContent});
        contentDiv.html(editorMarkup);

        $(document).ready(function () {

            var textarea = $('#coursewall-inplace-post-editor-' + postId);
            textarea.each(function () { autosize(this); }).focus();
            $('#coursewall-inplace-post-editor-post-button-' + postId).click(function (e) {

                coursewall.utils.savePost(postId, textarea.val(), function () {

                        contentDiv.html(coursewall.utils.toHtml(textarea.val()));
                        $('#coursewall-post-options-' + postId).show();
                    });
                });
        });
        $('#coursewall-post-options-' + postId).hide();
    },
    deletePostHandler: function (e) {

        var postId = this.dataset.postId;
        coursewall.utils.deletePost(postId, function () {
                $('#coursewall-post-' + postId).remove();
            });
    },
    cancelPostEdit: function (postId) {

        var contentDiv = $('#coursewall-post-content-' + postId);
        contentDiv.html(this.oldContent);
        $('#coursewall-post-options-' + postId).show();
    },
    editCommentHandler: function (e) {

        // Get the comment id from the link
        var commentId = this.dataset.commentId;

        var container = $('#coursewall-comment-' + commentId);

        var contentSpan = $('#coursewall-comment-content-' + commentId);

        var postId = container.data('post-id');

        // Comment metadata is attached to the container
        var comment = {
                id: commentId,
                postId: postId,
                creatorId: container.data('creator-id'),
                creatorDisplayName: container.data('creator-display-name'),
                createdDate: container.data('created-date'),
                modifiedDate: container.data('modified-date'),
                content: contentSpan.html(),
            };

        coursewall.utils.addPermissionsToComment(comment);

        coursewall.utils.commentBeingEdited = comment;
        coursewall.utils.renderTemplate('inplace_comment_editor', comment, 'coursewall-comment-' + commentId);

        $(document).ready(function () {

            var textarea = $('#coursewall-comment-textarea-' + comment.id);
            var tmp = coursewall.utils.fromHtml(comment.content);
            console.log(tmp);
            textarea.val(coursewall.utils.fromHtml(comment.content));
            textarea.each(function () { autosize(this); }).focus();
            //textarea.trigger('autosize:update');
            //
            $('#coursewall-inplace-comment-editor-cancel-button-' + comment.id).click(function (e) {

                coursewall.utils.renderTemplate(
                    'comment', coursewall.utils.commentBeingEdited, 'coursewall-comment-' + commentId);
                $('#coursewall-comment-edit-link-' + commentId).click(coursewall.utils.editCommentHandler);
            });

            $('#coursewall-inplace-comment-editor-post-button-' + commentId).click(function (e) {

                coursewall.utils.saveComment(commentId, postId, textarea.val(), function (savedComment) {

                        textarea.val('');
                        $('#coursewall-comments-' + postId).show();
                        coursewall.utils.addPermissionsToComment(savedComment);
                        coursewall.utils.renderTemplate('comment', savedComment, 'coursewall-comment-' + savedComment.id);
                        coursewall.utils.addHandlersToComment(commentId);
                    });
            });
        });
    },
    deleteCommentHandler: function (e) {

        var commentId = this.dataset.commentId;
        coursewall.utils.deleteComment(commentId, function () {
                $('#coursewall-comment-' + commentId).remove();
            });
    },
    cancelCommentEdit: function (commentId) {

        coursewall.utils.renderTemplate('comment', coursewall.utils.commentBeingEdited, 'coursewall-comment-' + commentId);
        $('#coursewall-comment-edit-link-' + commentId).click(coursewall.utils.editCommentHandler);
    },
    getCurrentUserPermissions: function (callback) {

        $.ajax( {
            url: "/direct/coursewall/userPerms.json?siteId=" + coursewall.siteId,
            dataType: "json",
            cache: false,
            timeout: coursewall.AJAX_TIMEOUT
        }).done(function (json) {
            callback(json.data);
        }).fail(function (xmlHttpRequest, textStatus, error) {
            alert("Failed to get the current user permissions. Status: " + textStatus + ". Error: " + error);
        });
    },
    getSitePermissionMatrix: function (callback) {

        $.ajax( {
            url: "/direct/coursewall/perms.json?siteId=" + coursewall.siteId,
            dataType: "json",
            cache: false,
            timeout: coursewall.AJAX_TIMEOUT
        }).done(function(json) {

            var p = json.data;

            var perms = [];

            for (role in p) {
                var permSet = {'role': role};

                p[role].forEach(function (p) {
                    eval("permSet." + p.replace(/\./g,"_") + " = true");
                });

                perms.push(permSet);
            }

            callback(perms);
        }).fail(function(xmlHttpRequest, textStatus, error) {
            alert("Failed to get permissions. Status: " + textStatus + ". Error: " + error);
        });
    },
    savePermissions: function () {

        var myData = { siteId: coursewall.siteId };
        $('.coursewall-permission-checkbox').each(function (b) {

            if (this.checked) {
                myData[this.id] = 'true';
            } else {
                myData[this.id] = 'false';
            }
        });

        $.ajax( {
            url: "/direct/coursewall/savePermissions",
            type: 'POST',
            data: myData,
            dataType: 'text',
            timeout: coursewall.AJAX_TIMEOUT
        }).done(function (result) {
            location.reload();
        }).fail(function(xmlHttpRequest, textStatus, error) {
            alert("Failed to save permissions. Status: " + textStatus + '. Error: ' + error);
        });

        return false;
    },
    attachProfilePopup: function () {

        $('a.profile').cluetip({
            width: '620px',
            cluetipClass: 'coursewall',
            sticky: true,
            dropShadow: false,
            arrows: true,
            mouseOutClose: true,
            closeText: '<img src="/library/image/silk/cross.png" alt="close" />',
            closePosition: 'top',
            showTitle: false,
            hoverIntent: true
        });
    },
    formatDate: function (millis) {

        if (millis <= 0) {
            return coursewall.i18n.none;
        } else {
            var d = new Date(millis);
            var hours = d.getHours();
            if (hours < 10) hours = '0' + hours;
            var minutes = d.getMinutes();
            if (minutes < 10) minutes = '0' + minutes;
            return d.getDate() + " " + coursewall.i18n.months[d.getMonth()] + " " + d.getFullYear() + " @ " + hours + ":" + minutes;
        }
    },
    addFormattedDatesToPosts: function (posts) {

        posts.forEach(function (p) {
            coursewall.utils.addFormattedDateToPost(p);
        });
    },
    addFormattedDateToPost: function (post) {

        post.formattedCreatedDate = this.formatDate(post.createdDate);
        post.formattedModifiedDate = this.formatDate(post.modifiedDate);

        post.comments.forEach(function (c) {

            c.formattedCreatedDate = coursewall.utils.formatDate(c.createdDate);
            c.formattedModifiedDate = coursewall.utils.formatDate(c.modifiedDate);
        });
    },
    addFormattedDatesToCurrentPost: function () {
        this.addFormattedDateToPost(coursewall.currentPost);
    },
    savePost: function (postId, content, callback) {

        var success = false;

        if (!postId) postId = '';

        if ('' == content) {
            alert(coursewall.i18n.no_content_warning);
            return 0;
        }

        content = coursewall.utils.toHtml(content);

        var post = {
                'id': postId,
                'content': content,
                'siteId': coursewall.siteId
            };
                
        $.ajax({
            url: '/direct/coursewall/savePost.json',
            type: 'POST',
            data: post,
            timeout: coursewall.AJAX_TIMEOUT
        }).done(function (data) {
            callback(data);
        }).fail(function(xmlHttpRequest, textStatus, error) {
                alert("Failed to store post. Status: " + textStatus + '. Error: ' + error);
        });
    },
    saveComment: function (commentId, postId, content, callback) {

        content = coursewall.utils.toHtml(content);

        var comment = {
                'id': commentId,
                'postId': postId,
                'content': content,
                'siteId': coursewall.siteId
            };

        $.ajax( {
            url: "/direct/coursewall/saveComment.json",
            type: 'POST',
            data: comment,
            timeout: coursewall.AJAX_TIMEOUT
        }).done(function (comment) {
            callback(comment);
        }).fail(function (xmlHttpRequest, textStatus, error) {
            alert("Failed to save comment. Status: " + textStatus + '. Error: ' + error);
        });

        return false;
    },
    deleteComment: function (commentId, callback) {
                        
        if (!confirm(coursewall.i18n.delete_comment_message)) {
            return false;
        }
        
        $.ajax( {
            url: '/direct/coursewall/deleteComment?siteId=' + coursewall.siteId + '&commentId=' + commentId,
            timeout: coursewall.AJAX_TIMEOUT
        }).done(function (text, status) {
            callback();
        }).fail(function (xmlHttpRequest, textStatus, error) {
            alert("Failed to delete comment. Status: " + textStatus + ". Error: " + error);
        });
        
        return false;
    },
    deletePost: function (postId, callback) {
                        
        if (!confirm(coursewall.i18n.delete_post_message)) {
            return false;
        }
        
        $.ajax({
            url: '/direct/coursewall/deletePost?postId=' + postId,
            timeout: coursewall.AJAX_TIMEOUT
        }).done(function (text, status) {
            callback();
        }).fail(function (xmlHttpRequest, textStatus, error) {
            alert("Failed to delete post. Status: " + textStatus + ". Error: " + error);
        });
        
        return false;
    },
    addPermissionsToPost: function (p) {

        p.currentUserId = coursewall.userId;

        p.canComment = coursewall.currentUserPermissions.commentCreate;
        p.canDelete = coursewall.currentUserPermissions.postDeleteAny
                        || (coursewall.currentUserPermissions.postDeleteOwn && p.creatorId === coursewall.userId);
        p.canEdit = coursewall.currentUserPermissions.postUpdateAny
                        || (coursewall.currentUserPermissions.postUpdateOwn && p.creatorId === coursewall.userId);
        p.isModified = p.modifiedDate > p.createdDate;

        p.comments.forEach(function (c) { coursewall.utils.addPermissionsToComment(c); });
    },
    addPermissionsToComment: function (c) {

        c.canComment = coursewall.currentUserPermissions.commentCreate;
        c.modified = c.modifiedDate > c.createdDate;
        c.canDelete = coursewall.currentUserPermissions.commentDeleteAny
                        || (coursewall.currentUserPermissions.commentDeleteOwn && c.creatorId === coursewall.userId);
        c.canEdit = coursewall.currentUserPermissions.commentUpdateAny
                        || (coursewall.currentUserPermissions.commentUpdateOwn && c.creatorId === coursewall.userId);
    },
    renderTemplate: function (name, data, output) {

        var template = Handlebars.templates[name];
        document.getElementById(output).innerHTML = template(data);
    },
    renderPost: function (post, output) {

        this.addPermissionsToPost(post);
        post.currentUserId = coursewall.userId;

        this.renderTemplate('post', post, output);

        var self = this;

        $(document).ready(function () {

            $('#coursewall-post-edit-link-' + post.id).click(self.editPostHandler);
            $('#coursewall-post-delete-link-' + post.id).click(self.deletePostHandler);
            var textarea = $('#coursewall-comment-textarea-' + post.id);
            textarea.each(function () { autosize(this); });
            var creator = $('#coursewall-comment-creator-' + post.id);
            var commentLink = $('#coursewall-create-comment-link-' + post.id);
            commentLink.click(function (e) {

                creator.show();
                textarea.focus();
                commentLink.hide();
            });
            $('#coursewall-inplace-comment-editor-cancel-button-' + post.id).click(function (e) {

                creator.hide();
                commentLink.show();
            });

            $('#coursewall-inplace-comment-editor-post-button-' + post.id).click(function (e) {

                coursewall.utils.saveComment('', post.id, textarea.val(), function (savedComment) {

                        textarea.val('');

                        var commentId = savedComment.id;
                    
                        creator.hide();
                        commentLink.show();

                        coursewall.utils.addPermissionsToComment(savedComment);
                        var wrappedComment = Handlebars.templates['wrapped_comment'] (savedComment);
                        $('#coursewall-comments-container-' + post.id).append(wrappedComment);
                        self.addHandlersToComment(commentId);
                    });
            });

            var comments = $('#coursewall-comments-' + post.id);
            comments.find('.coursewall-comment-edit-link').click(coursewall.utils.editCommentHandler);
            comments.find('.coursewall-comment-delete-link').click(coursewall.utils.deleteCommentHandler);
        });
    },
    renderPageOfPosts: function (all) {

        var self = this;

        var loadImage = $('#coursewall-loading-image')
        loadImage.show();

        var url = '/direct/coursewall/posts/' + coursewall.siteId + '.json?page=';
        url += (all) ? '-1' : coursewall.page;

        $.ajax( { url : url, dataType: "json", cache: false, timeout: coursewall.AJAX_TIMEOUT })
            .done(function (data) {

                if (data.status === 'END') {
                    $(window).off('scroll.coursewall');
                    loadImage.hide();
                } else {
                    $(window).off('scroll.coursewall').on('scroll.coursewall', coursewall.utils.getScrollFunction(coursewall.utils.renderPageOfPosts));
                }

                coursewall.postsTotal = data.postsTotal;
                var posts = data.posts;

                coursewall.currentPosts = coursewall.currentPosts.concat(posts);

                if (coursewall.page == 0 && data.postsTotal > 0) {
                    $('#coursewall-body-toggle').show();
                }

                coursewall.postsRendered += posts.length;

                coursewall.utils.addFormattedDatesToPosts(posts);

                // Add the next batch of placeholders to the post list
                var t = Handlebars.templates['posts_placeholders'];
                $('#coursewall-posts').append(t({ posts: posts }));

                $(document).ready(function () {

                    // Now render them into their placeholders
                    posts.forEach(function (p) { coursewall.utils.renderPost(p, 'coursewall-post-' + p.id); });
                    loadImage.hide();
                });
                coursewall.page += 1;
            }).fail(function (xmlHttpRequest, textStatus, errorThrown) {
                alert("Failed to get posts. Reason: " + errorThrown);
            });
    },
    getScrollFunction: function (callback) {

        var scroller = function () {
            
            var win = $(window);
            var wintop = win.scrollTop();
            var winheight = win.height();
            var docheight = $(document).height()

            if  ((wintop/(docheight-winheight)) > 0.95 || $('body').data('scroll-coursewall') === true) {
                $('body').data('scroll-coursewall', false);
                win.off('scroll.coursewall');
                callback();
            }
        };

        return scroller;
    }
};

