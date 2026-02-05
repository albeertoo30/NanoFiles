package es.um.redes.nanoFiles.udp.message;

public class DirMessageOps {

	/*
	 * DONE: (Boletín MensajesASCII) Añadir aquí todas las constantes que definen
	 * los diferentes tipos de mensajes del protocolo de comunicación con el
	 * directorio (valores posibles del campo "operation").
	 */
	public static final String OPERATION_INVALID = "invalid_operation";
	
	public static final String OPERATION_PING = "ping";
	public static final String OPERATION_PING_OK = "pingok";
	public static final String OPERATION_PING_FAIL = "pingfail";
	
	// Operación serve
	public static final String OPERATION_SERVE = "serve";
	public static final String OPERATION_SERVE_OK = "serveok";
	public static final String OPERATION_SERVE_FAIL = "servefail";
	
	// Operación dirfiles
	public static final String OPERATION_DIRFILES = "dirfiles";
	public static final String OPERATION_DIRFILES_OK = "dirfilesok";
	public static final String OPERATION_DIRFILES_FAIL = "dirfilesfail";
	
	// Operación peers
	public static final String OPERATION_PEERS = "peers";
	public static final String OPERATION_PEERS_OK = "peersok";
	public static final String OPERATION_PEERS_FAIL = "peersfail";
	
	// DONE: definir las operaciones del protocolo de directorio


}
