package dev.mbonigabay.crm_mock_api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mbonigabay.crm_mock_api.model.Customer;
import dev.mbonigabay.crm_mock_api.model.DataWrapper;
import dev.mbonigabay.crm_mock_api.model.Product;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Service
public class DataLoadService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private DataWrapper data;

    public DataLoadService() {
    }

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource("data.json");
            data = objectMapper.readValue(resource.getInputStream(), DataWrapper.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load data from data.json", e);
        }
    }

    public List<Customer> getCustomers() {
        return data != null && data.crm() != null ? data.crm().customers() : Collections.emptyList();
    }

    public List<Product> getProducts() {
        return data != null && data.inventory() != null ? data.inventory().products() : Collections.emptyList();
    }
}
