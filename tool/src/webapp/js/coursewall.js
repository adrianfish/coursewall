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

    $("#coursewall-post-editor").toggle(coursewall.currentUserPermissions.postCreate);

	if (coursewall.states.POSTS === state) {

        var templateData = {
                currentUserId: coursewall.userId,
                siteId: coursewall.siteId
            };

        // renderPageOfPosts uses this. Set it to the start page
        coursewall.page = 0;
        coursewall.postsRendered = 0;

        coursewall.currentPosts = [];

        coursewall.utils.renderTemplate(coursewall.states.POSTS, templateData, 'coursewall_content');

        $(document).ready(function () {

            $('.coursewall-post-editor')
                .toggle(coursewall.currentUserPermissions.postCreate)
                .find('textarea').focus(function (e) {

                    if (this.value === 'Type something ...') {
                        this.value = '';
                        $('#coursewall-editor-post-button').prop('disabled', false);
                    }
                }).each(function () {
                    autosize(this);
                });

            var textarea = $('#coursewall-post-creator-textarea');

            // This button is used to send the newly created post.
            $('#coursewall-editor-post-button').click(function (e) {

                coursewall.utils.savePost('', textarea.val(), function (post) {

                        textarea.val('');

                        var newPlaceholderId = 'coursewall-post-' + post.id;

                        $('#coursewall-posts').prepend(
                            '<div id=\"' + newPlaceholderId + '\" class=\"coursewall-post\"></div>');
                        coursewall.utils.addFormattedDateToPost(post);
                        coursewall.utils.renderPost(post, newPlaceholderId);
                        textarea.val('');
                    });
            });

            $('#coursewall-editor-cancel-button').click(function (e) {

                textarea.val(coursewall.i18n.post_editor_initial_text);
                $('#coursewall-editor-post-button').prop('disabled', true);
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
	    $('#coursewall_toolbar > li > span').removeClass('current');
	    $('#coursewall_permissions_link > span').addClass('current');

        var permissionsCallback = function (perms) {

                coursewall.utils.renderTemplate('permissions', {'perms': perms}, 'coursewall_content');

                $(document).ready(function () {
                    $('#coursewall_permissions_save_button').click(coursewall.utils.savePermissions);
                });
            };

		coursewall.utils.getSitePermissionMatrix(permissionsCallback);
	} else if (coursewall.states.PERMISSIONS_NOT_SET === state) {
        coursewall.utils.renderTemplate('permissions_not_set', {}, 'coursewall_content');
    }
};

(function ($) {

	if (!coursewall.siteId) {
		alert('The site id MUST be supplied as page parameters');
		return;
	}

    var languagesLoaded = function () {

        coursewall.i18n = $.i18n.map;

        coursewall.i18n.months = coursewall.i18n.months.split(',');

        coursewall.monthMappings = {};
        coursewall.i18n.months.forEach(function (m, i) {
            coursewall.monthMappings[m] = i + 1;
        });

        if (!coursewall.assignmentId) {
            coursewall.utils.renderTemplate('toolbar', {} ,'coursewall_toolbar');

            $('#coursewall_permissions_link>span>a').click(function (e) {
                coursewall.switchState(coursewall.states.PERMISSIONS);
            });
        }

        var permissionsCallback = function (permissions) {

                coursewall.currentUserPermissions = new CoursewallPermissions(permissions);

                if (coursewall.currentUserPermissions == null) {
                    return;
                }

                $("#coursewall_permissions_link").toggle(coursewall.currentUserPermissions.modifyPermissions);

                if (coursewall.currentUserPermissions.postReadAny || coursewall.currentUserPermissions.postCreate) {
                    coursewall.switchState(coursewall.states.POSTS, {});
                } else {
                    coursewall.switchState(coursewall.states.PERMISSIONS_NOT_SET, {});
                }
            };

        coursewall.utils.getCurrentUserPermissions(permissionsCallback);
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
}) (jQuery);

