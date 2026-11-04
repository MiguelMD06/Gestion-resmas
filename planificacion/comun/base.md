# Base Común de Contexto y Reglas del Proyecto

Este archivo establece el contexto técnico, la arquitectura de código y las convenciones inmutables para cualquier desarrollador o subagente que trabaje en el proyecto **Gestión de Resmas**. Debe ser leído antes de iniciar cualquier bloque de desarrollo.

---

## 1. Stack Técnico y Configuración

- **Plataforma**: Android nativo.
- **Lenguaje**: Java 11 (código fuente y bytecodes compatibles con Java 11).
- **SDK**:
  - `compileSdk`: 36
  - `targetSdk`: 36
  - `minSdk`: 24 (Android 7.0 Nougat o superior)
- **Sistema de Compilación**: Gradle con Kotlin DSL (`build.gradle.kts` a nivel raíz y módulo `:app`).
- **Librerías y Dependencias Principales**:
  - Google Material Components: `com.google.android.material:material:1.14.0`
  - AndroidX AppCompat: `androidx.appcompat:appcompat:1.7.1`
  - AndroidX Activity: `androidx.activity:activity:1.13.0` (incluye utilidades de Edge-to-Edge)
  - AndroidX ConstraintLayout: `androidx.constraintlayout:constraintlayout:2.2.1`
  - AndroidX RecyclerView: incluido vía Material / AppCompat
  - Persistencia: SQLite nativo a través de Android SDK (`android.database.sqlite.SQLiteOpenHelper` y `SQLiteDatabase`)
  - Testing: JUnit 4 (`junit:junit:4.13.2`)

---

## 2. Arquitectura de Paquetes (`com.example.gestionresmas`)

El proyecto sigue una arquitectura limpia por capas (Model - Data Access - UI Views):

```text
com.example.gestionresmas/
├── MainActivity.java                  # Dashboard principal con métricas de inventario y accesos directos
│
├── database/                          # Capa de persistencia y acceso a datos
│   ├── DatabaseHelper.java            # Esquema SQLite, creación de tablas, índices y gestión de versiones
│   ├── ClientDao.java                 # CRUD de clientes y cálculo de márgenes
│   ├── BoxPurchaseDao.java            # Compras de lotes de cajas, inventario activo y deducciones
│   └── SaleDao.java                   # Ventas, detalle de ítems, pagos y consultas transaccionales
│
├── models/                            # Entidades del dominio
│   ├── Client.java                    # Cliente (id, nombre, teléfono, margen_utilidad)
│   ├── BoxPurchase.java               # Lote de compra (tipo_resma, costo, resmas totales y disponibles)
│   ├── Sale.java                      # Registro de venta (cliente, fecha, total, estado_pago)
│   ├── SaleDetail.java                # Detalle de venta (lote origen, cantidad, precio aplicado, subtotal) - Serializable
│   ├── Payment.java                   # Abono/Pago a deuda (venta_id, monto, fecha, nota)
│   ├── ClientDebtSummary.java         # DTO con resumen de deuda consolidada por cliente
│   └── InventorySummary.java          # DTO con agregados de stock, inversión y utilidades estimadas
│
└── ui/                                # Capa de presentación (Activities y Adapters)
    ├── UiUtils.java                   # Métodos utilitarios transversales para Edge-to-Edge y adaptabilidad
    ├── boxes/
    │   ├── BoxPurchasesActivity.java  # Listado de compras de lotes y registro mediante diálogo modal
    │   └── BoxPurchaseAdapter.java    # Adaptador para renderizar tarjetas de lotes
    ├── clients/
    │   ├── ClientsActivity.java       # Cartera de clientes, búsqueda y formulario modal
    │   ├── ClientDetailActivity.java  # Historial de compras, estado financiero y abonos del cliente
    │   └── ClientAdapter.java         # Adaptador para lista de clientes
    ├── debt/
    │   ├── DebtLedgerActivity.java    # Libro de cuentas por cobrar agrupado por cliente
    │   └── DebtAdapter.java           # Adaptador para tarjetas de deudas
    └── sales/
        ├── NewSaleActivity.java       # Pantalla de creación de venta, carrito multi-lote y cálculo reactivo
        ├── SalesHistoryActivity.java  # Historial general de ventas con filtrado por tipo de pago
        ├── SalesAdapter.java          # Adaptador del historial de ventas
        └── SaleCartAdapter.java       # Adaptador de los ítems añadidos al carrito de venta actual
```

