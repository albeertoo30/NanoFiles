# NanoFiles — Sistema de Transferencia de Ficheros P2P Híbrido

NanoFiles es una aplicación de intercambio de archivos Peer-to-Peer (P2P) sustentada sobre una **topología híbrida**, inspirada en los modelos de arquitectura clásicos como BitTorrent o eMule. El sistema combina la eficiencia y rapidez del protocolo UDP para tareas de descubrimiento y censo, con la confiabilidad de conexiones TCP dedicadas para la transferencia masiva de datos binarios en crudo.

Desarrollado íntegramente en **Java 21** de forma nativa (sin frameworks externos), el proyecto ha sido diseñado bajo estrictos criterios de concurrencia, robustez y optimización de recursos de red.

---

## Arquitectura del Sistema

El ecosistema se divide en dos componentes independientes que cooperan de forma coordinada:

1. **Directory (Directorio Centralizado):** Actúa como un registro pasivo ("libreta de contactos"). Escucha en un puerto UDP fijo (6868), indexa en memoria los metadatos de los ficheros disponibles globales y mantiene un censo en tiempo real de los servidores activos.
2. **NanoFiles (Nodo/Peer):** Actúa con un rol dual (cliente/servidor). Se comunica por UDP con el directorio para publicar su catálogo o buscar archivos, y levanta un servidor multihilo TCP para despachar transferencias directas a otros nodos.
```text
                ┌─────────────────────────────┐
                │          Directory          │
                │  (UDP · Puerto 6868 fijo)   │
                │                             │
                │  registeredPeers: Map<      │
                │    nickname → IP:puertoTCP> │
                │  directoryFiles: FileInfo[] │
                └───────────┬─────────────────┘
                            │  UDP (Texto ASCII)
          ┌─────────────────┼──────────────────┐
          │                 │                  │
     ┌────▼────┐       ┌────▼────┐       ┌────▼────┐
     │ Peer A  │       │ Peer B  │       │ Peer C  │
     │NFServer │◄──TCP─┤NFConnec.│       │         │
     │(efímero)│       │(cliente)│       │         │
     └─────────┘       └─────────┘       └─────────┘
```
---

## Características Principales & Decisiones de Diseño

### 1. Transferencia masiva mediante *Streaming* (TCP)
* **Optimización de RAM:** Se descartó por completo la carga integral de ficheros en variables de memoria (arrays de bytes) antes del envío. 
* **Buffer Constante:** Tanto el servidor como el cliente implementan un flujo continuo (*streaming*) procesado en bloques fijos de **8 KB**, garantizando un consumo de RAM estrictamente constante e independiente de si el fichero pesa unos bytes o varios Gigabytes (`OutOfMemoryError` mitigado).
* **Control de Límites:** El cliente calcula matemáticamente los bytes restantes por lectura (`Math.min`) basándose en el tamaño de cabecera esperado, eludiendo bloqueos por buffers residuales.

### 2. Arquitectura Concurrente *Lock-Free*
* **Modelo Multihilo:** El servidor de ficheros local delega cada socket entrante aceptado por el hilo principal (`ServerSocket.accept()`) a un hilo trabajador secundario (`NFServerThread`), permitiendo descargas simultáneas de múltiples clientes.
* **Rendimiento Máximo:** Dado que el acceso a la base de datos de ficheros locales (`NanoFiles.db`) se realiza exclusivamente en modo de solo lectura durante la fase de red, se prescinde de bloques `synchronized`, implementando un diseño libre de bloqueos (*lock-free*) que erradica los interbloqueos (*deadlocks*).
* **Mitigación de Hilos Zombis:** Las conexiones de sockets aplican un *timeout* estricto de 3 segundos (`socket.setSoTimeout(3000)`) para capturar excepciones y liberar de inmediato recursos ante caídas abruptas de la red.

