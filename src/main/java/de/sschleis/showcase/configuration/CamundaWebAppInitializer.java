package de.sschleis.showcase.configuration;

import org.camunda.bpm.admin.impl.web.bootstrap.AdminContainerBootstrap;
import org.camunda.bpm.admin.impl.web.filter.plugin.AdminClientPluginsFilter;
import org.camunda.bpm.cockpit.impl.web.bootstrap.CockpitContainerBootstrap;
import org.camunda.bpm.cockpit.impl.web.filter.plugin.CockpitClientPluginsFilter;
import org.camunda.bpm.engine.rest.filter.CacheControlFilter;
import org.camunda.bpm.webapp.impl.engine.ProcessEnginesFilter;
import org.camunda.bpm.webapp.impl.security.auth.AuthenticationFilter;
import org.camunda.bpm.webapp.impl.security.filter.SecurityFilter;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.embedded.ServletContextInitializer;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Registriert die benötigten Beobachter, Filter und Servlets um die Camunda Webapplikationen zu nutzen.
 *
 * <p>Im Falle des eingebetteten Servletcontainers wird die bereitgestellte {@code web.xml} Datei der Camunda
 * Webapplkationen nicht berücksichtigt, so dass die benötigten Komponenten programmatisch registriert werden.</p>
 *
 * <p>Es ist darauf zu achten, dass die programmatische Registrierung mit jener aus der {@code web.xml}
 * übereinstimmt.</p>
 *
 * @author Christoph Berg
 */
@Component
public class CamundaWebAppInitializer implements ServletContextInitializer
{
    /**
     * Protokollinstanz dieser Klasse.
     */
    public static final Logger LOGGER = LoggerFactory.getLogger(CamundaWebAppInitializer.class);

    /**
     * Standardeinstellung, auf welcher Basis die Filter arbeiten sollen.
     */
    private static final EnumSet<DispatcherType> DISPATCHER_TYPES = EnumSet.of(DispatcherType.REQUEST);

    /**
     * Servlet Kontext, in dem zusätzliche Filter, Servlets, usw. registriert werden.
     */
    private ServletContext servletContext;

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException
    {
        this.servletContext = servletContext;

        // Beobachter zum Initialisieren der Webanwendung registrieren
        servletContext.addListener(new CockpitContainerBootstrap());
        servletContext.addListener(new AdminContainerBootstrap());

        // Filter registrieren
        registerFilter("Authentication Filter", AuthenticationFilter.class, "/*");

        HashMap<String, String> securityFilterParameters = new HashMap<>();
        securityFilterParameters.put("configFile", "/WEB-INF/securityFilterRules.json");
        registerFilter("Security Filter", SecurityFilter.class, securityFilterParameters, "/*");

        registerFilter("Cockpit Client Plugins Filter", CockpitClientPluginsFilter.class,
                "/app/cockpit/cockpit-bootstrap.js", "/app/cockpit/cockpit.js");

        registerFilter("Admin Client Plugins Filter", AdminClientPluginsFilter.class, "/app/admin/admin-bootstrap.js",
                "/app/admin/admin.js");

        registerFilter("Engines Filter", ProcessEnginesFilter.class, "/app/*");

        registerFilter("CacheControlFilter", CacheControlFilter.class, "/api/*");

        HashMap<String, String> cockpitApiParameters = new HashMap<>();
        cockpitApiParameters.put("javax.ws.rs.Application", "org.camunda.bpm.cockpit.impl.web.CockpitApplication");
        cockpitApiParameters.put("resteasy.servlet.mapping.prefix", "/api/cockpit");
        registerServlet("Cockpit Api", HttpServletDispatcher.class, cockpitApiParameters, "/api/cockpit/*");

        HashMap<String, String> adminApiParameters = new HashMap<>();
        adminApiParameters.put("javax.ws.rs.Application", "org.camunda.bpm.admin.impl.web.AdminApplication");
        adminApiParameters.put("resteasy.servlet.mapping.prefix", "/api/admin");
        registerServlet("Admin Api", HttpServletDispatcher.class, adminApiParameters, "/api/admin/*");

        HashMap<String, String> engineApiParameters = new HashMap<>();
        engineApiParameters.put("javax.ws.rs.Application", "org.camunda.bpm.webapp.impl.engine" +
                ".EngineRestApplication");
        engineApiParameters.put("resteasy.servlet.mapping.prefix", "/api/engine");
        registerServlet("Engine Api", HttpServletDispatcher.class, engineApiParameters, "/api/engine/*");
    }

