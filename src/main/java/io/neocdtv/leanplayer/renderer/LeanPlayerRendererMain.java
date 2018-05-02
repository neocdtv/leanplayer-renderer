package io.neocdtv.leanplayer.renderer;

import io.neocdtv.upnp.discovery.UpnpDiscoveryResponseLite;
import io.neocdtv.upnp.discovery.UpnpNotifyLite;
import io.neocdtv.upnp.discovery.UpnpHelper;
import io.neocdtv.leanplayer.renderer.boundary.MPlayerWebSocket;
import io.neocdtv.service.UrlBuilder;
import io.swagger.jaxrs.config.BeanConfig;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.jboss.weld.environment.servlet.Listener;
import org.springframework.core.io.ClassPathResource;

import javax.servlet.ServletException;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * LeanPlayerMain.
 *
 * @author xix
 * @since 22.12.17
 */
public class LeanPlayerRendererMain {

  private static final Logger LOGGER = Logger.getLogger(LeanPlayerRendererMain.class.getName());
  public static int NETWORK_PORT;
  private static String UUID = UpnpHelper.buildUuid();

  public static void main(String[] args) throws Exception {

    discoverFreeNetworkPort();
    configureJettyLogLevel();
    Server server = new Server(NETWORK_PORT);

    WebAppContext context = configureWebContext(server);

    configureCdi(context);
    final ResourceConfig jerseyConfig = configureJersey();
    configureSwagger(jerseyConfig);
    configureServlet(context, jerseyConfig);
    configureWebSocket(context);

    server.start();
    printUrls();
    UpnpNotifyLite.startIt(UUID, getBaseUrl());
    UpnpDiscoveryResponseLite.startIt(UUID, getHost());
    // TODO: what about device discovery on multiple interfaces
    // TODO: add a resource, which returns json schema of all subclasses of io.neocdtv.zenplayer.renderer.events.Event
    // TODO: add health check service - what for?
    // TODO: add service to get PlayerState - in case a player starts and wants for know the current status
    server.join();
  }

  private static void discoverFreeNetworkPort() throws IOException {
    final ServerSocket socket = new ServerSocket(0);
    socket.setReuseAddress(true);
    NETWORK_PORT = socket.getLocalPort();
    socket.close();
  }

  private static void configureJettyLogLevel() {
    System.setProperty(Constants.JETTY_LOG_LEVEL, Level.INFO.getName());
    System.setProperty(Constants.JETTY_LOGGER, Constants.JETTY_LOGGER_CLASS);
  }

  private static WebAppContext configureWebContext(Server server) throws IOException {
    WebAppContext context = new WebAppContext();
    context.setContextPath(Constants.CONTEXT_PATH);

    context.setResourceBase(new ClassPathResource(Constants.PATH_STATIC).getURI().toString());
    context.setClassLoader(Thread.currentThread().getContextClassLoader());

    server.setHandler(context);
    return context;
  }

  private static ResourceConfig configureJersey() {
    ResourceConfig config = new ResourceConfig();
    config.packages(Constants.RESOURCE_PACKAGE);
    config.register(LoggingFilter.class);
    return config;
  }

  private static void configureSwagger(ResourceConfig config) {

    config.register(io.swagger.jaxrs.listing.ApiListingResource.class);
    config.register(io.swagger.jaxrs.listing.SwaggerSerializers.class);

    BeanConfig beanConfig = new BeanConfig();
    beanConfig.setVersion(Constants.APP_VERSION);
    beanConfig.setSchemes(new String[]{Constants.NETWORK_PROTOCOL_HTTP});
    beanConfig.setHost(getHost());
    beanConfig.setBasePath(getBasePath());
    beanConfig.setResourcePackage(Constants.RESOURCE_PACKAGE);
    beanConfig.setScan(true);

  }

  private static void configureServlet(WebAppContext context, ResourceConfig jerseyConfig) {
    ServletHolder servlet = new ServletHolder(new ServletContainer(jerseyConfig));
    context.addServlet(servlet, getResourcePath());
  }

  private static void configureCdi(WebAppContext context) {
    Listener listener = new Listener();
    context.addEventListener(listener);
    // 18.3.2.2. Binding BeanManager to JNDI, is JDNI by default enabled on jetty?
    //context.addEventListener(new BeanManagerResourceBindingListener());
  }

  private static void configureWebSocket(WebAppContext context) throws ServletException, DeploymentException {
    ServerContainer webSocketContainer = WebSocketServerContainerInitializer.configureContext(context);
    webSocketContainer.addEndpoint(MPlayerWebSocket.class);
  }

  // TODO: refactor this somehow, it's ugly

  private static String getBasePath() {
    return String.format("/%s", Constants.PATH_BASE_REST);
  }

  private static String getHost() {
    return String.format("%s:%s", UrlBuilder.getIpV4Address(), NETWORK_PORT);
  }

  private static String getResourcePath() {
    return String.format("/%s/*", Constants.PATH_BASE_REST);
  }

  private static void printUrls() {
    final String applicationUrlInfo = String.format("Web application available at %s://%s",
        Constants.NETWORK_PROTOCOL_HTTP,
        getBaseUrl());
    final String swaggerUrlInfo = String.format("Swagger available at %s://%s/%s/swagger.json",
        Constants.NETWORK_PROTOCOL_HTTP,
        getBaseUrl(),
        Constants.PATH_BASE_REST);
    final String websocketUrlInfo = String.format("WebSocket connection available at - ws://%s%s%s",
        getHost(),
        Constants.CONTEXT_PATH,
        Constants.PATH_EVENTS);
    LOGGER.info("\n" + applicationUrlInfo + "\n" + swaggerUrlInfo + "\n" + websocketUrlInfo);
  }

  private static String getBaseUrl() {
    if (Constants.CONTEXT_PATH.equals("/")) {
      return getHost();
    } else {
      return getHost() + Constants.CONTEXT_PATH;
    }
  }
}