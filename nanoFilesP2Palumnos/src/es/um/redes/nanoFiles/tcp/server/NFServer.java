package es.um.redes.nanoFiles.tcp.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import es.um.redes.nanoFiles.application.NanoFiles;
import es.um.redes.nanoFiles.tcp.message.PeerMessage;
import es.um.redes.nanoFiles.tcp.message.PeerMessageOps;
import es.um.redes.nanoFiles.util.FileInfo;


public class NFServer implements Runnable {

	public static final int PORT = 10000;


	private ServerSocket serverSocket = null;
	
	// Bandera para poder apagar el hilo principal
	private boolean stopServer = false;

	public NFServer() throws IOException {
		/*
		 * DONE: (Boletín SocketsTCP) Crear una direción de socket a partir del puerto
		 * especificado (PORT)
		 */
		InetSocketAddress socketAddress = new InetSocketAddress(PORT);
	
		/*
		 * DONE: (Boletín SocketsTCP) Crear un socket servidor y ligarlo a la dirección
		 * de socket anterior
		 */
		this.serverSocket = new ServerSocket();
		this.serverSocket.bind(socketAddress);
	}

	/*
	 * DONE: (Boletín SocketsTCP) Añadir métodos a esta clase para: 1) Arrancar el
	 * servidor en un hilo nuevo que se ejecutará en segundo plano 2) Detener el
	 * servidor (stopserver) 3) Obtener el puerto de escucha del servidor etc.
	 */
	
	// Método para arrancar el servidor en un hilo nuevo que se ejecutará en segundo plano
	public void startServer() {
		Thread nuevoHilo = new Thread(this);
		nuevoHilo.start();
		System.out.println("*Servidor TCP iniciado en segundo plano en el puerto " + PORT);
	}
	
	// Método para detener el servidor 
	public void stopServer() {
		this.stopServer = true;
		try {
			if(serverSocket != null && !serverSocket.isClosed()) {
				serverSocket.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// Método para obtener el puerto de escucha del servidor
	public int getServerPort() {
		return serverSocket.getLocalPort();
	}
	
	/**
	 * Método para ejecutar el servidor de ficheros en primer plano. Sólo es capaz
	 * de atender una conexión de un cliente. Una vez se lanza, ya no es posible
	 * interactuar con la aplicación.
	 * 
	 */
	public void test() {
		if (serverSocket == null || !serverSocket.isBound()) {
			System.err.println(
					"[fileServerTestMode] Failed to run file server, server socket is null or not bound to any port");
			return;
		} else {
			System.out
					.println("[fileServerTestMode] NFServer running on " + serverSocket.getLocalSocketAddress() + ".");
		}

		while (true) {
			/*
			 * DONE: (Boletín SocketsTCP) Usar el socket servidor para esperar conexiones de
			 * otros peers que soliciten descargar ficheros.
			 */
			try {
				Socket clientSocket = serverSocket.accept();
				
				/*
				 * DONE: (Boletín SocketsTCP) Tras aceptar la conexión con un peer cliente, la
				 * comunicación con dicho cliente para servir los ficheros solicitados se debe
				 * implementar en el método serveFilesToClient, al cual hay que pasarle el
				 * socket devuelto por accept.
				 */
				serveFilesToClient(clientSocket);
			} catch (IOException e) {
				System.err.println("[fileServerTestMode] Error aceptando conexión: " + e.getMessage());
			}
			
		}
	}

	/**
	 * Método que ejecuta el hilo principal del servidor en segundo plano, esperando
	 * conexiones de clientes.
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		while(!stopServer) {
			try {
				/*
				 * DONE: (Boletín SocketsTCP) Usar el socket servidor para esperar conexiones de
				 * otros peers que soliciten descargar ficheros
				 */
				Socket clientSocket = serverSocket.accept();
				
				/*
				 * DONE: (Boletín TCPConcurrente) Crear un hilo nuevo de la clase
				 * NFServerThread, que llevará a cabo la comunicación con el cliente que se
				 * acaba de conectar, mientras este hilo vuelve a quedar a la escucha de
				 * conexiones de nuevos clientes (para soportar múltiples clientes). Si este
				 * hilo es el que se encarga de atender al cliente conectado, no podremos tener
				 * más de un cliente conectado a este servidor.
				 */
				/*
				 * DONE: (Boletín SocketsTCP) Al establecerse la conexión con un peer, la
				 * comunicación con dicho cliente se hace en el método
				 * serveFilesToClient(socket), al cual hay que pasarle el socket devuelto por
				 * accept
				 */
				NFServerThread clientThread = new NFServerThread(clientSocket);
				clientThread.start();
			} catch (IOException e) {
				if(!stopServer) {
					System.err.println("Error accepting conexion from client: " + e.getMessage());
				}
			}
			

		}
		
	}
	

