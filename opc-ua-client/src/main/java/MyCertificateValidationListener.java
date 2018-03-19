/**
 * Prosys OPC UA Java SDK
 *
 * Copyright (c) Prosys PMS Ltd., <http://www.prosysopc.com>.
 * All rights reserved.
 */

import java.security.cert.CertificateParsingException;
import java.util.Date;
import java.util.EnumSet;

import org.opcfoundation.ua.core.ApplicationDescription;
import org.opcfoundation.ua.transport.security.Cert;
import org.opcfoundation.ua.utils.CertificateUtils;

import com.prosysopc.ua.CertificateValidationListener;
import com.prosysopc.ua.PkiFileBasedCertificateValidator.CertificateCheck;
import com.prosysopc.ua.PkiFileBasedCertificateValidator.ValidationResult;

/**
 * A sampler listener for certificate validation results.
 */
public class MyCertificateValidationListener implements CertificateValidationListener {

	@Override
	public ValidationResult onValidate(Cert certificate, ApplicationDescription applicationDescription,
			EnumSet<CertificateCheck> passedChecks) {
		// Called whenever the PkiFileBasedCertificateValidator has
		// validated a certificate
		println("");
		println("*** The Server Certificate : ");
		println("");
		println("Subject   : " + certificate.getCertificate().getSubjectX500Principal().toString());
		println("Issued by : " + certificate.getCertificate().getIssuerX500Principal().toString());
		println("Valid from: " + certificate.getCertificate().getNotBefore().toString());
		println("        to: " + certificate.getCertificate().getNotAfter().toString());
		println("");
		if (!passedChecks.contains(CertificateCheck.Signature))
			println("* The Certificate is NOT SIGNED BY A TRUSTED SIGNER!");
		if (!passedChecks.contains(CertificateCheck.Validity)) {
			Date today = new Date();
			final boolean isYoung = certificate.getCertificate().getNotBefore().compareTo(today) > 0;
			final boolean isOld = certificate.getCertificate().getNotAfter().compareTo(today) < 0;
			final String oldOrYoung = isOld ? "(anymore)" : (isYoung ? "(yet)" : "");

			println("* The Certificate time interval IS NOT VALID " + oldOrYoung + "!");
		}
		if (!passedChecks.contains(CertificateCheck.Uri)) {
			println("* The Certificate URI DOES NOT MATCH the ApplicationDescription URI!");
			println("    ApplicationURI in ApplicationDescription = " + applicationDescription.getApplicationUri());
			try {
				println("    ApplicationURI in Certificate            = "
						+ CertificateUtils.getApplicationUriOfCertificate(certificate));
			} catch (CertificateParsingException e) {
				println("    ApplicationURI in Certificate is INVALID");
			}
		}
		if (passedChecks.contains(CertificateCheck.SelfSigned))
			println("* The Certificate is self-signed.");
		println("");
		// If the certificate is trusted, valid and verified, accept it
		if (passedChecks.containsAll(CertificateCheck.COMPULSORY))
			return ValidationResult.AcceptPermanently;
		do {
			println("Note: If the certificate is not OK,");
			println("you will be prompted again, even if you answer 'Always' here.");
			println("");
			println("Do you want to accept this certificate?\n" + " (A=Always, Y=Yes, this time, N=No)\n"
					+ " (D=Show Details of the Certificate)");
			String input = readInput().toLowerCase();
			if (input.equals("a"))
				// if the certificate is not valid anymore or the signature
				// is not verified, you will be prompted again, even if you
				// select always here
				return ValidationResult.AcceptPermanently;

			if (input.equals("y"))
				return ValidationResult.AcceptOnce;
			if (input.equals("n"))
				return ValidationResult.Reject;
			if (input.equals("d"))
				println("Certificate Details:" + certificate.getCertificate().toString());
		} while (true);
	}

	private void println(String string) {
		SampleConsoleClient.println(string);
	}

	private String readInput() {
		return SampleConsoleClient.readInput(false);
	}
};
