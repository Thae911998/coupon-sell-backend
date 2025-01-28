package com.css.coupon_sale.service.implementation;

import com.css.coupon_sale.dto.request.UserProfileRequest;
import com.css.coupon_sale.dto.response.UserListResponse;
import com.css.coupon_sale.dto.response.UserProfileResponse;
import com.css.coupon_sale.entity.UserEntity;
import com.css.coupon_sale.repository.UserRepository;
import com.css.coupon_sale.service.UserProfileService;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class UserProfileServiceImpl implements UserProfileService {
    @Autowired
    private UserRepository userRepository;
    @Value("${product.image.upload-dir}") // Specify folder path in application.properties
    private String uploadDir;

    @Override
    public UserProfileResponse updatebyId(Integer id, UserProfileRequest request) throws IOException {
        // Find user by ID
        UserEntity user = userRepository.findById(Long.valueOf(id))
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));

        // Update user details

        user.setAddress(request.getAddress());
        user.setName(request.getName());
        user.setPhone(request.getPhone());


        // Save the uploaded image
        MultipartFile imageFile = request.getProfile();
        if (imageFile != null && !imageFile.isEmpty()) {
            System.out.println("IMage is exist");
            String fileName = System.currentTimeMillis() + "_" + imageFile.getOriginalFilename();
            Path filePath = Paths.get(uploadDir+"/profile", fileName);
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, imageFile.getBytes());
            user.setProfile(fileName); // Save file name/path
        }
        // Save updated user
        UserEntity updatedUser = userRepository.save(user);

        // Map to response object
        UserProfileResponse response = new UserProfileResponse();
        response.setId(updatedUser.getId());
        response.setName(updatedUser.getName());
        response.setEmail(updatedUser.getEmail());
        response.setRole(updatedUser.getRole());
        response.setProfile(updatedUser.getProfile());
        response.setEnableNoti(updatedUser.getEnable_noti());
        response.setPhone(updatedUser.getPhone());
        response.setAddress(updatedUser.getAddress());
        response.setCreate_at(updatedUser.getCreated_at());

        return response;
    }

    @Override
        public byte[] generateCustomerListReport(List<UserListResponse> userListResponse, String reportType) throws JRException {
        // Load the JasperReport template
        InputStream reportStream = getClass().getResourceAsStream("/user.jrxml");
        JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

        // Log customer responses for debugging
        System.out.println("Customer Responses: " + userListResponse);
        for (UserListResponse u : userListResponse){
            System.out.println("CRE: " + u.getCreated_at());
            System.out.println("Name:" + u.getName());
        }

        // Convert the list of customers to a JRBeanCollectionDataSource
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(userListResponse);

        // Fill the report with data
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, null, dataSource);

        // Export the report to the desired format
        if ("pdf".equalsIgnoreCase(reportType)) {
            return JasperExportManager.exportReportToPdf(jasperPrint);
        } else if ("excel".equalsIgnoreCase(reportType)) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            JRXlsxExporter exporter = new JRXlsxExporter();
            exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
            exporter.exportReport();
            return outputStream.toByteArray();
        } else {
            throw new IllegalArgumentException("Invalid report type");
        }
    }
}
