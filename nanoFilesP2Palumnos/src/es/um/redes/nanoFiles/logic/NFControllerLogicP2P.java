package es.um.redes.nanoFiles.logic;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.io.File;
import java.io.IOException;
import es.um.redes.nanoFiles.tcp.client.NFConnector;
import es.um.redes.nanoFiles.tcp.message.PeerMessage;
import es.um.redes.nanoFiles.tcp.message.PeerMessageOps;
import es.um.redes.nanoFiles.application.NanoFiles;



import es.um.redes.nanoFiles.tcp.server.NFServer;
import es.um.redes.nanoFiles.util.FileInfo;

public class NFControllerLogicP2P {
	// Servidor TCP local para compartir ficheros con otros peers
	private NFServer fileServer = null;



	protected NFControllerLogicP2P() {
	}

	/**
	 * Método para ejecutar un servidor de ficheros en segundo plano. Debe arrancar
	 * el servidor en un nuevo hilo creado a tal efecto.
	 * 
	 * @return Verdadero si se ha arrancado en un nuevo hilo con el servidor de
	 *         ficheros, y está a la escucha en un puerto, falso en caso contrario.
	 * 
	 */
	protected boolean startFileServer() {
		boolean serverRunning = false;
		/*
		 * Comprobar que no existe ya un objeto NFServer previamente creado, en cuyo
		 * caso el servidor ya está en marcha.
		 */
		if (fileServer != null) {
			System.err.println("File server is already running");
		} else {
			/*
			 * DONE: (Boletín Servidor TCP concurrente) Arrancar servidor en segundo plano
			 * creando un nuevo hilo, comprobar que el servidor está escuchando en un puerto
			 * válido (>0), imprimir mensaje informando sobre el puerto de escucha, y
			 * devolver verdadero. Las excepciones que puedan lanzarse deben ser capturadas
			 * y tratadas en este método. Si se produce una excepción de entrada/salida
			 * (error del que no es posible recuperarse), se debe informar sin abortar el
			 * programa
			 * 
			 */
			try {
				fileServer = new NFServer();
				fileServer.startServer();
				if(fileServer.getServerPort() > 0) {
					serverRunning = true;
				}
			} catch (IOException e) {
				System.err.println("Error. Could not initialize file server: " + e.getMessage());
				fileServer = null;
			}

		}
		return serverRunning;

	}

	protected void testTCPServer() {
		assert (NanoFiles.testModeTCP);
		/*
		 * Comprobar que no existe ya un objeto NFServer previamente creado, en cuyo
		 * caso el servidor ya está en marcha.
		 */
		assert (fileServer == null);
		try {

			fileServer = new NFServer();
			/*
			 * DONE (Boletín SocketsTCP) Inicialmente, se creará un NFServer y se ejecutará su
			 * método "test" (servidor minimalista en primer plano, que sólo puede atender a
			 * un cliente conectado). Posteriormente, se desactivará "testModeTCP" para
			 * implementar un servidor en segundo plano, que se ejecute en un hilo
			 * secundario para permitir que este hilo (principal) siga procesando comandos
			 * introducidos mediante el shell.
			 */
			fileServer.test();
			// Este código es inalcanzable: el método 'test' nunca retorna...
		} catch (IOException e1) {
			e1.printStackTrace();
			System.err.println("Cannot start the file server");
			fileServer = null;
		}
	}

