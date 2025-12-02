package com.hzc.nonocontroller;

import java.util.List;

public interface BlunoLibraryDelegate {
    void onConectionStateChange(BlunoLibrary.connectionStateEnum theConnectionState);
    void onSerialReceived(String theString);
}