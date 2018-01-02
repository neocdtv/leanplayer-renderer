/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.neocdtv.leanplayer.renderer;

import io.neocdtv.leanplayer.renderer.boundary.MPlayerWebSocket;
import io.swagger.jaxrs.config.BeanConfig;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.jboss.weld.environment.servlet.Listener;
import org.springframework.core.io.ClassPathResource;

import javax.servlet.ServletException;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import java.io.IOException;
import java.net.ServerSocket;
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
		// TODO: send notify
		// TODO: wait for discovery
		server.join();
	}

	private static void discoverFreeNetworkPort() throws IOException {
		final ServerSocket socket = new ServerSocket(0);
    socket.setReuseAddress(true);
    NETWORK_PORT = socket.getLocalPort();
    socket.close();
	}

	private static void configureJettyLogLevel() {
		System.setProperty(Constants.JETTY_LOG_LEVEL, "INFO");
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

	private static String getBasePath() {
		return String.format("/%s", Constants.PATH_BASE_REST);
	}

	private static String getHost() {
		return String.format("%s:%s", Constants.NETWORK_HOST, NETWORK_PORT);
	}

	private static String getResourcePath() {
		return String.format("/%s/*", Constants.PATH_BASE_REST);
	}

	private static void printUrls() {
		final String applicationUrlInfo = String.format("Web application available at %s://%s%s",
				Constants.NETWORK_PROTOCOL_HTTP,
				getHost(),
				Constants.CONTEXT_PATH);
		final String swaggerUrlInfo = String.format("Swagger available at %s://%s%s%s/swagger.json",
				Constants.NETWORK_PROTOCOL_HTTP,
				getHost(),
				Constants.CONTEXT_PATH,
				Constants.PATH_BASE_REST);
		final String websocketUrlInfo = String.format("WebSocket connection available at - ws://%s%s%s",
				getHost(),
				Constants.CONTEXT_PATH,
				Constants.PATH_EVENTS);
		LOGGER.info("\n" + applicationUrlInfo + "\n" + swaggerUrlInfo + "\n" + websocketUrlInfo);
	}
}