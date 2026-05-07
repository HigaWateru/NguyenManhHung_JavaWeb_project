package demo.project.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UploadService {
    private final Cloudinary cloudinary;

    public String upload(MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename();

            if(fileName != null && fileName.contains(".")) fileName = fileName.substring(0, fileName.lastIndexOf("."));

            Map uploadParams = ObjectUtils.asMap("public_id", fileName, "resource_type", "auto");

            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);

            return uploadResult.get("url").toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