---

## 3. Reglas de Negocio Clave

1. **Margen y Precio de Venta**:
   El precio unitario de una resma nunca es fijo. Depende estrictamente del lote del cual se extraiga la resma y del cliente comprador:
   $$\text{Precio Unitario Aplicado} = \text{Costo Unitario de la Caja} \times \left(1 + \frac{\text{Margen del Cliente}}{100}\right)$$
2. **Consistencia Transaccional**:
   Toda inserción en `sales` y sus correspondientes `sale_details` debe restar atómicamente el stock en `box_purchases.resmas_disponibles` dentro de una transacción SQLite:
   ```java
   db.beginTransaction();
   try {
       // Insertar venta -> Insertar detalles -> Descontar stock
       db.setTransactionSuccessful();
   } finally {
       db.endTransaction();
   }
   ```
3. **Manejo de Estados de Pago**:
   - `PAGADO`: Venta liquidada al contado.
   - `DEBE`: Venta a crédito. Puede recibir abonos parciales en la tabla `payments`.

---

## 4. Convenciones de Desarrollo y UI

Para mantener la consistencia estética y funcional, todo nuevo desarrollo debe acatar las siguientes pautas:

1. **Soporte Edge-to-Edge Obligatorio**:
   - En cada `Activity.onCreate()`, justo después de `setContentView(...)`, llamar a `UiUtils.setupEdgeToEdge(this, rootView, appBarLayout, recyclerView, fab)`.
   - Las barras de sistema deben permanecer transparentes.
   - Los elementos de scroll (`RecyclerView`, `NestedScrollView`) deben configurar `clipToPadding="false"`.

2. **Diseño Adaptativo en Listas (`RecyclerView`)**:
   - Nunca usar un `LinearLayoutManager` rígido cuando el contenido deba adaptarse a orientación horizontal o tablets.
   - Usar siempre:
     ```java
     recyclerView.setLayoutManager(UiUtils.createAdaptiveLayoutManager(this, 340));
     ```
     Esto garantiza 1 columna en pantallas de ancho estándar (<680dp) y 2 o más columnas en anchos mayores (horizontal o tabletas).

3. **Accesibilidad y Ergonomía Táctil**:
   - Todo botón, chip, campo de entrada o ícono clicable debe cumplir con el área táctil mínima de **48dp x 48dp** (mediante `android:minHeight="48dp"`, `android:minWidth="48dp"` o padding adecuado).
   - Los diálogos modales que contengan campos de texto (`EditText` / `TextInputLayout`) deben estar contenidos en un `NestedScrollView` con `android:fadeScrollbars="false"`. Esto previene que la apertura del teclado virtual (IME) oculte botones de acción (`Guardar` / `Cancelar`) o campos inferiores.

4. **Preservación del Ciclo de Vida**:
   - Toda pantalla con datos en memoria que no hayan sido persistidos (por ejemplo, el carrito de `NewSaleActivity`) debe implementar `onSaveInstanceState` y restaurar los datos en `onCreate` o `onRestoreInstanceState`.
   - `SaleDetail` implementa `Serializable` para permitir su almacenamiento seguro en `Bundle`.

5. **Convención de Commits Atómicos**:
   - Estricto seguimiento de `commits-atomicos`: mensajes en español, prefijo en minúscula (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`), sin punto final, sin trailers de coautoría y un tema único por commit.

---

## 5. Comandos de Verificación del Proyecto

Antes de considerar terminada cualquier tarea o commit:
```powershell
# Ejecución de pruebas unitarias
.\gradlew.bat test

# Verificación de compilación y empaquetado del APK de debug
.\gradlew.bat assembleDebug
```
Ambos comandos deben finalizar con resultado `BUILD SUCCESSFUL`.
