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
package edu.pitt.sis.cn3.rest.api.filter;

import java.io.IOException;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;

import org.springframework.beans.factory.annotation.Autowired;

import edu.pitt.sis.cn3.rest.api.service.AuthFilterService;

/**
 *
 * Jun 5, 2016 10:52:12 PM
 *
 * @author Kevin V. Bui (kvb2@pitt.edu)
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthFilter implements ContainerRequestFilter {

	private final AuthFilterService authFilterService;

	@Autowired
	public AuthFilter(AuthFilterService authFilterService) {
		this.authFilterService = authFilterService;
	}

	// https://jersey.java.net/documentation/latest/filters-and-interceptors.html
	@Override
	public void filter(ContainerRequestContext requestContext)
			throws IOException {
		String method = requestContext.getMethod();
		String path = requestContext.getUriInfo().getPath(true);
		// No auth needed to see the WADL
		if (method.equals("GET") && path.equals("application.wadl")) {
			return;
		}

		// We'll only use basic auth for API sign in to get the JWT
		// and all other following requests will be handled by JWT verification
		if (method.equals("GET") && path.equals("jwt")) {
			authFilterService.verifyBasicAuth(requestContext);
		} else {
			authFilterService.verifyJwt(requestContext);
		}
	}

}
