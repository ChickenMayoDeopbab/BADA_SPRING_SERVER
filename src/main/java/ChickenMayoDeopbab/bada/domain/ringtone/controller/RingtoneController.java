package ChickenMayoDeopbab.bada.domain.ringtone.controller;

import ChickenMayoDeopbab.bada.domain.ringtone.dto.response.RingtoneResponse;
import ChickenMayoDeopbab.bada.domain.ringtone.dto.request.UpdateRingtoneSettingRequest;
import ChickenMayoDeopbab.bada.domain.ringtone.dto.response.RingtoneSettingResponse;
import ChickenMayoDeopbab.bada.domain.ringtone.service.RingtoneService;
import ChickenMayoDeopbab.bada.global.common.ApiResponse;
import jakarta.validation.constraints.Positive;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ringtones")
@RequiredArgsConstructor
@Validated
public class RingtoneController {

    private final RingtoneService ringtoneService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RingtoneResponse> upload(@RequestParam("file") MultipartFile file) {
        return ApiResponse.created(ringtoneService.upload(file), "벨소리가 업로드되었습니다.");
    }

    @GetMapping
    public ApiResponse<List<RingtoneResponse>> list() {
        return ApiResponse.ok(ringtoneService.list());
    }

    @GetMapping("/settings")
    public ApiResponse<RingtoneSettingResponse> getSetting() {
        return ApiResponse.ok(ringtoneService.getSetting());
    }

    @PutMapping("/settings")
    public ApiResponse<RingtoneSettingResponse> updateSetting(@Valid @RequestBody UpdateRingtoneSettingRequest request) {
        return ApiResponse.ok(ringtoneService.updateSetting(request), "벨소리 설정이 저장되었습니다.");
    }

    @GetMapping("/{fileId}")
    public ApiResponse<RingtoneResponse> get(@PathVariable @Positive Long fileId) {
        return ApiResponse.ok(ringtoneService.get(fileId));
    }

    @DeleteMapping("/{fileId}")
    public ApiResponse<Void> delete(@PathVariable @Positive Long fileId) {
        ringtoneService.delete(fileId);
        return ApiResponse.ok("벨소리가 삭제되었습니다.");
    }
}
