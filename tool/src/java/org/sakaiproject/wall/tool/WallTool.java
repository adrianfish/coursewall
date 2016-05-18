package org.sakaiproject.wall.tool;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.sakaiproject.wall.api.WallManager;
import org.sakaiproject.wall.api.SakaiProxy;
import org.sakaiproject.component.api.ComponentManager;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.util.RequestFilter;
import org.sakaiproject.util.ResourceLoader;

/**
 * @author Adrian Fish (adrian.r.fish@gmail.com)
 */
@Slf4j
public class WallTool extends HttpServlet {

    private SakaiProxy sakaiProxy;
    private WallManager wallManager;
    
    public void init(ServletConfig config) throws ServletException {

        super.init(config);

        log.debug("init");
        
        try {
            ComponentManager componentManager = org.sakaiproject.component.cover.ComponentManager.getInstance();
            sakaiProxy = (SakaiProxy) componentManager.get(SakaiProxy.class);
            wallManager = (WallManager) componentManager.get(WallManager.class);
        } catch (Throwable t) {
            throw new ServletException("Failed to initialise WallTool servlet.", t);
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        
        log.debug("doGet()");

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
            rl = new ResourceLoader("org.sakaiproject.wall");
            rl.setContextLocale(locale);
        } else {
            rl = new ResourceLoader(userId, "org.sakaiproject.wall");
            locale = rl.getLocale();
        }

        if (locale == null || rl == null) {
            log.error("Failed to load the site or user i18n bundle");
        }

        String language = locale.getLanguage();
        String country = locale.getCountry();

        if (country != null && !country.equals("")) {
            language += "_" + country;
        }

        request.setAttribute("sakaiHtmlHead", (String) request.getAttribute("sakai.html.head"));
        request.setAttribute("isolanguage", language);
        request.setAttribute("userId", userId);
        String siteId = sakaiProxy.getCurrentSiteId();
        request.setAttribute("siteId", siteId);
        boolean isUserSite = sakaiProxy.isUserSite(siteId);
        request.setAttribute("isUserSite", isUserSite);
        request.setAttribute("embedder", isUserSite ? "SOCIAL" : "SITE");
        request.setAttribute("wallId", isUserSite ? "SOCIAL" : siteId);

        response.setContentType("text/html");
        request.getRequestDispatcher("/WEB-INF/bootstrap.jsp").include(request, response);
    }
}
