# Sistema de Diseño y Guía de Interfaz (Design System)

Este documento detalla los fundamentos visuales, componentes de interfaz de usuario, tokens de diseño y reglas de adaptabilidad implementados en **Gestión de Resmas**. El sistema se basa en los principios de **Material Design 3 (M3)**, optimizado para vistas nativas Android en Java + XML.

---

## 1. Paleta de Colores y Tokens Visuales

La aplicación cuenta con soporte dual de temas (Modo Claro y Modo Oscuro). Cada color de marca (`primary`, `secondary`, etc.) posee su correspondiente contenedor de bajo contraste (`*_container`) y color de contenido (`on_*_container`), garantizando contraste WCAG AAA para texto y elementos interactivos.

### Tokens de Color (Mapeo Claro vs. Oscuro)

| Token Semántico | Modo Claro (`values/colors.xml`) | Modo Oscuro (`values-night/colors.xml`) | Uso Principal |
|---|---|---|---|
| `primary` | `#1E40AF` (Azul M3 Intenso) | `#3B82F6` (Azul M3 Vibrante) | Botones primarios, AppBar, estado activo en chips |
| `primary_dark` | `#1E3A8A` | `#1D4ED8` | Variantes de sombra o énfasis de marca |
| `primary_light` | `#3B82F6` | `#60A5FA` | Resaltados secundarios de marca |
| `primary_container` | `#DBEAFE` (Azul Suave) | `#1E3A8A` (Azul Noche Profundo) | Contenedores destacados, badges informativos |
| `on_primary_container` | `#1E3A8A` | `#DBEAFE` | Texto sobre contenedores primarios |
| `secondary` | `#0D9488` (Teal / Verde Azulado) | `#14B8A6` (Teal Brillante) | Acciones secundarias, badges de tipo de resma |
| `secondary_container` | `#CCFBF1` | `#134E4A` | Fondos de etiquetas de producto |
| `on_secondary_container` | `#115E59` | `#99F6E4` | Texto sobre contenedor secundario |
| `accent_warning` | `#D97706` (Ámbar) | `#F59E0B` (Ámbar Claro) | Alertas de stock bajo o estados pendientes |
| `warning_container` | `#FEF3C7` | `#78350F` | Fondo de tarjeta de advertencia |
| `on_warning_container` | `#92400E` | `#FDE68A` | Texto de advertencia |
| `accent_danger` | `#DC2626` (Rojo Carmesí) | `#EF4444` (Rojo Coral) | Cuentas por cobrar (`DEBE`), acciones destructivas |
| `danger_container` | `#FEE2E2` | `#7F1D1D` | Fondo de tarjetas o badges de deuda |
| `on_danger_container` | `#991B1B` | `#FECACA` | Texto de deuda o saldos pendientes |
| `accent_success` | `#16A34A` (Verde Esmeralda) | `#22C55E` (Verde Menta) | Ventas pagadas (`PAGADO`), stock saludable |
| `success_container` | `#DCFCE7` | `#14532D` | Fondo de badges de venta completada |
| `on_success_container` | `#166534` | `#BBF7D0` | Texto de éxito o liquidación |
| `background` | `#F8FAFC` (Gris Hielo) | `#0F172A` (Azul Marino Oscuro) | Fondo general de las actividades |
| `surface` | `#FFFFFF` (Blanco Puro) | `#1E293B` (Gris Pizarra Oscuro) | Fondo de cards, toolbars y diálogos |
| `surface_variant` | `#F1F5F9` | `#334155` | Fondos de chips no seleccionados, barras de búsqueda |
| `card_background` | `#FFFFFF` | `#1E293B` | Fondo de `MaterialCardView` |
| `card_stroke` | `#E2E8F0` | `#334155` | Borde sutil de separación en tarjetas |
| `dialog_background` | `#FFFFFF` | `#1E293B` | Superficie de diálogos modales |
| `text_primary` | `#0F172A` | `#F8FAFC` | Títulos principales, valores numéricos clave |
| `text_secondary` | `#475569` | `#CBD5E1` | Subtítulos, etiquetas de descripción |
| `text_muted` | `#94A3B8` | `#94A3B8` | Textos de ayuda, fechas secundarias |
| `divider` | `#E2E8F0` | `#334155` | Líneas divisorias en listas y diálogos |

---

## 2. Sistema Edge-to-Edge e Insets de Sistema

El soporte de visualización inmersiva de borde a borde (*Edge-to-Edge*) es transversal en todas las pantallas de la aplicación, desacoplando la renderización de la ventana de las barras del sistema operativo (Status Bar y Navigation Bar).

### Configuración en Temas (`themes.xml` y `values-night/themes.xml`)
Ambos archivos configuran barras de sistema 100% transparentes e inhabilitan el oscurecimiento forzado:
```xml
<!-- Status bar y Navigation bar transparentes para Edge-to-Edge -->
<item name="android:statusBarColor">@android:color/transparent</item>
<item name="android:windowLightStatusBar">false</item>
<item name="android:navigationBarColor">@android:color/transparent</item>
<item name="android:windowLightNavigationBar" tools:targetApi="27">true</item>
<!-- Desactiva el forzado de contraste automático del sistema (API 29+) -->
<item name="android:enforceStatusBarContrast" tools:targetApi="29">false</item>
<item name="android:enforceNavigationBarContrast" tools:targetApi="29">false</item>
```

