# Planificación y Trazabilidad: Gestión de Resmas

Este directorio contiene la documentación viva, diseño técnico, planes por etapas, registro de tareas delegadas y reportes de ejecución del proyecto **Gestión de Resmas**. Esta carpeta forma parte del repositorio para garantizar total transparencia, reproducibilidad y auditoría de cada ciclo de trabajo.

---

## 1. Resumen del Proyecto

**Gestión de Resmas** es una aplicación móvil nativa para Android desarrollada en **Java** con persistencia local en **SQLite** (`SQLiteOpenHelper`) e interfaz gráfica moderna basada en **Material Design 3 (Material Components)**.

### Propósito y Dominio del Negocio
La aplicación fue diseñada para controlar la comercialización mayorista y minorista de cajas y resmas de papel (tamaños Carta y Oficio), ofreciendo:
- **Gestión de Lotes de Compra (`box_purchases`)**: Registro del costo por caja, unidades por caja (resmas totales), cálculo automático del costo unitario por resma y trazabilidad estricta del inventario remanente por lote (`resmas_disponibles`).
- **Cartera de Clientes y Márgenes Dinámicos (`clients`)**: Registro de clientes con un margen de utilidad personalizado (porcentaje asignado individualmente).
- **Cálculo Dinámico de Precios**:
  $$\text{Precio Unitario Aplicado} = \text{Costo Unitario de la Caja} \times \left(1 + \frac{\text{Margen de Utilidad del Cliente}}{100}\right)$$
- **Ventas Multi-Lote con Carrito (`sales` y `sale_details`)**: Posibilidad de despachar resmas de diferentes cajas/lotes en una sola transacción con deducción atómica de inventario bajo transacciones SQLite seguras (`beginTransaction()`, `setTransactionSuccessful()`, `endTransaction()`).
- **Cuentas por Cobrar y Registro de Abonos (`sales` con estado `DEBE` y tabla `payments`)**: Seguimiento de saldos pendientes por cliente, historial consolidado de facturas impagadas y registro de pagos parciales o totales.
- **Métricas de Inventario y Utilidad Proyectada**: Dashboard ejecutivo con resmas disponibles por tipo (Carta vs. Oficio), capital invertido en inventario activo y estimación de ganancia total proyectada.

---

## 2. Estructura de la Carpeta `planificacion/`

```text
planificacion/
├── README.md                      # Índice general, metodología, tabla de etapas y registro de bloques
├── DESIGN_SYSTEM.md               # Especificación del sistema de diseño (colores, insets, componentes)
├── comun/
│   └── base.md                    # Contexto base, stack técnico, reglas y convenciones inmutables
└── etapas/
    ├── 1-inicializacion-git/
    │   ├── PLAN.md                # Plan de configuración inicial y ordenamiento de commits base
    │   └── REPORTE.md             # Reporte de cierre con los 3 commits iniciales en 'main'
    └── 2-rediseno-interfaz/
        ├── PLAN.md                # Diagnóstico UX, diseño Edge-to-Edge y plan de trabajo
        ├── REPORTE.md             # Reporte de ejecución de los 4 bloques y 10 commits en 'redesign-interface'
        └── bloques/
            ├── 1-infraestructura-edge-to-edge/
            │   └── tarea.md       # Especificación del Bloque 1 (iconos, temas transparentes, UiUtils)
            ├── 2-dashboard-cajas-historial/
            │   └── tarea.md       # Especificación del Bloque 2 (Dashboard, Compras y Ventas)
            ├── 3-clientes-detalle-deudas/
            │   └── tarea.md       # Especificación del Bloque 3 (Clientes, Detalle y Deudas)
            └── 4-nueva-venta-preservacion-estado/
                └── tarea.md       # Especificación del Bloque 4 (Nueva Venta y ciclo de vida)
```

---

## 3. Metodología de Trabajo y Orquestación de Subagentes

El desarrollo y mantenimiento del proyecto opera bajo un modelo jerárquico de **Agente Principal (Orquestador)** y **Subagentes de Ejecución Especializados**, garantizando máxima precisión, ahorro de tokens y trazabilidad quirúrgica:

### Rol del Agente Principal (Orquestador y Validador)
- **Análisis y Auditoría**: Inspecciona el código existente, evalúa cumplimiento de guías de diseño (Material 3, directrices de Android), audita arquitectura y detecta riesgos de concurrencia o interfaz.
- **Diseño de Planes**: Redacta los archivos `PLAN.md` y desglosa los objetivos en bloques de trabajo desacoplados y secuenciales.
- **Generación de Prompts Autocontenidos**: Envía instrucciones exhaustivas a cada subagente con especificaciones exactas de rutas, clases, métodos, firmas, layouts XML y mensajes de commit.
- **Validación Continua**: Verifica el estado del árbol de trabajo (`git status`, `git log`) antes y después de cada bloque.
- **Compilación y Pruebas**: Ejecuta `.\gradlew.bat test` y `.\gradlew.bat assembleDebug` tras cada entrega para garantizar que ningún cambio rompa el build.
- **Optimización de Contexto**: El orquestador no edita archivos de código directamente si puede delegarlo, protegiendo su ventana de contexto y capacidad de razonamiento estratégico.

### Rol de los Subagentes (Ejecutores con Gemini 3.8 Flash - `Model: "flash"`)
- **Ejecución Operativa Rápida**: Crean ramas, editan archivos Java/XML, crean drawables vectoriales y gestionan recursos locales.
- **Alineación Estricta a Convenciones**: Cumplen al pie de la letra las directrices de `commits-atomicos` y las reglas del stack documentadas en `comun/base.md`.
- **Commits Atómicos**: Crean commits independientes con un solo tema por commit, sin mezclar cambios ni incluir metadatos de autoría externa o trailers de IA.