    /**
     * Registriert einen Filter zur Ausführung, wenn die angegebenen URL Muster aufgerufen werden.
     *
     * <p>Ist der Filter bereits registriert, wird die zugehörige Registrierungsinformation zurückgeliefert.</p>
     *
     * @param filterName  Name, unter welchem der Filter nachgeschlagen werden kann
     * @param filterClass Klasse, die die Logik des Filters enthält
     * @param urlPatterns Muster, auf welche eine aufgerufene URL passen muss, damit der Filter ausgeführt wird
     *
     * @return Registrierungsinformationen des Filters
     */
    private FilterRegistration registerFilter(final String filterName,
                                              final Class<? extends Filter> filterClass,
                                              final String... urlPatterns)
    {
        return registerFilter(filterName, filterClass, null, urlPatterns);
    }

    /**
     * Registriert einen Filter zur Ausführung, wenn die angegebenen URL Muster aufgerufen werden.
     *
     * <p>Ist der Filter bereits registriert, wird die zugehörige Registrierungsinformation zurückgeliefert.</p>
     *
     * @param filterName     Name, unter welchem der Filter nachgeschlagen werden kann
     * @param filterClass    Klasse, die die Logik des Filters enthält
     * @param initParameters Parameter, welche dem Filter zur Initialisierung übergeben werden
     * @param urlPatterns    Muster, auf welche eine aufgerufene URL passen muss, damit der Filter ausgeführt wird
     *
     * @return Registrierungsinformationen des Filters
     */
    private FilterRegistration registerFilter(final String filterName,
                                              final Class<? extends Filter> filterClass,
                                              final Map<String, String> initParameters,
                                              final String... urlPatterns)
    {
        FilterRegistration filterRegistration = servletContext.getFilterRegistration(filterName);

        if (filterRegistration == null)
        {
            filterRegistration = servletContext.addFilter(filterName, filterClass);
            filterRegistration.addMappingForUrlPatterns(DISPATCHER_TYPES, true, urlPatterns);

            if (initParameters != null)
            {
                filterRegistration.setInitParameters(initParameters);
            }

            LOGGER.debug("Filter {} für URL Pfade {} registriert", filterName, urlPatterns);
        }

        return filterRegistration;
    }

    /**
     * Registriert ein Servlet zur Ausführung, wenn die angegebenen URL Muster aufgerufen werden.
     *
     * <p>Ist das Servlet bereits registriert, wird die zugehörige Registrierungsinformation zurückgeliefert.</p>
     *
     * @param servletName    Name, unter welchem das Servlet nachgeschlagen werden kann
     * @param servletClass   Klasse, die die Logik des Servlets enthält
     * @param initParameters Parameter, welche dem Servlet zur Initialisierung übergeben werden
     * @param urlPatterns    Muster, auf welche eine aufgerufene URL passen muss, damit das Servlet ausgeführt wird
     *
     * @return Registrierungsinformationen des Servlets
     */
    private ServletRegistration registerServlet(final String servletName, final Class<? extends Servlet>
            servletClass, final Map<String, String> initParameters, final String... urlPatterns)
    {
        ServletRegistration servletRegistration = servletContext.getServletRegistration(servletName);

        if (servletRegistration == null)
        {
            servletRegistration = servletContext.addServlet(servletName, servletClass);
            servletRegistration.addMapping(urlPatterns);
            servletRegistration.setInitParameters(initParameters);

            LOGGER.debug("Servlet {} für URL Pfade {} registriert.", servletName, urlPatterns);
        }

        return servletRegistration;
    }
}