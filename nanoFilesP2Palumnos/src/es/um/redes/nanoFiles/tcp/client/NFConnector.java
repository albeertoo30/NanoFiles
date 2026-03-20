package es.um.redes.nanoFiles.tcp.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import es.um.redes.nanoFiles.tcp.message.PeerMessage;
import es.um.redes.nanoFiles.tcp.message.PeerMessageOps;
import es.um.redes.nanoFiles.util.FileInfo;

//Esta clase proporciona la funcionalidad necesaria para intercambiar mensajes entre el cliente y el servidor
public class NFConnector {
	private Socket socket;
	private InetSocketAddress serverAddr;

	private DataInputStream dis;
	private DataOutputStream dos;

	public NFConnector(InetSocketAddress fserverAddr) throws UnknownHostException, IOException {
		serverAddr = fserverAddr;
		/*
		 * DONE: (Boletín SocketsTCP) Se crea el socket a partir de la dirección del
		 * servidor (IP, puerto). La creación exitosa del socket significa que la
		 * conexión TCP ha sido establecida.
		 */
		this.socket = new Socket(serverAddr.getAddress(), serverAddr.getPort());
		
		/*
		 * DONE: (Boletín SocketsTCP) Se crean los DataInputStream/DataOutputStream a
		 * partir de los streams de entrada/salida del socket creado. Se usarán para
		 * enviar (dos) y recibir (dis) datos del servidor.
		 */
		this.dis = new DataInputStream(socket.getInputStream());
		this.dos = new DataOutputStream(socket.getOutputStream());
	}

	public void test() {
		/*
		 * DONE: (Boletín SocketsTCP) Enviar entero cualquiera a través del socket y
		 * después recibir otro entero, comprobando que se trata del mismo valor.
		 */
		try {
			int numEnviado = 12345;
			System.out.println("[testTCPClient] Enviando entero de prueba: " + numEnviado);
			dos.writeInt(numEnviado);
			dos.flush();
			
			int numRecibido = dis.readInt();
			System.out.println("[testTCPClient] Recibido entero de prueba: " + numRecibido);
			
			if (numEnviado == numRecibido) {
				System.out.println("[testTCPClient] Éxito! La comunicación TCP bidireccional funciona perfectamente.");
			} else {
				System.err.println("[testTCPClient] Fallo: los valores no coinciden.");
			}
		} catch (IOException e) {
			System.err.println("[testTCPClient] Error de E/S durante el test: " + e.getMessage());
		}
	}


	public InetSocketAddress getServerAddr() {
		return serverAddr;
	}

	// Método que solicita la lista de ficheros al servidor y devuelve el mensaje de respuesta
	public PeerMessage requestPeerFiles() throws IOException {
		PeerMessage request = new PeerMessage(PeerMessageOps.OPCODE_PEERFILES_REQ);
		request.writeMessageToOutputStream(dos);

		return PeerMessage.readMessageFromInputStream(dis);
	}
	
	// Método que solicita la descarga de un fichero enviando la subcadena de su hash.
	public PeerMessage requestDownload(String targetHashSubstring) throws IOException {
		PeerMessage request = new PeerMessage(PeerMessageOps.OPCODE_DOWNLOAD_REQ);
		request.setHash(targetHashSubstring);
		request.writeMessageToOutputStream(dos);

		return PeerMessage.readMessageFromInputStream(dis);
	}
	
	/* Método que realiza el "streaming" inverso. Lee los bytes del socket TCP y los va escribiendo
	directamente en disco duro. */
	public void downloadFileStreaming(File targetFile, long expectedSize) throws IOException {
		FileOutputStream fos = new FileOutputStream(targetFile);
		byte[] buffer = new byte[8192];
		long bytesReceived = 0;
		int bytesRead;

		while (bytesReceived < expectedSize) {
			// Ajustamos cuánto queremos leer para no pasarnos del tamaño del fichero al final
			int maxToRead = (int) Math.min(buffer.length, expectedSize - bytesReceived);
			bytesRead = dis.read(buffer, 0, maxToRead);

			if (bytesRead == -1) {
				fos.close();
				throw new IOException("Server closed connexion before finishing.");
			}

			fos.write(buffer, 0, bytesRead);
			bytesReceived += bytesRead;
		}
		
		fos.close();
	}
	
	// Método para cerrar la conexión de forma limpia.
	public void close() {
		try {
			if (socket != null && !socket.isClosed()) {
				socket.close();
			}
		} catch (IOException e) {
			System.err.println("* Error closing client socket: " + e.getMessage());
		}
	}
	
}
