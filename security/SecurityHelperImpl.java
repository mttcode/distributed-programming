package at.ac.tuwien.infosys.rnue.implementation.security;

import java.util.*;
import java.lang.*;
import at.ac.tuwien.infosys.rnue.interfaces.*;
import at.ac.tuwien.infosys.rnue.helpers.*;
import java.io.*;
import java.security.*;
import org.omg.CORBA.*;
import org.omg.CosNaming.*;
import org.omg.CosNaming.NamingContextPackage.*;
import at.ac.tuwien.infosys.rnue.implementation.security.CorbaCAImpl.*;

public class SecurityHelperImpl implements ISecurityHelper {
	private ICertificationAuthority ica;
	private CryptographyImpl crypt;
		
	public SecurityHelperImpl(Properties properties) throws ShareMeException {
		String orbHost = properties.getProperty(IConstants.ORB_HOST);
		String orbPort = properties.getProperty(IConstants.ORB_PORT);
		ORB orb = ORB.init(new String[] {"-ORBInitialHost", orbHost, "-ORBInitialPort", orbPort}, null);
		try {
			org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
			NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);
			this.ica = ICertificationAuthorityHelper.narrow(ncRef.resolve_str(IConstants.DSG_CA_NAME));
			System.out.println("Server object resolved.");
			
		} catch (NotFound nfe) {
			throw new ShareMeException(nfe.getMessage());
		} catch (CannotProceed cpe) {
			throw new ShareMeException(cpe.getMessage());
		} catch (InvalidName ine) {
			throw new ShareMeException(ine.getMessage());
		} catch (org.omg.CORBA.ORBPackage.InvalidName ine) {
			throw new ShareMeException(ine.getMessage());
		}
		
		crypt = new CryptographyImpl();
		//this.unregisterPublicKey(properties.getProperty(IConstants.HUMAN_READABLE_NAME), "please");
		PublicKey pubKey = (PublicKey) crypt.initialize(properties.getProperty(IConstants.HUMAN_READABLE_NAME), properties.getProperty(IConstants.KEYFILE_NAME));
		if (pubKey != null) {
			this.registerPublicKey(properties.getProperty(IConstants.HUMAN_READABLE_NAME), pubKey);
		}
	}

	public PublicKey getPublicKeyFromCA(String owner) throws ShareMeException {
		PublicKey key = null;
		
		try {
			java.lang.Object obj = (new ObjectInputStream(new ByteArrayInputStream(this.ica.getKey(owner)))).readObject();
			if (obj instanceof PublicKey) key = (PublicKey) obj;
		} catch (CorbaCAException ccae) {
			throw new ShareMeException("<"+owner+">\n"+ccae.getMessage()+"\n");
		} catch (ClassNotFoundException cnfe) {
			throw new ShareMeException(cnfe.getMessage());
		} catch (IOException ioe) {
			throw new ShareMeException(ioe.getMessage());
		}
		
		
		return key;
	}

	public boolean registerPublicKey(String owner, PublicKey pk) throws ShareMeException {
		byte[] key = this.crypt.convertSerializableToByteArray(pk);
		boolean result = false;
		
		try {
			result = this.ica.registerKey(owner, key, "please");
		} catch (CorbaCAException ccae) {
			throw new ShareMeException(ccae.getMessage());
		}

		return result;
	}

	public boolean unregisterPublicKey(String owner, String password) throws ShareMeException {
		boolean result = false;
		
		try {   
			result = this.ica.unregisterKey(owner, password);
		} catch (CorbaCAException ccae) {
			throw new ShareMeException(ccae.getMessage());
		}

		return result;
	}

	public byte[] sign(Serializable obj) throws ShareMeException {
		return this.crypt.sign(obj);
	}

	public boolean verify(Serializable obj, byte[] sig, PublicKey pk) throws ShareMeException {
		return this.crypt.verify(obj, sig, pk);
	}
}
