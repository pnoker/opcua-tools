/**
 * Prosys OPC UA Java SDK
 *
 * Copyright (c) Prosys PMS Ltd., <http://www.prosysopc.com>.
 * All rights reserved.
 */
package com.prosysopc.ua.samples;

import org.opcfoundation.ua.builtintypes.StatusCode;
import org.opcfoundation.ua.core.StatusCodes;
import org.opcfoundation.ua.core.UserIdentityToken;
import org.opcfoundation.ua.core.UserTokenType;
import org.opcfoundation.ua.transport.security.Cert;

import com.prosysopc.ua.PkiFileBasedCertificateValidator;
import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.server.ServerUserIdentity;
import com.prosysopc.ua.server.Session;
import com.prosysopc.ua.server.UserValidator;

/**
 * A sample implementation of the UserValidator
 */
public class MyUserValidator implements UserValidator {

	private final PkiFileBasedCertificateValidator userValidator;

	/**
	 *
	 */
	public MyUserValidator(PkiFileBasedCertificateValidator userValidator) {
		this.userValidator = userValidator;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.prosysopc.ua.server.UserValidator#onValidate(com.prosysopc.ua.server
	 * .Session, com.prosysopc.ua.server.SessionManager.ServerUserIdentity)
	 */
	@Override
	public boolean onValidate(Session session, ServerUserIdentity userIdentity) throws StatusException {
		// Return true, if the user is allowed access to the server
		// Note that the UserIdentity can be of different actual types,
		// depending on the selected authentication mode (by the client).
		SampleConsoleServer.println("onValidate: userIdentity=" + userIdentity);
		if (userIdentity.getType().equals(UserTokenType.UserName))
			if (userIdentity.getName().equals("opcua") && userIdentity.getPassword().equals("opcua"))
				return true;
			else if (userIdentity.getName().equals("opcua2") && userIdentity.getPassword().equals("opcua2"))
				return true;
			else
				return false;

		// Example for validating the user auth certs via
		// PkiFileBasedCertificateValidator
		if (userIdentity.getType().equals(UserTokenType.Certificate)) {
			Cert cert = userIdentity.getCertificate();
			if (userValidator.validateCertificate(cert).isGood())
				return true;
			else
				throw new StatusException(new StatusCode(StatusCodes.Bad_IdentityTokenRejected));

		}

		return true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.prosysopc.ua.server.UserValidator#onValidationError(com.prosysopc
	 * .ua.server.Session, org.opcfoundation.ua.core.UserIdentityToken,
	 * java.lang.Exception)
	 */
	@Override
	public void onValidationError(Session session, UserIdentityToken userToken, Exception exception) {
		SampleConsoleServer
				.println("onValidationError: User validation failed: userToken=" + userToken + " error=" + exception);
	}

}
