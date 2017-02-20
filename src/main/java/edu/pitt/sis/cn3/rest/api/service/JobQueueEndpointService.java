package edu.pitt.sis.cn3.rest.api.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.pitt.sis.cn3.db.entity.JobQueueInfo;
import edu.pitt.sis.cn3.db.entity.UserInfo;
import edu.pitt.sis.cn3.db.service.JobQueueInfoService;
import edu.pitt.sis.cn3.db.service.UserInfoService;
import edu.pitt.sis.cn3.rest.api.dto.JobInfoDTO;

/**
 *
 * Feb 17, 2017 1:11:39 AM
 *
 * @author Chirayu Kong Wongchokprasitti (chw20@pitt.edu)
 */
@Service
public class JobQueueEndpointService {

	private static final Logger LOGGER = LoggerFactory.getLogger(JobQueueEndpointService.class);
	
	private final UserInfoService userInfoService;
	
	private final JobQueueInfoService jobQueueInfoService;

	@Autowired
	public JobQueueEndpointService(UserInfoService userInfoService, JobQueueInfoService jobQueueInfoService) {
		this.userInfoService = userInfoService;
		this.jobQueueInfoService = jobQueueInfoService;
	}

	
	public JobInfoDTO addNewRecommendationRequestJob(Long ownerId, Long targetUserId){
		UserInfo userInfo = userInfoService.findById(ownerId);
		UserInfo targetUserInfo = userInfoService.findById(targetUserId);
		
		JobQueueInfo jobQueueInfo = new JobQueueInfo();
		jobQueueInfo.setAddedTime(new Date(System.currentTimeMillis()));
		jobQueueInfo.setStatus(0);
		jobQueueInfo.setOwners(Collections.singleton(userInfo));
		jobQueueInfo.setTargetUsers(Collections.singleton(targetUserInfo));
		
		jobQueueInfo = jobQueueInfoService.saveJobIntoQueue(jobQueueInfo);
		
		Long newJobId = jobQueueInfo.getId();

        LOGGER.info(String.format("New recommendation request job submitted. Job ID: %d", newJobId));

        JobInfoDTO jobInfo = new JobInfoDTO();
        jobInfo.setAddedTime(jobQueueInfo.getAddedTime());
        jobInfo.setId(newJobId);
        jobInfo.setStatus(jobQueueInfo.getStatus());
        jobInfo.setTargetUserId(targetUserId);
        
		return jobInfo;
	}
	
	public List<JobInfoDTO> listAllJobQueues(Long ownerId){
		UserInfo userInfo = userInfoService.findById(ownerId);
		List<JobQueueInfo> jobQueueInfos = jobQueueInfoService.findByOwners(Collections.singleton(userInfo));
		
		List<JobInfoDTO> jobInfos = new ArrayList<>();
		jobQueueInfos.forEach(jobQueueInfo -> {
			JobInfoDTO jobInfo = new JobInfoDTO();
			jobInfo.setAddedTime(jobQueueInfo.getAddedTime());
			jobInfo.setId(jobQueueInfo.getId());
	        jobInfo.setStatus(jobQueueInfo.getStatus());
	        jobInfo.setTargetUserId(jobQueueInfo.getTargetUsers().iterator().next().getId());
	        jobInfos.add(jobInfo);
		});
		
		return jobInfos;
	}
	
	public JobInfoDTO jobStatus(Long ownerId, Long jobId){
		UserInfo userInfo = userInfoService.findById(ownerId);
		JobQueueInfo jobQueueInfo = jobQueueInfoService.findByIdAndOwners(jobId, userInfo);
		JobInfoDTO jobInfo = new JobInfoDTO();
        jobInfo.setAddedTime(jobQueueInfo.getAddedTime());
        jobInfo.setId(jobQueueInfo.getId());
        jobInfo.setStatus(jobQueueInfo.getStatus());
        jobInfo.setTargetUserId(jobQueueInfo.getTargetUsers().iterator().next().getId());
        
		return jobInfo;
	}
	
	public boolean cancelJob(Long ownerId, Long jobId){
		UserInfo userInfo = userInfoService.findById(ownerId);
		JobQueueInfo jobQueueInfo = jobQueueInfoService.findByIdAndOwners(jobId, userInfo);
		boolean success = jobQueueInfoService.deleteJobInQueue(jobQueueInfo);
		return success;
	}
}
