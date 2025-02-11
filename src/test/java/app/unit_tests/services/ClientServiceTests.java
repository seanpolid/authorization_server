package app.unit_tests.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAKey;
import java.security.interfaces.RSAPublicKey;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.auth0.jwt.interfaces.DecodedJWT;

import app.dtos.ClientDTO;
import app.dtos.ClientRegistrationDTO;
import app.dtos.TokenDTO;
import app.entities.AuthorizationCode;
import app.entities.Client;
import app.enums.GrantType;
import app.enums.Scope;
import app.exceptions.ApplicationNameTakenException;
import app.exceptions.KeyNotFoundException;
import app.repositories.IAuthorizationCodeRepository;
import app.repositories.IClientRepository;
import app.repositories.ITokenRepository;
import app.services.ClientService;
import app.utils.JWTUtil;
import app.utils.RSAUtil;

public class ClientServiceTests {

	private IClientRepository clientRepository;
	private IAuthorizationCodeRepository authorizationCodeRepository;
	private PasswordEncoder passwordEncoder;
	private ITokenRepository tokenRepository;
	private RSAUtil rsaUtil;
	
	private ClientService clientService;
	private String code = "code";
	private String clientSecret = "secret";
	
	@BeforeEach
	public void setup() {
		clientRepository = mock(IClientRepository.class);
		authorizationCodeRepository = mock(IAuthorizationCodeRepository.class);
		passwordEncoder = mock(PasswordEncoder.class);
		tokenRepository = mock(ITokenRepository.class);
		rsaUtil = mock(RSAUtil.class);
		
		clientService = new ClientService(clientRepository,
										  authorizationCodeRepository,
										  passwordEncoder,
										  tokenRepository,
										  rsaUtil);
	}
	
	@Test
	public void isValidAuthorizationCode_true() {
		// Arrange		
		when(authorizationCodeRepository.findByCode(anyString())).thenReturn(Optional.of(new AuthorizationCode()));
		
		Client client = new Client();
		client.setId(1);
		client.setSecret(clientSecret);
		when(clientRepository.findByIdentifier(anyString())).thenReturn(Optional.of(client));
		
		when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
		
		AuthorizationCode authorizationCode = new AuthorizationCode(1, code, LocalDateTime.now(), false);
		when(authorizationCodeRepository.findByClientIdAndRedirectUri(anyInt(), anyString())).thenReturn(List.of(authorizationCode));
		
		// Act
		boolean isValid = clientService.isValidAuthorizationCode(code, "identifer", clientSecret, "redirect uri");
	
		// Assert
		assertTrue(isValid);
	}
	
	@Test
	public void isValidAuthorizationCode_codeNotPresent_false() {
		// Arrange
		when(authorizationCodeRepository.findByCode(anyString())).thenReturn(Optional.empty());

		// Act
		boolean isValid = clientService.isValidAuthorizationCode(code, "identifer", clientSecret, "redirect uri");
	
		// Assert
		assertFalse(isValid);
	}
	
	@Test
	public void isValidAuthorizationCode_clientNotPresent_false() {
		// Arrange		
		when(authorizationCodeRepository.findByCode(anyString())).thenReturn(Optional.of(new AuthorizationCode()));
		when(clientRepository.findByIdentifier(anyString())).thenReturn(Optional.empty());
		
		// Act
		boolean isValid = clientService.isValidAuthorizationCode(code, "identifer", clientSecret, "redirect uri");
	
		// Assert
		assertFalse(isValid);
	}
	
	@Test
	public void isValidAuthorizationCode_passwordDoesntMatch_false() {
		// Arrange		
		when(authorizationCodeRepository.findByCode(anyString())).thenReturn(Optional.of(new AuthorizationCode()));
		
		Client client = new Client();
		client.setId(1);
		client.setSecret(clientSecret);
		when(clientRepository.findByIdentifier(anyString())).thenReturn(Optional.of(client));
		
		when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

		// Act
		boolean isValid = clientService.isValidAuthorizationCode(code, "identifer", clientSecret, "redirect uri");
	
		// Assert
		assertFalse(isValid);
	}
	
	@Test
	public void isValidAuthorizationCode_latestCodeNotPresent_false() {
		// Arrange		
		when(authorizationCodeRepository.findByCode(anyString())).thenReturn(Optional.of(new AuthorizationCode()));
		
		Client client = new Client();
		client.setId(1);
		client.setSecret(clientSecret);
		when(clientRepository.findByIdentifier(anyString())).thenReturn(Optional.of(client));
		
		when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
		
		when(authorizationCodeRepository.findByClientIdAndRedirectUri(anyInt(), anyString())).thenReturn(List.of());
		
		// Act
		boolean isValid = clientService.isValidAuthorizationCode(code, "identifer", clientSecret, "redirect uri");
	
		// Assert
		assertFalse(isValid);
	}
	
	@Test
	public void isValidAuthorizationCode_latestCodeIsUsed_false() {
		// Arrange		
		when(authorizationCodeRepository.findByCode(anyString())).thenReturn(Optional.of(new AuthorizationCode()));
		
		Client client = new Client();
		client.setId(1);
		client.setSecret(clientSecret);
		when(clientRepository.findByIdentifier(anyString())).thenReturn(Optional.of(client));
		
		when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
		
		AuthorizationCode authorizationCode = new AuthorizationCode(1, code, LocalDateTime.now(), true);
		when(authorizationCodeRepository.findByClientIdAndRedirectUri(anyInt(), anyString())).thenReturn(List.of(authorizationCode));
		
		// Act
		boolean isValid = clientService.isValidAuthorizationCode(code, "identifer", clientSecret, "redirect uri");
	
		// Assert
		assertFalse(isValid);
	}
	
