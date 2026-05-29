package com.telelift.plugin.model;

import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

public class TrackSegment {
    private final String segmentId;
    private final Path2D geometryPath;
    private boolean isBlocked = false;
    private String flowDirection = "FORWARD"; // Options: FORWARD, BACKWARD, BI_DIRECTIONAL
    private final List<TrackElement> snappedWidgets = new ArrayList<>();

    public TrackSegment(String segmentId, Path2D geometryPath) {
        this.segmentId = segmentId;
        this.geometryPath = geometryPath;
    }

    public void setBlocked(boolean blocked) {
        this.isBlocked = blocked;
        for (TrackElement el : snappedWidgets) {
            el.getProperties().put("Is_Blocked", String.valueOf(blocked));
            el.getProperties().put("System_Status", blocked ? "BLOCKED_BY_TRACK" : "OPERATIONAL");
        }
    }

    // Getters and Setters for automation rules
    public String getSegmentId() { return segmentId; }
    public Path2D getGeometryPath() { return geometryPath; }
    public boolean isBlocked() { return isBlocked; }
    public String getFlowDirection() { return flowDirection; }
    public void setFlowDirection(String dir) { this.flowDirection = dir; }
    public List<TrackElement> getSnappedWidgets() { return snappedWidgets; }
}