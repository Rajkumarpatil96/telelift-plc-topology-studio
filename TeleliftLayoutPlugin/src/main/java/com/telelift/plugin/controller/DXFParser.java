package com.telelift.plugin.controller;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.telelift.plugin.model.ElementType;
import com.telelift.plugin.model.TrackElement;

public class DXFParser {
    public static List<TrackElement> loadDXF(String filePath) throws IOException {
        List<TrackElement> elements = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().equals("INSERT")) {
                    String blockName = "";
                    double x = 0, y = 0, rotation = 0;
                    while ((line = br.readLine()) != null) {
                        String code = line.trim();
                        if (code.equals("0")) break;
                        String value = br.readLine().trim();
                        switch (code) {
                            case "2": blockName = value.toUpperCase(); break;
                            case "10": x = Double.parseDouble(value); break;
                            case "20": y = Double.parseDouble(value); break;
                            case "50": rotation = Double.parseDouble(value); break;
                        }
                    }
                    ElementType type = null;
                    if (blockName.contains("SWITCH")) type = ElementType.SWITCH;
                    else if (blockName.contains("STATION")) type = ElementType.STATION;
                    else if (blockName.contains("TERMINAL")) type = ElementType.TERMINAL;
                    else if (blockName.contains("ARROW_FWD") || blockName.contains("FWD")) type = ElementType.ARROW_FWD;
                    else if (blockName.contains("ARROW_BWD") || blockName.contains("BWD")) type = ElementType.ARROW_BWD;

                    if (type != null) {
                        int canvasX = 150 + (int) (x * 0.4);
                        int canvasY = 450 - (int) (y * 0.4);
                        
                        TrackElement newElement = new TrackElement(type, canvasX, canvasY, rotation);

                        // =====================================================================
                        // 🔴 AUTOMATION ENHANCEMENT SITS HERE: Extracting Meta Attributes
                        // =====================================================================
                        // Use unique data parsed from the DXF block name to fill component profiles
                        newElement.getProperties().put("CAD_Source_Block", blockName);
                        
                        if (type == ElementType.STATION) {
                            newElement.getProperties().put("Station_Number", "STN-" + (elements.size() + 1));
                            newElement.getProperties().put("Max_Capacity", "3");
                            newElement.getProperties().put("Speed_Limit_mps", "1.2"); // Default Telelift speed zone
                        } else if (type == ElementType.SWITCH) {
                            newElement.getProperties().put("PLC_Output_Address", "Q0." + elements.size());
                            newElement.getProperties().put("Default_Position", "Straight");
                            newElement.getProperties().put("Switch_Delay_ms", "500");
                        } else if (type == ElementType.ARROW_FWD || type == ElementType.ARROW_BWD) {
                            newElement.getProperties().put("Speed_Limit_mps", "1.5");
                            newElement.getProperties().put("Flow_Direction", type == ElementType.ARROW_FWD ? "FORWARD" : "BACKWARD");
                        }
                        // =====================================================================

                        elements.add(newElement);
                    }
                }
            }
        }
        return elements;
    }
}