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

import com.auth0.jwt.JWTSigner;

import edu.pitt.sis.cn3.db.entity.UserInfo;
import edu.pitt.sis.cn3.db.service.UserInfoService;
import edu.pitt.sis.cn3.rest.api.dto.JwtDTO;

import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 *
 * @author Zhou Yuan (zhy19@pitt.edu)
 */
@Service
public class JwtEndpointService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtEndpointService.class);

    public static final String AUTH_SCHEME_BASIC = "Basic";

    private final UserInfoService userInfoService;

    private final long jwtLiftTime;
    
    private final String jwtSecret;
    
    private final String jwtIssuer;
    
    @Autowired
    public JwtEndpointService(UserInfoService userInfoService, 
    		@Value("${cn3.jwt.lifetime}") long jwtLiftTime,
    		@Value("${cn3.jwt.secret}") String jwtSecret,
    		@Value("${cn3.jwt.issuer}") String jwtIssuer) {
        this.userInfoService = userInfoService;
        this.jwtLiftTime = jwtLiftTime;
        this.jwtSecret = jwtSecret;
        this.jwtIssuer = jwtIssuer;
    }

    public JwtDTO generateJwt(String authString) {
        // Parse the email from the authString
        // "\\s+" will cause any number of consecutive spaces to split the string into tokens
        // Here we split the auth string by space(s) and the second part is the base64 encoded
        String authCredentialBase64 = authString.split("\\s+")[1];
        String credentials = new String(Base64.getDecoder().decode(authCredentialBase64));
        StringTokenizer tokenizer = new StringTokenizer(credentials, ":");
        String email = tokenizer.nextToken();

        // When we can get here vai AuthFilterSerice, it means the user exists
        // so no need to check if (userAccount == null) and throw UserNotFoundException(uid)
        UserInfo userInfo = userInfoService.findByEmail(email);

        // Note this uid is Long object, we'll need to use the numeric primitive long
        // to store it into JWT claims
        Long uid = userInfo.getId();

        // Generate JWT (JSON Web Token, for API authentication)
        // Each jwt is issued at claim (per API request)
        // When refresh the request, the new jwt will overwrite the old one
        // Using Java 8 time API
        Instant iatInstant = Instant.now();
        // The token expires in 3600 seconds (1 hour)
        Instant expInstant = iatInstant.plusSeconds(jwtLiftTime);
        Date iatDate = Date.from(iatInstant);
        Date expDate = Date.from(expInstant);

        // Sign the token with secret
        JWTSigner signer = new JWTSigner(jwtSecret);

        // JWT claims
        HashMap<String, Object> claims = new HashMap<>();
        // Add reserved claims
        claims.put("iss", jwtIssuer);
        // Convert iatDate and expDate into long primitive
        claims.put("iat", iatDate.getTime());
        claims.put("exp", expDate.getTime());
        // Private/custom claim
        claims.put("uid", uid);

        // Generate the token string
        String jwt = signer.sign(claims);

        // We store this JWT into `public_key` field of the user account table
        userInfo.setPublicKey(jwt);
        userInfoService.save(userInfo);

        LOGGER.info(String.format("Added JWT for user id %d", uid));

        // Return the jwt to API consumer
        JwtDTO jwtDTO = new JwtDTO();
        jwtDTO.setUserId(uid);
        jwtDTO.setJwt(jwt);
        jwtDTO.setIssuedTime(iatDate);
        jwtDTO.setLifetime(jwtLiftTime);
        jwtDTO.setExpireTime(expDate);

        return jwtDTO;
    }

}
