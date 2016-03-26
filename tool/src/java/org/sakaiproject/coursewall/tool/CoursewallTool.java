package org.sakaiproject.coursewall.tool;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import org.sakaiproject.authz.api.PermissionsHelper;
import org.sakaiproject.coursewall.api.CoursewallManager;
import org.sakaiproject.coursewall.api.SakaiProxy;
import org.sakaiproject.component.api.ComponentManager;
import org.sakaiproject.tool.api.ActiveTool;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.Tool;
import org.sakaiproject.tool.api.ToolException;
import org.sakaiproject.tool.api.ToolSession;
import org.sakaiproject.util.RequestFilter;
import org.sakaiproject.util.ResourceLoader;

/**
 * @author Adrian Fish (a.fish@lancaster.ac.uk)
 */
public class CoursewallTool extends HttpServlet {

    private Logger logger = Logger.getLogger(getClass());

    private SakaiProxy sakaiProxy;
    private CoursewallManager coursewallManager;
    
    public void init(ServletConfig config) throws ServletException {

        super.init(config);

        logger.debug("init");
        
        try {
            ComponentManager componentManager = org.sakaiproject.component.cover.ComponentManager.getInstance();
            sakaiProxy = (SakaiProxy) componentManager.get(SakaiProxy.class);
            coursewallManager = (CoursewallManager) componentManager.get(CoursewallManager.class);
        } catch (Throwable t) {
            throw new ServletException("Failed to initialise CoursewallTool servlet.", t);
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        
        logger.debug("doGet()");

        String userId = null;
        Session session = (Session) request.getAttribute(RequestFilter.ATTR_SESSION);
        if (session != null) {
            userId = session.getUserId();
        } else {
            throw new ServletException("Not logged in.");
        }

        String siteLanguage = sakaiProxy.getCurrentSiteLocale();

        Locale locale = null;
        ResourceLoader rl = null;

        if (siteLanguage != null) {
            String[] parts = siteLanguage.split("_");
            if (parts.length == 1) {
                locale = new Locale(parts[0]);
            } else if (parts.length == 2) {
                locale = new Locale(parts[0], parts[1]);
            } else if (parts.length == 3) {
                locale = new Locale(parts[0], parts[1], parts[2]);
            }
            rl = new ResourceLoader("org.sakaiproject.coursewall");
            rl.setContextLocale(locale);
        } else {
            rl = new ResourceLoader(userId, "org.sakaiproject.coursewall");
            locale = rl.getLocale();
        }

        if (locale == null || rl == null) {
            logger.error("Failed to load the site or user i18n bundle");
        }

        String language = locale.getLanguage();
        String country = locale.getCountry();

        if (country != null && !country.equals("")) {
            language += "_" + country;
        }

        String pathInfo = request.getPathInfo();

        String[] pathParts = (pathInfo == null) ? new String[] {} : pathInfo.split("/");

        for (String part : pathParts) {
            System.out.println("part: " + part);
        }

        String siteId = sakaiProxy.getCurrentSiteId();

        String placementId = (String) request.getAttribute(Tool.PLACEMENT_ID);

        if (pathParts.length > 1 && "permissions".equals(pathParts[1])) {
            ToolSession toolSession = sakaiProxy.getCurrentToolSession();
            toolSession.setAttribute(PermissionsHelper.TARGET_REF, "/site/" + siteId);
            toolSession.setAttribute(PermissionsHelper.DESCRIPTION, "permissions");
            toolSession.setAttribute(PermissionsHelper.PREFIX, "coursewall.");

            Tool tool = sakaiProxy.getCurrentTool();

            String portalUrl = sakaiProxy.getPortalUrl();
            String returnUrl = portalUrl + "/site/" + siteId + "/tool/" + placementId;
            ActiveTool helperTool = sakaiProxy.getActiveTool("sakai.permissions.helper");
            toolSession.setAttribute(Tool.HELPER_DONE_URL, returnUrl);

            try {
                helperTool.help(request, response, "", "");
            } catch (ToolException te) {
                logger.error("Failed to launch permissions helper", te);
            }
        } else {
            request.setAttribute("sakaiHtmlHead", (String) request.getAttribute("sakai.html.head"));
            
            request.setAttribute("isolanguage", language);
            request.setAttribute("placementId", placementId);

            response.setContentType("text/html");
            request.getRequestDispatcher("/WEB-INF/bootstrap.jsp").include(request, response);  
        }
    }
}