### 3. Mecanismos Avanzados en Capa de Aplicación (Funcionalidades Optativas)
* **Paginación UDP Guiada por el Cliente (`dirfiles` ampliado):** Para sortear el límite físico del datagrama IP (`PACKET_MAX_SIZE`), el servidor trocea de forma *stateless* el catálogo global en fragmentos dinámicos. El cliente, mediante un bucle iterativo controlado por *timeouts*, reconstruye la lista completa de manera tolerante a fallos.
* **Descarga Directa Cifrada (`dirdl` básico):** Implementación de descargas directas desde el Directorio centralizado sobre UDP. Los ficheros binarios se codifican dinámicamente en **Base64** para encapsularlos limpiamente dentro de los campos de texto del protocolo sin corromper sus delimitadores ASCII.
* **Desconexión Elegante (`quit`):** Al detenerse, el nodo cierra sus hilos TCP de forma segura y notifica síncronamente su baja por UDP (`unregister`), purgando el censo del Directorio de inmediato para prevenir llamadas "fantasma" de otros nodos.
* **Resolución de Colisiones de Nicknames:** Si dos nodos se registran simultáneamente con el mismo identificador, el Directorio calcula variaciones numéricas proactivas y las notifica al nodo, el cual actualiza su estado interno de forma transparente para el usuario.
* **Parseo Seguro de Metadatos:** En la capa TCP, los listados se tokenizan limitando las particiones de cadena (`split(":", 3)`), inmunizando al protocolo contra fallos si los nombres de los archivos contienen caracteres especiales conflictivos.

---

## Requisitos e Instalación

* **Entorno de ejecución:** Java Runtime Environment (JRE) v21 o superior instalado y configurado en las variables de entorno de la terminal.
* **Compilación (Opcional):** El proyecto incluye una estructura limpia lista para ser importada en entornos Eclipse IDE.

## Guía de Despliegue en Red Real

Para evaluar la aplicación simulando un entorno de red real distribuyendo los servicios en diferentes estaciones físicas (p.ej. `HOST 1` y `HOST 2`), siga estos comandos de ejecución:

### Fase 1: Despliegue del Servidor Central (HOST 1)
Localice la dirección IP asignada a la interfaz de red Ethernet del Host 1 (p.ej. `155.54.10.20`). Inicie el proceso de escucha del Directorio:

```bash
java -jar Directory.jar
```

### Fase 2: Ejecución de Nodos/PeersInstancia 1 (Ejecutada en el HOST 1):
Abra una nueva terminal apuntando a su respectivo directorio de recursos compartidos (nf-shared1) y declare explícitamente la IP del Directorio:
```bash
java -jar NanoFiles.jar nf-shared1 155.54.10.20
```

### Instancia 2 e Instancia 3 (Concurrentes en el HOST 2):
La aplicación hace uso de puertos efímeros (new ServerSocket(0)), delegando en el Sistema Operativo la asignación de puertos libres al azar. Esto permite levantar múltiples procesos clientes/servidores de forma concurrente en una misma máquina local sin conflictos de binding de red:
```bash
# Terminal para la Instancia 2 (usa el directorio de intercambio 2)
java -jar NanoFiles.jar nf-shared2 155.54.10.20

# Terminal para la Instancia 3 (usa el directorio de intercambio 3)
java -jar NanoFiles.jar nf-shared3 155.54.10.20
```

## Diccionario de Comandos del Shell
Una vez dentro de la consola interactiva de NanoFiles, puede interactuar mediante el siguiente vocabulario:
| Comando | Tipo de Red | Descripción |
|---|---|---|
| ping | UDP | Evalúa la disponibilidad y compatibilidad con el servidor de Directorio. |
| serve | UDP/TCP | Instancia el servidor TCP local en un puerto aleatorio y registra el nodo en el censo del Directorio. |
| dirfiles | UDP | Solicita y lista el catálogo global consolidado de archivos paginados en la red. |
| peers | UDP | Descarga el mapa de nodos servidores activos mapeados por IP y Puerto. |
| dirdl [hash] | UDP | Descarga un archivo directamente del Directorio (decodificado en Base64). |
| peerfiles [nick] | TCP | Conecta a un par y lista de manera segura su catálogo local de ficheros compartidos. |
| peerdl [nick] [hash] | TCP | Descarga un archivo por streaming directo desde el nodo especificado. |
| quit | UDP/TCP | Realiza una desconexión limpia liberando sockets y actualizando el censo del Directorio. |
