wall.utils = {

    POST_WITHOUT_COMMENTS_STYLE: 'wall-post-without-comments',
    POST_WITH_COMMENTS_STYLE: 'wall-post-with-comments',
    OGP_IMAGE_REGEX: /og:image" content="([^"]*)"/,
    TWITTER_IMAGE_REGEX: /twitter:image" content="([^"]*)"/,
    OGP_TITLE_REGEX: /og:title" content="([^"]*)"/,
    OGP_DESCRIPTION_REGEX: /og:description" content="([^"&#]*)"/,
    OGP_SITE_NAME_REGEX: /og:site_name" content="([^"]*)"/,

    fromHtml: function (html) {
        return html.replace(/<br>/g, '\n');
    },
    toHtml: function (text) {
        return text.replace(/\r\n|\n|\r/g, '<br>');
    },
    getOGPMarkup: function (url, callback) {

        var self = this;

        $.get('/direct/wall/getUrlMarkup?url=' + url, function (markup) {

            var div = document.createElement('div');

            var title = '';
            var matches = markup.match(self.OGP_TITLE_REGEX);
            if (matches && matches.length == 2) {
                title = matches[1];
                div.innerHTML = title;
                title = $(div).html();
            }


            var image = '';
            var matches = markup.match(self.TWITTER_IMAGE_REGEX);
            if (matches && matches.length == 2) {
                image = matches[1];
            } else {
                matches = markup.match(self.OGP_IMAGE_REGEX);
                if (matches && matches.length == 2) {
                    image = matches[1];
                }
            }

            var description = '';
            var matches = markup.match(self.OGP_DESCRIPTION_REGEX);
            if (matches && matches.length == 2) {
                description = matches[1];
                div.innerHTML = description;
                description = $(div).html();
            }

            var siteName = '';
            var matches = markup.match(self.OGP_SITE_NAME_REGEX);
            if (matches && matches.length == 2) {
                siteName = matches[1];
            }

            if (!title && !image) {
                callback(null);
            } else {
                callback(Handlebars.templates['og_fragment']({title: title,
                                                                image: image,
                                                                url: url,
                                                                description: description,
                                                                siteName: siteName}));
            }
        });
    },
    addHandlersToComment: function (commentId) {

        $('#wall-comment-edit-link-' + commentId).click(wall.utils.editCommentHandler);
        $('#wall-comment-delete-link-' + commentId).click(wall.utils.deleteCommentHandler);
    },
    editPostHandler: function (e) {

        var postId = this.dataset.postId;
        var contentDiv = $('#wall-post-content-' + postId);
        wall.utils.oldContent = contentDiv.html();
        contentDiv.prop('contenteditable', true).focus();
        var postEditButtons = $('#wall-post-edit-buttons-'+ postId);
        postEditButtons.show();

        $(document).ready(function () {

            $('#wall-inplace-post-editor-post-button-' + postId).off('click').click(function (e) {

                wall.utils.savePost(postId, contentDiv.html(), function () {

                        contentDiv.prop('contenteditable', false);
                        $('#wall-post-options-' + postId).show();
                        postEditButtons.hide();
                    });
                });
        });
        $('#wall-post-options-' + postId).hide();
    },
    deletePostHandler: function (e) {

        var postId = this.dataset.postId;
        wall.utils.deletePost(postId, function () {
                $('#wall-post-' + postId).remove();
            });
    },
    cancelPostEdit: function (postId) {

        var contentDiv = $('#wall-post-content-' + postId);
        contentDiv.html(this.oldContent);
        contentDiv.prop('contenteditable', false);
        $('#wall-post-options-' + postId).show();
        $('#wall-post-edit-buttons-'+ postId).hide();
    },
    editCommentHandler: function (e) {

        // Get the comment id from the link
        var commentId = this.dataset.commentId;

        var container = $('#wall-comment-' + commentId);

        var contentSpan = $('#wall-comment-content-' + commentId);

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

        wall.utils.addPermissionsToComment(comment);

        wall.utils.commentBeingEdited = comment;
        wall.utils.renderTemplate('inplace_comment_editor', comment, 'wall-comment-' + commentId);

        $(document).ready(function () {

            var textarea = $('#wall-comment-textarea-' + comment.id);
            var tmp = wall.utils.fromHtml(comment.content);
            console.log(tmp);
            textarea.val(wall.utils.fromHtml(comment.content));
            textarea.each(function () { autosize(this); }).focus();

            $('#wall-inplace-comment-editor-cancel-button-' + comment.id).click(function (e) {

                wall.utils.renderTemplate(
                    'comment', wall.utils.commentBeingEdited, 'wall-comment-' + commentId);
                $('#wall-comment-edit-link-' + commentId).click(wall.utils.editCommentHandler);
            });

            $('#wall-inplace-comment-editor-post-button-' + commentId).click(function (e) {

                wall.utils.saveComment(commentId, postId, textarea.val(), function (savedComment) {

                        textarea.val('');
                        $('#wall-comments-' + postId).show();
                        wall.utils.addPermissionsToComment(savedComment);
                        wall.utils.renderTemplate('comment', savedComment, 'wall-comment-' + savedComment.id);
                        wall.utils.addHandlersToComment(commentId);
                    });
            });
        });
    },
    deleteCommentHandler: function (e) {

        if (!confirm(wall.i18n.delete_comment_message)) {
            return false;
        }

        var commentId = this.dataset.commentId;
        var postId = this.dataset.postId;
        var numComments = $('#wall-comments-' + postId + ' .wall-comment').length;
        if (numComments <= 2) {
            $('#wall-hide-comments-link-' + postId).hide();
            $('#wall-show-comments-link-' + postId).hide();
        }
        var commentToDelete = $('#wall-comment-' + commentId);

        if (commentToDelete.hasClass('wall-comment-latest')) {
            commentToDelete.prev().removeClass('wall-comment-not-latest').addClass('wall-comment-latest').show();
        }
        wall.utils.deleteComment(commentId, function () {
                commentToDelete.remove();
            });
    },
    cancelCommentEdit: function (commentId) {

        wall.utils.renderTemplate('comment', wall.utils.commentBeingEdited, 'wall-comment-' + commentId);
        $('#wall-comment-edit-link-' + commentId).click(wall.utils.editCommentHandler);
    },
    getCurrentUserPermissions: function (callback) {

        $.ajax( {
            url: '/direct/wall/userPerms.json?siteId=' + wall.siteId + '&embedder=' + wall.embedder,
            dataType: 'json',
            cache: false,
            timeout: wall.AJAX_TIMEOUT
        }).done(function (json) {
            callback(json.data);
        }).fail(function (xmlHttpRequest, textStatus, error) {
            alert('Failed to get the current user permissions. Status: ' + textStatus + '. Error: ' + error);
        });
    },
    getSitePermissionMatrix: function (callback) {

        $.ajax( {
            url: '/direct/wall/perms.json?siteId=' + wall.siteId,
            dataType: 'json',
            cache: false,
            timeout: wall.AJAX_TIMEOUT
        }).done(function(json) {

            var p = json.data;

            var perms = [];

            for (role in p) {
                var permSet = {'role': role};

                p[role].forEach(function (p) {
                    eval('permSet.' + p.replace(/\./g,'_') + ' = true');
                });

                perms.push(permSet);
            }

            callback(perms);
        }).fail(function(xmlHttpRequest, textStatus, error) {
            alert("Failed to get permissions. Status: " + textStatus + ". Error: " + error);
        });
    },
    savePermissions: function () {

        var myData = { siteId: wall.siteId };
        $('.wall-permission-checkbox').each(function (b) {

            if (this.checked) {
                myData[this.id] = 'true';
            } else {
                myData[this.id] = 'false';
            }
        });

        $.ajax( {
            url: "/direct/wall/savePermissions",
            type: 'POST',
            data: myData,
            dataType: 'text',
            timeout: wall.AJAX_TIMEOUT
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
            cluetipClass: 'wall',
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
            return wall.i18n.none;
        } else {
            var d = new Date(millis);
            var hours = d.getHours();
            if (hours < 10) hours = '0' + hours;
            var minutes = d.getMinutes();
            if (minutes < 10) minutes = '0' + minutes;
            return d.getDate() + " " + wall.i18n.months[d.getMonth()] + " " + d.getFullYear() + " @ " + hours + ":" + minutes;
        }
    },
    addFormattedDatesToPosts: function (posts) {

        posts.forEach(function (p) {
            wall.utils.addFormattedDateToPost(p);
        });
    },
    addFormattedDateToPost: function (post) {

        post.formattedCreatedDate = this.formatDate(post.createdDate);
        post.formattedModifiedDate = this.formatDate(post.modifiedDate);

        post.comments.forEach(function (c, index) {

            if (index < (post.comments.length - 1)) {
                c.orderClass = 'wall-comment-not-latest';
            } else {
                c.orderClass = 'wall-comment-latest';
                c.isLatest = true;
            }
            c.formattedCreatedDate = wall.utils.formatDate(c.createdDate);
            c.formattedModifiedDate = wall.utils.formatDate(c.modifiedDate);
        });
    },
    addFormattedDatesToCurrentPost: function () {
        this.addFormattedDateToPost(wall.currentPost);
    },
    savePost: function (postId, content, callback) {

        var success = false;

        if (!postId) postId = '';

        if ('' == content) {
            alert(wall.i18n.no_content_warning);
            return 0;
        }

        //content = wall.utils.toHtml(content);

        var post = {
                'id': postId,
                'content': content,
                'wallId': wall.wallId,
                'siteId': wall.siteId,
                'embedder': wall.embedder
            };
                
        $.ajax({
            url: '/direct/wall/savePost.json',
            type: 'POST',
            data: post,
            timeout: wall.AJAX_TIMEOUT
        }).done(function (data) {
            callback(data);
        }).fail(function(xmlHttpRequest, textStatus, error) {
                alert("Failed to store post. Status: " + textStatus + '. Error: ' + error);
        });
    },
    saveComment: function (commentId, postId, content, callback) {

        content = wall.utils.toHtml(content);

        var comment = {
                'id': commentId,
                'postId': postId,
                'content': content,
                'wallId': wall.wallId,
                'siteId': wall.siteId,
                'embedder': wall.embedder
            };

        $.ajax( {
            url: "/direct/wall/saveComment.json",
            type: 'POST',
            data: comment,
            timeout: wall.AJAX_TIMEOUT
        }).done(function (comment) {
            callback(comment);
        }).fail(function (xmlHttpRequest, textStatus, error) {
            alert("Failed to save comment. Status: " + textStatus + '. Error: ' + error);
        });

        return false;
    },
    deleteComment: function (commentId, callback) {
        
        $.ajax( {
            url: '/direct/wall/deleteComment?wallId=' + wall.wallId + '&commentId=' + commentId,
            timeout: wall.AJAX_TIMEOUT
        }).done(function (text, status) {
            callback();
        }).fail(function (xmlHttpRequest, textStatus, error) {
            alert("Failed to delete comment. Status: " + textStatus + ". Error: " + error);
        });
        
        return false;
    },
    deletePost: function (postId, callback) {
                        
        if (!confirm(wall.i18n.delete_post_message)) {
            return false;
        }
        
        $.ajax({
            url: '/direct/wall/deletePost?postId=' + postId,
            timeout: wall.AJAX_TIMEOUT
        }).done(function (text, status) {
            callback();
        }).fail(function (xmlHttpRequest, textStatus, error) {
            alert("Failed to delete post. Status: " + textStatus + ". Error: " + error);
        });
        
        return false;
    },
    addPermissionsToPost: function (p) {

        p.currentUserId = wall.userId;

        p.canComment = wall.currentUserPermissions.commentCreate;
        p.canDelete = wall.currentUserPermissions.postDeleteAny
                        || (wall.currentUserPermissions.postDeleteOwn && p.creatorId === wall.userId);
        p.canEdit = wall.currentUserPermissions.postUpdateAny
                        || (wall.currentUserPermissions.postUpdateOwn && p.creatorId === wall.userId);
        p.isModified = p.modifiedDate > p.createdDate;

        p.comments.forEach(function (c) { wall.utils.addPermissionsToComment(c); });
    },
    addPermissionsToComment: function (c) {

        c.canComment = wall.currentUserPermissions.commentCreate;
        c.modified = c.modifiedDate > c.createdDate;
        c.canDelete = wall.currentUserPermissions.commentDeleteAny
                        || (wall.currentUserPermissions.commentDeleteOwn && c.creatorId === wall.userId);
        c.canEdit = wall.currentUserPermissions.commentUpdateAny
                        || (wall.currentUserPermissions.commentUpdateOwn && c.creatorId === wall.userId);
    },
    renderTemplate: function (name, data, output) {

        var template = Handlebars.templates[name];
        document.getElementById(output).innerHTML = template(data);
    },
    renderPost: function (post, output) {

        this.addPermissionsToPost(post);
        post.currentUserId = wall.userId;

        this.renderTemplate('post', post, output);

        var self = this;

        $(document).ready(function () {

            $('#wall-post-edit-link-' + post.id).click(self.editPostHandler);
            $('#wall-post-delete-link-' + post.id).click(self.deletePostHandler);
            var textarea = $('#wall-comment-textarea-' + post.id);
            textarea.each(function () { autosize(this); });
            var creator = $('#wall-comment-creator-' + post.id);
            var commentLink = $('#wall-create-comment-link-' + post.id);
            commentLink.click(function (e) {

                creator.show();
                textarea.focus();
                commentLink.hide();
            });
            $('#wall-inplace-comment-editor-cancel-button-' + post.id).click(function (e) {

                creator.hide();
                commentLink.show();
            });

            var showCommentsLink = $('#wall-show-comments-link-' + post.id);
            var hideCommentsLink = $('#wall-hide-comments-link-' + post.id);
            showCommentsLink.click(function (e) {

                $('#wall-comments-' + post.id + ' .wall-comment-not-latest').show();
                showCommentsLink.hide();
                hideCommentsLink.show();
            });
            hideCommentsLink.click(function (e) {

                $('#wall-comments-' + post.id + ' .wall-comment-not-latest').hide();
                hideCommentsLink.hide();
                showCommentsLink.show();
            });

            $('#wall-inplace-comment-editor-post-button-' + post.id).click(function (e) {

                wall.utils.saveComment('', post.id, textarea.val(), function (savedComment) {

                        textarea.val('');

                        var commentId = savedComment.id;
                    
                        creator.hide();
                        commentLink.show();

                        var numComments = $('#wall-comments-' + post.id + ' .wall-comment').length;

                        if (numComments > 0) {
                            var latestComment = $('#wall-comments-' + post.id + ' .wall-comment-latest');
                            latestComment.removeClass('wall-comment-latest')
                                            .addClass('wall-comment-not-latest');
                            if (numComments == 1 || showCommentsLink.is(':visible')) {
                                latestComment.hide();
                                showCommentsLink.show();
                            }
                        }

                        wall.utils.addPermissionsToComment(savedComment);
                        savedComment.formattedCreatedDate = wall.utils.formatDate(savedComment.createdDate);
                        savedComment.orderClass = 'wall-comment-latest';
                        savedComment.isLatest = true;
                        var wrappedComment = Handlebars.templates['wrapped_comment'] (savedComment);
                        $('#wall-comments-container-' + post.id).append(wrappedComment);

                        self.addHandlersToComment(commentId);
                    });
            });

            if (post.comments.length <= 1) {
                showCommentsLink.hide();
            }

            var comments = $('#wall-comments-' + post.id);
            comments.find('.wall-comment-edit-link').click(wall.utils.editCommentHandler);
            comments.find('.wall-comment-delete-link').click(wall.utils.deleteCommentHandler);
        });
    },
    renderPageOfPosts: function (all) {

        var self = this;

        var loadImage = $('#wall-loading-image')
        loadImage.show();

        var url = '/direct/wall/posts/' + wall.wallId + '.json?siteId='
                        + wall.siteId + '&embedder=' + wall.embedder + '&page=';
        url += (all) ? '-1' : wall.page;

        $.ajax( { url : url, dataType: "json", cache: false, timeout: wall.AJAX_TIMEOUT })
            .done(function (data) {

                if (data.status === 'END') {
                    $(window).off('scroll.wall');
                    loadImage.hide();
                } else {
                    $(window).off('scroll.wall').on('scroll.wall', wall.utils.getScrollFunction(wall.utils.renderPageOfPosts));
                }

                wall.postsTotal = data.postsTotal;
                var posts = data.posts;

                wall.currentPosts = wall.currentPosts.concat(posts);

                if (wall.page == 0 && data.postsTotal > 0) {
                    $('#wall-body-toggle').show();
                }

                wall.postsRendered += posts.length;

                wall.utils.addFormattedDatesToPosts(posts);

                // Add the next batch of placeholders to the post list
                var t = Handlebars.templates['posts_placeholders'];
                $('#wall-posts').append(t({ posts: posts }));

                $(document).ready(function () {

                    // Now render them into their placeholders
                    posts.forEach(function (p) { wall.utils.renderPost(p, 'wall-post-' + p.id); });

                    loadImage.hide();
                });
                wall.page += 1;
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

            if  ((wintop/(docheight-winheight)) > 0.95 || $('body').data('scroll-wall') === true) {
                $('body').data('scroll-wall', false);
                win.off('scroll.wall');
                callback();
            }
        };

        return scroller;
    },
    placeCaretAtEnd: function (el) {

        el.focus();
        if (typeof window.getSelection != "undefined"
                && typeof document.createRange != "undefined") {
            var range = document.createRange();
            range.selectNodeContents(el);
            range.collapse(false);
            var sel = window.getSelection();
            sel.removeAllRanges();
            sel.addRange(range);
        } else if (typeof document.body.createTextRange != "undefined") {
            var textRange = document.body.createTextRange();
            textRange.moveToElementText(el);
            textRange.collapse(false);
            textRange.select();
        }
    }
};
