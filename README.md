# Cesta de Compra

Aplicación Android nativa para gestionar **listas de la compra compartidas** en tiempo real. Cada usuario entra con su cuenta de Google, crea listas con categorías y productos, y puede invitar a otras personas a editarlas.

Proyecto del **DevChallenge2** del módulo Proyecto Intermodular (CFGS DAM, INS Baix Camp).

---

## Descarga y prueba rápida

| | |
|---|---|
| **APK** | [Descargar app-debug.apk](https://drive.google.com/file/d/1HKDPfbXuw9zs3lOic3gCsZksuBna_Xn_/view?usp=sharing) |
| **Acceso** | Cualquier cuenta de Google |
| **Requisitos** | Android 7.0 (API 24) o superior |

1. Descarga el APK en el móvil.
2. Permite *"Instalar apps de orígenes desconocidos"* cuando Android lo pida. Si Play Protect avisa de que la app no está verificada, pulsa **Instalar de todos modos**.
3. Abre la app e inicia sesión con Google.

> **Para probar la función de compartir** hacen falta **dos cuentas de Google**. La persona invitada debe haber iniciado sesión al menos una vez en la app.

---

## Capturas

| 1. Inicio de sesión | 2. Mis listas y compartidas | 3. Categorías de la lista |
| :---: | :---: | :---: |
| <img width="290" src="https://github.com/user-attachments/assets/a0792865-d998-4154-90e4-0bfe2aac4211" /> | <img width="307" src="https://github.com/user-attachments/assets/9e480491-7df7-4b24-a75e-07c527a889fa" /> | <img width="292" src="https://github.com/user-attachments/assets/62de1cf5-1384-4d34-b804-64c853d0f07d" /> |

| 4. Productos por categoría | 5. Bandeja de invitaciones | 6. Compartir lista |
| :---: | :---: | :---: |
| <img width="285" src="https://github.com/user-attachments/assets/446542b5-c702-4c71-8de6-b68254a77ddf" /> | <img width="222" src="https://github.com/user-attachments/assets/2182ec18-6ae4-4f40-914e-372009a0e2d0" /> | <img width="283" src="https://github.com/user-attachments/assets/e5204fd7-88c0-4908-85b3-e87db5a4ace3" /> |

---

## Requisitos del enunciado

| Requisito | Cómo se cumple |
|---|---|
| Lista personal y compartible, editable por otros | Cada lista tiene un propietario y un array `miembros`; todos los miembros pueden editarla |
| Un usuario con varias listas (propias y compartidas) | Pantalla con dos secciones: **Mis listas** y **Compartidas conmigo** |
| Categorías y productos: ver, crear, editar, eliminar | CRUD completo para cualquier miembro de la lista |
| Productos marcables como completados | Checkbox; el producto se tacha y **no se elimina** |
| Categoría y producto con nombre | Validación: no se puede guardar con el nombre vacío |
| Accesible para el profesorado | APK descargable (ver arriba) y código en este repositorio |
| Característica extra | Ver la sección siguiente |

### Extras sobre el enunciado
- **Sistema de etiquetas** en los productos (por ejemplo *Bonpreu-Esclat*, *Mercadona*).
- **Invitaciones con bandeja de entrada**: la lista no se añade sola, el destinatario la acepta o la rechaza.
- **Colaboradores frecuentes**: acceso rápido a personas con las que ya has compartido listas.
- **Categorías por defecto** al crear una lista, todas editables y eliminables.
- **Borrado en cascada** con confirmación previa: al eliminar una lista o una categoría se borran también sus productos.

---

## Funcionalidades

### Autenticación
- Inicio de sesión con Google mediante **Credential Manager** y Firebase Authentication.
- Cada usuario que inicia sesión queda registrado en la colección `usuarios` de Firestore.

### Listas
- Crear listas con nombre obligatorio.
- Al crear una lista se generan 6 categorías: *Frutas y verduras, Lácteos, Carne y pescado, Panadería, Limpieza, Otros*.
- Compartir (solo el propietario) y eliminar listas (solo el propietario).
- Cambios sincronizados en tiempo real entre todos los miembros.

### Categorías y productos
- Las categorías muestran un resumen de sus productos (pendientes primero, completados tachados).
- Desde cada categoría se abre la pantalla de productos para añadir, completar, etiquetar y eliminar.

### Invitaciones
- Al invitar a alguien por email, la lista **no** se añade automáticamente: llega a su bandeja de entrada.
- Se evitan invitaciones duplicadas, y no se puede invitar a quien ya es miembro ni a uno mismo.
- El destinatario puede aceptar (✓) o rechazar (✕). El remitente recibe un aviso con la respuesta.

---

## Modelo de datos (Firestore)

```text
listas/{listaId}
   nombre, propietario, propietarioNombre, miembros[]
   categorias/{categoriaId}
      nombre
      productos/{productoId}
         nombre, completado, etiqueta
solicitudes/{listaId_email}
   paraEmail, deEmail, deNombre, tipo, listaId, listaNombre, mensaje, fecha
usuarios/{...}
   datos del usuario registrado
```

- Las categorías y los productos cuelgan de la lista, así que heredan sus permisos.
- Las reglas de seguridad solo permiten leer y escribir una lista a los emails incluidos en `miembros`.
- Un invitado solo puede añadirse a una lista si existe una invitación dirigida a él.
- `solicitudes` guarda tanto invitaciones (`tipo: "lista"`) como avisos (`tipo: "notificacion"`).

---

## Tecnologías

- **Kotlin** y **Jetpack Compose** con Material 3
- **Cloud Firestore**: base de datos con listeners en tiempo real (`addSnapshotListener`)
- **Firebase Authentication** + **Credential Manager**
- Estado de la UI con `remember`, `mutableStateOf` y `DisposableEffect`

---

## Estructura del proyecto

```text
app/src/main/java/org/insbaixcamp/cestadecompra/
├── MainActivity.kt          # Punto de entrada y navegación entre pantallas
├── AuthHelper.kt            # Login con Google (Credential Manager)
├── model/
│   ├── Lista.kt
│   ├── Categoria.kt
│   ├── Producto.kt
│   ├── Solicitud.kt
│   └── Usuario.kt
└── ui/
    ├── LoginScreen.kt
    ├── ListasScreen.kt          # Mis listas y compartidas
    ├── ListaCompraScreen.kt     # Categorías de una lista
    ├── ProductosCategoria.kt    # Productos de una categoría
    ├── CompartirDialog.kt       # Enviar invitaciones
    └── SolicitudesDialog.kt     # Bandeja de invitaciones y avisos
```

---

## Compilar desde el código

Esta opción es solo para quien quiera compilar el proyecto. Para probar la app basta con el APK.

1. Clona el repositorio y ábrelo en **Android Studio**.
2. Genera el APK:
   ```bash
   ./gradlew assembleDebug
   ```
   Se crea en `app/build/outputs/apk/debug/app-debug.apk`.

> **Login con Google al compilar por tu cuenta**: el inicio de sesión solo funciona si la huella **SHA-1** de tu clave de depuración está registrada en el proyecto de Firebase. Si compilas tú mismo, el login fallará con el error `DEVELOPER_ERROR` o `No credentials available`. Para evitarlo, usa el APK ya compilado o conecta el proyecto a tu propio Firebase (`google-services.json`, SHA-1 y proveedor Google activado).
