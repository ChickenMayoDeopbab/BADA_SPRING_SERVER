package ChickenMayoDeopbab.bada.domain.file.service;

import ChickenMayoDeopbab.bada.domain.file.entity.File;
import ChickenMayoDeopbab.bada.domain.file.enumeration.FileType;
import ChickenMayoDeopbab.bada.domain.file.exception.FileStatusCode;
import ChickenMayoDeopbab.bada.domain.file.repository.FileRepository;
import ChickenMayoDeopbab.bada.domain.legalconsent.service.ProfileImageConsentPolicy;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FileServiceUploadLimitTest {

    private final S3Client s3 = mock(S3Client.class);
    private final S3Presigner presigner = mock(S3Presigner.class);
    private final FileRepository files = mock(FileRepository.class);
    private final UsersRepository users = mock(UsersRepository.class);
    private final FileService service = new FileService(s3, presigner, files, users, mock(ProfileImageConsentPolicy.class));

    @BeforeEach
    void setUp() throws Exception {
        ReflectionTestUtils.setField(service, "bucket", "upload-limit-test");
        Users user = mock(Users.class);
        when(user.getUserId()).thenReturn(7L);
        when(users.findByUsername("owner")).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("owner", null));
        when(files.save(any(File.class))).thenAnswer(invocation -> invocation.getArgument(0));
        PresignedGetObjectRequest signed = mock(PresignedGetObjectRequest.class);
        when(signed.url()).thenReturn(URI.create("https://example.com/uploaded-file").toURL());
        when(presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(signed);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @EnumSource(value = FileType.class, names = "RINGTONE", mode = EnumSource.Mode.EXCLUDE)
    void rejectsNonRingtoneFilesAboveOneMegabyteBeforeStorage(FileType type) throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn(1024L * 1024 + 1);

        assertThatThrownBy(() -> service.upload(file, type))
                .isInstanceOf(ApplicationException.class).extracting("statusCode")
                .isEqualTo(FileStatusCode.UPLOAD_TOO_LARGE);
        verifyNoInteractions(s3, files);
        verify(file, never()).getInputStream();
    }

    @ParameterizedTest
    @EnumSource(value = FileType.class, names = "RINGTONE", mode = EnumSource.Mode.EXCLUDE)
    void acceptsNonRingtoneFilesAtOneMegabyte(FileType type) {
        MockMultipartFile file = new MockMultipartFile("file", "upload.png", "image/png", new byte[1024 * 1024]);

        service.upload(file, type);

        ArgumentCaptor<PutObjectRequest> put = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3).putObject(put.capture(), any(RequestBody.class));
        assertThat(put.getValue().contentLength()).isEqualTo(1024L * 1024);
        verify(files).save(argThat(saved -> saved.getFileType() == type));
    }

    @ParameterizedTest
    @ValueSource(ints = {1024 * 1024, 10 * 1024 * 1024 - 44})
    void keepsRingtoneAllowanceAboveOneMegabyteThroughTenMegabytes(int pcmByteCount) throws IOException {
        byte[] wave = RingtoneAudioFixtures.wave(pcmByteCount);
        MockMultipartFile file = new MockMultipartFile("file", "ring.wav", "audio/wav", wave);

        service.upload(file, FileType.RINGTONE);

        assertThat(wave.length).isGreaterThan(1024 * 1024).isLessThanOrEqualTo(10 * 1024 * 1024);
        ArgumentCaptor<PutObjectRequest> put = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3).putObject(put.capture(), any(RequestBody.class));
        assertThat(put.getValue().contentLength()).isEqualTo((long) wave.length);
    }

    @Test
    void rejectsRingtoneAboveTenMegabytesWithRingtoneErrorCode() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn(10L * 1024 * 1024 + 1);

        assertThatThrownBy(() -> service.upload(file, FileType.RINGTONE))
                .isInstanceOf(ApplicationException.class).extracting("statusCode")
                .isEqualTo(FileStatusCode.RINGTONE_TOO_LARGE);
        verifyNoInteractions(s3, files);
    }
}