	@Test
	public void isValidAuthorizationCode_latestCodeDoesntEqualCode_false() {
		// Arrange		
		when(authorizationCodeRepository.findByCode(anyString())).thenReturn(Optional.of(new AuthorizationCode()));
		
		Client client = new Client();
		client.setId(1);
		client.setSecret(clientSecret);
		when(clientRepository.findByIdentifier(anyString())).thenReturn(Optional.of(client));
		
		when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
		
		AuthorizationCode authorizationCode = new AuthorizationCode(1, code, LocalDateTime.now(), false);
		when(authorizationCodeRepository.findByClientIdAndRedirectUri(anyInt(), anyString())).thenReturn(List.of(authorizationCode));
		
		// Act
		boolean isValid = clientService.isValidAuthorizationCode("different code", "identifer", clientSecret, "redirect uri");
	
		// Assert
		assertFalse(isValid);
	}
	
	@Test
	public void generateToken_success() throws KeyNotFoundException, NoSuchAlgorithmException {
		// Arrange
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		
		KeyPair keyPair = generator.generateKeyPair();
		RSAKey privateKey = (RSAKey) keyPair.getPrivate();
		when(rsaUtil.getPrivateKey(anyString())).thenReturn(privateKey);
		
		String clientId = "client";
		int userId = 1;
		SimpleGrantedAuthority authority = new SimpleGrantedAuthority("READ");
		Authentication authentication = new UsernamePasswordAuthenticationToken(userId, null, List.of(authority));
		
		// Act
		TokenDTO tokenDTO = clientService.generateToken(authentication, clientId, null);
		String accessToken = tokenDTO.getAccessToken();
		
		// Assert
		DecodedJWT decodedJWT = JWTUtil.decode(accessToken, (RSAPublicKey) keyPair.getPublic());
		String actualAuthority = decodedJWT.getClaim("authorities").asList(String.class).get(0);
				
		assertEquals(clientId, decodedJWT.getClaim("client").asString());
		assertEquals(userId, decodedJWT.getClaim("user").asInt());
		assertEquals(authority.getAuthority(), actualAuthority);
	}
	
	@Test
	public void generateToken_privateKeyNull_exception() throws KeyNotFoundException {
		// Arrange
		when(rsaUtil.getPrivateKey(anyString())).thenReturn(null);
		
		String clientId = "client";
		int userId = 1;
		SimpleGrantedAuthority authority = new SimpleGrantedAuthority("READ");
		Authentication authentication = new UsernamePasswordAuthenticationToken(userId, null, List.of(authority));
		
		// Act and Assert
		assertThrows(KeyNotFoundException.class, () -> {
			clientService.generateToken(authentication, clientId, null);
		});
	}
	
	@Test
	public void register_success() throws ApplicationNameTakenException {
		//Arrange
		ClientRegistrationDTO clientRegistration = new ClientRegistrationDTO("application name", 
																			 true, 
																			 List.of(GrantType.AUTHORIZATION_CODE),
																			 List.of(Scope.READ_WRITE),
																			 List.of("http://localhost"));
		
		when(passwordEncoder.encode(anyString())).thenReturn("encoded password");
		
		Client client = new Client(clientRegistration.getApplicationName(),
								   "identifier",
								   "encoded password",
								   true,
								   Set.of(GrantType.AUTHORIZATION_CODE),
								   Set.of(Scope.READ_WRITE),
								   Set.of("http://localhost"));
		when(clientRepository.save(any(Client.class))).thenReturn(client);
		
		// Act
		ClientDTO savedClient = clientService.register(clientRegistration);
		
		// Assert
		assertEquals(savedClient.getApplicationName(), client.getApplicationName());
		assertEquals(savedClient.getGrantTypes(), client.getGrantTypes().stream().toList());
		assertEquals(savedClient.getScopes(), client.getScopes().stream().toList());
		assertEquals(savedClient.getRedirectUris(), client.getRedirectUris().stream().toList());
		assertNotEquals(savedClient.getSecret(), client.getSecret());
	}
	
	@Test
	public void register_applicationNameTaken_exception() {
		//Arrange
		ClientRegistrationDTO clientRegistration = new ClientRegistrationDTO("application name", 
																			 true, 
																			 List.of(GrantType.AUTHORIZATION_CODE),
																			 List.of(Scope.READ_WRITE),
																			 List.of("http://localhost"));
		
		when(clientRepository.findByApplicationName(anyString())).thenReturn(Optional.of(new Client()));
		
		// Act and Assert
		assertThrows(ApplicationNameTakenException.class, () -> {
			clientService.register(clientRegistration);
		});
	}
	
	@Test
	public void getClients_success() {
		// Arrange
		Client client = new Client("application name",
								   "identifier",
								   "secret",
								   true,
								   Set.of(GrantType.AUTHORIZATION_CODE),
								   Set.of(Scope.READ_WRITE),
								   Set.of("http://localhost"));
		client.setId(1);
		
		ClientDTO expected = new ClientDTO(1,
										   client.getApplicationName(),
										   client.getIdentifier(),
										   null,
										   client.isRequiresConsent(),
										   client.getScopes().stream().toList(),
										   client.getRedirectUris().stream().toList(),
										   client.getGrantTypes().stream().toList());
		
		when(clientRepository.findAll()).thenReturn(List.of(client));
		
		// Act
		List<ClientDTO> savedClients = clientService.getClients();
		
		// Assert
		assertEquals(1, savedClients.size());
		assertEquals(expected, savedClients.get(0));
	}
}