---

## 4. Skills Utilizadas en el Proyecto

El equipo y los agentes aplican estrictamente las siguientes skills:

1. **`commits-atomicos`**:
   - Cada commit cuenta un único cambio lógico y deja el proyecto compilando y pasando pruebas.
   - Mensajes en español con prefijo de tipo (`feat:`, `fix:`, `refactor:`, `perf:`, `test:`, `docs:`, `chore:`).
   - Minúscula tras los dos puntos y sin punto final.
   - Sin trailers de coautoría (`Co-Authored-By:`) ni menciones a herramientas de IA.

2. **`edge-to-edge` (Adaptada a Java + XML Views)**:
   - Integración nativa mediante `EdgeToEdge.enable(activity)` en `UiUtils.setupEdgeToEdge(...)`.
   - Barras de estado y navegación 100% transparentes en `values/themes.xml` y `values-night/themes.xml` (`statusBarColor`, `navigationBarColor` a `@android:color/transparent`, `enforceStatusBarContrast` y `enforceNavigationBarContrast` a `false`).
   - Aplicación de insets con `ViewCompat.setOnApplyWindowInsetsListener`:
     - **Top / Display Cutout**: Padding superior en `AppBarLayout` / `MaterialToolbar`.
     - **Bottom / IME**: Padding inferior en listas con `clipToPadding="false"` y margen inferior dinámico en `FloatingActionButton` (evitando solapamientos con la barra de gestos o el teclado).

3. **`adaptive` (Adaptada a Java + XML Views)**:
   - Layouts dinámicos mediante `UiUtils.createAdaptiveLayoutManager(context, 340)`: calcula el ancho disponible en `dp` y ajusta automáticamente las columnas (`spanCount`) en teléfonos verticales (1 col) y en tablets/horizontal (2 o más cols).
   - Ocultación eficiente de barras de herramientas al hacer scroll (`app:layout_scrollFlags="scroll|enterAlways|snap"`) en `CoordinatorLayout` para maximizar el área visible.
   - Preservación de estado ante rotaciones y cambios de configuración (`onSaveInstanceState` / `onRestoreInstanceState`) para listas dinámicas (carrito de ventas) y selección de filtros en chips.

4. **`planificacion-por-etapas`**:
   - Centralización documental fuera del control de versiones de Git en la carpeta `planificacion/`, dividida en etapas numeradas con planes formales, bloques de ejecución y reportes de cierre.

---

## 5. Tabla de Etapas del Proyecto

| # | Etapa | Rama | Estado | Fecha | Resumen |
|---|---|---|---|---|---|
| 1 | `1-inicializacion-git` | `main` | Completada | 2026-11-03 | Inicialización del repositorio Git, depuración de `.gitignore` para excluir archivos de Android Studio y partición del código base en 3 commits atómicos. |
| 2 | `2-rediseno-interfaz` | `redesign-interface` | Completada | 2026-11-03 | Auditoría integral de usabilidad, implementación de Edge-to-Edge nativo, grillas adaptativas, accesibilidad táctil (48dp) y preservación de estado ejecutada en 4 bloques con subagentes. |

---

## 6. Índice de Bloques y Subagentes Ejecutados (Etapa 2)

| Bloque | Descripción | Subagente ID | Modelo | Archivos Principales | Commits Generados |
|---|---|---|---|---|---|
| **Bloque 1** | Infraestructura visual, drawables vectoriales, temas transparentes y clase de utilidades `UiUtils`. | `87e80f7e-d4fb-48cb-8a61-33c7148dcd90` | `Gemini 3.8 Flash` | `ic_delete.xml`, `ic_search.xml`, `ic_calendar.xml`, `values/themes.xml`, `values-night/themes.xml`, `UiUtils.java` | `1d47e74`, `8f30490`, `cb01428` |
| **Bloque 2** | Rediseño de Dashboard principal, gestión de compras de cajas con scroll de diálogo e historial de ventas con chips adaptativos. | `9feabb2a-4016-4c0a-8e48-a902e87d4c65` | `Gemini 3.8 Flash` | `MainActivity.java`, `activity_main.xml`, `BoxPurchasesActivity.java`, `activity_box_purchases.xml`, `dialog_add_box.xml`, `item_box_purchase.xml`, `SalesHistoryActivity.java`, `activity_sales_history.xml`, `item_sale.xml` | `cf74e80`, `b4395e3`, `caf1f26` |
| **Bloque 3** | Adaptabilidad y Edge-to-Edge en cartera de clientes, diálogo con scroll, pantalla de detalle y libro de deudas con registro de abonos. | `2d82de51-6f49-43f1-b303-743b92450f75` | `Gemini 3.8 Flash` | `ClientAdapter.java`, `ClientsActivity.java`, `activity_clients.xml`, `dialog_add_edit_client.xml`, `item_client.xml`, `ClientDetailActivity.java`, `activity_client_detail.xml`, `DebtLedgerActivity.java`, `activity_debt_ledger.xml`, `dialog_register_payment.xml`, `item_debt_client.xml` | `ce2992c`, `8b2dfa2`, `523f2f6` |
| **Bloque 4** | Formulario de nueva venta con carrito multi-lote, selector de cliente, preservación de estado (`onSaveInstanceState`) y cálculo en tiempo real. | `8e2d3eb6-333f-47bc-8196-07bd6baef058` | `Gemini 3.8 Flash` | `NewSaleActivity.java`, `activity_new_sale.xml`, `item_sale_cart.xml` | `dd00b3a` |

---
*Fin del índice general de planificación.*
