coursewall.utils = {

    POST_WITHOUT_COMMENTS_STYLE: 'coursewall-post-without-comments',
    POST_WITH_COMMENTS_STYLE: 'coursewall-post-with-comments',

    removeRoundedBottomFromPost: function (postId) {
        //$('#coursewall-post-outer-container-' + postId).removeClass(this.POST_WITHOUT_COMMENTS_STYLE);
        $('#coursewall-post-outer-container-' + postId).addClass(this.POST_WITH_COMMENTS_STYLE);
    },
    addHandlersToComment: function (commentId) {

        $('#coursewall-comment-edit-link-' + commentId).click(coursewall.utils.editCommentHandler);
        $('#coursewall-comment-delete-link-' + commentId).click(coursewall.utils.deleteCommentHandler);
    },
    addRoundedBottomToPost: function (postId) {
        $('#coursewall-post-outer-container-' + postId).removeClass(this.POST_WITH_COMMENTS_STYLE);
    },
    editPostHandler: function (e) {

        var postId = this.dataset.postId;
        var contentDiv = $('#coursewall-post-content-' + postId);
        coursewall.utils.oldContent = contentDiv.html();
        var editorTemplate = Handlebars.templates['inplace_post_editor'];
        var markup = editorTemplate({postId: postId, content: coursewall.utils.oldContent});
        contentDiv.html(markup).find('textarea').focus();
        $('#coursewall-post-options-' + postId).hide();
        $('#coursewall-inplace-post-editor-post-button-' + postId).click(function (e) {

            var textarea = $('#coursewall-inplace-post-editor-' + postId);
            coursewall.utils.storePost(postId, textarea, function () {

                    contentDiv.html(textarea.val());
                    $('#coursewall-post-options-' + postId).show();
                });
        });
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
                content: contentSpan.html()
            };

        coursewall.utils.addPermissionsToComment(comment);

        coursewall.utils.commentBeingEdited = comment;
        coursewall.utils.renderTemplate('inplace_comment_editor', comment, 'coursewall-comment-' + commentId);
        var textarea = container.find('textarea');
        textarea.focus();

        $('#coursewall-inplace-comment-editor-post-button-' + commentId).click(function (e) {

            coursewall.utils.saveComment(commentId, postId, textarea.val(), function (savedComment) {

                    textarea.val('');
                    $('#coursewall-comments-' + postId).show();
                    coursewall.utils.addPermissionsToComment(savedComment);
                    coursewall.utils.renderTemplate('comment', savedComment, 'coursewall-comment-' + savedComment.id);
                    coursewall.utils.addHandlersToComment(commentId);
                });
        });
    },
    deleteCommentHandler: function (e) {

        var commentId = this.dataset.commentId;
        var postId = this.dataset.postId;
        coursewall.utils.deleteComment(commentId, function () {

                $('#coursewall-comment-' + commentId).remove();
                var comments = document.getElementById('coursewall-comments-' + postId);
                if (comments.childElementCount == 0) {
                    comments.style.display = 'none';
                    coursewall.utils.addRoundedBottomToPost(postId);
                }
            });
    },
    cancelCommentEdit: function (commentId) {

        coursewall.utils.renderTemplate('comment', coursewall.utils.commentBeingEdited, 'coursewall-comment-' + commentId);
        $('#coursewall-comment-edit-link-' + commentId).click(coursewall.utils.editCommentHandler);
    },
    getCurrentUserPermissions: function (callback) {

        $.ajax( {
            url: "/direct/coursewall/userPerms.json?siteId=" + portal.siteId,
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
            url: "/direct/coursewall/perms.json?siteId=" + portal.siteId,
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

        var myData = { siteId: portal.siteId };
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
    storePost: function (postId, editor, callback) {

        var success = false;

        if (!postId) postId = '';

        var content = editor.val();

        if ('' == content) {
            alert(coursewall.i18n.no_content_warning);
            return 0;
        }

        var post = {
                'id': postId,
                'content': content,
                'siteId': portal.siteId
            };
                
        $.ajax({
            url: '/direct/coursewall/storePost.json',
            type: 'POST',
            data: post,
            timeout: coursewall.AJAX_TIMEOUT
        }).done(function (data) {
            callback(data);
        }).fail(function(xmlHttpRequest, textStatus, error) {
                alert("Failed to store post. Status: " + textStatus + '. Error: ' + error);
        });
    },
    saveComment: function (commentId, postId, text, callback) {

        var comment = {
                'id': commentId,
                'postId': postId,
                'content': text,
                'siteId': portal.siteId
            };

        $.ajax( {
            url: "/direct/coursewall/storeComment.json",
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
            url: '/direct/coursewall/deleteComment?siteId=' + portal.siteId + '&commentId=' + commentId,
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

        p.currentUserId = portal.user.id;

        p.canComment = coursewall.currentUserPermissions.commentCreate;
        p.canDelete = coursewall.currentUserPermissions.postDeleteAny
                        || (coursewall.currentUserPermissions.postDeleteOwn && p.creatorId === portal.user.id);
        p.canEdit = coursewall.currentUserPermissions.postUpdateAny
                        || (coursewall.currentUserPermissions.postUpdateOwn && p.creatorId === portal.user.id);
        p.isModified = p.modifiedDate > p.createdDate;

        p.comments.forEach(function (c) { coursewall.utils.addPermissionsToComment(c); });
    },
    addPermissionsToComment: function (c) {

        c.canComment = coursewall.currentUserPermissions.commentCreate;
        c.modified = c.modifiedDate > c.createdDate;
        c.canDelete = coursewall.currentUserPermissions.commentDeleteAny
                        || (coursewall.currentUserPermissions.commentDeleteOwn && c.creatorId === portal.user.id);
        c.canEdit = coursewall.currentUserPermissions.commentUpdateAny
                        || (coursewall.currentUserPermissions.commentUpdateOwn && c.creatorId === portal.user.id);
    },
    renderTemplate: function (name, data, output) {

        var template = Handlebars.templates[name];
        document.getElementById(output).innerHTML = template(data);
    },
    renderPost: function (post, output) {

        this.addPermissionsToPost(post);
        this.renderTemplate('post', post, output);

        var self = this;

        $(document).ready(function () {

            $('#coursewall-post-edit-link-' + post.id).click(self.editPostHandler);
            $('#coursewall-post-delete-link-' + post.id).click(self.deletePostHandler);

            $('#coursewall-post-comment-link-' + post.id).click(function (e) {
                $('#coursewall-comment-editor-' + post.id).show();
            });

            $('#coursewall-comment-editor-post-button-' + post.id).click(function (e) {

                    var textarea = $('#coursewall-comment-editor-textarea-' + post.id);

                    coursewall.utils.saveComment('', post.id, textarea.val(), function (savedComment) {

                            textarea.val('');

                            var commentId = savedComment.id;
                        
                            $('#coursewall-comment-editor-' + post.id).hide();
                            coursewall.utils.addPermissionsToComment(savedComment);
                            var wrappedComment = Handlebars.templates['wrapped_comment'] (savedComment);
                            $('#coursewall-comments-' + post.id).prepend(wrappedComment).show();
                            self.addHandlersToComment(commentId);
                            self.removeRoundedBottomFromPost(post.id);
                        });
                });

            $('#coursewall-comment-editor-cancel-button-' + post.id).click(function (e) {
                $('#coursewall-comment-editor-' + post.id).hide();
                $('#coursewall-comment-editor-textarea-' + post.id).val('Write a comment ...');
            });

            $('#coursewall-comment-editor-textarea-' + post.id)
                .focus(function (e) {

                    if (this.value === 'Write a comment ...') {
                        this.value = '';
                        $('#coursewall-comment-post-buttons-' + post.id).show();
                    }
                }).each(function () {
                    autosize(this);
                });

            var comments = $('#coursewall-comments-' + post.id);
            comments.find('.coursewall-comment-edit-link').click(coursewall.utils.editCommentHandler);
            comments.find('.coursewall-comment-delete-link').click(coursewall.utils.deleteCommentHandler);

            if (post.comments.length > 0) {
                comments.show();
                self.removeRoundedBottomFromPost(post.id);
            } else {
                comments.hide();
                self.addRoundedBottomToPost(post.id);
            }
        });
    },
    renderPageOfPosts: function (args) {

        var self = this;

        var loadImage = $('#coursewall-loading-image')
        loadImage.show();

        var url = '/direct/coursewall/posts/' + portal.siteId + '.json?page=' + coursewall.page;

        $.ajax( { url : url, dataType: "json", cache: false, timeout: coursewall.AJAX_TIMEOUT})
            .done(function (data) {

                if (data.status === 'END') {
                    $(window).off('scroll.coursewall');
                    loadImage.hide();
                } else {
                    $(window).off('scroll.coursewall').on('scroll.coursewall', coursewall.utils.getScrollFunction(args, coursewall.utils.renderPageOfPosts));
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
    checkScroll: function () {

        // Check if there is no scroll rendered and there are more pages

        // Check if body height is lower than window height (scrollbar missed, maybe you need to get more pages automatically)
        if ($("body").height() <= $(window).height()) {
            setTimeout(function () {

                if (coursewall.postsTotal > coursewall.postsRendered && coursewall.postsRendered > 0 && coursewall.postsRendered % 10 === 0) {
                    $("body").data("scroll-coursewall", true);
                    $(window).trigger('scroll.coursewall');
                }
            }, 100);
        }
    },
    getScrollFunction: function (args, callback) {

        var scroller = function () {
            
            var wintop = $(window).scrollTop(), docheight = $(document).height(), winheight = $(window).height();

            if  ((wintop/(docheight-winheight)) > 0.95 || $("body").data("scroll-coursewall") === true) {
                $("body").data("scroll-coursewall", false);
                $(window).off('scroll.coursewall');
                callback(args);
            }
        };

        return scroller;
    }
};

