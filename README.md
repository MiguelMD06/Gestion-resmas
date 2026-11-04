# Gestión de Resmas — Rama `redesign-interface`

> **Rama `redesign-interface`**: Versión con rediseño completo de interfaz, soporte Edge-to-Edge nativo y adaptabilidad para múltiples factores de forma (smartphones y tablets). Si buscas la versión base estándar del sistema, consulta la rama [`main`](https://github.com/MiguelMD06/Gestion-resmas/tree/main).

Aplicación móvil nativa para Android desarrollada en **Java** con persistencia local en **SQLite** y componentes **Material Design 3**, diseñada para el control comercial, trazabilidad de lotes y administración de ventas de resmas de papel (Carta y Oficio).

---

## 📱 Novedades y Mejoras en esta Rama

Esta rama implementa la **Etapa 2** del proyecto, enfocada en optimizar la ergonomía, la accesibilidad táctil y la experiencia visual en cualquier dispositivo Android:

### 1. Experiencia Edge-to-Edge Nativa y Limpia
- **Barras de sistema 100% transparentes**: Configuración en temas diurno y nocturno (`values/themes.xml` y `values-night/themes.xml`) para eliminar barras opacas o contrastes forzados (`enforceStatusBarContrast="false"` y `enforceNavigationBarContrast="false"`).
- **Gestión reactiva de WindowInsets (`UiUtils.setupEdgeToEdge`)**:
  - **Superior**: Padding automático en `AppBarLayout` y toolbars para respetar notches y cortes de cámara (*display cutouts*).
  - **Inferior / Gestos**: Padding inferior dinámico en `RecyclerView` con `clipToPadding="false"`, asegurando que el último elemento de cualquier lista nunca quede oculto detrás de la barra de navegación por gestos.
  - **Teclado virtual (IME)**: Los diálogos y formularios ajustan sus márgenes e insets automáticamente cuando se despliega el teclado.
  - **Botones de acción flotante (FAB)**: Margen inferior reactivo que se eleva cuando la barra de navegación o el teclado lo requieren.

### 2. Diseño Adaptativo (Smartphones y Tablets)
- **Columnas dinámicas (`UiUtils.createAdaptiveLayoutManager`)**: Cálculo en tiempo de ejecución del ancho disponible en `dp`:
  - **Smartphones en vertical**: 1 columna con tarjetas de ancho completo para máxima legibilidad.
  - **Smartphones en horizontal y Tablets (ancho $\ge$ 600dp)**: 2 o más columnas automáticas mediante `GridLayoutManager`, aprovechando el espacio horizontal sin estirar artificialmente las tarjetas.
- **Scroll coordinado**: Integración de `CoordinatorLayout` con `app:layout_scrollFlags="scroll|enterAlways|snap"` en cabeceras para ocultar suavemente las barras de herramientas al hacer scroll descendente y recuperarlas al instante con un gesto ascendente.

### 3. Preservación de Estado de Ciclo de Vida
- Implementación de `onSaveInstanceState` y `onRestoreInstanceState` en flujos críticos como `NewSaleActivity`, garantizando que ante rotaciones de pantalla o cambios de configuración no se pierda el cliente seleccionado, las cantidades ingresadas ni los ítems cargados en el carrito de venta.

### 4. Diálogos Accesibles e Iconografía Vectorial
- Todos los formularios modales (`dialog_add_box.xml`, `dialog_add_edit_client.xml`, `dialog_register_payment.xml`) implementan `NestedScrollView` con restricciones de altura controlada, permitiendo scroll fluido incluso en pantallas pequeñas con el teclado abierto.
- Nuevos activos vectoriales nativos para acciones comunes: `ic_calendar.xml`, `ic_delete.xml` e `ic_search.xml`.

---

## 🔄 Comparativa de Ramas

| Característica | Rama `main` | Rama `redesign-interface` |
|---|---|---|
| **Arquitectura de Base de Datos y Negocio** | SQLite relacional completo | Idéntica base y compatibilidad |
| **Diseño Visual** | Material Design 3 estándar | Material Design 3 refinado con microinteracciones |
| **Soporte Edge-to-Edge** | No implementado (insets predeterminados) | Nativo con barras transparentes y manejo de insets |
| **Adaptabilidad a Pantallas** | Columnas fijas | Grillas adaptativas automáticas (teléfono / tablet) |
| **Preservación de Estado ante Rotación** | Comportamiento por defecto | Persistencia de estado en carrito de ventas |
| **Formularios Modales** | Estándar | Con `NestedScrollView` y compatibilidad IME |

---

## 💼 Dominio y Reglas de Negocio

El núcleo de la aplicación resuelve de forma automatizada las operaciones diarias del negocio:

1. **Trazabilidad de Cajas y Lotes (`box_purchases`)**:
   - Registro de compras de cajas de resmas (Carta / Oficio).
   - Cálculo automático del costo unitario por resma:
     $$\text{Costo Unitario} = \frac{\text{Precio Compra Caja}}{\text{Cantidad Total de Resmas}}$$
   - Control de saldo remanente (`resmas_disponibles`) lote por lote.

2. **Cartera de Clientes y Precios Dinámicos (`clients`)**:
   - Cada cliente tiene un porcentaje de margen de utilidad configurado.
   - Al registrar una venta, el precio de venta unitario se calcula en tiempo real según el lote seleccionado:
     $$\text{Precio Venta Unitario} = \text{Costo Unitario del Lote} \times \left(1 + \frac{\text{Margen de Utilidad del Cliente}}{100}\right)$$

3. **Ventas Atómicas Multi-Lote (`sales` y `sale_details`)**:
   - Posibilidad de vender resmas de distintos lotes en una sola transacción.
   - Actualización transaccional segura en SQLite: el inventario remanente se descuenta atómicamente; si algún lote no tiene existencias suficientes, la transacción se revierte.

4. **Libro de Deudas y Registro de Abonos (`payments`)**:
   - Seguimiento de facturas con estado `DEBE`.
   - Consulta del saldo consolidado adeudado por cliente y registro de pagos parciales o liquidaciones totales.

5. **Métricas de Inventario y Ganancia Estimada**:
   - Stock total discriminado por tipo (Carta vs. Oficio).
   - Capital total invertido en inventario activo y estimación de ganancia proyectada.

---

## 🗄️ Esquema de Base de Datos (SQLite)

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
│   │   ├── models/                          # Clientes, Cajas, Ventas, Detalles y Pagos
│   │   └── ui/
│   │       ├── UiUtils.java                 # Utilidades Edge-to-Edge y LayoutManager adaptativo
│   │       ├── boxes/                       # Gestión y registro de compras de cajas
│   │       ├── clients/                     # Cartera, búsqueda, detalle y margen de clientes
│   │       ├── debt/                        # Libro de deudas y registro de abonos
│   │       └── sales/                       # Carrito de ventas multi-lote e historial
│   └── res/                                 # Layouts adaptativos, drawables y temas M3
├── planificacion/                           # Documentación viva, diseño y auditoría por etapas
│   ├── README.md                            # Índice general y metodología de trabajo
│   ├── DESIGN_SYSTEM.md                     # Guía de estilo visual y directrices UX
│   ├── comun/base.md                        # Contexto base, stack tecnológico y convenciones
│   └── etapas/                              # Planes y reportes de ejecución (Etapas 1 y 2)
└── README.md
```

---

## 📋 Documentación de Planificación

Para conocer en detalle el proceso de diseño, los diagnósticos de interfaz y los reportes de ejecución de los 4 bloques de trabajo de esta rama, consulta la carpeta [`planificacion/`](planificacion/README.md).

---

## 🛠️ Requisitos y Compilación

- **Entorno de desarrollo**: Android Studio (Ladybug / Hedgehog o superior).
- **JDK**: Java 17 o superior.
- **Min SDK**: API 26 (Android 8.0 Oreo).
- **Target / Compile SDK**: API 36.

### Compilar desde la terminal:
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

Este proyecto está bajo la Licencia MIT. Consulta el archivo [LICENSE](LICENSE) para más información.
