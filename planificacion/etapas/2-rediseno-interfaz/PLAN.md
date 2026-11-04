# Plan: Etapa 2 - Auditoría UX/UI y Rediseño con Soporte Edge-to-Edge y Adaptabilidad

## 1. Contexto y Diagnóstico Inicial

La aplicación **Gestión de Resmas** contaba con una arquitectura de datos sólida en SQLite y lógica de negocio completa. No obstante, una auditoría técnica de interfaz y experiencia de usuario (UX/UI) reveló deficiencias críticas frente a los estándares modernos de Android 15/16 y Material Design 3:

### Diagnóstico por Pantallas y Componentes

1. **Dashboard Principal (`MainActivity` / `activity_main.xml`)**:
   - Falta de soporte Edge-to-Edge: la barra de estado y la barra de navegación superponían o recortaban los elementos superiores del toolbar y las tarjetas inferiores.
   - Layout estático no responsivo a pantallas anchas o cambios de orientación.
2. **Compras de Cajas (`BoxPurchasesActivity` / `activity_box_purchases.xml`)**:
   - Uso de `LinearLayoutManager` rígido en una sola columna, desaprovechando espacio en horizontal y tabletas.
   - El `FloatingActionButton` (FAB) tapaba el último ítem de la lista por falta de padding inferior dinámico (`clipToPadding="false"` ausente).
   - El diálogo de registro (`dialog_add_box.xml`) no tenía scroll, provocando que al desplegarse el teclado virtual (IME), los campos de costo, cantidad y los botones de acción quedaran inaccesibles.
3. **Historial de Ventas (`SalesHistoryActivity` / `activity_sales_history.xml`)**:
   - `ChipGroup` de filtrado sin scroll horizontal fluido (`HorizontalScrollView`).
   - Lista en 1 sola columna sin ajuste dinámico a tabletas.
   - Falta de iconografía vectorial adecuada.
4. **Cartera de Clientes (`ClientsActivity` / `activity_clients.xml`)**:
   - Barra de búsqueda sin insets adecuados.
   - Diálogo modal de cliente (`dialog_add_edit_client.xml`) sin contenedor scrollable: el teclado tapaba el campo de margen de utilidad y los botones `Guardar` / `Cancelar`.
   - Área táctil de edición/eliminación menor a 48dp.
5. **Detalle de Cliente (`ClientDetailActivity` / `activity_client_detail.xml`)**:
   - Falta de absorción de insets de `displayCutout` (muescas / orificios de cámara).
   - Encabezados de métricas financieras desalineados en pantallas de alta densidad.
6. **Libro de Cuentas por Cobrar (`DebtLedgerActivity` / `activity_debt_ledger.xml`)**:
   - Diálogo de registro de abonos (`dialog_register_payment.xml`) sin protección ante el teclado.
   - Lista de deudores estática en una sola columna.
7. **Nueva Venta (`NewSaleActivity` / `activity_new_sale.xml`)**:
   - **Riesgo crítico de pérdida de datos**: La actividad no implementaba `onSaveInstanceState`. Al girar la pantalla o cambiar de tamaño de ventana en modo multitarea, el carrito de compras en memoria se reiniciaba a cero perdiendo todo el trabajo del usuario.
   - Sin cálculo visual adaptativo para pantallas amplias.

---

## 2. Decisiones de Diseño y Arquitectura

1. **Continuidad del Stack**:
   - Se acordó con el usuario **mantener Java 11 + XML Views** con Material Components 1.14.0 en lugar de migrar a Jetpack Compose, preservando la compatibilidad y ligereza del proyecto existente.
2. **Infraestructura Edge-to-Edge Centralizada**:
   - Centralizar el manejo de insets en una clase utilitaria `UiUtils.java`, invocando `EdgeToEdge.enable(activity)` y gestionando `WindowInsetsCompat` para `systemBars()`, `displayCutout()` e `ime()`.
   - Barras del sistema transparentes (`@android:color/transparent`) en `themes.xml` y `values-night/themes.xml`.
3. **Grillas Adaptativas**:
   - Utilizar `UiUtils.createAdaptiveLayoutManager(context, 340)` en todos los `RecyclerView` para calcular automáticamente el número de columnas (`spanCount`) en base al ancho en `dp`.
   - Añadir márgenes horizontales de `4dp` en tarjetas para presentación simétrica en múltiples columnas.
4. **Ergonomía Táctil y Accesibilidad**:
   - Garantizar que todos los elementos clicables cumplan con el mínimo de `48dp x 48dp`.
   - Envolver todos los diálogos con inputs en `NestedScrollView` con `android:fadeScrollbars="false"`.
   - Scroll flags en toolbars (`scroll|enterAlways|snap`) para ganar espacio vertical en scroll.
5. **Preservación del Ciclo de Vida**:
   - Hacer que `SaleDetail` implemente `Serializable` y gestionar `onSaveInstanceState` / `onRestoreInstanceState` en `NewSaleActivity`.

---

## 3. Plan de Olas y Bloques de Trabajo

Para maximizar la eficiencia y reducir el consumo de tokens del orquestador, el desarrollo se dividió en **4 bloques secuenciales delegados a subagentes** (`Model: "flash"`):

```text
Rama: redesign-interface

[Bloque 1: Infraestructura]
   ├── Drawables vectoriales (ic_delete, ic_search, ic_calendar)
   ├── Temas transparentes (values/themes.xml, values-night/themes.xml)
   └── UiUtils.java (Edge-to-Edge + createAdaptiveLayoutManager)
           │
           ▼
[Bloque 2: Dashboard, Compras e Historial]
   ├── MainActivity (Dashboard insets y layout)
   ├── BoxPurchasesActivity + dialog_add_box + item_box_purchase
   └── SalesHistoryActivity + activity_sales_history + item_sale
           │
           ▼
[Bloque 3: Clientes, Detalle y Deudas]
   ├── ClientsActivity + dialog_add_edit_client + item_client
   ├── ClientDetailActivity + activity_client_detail
   └── DebtLedgerActivity + dialog_register_payment + item_debt_client
           │
           ▼
[Bloque 4: Nueva Venta y Preservación de Estado]
   └── NewSaleActivity + activity_new_sale + item_sale_cart (onSaveInstanceState)
```

---

## 4. Estrategia de Verificación y Criterios de Éxito

- Cada bloque debe culminar con commits atómicos específicos según la convención `commits-atomicos`.
- Tras cada bloque, el orquestador valida `git status`, `git log` y corre `.\gradlew.bat test`.
- Al culminar el Bloque 4, se ejecuta la compilación y empaquetado completo:
  ```powershell
  .\gradlew.bat test assembleDebug
  ```
  El resultado debe ser 100% exitoso sin advertencias de layout o runtime.
