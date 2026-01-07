package whiterose.foundation.deus.api.exceptions;

import whiterose.foundation.deus.api.module.CheatModule;

public class DuplicateModuleException extends RuntimeException {
    
    public DuplicateModuleException(CheatModule module) {
        super("Duplicate module found [" + module + "]");
    }

}
