package it.aredegalli.coachly.workout;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "it.aredegalli.coachly")
public class CoachlyWorkoutsBeApplication {

	public static void main(String[] args) {
		SpringApplication.run(CoachlyWorkoutsBeApplication.class, args);
	}

}
