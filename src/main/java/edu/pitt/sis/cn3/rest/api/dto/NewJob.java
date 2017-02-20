package edu.pitt.sis.cn3.rest.api.dto;

import javax.validation.constraints.NotNull;

/**
 *
 * Feb 19, 2017 4:11:38 PM
 *
 * @author kong
 */
public class NewJob {

	@NotNull
	private Long targetUserId;

	public Long getTargetUserId() {
		return targetUserId;
	}

	public void setTargetUserId(Long targetUserId) {
		this.targetUserId = targetUserId;
	}

}
