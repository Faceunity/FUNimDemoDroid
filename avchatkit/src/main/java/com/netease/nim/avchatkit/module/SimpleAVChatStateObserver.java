package com.netease.nim.avchatkit.module;

import com.netease.nimlib.sdk.avchat.AVChatStateObserver;
import com.netease.nimlib.sdk.avchat.model.AVChatAudioFrame;
import com.netease.nimlib.sdk.avchat.model.AVChatNetworkStats;
import com.netease.nimlib.sdk.avchat.model.AVChatSessionStats;
import com.netease.nimlib.sdk.avchat.model.AVChatVideoFrame;
import com.netease.nrtc.sdk.common.VideoFilterParameter;
import com.netease.nrtc.sdk.video.VideoFrame;

import java.util.Map;
import java.util.Set;

/**
 * Created by winnie on 2017/12/8.
 */

public class SimpleAVChatStateObserver implements AVChatStateObserver {
    @Override
    public void onTakeSnapshotResult(String account, boolean success, String file) {

    }

    @Override
    public void onAVRecordingCompletion(String account, String filePath) {

    }

    @Override
    public void onAudioRecordingCompletion(String filePath) {

    }

    @Override
    public void onLowStorageSpaceWarning(long availableSize) {

    }

    @Override
    public void onAudioMixingProgressUpdated(long progressMs, long durationMs) {

    }

    @Override
    public void onAudioMixingEvent(int event) {

    }

    @Override
    public void onAudioEffectPreload(int effectId, int result) {

    }

    @Override
    public void onAudioEffectPlayEvent(int effectId, int event) {

    }

    @Override
    public void onPublishVideoResult(int result) {

    }

    @Override
    public void onUnpublishVideoResult(int result) {

    }

    @Override
    public void onSubscribeVideoResult(String account, int videoType, int result) {

    }

    @Override
    public void onUnsubscribeVideoResult(String account, int videoType, int result) {

    }

    @Override
    public void onRemotePublishVideo(String account, int[] videoTypes) {

    }

    @Override
    public void onRemoteUnpublishVideo(String account) {

    }

    @Override
    public void onUnsubscribeAudioResult(int result) {

    }

    @Override
    public void onSubscribeAudioResult(int result) {

    }

    @Override
    public void onJoinedChannel(int code, String audioFile, String videoFile, int elapsed) {

    }

    @Override
    public void onUserJoined(String account) {

    }

    @Override
    public void onUserLeave(String account, int event) {

    }

    @Override
    public void onLeaveChannel() {

    }

    @Override
    public void onProtocolIncompatible(int status) {

    }

    @Override
    public void onDisconnectServer(int code) {

    }

    @Override
    public void onNetworkQuality(String user, int quality, AVChatNetworkStats stats) {

    }

    @Override
    public void onCallEstablished() {

    }

    @Override
    public void onDeviceEvent(int code, String desc) {

    }

    @Override
    public void onConnectionTypeChanged(int netType) {

    }

    @Override
    public void onFirstVideoFrameAvailable(String account) {

    }

    @Override
    public void onFirstVideoFrameRendered(String user) {

    }

    @Override
    public void onVideoFrameResolutionChanged(String user, int width, int height, int rotate) {

    }

    @Override
    public void onVideoFpsReported(String account, int fps) {

    }

    @Override
    public boolean onVideoFrameFilter(AVChatVideoFrame frame, boolean maybeDualInput) {
        return false;
    }

    @Override
    public boolean onAudioFrameFilter(AVChatAudioFrame frame) {
        return false;
    }

    @Override
    public void onAudioDeviceChanged(int device, Set<Integer> set, boolean shouldSelect) {

    }


    @Override
    public void onReportSpeaker(Map<String, Integer> speakers, int mixedEnergy) {

    }

    @Override
    public void onSessionStats(AVChatSessionStats sessionStats) {

    }

    @Override
    public void onLiveEvent(int event) {

    }

    @Override
    public void onAVRecordingStart(String account, String fileDir) {

    }

    @Override
    public void onAudioRecordingStart(String fileDir) {

    }

    @Override
    public boolean onVideoFrameFilter(VideoFrame input, VideoFrame[] outputFrames, VideoFilterParameter filterParameter) {
        return false;
    }
}