	public void testTCPClient() {

		assert (NanoFiles.testModeTCP);
		/*
		 * DONE (Boletín SocketsTCP) Inicialmente, se creará un NFConnector (cliente TCP)
		 * para conectarse a un servidor que esté escuchando en la misma máquina y un
		 * puerto fijo. Después, se ejecutará el método "test" para comprobar la
		 * comunicación mediante el socket TCP. Posteriormente, se desactivará
		 * "testModeTCP" para implementar la descarga de un fichero desde múltiples
		 * servidores.
		 */

		try {
			NFConnector nfConnector = new NFConnector(new InetSocketAddress(NFServer.PORT));
			nfConnector.test();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Método para listar los ficheros de un peer concreto vía TCP e imprimirlos por
	 * pantalla.
	 * 
	 * @param La dirección del peer cuyos ficheros se quiere listar
	 * @return Verdadero si se ha obtenido exitosamente el listado de fichero del
	 *         peer
	 */
	protected boolean listPeerFiles(InetSocketAddress peerAddr) {
		boolean success = false;
		try {
			System.out.println("*Trying to connect with peer " + peerAddr);
			NFConnector connector = new NFConnector(peerAddr);
			
			PeerMessage response = connector.requestPeerFiles();
			if(response.getOpcode() == PeerMessageOps.OPCODE_PEERFILES_RESP) {
				System.out.println("Files shared from the peer: ");
				String fileListStr = response.getFileList();
				
				if (fileListStr != null && !fileListStr.trim().isEmpty()) {
					String[] lineas = fileListStr.split("\n");
					ArrayList<FileInfo> arrayFicheros = new ArrayList<>();
					
					for (String linea : lineas) {
						if (linea.contains(":")) {
							String[] partes = linea.split(":", 3); 
							if (partes.length >= 2) {
								FileInfo fi = new FileInfo();
								fi.fileHash = partes[0].trim();
								fi.fileName = partes[1].trim();
								if (partes.length == 3) {
									try {
										fi.fileSize = Long.parseLong(partes[2].trim());
									} catch (NumberFormatException e) {
										fi.fileSize = -1; // Por si acaso hay un fallo al parsear el número
									}
								} else {
									fi.fileSize = -1;
								}
								arrayFicheros.add(fi);
							}
						}
					}
					
					/* Convertimos a array y usamos el método printToSysout de FileInfo para imprimir con el 
					mismo formato que dirfiles o myfiles */
					FileInfo[] ficherosParaImprimir = arrayFicheros.toArray(new FileInfo[0]);
					FileInfo.printToSysout(ficherosParaImprimir);	
				} else {
					System.out.println(" (This peer is not sharing any files yet)");
				}
				success = true;
			}else {
				System.err.println("Unexpected response code from the peer.");
			}
			
			connector.close();
		} catch (IOException e) {
			System.err.println("Communication or conexion error with the peer: " + e.getMessage());
		}

		return success;
	}

	/**
	 * Descarga un fichero identificado por subcadena de hash desde uno o varios
	 * peers. Si se pasa "*" como nickname, usa el directorio para localizar los
	 * peers que tienen el hash.
	 */
	protected boolean downloadFromPeers(NFControllerLogicDir dirLogic, String targetPeerNickname,
			String targetHashSubstring) {
		// DONE: localizar peers con el hash solicitado (o uno concreto) y delegar en
		// downloadFileFromServers
		boolean success = false;

		InetSocketAddress peerAddr = dirLogic.getPeerAddress(targetPeerNickname);
		if(peerAddr != null) {
			InetSocketAddress[] servers = { peerAddr };
			success = downloadFileFromServers(servers, targetHashSubstring);
		}else {
			System.err.println("*Could not resolve peer direction: " + targetPeerNickname);
		}

		return success;
	}

	/**
	 * Método para descargar un fichero del peer servidor de ficheros
	 * 
	 * @param serverAddressList   La lista de direcciones de los servidores a los
	 *                            que se conectará
	 * @param targetHashSubstring Subcadena del hash del fichero a descargar
	 */
	protected boolean downloadFileFromServers(InetSocketAddress[] serverAddressList, String targetHashSubstring) {
		boolean downloaded = false;

		if (serverAddressList.length == 0) {
			System.err.println("* Cannot start download - No list of server addresses provided");
			return false;
		}
		// DONE: crear conectores TCP solo a los servidores que confirmen el hash
		// pedido, obtener nombre remoto, reservar nombre local sin colisiones, alternar
		// descarga de chunks y verificar hash final. Cerrar los sockets al terminar.
		for(InetSocketAddress serverAddr : serverAddressList) {
			try {
				NFConnector connector = new NFConnector(serverAddr);
				PeerMessage response = connector.requestDownload(targetHashSubstring);
				if(response.getOpcode() == PeerMessageOps.OPCODE_DOWNLOAD_RESP) {
					String fullHashAndName = response.getHash();
					long expectedSize = response.getFileSize();
					
					String hash = "";
					// Intentamos obtener el nombre original. Si el servidor no lo manda, usamos un genérico.
					String originalName = "descarga_desconocida.bin";
					if(fullHashAndName.contains(":")) {
						String[] parts = fullHashAndName.split(":", 2);
						hash = parts[0];
						originalName = parts[1];
					}
					// Separamos el nombre de la extensión para meter el _copiaX
					String nameSinExtension = originalName;
					String extension = "";
					int dotIndex = originalName.lastIndexOf('.');
					if(dotIndex > 0) {
						nameSinExtension = originalName.substring(0, dotIndex);
						extension = originalName.substring(dotIndex);
					}
					
					File downloadFolder = new File(NanoFiles.sharedDirname);
					String localFileName = originalName;
					File localFile = new File(downloadFolder, localFileName);
					
					int index = 1;		
					while(localFile.exists()) {
						localFileName = nameSinExtension + "_copia" + index + extension;
						localFile = new File(downloadFolder, localFileName);
						index++;
					}
					
					System.out.println("*Downloading file (Expected size: " + expectedSize + " bytes)...");
					connector.downloadFileStreaming(localFile, expectedSize);
					System.out.println("*Download completed successfully in: " + toDisplayPath(localFile.toPath()));
					
					downloaded = true;
					connector.close();
					break;
				}else if(response.getOpcode() == PeerMessageOps.OPCODE_FILE_NOT_FOUND) {
					System.err.println("The peer does not contain any file matching with " + targetHashSubstring);
				}else if(response.getOpcode() == PeerMessageOps.OPCODE_AMBIGUOUS_HASH) {
					System.err.println("The peer contains several files matching with the substring");
				}
				connector.close();
			} catch (IOException e) {
				System.err.println("* Error trying to download from server " + serverAddr + ": " + e.getMessage());
			}

		}

		return downloaded;
	}

	private String toDisplayPath(java.nio.file.Path path) {
		java.nio.file.Path abs = path.toAbsolutePath().normalize();
		java.nio.file.Path cwd = java.nio.file.Paths.get("").toAbsolutePath().normalize();
		if (abs.startsWith(cwd)) {
			return cwd.relativize(abs).toString();
		}
		return path.toString();
	}

	/**
	 * Método para obtener el puerto de escucha de nuestro servidor de ficheros
	 * 
	 * @return El puerto en el que escucha el servidor, o 0 en caso de error.
	 */
	protected int getServerPort() {
		int port = 0;
		/*
		 * DONE: Devolver el puerto de escucha de nuestro servidor de ficheros
		 */
		if(fileServer != null) {
			port = fileServer.getServerPort();
		}

		return port;
	}

	/**
	 * Método para detener nuestro servidor de ficheros en segundo plano
	 * 
	 */
	protected void stopFileServer() {
		/*
		 * DONE: Enviar señal para detener nuestro servidor de ficheros en segundo plano
		 */
		if(fileServer != null) {
			fileServer.stopServer();
			fileServer = null;
			System.out.println("Local file server stopped.");
		}
	}

	protected boolean serving() {
		boolean result = false;
		
			if(fileServer != null) {
				result = true;
			}

		return result;
	}

}
