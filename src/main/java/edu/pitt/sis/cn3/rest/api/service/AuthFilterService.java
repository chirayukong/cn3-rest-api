/*
 * Copyright (C) 2016 University of Pittsburgh.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package edu.pitt.sis.cn3.rest.api.service;

import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.JWTVerifyException;

import edu.pitt.sis.cn3.db.entity.UserInfo;
import edu.pitt.sis.cn3.db.service.UserInfoService;
import edu.pitt.sis.cn3.rest.api.Role;
import edu.pitt.sis.cn3.rest.api.exception.AccessDeniedException;
import edu.pitt.sis.cn3.rest.api.exception.AccessForbiddenException;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.SignatureException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 *
 * Jun 5, 2016 10:52:45 PM
 *
 * @author Kevin V. Bui (kvb2@pitt.edu)
 */
@Service
public class AuthFilterService {

	@Value("${cn3.jwt.issuer}")
	private String jwtIssuer;

	@Value("${cn3.jwt.secret}")
	private String jwtSecret;

	@Value("${cn3.md5.salty}")
	private String MD5_SALTY;

	private static final Logger LOGGER = LoggerFactory
			.getLogger(AuthFilterService.class);

	private static final String AUTH_HEADER = "Authorization";
	public static final String AUTH_SCHEME_BASIC = "Basic";
	public static final String AUTH_SCHEME_BEARER = "Bearer";

	private static final AccessDeniedException BASIC_AUTH_USER_CREDENTIALS_REQUIRED = new AccessDeniedException(
			"User credentials are required.");
	private static final AccessDeniedException BASIC_AUTH_SCHEME_REQUIRED = new AccessDeniedException(
			"Basic Authentication scheme is required to get the JSON Web Token(JWT).");
	private static final AccessDeniedException BASIC_AUTH_INVALID_USER_CREDENTIALS = new AccessDeniedException(
			"Invalid user credentials.");

	private static final AccessDeniedException BEARER_AUTH_JWT_REQUIRED = new AccessDeniedException(
			"JSON Web Token(JWT) is required.");
	private static final AccessDeniedException BEARER_AUTH_SCHEME_REQUIRED = new AccessDeniedException(
			"Bearer Authentication scheme is required to access this resource.");
	private static final AccessDeniedException BEARER_AUTH_EXPIRED_JWT = new AccessDeniedException(
			"Your JSON Web Token(JWT) has expired, please get a new one and try again.");
	private static final AccessDeniedException BEARER_AUTH_INVALID_JWT = new AccessDeniedException(
			"Invalid JSON Web Token(JWT).");

	private static final AccessForbiddenException FORBIDDEN_ACCESS = new AccessForbiddenException(
			"You don't have permission to access this resource.");

	private final UserInfoService userInfoService;

	@Autowired
	public AuthFilterService(UserInfoService userInfoService) {
		this.userInfoService = userInfoService;
	}

	// Direct the actual authentication to baisc auth
	public void verifyBasicAuth(ContainerRequestContext requestContext) {
		// Based on testing, getHeaderString() matches the header string in a
		// case-insensitive way
		// So no need to worry about the cases of the "Authorization" header
		String authCredentials = requestContext.getHeaderString(AUTH_HEADER);
		if (authCredentials == null) {
			throw BASIC_AUTH_USER_CREDENTIALS_REQUIRED;
		}

		// Use lower case to check since HTTP headers are case-insentive
		// Add a space to make sure "Basic" is separated from the base64 string
		// by a space
		if (!authCredentials.toLowerCase().startsWith(
				AUTH_SCHEME_BASIC.toLowerCase() + " ")) {
			throw BASIC_AUTH_SCHEME_REQUIRED;
		}

		// "\\s+" will cause any number of consecutive spaces to split the
		// string into tokens
		// Here we split the auth string by space(s) and the second part is the
		// base64 encoded
		String authCredentialBase64 = authCredentials.split("\\s+")[1];
		// In the basic auth schema, both username and password are encoded in
		// the request header
		// So we'll need to get the user account info with username and password
		String credentials = new String(Base64.getDecoder().decode(
				authCredentialBase64));
		UserInfo userInfo = retrieveUserInfo(credentials);

		if (userInfo == null) {
			throw BASIC_AUTH_INVALID_USER_CREDENTIALS;
		}

		// No need to check isUserInRole("admin") since everyone can sign in
		// No need to check isAccountMatchesRequest(userAccount, requestContext)
		// since the jwt URI doesn't contain username
		SecurityContext securityContext = createSecurityContext(userInfo,
				requestContext, AUTH_SCHEME_BASIC);

		requestContext.setSecurityContext(securityContext);
	}

