package edu.pitt.sis.cn3.rest.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import edu.pitt.sis.cn3.db.CN3DatabaseApplication;

/**
 *
 * Feb 17, 2017 12:34:27 AM
 *
 * @author Chirayu Kong Wongchokprasitti (chw20@pitt.edu) 
 */
@SpringBootApplication
@Import({CN3DatabaseApplication.class})
public class CN3RestApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(CN3RestApiApplication.class, args);
	}

}
