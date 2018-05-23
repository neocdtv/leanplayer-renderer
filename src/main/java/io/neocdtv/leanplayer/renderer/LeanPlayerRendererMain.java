package io.neocdtv.leanplayer.renderer;

import io.neocdtv.commons.network.NetworkUtil;
import io.neocdtv.leanplayer.renderer.boundary.MPlayerWebSocket;
import io.neocdtv.service.UrlBuilder;
import io.neocdtv.upnp.discovery.UpnpHelper;
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

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import javax.servlet.ServletException;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.neocdtv.leanplayer.renderer.Constants.PATH_API_REST;

/**
 * LeanPlayerMain.
 *
 * @author xix
 * @since 22.12.17
 */
public class LeanPlayerRendererMain {

  private static final Logger LOGGER = Logger.getLogger(LeanPlayerRendererMain.class.getName());
  private static final String SERVICE_TYPE = "_leanplayer._tcp.local.";
  private final String uuid = UpnpHelper.buildUuid();
  private final NetworkUtil networkUtil = new NetworkUtil();

  private int networkPort;
  private InetAddress inetAddress;
  private JmDNS jmdns;

  public static void main(String[] args) throws Exception {

    final LeanPlayerRendererMain leanPlayerRendererMain = new LeanPlayerRendererMain();

    leanPlayerRendererMain.discoverInetAddress();
    leanPlayerRendererMain.networkPort = leanPlayerRendererMain.discoverFreeNetworkPort();
    leanPlayerRendererMain.configureJettyLogLevel();

    Server server = new Server(new InetSocketAddress(leanPlayerRendererMain.inetAddress, leanPlayerRendererMain.networkPort));

    WebAppContext context = leanPlayerRendererMain.configureWebContext(server);

    leanPlayerRendererMain.configureCdi(context);
    final ResourceConfig jerseyConfig = leanPlayerRendererMain.configureJersey();
    leanPlayerRendererMain.configureSwagger(jerseyConfig);
    leanPlayerRendererMain.configureServlet(context, jerseyConfig);
    leanPlayerRendererMain.configureWebSocket(context);

    Runtime.getRuntime().addShutdownHook(leanPlayerRendererMain.cleanupThread);

    server.start();
    leanPlayerRendererMain.printUrls();
    leanPlayerRendererMain.startDiscovery();
    server.join();
  }

  private void discoverInetAddress() throws SocketException {
    final List<InetAddress> ipv4AddressForActiveInterfaces = networkUtil.findIpv4AddressForActiveInterfaces();
    if (ipv4AddressForActiveInterfaces.size() == 1) {
      inetAddress = ipv4AddressForActiveInterfaces.get(0);
      return;
    }

    if (ipv4AddressForActiveInterfaces.size() == 2) {
      for (InetAddress ipv4AddressForActiveInterface : ipv4AddressForActiveInterfaces) {
        if (!ipv4AddressForActiveInterface.isLinkLocalAddress()) {
          inetAddress = ipv4AddressForActiveInterface;
          return;
        }
      }
    }
  }

  private void startDiscovery() throws IOException {
    jmdns = JmDNS.create(inetAddress);
    jmdns.registerService(createServiceInfo(getBaseUrl(inetAddress)));
  }

  private ServiceInfo createServiceInfo(final String baseUrl) throws IOException {
    final Map<String, String> props = new HashMap<>();
    props.put("events-location", "ws://" + baseUrl + "/events");
    props.put("control-location", "http://" + baseUrl + "/" + PATH_API_REST + "/control");
    props.put("uuid", uuid);
    props.put("name", "Leanplayer Renderer");
    final ServiceInfo serviceInfo = ServiceInfo.create(SERVICE_TYPE, "Leanplayer Renderer", discoverFreeNetworkPort(), "Renderer");
    serviceInfo.setText(props);
    return serviceInfo;
  }

  private int discoverFreeNetworkPort() throws IOException {
    final ServerSocket socket = new ServerSocket(0);
    socket.setReuseAddress(true);
    final int networkPortTmp = socket.getLocalPort();
    socket.close();
    return networkPortTmp;
  }

