package ChickenMayoDeopbab.bada.domain.file.service;

import ChickenMayoDeopbab.bada.domain.file.dto.response.FileUploadResponse;
import ChickenMayoDeopbab.bada.domain.file.entity.File;
import ChickenMayoDeopbab.bada.domain.file.enumeration.FileType;
import ChickenMayoDeopbab.bada.domain.file.exception.FileStatusCode;
import ChickenMayoDeopbab.bada.domain.file.repository.FileRepository;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class FileService {

    private final AmazonS3Client amazonS3Client;
    private final FileRepository fileRepository;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    private static final Duration PRESIGNED_URL_EXPIRATION = Duration.ofMinutes(10);

    public FileUploadResponse upload(MultipartFile multipartFile, FileType fileType) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw ApplicationException.of(FileStatusCode.EMPTY_FILE);
        }

        String originalFilename = multipartFile.getOriginalFilename();
        String s3Key = generateS3Key(fileType);

        putObject(multipartFile, s3Key);

        File file = fileRepository.save(File.builder()
                .title(originalFilename)
                .fileType(fileType)
                .s3Key(s3Key)
                .build());

        String url = generatePresignedUrl(s3Key);
        return FileUploadResponse.of(file, url);
    }

    @Transactional(readOnly = true)
    public String getUrl(Long fileId) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> ApplicationException.of(FileStatusCode.FILE_NOT_FOUND));
        return generatePresignedUrl(file.getS3Key());
    }

    public void delete(Long fileId) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> ApplicationException.of(FileStatusCode.FILE_NOT_FOUND));
        amazonS3Client.deleteObject(bucket, file.getS3Key());
        fileRepository.delete(file);
    }

    private String generatePresignedUrl(String s3Key) {
        Date expiration = new Date(System.currentTimeMillis() + PRESIGNED_URL_EXPIRATION.toMillis());

        GeneratePresignedUrlRequest request =
                new GeneratePresignedUrlRequest(bucket, s3Key)
                        .withMethod(HttpMethod.GET)
                        .withExpiration(expiration);

        return amazonS3Client.generatePresignedUrl(request).toString();
    }

    private void putObject(MultipartFile multipartFile, String s3Key) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(multipartFile.getContentType());
        metadata.setContentLength(multipartFile.getSize());

        try {
            amazonS3Client.putObject(bucket, s3Key, multipartFile.getInputStream(), metadata);
        } catch (IOException e) {
            throw ApplicationException.of(FileStatusCode.FILE_UPLOAD_FAILED, e);
        }
    }

    private String generateS3Key(FileType fileType) {
        String directory = fileType.name().toLowerCase();
        return directory + "/" + UUID.randomUUID();
    }
}