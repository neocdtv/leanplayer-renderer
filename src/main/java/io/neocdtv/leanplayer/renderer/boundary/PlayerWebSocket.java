package io.neocdtv.leanplayer.renderer.boundary;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.neocdtv.leanplayer.renderer.Constants;
import io.neocdtv.leanplayer.renderer.control.JacksonObjectMapper;
import io.neocdtv.zenplayer.renderer.events.TrackEndedRendererEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.websocket.CloseReason;
import javax.websocket.EncodeException;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * MPlayerWebSocket.
 *
 * @author xix
 * @since 22.12.17
 */
@ApplicationScoped
@ServerEndpoint(value = "/" + Constants.PATH_EVENTS)
public class PlayerWebSocket {
  
  private static final Logger LOGGER = LoggerFactory.getLogger(PlayerWebSocket.class);
  private static final Set<Session> SESSIONS = Collections.synchronizedSet(new HashSet<Session>());

  @OnOpen
  public void onOpen(final Session session, final EndpointConfig config) {
    LOGGER.info("adding session '{}'", session.getId());
    session.setMaxIdleTimeout(0);
    SESSIONS.add(session);
  }

  @OnClose
  public void onClose(final Session session, CloseReason closeReason) {
    LOGGER.info("removing session '{}', reason '{}'", session.getId());
    SESSIONS.remove(session);
  }
  
  @OnError
  public void onError(Session session, Throwable t) {
    LOGGER.error(t.getMessage(), t);
  }

  public void handleStreamEndedEvent(@Observes TrackEndedRendererEvent endedRendererEvent) throws EncodeException {
    LOGGER.info("handling CDI event '{}' and rethrowing over WebSocket connection", endedRendererEvent.getClass().getSimpleName());
    for (final Session session : SESSIONS) {
      try {
        final String eventAsJson = toJson(endedRendererEvent);
        LOGGER.info(String.format("sending message %s", eventAsJson));
        session.getBasicRemote().sendText(eventAsJson);
      } catch (IOException ex) {
        LOGGER.error(ex.getMessage(), ex);
      }
    }
  }
  
  public String toJson(final Object object) throws JsonProcessingException {
    final ObjectMapper mapper = JacksonObjectMapper.getInstanceWithType();
    final String valueAsString = mapper.writeValueAsString(object);
    return valueAsString;
  }
}
