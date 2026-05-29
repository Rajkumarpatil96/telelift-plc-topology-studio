package com.telelift.plugin.controller;

import com.telelift.plugin.model.TrackElement;
import com.telelift.plugin.model.TrackSegment;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class CSVGenerator {

    public static void exportTopologyToCSV(List<TrackElement> elements, String destinationPath) throws IOException {
        try (FileWriter writer = new FileWriter(destinationPath)) {
            // Write standard schema headers for the automation data blocks
            writer.append("ELEMENT_ID,ELEMENT_TYPE,COORD_X,COORD_Y,ROTATION,ATTACHED_TRACK,SYSTEM_STATUS,CUSTOM_ATTRIBUTES\n");

            for (TrackElement el : elements) {
                String trackId = (el.getParentSegment() != null) ? el.getParentSegment().getSegmentId() : "UNATTACHED";
                String status = el.getProperties().getOrDefault("System_Status", "OPERATIONAL");
                
                // Compress all dynamic sheet properties into a clean pipe-delimited sub-column
                StringBuilder attributesString = new StringBuilder();
                for (Map.Entry<String, String> entry : el.getProperties().entrySet()) {
                    if (!entry.getKey().equals("ID") && !entry.getKey().equals("System_Status")) {
                        attributesString.append(entry.getKey()).append("=").append(entry.getValue()).append("|");
                    }
                }
                if (attributesString.length() > 0) {
                    attributesString.setLength(attributesString.length() - 1); // Drop trailing delimiter
                }

                // Write the normalized relational data row
                writer.append(el.getProperties().getOrDefault("ID", "UNKNOWN"))
                      .append(",")
                      .append(el.getType().name())
                      .append(",")
                      .append(String.valueOf(el.getX()))
                      .append(",")
                      .append(String.valueOf(el.getY()))
                      .append(",")
                      .append(String.format("%.2f", el.getRotation()))
                      .append(",")
                      .append(trackId)
                      .append(",")
                      .append(status)
                      .append(",")
                      .append("\"").append(attributesString.toString()).append("\"\n");
            }
            writer.flush();
        }
    }
}