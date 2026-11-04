# Plan: Etapa 1 - Inicialización del Repositorio Git y Commits Base

## 1. Contexto y Estado Inicial

El proyecto fue creado como una aplicación Android nativa en Java + SQLite dentro de Android Studio, pero no contaba con control de versiones Git inicializado. En el espacio de trabajo existían archivos generados por el entorno local de desarrollo (cachés de Android Studio, descriptores de dispositivos de prueba en `.idea/`) junto con el código fuente y especificaciones de negocio sin clasificar.

- **Directorio de trabajo**: `C:\Users\MIGUEL ANGEL\AndroidStudioProjects\GestionResmas`
- **Rama objetivo**: `main`

---

## 2. Objetivos de la Etapa

1. **Inicialización de Git**:
   Configurar el repositorio Git local en la rama principal (`main`).
2. **Saneamiento de `.gitignore`**:
   Configurar reglas rigurosas para excluir artefactos de compilación (`.gradle`, `build/`, `local.properties`) y archivos efímeros propios de Android Studio (`/.idea/deploymentTargetSelector.xml`, `/.idea/deviceManager.xml`).
3. **Estructuración en Commits Atómicos**:
   Dividir los archivos preexistentes en commits lógicos, independientes y atómicos siguiendo la convención de `commits-atomicos`:
   - Configuración e ignorados en un commit `chore:`.
   - Código fuente de la aplicación, layouts y assets en un commit `feat:`.
   - Documentación de requerimientos y reglas de negocio en un commit `docs:`.
4. **Verificación de Compilación y Tests**:
   Asegurar que el proyecto compile de forma limpia y pase las pruebas unitarias existentes.

---

## 3. Desglose de Commits Planificados

| Commit Planificado | Tipo | Alcance | Archivos Involucrados |
|---|---|---|---|
| 1 | `chore` | `chore: ignorar artefactos de compilación y archivos locales de Android Studio` | `.gitignore`, archivos de configuración base |
| 2 | `feat` | `feat: aplicación de gestión de ventas e inventario de resmas` | Código Java (`models/`, `database/`, `ui/`), layouts XML, drawables, gradle wrappers y scripts de compilación |
| 3 | `docs` | `docs: especificación y reglas de negocio del sistema de resmas` | `SKILL.md` con las fórmulas de cálculo de márgenes, tablas de base de datos y flujos |

---

## 4. Criterios de Aceptación y Verificación

- El comando `git status` debe quedar limpio tras la creación de los commits.
- El historial de `git log --oneline` debe reflejar la secuencia cronológica ordenada (`chore` $\rightarrow$ `feat` $\rightarrow$ `docs`).
- La ejecución de `.\gradlew.bat test` debe completarse con éxito (`BUILD SUCCESSFUL`).
