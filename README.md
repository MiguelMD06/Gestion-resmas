# Gestión de Resmas — Control de Ventas e Inventario (Android)

Aplicación móvil nativa para Android desarrollada en **Java** con persistencia local en **SQLite** y componentes **Material Design 3**, diseñada para optimizar la comercialización, trazabilidad de lotes e inventario de resmas de papel (tamaños Carta y Oficio).

---

## 🌿 Ramas del Repositorio

El repositorio mantiene dos ramas de trabajo independientes y complementarias:

- **[`main`](https://github.com/MiguelMD06/Gestion-resmas/tree/main) (Rama actual)**: Contiene la arquitectura base del sistema, el esquema de datos relacional en SQLite, las reglas de negocio y los flujos operativos estándar de la aplicación.
- **[`redesign-interface`](https://github.com/MiguelMD06/Gestion-resmas/tree/redesign-interface)**: Rama con la **Etapa 2** implementada, incorporando un rediseño visual moderno, soporte nativo de **Edge-to-Edge** (barras de sistema 100% transparentes e insets dinámicos para cámara y gestos), **grillas adaptativas** automáticas para tablets/teléfonos apaisados y preservación de estado de ciclo de vida (`onSaveInstanceState`).

---

## 🚀 Funcionalidades Principales

1. **Gestión de Lotes de Compra (`box_purchases`)**:
   - Registro de compras de cajas de resmas (Carta u Oficio) con costo total y unidades por caja.
   - Cálculo automático del costo unitario por resma:
     $$\text{Costo Unitario} = \frac{\text{Precio Compra Caja}}{\text{Cantidad Total de Resmas}}$$
   - Trazabilidad estricta del inventario remanente por lote individual (`resmas_disponibles`).

2. **Cartera de Clientes y Precios Dinámicos (`clients`)**:
   - Cada cliente cuenta con un porcentaje de utilidad propio (p. ej., 5%, 8%, 10%).
   - Cálculo en tiempo real del precio unitario al momento del despacho:
     $$\text{Precio Venta Unitario} = \text{Costo Unitario del Lote} \times \left(1 + \frac{\text{Margen de Utilidad del Cliente}}{100}\right)$$

3. **Ventas Multi-Lote con Carrito (`sales` y `sale_details`)**:
   - Despacho de resmas de diferentes cajas/lotes en una sola venta.
   - Manejo transaccional atómico en SQLite (`beginTransaction()`, `setTransactionSuccessful()`, `endTransaction()`): la deducción de existencias y el registro de la venta ocurren de forma indivisible.

4. **Libro de Deudas y Registro de Abonos (`payments`)**:
   - Control de ventas a crédito (`DEBE`) o al contado (`PAGADO`).
   - Consulta consolidada de saldos pendientes por cliente y registro de pagos parciales o cancelaciones totales con historial fechado.

5. **Dashboard Ejecutivo de Inventario**:
   - Resumen en tiempo real de resmas disponibles (Carta vs. Oficio).
   - Valuación del capital activo invertido en stock y estimación de ganancia total proyectada.

---

## 🗄️ Esquema de Base de Datos (SQLite)

La persistencia local se gestiona a través de `DatabaseHelper.java` (`SQLiteOpenHelper`) con las siguientes entidades y relaciones:

```text
clients (id, nombre, telefono, margen_utilidad)
   │
   ├──< sales (id, cliente_id, fecha, total_venta, estado_pago)
   │       │
   │       ├──< sale_details (id, venta_id, caja_id, cantidad, precio_unitario_aplicado, subtotal)
   │       │                               ▲
   │       └──< payments (id, venta_id, fecha, monto)
   │
box_purchases (id, tipo_resma, fecha_compra, precio_compra_caja,
               cantidad_resmas_total, resmas_disponibles, precio_costo_unitario)
```

---

## 📂 Estructura del Código

```text
GestionResmas/
├── app/src/main/
│   ├── java/com/example/gestionresmas/
│   │   ├── MainActivity.java                # Dashboard principal con métricas y accesos
│   │   ├── database/
│   │   │   └── DatabaseHelper.java          # Helper SQLite con DDL, transacciones y consultas
│   │   ├── models/                          # Modelos: Client, BoxPurchase, Sale, SaleDetail, Payment
│   │   └── ui/
│   │       ├── boxes/                       # Registro y catálogo de lotes de cajas
│   │       ├── clients/                     # Cartera, búsqueda y edición de clientes
│   │       ├── debt/                        # Libro de deudas y registro de abonos
│   │       └── sales/                       # Carrito de ventas e historial de transacciones
│   └── res/                                 # Layouts XML, recursos y temas Material 3
├── planificacion/                           # Documentación viva, diseño y auditoría por etapas
│   ├── README.md                            # Metodología de desarrollo y tabla de etapas
│   ├── DESIGN_SYSTEM.md                     # Directrices del sistema de diseño
│   ├── comun/base.md                        # Stack tecnológico y convenciones inmutables
│   └── etapas/                              # Especificaciones de etapas 1 (Base) y 2 (Rediseño)
├── SKILL.md                                 # Especificación funcional de reglas de negocio
└── README.md
```

---

## 📋 Metodología y Planificación

El proyecto sigue un esquema de planificación formal por etapas y bloques documentado en la carpeta [`planificacion/`](planificacion/README.md), donde se detallan los objetivos técnicos, la arquitectura de diseño y los reportes de auditoría de cada ciclo.

---

## 🛠️ Requisitos y Compilación

- **Entorno recomendado**: Android Studio (Ladybug / Hedgehog o superior).
- **JDK**: Java 17 o superior.
- **Min SDK**: API 26 (Android 8.0 Oreo).
- **Target / Compile SDK**: API 36.

### Compilar el proyecto en modo depuración:
```bash
# En Windows (PowerShell / CMD)
.\gradlew.bat assembleDebug

# En Linux / macOS
./gradlew assembleDebug
```

### Ejecutar pruebas unitarias:
```bash
.\gradlew.bat test
```

---

## 📄 Licencia

Este proyecto está distribuido bajo la Licencia MIT. Consulta el archivo [LICENSE](LICENSE) para más información.
