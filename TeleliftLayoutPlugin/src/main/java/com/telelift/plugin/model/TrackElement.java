package com.telelift.plugin.model;

import java.util.HashMap;
import java.util.Map;

public class TrackElement {
    private final ElementType type;
    private int x, y;
    private double rotation; 
    private final Map<String, String> properties = new HashMap<>();

    public TrackElement(ElementType type, int x, int y, double rotation) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.rotation = rotation;
        initializeDefaultProperties();
    }

    private void initializeDefaultProperties() {
        properties.put("ID", type.name() + "_" + (System.nanoTime() % 10000));
        switch (type) {
            case SWITCH:
                properties.put("PLC_Output_Address", "Q0.0");
                properties.put("Default_Position", "Straight");
                break;
            case STATION:
                properties.put("Station_Number", "1");
                properties.put("Max_Capacity", "3");
                break;
            case TERMINAL:
                properties.put("Terminal_Zone", "Main_Hub");
                break;
            case ARROW_FWD:
            case ARROW_BWD:
                properties.put("Speed_Limit_mps", "1.2");
                properties.put("Track_Segment_ID", "SEG_01");
                break;
        }
    }
    
 // Inside TrackElement.java, add these fields and updates:
    private TrackSegment parentSegment = null;

    public void setParentSegment(TrackSegment segment) {
        this.parentSegment = segment;
        if (segment != null) {
            this.properties.put("Attached_Track_ID", segment.getSegmentId());
            this.properties.put("Is_Blocked", String.valueOf(segment.isBlocked()));
            this.properties.put("System_Status", segment.isBlocked() ? "BLOCKED_BY_TRACK" : "OPERATIONAL");
        } else {
            this.properties.remove("Attached_Track_ID");
            this.properties.put("System_Status", "UNATTACHED");
        }
    }

    public TrackSegment getParentSegment() { return parentSegment; }

    // Getters and Setters (Encapsulation)
    public ElementType getType() { return type; }
    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public double getRotation() { return rotation; }
    public void setRotation(double rotation) { this.rotation = rotation; }
    public Map<String, String> getProperties() { return properties; }
}
