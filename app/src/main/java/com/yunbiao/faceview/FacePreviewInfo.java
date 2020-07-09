package com.yunbiao.faceview;


import com.arcsoft.face.FaceInfo;

public class FacePreviewInfo {
    private FaceInfo faceInfo;
    private int trackId;
    private float temper;
    private float oringinTemper;
    private int mask;
    private int faceShelter;
    private int liveness;

    public int getLiveness() {
        return liveness;
    }

    public void setLiveness(int liveness) {
        this.liveness = liveness;
    }

    public int getMask() {
        return mask;
    }

    public void setMask(int mask) {
        this.mask = mask;
    }

    public int getFaceShelter() {
        return faceShelter;
    }

    public void setFaceShelter(int faceShelter) {
        this.faceShelter = faceShelter;
    }
    public float getOringinTemper() {
        return oringinTemper;
    }

    public void setOringinTemper(float oringinTemper) {
        this.oringinTemper = oringinTemper;
    }

    public float getTemper() {
        return temper;
    }

    public void setTemper(float temper) {
        this.temper = temper;
    }

    public FacePreviewInfo(FaceInfo faceInfo, int trackId) {
        this.faceInfo = faceInfo;
        this.trackId = trackId;
    }

    public FacePreviewInfo(FaceInfo faceInfo, int faceShelter, int mask, int trackId) {
        this.faceInfo = faceInfo;
        this.faceShelter = faceShelter;
        this.mask = mask;
        this.trackId = trackId;
    }

    public FacePreviewInfo(FaceInfo faceInfo, int trackId, float temper, float oringinTemper, int mask, int faceShelter) {
        this.faceInfo = faceInfo;
        this.trackId = trackId;
        this.temper = temper;
        this.oringinTemper = oringinTemper;
        this.mask = mask;
        this.faceShelter = faceShelter;
    }

    public FaceInfo getFaceInfo() {
        return faceInfo;
    }

    public void setFaceInfo(FaceInfo faceInfo) {
        this.faceInfo = faceInfo;
    }


    public int getTrackId() {
        return trackId;
    }

    public void setTrackId(int trackId) {
        this.trackId = trackId;
    }

}
