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
	
	public static final String OPERATION_SERVE = "serve";
	public static final String OPERATION_SERVE_OK= "serveok";
	public static final String OPERATION_SERVE_FAIL= "servefail";

	public static final String OPERATION_FILELIST = "filelist";
	public static final String OPERATION_FILELIST_OK = "filelistok";
	public static final String OPERATION_FILELIST_FAIL = "filelistfail";
	
	public static final String OPERATION_DOWNLOAD = "download";
	public static final String OPERATION_DOWNLOAD_OK = "downloadok";
	public static final String OPERATION_DOWNLOAD_FAIL = "downloadfail";
	
	
}
