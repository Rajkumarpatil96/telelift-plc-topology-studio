package com.telelift.plugin;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import com.telelift.plugin.controller.CSVGenerator;
import com.telelift.plugin.controller.DXFParser; // Imported to execute automated CAD parsing
import com.telelift.plugin.model.ElementType;
import com.telelift.plugin.model.TrackElement;
import com.telelift.plugin.model.TrackSegment;
import com.telelift.plugin.view.PropertyPanel;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.List;

public class TeleliftLayoutPlugin extends JFrame {

    // --- DRAG & DROP TRANSFERABLE PAYLOAD PACKET ---
    public static class WidgetTransferable implements Transferable {
        public static final DataFlavor WIDGET_FLAVOR = new DataFlavor(ElementType.class, "TeleliftElement");
        private final ElementType type;
        public WidgetTransferable(ElementType type) { this.type = type; }
        @Override
        public DataFlavor[] getTransferDataFlavors() { return new DataFlavor[]{WIDGET_FLAVOR}; }
        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) { return WIDGET_FLAVOR.equals(flavor); }
        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
            return type;
        }
    }

    // --- ENHANCED GRAPHICS DESIGN CANVAS ---
    private static class DesignCanvas extends JPanel implements DropTargetListener {
        private final List<TrackElement> elements = new ArrayList<>();
        private final List<TrackSegment> tracks = new ArrayList<>();
        private TrackElement selectedElement = null;
        private TrackElement draggingElement = null;
        private final Point dragOffset = new Point();
        private final PropertyPanel propertyPanel;

        public DesignCanvas(PropertyPanel propertyPanel) {
            this.propertyPanel = propertyPanel;
            setBackground(new Color(20, 22, 23));
            setFocusable(true);

            new DropTarget(this, DnDConstants.ACTION_COPY, this, true);

            // Initialize a Demo Layout Setup: 1 Curved Track, 1 Straight Track
            Path2D.Double curve1 = new Path2D.Double();
            curve1.moveTo(100, 250);
            curve1.curveTo(300, 100, 500, 400, 800, 250); // Complex non-straight spline
            tracks.add(new TrackSegment("TRACK_CURVE_MAIN", curve1));

            Path2D.Double straight1 = new Path2D.Double();
            straight1.moveTo(100, 450);
            straight1.lineTo(800, 450); // Straight processing rail line
            tracks.add(new TrackSegment("TRACK_STRAIGHT_FEED", straight1));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    requestFocusInWindow();
                    selectedElement = null;
                    draggingElement = null;
                    
                    for (int i = elements.size() - 1; i >= 0; i--) {
                        TrackElement el = elements.get(i);
                        if (new Rectangle(el.getX() - 25, el.getY() - 25, 50, 50).contains(e.getPoint())) {
                            selectedElement = el;
                            draggingElement = el;
                            dragOffset.x = e.getX() - el.getX();
                            dragOffset.y = e.getY() - el.getY();
                            break;
                        }
                    }
                    propertyPanel.updateProperties(selectedElement);
                    repaint();
                }
                @Override
                public void mouseReleased(MouseEvent e) { 
                    if (draggingElement != null) {
                        snapWidgetToNearestTrack(draggingElement);
                    }
                    draggingElement = null; 
                    repaint();
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (draggingElement != null) {
                        draggingElement.setX(e.getX() - dragOffset.x);
                        draggingElement.setY(e.getY() - dragOffset.y);
                        repaint();
                        propertyPanel.updateProperties(draggingElement);
                    }
                }
            });
        }

        public void addImportedElements(List<TrackElement> importedList) {
            this.elements.addAll(importedList);
            for (TrackElement el : importedList) {
                snapWidgetToNearestTrack(el); // Force direction alignment for imported components
            }
            repaint();
        }

        public void clearCanvas() {
            elements.clear();
            selectedElement = null;
            draggingElement = null;
            propertyPanel.updateProperties(null);
            repaint();
        }

        public void toggleTrackBlockState(int index) {
            if (index >= 0 && index < tracks.size()) {
                TrackSegment seg = tracks.get(index);
                seg.setBlocked(!seg.isBlocked());
                if (selectedElement != null) propertyPanel.updateProperties(selectedElement);
                repaint();
            }
        }

        @Override
        public void dragEnter(DropTargetDragEvent dtde) { dtde.acceptDrag(DnDConstants.ACTION_COPY); }
        @Override
        public void dragOver(DropTargetDragEvent dtde) {}
        @Override
        public void dropActionChanged(DropTargetDragEvent dtde) {}
        @Override
        public void dragExit(DropTargetEvent dte) {}

        @Override
        public void drop(DropTargetDropEvent dtde) {
            try {
                if (dtde.isDataFlavorSupported(WidgetTransferable.WIDGET_FLAVOR)) {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    ElementType type = (ElementType) dtde.getTransferable().getTransferData(WidgetTransferable.WIDGET_FLAVOR);
                    Point dropPoint = dtde.getLocation();
                    
                    TrackElement newElement = new TrackElement(type, dropPoint.x, dropPoint.y, 0);
                    snapWidgetToNearestTrack(newElement);
                    elements.add(newElement);
                    selectedElement = newElement;
                    propertyPanel.updateProperties(selectedElement);
                    
                    dtde.dropComplete(true);
                    repaint();
                } else { dtde.rejectDrop(); }
            } catch (Exception ex) { dtde.rejectDrop(); }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Background grid
            g2.setColor(new Color(32, 35, 37));
            for (int i = 0; i < getWidth(); i += 30) g2.drawLine(i, 0, i, getHeight());
            for (int j = 0; j < getHeight(); j += 30) g2.drawLine(0, j, getWidth(), j);

            // RENDER INDEPENDENT LAYOUT TRACK LINES FIRST
            for (TrackSegment track : tracks) {
                g2.setStroke(new BasicStroke(6.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                if (track.isBlocked()) {
                    g2.setColor(new Color(192, 57, 43, 200)); // Vivid Warning Block Alert Red Line
                } else {
                    g2.setColor(new Color(44, 62, 80));    // Operational Steel Navy Blue Line
                }
                g2.draw(track.getGeometryPath());
            }

            // PAINT SNAPPED ARROWS AND WIDGET OVERLAYS
            for (TrackElement el : elements) {
                g2.translate(el.getX(), el.getY());
                g2.rotate(Math.toRadians(el.getRotation()));

                boolean parentIsBlocked = el.getParentSegment() != null && el.getParentSegment().isBlocked();

                switch (el.getType()) {
                    case SWITCH:
                        g2.setColor(parentIsBlocked ? Color.LIGHT_GRAY : new Color(231, 76, 60));
                        g2.setStroke(new BasicStroke(3.0f));
                        g2.drawLine(-20, 0, 20, 0); g2.drawLine(-5, 0, 15, -15);
                        g2.fillOval(-6, -6, 12, 12);
                        break;
                    case STATION:
                        g2.setColor(parentIsBlocked ? Color.LIGHT_GRAY : new Color(46, 204, 113));
                        g2.setStroke(new BasicStroke(2.0f));
                        g2.drawRoundRect(-22, -16, 44, 32, 6, 6);
                        g2.setStroke(new BasicStroke(3.0f)); g2.drawLine(-22, 6, 22, 6);
                        g2.fillRect(-12, -10, 24, 12);
                        break;
                    case TERMINAL:
                        g2.setColor(new Color(52, 152, 219));
                        g2.setStroke(new BasicStroke(3.0f));
                        g2.drawLine(-15, 0, 15, 0); g2.fillRect(8, -12, 8, 24);
                        break;
                    case ARROW_FWD:
                        g2.setColor(parentIsBlocked ? Color.DARK_GRAY : new Color(241, 196, 15));
                        g2.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g2.drawPolyline(new int[]{-12, 4, -12}, new int[]{-10, 0, 10}, 3);
                        g2.drawPolyline(new int[]{-4, 12, -4}, new int[]{-10, 0, 10}, 3);
                        break;
                    case ARROW_BWD:
                        g2.setColor(parentIsBlocked ? Color.DARK_GRAY : new Color(155, 89, 182));
                        g2.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g2.drawPolyline(new int[]{12, -4, 12}, new int[]{-10, 0, 10}, 3);
                        g2.drawPolyline(new int[]{4, -12, 4}, new int[]{-10, 0, 10}, 3);
                        break;
                }

                if (el == selectedElement) {
                    g2.setColor(Color.CYAN);
                    g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
                    g2.drawOval(-28, -28, 56, 56);
                }

                g2.rotate(-Math.toRadians(el.getRotation()));
                g2.translate(-el.getX(), -el.getY());

                g2.setColor(parentIsBlocked ? Color.RED : new Color(150, 160, 165));
                g2.setFont(new Font("Monospaced", Font.BOLD, 11));
                g2.drawString(el.getProperties().getOrDefault("ID", "Obj"), el.getX() - 25, el.getY() - 32);
            }
        }
        
        public List<TrackElement> getElements() {
            return this.elements;
        }
        
        private void snapWidgetToNearestTrack(TrackElement el) {
            double closestDistance = 35.0; 
            TrackSegment closestSegment = null;
            Point snapPoint = null;
            double trackAngleDegrees = 0.0;

            for (TrackSegment seg : tracks) {
                PathIterator pi = seg.getGeometryPath().getPathIterator(null, 1.0);
                float[] coords = new float[6];
                float lastX = 0, lastY = 0;
                
                while (!pi.isDone()) {
                    int type = pi.currentSegment(coords);
                    if (type == PathIterator.SEG_MOVETO || type == PathIterator.SEG_LINETO) {
                        double dist = Point.distance(el.getX(), el.getY(), coords[0], coords[1]);
                        if (dist < closestDistance) {
                            closestDistance = dist;
                            closestSegment = seg;
                            snapPoint = new Point((int)coords[0], (int)coords[1]);
                            trackAngleDegrees = Math.toDegrees(Math.atan2(coords[1] - lastY, coords[0] - lastX));
                        }
                        lastX = coords[0];
                        lastY = coords[1];
                    }
                    pi.next();
                }
            }

            if (el.getParentSegment() != null) {
                el.getParentSegment().getSnappedWidgets().remove(el);
            }

            if (closestSegment != null && snapPoint != null) {
                el.setX(snapPoint.x);
                el.setY(snapPoint.y);
                el.setParentSegment(closestSegment);
                closestSegment.getSnappedWidgets().add(el);
                
                if (el.getType() == ElementType.ARROW_BWD) {
                    el.setRotation(trackAngleDegrees + 180.0); 
                } else {
                    el.setRotation(trackAngleDegrees); 
                }
            } else {
                el.setParentSegment(null);
                el.setRotation(0.0); 
            }
            propertyPanel.updateProperties(el);
        }
        
        /**
         * AUTOMATION ENGINE: Evaluates traffic loops at a switch junction.
         * Looks for free alternates matching the vehicle's direction profile.
         */
        public String calculateIntelligentRoute(TrackElement switchElement, String trolleyDesiredDirection) {
            if (switchElement.getType() != ElementType.SWITCH) return "ERROR_NOT_A_SWITCH";

            TrackSegment primaryTrack = null;
            List<TrackSegment> alternateOptions = new ArrayList<>();

            // 1. Gather all track lines intersecting or nearby this switch junction
            for (TrackSegment track : tracks) {
                double distanceToTrack = getMinimumDistanceToTrack(switchElement, track);
                if (distanceToTrack < 35.0) { // Intersects junction zone
                    if (primaryTrack == null) {
                        primaryTrack = track; // First discovered is mapped as default path
                    } else {
                        alternateOptions.add(track); // Others are cataloged as bypass paths
                    }
                }
            }

            if (primaryTrack == null) return "NO_CONNECTED_TRACKS";

            // 2. Evaluate status metrics of the default track
            if (!primaryTrack.isBlocked() && primaryTrack.getFlowDirection().equalsIgnoreCase(trolleyDesiredDirection)) {
                switchElement.getProperties().put("Active_Turnout_Route", "DEFAULT_STRAIGHT");
                return "ROUTE_CLEAR: Maintaining Path on " + primaryTrack.getSegmentId();
            }

            // 3. Automation Core Loop: Bypass path tracking lookup
            System.out.println("[TRAFFIC WARNING] Default Track " + primaryTrack.getSegmentId() + " Blocked/Invalid. Evaluating alternates...");
            
            for (TrackSegment alternateTrack : alternateOptions) {
                // Criteria A: Must be free of physical blockages
                if (!alternateTrack.isBlocked()) {
                    // Criteria B: Direction vector verification profile check
                    if (alternateTrack.getFlowDirection().equalsIgnoreCase(trolleyDesiredDirection)) {
                        
                        // Action: Update turnout parameters map state
                        switchElement.getProperties().put("Active_Turnout_Route", "DIVERT_TO_ALTERNATE");
                        switchElement.getProperties().put("Target_Bypass_Track_ID", alternateTrack.getSegmentId());
                        
                        return "ROUTE_CHANGED: Switch flipped! Diverting trolley safely onto " + alternateTrack.getSegmentId();
                    }
                }
            }

            // 4. Interlock Condition: Fail-safe halt if all lines are blocked or running inverted direction flow
            switchElement.getProperties().put("Active_Turnout_Route", "HALT_INTERLOCK");
            return "ROUTE_BLOCKED: No free alternate paths match required direction vector! Halting Trolley.";
        }

        // Helper calculation utility line tracking coordinate point projections
        private double getMinimumDistanceToTrack(TrackElement el, TrackSegment seg) {
            PathIterator pi = seg.getGeometryPath().getPathIterator(null, 1.0);
            float[] coords = new float[6];
            double min = Double.MAX_VALUE;
            while (!pi.isDone()) {
                int type = pi.currentSegment(coords);
                if (type == PathIterator.SEG_MOVETO || type == PathIterator.SEG_LINETO) {
                    double dist = Point.distance(el.getX(), el.getY(), coords[0], coords[1]);
                    if (dist < min) min = dist;
                }
                pi.next();
            }
            return min;
        }
    }

    // --- VISUAL SYMBOL PALETTE ---
    private static class PaletteIconLabel extends JLabel {
        private final ElementType type;
        public PaletteIconLabel(ElementType type) {
            this.type = type;
            setOpaque(true);
            setBackground(new Color(30, 33, 35));
            setBorder(BorderFactory.createLineBorder(new Color(55, 60, 65), 1));
            setMaximumSize(new Dimension(150, 65));
            setPreferredSize(new Dimension(150, 65));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Drag onto a track path line: " + type.name());
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int midX = getWidth() / 2; int midY = getHeight() / 2 - 6;
            g2.translate(midX, midY);
            switch (type) {
                case SWITCH:
                    g2.setColor(new Color(231, 76, 60)); g2.setStroke(new BasicStroke(2.5f));
                    g2.drawLine(-20, 0, 20, 0); g2.drawLine(-5, 0, 15, -12); g2.fillOval(-5, -5, 10, 10);
                    break;
                case STATION:
                    g2.setColor(new Color(46, 204, 113)); g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(-20, -14, 40, 28, 5, 5); g2.setStroke(new BasicStroke(2.5f));
                    g2.drawLine(-20, 5, 20, 5); g2.fillRect(-10, -8, 20, 10);
                    break;
                case TERMINAL:
                    g2.setColor(new Color(52, 152, 219)); g2.setStroke(new BasicStroke(2.5f));
                    g2.drawLine(-15, 0, 10, 0); g2.fillRect(6, -10, 6, 20);
                    break;
                case ARROW_FWD:
                    // FIXED: Removed 'parentIsBlocked' reference (Palette items are independent templates)
                    g2.setColor(new Color(241, 196, 15)); 
                    g2.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawPolyline(new int[]{-12, 4, -12}, new int[]{-10, 0, 10}, 3);
                    g2.drawPolyline(new int[]{-4, 12, -4}, new int[]{-10, 0, 10}, 3);
                    break;

                case ARROW_BWD:
                    // FIXED: Removed 'parentIsBlocked' reference 
                    g2.setColor(new Color(155, 89, 182)); 
                    g2.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawPolyline(new int[]{12, -4, 12}, new int[]{-10, 0, 10}, 3);
                    g2.drawPolyline(new int[]{4, -12, 4}, new int[]{-10, 0, 10}, 3);
                    break;
            }
            g2.translate(-midX, -midY);
            g2.setColor(new Color(140, 150, 155)); g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            String text = type.name().replace("_", " ");
            g2.drawString(text, (getWidth() - g2.getFontMetrics().stringWidth(text)) / 2, getHeight() - 6);
        }
    }

    private final DesignCanvas canvas;

    public TeleliftLayoutPlugin() {
        setTitle("Telelift Layout Architecture Studio (Snapping & Block Logic Active)");
        setSize(1350, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        PropertyPanel propertyPanel = new PropertyPanel();
        canvas = new DesignCanvas(propertyPanel);
        propertyPanel.setOnChange(canvas::repaint); // Connect property-grid updates to repaint engine

        // Sidebar Palette Panel Construction
        JPanel palettePanel = new JPanel();
        palettePanel.setLayout(new BoxLayout(palettePanel, BoxLayout.Y_AXIS));
        palettePanel.setBackground(new Color(25, 27, 28));
        palettePanel.setBorder(new TitledBorder("Symbol Palette"));
        palettePanel.setPreferredSize(new Dimension(180, 0));

        for (ElementType type : ElementType.values()) {
            PaletteIconLabel iconLabel = new PaletteIconLabel(type);
            DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(
                iconLabel, DnDConstants.ACTION_COPY, 
                dge -> dge.startDrag(DragSource.DefaultCopyDrop, new WidgetTransferable(type))
            );
            palettePanel.add(Box.createRigidArea(new Dimension(0, 12)));
            palettePanel.add(iconLabel);
        }

        // Automated Controls Ribbon (Top bar simulation triggers)
        JPanel controlToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        
        // UPGRADE FEATURE: Add Import DXF Blueprint Trigger
        JButton btnImportDXF = new JButton("Import DXF Layout File");
        btnImportDXF.setBackground(new Color(52, 152, 219));
        btnImportDXF.setForeground(Color.WHITE);
        
        btnImportDXF.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select Track Layout DXF Map File");
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    String dxfPath = fileChooser.getSelectedFile().getAbsolutePath();
                    List<TrackElement> parsedNodes = DXFParser.loadDXF(dxfPath);
                    canvas.clearCanvas();
                    canvas.addImportedElements(parsedNodes);
                    JOptionPane.showMessageDialog(this, "Successfully loaded " + parsedNodes.size() + " engineering points from CAD!", 
                            "Import Finished", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "CAD Load Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
        JButton btnBlockCurve = new JButton("Toggle Curve Blockage State");
        JButton btnBlockStraight = new JButton("Toggle Straight Blockage State");

        btnBlockCurve.addActionListener(e -> canvas.toggleTrackBlockState(0));
        btnBlockStraight.addActionListener(e -> canvas.toggleTrackBlockState(1));
        
        controlToolbar.add(btnImportDXF); // Load file button registered
        controlToolbar.add(btnBlockCurve);
        controlToolbar.add(btnBlockStraight);
        
        JButton btnCSVExport = new JButton("Generate Automation CSV");
        btnCSVExport.setBackground(new Color(46, 204, 113));
        btnCSVExport.setForeground(Color.WHITE);

        btnCSVExport.addActionListener(e -> {
            JFileChooser saveChooser = new JFileChooser();
            saveChooser.setDialogTitle("Specify PLC Data Target File Destination");
            if (saveChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    String filePath = saveChooser.getSelectedFile().getAbsolutePath();
                    if (!filePath.toLowerCase().endsWith(".csv")) {
                        filePath += ".csv"; 
                    }
                    
                    CSVGenerator.exportTopologyToCSV(canvas.getElements(), filePath);
                    
                    JOptionPane.showMessageDialog(this, "PLC Configuration Database successfully exported to CSV!", 
                            "Automation Complete", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "CSV Generation Error: " + ex.getMessage(), 
                            "System Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        controlToolbar.add(btnCSVExport);

        // Core Window Framework Additions continue below
        add(controlToolbar, BorderLayout.NORTH);
        add(palettePanel, BorderLayout.WEST);
        add(canvas, BorderLayout.CENTER);
        add(propertyPanel, BorderLayout.EAST);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new TeleliftLayoutPlugin().setVisible(true);
        });
    }
}