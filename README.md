# NanoFiles

## Proyecto de prácticas perteneciente a la asignatura de Redes de Comunicaciones. 

Se trata de un sistema de compartición y transferencia de ficheros. Está formado por un servidor de directorio (programa Directory) y un conjunto de peers o pares (programa NanoFiles), que se comunican entre sí de la siguiente forma:
- La comunicación entre cada peer de NanoFiles y el servidor de directorio se rige por el modelo cliente-servidor. Un peer actúa como cliente del directorio para consultar los ficheros que pueden ser descargados de otros peers, publicar los ficheros que quiere compartir con el resto de pares y obtener los servidores que comparten un determinado fichero.
- Por otro lado, el modelo de comunicación entre pares de NanoFiles es peer-to-peer (P2P). Cuando un peer actúa como cliente de otro peer servidor, el cliente puede consultar los ficheros disponibles en el servidor y descargar aquellos fragmentos (chunks) de un fichero determinado que solicite. De manera complementaria, un peer puede convertirse a petición del usuario en servidor de ficheros, de forma que escuche en un puerto determinado en espera de que otros peers se conecten para solicitarle fragmentos de los ficheros que está compartiendo.
