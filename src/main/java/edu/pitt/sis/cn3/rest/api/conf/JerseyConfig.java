package edu.pitt.sis.cn3.rest.api.conf;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.springframework.stereotype.Component;

import edu.pitt.sis.cn3.rest.api.endpoint.JwtEndpoint;
import edu.pitt.sis.cn3.rest.api.endpoint.JobQueueEndpoint;
import edu.pitt.sis.cn3.rest.api.exception.mapper.WebApplicationExceptionMapper;
import edu.pitt.sis.cn3.rest.api.filter.AuthFilter;
import edu.pitt.sis.cn3.rest.api.filter.CORSFilter;

/**
 *
 * Feb 17, 2017 12:59:16 AM
 *
 * @author Chirayu Kong Wongchokprasitti (chw20@pitt.edu)
 */
@Component
public class JerseyConfig extends ResourceConfig {

	public JerseyConfig() {
		register(JwtEndpoint.class);
		register(JobQueueEndpoint.class);
		
		// Register exception mapper
        register(WebApplicationExceptionMapper.class);

        //Register filters
        register(AuthFilter.class);
        register(CORSFilter.class);
		
        register(RolesAllowedDynamicFeature.class);

        // By default, Jersey doesn't return any entities that would include validation errors to the client.
        // Enable Jersey bean validation errors to users
        property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);

	}

}
