# 🛒 QuickMart

**QuickMart** is a JavaFX-based retail management system developed as a BIT 4th semester project. It provides smooth store management with billing, inventory handling, and user-friendly UI features.  

---

## 📦 Features & Operations

- **Billing & Inventory Management** 💵📦  
- **Low-stock alerts** ⚠️  
- **Barcode support** via `controller.BarcodeManager` 🔖  
- **User-friendly UI** with JavaFX components 🎨  

---

## 📄 Export & Libraries

QuickMart supports exporting sales and inventory data in multiple formats:  

- **CSV export** using `PrintWriter` helpers 📝  
- **XLSX export** using **Apache POI** 📊  
- **PDF export** using **iText** 📑  
- **Filename helpers** for safe exports (`sanitizeForFilename`, `computeExportSuffixForSales`) 🗂️  

---

## 🖥️ UI Components & Controls

- **TableView / TableColumn / TableCell** for grids and actions 📋  
- **ContextMenu** for autocomplete/search suggestions 🔍  
- **Dialogs / Stages** for add/update forms and low-stock details 🪟  
- **ScrollPane wrapper** `createScrollable(...)` to ensure consistent layout ⬇️  
- **ImageView** for assets (e.g., low-stock warning image) 🖼️  
- **Dynamic Buttons** with inline styles & hover effects 🎯  

---

## 🔧 Libraries Used

- **JavaFX** for UI components  
- **Apache POI** for Excel export  
- **iText** for PDF export  
- **ZXing (or similar)** for barcode generation  

---

QuickMart combines intuitive UI with robust backend functionality to make retail management easy and efficient. 🚀
