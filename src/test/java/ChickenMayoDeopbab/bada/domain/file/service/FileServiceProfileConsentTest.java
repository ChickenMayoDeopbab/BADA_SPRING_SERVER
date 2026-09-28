package ChickenMayoDeopbab.bada.domain.file.service;

import ChickenMayoDeopbab.bada.domain.file.enumeration.FileType;
import ChickenMayoDeopbab.bada.domain.file.repository.FileRepository;
import ChickenMayoDeopbab.bada.domain.legalconsent.exception.LegalConsentStatusCode;
import ChickenMayoDeopbab.bada.domain.legalconsent.service.ProfileImageConsentPolicy;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FileServiceProfileConsentTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void profileUploadStopsBeforeS3WhenConsentIsMissing() {
        S3Client s3Client = mock(S3Client.class);
        FileRepository fileRepository = mock(FileRepository.class);
        UsersRepository usersRepository = mock(UsersRepository.class);
        ProfileImageConsentPolicy policy = mock(ProfileImageConsentPolicy.class);
        Users user = mock(Users.class);
        MultipartFile multipartFile = mock(MultipartFile.class);

        when(multipartFile.isEmpty()).thenReturn(false);
        when(usersRepository.findByUsername("tester")).thenReturn(Optional.of(user));
        doThrow(new ApplicationException(LegalConsentStatusCode.PROFILE_IMAGE_CONSENT_REQUIRED))
                .when(policy).ensureAgreed(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("tester", null)
        );

        FileService service = new FileService(
                s3Client,
                mock(S3Presigner.class),
                fileRepository,
                usersRepository,
                policy
        );

        assertThatThrownBy(() -> service.upload(multipartFile, FileType.PROFILE))
                .isInstanceOf(ApplicationException.class)
                .extracting("statusCode")
                .isEqualTo(LegalConsentStatusCode.PROFILE_IMAGE_CONSENT_REQUIRED);

        verifyNoInteractions(s3Client, fileRepository);
    }
}
