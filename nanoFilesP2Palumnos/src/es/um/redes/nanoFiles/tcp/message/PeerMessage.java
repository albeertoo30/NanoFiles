package es.um.redes.nanoFiles.tcp.message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import es.um.redes.nanoFiles.util.FileInfo;

public class PeerMessage {

	private byte opcode;

	/*
	 * DONE: (Boletín MensajesBinarios) Añadir atributos u otros constructores
	 * específicos para crear mensajes con otros campos, según sea necesario
	 * 
	 */
	private String fileList; // para PEERFILES_RESP
	private String hash;	// para DOWNLOAD_REQ y DOWNLOAD_RESP
	private long fileSize;	// para DOWNLOAD_RESP

	public PeerMessage() {
		opcode = PeerMessageOps.OPCODE_INVALID_CODE;
	}

	public PeerMessage(byte op) {
		opcode = op;
	}

	/*
	 * DONE: (Boletín MensajesBinarios) Crear métodos getter y setter para obtener
	 * los valores de los atributos de un mensaje. Se aconseja incluir código que
	 * compruebe que no se modifica/obtiene el valor de un campo (atributo) que no
	 * esté definido para el tipo de mensaje dado por "operation".
	 */
	public byte getOpcode() {
		return opcode;
	}

	public String getFileList() {
		if (opcode != PeerMessageOps.OPCODE_PEERFILES_RESP) {
			throw new IllegalStateException("El campo 'fileList' no está definido para el mensaje " 
					+ PeerMessageOps.opcodeToOperation(opcode));
		}
		return fileList;
	}

	public void setFileList(String fileList) {
		if (opcode != PeerMessageOps.OPCODE_PEERFILES_RESP) {
			throw new IllegalStateException("El campo 'fileList' no está definido para el mensaje " 
					+ PeerMessageOps.opcodeToOperation(opcode));
		}
		this.fileList = fileList;
	}

	public String getHash() {
		if (opcode != PeerMessageOps.OPCODE_DOWNLOAD_REQ && opcode != PeerMessageOps.OPCODE_DOWNLOAD_RESP) {
			throw new IllegalStateException("El campo 'hash' no está definido para el mensaje " 
					+ PeerMessageOps.opcodeToOperation(opcode));
		}
		return hash;
	}

	public void setHash(String hash) {
		if (opcode != PeerMessageOps.OPCODE_DOWNLOAD_REQ && opcode != PeerMessageOps.OPCODE_DOWNLOAD_RESP) {
			throw new IllegalStateException("El campo 'hash' no está definido para el mensaje " 
					+ PeerMessageOps.opcodeToOperation(opcode));
		}
		this.hash = hash;
	}

	public long getFileSize() {
		if (opcode != PeerMessageOps.OPCODE_DOWNLOAD_RESP) {
			throw new IllegalStateException("El campo 'fileSize' no está definido para el mensaje " 
					+ PeerMessageOps.opcodeToOperation(opcode));
		}
		return fileSize;
	}

	public void setFileSize(long fileSize) {
		if (opcode != PeerMessageOps.OPCODE_DOWNLOAD_RESP) {
			throw new IllegalStateException("El campo 'fileSize' no está definido para el mensaje " 
					+ PeerMessageOps.opcodeToOperation(opcode));
		}
		this.fileSize = fileSize;
	}

