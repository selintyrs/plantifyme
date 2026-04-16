package ch.zhaw.plantifyme.service;

import ch.zhaw.plantifyme.model.SavedProduct;
import ch.zhaw.plantifyme.model.SupermarketProduct;
import ch.zhaw.plantifyme.model.dto.SaveProductRequest;
import ch.zhaw.plantifyme.repository.SavedProductRepository;
import ch.zhaw.plantifyme.repository.SupermarketProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class SavedProductService {

    @Autowired
    private SupermarketProductRepository supermarketProductRepository;

    @Autowired
    private SavedProductRepository savedProductRepository;

    /**
     * Saves a product as a favourite for the given user.
     *
     * @return Optional.empty() if the supermarket product does not exist
     * @throws ConflictException if the product is already saved by this user
     */
    public Optional<SavedProduct> saveProduct(String userId, SaveProductRequest request) {
        Optional<SupermarketProduct> optionalProduct =
                supermarketProductRepository.findById(request.getProductId());
        if (optionalProduct.isEmpty()) {
            return Optional.empty();
        }

        if (savedProductRepository.findByUserIdAndProductId(userId, request.getProductId()).isPresent()) {
            throw new ConflictException("Product " + request.getProductId() + " already saved by user " + userId);
        }

        SupermarketProduct product = optionalProduct.get();
        SavedProduct saved = SavedProduct.builder()
                .userId(userId)
                .productId(product.getId())
                .type(request.getType())
                .name(product.getName())
                .brand(product.getBrand())
                .price(product.getPrice())
                .supermarket(product.getSupermarket())
                .imageUrl(product.getImageUrl())
                .savedAt(LocalDateTime.now())
                .build();

        return Optional.of(savedProductRepository.save(saved));
    }
}