  private void configureJettyLogLevel() {
    System.setProperty(Constants.JETTY_LOG_LEVEL, Level.INFO.getName());
    System.setProperty(Constants.JETTY_LOGGER, Constants.JETTY_LOGGER_CLASS);
  }

  private WebAppContext configureWebContext(Server server) throws IOException {
    WebAppContext context = new WebAppContext();
    context.setContextPath(Constants.CONTEXT_PATH);

    context.setResourceBase(new ClassPathResource(Constants.PATH_STATIC).getURI().toString());
    context.setClassLoader(Thread.currentThread().getContextClassLoader());

    server.setHandler(context);
    return context;
  }

  private ResourceConfig configureJersey() {
    ResourceConfig config = new ResourceConfig();
    config.packages(Constants.RESOURCE_PACKAGE);
    config.register(LoggingFilter.class);
    return config;
  }

  private void configureSwagger(ResourceConfig config) {

    config.register(io.swagger.jaxrs.listing.ApiListingResource.class);
    config.register(io.swagger.jaxrs.listing.SwaggerSerializers.class);

    BeanConfig beanConfig = new BeanConfig();
    beanConfig.setVersion(Constants.APP_VERSION);
    beanConfig.setSchemes(new String[]{Constants.NETWORK_PROTOCOL_HTTP});
    beanConfig.setHost(getHost(inetAddress));
    beanConfig.setBasePath(getBasePath());
    beanConfig.setResourcePackage(Constants.RESOURCE_PACKAGE);
    beanConfig.setScan(true);

  }

  private void configureServlet(WebAppContext context, ResourceConfig jerseyConfig) {
    ServletHolder servlet = new ServletHolder(new ServletContainer(jerseyConfig));
    context.addServlet(servlet, getResourcePath());
  }

  private void configureCdi(WebAppContext context) {
    Listener listener = new Listener();
    context.addEventListener(listener);
    // 18.3.2.2. Binding BeanManager to JNDI, is JDNI by default enabled on jetty?
    //context.addEventListener(new BeanManagerResourceBindingListener());
  }

  private void configureWebSocket(WebAppContext context) throws ServletException, DeploymentException {
    ServerContainer webSocketContainer = WebSocketServerContainerInitializer.configureContext(context);
    webSocketContainer.addEndpoint(MPlayerWebSocket.class);
  }

  // TODO: refactor this somehow, it's ugly

  private String getBasePath() {
    return String.format("/%s", PATH_API_REST);
  }

  private String getHost(InetAddress inetAddress) {
    return String.format("%s:%s", inetAddress.getHostAddress(), networkPort);
  }

  private String getResourcePath() {
    return String.format("/%s/*", PATH_API_REST);
  }

  private void printUrls() {
    final String applicationUrlInfo = String.format("Web application available at %s://%s",
        Constants.NETWORK_PROTOCOL_HTTP,
        getBaseUrl(inetAddress));
    final String swaggerUrlInfo = String.format("Swagger available at %s://%s/%s/swagger.json",
        Constants.NETWORK_PROTOCOL_HTTP,
        getBaseUrl(inetAddress),
        PATH_API_REST);
    final String websocketUrlInfo = String.format("WebSocket connection available at - ws://%s%s%s",
        getHost(inetAddress),
        Constants.CONTEXT_PATH,
        Constants.PATH_EVENTS);
    LOGGER.info("\n" + applicationUrlInfo + "\n" + swaggerUrlInfo + "\n" + websocketUrlInfo);
  }

  private String getBaseUrl(final InetAddress inetAddress) {
    if (Constants.CONTEXT_PATH.equals("/")) {
      return getHost(inetAddress);
    } else {
      return getHost(inetAddress) + Constants.CONTEXT_PATH;
    }
  }

  private Thread cleanupThread = new Thread(this::cleanup);

  private void cleanup() {
    LOGGER.log(Level.INFO, "clean up");
    jmdns.unregisterAllServices();
  }
}