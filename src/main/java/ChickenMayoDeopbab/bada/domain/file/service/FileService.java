package ChickenMayoDeopbab.bada.domain.file.service;

import ChickenMayoDeopbab.bada.domain.file.dto.response.FileUploadResponse;
import ChickenMayoDeopbab.bada.domain.file.entity.File;
import ChickenMayoDeopbab.bada.domain.file.enumeration.FileType;
import ChickenMayoDeopbab.bada.domain.file.exception.FileStatusCode;
import ChickenMayoDeopbab.bada.domain.file.repository.FileRepository;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class FileService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final FileRepository fileRepository;

    @Value("${aws.s3.bucket}")
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
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(file.getS3Key())
                .build());
        fileRepository.delete(file);
    }

    private String generatePresignedUrl(String s3Key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(PRESIGNED_URL_EXPIRATION)
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    private void putObject(MultipartFile multipartFile, String s3Key) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .contentType(multipartFile.getContentType())
                .contentLength(multipartFile.getSize())
                .build();

        try {
            s3Client.putObject(request,
                    RequestBody.fromInputStream(multipartFile.getInputStream(), multipartFile.getSize()));
        } catch (IOException e) {
            throw ApplicationException.of(FileStatusCode.FILE_UPLOAD_FAILED, e);
        }
    }

    private String generateS3Key(FileType fileType) {
        String directory = fileType.name().toLowerCase();
        return directory + "/" + UUID.randomUUID();
    }
}