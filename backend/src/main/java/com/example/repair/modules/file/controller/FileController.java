package com.example.repair.modules.file.controller;

import com.example.repair.common.api.ApiResponse;
import com.example.repair.common.exception.BizException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

  @Value("${app.upload.dir:./uploads}")
  private String uploadDir;

  @PostMapping("/upload")
  public ApiResponse<String> upload(@RequestParam("file") MultipartFile file) throws IOException {
    if (file.isEmpty()) throw new BizException("Empty file");
    String original = file.getOriginalFilename();
    String ext = "";
    if (StringUtils.hasText(original) && original.contains(".")) {
      ext = original.substring(original.lastIndexOf('.'));
    }
    String name = UUID.randomUUID().toString().replace("-", "") + ext;
    Path dir = Paths.get(uploadDir);
    Files.createDirectories(dir);
    Path target = dir.resolve(name);
    file.transferTo(target);
    return ApiResponse.ok("/uploads/" + name);
  }
}

