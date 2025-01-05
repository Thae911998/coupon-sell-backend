package com.css.coupon_sale.controller;

import com.css.coupon_sale.dto.request.OrderItemRequest;
import com.css.coupon_sale.dto.request.OrderRequest;
import com.css.coupon_sale.dto.request.ProductRequest;
import com.css.coupon_sale.dto.response.*;
import com.css.coupon_sale.service.OrderService;
import com.css.coupon_sale.service.ProductService;
import com.css.coupon_sale.service.QrCodeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

  @Value("${product.image.upload-dir}") // Specify folder path in application.properties
  private String uploadDir;

  @Autowired
  private OrderService service;

  @Autowired
  private QrCodeService qrCodeService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> saveOrders(
            @RequestParam("user_id") long userId,
            @RequestParam("payment_id") int paymentId,
            @RequestParam("phoneNumber") String phoneNumber,
            @RequestParam("totalPrice") int totalPrice,
            @RequestParam("quantities") String quantitiesJson,
            @RequestPart("screenshot") MultipartFile screenshot,
            @RequestParam("coupon_ids") String couponIdsJson
    ) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            List<Integer> quantities = Arrays.asList(objectMapper.readValue(quantitiesJson, Integer[].class));

            List<Integer> couponIds = Arrays.asList(
                    objectMapper.readValue(couponIdsJson, Integer[].class)
            );
            // Call the service to save all orders with the same order_id
            List<OrderResponse> responses = service.saveOrders(userId, paymentId, phoneNumber, totalPrice, quantities, screenshot, couponIds);
            if(!responses.isEmpty()){
                return ResponseEntity.ok(responses);
            }
            return ResponseEntity.status(400).build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing order: " + e.getMessage());
        }
    }

    @GetMapping
  public ResponseEntity<List<OrderResponse>> showAllOrder(){
    List<OrderResponse> response =service.getAllOrderlist();
    return  ResponseEntity.ok(response);
  }

  @GetMapping("/o/{id}")
  public ResponseEntity<List<OrderDetailResponse>> getByOrderId(@PathVariable("id")Integer id){
      List<OrderDetailResponse> responses = service.getByOrderId(id);
      if(responses != null){
          return ResponseEntity.ok(responses);
      }
      return ResponseEntity.notFound().build();
  }
    @GetMapping("/accept/{id}")
    public ResponseEntity<Boolean> acceptOrderByOrderId(@PathVariable("id")Integer id){
        boolean response = service.updateOrderStatus(id,"ACCEPT");
        if(response){
            return ResponseEntity.ok(true);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/reject/{id}")
    public ResponseEntity<Boolean> rejectOrderByOrderId(@PathVariable("id")Integer id){
        boolean response = service.updateOrderStatus(id,"REJECT");
        if(response){
            return ResponseEntity.ok(true);
        }
        return ResponseEntity.notFound().build();
    }


  @GetMapping("/p/{id}")
  public ResponseEntity<List<OrderResponse>> getByPayment(@PathVariable("id")Integer id){
    List<OrderResponse> responses = service.getByPaymentId(id);
    if(responses != null){
      return ResponseEntity.ok(responses);
    }
    return ResponseEntity.notFound().build();
  }

  @GetMapping("/user/{id}")
  public ResponseEntity<List<OrderResponse>> getByUserId(@PathVariable int id) {
    return ResponseEntity.ok(service.getByUserId(id));

  }
  @GetMapping("/c/{id}")
  public ResponseEntity<List<OrderResponse>> getByBusiness(@PathVariable("id")Integer id){
    List<OrderResponse> responses = service.getByCouponId(id);
    if(responses != null){
      return ResponseEntity.ok(responses);
    }
    return ResponseEntity.notFound().build();
  }
}
