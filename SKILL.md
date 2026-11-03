# SKILL: Android Paper Ream Sales Management App (Java & SQLite)

## Description
This skill instructs the Antigravity CLI agent to build a native Android application using **Java** and **SQLite** for managing paper ream sales (Carta & Oficio). The app must support box purchase traceability, inventory control by lots, custom profit margins per client, dynamic unit pricing, and detailed debt/payment tracking.

## Core Domain & Database Schema
The SQLite database must be implemented with the following relational structure:

1. **`clients` Table**:
   - `id` (INTEGER, PRIMARY KEY AUTOINCREMENT)
   - `nombre` (TEXT, NOT NULL)
   - `telefono` (TEXT)
   - `margen_utilidad` (REAL, NOT NULL) -> Custom profit percentage for this client (e.g., 5.0 for 5%, 3.0 for 3%).

2. **`box_purchases` Table (CajaResma)**:
   - `id` (INTEGER, PRIMARY KEY AUTOINCREMENT)
   - `tipo_resma` (TEXT, NOT NULL) -> Values: 'CARTA' or 'OFICIO'
   - `fecha_compra` (TEXT, NOT NULL)
   - `precio_compra_caja` (REAL, NOT NULL) -> Total cost of the purchased box.
   - `cantidad_resmas_total` (INTEGER, NOT NULL) -> Total reams inside the box.
   - `resmas_disponibles` (INTEGER, NOT NULL) -> Current remaining stock in this specific lot.
   - `precio_costo_unitario` (REAL, NOT NULL) -> Computed as `precio_compra_caja / cantidad_resmas_total`.

3. **`sales` Table (Venta)**:
   - `id` (INTEGER, PRIMARY KEY AUTOINCREMENT)
   - `cliente_id` (INTEGER, FOREIGN KEY references clients(id))
   - `fecha` (TEXT, NOT NULL)
   - `total_venta` (REAL, NOT NULL)
   - `estado_pago` (TEXT, NOT NULL) -> Values: 'PAGADO' or 'DEBE'

4. **`sale_details` Table (DetalleVenta)**:
   - `id` (INTEGER, PRIMARY KEY AUTOINCREMENT)
   - `venta_id` (INTEGER, FOREIGN KEY references sales(id))
   - `caja_id` (INTEGER, FOREIGN KEY references box_purchases(id)) -> Traceability link to the specific box lot.
   - `cantidad` (INTEGER, NOT NULL)
   - `precio_unitario_aplicado` (REAL, NOT NULL)
   - `subtotal` (REAL, NOT NULL) -> Computed as `cantidad * precio_unitario_aplicado`.

## Core Business Rules & Calculations
1. **Dynamic Unit Pricing Formula**:
   When selling a ream from a specific box to a specific client, the unit price must be calculated dynamically:
   $$\text{Precio Unitario Aplicado} = \text{Costo Unitario de la Caja} \times \left(1 + \frac{\text{Margen de Utilidad del Cliente}}{100}\right)$$
2. **Inventory Deduction**:
   When a sale is registered, the `resmas_disponibles` of the selected `box_purchases` lot must be reduced by the sold quantity. Prevent sales if stock is insufficient.
3. **Stock & Profit Analytics**:
   - Total stock per size is obtained by summing `resmas_disponibles` across all active boxes (`resmas_disponibles > 0`).
   - Estimated stock profit: Sum of `(Calculated Selling Price - Unit Cost) * resmas_disponibles` for all remaining items in stock.
4. **Debt & Client Ledger**:
   - Filter sales where `estado_pago = 'DEBE'` grouped by client to show who owes money, total balance owed, and full purchase history (date, quantities, and totals).

## Technical Architecture & Implementation Guidelines
- **Language**: Java.
- **Database Helper**: Use `SQLiteOpenHelper` with proper transaction management (`beginTransaction()`, `setTransactionSuccessful()`, `endTransaction()`) when inserting sales and updating inventory simultaneously.
- **UI Design**: Clean, intuitive, and modern Android Activities/Fragments with responsive XML layouts.
- **Code Structure**: Organize code cleanly into packages: `models`, `database` (DAOs / DB Helper), and `ui` (Activities / Adapters).

## Execution Roadmap for the Agent
1. **Step 1**: Create the SQLite database helper (`DatabaseHelper.java`) implementing all tables, constraints, and sample data seeding if necessary.
2. **Step 2**: Implement Repository/DAO classes for Clients, Box Purchases, and Sales with CRUD operations and core mathematical calculation methods.
3. **Step 3**: Develop the UI Activities:
   - **Dashboard**: Stock status, total inventory valuation, and active debt summary.
   - **Box Purchase Management**: Register box lots and keep price traceability.
   - **Client Management**: Create/edit clients and assign their utility margin percentage.
   - **Sales Registration**: Select client, box lot, compute dynamic prices, and log payment status (Paid/Debt).
   - **Client Debt Ledger**: Track active debts and historical purchases per client.