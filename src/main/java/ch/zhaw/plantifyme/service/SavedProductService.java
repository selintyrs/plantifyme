package ch.zhaw.plantifyme.service;

import ch.zhaw.plantifyme.model.SavedProduct;
import ch.zhaw.plantifyme.model.SupermarketProduct;
import ch.zhaw.plantifyme.model.dto.SaveProductRequest;
import ch.zhaw.plantifyme.repository.SavedProductRepository;
import ch.zhaw.plantifyme.repository.SupermarketProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
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

    public List<SavedProduct> getByUser(String userId) {
        return savedProductRepository.findByUserIdOrderBySavedAtDesc(userId);
    }

    /**
     * Removes a saved product.
     *
     * @return false if the saved product does not exist, true on successful deletion
     * @throws OwnershipException if userId does not match the saved product owner
     */
    public boolean unsaveProduct(String id, String userId) {
        Optional<SavedProduct> opt = savedProductRepository.findById(id);
        if (opt.isEmpty()) {
            return false;
        }
        if (!opt.get().getUserId().equals(userId)) {
            throw new OwnershipException("User " + userId + " does not own saved product " + id);
        }
        savedProductRepository.deleteById(id);
        return true;
    }
}
