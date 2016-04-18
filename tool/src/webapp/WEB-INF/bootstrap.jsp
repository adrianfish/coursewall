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

            var coursewall = {
                postId: '${postId}',
                i18n: {}
            };
        
        </script>
        ${sakaiHtmlHead}
        <link rel="stylesheet" type="text/css" href="/library/js/jquery/jquery-ui/css/smoothness/jquery-ui.css" media="all"/>
        <link href="/profile2-tool/css/profile2-profile-entity.css" type="text/css" rel="stylesheet" media="all" />
        <link rel="stylesheet" type="text/css" href="/coursewall-tool/css/coursewall.css"  media="all"/>
        <script type="text/javascript" src="/library/webjars/jquery/1.11.3/jquery.min.js"></script>
        <script type="text/javascript" src="/library/webjars/jquery-ui/1.11.3/jquery-ui.min.js"></script>
        <script type="text/javascript" src="/library/js/jquery/hoverIntent/r7/jquery.hoverIntent.minified.js"></script>
        <script type="text/javascript" src="/library/js/jquery/cluetip/1.2.10/jquery.cluetip.min.js"></script>
        <script type="text/javascript" src="/coursewall-tool/lib/jquery.i18n.properties.js"></script>
        <script type="text/javascript" src="/coursewall-tool/lib/handlebars.runtime-v4.0.5.js"></script>
        <script type="text/javascript" src="/coursewall-tool/templates/partials.js"></script>
        <script type="text/javascript" src="/coursewall-tool/templates/templates.js"></script>
        <script type="text/javascript" src="/coursewall-tool/lib/autosize.min.js"></script>
        <script type="text/javascript" src="/coursewall-tool/lib/sakai_utils.js"></script>
        <script type="text/javascript" src="/coursewall-tool/js/coursewall_utils.js"></script>
        <script type="text/javascript" src="/coursewall-tool/js/coursewall_permissions.js"></script>
        <script type="text/javascript" src="/profile2-tool/javascript/profile2-eb.js"></script>
    </head>
    <body>

        <div class="portletBody">

            <ul id="coursewall_toolbar" class="navIntraTool actionToolBar" role="menu"></ul>

            <div id="coursewallMainContainer">
                <div id="coursewall_content"></div>
            </div>

        </div> <!-- /portletBody-->

        <script type="text/javascript" src="/coursewall-tool/js/coursewall.js"></script>

    </body>
</html>