	/**
	 * Método de clase para parsear los campos de un mensaje y construir el objeto
	 * DirMessage que contiene los datos del mensaje recibido
	 * 
	 * @param data El array de bytes recibido
	 * @return Un objeto de esta clase cuyos atributos contienen los datos del
	 *         mensaje recibido.
	 * @throws IOException
	 */
	public static PeerMessage readMessageFromInputStream(DataInputStream dis) throws IOException {
		/*
		 * DONE: (Boletín MensajesBinarios) En función del tipo de mensaje, leer del
		 * socket a través del "dis" el resto de campos para ir extrayendo con los
		 * valores y establecer los atributos del un objeto DirMessage que contendrá
		 * toda la información del mensaje, y que será devuelto como resultado. NOTA:
		 * Usar dis.readFully para leer un array de bytes, dis.readInt para leer un
		 * entero, etc.
		 */
		PeerMessage message = new PeerMessage();
		byte opcode = dis.readByte();
		message.opcode = opcode;
		
		switch (opcode) {
		// Formato control: solo se lee el opcode
		case PeerMessageOps.OPCODE_PEERFILES_REQ:
		case PeerMessageOps.OPCODE_FILE_NOT_FOUND:
		case PeerMessageOps.OPCODE_AMBIGUOUS_HASH:
			break; 
		
		// Formato TLV: longitud (int) + valor
		case PeerMessageOps.OPCODE_PEERFILES_RESP:
			int listLength = dis.readInt();
			byte[] listBytes = new byte[listLength];
			dis.readFully(listBytes);
			message.setFileList(new String(listBytes));
			break;
			
		// Formato TLV: longitud (short) + valor
		case PeerMessageOps.OPCODE_DOWNLOAD_REQ:
			short reqHashLength = dis.readShort();
			byte[] reqHashBytes = new byte[reqHashLength];
			dis.readFully(reqHashBytes);
			message.setHash(new String(reqHashBytes));
			break;
			
		// Formato compuesto: HashLength (short) + HashValue (string) + FileSize (long)
		case PeerMessageOps.OPCODE_DOWNLOAD_RESP:
			short respHashLength = dis.readShort();
			byte[] respHashBytes = new byte[respHashLength];
			dis.readFully(respHashBytes);
			message.setHash(new String(respHashBytes));
			long fsize = dis.readLong();
			message.setFileSize(fsize);
			break;
			
		default:
			System.err.println("PeerMessage.readMessageFromInputStream doesn't know how to parse this message opcode: "
					+ PeerMessageOps.opcodeToOperation(opcode));
			System.exit(-1);
		}
		return message;
	}

	public void writeMessageToOutputStream(DataOutputStream dos) throws IOException {
		/*
		 * TODO (Boletín MensajesBinarios): Escribir los bytes en los que se codifica el
		 * mensaje en el socket a través del "dos", teniendo en cuenta opcode del
		 * mensaje del que se trata y los campos relevantes en cada caso. NOTA: Usar
		 * dos.write para leer un array de bytes, dos.writeInt para escribir un entero,
		 * etc.
		 */

		dos.writeByte(opcode);
		switch (opcode) {
		// Formato control: no hay parámetros que escribir
		case PeerMessageOps.OPCODE_PEERFILES_REQ:
		case PeerMessageOps.OPCODE_FILE_NOT_FOUND:
		case PeerMessageOps.OPCODE_AMBIGUOUS_HASH:
			break;
			
		// Formato TLV: longitud (int) + valor
		case PeerMessageOps.OPCODE_PEERFILES_RESP:
			byte[] listBytes = fileList.getBytes();
			dos.writeInt(listBytes.length);
			dos.write(listBytes);
			break;
			
		// Formato TLV: longitud (short) + valor
		case PeerMessageOps.OPCODE_DOWNLOAD_REQ:
			byte[] reqHashBytes = hash.getBytes();
			dos.writeShort(reqHashBytes.length);
			dos.write(reqHashBytes);
			break;
			
		// Formato compuesto: HashLength + HashValue + FileSize
		case PeerMessageOps.OPCODE_DOWNLOAD_RESP:
			byte[] respHashBytes = hash.getBytes();
			dos.writeShort(respHashBytes.length);
			dos.write(respHashBytes);
			dos.writeLong(fileSize);
			break;

		default:
			System.err.println("PeerMessage.writeMessageToOutputStream found unexpected message opcode " + opcode + "("
					+ PeerMessageOps.opcodeToOperation(opcode) + ")");
		}
	}


}
