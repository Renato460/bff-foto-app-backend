
# Backend de la App de Fotos para Boda

Este repositorio contiene el código fuente del backend para una aplicación de intercambio de fotos de boda. Ha sido desarrollado con **Java** y **Spring Boot**, siguiendo una arquitectura **BFF (Backend for Frontend)** para comunicarse de forma segura con **Supabase**.

## 🚀 Sobre el Proyecto

El objetivo de este backend es servir como el único punto de comunicación entre la aplicación cliente (desarrollada en Angular) y los servicios de Supabase (Base de Datos y Almacenamiento). Esta arquitectura asegura que las claves sensibles de Supabase, como la `SERVICE_ROLE_KEY`, nunca se expongan al cliente.

### Funcionalidades Principales

* **Autenticación segura**: Valida credenciales contra Supabase Auth y genera JWTs locales para la gestión de sesiones.
* **Gestión de roles**: Distingue entre usuarios `admin` (control total) y `guest` (ver y subir fotos).
* **Subida de fotos**: Permite a los usuarios autenticados subir imágenes, que se almacenan en Supabase Storage.
* **Visualización y borrado**: Lista todas las fotos y permite a los administradores borrarlas.
* **Descarga de álbum**: Permite a los administradores descargar un álbum completo de fotos en un archivo `.zip`.

-----

## 🛠️ Pila Tecnológica

* **Lenguaje**: Java 17+
* **Framework**: Spring Boot 3+
* **Seguridad**: Spring Security 6+
* **Autenticación**: JSON Web Tokens (JWT)
* **Plataforma de Datos**: Supabase
    * **Base de Datos**: Postgres
    * **Almacenamiento de Archivos**: Supabase Storage
* **Gestor de Dependencias**: Maven

-----

## ⚙️ Configuración del Entorno

Para ejecutar el proyecto localmente, sigue estos pasos:

### Prerrequisitos

* Tener instalado Java (JDK 17 o superior).
* Tener instalado Maven.
* Tener una cuenta de Supabase con un proyecto creado.
* Haber creado las tablas `profiles` y `photos` y un bucket público `photos` en Supabase Storage.

### Instalación

1.  **Clona el repositorio:**

    ```bash
    git clone https://github.com/tu-usuario/tu-repositorio.git
    cd tu-repositorio
    ```

2.  **Configura las variables de entorno:**
    Crea un archivo `application.properties` en `src/main/resources/` y añade las siguientes propiedades. **Nunca subas este archivo con valores reales a un repositorio público.**

    ```properties
    # URL de tu proyecto Supabase
    supabase.url=https://<ID_PROYECTO>.supabase.co

    # Clave anónima (pública) de Supabase
    supabase.anon.key=<TU_SUPABASE_ANON_KEY>

    # Clave de servicio (secreta) de Supabase
    supabase.service.key=<TU_SUPABASE_SERVICE_ROLE_KEY>

    # Clave secreta para firmar los JWTs locales (genera una clave segura)
    jwt.secret=MI_CLAVE_SECRETA_PARA_JWTs_DEBE_SER_MUY_LARGA_Y_SEGURA
    # 24 horas
    jwt.expiration.ms=86400000 
    ```

3.  **Ejecuta la aplicación:**
    Usa el wrapper de Maven para compilar y ejecutar el proyecto.

    ```bash
    ./mvnw spring-boot:run
    ```

    El servidor se iniciará en `http://localhost:8080`.

-----

## Endpoints de la API

A continuación se detallan todos los endpoints disponibles.

| Método | Ruta                      | Rol Requerido | Descripción                                                                    |
| :----- | :------------------------ | :------------ | :----------------------------------------------------------------------------- |
| `POST` | `/api/auth/login`         | Público       | Autentica a un usuario y devuelve un JWT local.                                |
| `GET`  | `/api/photos`             | `guest` o `admin` | Devuelve una lista con la información de todas las fotos.                          |
| `POST` | `/api/photos/upload`      | `guest` o `admin` | Sube un archivo de imagen.                                                     |
| `DELETE`| `/api/photos/{photoId}`   | `admin`         | Elimina una foto del Storage y de la base de datos.                            |
| `POST` | `/api/album/download`     | `admin`         | Descarga las fotos de las URLs especificadas en un archivo `.zip`.             |

### Ejemplos con `curl`

* **Login**

  ```bash
  curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "usuario@ejemplo.com", "password": "tu_contraseña"}'
  ```

* **Subir Foto** (Requiere token)

  ```bash
  curl -X POST http://localhost:8080/api/photos/upload \
  -H "Authorization: Bearer <TU_TOKEN_JWT>" \
  -F "file=@/ruta/a/tu/imagen.jpg"
  ```

* **Borrar Foto** (Requiere token de admin)

  ```bash
  curl -X DELETE http://localhost:8080/api/photos/123 \
  -H "Authorization: Bearer <TU_TOKEN_DE_ADMIN>"
  ```

* **Descargar Álbum** (Requiere token de admin)

  ```bash
  curl -X POST http://localhost:8080/api/album/download \
  -H "Authorization: Bearer <TU_TOKEN_DE_ADMIN>" \
  -H "Content-Type: application/json" \
  -d '{"urls": ["URL_FOTO_1", "URL_FOTO_2"]}' \
  --output album.zip
  ```