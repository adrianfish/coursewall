/* Stuff that we always expect to be setup */
coursewall.currentUserPermissions = null;
coursewall.currentPost = null;
coursewall.currentPosts = [];
coursewall.currentState = null;
coursewall.page = 0;
coursewall.postsTotal = 0;
coursewall.postsRendered = 0;

coursewall.LOCAL_STORAGE_KEY = 'coursewall';
coursewall.AJAX_TIMEOUT = 5000;

Handlebars.registerPartial('comment', Handlebars.partials['comment']);
Handlebars.registerPartial('wrapped_comment', Handlebars.partials['wrapped_comment']);
Handlebars.registerPartial('inplace_comment_editor', Handlebars.partials['inplace_comment_editor']);
Handlebars.registerHelper('translate', function (key) {
    return coursewall.i18n[key];
});

coursewall.states = {
        POSTS: 'posts',
        PERMISSIONS: 'permissions',
        PERMISSIONS_NOT_SET: 'permissions_not_set'
    };

coursewall.switchState = function (state, arg) {

	coursewall.currentState = state;

	// Just in case we have a floating cluetip hanging about
	$('#cluetip').hide();

    $("#cw-post-editor").toggle(coursewall.currentUserPermissions.postCreate);

	if (coursewall.states.POSTS === state) {

        var templateData = {
                currentUserId: coursewall.userId,
                isUserSite: coursewall.isUserSite
            };

        // renderPageOfPosts uses this. Set it to the start page
        coursewall.page = 0;
        coursewall.postsRendered = 0;

        coursewall.currentPosts = [];

        coursewall.utils.renderTemplate(coursewall.states.POSTS, templateData, 'cw-content');

        $(document).ready(function () {

            $('.cw-post-editor').toggle(coursewall.currentUserPermissions.postCreate);

            var editor = $('#cw-post-creator-editor');
            editor.click(function (e) {

                if (this.innerHTML == 'Type something ...') {
                    this.innerHTML = '';
                    $('#cw-editor-post-button').prop('disabled', false);
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
                    coursewall.utils.getOGPMarkup(pasted, function (fragment) {

                        if (fragment) {
                            self.innerHTML = self.innerHTML + fragment;
                        }
                    });
                }
                e.preventDefault();
            });

            $('#cw-editor-post-button').click(function (e) {

                coursewall.utils.savePost('', editor.html(), function (post) {

                        editor.html('');

                        var newPlaceholderId = 'cw-post-' + post.id;

                        $('#cw-posts').prepend(
                            '<div id=\"' + newPlaceholderId + '\" class=\"cw-post\"></div>');
                        coursewall.utils.addFormattedDateToPost(post);
                        coursewall.utils.renderPost(post, newPlaceholderId);
                    });
            });

            $('#cw-editor-cancel-button').click(function (e) {

                editor.html(coursewall.i18n.post_editor_initial_text);
                $('#cw-editor-post-button').prop('disabled', true);
            });
            
            if (window.parent === window) {
                coursewall.utils.renderPageOfPosts();
            } else {
                coursewall.utils.renderPageOfPosts(true);
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
	} else if (coursewall.states.PERMISSIONS === state) {
	    $('#cw-toolbar > li > span').removeClass('current');
	    $('#cw-permissions-link > span').addClass('current');

        var permissionsCallback = function (perms) {

                coursewall.utils.renderTemplate('permissions', {'perms': perms}, 'cw-content');

                $(document).ready(function () {
                    $('#coursewall_permissions_save_button').click(coursewall.utils.savePermissions);
                });
            };

		coursewall.utils.getSitePermissionMatrix(permissionsCallback);
	} else if (coursewall.states.PERMISSIONS_NOT_SET === state) {
        coursewall.utils.renderTemplate('permissions_not_set', {}, 'cw-content');
    }
};

(function ($) {

	if (!coursewall.isUserSite && !coursewall.wallId) {
		alert('The wallId MUST be supplied as page parameters');
		return;
	}

    var languagesLoaded = function () {

        coursewall.i18n = $.i18n.map;

        coursewall.i18n.months = coursewall.i18n.months.split(',');

        coursewall.monthMappings = {};
        coursewall.i18n.months.forEach(function (m, i) {
            coursewall.monthMappings[m] = i + 1;
        });

        if (!coursewall.wallId) {
            coursewall.utils.renderTemplate('toolbar', {} ,'cw-toolbar');

            $('#cw-permissions-link>span>a').click(function (e) {
                coursewall.switchState(coursewall.states.PERMISSIONS);
            });
        }

        if (coursewall.isUserSite) {
            coursewall.currentUserPermissions = new CoursewallPermissions(['coursewall.post.read.any','coursewall.post.create']);
            coursewall.switchState(coursewall.states.POSTS, {});
        } else {
            var permissionsCallback = function (permissions) {

                    coursewall.currentUserPermissions = new CoursewallPermissions(permissions);

                    if (coursewall.currentUserPermissions == null) {
                        return;
                    }

                    $("#cw-permissions-link").toggle(coursewall.currentUserPermissions.modifyPermissions);

                    if (coursewall.currentUserPermissions.postReadAny || coursewall.currentUserPermissions.postCreate) {
                        coursewall.switchState(coursewall.states.POSTS, {});
                    } else {
                        coursewall.switchState(coursewall.states.PERMISSIONS_NOT_SET, {});
                    }
                };

            coursewall.utils.getCurrentUserPermissions(permissionsCallback);
        }
    };

    $.i18n.properties({
        name:'ui',
        path:'/coursewall-tool/i18n/',
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

