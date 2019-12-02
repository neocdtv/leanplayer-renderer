package io.neocdtv.leanplayer.renderer.boundary;

import io.neocdtv.leanplayer.renderer.Constants;
import io.neocdtv.leanplayer.renderer.control.LeanPlayerEventsHandler;
import io.neocdtv.player.core.Player;
import io.neocdtv.player.core.mplayer.Amixer;
import io.neocdtv.player.core.mplayer.MPlayer;
import io.neocdtv.player.core.omxplayer.OmxPlayer;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
public class LeanPlayerResource {

  private final static Logger LOGGER = Logger.getLogger(LeanPlayerResource.class.getName());
  private static final String QUERY_PARAM_URL = "url";
  private static final String PATH_PLAYER_PLAY = "/play";
  private final static String PATH_PLAYER_STOP = "/stop";
  private final static String PATH_PLAYER_PAUSE = "/pause";

  private Player player;

  @Inject
  private LeanPlayerEventsHandler eventsHandler;

  @PostConstruct
  public void postConstruct() {
    String playerType = System.getProperty("player");
    List<String> additionalParams = convertParams(System.getProperty("params"));
    if (playerType == null || playerType.equals("mplayer")) {
      player = new MPlayer();
      player.setAmixer(new Amixer("mplayerChannel"));
    } else if (playerType.equals("omxplayer")) {
      player = new OmxPlayer();
    } else {
      throw new RuntimeException("cant determine player");
    }
    player.setPlayerEventHandler(eventsHandler);
    player.setAdditionalParameters(additionalParams);
  }

  List<String> convertParams(final String paramsAsString) {
    ArrayList<String> params = new ArrayList<>();
    if (paramsAsString != null) {
      params.addAll(Arrays.asList(paramsAsString.trim().split("\\s+")));
    }
    return params;
  }

  @GET
  @Path(PATH_PLAYER_PLAY)
  public void play(@QueryParam(QUERY_PARAM_URL) String url) throws MalformedURLException, UnsupportedEncodingException, InterruptedException {
    LOGGER.info("Query Param Url: " + url);
    String forPlay = buildUrl(url);
    player.play(forPlay);
  }

  // TODO: pretty ugly, do something about it
  private String buildUrl(final String url) throws MalformedURLException, UnsupportedEncodingException {
    if (url.contains("resource")) {
      final URL asUrl = new URL(url);
      String resource = asUrl.getQuery().replaceFirst("resource=", "");
      String encodeResource = URLEncoder.encode(resource, "UTF-8");
      return String.format("%s://%s:%s%s?resource=%s", asUrl.getProtocol(), asUrl.getHost(), asUrl.getPort(), asUrl.getPath(), encodeResource);
    } else {
      return url;
    }
  }

  @GET
  @Path(PATH_PLAYER_STOP)
  public void stop() {
    player.stop();
  }

  @GET
  @Path(PATH_PLAYER_PAUSE)
  public void pause() {
    player.pause();
  }
}
