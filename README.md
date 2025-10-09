# QuickMart 🛒💵

## Overview
**QuickMart** is a JavaFX-based retail management system developed as a BIT 4th semester project. The system streamlines store operations by combining billing, inventory management, and a user-friendly interface. QuickMart helps retail managers and store owners efficiently handle sales, track stock, and generate reports in multiple formats.

---

## Key Features

- **Billing & Inventory Management** 💵📦  
  Seamlessly manage sales transactions and inventory updates.  

- **Low-Stock Alerts** ⚠️  
  Get notified when product stock levels are low.  

- **Barcode Support** 🔖  
  Generate and scan barcodes via `controller.BarcodeManager` for faster product handling.  

- **User-Friendly UI** 🎨  
  Intuitive interface built with JavaFX components for easy navigation and data entry.  

- **Export Functionality** 📝📊📑  
  Export sales and inventory data in CSV, XLSX, and PDF formats for reporting and analysis.  

---

## Technical Details

- **Programming Language:** Java  
- **Framework:** JavaFX  
- **Libraries Used:**  
  - **Apache POI** – Excel (XLSX) export  
  - **iText** – PDF export  
  - **ZXing** – Barcode generation  

- **Repository:** `Prajjwal-dev/QuickMart`  
- **Repository ID:** _(Add ID if applicable)_

---

## Project Structure

1. **Billing & Inventory Modules**  
   Handles sales processing, stock updates, and low-stock checks.  

2. **Barcode Management**  
   Supports barcode generation and scanning via `controller.BarcodeManager`.  

3. **Data Export Modules**  
   Implements CSV, XLSX, and PDF exports using helper functions (`sanitizeForFilename`, `computeExportSuffixForSales`).  

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
