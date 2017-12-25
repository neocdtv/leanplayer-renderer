/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.neocdtv.zenplayer.renderer.events;

/**
 *
 * @author xix
 */
public class PositionRendererEvent {
    private final int currentPosition;

    public PositionRendererEvent(int currentPosition) {
        this.currentPosition = currentPosition;
    }

    public int getCurrentPosition() {
        return currentPosition;
    }
}