### Gestión Dinámica mediante `UiUtils.setupEdgeToEdge(...)`
La clase central `UiUtils` se encarga de:
1. Activar el modo con `EdgeToEdge.enable(activity)`.
2. Registrar un listener `ViewCompat.setOnApplyWindowInsetsListener` sobre el contenedor raíz.
3. Extraer y combinar los insets de:
   - `WindowInsetsCompat.Type.systemBars()`
   - `WindowInsetsCompat.Type.displayCutout()` (cámaras punch-hole y notches)
   - `WindowInsetsCompat.Type.ime()` (teclado virtual en pantalla)
4. Distribuir inteligentemente los offsets:
   - **`appBar`**: Se le suma padding superior (`topInset`), preservando márgenes laterales si el dispositivo tiene recorte horizontal.
   - **`scrollingContent` (`RecyclerView` o `NestedScrollView`)**: Recibe `setClipToPadding(false)` y un padding inferior dinámico que incluye el `bottomInset` (barra de navegación o teclado) más **80dp adicionales** si la pantalla tiene un `FloatingActionButton` (evitando que el último elemento de la lista quede tapado).
   - **`fab` (`FloatingActionButton`)**: Ajusta dinámicamente sus `bottomMargin` y `rightMargin` sumando los insets del sistema, asegurando que el botón flote con separación limpia sobre la barra de navegación o se eleve fluidamente al abrir el teclado.

---

## 3. Layouts Adaptativos y Multi-Ventana

La interfaz fue diseñada para escalar con elegancia desde pantallas compactas de smartphone hasta plegables (*foldables*) y tabletas en orientación horizontal.

### Grilla Adaptativa Automática
En lugar de fijar un número estático de columnas o depender exclusivamente de `LinearLayoutManager`, las pantallas principales (`BoxPurchasesActivity`, `ClientsActivity`, `DebtLedgerActivity`, `SalesHistoryActivity`) configuran su `RecyclerView` con:
```java
recyclerView.setLayoutManager(UiUtils.createAdaptiveLayoutManager(this, 340));
```
- **Lógica**: Divide el ancho total disponible en pantalla en `dp` entre el ancho mínimo óptimo por tarjeta (`340dp`).
  - Pantallas estándar de teléfono vertical (~360dp - 410dp) $\rightarrow$ **1 columna**.
  - Teléfonos en horizontal o pantallas plegables abiertas (~700dp) $\rightarrow$ **2 columnas**.
  - Tabletas y pantallas grandes ($\ge$ 1024dp) $\rightarrow$ **3 o más columnas**.

### Márgenes Horizontales en Tarjetas
Para que los ítems se distribuyan simétricamente en grillas de 2 o más columnas sin tocarse, todos los layouts de ítems (`item_box_purchase.xml`, `item_client.xml`, `item_debt_client.xml`, `item_sale.xml`, `item_sale_cart.xml`) implementan:
```xml
android:layout_marginHorizontal="4dp"
android:layout_marginVertical="6dp"
```

### Scroll Flags en `CoordinatorLayout`
Las actividades utilizan `CoordinatorLayout` envolviendo un `AppBarLayout` con `MaterialToolbar`:
```xml
app:layout_scrollFlags="scroll|enterAlways|snap"
```
Al realizar scroll hacia abajo en listas densas, la barra de herramientas se oculta de manera suave, devolviendo el 100% del área de pantalla a la lista. Al hacer un ligero gesto hacia arriba, reaparece de inmediato.

---

## 4. Componentes y Estándares de Accesibilidad

### Áreas Táctiles Mínimas (48dp x 48dp)
Siguiendo las pautas de accesibilidad de Google Play y Android:
- Todos los botones (`MaterialButton`) tienen `android:minHeight="48dp"`.
- Los botones de icono y acciones rápidas (eliminar ítem, selector de fecha, limpiar búsqueda) tienen configurados tamaños mínimos y paddings de al menos `48dp x 48dp`.
- Los Chips de filtrado implementan `chipMinTouchTargetSize="48dp"`.

### Formularios y Diálogos con Scroll Integrado
Para prevenir errores comunes en Android donde el teclado virtual cubre los campos de entrada o los botones de confirmación:
- Diálogos como `dialog_add_box.xml`, `dialog_add_edit_client.xml` y `dialog_register_payment.xml` están estructurados con un contenedor raíz `NestedScrollView` con `android:fadeScrollbars="false"`.
- Los campos de texto utilizan el estilo estándar de Material 3:
  `style="@style/Widget.Material3.TextInputLayout.OutlinedBox"`
  con esquinas redondeadas y etiquetas flotantes animadas.

### Chips de Filtro
- Contenedores `ChipGroup` con `app:singleLine="true"` dentro de un `HorizontalScrollView`, permitiendo un deslizamiento fluido sin romper la jerarquía vertical.
- `app:selectionRequired="true"` para garantizar que siempre haya un filtro activo (p. ej., "Todos", "Carta", "Oficio" o "Todos", "Al Contado", "A Crédito").

### Iconografía Vectorial
El proyecto incluye iconos vectoriales independientes de resolución en `res/drawable/`:
- `ic_delete.xml`: Papelera de reciclaje para eliminación de compras o ítems del carrito.
- `ic_search.xml`: Lupa para búsquedas de clientes y facturas.
- `ic_calendar.xml`: Calendario para selección de fechas de transacciones y abonos.
