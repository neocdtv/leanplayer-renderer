package io.neocdtv.leanplayer.renderer.boundary;

import io.neocdtv.leanplayer.renderer.Constants;
import io.neocdtv.leanplayer.renderer.control.MPlayerEventsHandler;
import io.neocdtv.player.core.mplayer.MPlayer;
import io.swagger.annotations.Api;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;


/**
 * MPlayerResource.
 *
 * @author xix
 * @since 22.12.17
 */
@Api
@ApplicationScoped
@Path(Constants.PATH_BASE_CONTROL)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MPlayerResource {

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
  public void play(@QueryParam(QUERY_PARAM_URL) String url) {
    renderer.play(url);
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
}
