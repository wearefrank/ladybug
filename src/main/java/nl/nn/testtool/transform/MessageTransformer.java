package nl.nn.testtool.transform;

/**
 * Transform a message before it is stored in a checkpiont. E.g. make it
 * possible to hide a password.
 * 
 * @author Jaco de Groot
 */
public interface MessageTransformer {

	String transform(String message);

}