	/**
	 * Método de clase que implementa el extremo del servidor del protocolo de
	 * transferencia de ficheros entre pares.
	 * 
	 * @param socket El socket para la comunicación con un cliente que desea
	 *               descargar ficheros.
	 */
	public static void serveFilesToClient(Socket socket) {
		try {
			// Decisión de diseño: timeout de 3 seg para evitar bloqueos
			socket.setSoTimeout(3000);
			
			/*
			 * DONE: (Boletín SocketsTCP) Crear dis/dos a partir del socket
			 */
			DataInputStream dis = new DataInputStream(socket.getInputStream());
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
			
			/*
			 * DONE: (Boletín SocketsTCP) Mientras el cliente esté conectado, leer mensajes
			 * de socket, convertirlo a un objeto PeerMessage y luego actuar en función del
			 * tipo de mensaje recibido, enviando los correspondientes mensajes de
			 * respuesta.
			 */
			while(!socket.isClosed()) {
				try {
					PeerMessage request = PeerMessage.readMessageFromInputStream(dis);
					byte opcode = request.getOpcode();
					if(opcode == PeerMessageOps.OPCODE_PEERFILES_REQ) {
						FileInfo[] files = NanoFiles.db.getFiles();
						StringBuilder sb = new StringBuilder();
						for(FileInfo f : files) {
							sb.append(f.fileHash).append(":").append(f.fileName).append("\n");
						}
						PeerMessage response = new PeerMessage(PeerMessageOps.OPCODE_PEERFILES_RESP);
						response.setFileList(sb.toString());
						response.writeMessageToOutputStream(dos);
					}else if(opcode == PeerMessageOps.OPCODE_DOWNLOAD_REQ) {
						/*
						 * DONE: (Boletín SocketsTCP) Para servir un fichero, hay que localizarlo a
						 * partir de su hash (o subcadena) en nuestra base de datos de ficheros
						 * compartidos. Los ficheros compartidos se pueden obtener con
						 * NanoFiles.db.getFiles(). Los métodos lookupHashSubstring y
						 * lookupFilenameSubstring de la clase FileInfo son útiles para buscar ficheros
						 * coincidentes con una subcadena dada del hash o del nombre del fichero. El
						 * método lookupFilePath() de FileDatabase devuelve la ruta al fichero a partir
						 * de su hash completo.
						 */
						String targetHashSubstring = request.getHash();
						FileInfo[] files = NanoFiles.db.getFiles();
						List<FileInfo> matches = new ArrayList<>();
						// Buscamos coindidencias con la subcadena
						for(FileInfo f : files) {
							// Pasamos a minúsculas para poder hacer la comparación bien
							String hashLowerCase = f.fileHash.toLowerCase();
							String targetHashSubstringLower = targetHashSubstring.toLowerCase();
							if(hashLowerCase.contains(targetHashSubstringLower)) {
								matches.add(f);
							}
						}
							
						if(matches.isEmpty()) { // Sin coincidencias
							PeerMessage fileNotFound = new PeerMessage(PeerMessageOps.OPCODE_FILE_NOT_FOUND);
							fileNotFound.writeMessageToOutputStream(dos);
						}else if(matches.size() > 1) { // Varias coincidencias
							PeerMessage ambiguousHash = new PeerMessage(PeerMessageOps.OPCODE_AMBIGUOUS_HASH);
							ambiguousHash.writeMessageToOutputStream(dos);
						}else { // Una única coincidencia
							FileInfo targetFile = matches.get(0);
							String filePath = NanoFiles.db.lookupFilePath(targetFile.fileHash);
							File file = new File(filePath);
								
							// Enviamos primero la cabecera (metadatos)
							PeerMessage response = new PeerMessage(PeerMessageOps.OPCODE_DOWNLOAD_RESP);
							response.setHash(targetFile.fileHash);
							response.setFileSize(file.length());
							response.writeMessageToOutputStream(dos);
							
							// Enviamos el fichero en crudo
							FileInputStream fis = new FileInputStream(file);
							byte[] buffer = new byte[8092];
							int bytesRead = fis.read(buffer);
							while(bytesRead > 0) {
								dos.write(buffer, 0, bytesRead);
							}
							fis.close();
							dos.flush();
						}
					}else {
						System.err.println("Opcode TCP unexpected from client.");
						break;
					}
				}catch (EOFException e) {
					break;
				}catch (SocketTimeoutException e) {
					System.err.println("Timeout. Client did not send more requests.");
					break;
				}
			}
			
		} catch (IOException e) {
			System.err.println("ErrorTCP. I/O error in communication with client: " + e.getMessage());
		} finally {
			try {
				if(socket != null && !socket.isClosed()) {
					socket.close();
				}
			} catch (IOException e2) {
				e2.printStackTrace();
			}
		}
		
	}


}
