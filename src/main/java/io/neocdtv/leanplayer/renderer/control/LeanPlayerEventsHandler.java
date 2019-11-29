package io.neocdtv.leanplayer.renderer.control;

import io.neocdtv.player.core.PlayerEventsHandler;
import io.neocdtv.zenplayer.renderer.events.TrackEndedRendererEvent;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.util.logging.Logger;

/**
 * MPlayerEventsHandler.
 *
 * @author xix
 * @since 22.12.17
 */
public class LeanPlayerEventsHandler implements PlayerEventsHandler {

  private final static Logger LOGGER = Logger.getLogger(LeanPlayerEventsHandler.class.getName());

  @Inject
  private Event<TrackEndedRendererEvent> streamEndedEvent;

  @Override
  public void onTrackEnded() {
    LOGGER.info("throwing event...");
    streamEndedEvent.fire(new TrackEndedRendererEvent());
  }

  @Override
  public void onStaringPlayback() {
    LOGGER.info("doing nothing...");
  }
}
