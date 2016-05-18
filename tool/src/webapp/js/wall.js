/* Stuff that we always expect to be setup */
wall.currentUserPermissions = null;
wall.currentPost = null;
wall.currentPosts = [];
wall.currentState = null;
wall.page = 0;
wall.postsTotal = 0;
wall.postsRendered = 0;

wall.LOCAL_STORAGE_KEY = 'wall';
wall.AJAX_TIMEOUT = 5000;

Handlebars.registerPartial('comment', Handlebars.partials['comment']);
Handlebars.registerPartial('wrapped_comment', Handlebars.partials['wrapped_comment']);
Handlebars.registerPartial('inplace_comment_editor', Handlebars.partials['inplace_comment_editor']);
Handlebars.registerHelper('translate', function (key) {
    return wall.i18n[key];
});

wall.states = {
        POSTS: 'posts',
        PERMISSIONS: 'permissions',
        PERMISSIONS_NOT_SET: 'permissions_not_set'
    };

wall.switchState = function (state, arg) {

	wall.currentState = state;

	// Just in case we have a floating cluetip hanging about
	$('#cluetip').hide();

    $("#wall-post-editor").toggle(wall.currentUserPermissions.postCreate);

	if (wall.states.POSTS === state) {

        var templateData = {
                currentUserId: wall.userId,
                isUserSite: wall.isUserSite
            };

        // renderPageOfPosts uses this. Set it to the start page
        wall.page = 0;
        wall.postsRendered = 0;

        wall.currentPosts = [];

        wall.utils.renderTemplate(wall.states.POSTS, templateData, 'wall-content');

        $(document).ready(function () {

            $('.wall-post-editor').toggle(wall.currentUserPermissions.postCreate);

            var editor = $('#wall-post-creator-editor');
            editor.click(function (e) {

                if (this.innerHTML == 'Type something ...') {
                    this.innerHTML = '';
                    $('#wall-editor-post-button').prop('disabled', false);
                }
            }).on('paste', function (e) {

                var cd = e.originalEvent.clipboardData;
                if (!cd) cd = window.clipboardData;
                var pasted = cd.getData('text');
                if (pasted.match(/^https?:\/\//)) {
                    var sel = window.getSelection();
                    var caretPos = -1;
                    if (sel.rangeCount) {
                        var range = sel.getRangeAt(0);
                        if (range.commonAncestorContainer.parentNode == this) {
                            caretPos = range.endOffset;
                        }
                    }
                    var wrapped = '<a href=\"' + pasted + '" target="_blank">' + pasted + "</a>";
                    if (caretPos == -1) {
                        this.innerHTML = this.innerHTML + wrapped;
                    } else {
                        this.innerHTML = this.innerHTML.substring(0,caretPos) + wrapped + this.innerHTML.substring(caretPos);
                    }
                    var self = this;
                    wall.utils.getOGPMarkup(pasted, function (fragment) {

                        if (fragment) {
                            self.innerHTML = self.innerHTML + fragment;
                        }
                    });
                }
                e.preventDefault();
            });

            $('#wall-editor-post-button').click(function (e) {

                wall.utils.savePost('', editor.html(), function (post) {

                        editor.html('');

                        var newPlaceholderId = 'wall-post-' + post.id;

                        $('#wall-posts').prepend(
                            '<div id=\"' + newPlaceholderId + '\" class=\"wall-post\"></div>');
                        wall.utils.addFormattedDateToPost(post);
                        wall.utils.renderPost(post, newPlaceholderId);
                    });
            });

            $('#wall-editor-cancel-button').click(function (e) {

                editor.html(wall.i18n.post_editor_initial_text);
                $('#wall-editor-post-button').prop('disabled', true);
            });
            
            if (window.parent === window) {
                wall.utils.renderPageOfPosts();
            } else {
                wall.utils.renderPageOfPosts(true);
                try {
                    if (window.frameElement) {
                        setMainFrameHeight(window.frameElement.id);
                    }
                } catch (err) {
                    // This is likely under an LTI provision scenario.
                    // XSS protection will block this call.
                }
            }
        });
	} else if (wall.states.PERMISSIONS === state) {
	    $('#wall-toolbar > li > span').removeClass('current');
	    $('#wall-permissions-link > span').addClass('current');

        var permissionsCallback = function (perms) {

                wall.utils.renderTemplate('permissions', {'perms': perms}, 'wall-content');

                $(document).ready(function () {
                    $('#wall_permissions_save_button').click(wall.utils.savePermissions);
                });
            };

		wall.utils.getSitePermissionMatrix(permissionsCallback);
	} else if (wall.states.PERMISSIONS_NOT_SET === state) {
        wall.utils.renderTemplate('permissions_not_set', {}, 'wall-content');
    }
};

(function ($) {

	if (!wall.isUserSite && !wall.wallId) {
		alert('The wallId MUST be supplied as page parameters');
		return;
	}

    var languagesLoaded = function () {

        wall.i18n = $.i18n.map;

        wall.i18n.months = wall.i18n.months.split(',');

        wall.monthMappings = {};
        wall.i18n.months.forEach(function (m, i) {
            wall.monthMappings[m] = i + 1;
        });

        if (wall.embedder === 'SITE') {
            wall.utils.renderTemplate('toolbar', {} ,'wall-toolbar');

            $('#wall-permissions-link>span>a').click(function (e) {
                wall.switchState(wall.states.PERMISSIONS);
            });
        }

        var permissionsCallback = function (permissions) {

                wall.currentUserPermissions = new WallPermissions(permissions);

                if (wall.currentUserPermissions == null) {
                    return;
                }

                $("#wall-permissions-link").toggle(wall.currentUserPermissions.modifyPermissions);

                if (wall.currentUserPermissions.postReadAny || wall.currentUserPermissions.postCreate) {
                    wall.switchState(wall.states.POSTS, {});
                } else {
                    wall.switchState(wall.states.PERMISSIONS_NOT_SET, {});
                }
            };

        wall.utils.getCurrentUserPermissions(permissionsCallback);
    };

    $.i18n.properties({
        name:'ui',
        path:'/wall-tool/i18n/',
        mode: 'both',
        checkAvailableLanguages: true,
        async: true,
        language: sakai.locale.userLocale,
        callback: function () { languagesLoaded(); }
    });

    if (CKEDITOR) {
        CKEDITOR.disableAutoInline = true;
    }

}) (jQuery);

