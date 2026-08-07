package cn.zfzcraft.pureioc.core.exception;

public class ConstructorCircularDependencyException extends RuntimeException {

	private static final long serialVersionUID = -8352871842725328184L;

	public ConstructorCircularDependencyException(String message) {
		super(message);
	}

}
