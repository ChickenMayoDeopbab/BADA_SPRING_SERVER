package ChickenMayoDeopbab.bada.domain.file.service;

import ChickenMayoDeopbab.bada.domain.file.dto.response.FileUploadResponse;
import ChickenMayoDeopbab.bada.domain.file.entity.File;
import ChickenMayoDeopbab.bada.domain.file.enumeration.FileType;
import ChickenMayoDeopbab.bada.domain.file.exception.FileStatusCode;
import ChickenMayoDeopbab.bada.domain.file.repository.FileRepository;
import ChickenMayoDeopbab.bada.domain.legalconsent.service.ProfileImageConsentPolicy;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.exception.UsersStatusCode;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
import java.util.List;
import java.util.UUID;

@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
@Slf4j
public class FileService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final FileRepository fileRepository;
    private final UsersRepository usersRepository;
    private final ProfileImageConsentPolicy profileImageConsentPolicy;

    @Value("${aws.s3.bucket}")
    private String bucket;

    private static final Duration PRESIGNED_URL_EXPIRATION = Duration.ofMinutes(10);
    private static final long MAX_NON_RINGTONE_FILE_SIZE = 1024L * 1024;

    public FileUploadResponse upload(MultipartFile multipartFile, FileType fileType) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw ApplicationException.of(FileStatusCode.EMPTY_FILE);
        }
        // 벨소리 전송 상한을 높여도 기존 파일 유형의 1MB 정책은 유지한다.
        if (fileType != FileType.RINGTONE && multipartFile.getSize() > MAX_NON_RINGTONE_FILE_SIZE) {
            throw ApplicationException.of(FileStatusCode.UPLOAD_TOO_LARGE);
        }
        if (fileType == FileType.COMMUNITY_IMAGE && !isImage(multipartFile)) {
            throw ApplicationException.of(FileStatusCode.INVALID_IMAGE_TYPE);
        }
        String contentType = fileType == FileType.RINGTONE
                ? RingtoneFileValidator.validate(multipartFile)
                : multipartFile.getContentType();

        Users user = getUserInfo();
        if (fileType == FileType.PROFILE) {
            profileImageConsentPolicy.ensureAgreed(user);
        }
        String originalFilename = multipartFile.getOriginalFilename();
        String s3Key = generateS3Key(fileType);

        boolean rollbackCleanup = fileType == FileType.RINGTONE && registerRingtoneRollbackCleanup(s3Key);
        try {
            putObject(multipartFile, s3Key, contentType);
            File file = fileRepository.save(File.builder()
                    .title(originalFilename)
                    .fileType(fileType)
                    .s3Key(s3Key)
                    .userId(user.getUserId())
                    .build());

            String url = generatePresignedUrl(s3Key);
            return FileUploadResponse.of(file, url);
        } catch (RuntimeException exception) {
            if (fileType == FileType.RINGTONE && !rollbackCleanup) {
                cleanupRingtoneObject(s3Key);
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public String getUrl(String s3Key) {
        File file = fileRepository.findByS3Key(s3Key)
                .orElseThrow(() -> new ApplicationException(FileStatusCode.FILE_NOT_FOUND));
        if (file.getFileType() == FileType.RINGTONE && !file.getUserId().equals(getUserInfo().getUserId())) {
            throw ApplicationException.of(FileStatusCode.FILE_NOT_FOUND);
        }
        return generatePresignedUrl(file.getS3Key());
    }

    public void deleteRingtone(File file) {
        fileRepository.delete(file);
        fileRepository.flush();
        scheduleRingtoneCleanup(file.getS3Key());
    }

    private void scheduleRingtoneCleanup(String s3Key) {
        // DB 롤백 시 재생 가능한 파일이 먼저 사라지지 않도록 커밋 후 S3를 정리한다.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cleanupRingtoneObject(s3Key);
                }
            });
        } else {
            cleanupRingtoneObject(s3Key);
        }
    }

    private boolean registerRingtoneRollbackCleanup(String s3Key) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return false;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    cleanupRingtoneObject(s3Key);
                }
            }
        });
        return true;
    }

    private void cleanupRingtoneObject(String s3Key) {
        try {
            deleteByKey(s3Key);
        } catch (RuntimeException exception) {
            // S3/DB는 원자적으로 커밋할 수 없다. 실패한 key는 운영자가 재정리할 수 있도록 남긴다.
            log.error("벨소리 S3 파일 정리 실패: key={}", s3Key, exception);
        }
    }

    public void delete(Long fileId) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> ApplicationException.of(FileStatusCode.FILE_NOT_FOUND));
        if (file.getFileType() == FileType.RINGTONE) {
            deleteRingtone(file);
            return;
        }
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(file.getS3Key())
                .build());
        fileRepository.delete(file);
    }

    public void deleteByKey(String s3Key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .build());
    }

    public void deleteAllByUserId(Long userId) {
        List<File> files = fileRepository.findAllByUserId(userId);
        deleteFiles(files);
    }

    public void deleteProfileFilesByUserId(Long userId) {
        List<File> files = fileRepository.findAllByUserIdAndFileType(userId, FileType.PROFILE);
        deleteFiles(files);
    }

    public void deleteObsoleteProfileFiles(Long userId, String retainedS3Key) {
        List<File> files = fileRepository.findAllByUserIdAndFileType(userId, FileType.PROFILE)
                .stream()
                .filter(file -> !file.getS3Key().equals(retainedS3Key))
                .toList();
        deleteFiles(files);
    }

    private void deleteFiles(List<File> files) {
        for (File file : files) {
            if (file.getFileType() == FileType.RINGTONE) {
                scheduleRingtoneCleanup(file.getS3Key());
            } else {
                deleteByKey(file.getS3Key());
            }
        }
        fileRepository.deleteAll(files);
    }

    public String generatePresignedUrl(String s3Key) {
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

    public byte[] download(String s3Key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .build();
        return s3Client.getObjectAsBytes(request).asByteArray();
    }

    private void putObject(MultipartFile multipartFile, String s3Key, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .contentType(contentType)
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

    private boolean isImage(MultipartFile multipartFile) {
        String contentType = multipartFile.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }

    private Users getUserInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        return usersRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ApplicationException(UsersStatusCode.USER_NOT_FOUND));
    }
}
