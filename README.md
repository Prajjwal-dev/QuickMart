# QuickMart 🛒💵

## Overview
**QuickMart** is a JavaFX-based retail management system developed as a BIT 4th semester project. It streamlines store operations with billing, inventory management, and a user-friendly interface. QuickMart helps retail managers efficiently handle sales, track stock, and generate reports in multiple formats.

---

## Key Features

- **Billing & Inventory Management** 💵📦  
  Seamlessly manage sales transactions and stock updates.  

- **Low-Stock Alerts** ⚠️  
  Get notified when product stock levels are low.  

- **Barcode Support** 🔖  
  Generate and scan barcodes via `controller.BarcodeManager`.  

- **User-Friendly UI** 🎨  
  Intuitive interface built with JavaFX components.  

- **Data Export Functionality** 📝📊📑  
  - **CSV Export** using `PrintWriter` helpers  
  - **Excel (XLSX) Export** using **Apache POI**  
  - **PDF Export** using **iText**  

---

## Technical Details

- **Programming Language:** Java  
- **Framework:** JavaFX  
- **Libraries Used:**  
  - **Apache POI** – Excel spreadsheet export (XLSX)  
  - **iText** – PDF export  
  - **ZXing** – Barcode generation
  - **CSV Export** - `PrintWriter` helpers 

- **Repository:** `Prajjwal-dev/QuickMart`  

---

## Project Structure

1. **Billing & Inventory Modules**  
   Handles sales processing, stock updates, and low-stock checks.  

2. **Barcode Management**  
   Supports barcode generation and scanning via `controller.BarcodeManager`.  

3. **Data Export Modules**  
   - CSV exports using `PrintWriter`  
   - XLSX exports using **Apache POI**  
   - PDF exports using **iText**  
   - Helpers for safe filenames (`sanitizeForFilename`, `computeExportSuffixForSales`)  

4. **UI Components & Controls**  
   - `TableView` / `TableColumn` / `TableCell` for structured data display  
   - `ContextMenu` for search and autocomplete suggestions  
   - `Dialogs` / `Stages` for add/update forms and low-stock details  
   - `ScrollPane` wrapper for consistent layout (`createScrollable(...)`)  
   - `ImageView` for UI assets (e.g., warning images)  
   - Dynamic buttons with hover effects for interactivity  

---

## Installation & Usage

1. **Clone the repository:**
```bash
git clone https://github.com/Prajjwal-dev/QuickMart.git
