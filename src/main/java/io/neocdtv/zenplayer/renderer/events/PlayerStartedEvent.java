/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.neocdtv.zenplayer.renderer.events;

import java.io.InputStream;

/**
 *
 * @author xix
 */
public class PlayerStartedEvent {

    private final InputStream inputStream;
    private final InputStream errorStream;

    public PlayerStartedEvent(final InputStream inputStream, final InputStream errorStream) {
        this.inputStream = inputStream;
        this.errorStream = errorStream;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public InputStream getErrorStream() {
        return errorStream;
    }
}
