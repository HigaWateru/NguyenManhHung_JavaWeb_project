package demo.project.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {
    private String cloudName = "dxkw3nupk";
    private String apiKey = "628636577581966";
    private String apiSecret = "rFh3fRsPzKxdhDax-FH-mHhmOmc";

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap("cloud_name", cloudName, "api_key", apiKey, "api_secret", apiSecret));
    }
}