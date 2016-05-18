<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html  
      xmlns="http://www.w3.org/1999/xhtml"  
      xml:lang="${isolanguage}"
      lang="${isolanguage}">
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <script type="text/javascript">

            var wall = {
                i18n: {},
                wallId: '${wallId}',
                siteId: '${siteId}',
                embedder: '${embedder}',
                userId: '${userId}',
                isUserSite: ${isUserSite}
            };
        
        </script>
        ${sakaiHtmlHead}
        <link rel="stylesheet" type="text/css" href="/library/webjars/jquery-ui/1.11.3/jquery-ui.min.css"></script>
        <link href="/profile2-tool/css/profile2-profile-entity.css" type="text/css" rel="stylesheet" media="all" />
        <link rel="stylesheet" type="text/css" href="/wall-tool/css/wall.css"  media="all"/>
        <script type="text/javascript" src="/library/webjars/jquery/1.11.3/jquery.min.js"></script>
        <script type="text/javascript" src="/library/webjars/jquery-ui/1.11.3/jquery-ui.min.js"></script>
        <script type="text/javascript" src="/library/webjars/jquery-i18n-properties/1.2.2/jquery.i18n.properties.min.js"></script>
        <script type="text/javascript" src="/library/js/jquery/hoverIntent/r7/jquery.hoverIntent.minified.js"></script>
        <script type="text/javascript" src="/library/js/jquery/cluetip/1.2.10/jquery.cluetip.min.js"></script>
        <script type="text/javascript" src="/wall-tool/lib/handlebars.runtime-v4.0.5.js"></script>
        <script type="text/javascript" src="/wall-tool/templates/templates.js"></script>
        <script type="text/javascript" src="/wall-tool/lib/autosize.min.js"></script>
        <script type="text/javascript" src="/wall-tool/lib/sakai_utils.js"></script>
        <script type="text/javascript" src="/wall-tool/js/wall_utils.js"></script>
        <script type="text/javascript" src="/wall-tool/js/wall_permissions.js"></script>
        <script type="text/javascript" src="/profile2-tool/javascript/profile2-eb.js"></script>
    </head>
    <body>

        <div class="portletBody">

            <ul id="wall-toolbar" class="navIntraTool actionToolBar" role="menu"></ul>

            <div id="wall-main-container">
                <div id="wall-content"></div>
            </div>

        </div> <!-- /portletBody-->

        <script type="text/javascript" src="/wall-tool/js/wall.js"></script>

    </body>
</html>
