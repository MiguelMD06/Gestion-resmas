# Tarea: Bloque 4 - Nueva Venta y Preservación de Estado

## 1. Identificación y Metadatos
- **Etapa**: `2-rediseno-interfaz`
- **Bloque**: `4-nueva-venta-preservacion-estado`
- **Subagente Ejecutor ID**: `8e2d3eb6-333f-47bc-8196-07bd6baef058`
- **Modelo utilizado**: `Gemini 3.8 Flash`
- **Estado**: Completada exitosamente

---

## 2. Objetivo del Bloque
Modernizar y asegurar la robustez de la pantalla más crítica de la aplicación: el flujo de creación de una nueva venta (`NewSaleActivity`):
1. Incorporar soporte de Edge-to-Edge mediante `UiUtils.setupEdgeToEdge`, protegiendo el formulario contra recortes superiores e inferiores.
2. Hacer adaptativa la lista de ítems agregados al carrito de venta mediante `createAdaptiveLayoutManager`.
3. Ajustar todos los botones de acción (`Agregar al Carrito`, `Confirmar Venta`, selectores de tipo de pago) a una altura mínima accesible de `48dp`.
4. **Preservación crítica de ciclo de vida**: Implementar `onSaveInstanceState` y la lógica de restauración en `onCreate` / `onRestoreInstanceState` para que, ante giros de pantalla, cambios de configuración o minimizado del sistema, la lista de `SaleDetail` en el carrito, el cliente seleccionado y el tipo de pago se conserven intactos.

---

## 3. Archivos Modificados y Creados

- `app/src/main/java/com/example/gestionresmas/ui/sales/NewSaleActivity.java`
- `app/src/main/res/layout/activity_new_sale.xml`
- `app/src/main/res/layout/item_sale_cart.xml`

---

## 4. Commits Generados

Siguiendo la convención de `commits-atomicos`:
10. `dd00b3a` — `feat: interfaz adaptativa y preservación de estado en nueva venta`

---

## 5. Verificación
- Prueba de serialización y restauración en `Bundle` de los ítems `SaleDetail`.
- Compilación final y verificación de build sin advertencias:
  ```powershell
  .\gradlew.bat test assembleDebug
  ```
  `BUILD SUCCESSFUL` con 40 tareas ejecutadas.
