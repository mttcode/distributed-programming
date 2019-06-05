package at.ac.tuwien.infosys.rnue.implementation.security;

import java.util.*;
import java.lang.*;
import at.ac.tuwien.infosys.rnue.interfaces.*;
import at.ac.tuwien.infosys.rnue.helpers.*;
import java.io.*;
import java.security.*;

public class CryptographyImpl implements ICryptography {
	private Signature sign;
	private KeyPair keys;
	
	public CryptographyImpl() throws ShareMeException {
		try {
			sign = Signature.getInstance("MD5withRSA");
		} catch (NoSuchAlgorithmException nsae) {
			throw new ShareMeException(nsae.getMessage());
		}
	}

	public PublicKey initialize(String owner, String keyFile) throws ShareMeException {
		File file = new File(keyFile);
		
		if (file.exists()) {
			try {
				keys = (KeyPair) (new ObjectInputStream(new FileInputStream(file))).readObject();
				System.out.println("Using keys from file.");
			} catch (IOException ioe) {
				throw new ShareMeException(ioe.getMessage());
			} catch (ClassNotFoundException cnfe) {
				throw new ShareMeException(cnfe.getMessage());
			}
			return null;
		} else {
			KeyPairGenerator gen = null;
			try {
				gen = KeyPairGenerator.getInstance("RSA");
				System.out.println("Generating new keys.");
			} catch (NoSuchAlgorithmException nsae) {
				throw new ShareMeException(nsae.getMessage());
			}
			gen.initialize(IConstants.KEY_LENGTH);
			keys = gen.generateKeyPair();
			try {
				file.createNewFile();
				(new ObjectOutputStream(new FileOutputStream(file))).writeObject(keys);
			} catch (IOException ioe) {
				throw new ShareMeException(ioe.getMessage());
			}
			return keys.getPublic();
		}
	}

	public byte[] convertSerializableToByteArray(Serializable obj) throws ShareMeException {
		byte[] buf = null;

		try {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			(new ObjectOutputStream(stream)).writeObject(obj);
			buf = stream.toByteArray();
		} catch (IOException ioe) {
			throw new ShareMeException(ioe.getMessage());
		}

		return buf;
	}
		
	public byte[] sign(Serializable obj) throws ShareMeException {
		byte[] buf = null;
		
		try {
			sign.initSign(keys.getPrivate());
			sign.update(this.convertSerializableToByteArray(obj));
			buf = sign.sign();
		} catch (InvalidKeyException ike) {
			throw new ShareMeException(ike.getMessage());
		} catch (SignatureException se) {
			throw new ShareMeException(se.getMessage());
		}
		
		return buf;
	}

	public boolean verify(Serializable obj, byte[] signature, PublicKey pubKey) throws ShareMeException {
		boolean result = false;
		
		try {
			sign.initVerify(pubKey);
			sign.update(this.convertSerializableToByteArray(obj));
			result = sign.verify(signature);
		} catch (InvalidKeyException ike) {
			throw new ShareMeException(ike.getMessage());
		} catch (SignatureException se) {
			throw new ShareMeException(se.getMessage());
		}
		
		return result;
	}
}
