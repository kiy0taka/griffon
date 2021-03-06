package org.codehaus.griffon.cli;

/**
 * Exception thrown when Griffon is asked to execute a script that it
 * can't find.
 */
public class ScriptNotFoundException extends RuntimeException {
    private final String scriptName;

    public ScriptNotFoundException(String scriptName) {
        super();
        this.scriptName = scriptName;
    }

    public ScriptNotFoundException(String scriptName, String message, Throwable cause) {
        super(message, cause);
        this.scriptName = scriptName;
    }

    public ScriptNotFoundException(String scriptName, Throwable cause) {
        super(cause);
        this.scriptName = scriptName;
    }

    public String getScriptName() {
        return this.scriptName;
    }
}
