package edu.pitt.sis.cn3.rest.api.dto;

import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * Feb 19, 2017 3:26:43 PM
 *
 * @author Chirayu (Kong) Wongchokprasitti, PhD (chw20@pitt.edu)
 */
@XmlRootElement(name = "jobInfo")
@XmlAccessorType(XmlAccessType.FIELD)
public class JobInfoDTO {

    @XmlElement
    private Long id;

    @XmlElement
    private Long targetUserId;
    
    @XmlElement
    private int status;

    @XmlElement
    private Date addedTime;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getTargetUserId() {
		return targetUserId;
	}

	public void setTargetUserId(Long targetUserId) {
		this.targetUserId = targetUserId;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public Date getAddedTime() {
		return addedTime;
	}

	public void setAddedTime(Date addedTime) {
		this.addedTime = addedTime;
	}


}
