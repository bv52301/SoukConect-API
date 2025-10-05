package com.souk.vendor.api;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/vendors")
public class VendorController {
    @GetMapping("/{id}")
    public Map<String,Object> one(@PathVariable long id){ return Map.of("id",id,"name","Vendor "+id); }
}