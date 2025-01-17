package com.css.coupon_sale.controller;


import com.css.coupon_sale.dto.request.BusinessRequest;
import com.css.coupon_sale.dto.request.SignupRequest;
import com.css.coupon_sale.dto.request.UpdateBusinessRequest;
import com.css.coupon_sale.dto.response.BusinessResponse;
import com.css.coupon_sale.dto.response.HttpResponse;
import com.css.coupon_sale.dto.response.SignupResponse;
import com.css.coupon_sale.entity.BusinessEntity;
import com.css.coupon_sale.entity.UserEntity;
import com.css.coupon_sale.service.BusinessService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/businesses")
public class BusinessController {

    @Autowired
    private BusinessService businessService;

    @Autowired
    private ModelMapper modelMapper;


    @PostMapping("/add/owner")
    public ResponseEntity<?> signupUser(@RequestBody SignupRequest request){
        System.out.println("In REQ:" + request.toString());
        SignupResponse response = businessService.addBusinessOwner(request);
        if (response != null){
            HttpResponse<SignupResponse> res = new HttpResponse<>();
            res.setStatus(true);
            res.setMessage("User Created Successfully");
            res.setData(response);
            return ResponseEntity.status(HttpStatus.CREATED).body(res);
        }else return  ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Fail to Add User");
    }
    @PostMapping
    public ResponseEntity<?> createBusiness(
            @RequestParam("name") String name,
            @RequestParam("location") String location,
            @RequestParam("description") String description,
            @RequestParam("contactNumber") String contactNumber,
            @RequestParam("categoryId") Integer categoryId,
            @RequestParam("userId") Long userId,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        System.out.println("Name"+ name);
        System.out.println("Category ID: " + categoryId);
        try {
            BusinessRequest dto = new BusinessRequest();
            dto.setName(name);
            dto.setLocation(location);
            dto.setDescription(description);
            dto.setContactNumber(contactNumber);
            dto.setCategoryId(categoryId);
            dto.setUserId(userId);
            dto.setImage(image);

            BusinessResponse responseDto = businessService.createBusiness(dto);

            return ResponseEntity.ok(responseDto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating business: " + e.getMessage());
        }
    }


    @GetMapping("/user/{id}")
    public ResponseEntity<BusinessResponse> getBusinessByUserId(@PathVariable Long id) {
        try {
            BusinessResponse response = businessService.getByUserId(id);
            return ResponseEntity.ok(response);
        }catch (Exception e){
            System.out.println("Error"+ e.getMessage());
            return ResponseEntity.ok(new BusinessResponse());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<BusinessResponse> getBusinessById(@PathVariable Integer id) {
        BusinessResponse business = businessService.getBusinessById(id);
        return ResponseEntity.ok(business);
    }

    @GetMapping
    public ResponseEntity<List<BusinessResponse>> getAllBusinesses() {
        List<BusinessResponse> businesses = businessService.getAllBusinesses();
        return ResponseEntity.ok(businesses);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BusinessResponse> updateBusiness(@PathVariable("id") Integer id, @ModelAttribute UpdateBusinessRequest requestDTO) throws IOException{
        BusinessResponse updatedBusiness = businessService.updateBusiness(id, requestDTO);
        return ResponseEntity.ok(updatedBusiness);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteBusiness(@PathVariable Integer id) {
        BusinessResponse b = businessService.softDeleteBusiness(id);
        return ResponseEntity.ok("Business deleted successfully");
    }


}
