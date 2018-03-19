/**
 * Prosys OPC UA Java SDK
 *
 * Copyright (c) Prosys PMS Ltd., <http://www.prosysopc.com>.
 * All rights reserved.
 */
package com.prosysopc.ua.samples;

import java.security.cert.CertificateParsingException;
import java.util.EnumSet;

import org.opcfoundation.ua.core.ApplicationDescription;
import org.opcfoundation.ua.transport.security.Cert;
import org.opcfoundation.ua.utils.CertificateUtils;

import com.prosysopc.ua.CertificateValidationListener;
import com.prosysopc.ua.PkiFileBasedCertificateValidator.CertificateCheck;
import com.prosysopc.ua.PkiFileBasedCertificateValidator.ValidationResult;

/**
 * A sample implementation of a CertificateValidationListener
 */
public class MyCertificateValidationListener implements CertificateValidationListener {

	@Override
	public ValidationResult onValidate(Cert certificate, ApplicationDescription applicationDescription,
			EnumSet<CertificateCheck> passedChecks) {
		try {
			SampleConsoleServer.println(
					applicationDescription + ", " + CertificateUtils.getApplicationUriOfCertificate(certificate));
		} catch (CertificateParsingException e1) {
			throw new RuntimeException(e1);
		}

		// Do not mind about URI...
		if (passedChecks.containsAll(
				EnumSet.of(CertificateCheck.Trusted, CertificateCheck.Validity, CertificateCheck.Signature))) {
			if (!passedChecks.contains(CertificateCheck.Uri))
				try {
					SampleConsoleServer.println("Client's ApplicationURI (" + applicationDescription.getApplicationUri()
							+ ") does not match the one in certificate: "
							+ CertificateUtils.getApplicationUriOfCertificate(certificate));
				} catch (CertificateParsingException e) {
					throw new RuntimeException(e);
				}
			return ValidationResult.AcceptPermanently;
		}
		return ValidationResult.Reject;
	}

}
