/* Stuff that we always expect to be setup */
commons.currentUserPermissions = null;
commons.currentPost = null;
commons.currentPosts = [];
commons.currentState = null;
commons.page = 0;
commons.postsTotal = 0;
commons.postsRendered = 0;

commons.LOCAL_STORAGE_KEY = 'commons';
commons.AJAX_TIMEOUT = 5000;

Handlebars.registerPartial('comment', Handlebars.partials['comment']);
Handlebars.registerPartial('wrapped_comment', Handlebars.partials['wrapped_comment']);
Handlebars.registerPartial('inplace_comment_editor', Handlebars.partials['inplace_comment_editor']);
Handlebars.registerHelper('translate', function (key) {
    return commons.i18n[key];
});

commons.states = {
        POSTS: 'posts',
        PERMISSIONS: 'permissions',
        PERMISSIONS_NOT_SET: 'permissions_not_set'
    };

commons.switchState = function (state, arg) {

	commons.currentState = state;

	// Just in case we have a floating cluetip hanging about
	$('#cluetip').hide();

    $("#commons-post-editor").toggle(commons.currentUserPermissions.postCreate);

	if (commons.states.POSTS === state) {

        var templateData = {
                currentUserId: commons.userId,
                isUserSite: commons.isUserSite
            };

        // renderPageOfPosts uses this. Set it to the start page
        commons.page = 0;
        commons.postsRendered = 0;

        commons.currentPosts = [];

        commons.utils.renderTemplate(commons.states.POSTS, templateData, 'commons-content');

        $(document).ready(function () {

            $('.commons-post-editor').toggle(commons.currentUserPermissions.postCreate);

            var editor = $('#commons-post-creator-editor');
            editor.click(function (e) {

                if (this.innerHTML == 'Type something ...') {
                    this.innerHTML = '';
                    $('#commons-editor-post-button').prop('disabled', false);
                }
            }).on('paste', function (e) {

                var cd = e.originalEvent.clipboardData;
                if (!cd) cd = window.clipboardData;
                var pasted = cd.getData('text');
                var url = document.createElement('a');
                url.href = pasted;
                if (url.hostname) {
                    // We need to add the protocol for the server side code. It needs a valid URL.
                    if (pasted.slice(0, url.protocol.length) !== url.protocol) {
                        pasted = url.protocol + '//' + pasted;
                    }

                    var wrapped = '<a href=\"' + pasted + '" target="_blank">' + pasted + "</a>";

                    if (!document.execCommand('insertHtml', false, wrapped)) {
                        var sel = window.getSelection();
                        var range = sel.getRangeAt(0);
                        url.innerHTML = pasted;
                        url.target = '_blank';
                        range.insertNode(url);
                    }

                    var self = $(this);
                    commons.utils.getOGPMarkup(pasted, function (fragment) {

                        if (fragment) {
                            self.append(fragment);
                        }
                    });
                    e.preventDefault();
                }
            });

            $('#commons-editor-post-button').click(function (e) {

                commons.utils.savePost('', editor.html(), function (post) {

                        editor.html('');

                        var newPlaceholderId = 'commons-post-' + post.id;

                        $('#commons-posts').prepend(
                            '<div id=\"' + newPlaceholderId + '\" class=\"commons-post\"></div>');
                        commons.utils.addFormattedDateToPost(post);
                        commons.utils.renderPost(post, newPlaceholderId);
                    });
            });

            $('#commons-editor-cancel-button').click(function (e) {

                editor.html(commons.i18n.post_editor_initial_text);
                $('#commons-editor-post-button').prop('disabled', true);
            });
            
            //if (window.parent === window) {
                commons.utils.renderPageOfPosts();
            /*} else {
                commons.utils.renderPageOfPosts(true);
                try {
                    if (window.frameElement) {
                        setMainFrameHeight(window.frameElement.id);
                    }
                } catch (err) {
                    // This is likely under an LTI provision scenario.
                    // XSS protection will block this call.
                }
            }*/
        });
	} else if (commons.states.PERMISSIONS === state) {
	    $('#commons-toolbar > li > span').removeClass('current');
	    $('#commons-permissions-link > span').addClass('current');

        var permissionsCallback = function (perms) {

                commons.utils.renderTemplate('permissions', {'perms': perms}, 'commons-content');

                $(document).ready(function () {
                    $('#commons_permissions_save_button').click(commons.utils.savePermissions);
                });
            };

		commons.utils.getSitePermissionMatrix(permissionsCallback);
	} else if (commons.states.PERMISSIONS_NOT_SET === state) {
        commons.utils.renderTemplate('permissions_not_set', {}, 'commons-content');
    }
};

(function ($) {

	if (!commons.isUserSite && !commons.commonsId) {
		alert('The commonsId MUST be supplied as page parameters');
		return;
	}

    var languagesLoaded = function () {

        commons.i18n = $.i18n.map;

        commons.i18n.months = commons.i18n.months.split(',');

        commons.monthMappings = {};
        commons.i18n.months.forEach(function (m, i) {
            commons.monthMappings[m] = i + 1;
        });

        if (commons.embedder === 'SITE') {
            commons.utils.renderTemplate('toolbar', {} ,'commons-toolbar');

            $('#commons-permissions-link>span>a').click(function (e) {
                commons.switchState(commons.states.PERMISSIONS);
            });
        }

        var permissionsCallback = function (permissions) {

                commons.currentUserPermissions = new CommonsPermissions(permissions);

                if (commons.currentUserPermissions == null) {
                    return;
                }

                $("#commons-permissions-link").toggle(commons.currentUserPermissions.modifyPermissions);

                if (commons.currentUserPermissions.postReadAny || commons.currentUserPermissions.postCreate) {
                    commons.switchState(commons.states.POSTS, {});
                } else {
                    commons.switchState(commons.states.PERMISSIONS_NOT_SET, {});
                }
            };

        commons.utils.getCurrentUserPermissions(permissionsCallback);
    };

    $.i18n.properties({
        name:'ui',
        path:'/commons-tool/i18n/',
        mode: 'both',
        checkAvailableLanguages: true,
        async: true,
        language: sakai.locale.userLocale,
        callback: function () { languagesLoaded(); }
    });

    if (CKEDITOR) {
        CKEDITOR.disableAutoInline = true;
    }

    commons.scrollable = $(window.frameElement ? window.frameElement.ownerDocument.defaultView : window);
    commons.doc = $(window.frameElement ? window.frameElement.ownerDocument : document);

}) (jQuery);

