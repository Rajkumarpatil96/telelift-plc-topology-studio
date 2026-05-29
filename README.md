
# Telelift Low-Code PLC Topology Studio

A low-code engineering tool built on SOLID principles that parses CAD layout drawings, models dynamic Telelift track snapping, and exports automated PLC configuration matrices.
---

## 🚀 Key Features

* **Visual Drag & Drop Canvas:** Seamlessly place track primitives (Switches, Stations, Terminals, Flow Chevrons) directly onto a dynamic schematic grid.
* **Smart Rail Snapping Engine:** Automatically projects floating nodes onto the nearest curved (`Path2D` spline) or straight vector path.
* **Live Interlocking & Traffic Simulation:** Real-time state replication allows engineers to toggle track blockage states to verify alternate bypass routes.
* **Automated CAD Ingestion:** Built-in lightweight `DXFParser` translates raw AutoCAD/MicroStation `INSERT` vector layers into functional model data streams.
* **PLC Matrix Export:** Generates flat-file CSV database layouts containing topology mappings and operational metadata for instant PLC Data Block mapping.

---

## 🛠️ System Architecture (SOLID / MVC)

The project is strictly decoupled following Object-Oriented SOLID design principles to ensure stability and independent maintainability:

* **Model (`com.telelift.plugin.model`):** Pure data layers (`TrackElement`, `TrackSegment`) maintaining spatial properties, directional parameters, and operational attributes.
* **View (`com.telelift.plugin.view`, `com.telelift.plugin`):** High-performance vector drawing components (`DesignCanvas`, `PaletteIconLabel`) utilizing Java 2D geometry engines.
* **Controller (`com.telelift.plugin.controller`):** Execution pipelines handling file reading (`DXFParser`) and data structuring transformations (`CSVGenerator`).

---

## ⚙️ How the Automated Traffic Routing Engine Works

When a vehicle approaches a `SWITCH` block layout, the internal routing controller evaluates the downstream tracks using a 3-tier matrix rule:

1. **Downstream Check:** Scans the `isBlocked` state of the current path.
2. **Direction Alignment:** Validates that the track flow property (`FORWARD`/`BACKWARD`) matches the target profile vector.
3. **Bypass Diverting:** If the primary lane flashes blocked, it scans intersection proximity zones for a clear alternate rail and automatically updates the turnout memory address (`Active_Turnout_Route=DIVERT_TO_ALTERNATE`).

---

## 📋 PLC Export Schema Layout

The generated `topology_export.csv` outputs a flat relational schema layout mapped directly to the automated processing script:

| Column Name | Data Type | Description |
| :--- | :--- | :--- |
| `ELEMENT_ID` | String | Unique system identifier string block tag. |
| `ELEMENT_TYPE` | Enum | Classification primitive (`SWITCH`, `STATION`, etc.). |
| `COORD_X` / `COORD_Y` | Integer | Normalized pixel layout space rendering vectors. |
| `ROTATION` | Double | Tangent-aligned path slope orientation tracking angle. |
| `ATTACHED_TRACK` | String | Relational string foreign key binding to the active rail line. |
| `CUSTOM_ATTRIBUTES` | String | Pipe-delimited configurations (`PLC_Output_Address=Q0.1\|Max_Capacity=3`). |

---

## 🚀 Getting Started

### Prerequisites
* Java Development Kit (JDK) 8 or higher.

### Compilation
Compile all decoupled source package files simultaneously from your root terminal shell window:
```bash
javac com/telelift/plugin/model/*.java com/telelift/plugin/view/*.java com/telelift/plugin/controller/*.java com/telelift/plugin/*.java