	// Direct the actual authentication to jwt based bearer schema
	public void verifyJwt(ContainerRequestContext requestContext) {
		// Based on testing, getHeaderString() matches the header string in a
		// case-insensitive way
		// So no need to worry about the cases of the "Authorization" header
		String authCredentials = requestContext.getHeaderString(AUTH_HEADER);
		if (authCredentials == null) {
			throw BEARER_AUTH_JWT_REQUIRED;
		}

		// All other endpoints use bearer JWT to verify the API consumer
		// Use lower case to check since HTTP headers are case-insentive
		// Add a space to makre sure "Bearer" is sepatated from the base64
		// string by a space
		if (!authCredentials.toLowerCase().startsWith(
				AUTH_SCHEME_BEARER.toLowerCase() + " ")) {
			throw BEARER_AUTH_SCHEME_REQUIRED;
		}

		// Verify JWT
		try {
			// "\\s+" will cause any number of consecutive spaces to split the
			// string into tokens
			// Here we split the auth string by space(s) and the second part is
			// the base64 encoded JWT
			String jwt = authCredentials.split("\\s+")[1];

			// Verify both secret and issuer
			final JWTVerifier jwtVerifier = new JWTVerifier(jwtSecret, null,
					jwtIssuer);
			final Map<String, Object> claims = jwtVerifier.verify(jwt);

			// Verify the expiration date
			Long exp = (Long) claims.get("exp");
			Instant nowInstant = Instant.now();
			Long now = Date.from(nowInstant).getTime();
			if (now.compareTo(exp) > 0) {
				throw BEARER_AUTH_EXPIRED_JWT;
			}

			// We can simply get the user account based on the user id
			// Turned out jwt library returns claims.get("uid") as
			// java.lang.Integer
			// System.out.println(claims.get("uid").getClass().getName());
			Integer uidInteger = (Integer) claims.get("uid");
			Long uid = uidInteger.longValue();

			UserInfo userInfo = userInfoService.findById(uid);
			// Since we check the user existence here, no need to check it again
			// in each endpoint service
			if (userInfo == null) {
				throw BEARER_AUTH_INVALID_JWT;
			}

			// Also make sure the uid found in jwt matches the one in URI
			SecurityContext securityContext = createSecurityContext(userInfo,
					requestContext, AUTH_SCHEME_BEARER);
			if (!(securityContext.isUserInRole("admin") || isAccountMatchesRequest(
					uid, requestContext))) {
				throw FORBIDDEN_ACCESS;
			}

			// Then compare the jwt with the one stored in `public_key` field in
			// user account table
			// It's very possible that the jwt sent here has already been
			// overwritten
			String currentJwt = userInfo.getPublicKey();
			if (!currentJwt.equals(jwt)) {
				throw BEARER_AUTH_INVALID_JWT;
			}

			requestContext.setSecurityContext(securityContext);
		} catch (NoSuchAlgorithmException | InvalidKeyException
				| IllegalStateException | IOException | SignatureException
				| JWTVerifyException ex) {
			LOGGER.error("Failed to verify JWT", ex);
		}
	}

	private boolean isAccountMatchesRequest(Long uid,
			ContainerRequestContext requestContext) {
		MultivaluedMap<String, String> pathParams = requestContext.getUriInfo()
				.getPathParameters();
		long reqUid = Long.parseLong(pathParams.getFirst("uid"));

		return uid.equals(reqUid);
	}

	private SecurityContext createSecurityContext(UserInfo userInfo,
			ContainerRequestContext requestContext, String authScheme) {
		String email = userInfo.getEmail();

		Set<String> roles = new HashSet<>();
		int userroleId = userInfo.getRoleId();
		switch (userroleId) {
		case 1:
			roles.add(Role.USER);
			break;
		case 2:
			roles.add(Role.ADMIN);
			break;
		}

		boolean secure = "https".equals(requestContext.getUriInfo()
				.getRequestUri().getScheme());

		return new CustomSecurityContext(email, roles, authScheme, secure);
	}

	/**
	 * Find the user info by email and password provided in Basic Auth header
	 *
	 * @param credentials
	 * @return
	 */
	private UserInfo retrieveUserInfo(String credentials) {
		StringTokenizer tokenizer = new StringTokenizer(credentials, ":");
		String email = tokenizer.nextToken();
		String password = tokenizer.nextToken();
		UserInfo userInfo = null;
		try {
			MessageDigest m = MessageDigest.getInstance("MD5");
			m.reset();
			m.update(password.getBytes(Charset.forName("UTF-8")));
			BigInteger bigInt = new BigInteger(1, m.digest());
			String hashPassword = bigInt.toString(16);
			hashPassword = hashPassword + MD5_SALTY;
			m.reset();
			m.update(hashPassword.getBytes(Charset.forName("UTF-8")));
			bigInt = new BigInteger(1, m.digest());
			String saltyHashPassword = bigInt.toString(16);
			userInfo = userInfoService.findByEmail(email);
			if(userInfo!=null){
				if (!userInfo.getPassword().equals(saltyHashPassword)) {
					userInfo = null;
				}
			}

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		return userInfo;
		// return userInfoService.authenticate(email, password); // Too slow
	}

	class CustomSecurityContext implements SecurityContext {

		private final Principal principal;
		private final Set<String> roles;
		private final String authScheme;
		private final boolean secure;

		public CustomSecurityContext(String username, Set<String> roles,
				String authScheme, boolean secure) {
			this.principal = () -> {
				return username;
			};
			this.roles = roles;
			this.authScheme = authScheme;
			this.secure = secure;
		}

		@Override
		public Principal getUserPrincipal() {
			return principal;
		}

		@Override
		public boolean isUserInRole(String role) {
			return roles.contains(role);
		}

		@Override
		public boolean isSecure() {
			return secure;
		}

		@Override
		public String getAuthenticationScheme() {
			return authScheme;
		}

	}

}
