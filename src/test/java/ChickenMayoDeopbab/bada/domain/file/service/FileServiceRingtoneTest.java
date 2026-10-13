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
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
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

class FileServiceRingtoneTest {

    private final S3Client s3 = mock(S3Client.class);
    private final S3Presigner presigner = mock(S3Presigner.class);
    private final FileRepository files = mock(FileRepository.class);
    private final UsersRepository users = mock(UsersRepository.class);
    private final FileService service = new FileService(s3, presigner, files, users, mock(ProfileImageConsentPolicy.class));
    private final Users user = mock(Users.class);

    @BeforeEach
    void setUp() throws Exception {
        ReflectionTestUtils.setField(service, "bucket", "ringtone-test");
        when(user.getUserId()).thenReturn(7L);
        when(users.findByUsername("owner")).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("owner", null));
        PresignedGetObjectRequest signed = mock(PresignedGetObjectRequest.class);
        when(signed.url()).thenReturn(URI.create("https://example.com/ringtone.wav").toURL());
        when(presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(signed);
        when(files.save(any(File.class))).thenAnswer(invocation -> {
            File file = invocation.getArgument(0);
            ReflectionTestUtils.setField(file, "id", 12L);
            return file;
        });
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void uploadPersistsOwnerAndNormalizedContentType() throws IOException {
        var response = service.upload(wave(), FileType.RINGTONE);

        ArgumentCaptor<PutObjectRequest> put = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3).putObject(put.capture(), any(RequestBody.class));
        assertThat(put.getValue().contentType()).isEqualTo("audio/wav");
        assertThat(put.getValue().key()).startsWith("ringtone/");
        ArgumentCaptor<File> saved = ArgumentCaptor.forClass(File.class);
        verify(files).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(7L);
        assertThat(saved.getValue().getFileType()).isEqualTo(FileType.RINGTONE);
        assertThat(response.fileId()).isEqualTo(12L);
        assertThat(response.url()).isEqualTo("https://example.com/ringtone.wav");
    }

    @Test
    void genericUploadCannotBypassRingtoneValidation() {
        MockMultipartFile fake = new MockMultipartFile("file", "fake.mp3", "audio/mpeg", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> service.upload(fake, FileType.RINGTONE))
                .isInstanceOf(ApplicationException.class).extracting("statusCode")
                .isEqualTo(FileStatusCode.INVALID_RINGTONE_TYPE);
        verifyNoInteractions(s3, files);
    }

    @Test
    void rollbackAfterUploadDeletesS3Object() throws IOException {
        TransactionSynchronizationManager.initSynchronization();
        var uploaded = service.upload(wave(), FileType.RINGTONE);
        complete(TransactionSynchronization.STATUS_ROLLED_BACK);

        ArgumentCaptor<DeleteObjectRequest> deleted = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3).deleteObject(deleted.capture());
        assertThat(deleted.getValue().key()).isEqualTo(uploaded.s3Key());
    }

    @Test
    void successfulCommitKeepsUploadedObject() throws IOException {
        TransactionSynchronizationManager.initSynchronization();
        service.upload(wave(), FileType.RINGTONE);
        complete(TransactionSynchronization.STATUS_COMMITTED);

        verify(s3, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void saveFailureWithoutSynchronizationStillCleansUploadedObject() throws IOException {
        when(files.save(any(File.class))).thenThrow(new IllegalStateException("database failure"));

        assertThatThrownBy(() -> service.upload(wave(), FileType.RINGTONE))
                .isInstanceOf(IllegalStateException.class).hasMessage("database failure");
        verify(s3).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void failedS3PutDoesNotPersistMetadata() throws IOException {
        when(s3.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(new IllegalStateException("storage failure"));

        assertThatThrownBy(() -> service.upload(wave(), FileType.RINGTONE)).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(files);
        // PUT 응답이 유실되어 실제 객체가 생성되었을 가능성도 정리한다.
        verify(s3).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void genericUrlLookupRejectsOtherUsersRingtone() {
        File file = ringtone(99L);
        when(files.findByS3Key(file.getS3Key())).thenReturn(Optional.of(file));

        assertThatThrownBy(() -> service.getUrl(file.getS3Key()))
                .isInstanceOf(ApplicationException.class).extracting("statusCode")
                .isEqualTo(FileStatusCode.FILE_NOT_FOUND);
        verifyNoInteractions(presigner);
    }

    @Test
    void genericUrlLookupAllowsOwner() {
        File file = ringtone(7L);
        when(files.findByS3Key(file.getS3Key())).thenReturn(Optional.of(file));

        assertThat(service.getUrl(file.getS3Key())).isEqualTo("https://example.com/ringtone.wav");
    }

    @Test
    void deletingRingtoneWaitsForDatabaseCommit() {
        TransactionSynchronizationManager.initSynchronization();
        service.deleteRingtone(ringtone(7L));
        verify(files).flush();
        verifyNoInteractions(s3);

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(s3).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void rolledBackDeletionKeepsObject() {
        TransactionSynchronizationManager.initSynchronization();
        service.deleteRingtone(ringtone(7L));
        complete(TransactionSynchronization.STATUS_ROLLED_BACK);

        verifyNoInteractions(s3);
    }

    @Test
    void databaseDeleteFailureKeepsObject() {
        doThrow(new IllegalStateException("delete failure")).when(files).flush();

        assertThatThrownBy(() -> service.deleteRingtone(ringtone(7L))).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(s3);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {99L})
    void directRingtoneDeletionRejectsOtherOrMissingOwner(Long ownerId) {
        assertThatThrownBy(() -> service.deleteRingtone(ringtone(ownerId)))
                .isInstanceOf(ApplicationException.class).extracting("statusCode")
                .isEqualTo(FileStatusCode.FILE_NOT_FOUND);
        verifyNoInteractions(files, s3);
    }

    @ParameterizedTest
    @EnumSource(value = FileType.class, names = "RINGTONE", mode = EnumSource.Mode.EXCLUDE)
    void directRingtoneDeletionRejectsOtherFileTypesEvenForOwner(FileType type) {
        File file = File.builder().title("owned-file").fileType(type).userId(7L).s3Key("other/example").build();

        assertThatThrownBy(() -> service.deleteRingtone(file))
                .isInstanceOf(ApplicationException.class).extracting("statusCode")
                .isEqualTo(FileStatusCode.FILE_NOT_FOUND);
        verifyNoInteractions(files, s3);
    }

    @Test
    void genericDeletionCannotBypassRingtoneOwnershipCheck() {
        when(files.findById(12L)).thenReturn(Optional.of(ringtone(99L)));

        assertThatThrownBy(() -> service.delete(12L))
                .isInstanceOf(ApplicationException.class).extracting("statusCode")
                .isEqualTo(FileStatusCode.FILE_NOT_FOUND);
        verify(files, never()).delete(any(File.class));
        verify(files, never()).flush();
        verifyNoInteractions(s3);
    }

    private MockMultipartFile wave() throws IOException {
        return new MockMultipartFile("file", "ring.wav", "application/octet-stream", RingtoneAudioFixtures.wave());
    }

    private File ringtone(Long owner) {
        return File.builder().title("ring.wav").fileType(FileType.RINGTONE).userId(owner).s3Key("ringtone/example").build();
    }

    private void complete(int status) {
        TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCompletion(status));
    }
}
