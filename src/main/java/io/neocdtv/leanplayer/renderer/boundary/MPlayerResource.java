package io.neocdtv.leanplayer.renderer.boundary;

import io.neocdtv.leanplayer.renderer.Constants;
import io.neocdtv.leanplayer.renderer.control.MPlayerEventsHandler;
import io.neocdtv.player.core.mplayer.MPlayer;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;


/**
 * MPlayerResource.
 *
 * @author xix
 * @since 22.12.17
 */

@ApplicationScoped
@Path(Constants.PATH_BASE_CONTROL)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MPlayerResource {

  private final static Logger LOGGER = Logger.getLogger(MPlayerResource.class.getName());
  private static final String QUERY_PARAM_URL = "url";
  private static final String PATH_PLAYER_PLAY = "/play";
  private final static String PATH_PLAYER_STOP = "/stop";
  private final static String PATH_PLAYER_PAUSE = "/pause";
  private MPlayer renderer;
  @Inject
  private MPlayerEventsHandler eventsHandler;

  @PostConstruct
  public void postConstruct() {
    renderer = new MPlayer(eventsHandler);
  }

  @GET
  @Path(PATH_PLAYER_PLAY)
  // TODO: can I add similar description to queryparam, like operationId in method in swagger
  public void play(@QueryParam(QUERY_PARAM_URL) String url) throws MalformedURLException, UnsupportedEncodingException {
    LOGGER.info("Query Param Url: " + url);
    String forPlay = buildUrl(url);
    renderer.play(forPlay);
  }

  // TODO: pretty ugly,
  private String buildUrl(@QueryParam(QUERY_PARAM_URL) String url) throws MalformedURLException, UnsupportedEncodingException {
    final URL asUrl = new URL(url);
    String resource = asUrl.getQuery().replaceFirst("resource=", "");
    String encodeResource = URLEncoder.encode(resource, "UTF-8");
    return String.format("%s://%s:%s%s?resource=%s", asUrl.getProtocol(), asUrl.getHost(), asUrl.getPort(), asUrl.getPath(), encodeResource);
  }

  @GET
  @Path(PATH_PLAYER_STOP)
  public void stop() {
    renderer.stop();
  }

  @GET
  @Path(PATH_PLAYER_PAUSE)
  public void pause() {
    renderer.pause();
  }

  private Map<String, String> getQueryMap(String query)
  {
    String[] params = query.split("&");
    Map<String, String> map = new HashMap<String, String>();
    for (String param : params)
    {
      String name = param.split("=")[0];
      String value = param.split("=")[1];
      map.put(name, value);
    }
    return map;
  }
}
