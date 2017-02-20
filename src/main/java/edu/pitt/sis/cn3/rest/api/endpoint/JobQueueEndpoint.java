package edu.pitt.sis.cn3.rest.api.endpoint;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;

import java.io.IOException;
import java.util.List;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.pitt.sis.cn3.rest.api.Role;
import edu.pitt.sis.cn3.rest.api.dto.JobInfoDTO;
import edu.pitt.sis.cn3.rest.api.dto.NewJob;
import edu.pitt.sis.cn3.rest.api.service.JobQueueEndpointService;

/**
 *
 * Feb 17, 2017 1:07:19 AM
 *
 * @author Chirayu (Kong) Wongchokprasitti, PhD (chw20@pitt.edu)
 */
@Component
@PermitAll
@Path("/{uid}")
public class JobQueueEndpoint {

	private final JobQueueEndpointService jobQueueEndpointService;
	
	@Autowired
	public JobQueueEndpoint(JobQueueEndpointService jobQueueEndpointService) {
		this.jobQueueEndpointService = jobQueueEndpointService;
	}

	@POST
    @Path("/jobs")
    @Consumes(APPLICATION_JSON)
    @Produces({APPLICATION_JSON, APPLICATION_XML})
    @RolesAllowed({Role.USER,Role.ADMIN})
	public Response addNewRecommendationJob(@PathParam("uid") Long uid, @Valid NewJob newJob) throws IOException {
		JobInfoDTO jobInfo = jobQueueEndpointService.addNewRecommendationRequestJob(uid, newJob.getTargetUserId());
		GenericEntity<JobInfoDTO> jobRequestEntity = new GenericEntity<JobInfoDTO>(jobInfo) {};
		return Response.status(Status.CREATED).entity(jobRequestEntity).build();
	}
	
	@GET
	@Path("/jobs")
    @Consumes(APPLICATION_JSON)
    @Produces({APPLICATION_JSON, APPLICATION_XML})
    @RolesAllowed({Role.USER,Role.ADMIN})
	public Response listAllJobQueues(@PathParam("uid") Long uid) throws IOException {
		List<JobInfoDTO> jobInfos = jobQueueEndpointService.listAllJobQueues(uid);
		GenericEntity<List<JobInfoDTO>> entity = new GenericEntity<List<JobInfoDTO>>(jobInfos) {
        };

        return Response.ok(entity).build();
	}
	
	@GET
	@Path("/jobs/{id}")
    @Consumes(APPLICATION_JSON)
    @Produces({APPLICATION_JSON, APPLICATION_XML})
    @RolesAllowed({Role.USER,Role.ADMIN})
	public Response jobStatus(@PathParam("uid") Long uid, @PathParam("id") Long id) throws IOException {
		JobInfoDTO jobInfo = jobQueueEndpointService.jobStatus(uid, id);
		GenericEntity<JobInfoDTO> entity = new GenericEntity<JobInfoDTO>(jobInfo) {
        };
        return Response.ok(entity).build();
	}
	
	@DELETE
    @Path("/jobs/{id}")
    @RolesAllowed({Role.USER,Role.ADMIN})
    public Response cancelJob(@PathParam("uid") Long uid, @PathParam("id") Long id) throws IOException {
		boolean canceled = jobQueueEndpointService.cancelJob(uid, id);

        if (canceled) {
            return Response.ok("Job " + id + " has been canceled").build();
        } else {
            return Response.ok("Unable to cancel job " + id).build();
        }
	}
}
