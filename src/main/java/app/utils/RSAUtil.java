package app.utils;

import java.io.FileInputStream;
import java.security.KeyFactory;
import java.security.interfaces.RSAKey;
import java.security.spec.PKCS8EncodedKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RSAUtil {

	public RSAKey getPrivateKey(String privateKeyPath) {
		try (FileInputStream inputStream = new FileInputStream(privateKeyPath)) {
			byte[] bytes = inputStream.readAllBytes();
			PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(bytes);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			return (RSAKey) keyFactory.generatePrivate(keySpec);
		} catch (Exception ex) {
			System.out.println(ex);
		}
		
		return null;
	}

}
