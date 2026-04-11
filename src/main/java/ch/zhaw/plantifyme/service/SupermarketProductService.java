package ch.zhaw.plantifyme.service;

import ch.zhaw.plantifyme.model.SupermarketProduct;
import ch.zhaw.plantifyme.repository.SupermarketProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class SupermarketProductService {

    @Autowired
    private SupermarketProductRepository supermarketProductRepository;

    public Optional<List<SupermarketProduct>> getAlternatives(String productId, String filter) {
        Optional<SupermarketProduct> original = supermarketProductRepository.findById(productId);
        if (original.isEmpty()) {
            return Optional.empty();
        }

        SupermarketProduct product = original.get();
        List<SupermarketProduct> alternatives = supermarketProductRepository
                .findByPlantifyCatL4AndSupermarketAndIdNot(
                        product.getPlantifyCatL4(),
                        product.getSupermarket(),
                        productId
                );

        Comparator<SupermarketProduct> comparator = switch (filter) {
            case "healthier" -> Comparator.comparingDouble(
                    p -> p.getProteinG() != null ? -p.getProteinG() : 0.0
            );
            case "eco" -> Comparator.comparingDouble(
                    p -> p.getImpactCo2SavedPct() != null ? -p.getImpactCo2SavedPct() : 0.0
            );
            default -> Comparator.comparingDouble(
                    p -> p.getPrice() != null ? p.getPrice() : Double.MAX_VALUE
            );
        };

        return Optional.of(
                alternatives.stream()
                        .sorted(comparator)
                        .limit(4)
                        .toList()
        );
    }
}
